package com.indie.roastlog.ui.screens.running

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indie.roastlog.data.RoastDatabase
import com.indie.roastlog.data.RoastSessionEntity
import com.indie.roastlog.ui.components.ChartDataPoint
import com.indie.roastlog.ui.model.IntervalData
import com.indie.roastlog.ui.model.RoastingEvent
import com.indie.roastlog.ui.screens.form.RoastingFormState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

data class RoastingRunState(
    // Setup Data from Form
    val setupData: RoastingFormState = RoastingFormState(),
    
    // Running Data (Results)
    val weightOut: String = "",
    val endTimeTemp: String = "auto",
    val roastTime: String = "auto",
    val devTime: String = "auto",
    
    // Recorded during run (Actual events)
    val actualTurnPoint: RoastingEvent? = null,
    val actualYellowing: RoastingEvent? = null,
    val actualFirstCrack: RoastingEvent? = null,
    val actualEndRoast: RoastingEvent? = null,
    
    // Running State
    val elapsedMillis: Long = 0L,
    val isTimerRunning: Boolean = false,
    val intervalDataList: List<IntervalData> = emptyList(),
    val showTemperatureDialog: Boolean = false,
    val currentInterval: Int = 0,
    
    // ROR Dialog
    val showRorDialog: Boolean = false,
    val lastRorValue: Int? = null,
    
    // Burner Dialog
    val showBurnerDialog: Boolean = false,
    val pendingBurnerIndex: Int = -1,
    val currentBurnerIndex: Int = 0
)

class RoastingRunViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RoastingRunState())
    val uiState: StateFlow<RoastingRunState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var lastInterval: Int = 0

    fun init(formState: RoastingFormState) {
        _uiState.update { it.copy(
            setupData = formState,
            // Pre-fill actuals from setup as a baseline, or keep them for reference
            actualTurnPoint = formState.setupTurnPoint,
            actualYellowing = formState.setupYellowing,
            actualFirstCrack = formState.setupFirstCrack,
            actualEndRoast = formState.setupEndRoast
        ) }
        updateResults()
    }

    private fun updateResults() {
        _uiState.update { state ->
            val er = state.actualEndRoast
            val fc = state.actualFirstCrack
            
            var devTimeStr = "auto"
            if (fc != null && er != null) {
                val diff = er.seconds - fc.seconds
                if (diff >= 0) {
                    devTimeStr = "%d.%02d".format(diff / 60, diff % 60)
                }
            }
            
            state.copy(
                endTimeTemp = er?.temperature?.toString() ?: "auto",
                roastTime = er?.time ?: "auto",
                devTime = devTimeStr
            )
        }
    }

    private fun getRorValue(temp: Int, seconds: Int): Int? {
        val state = _uiState.value
        val intervalSec = state.setupData.intervalSeconds.toIntOrNull() ?: 60
        
        val points = mutableListOf<Pair<Int, Int>>()
        state.intervalDataList.forEach { 
            points.add(it.intervalNumber * intervalSec to it.temperature) 
        }
        
        listOfNotNull(state.actualTurnPoint, state.actualYellowing, state.actualFirstCrack, state.actualEndRoast)
            .forEach { points.add(it.seconds to it.temperature) }
            
        val prevPoint = points.filter { it.first < seconds }.maxByOrNull { it.first }
        
        return if (prevPoint != null) {
            val tempDiff = temp - prevPoint.second
            val timeDiffSeconds = seconds - prevPoint.first
            if (timeDiffSeconds > 0) {
                (tempDiff / timeDiffSeconds) * intervalSec
            } else null
        } else null
    }

    fun markTurnPoint(temp: Int) {
        val seconds = (_uiState.value.elapsedMillis / 1000).toInt()
        val ror = getRorValue(temp, seconds)
        _uiState.update { it.copy(
            actualTurnPoint = RoastingEvent(temp, seconds),
            showRorDialog = true,
            lastRorValue = ror
        ) }
        updateResults()
    }

    fun markYellowing(temp: Int) {
        val seconds = (_uiState.value.elapsedMillis / 1000).toInt()
        val ror = getRorValue(temp, seconds)
        _uiState.update { it.copy(
            actualYellowing = RoastingEvent(temp, seconds),
            showRorDialog = true,
            lastRorValue = ror
        ) }
        updateResults()
    }

    fun markFirstCrack(temp: Int) {
        val seconds = (_uiState.value.elapsedMillis / 1000).toInt()
        val ror = getRorValue(temp, seconds)
        _uiState.update { it.copy(
            actualFirstCrack = RoastingEvent(temp, seconds),
            showRorDialog = true,
            lastRorValue = ror
        ) }
        updateResults()
    }

    fun markEndRoast(temp: Int) {
        val seconds = (_uiState.value.elapsedMillis / 1000).toInt()
        val ror = getRorValue(temp, seconds)
        _uiState.update { it.copy(
            actualEndRoast = RoastingEvent(temp, seconds),
            showRorDialog = true,
            lastRorValue = ror
        ) }
        updateResults()
        stopTimer()
    }

    fun updateWeightOut(value: String) {
        _uiState.update { it.copy(weightOut = value.filter { c -> c.isDigit() }) }
    }

    fun startTimer() {
        if (timerJob?.isActive == true) return
        val setup = _uiState.value.setupData
        val duration = setup.targetDuration.toIntOrNull() ?: return
        val interval = setup.intervalSeconds.toIntOrNull() ?: return
        val startTemp = setup.chargeTimeTemp.toIntOrNull() ?: return

        _uiState.update { it.copy(isTimerRunning = true) }
        lastInterval = 0

        val totalMillis = duration.toLong() * 60 * 1000

        _uiState.update { currentState ->
            val newData = currentState.intervalDataList + IntervalData(
                intervalNumber = 0,
                temperature = startTemp
            )
            currentState.copy(intervalDataList = newData)
        }

        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis() - _uiState.value.elapsedMillis
            while (true) {
                val currentMillis = System.currentTimeMillis() - startTime
                _uiState.update { currentState ->
                    val currentSeconds = (currentMillis / 1000).toInt()
                    val currentIntervalCount = currentSeconds / interval
                    
                    if (currentIntervalCount > lastInterval) {
                        lastInterval = currentIntervalCount
                        onIntervalPassed(currentIntervalCount)
                    }

                    val burnerEventIndex = currentState.setupData.burnerPlan.indexOfFirst { it.seconds == currentSeconds }
                    var pendingBIndex = currentState.pendingBurnerIndex
                    if (burnerEventIndex != -1) {
                        if (!currentState.showTemperatureDialog && !currentState.showRorDialog) {
                            onBurnerEventReached(burnerEventIndex)
                        } else {
                            pendingBIndex = burnerEventIndex
                        }
                    }

                    if (currentMillis >= totalMillis && !currentState.showTemperatureDialog) {
                        stopTimer()
                        return@update currentState.copy(elapsedMillis = totalMillis, isTimerRunning = false)
                    }

                    currentState.copy(elapsedMillis = currentMillis, pendingBurnerIndex = pendingBIndex)
                }
                delay(10) // Update every 10ms for smooth millisecond display
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isTimerRunning = false) }
    }

    fun resetTimer() {
        stopTimer()
        lastInterval = 0
        _uiState.update { it.copy(elapsedMillis = 0L, intervalDataList = emptyList(), pendingBurnerIndex = -1, currentBurnerIndex = 0) }
    }

    private fun onIntervalPassed(interval: Int) {
        _uiState.update { it.copy(showTemperatureDialog = true, currentInterval = interval) }
    }

    private fun onBurnerEventReached(index: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                showBurnerDialog = true,
                currentBurnerIndex = index
            )
        }
    }

    fun dismissBurnerDialog() { _uiState.update { it.copy(showBurnerDialog = false) } }

    fun addTemperature(temperature: Int) {
        val intervalSec = _uiState.value.setupData.intervalSeconds.toIntOrNull() ?: 60
        val seconds = _uiState.value.currentInterval * intervalSec
        val rorValue = getRorValue(temperature, seconds)

        _uiState.update { currentState ->
            val newIntervalData = IntervalData(
                intervalNumber = currentState.currentInterval,
                temperature = temperature
            )
            val newData = currentState.intervalDataList + newIntervalData

            currentState.copy(
                intervalDataList = newData,
                showTemperatureDialog = false,
                showRorDialog = true,
                lastRorValue = rorValue
            )
        }
    }

    fun dismissTemperatureDialog() {
        _uiState.update { currentState ->
            val showBurner = currentState.pendingBurnerIndex >= 0
            currentState.copy(
                showTemperatureDialog = false,
                showBurnerDialog = showBurner,
                currentBurnerIndex = if (showBurner) currentState.pendingBurnerIndex else currentState.currentBurnerIndex,
                pendingBurnerIndex = if (showBurner) -1 else currentState.pendingBurnerIndex
            )
        }
    }

    fun dismissRorDialog() {
        _uiState.update { currentState ->
            val showBurner = currentState.pendingBurnerIndex >= 0
            currentState.copy(
                showRorDialog = false,
                lastRorValue = null,
                showBurnerDialog = showBurner,
                currentBurnerIndex = if (showBurner) currentState.pendingBurnerIndex else currentState.currentBurnerIndex,
                pendingBurnerIndex = if (showBurner) -1 else currentState.pendingBurnerIndex
            )
        }
    }

    fun updateTemperatureAtInterval(interval: Int, temperature: Int) {
        _uiState.update { currentState ->
            val updated = currentState.intervalDataList
                .filterNot { it.intervalNumber == interval }
                .plus(IntervalData(intervalNumber = interval, temperature = temperature))
                .sortedBy { it.intervalNumber }
            currentState.copy(intervalDataList = updated)
        }
    }

    fun getChartData(): List<ChartDataPoint> {
        val state = _uiState.value.setupData
        val duration = state.targetDuration.toIntOrNull() ?: return emptyList()
        val interval = state.intervalSeconds.toIntOrNull() ?: 60
        if (duration <= 0 || interval <= 0) return emptyList()

        val dataMap = _uiState.value.intervalDataList.associateBy { it.intervalNumber }
        val totalSeconds = duration * 60
        val maxIntervals = totalSeconds / interval

        val points = (0..maxIntervals).map { intervalNum ->
            val secondsAtThisInterval = intervalNum * interval
            val intervalData = dataMap[intervalNum]
            val currentTemp = intervalData?.temperature
            val prevTemp = if (intervalNum > 0) dataMap[intervalNum - 1]?.temperature else null
            val rorValue = if (intervalNum > 0 && currentTemp != null && prevTemp != null) currentTemp - prevTemp else null
            
            ChartDataPoint(
                intervalNumber = intervalNum.toInt(),
                totalSeconds = secondsAtThisInterval,
                temperature = currentTemp,
                ror = rorValue
            )
        }.toMutableList()

        val runningState = _uiState.value
        listOfNotNull(runningState.actualTurnPoint, runningState.actualYellowing, runningState.actualFirstCrack, runningState.actualEndRoast).forEach { ev ->
            points.add(ChartDataPoint(
                intervalNumber = ev.seconds.toInt() / interval,
                totalSeconds = ev.seconds,
                temperature = ev.temperature,
                ror = null
            ))
        }

        return points.sortedBy { it.totalSeconds }
    }

    fun saveToDatabase(context: Context) {
        val state = _uiState.value
        val setup = state.setupData
        
        val entity = RoastSessionEntity(
            date = Date().time,
            beanType = setup.beanType,
            waterContent = setup.waterContent,
            density = setup.density,
            weightIn = setup.weightIn,
            weightOut = state.weightOut,
            roastType = setup.roastType,
            intervalSeconds = setup.intervalSeconds.toIntOrNull() ?: 60,
            targetDuration = setup.targetDuration.toIntOrNull() ?: 0,
            endTimeTemp = state.endTimeTemp,
            roastTime = state.roastTime,
            devTime = state.devTime,
            turnPoint = state.actualTurnPoint,
            yellowing = state.actualYellowing,
            firstCrack = state.actualFirstCrack,
            endRoast = state.actualEndRoast,
            burnerPlan = setup.burnerPlan,
            temperatureData = state.intervalDataList
        )

        viewModelScope.launch {
            RoastDatabase.getDatabase(context).roastDao().insertSession(entity)
        }
    }
}
