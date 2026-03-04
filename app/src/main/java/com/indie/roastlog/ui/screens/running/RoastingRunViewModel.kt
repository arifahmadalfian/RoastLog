package com.indie.roastlog.ui.screens.running

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indie.roastlog.ui.components.ChartDataPoint
import com.indie.roastlog.ui.model.IntervalData
import com.indie.roastlog.ui.screens.form.RoastingFormState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class RoastingRunState(
    // Setup Data from Form
    val setupData: RoastingFormState = RoastingFormState(),
    
    // Running Data (Results)
    val weightOut: String = "",
    val endTimeTemp: String = "auto",
    val roastTime: String = "auto",
    val devTime: String = "auto",
    
    // Running State
    val elapsedSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val intervalDataList: List<IntervalData> = emptyList(),
    val showTemperatureDialog: Boolean = false,
    val currentInterval: Int = 0,
    
    // ROR Dialog
    val showRorDialog: Boolean = false,
    val lastRorValue: Float? = null,
    
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
            endTimeTemp = formState.setupEndRoast?.temperature?.toString() ?: "auto",
            roastTime = formState.setupEndRoast?.time ?: "auto"
        ) }
        calculateDevTime()
    }

    private fun calculateDevTime() {
        _uiState.update { state ->
            val fc = state.setupData.setupFirstCrack
            val er = state.setupData.setupEndRoast
            var devTimeStr = "auto"
            if (fc != null && er != null) {
                val diff = er.seconds - fc.seconds
                if (diff >= 0) {
                    val m = diff / 60
                    val s = diff % 60
                    devTimeStr = "%d.%02d".format(m, s)
                }
            }
            state.copy(devTime = devTimeStr)
        }
    }

    fun updateWeightOut(value: String) {
        _uiState.update { it.copy(weightOut = value.filter { c -> c.isDigit() }) }
    }

    fun startTimer() {
        if (timerJob?.isActive == true) return
        val setup = _uiState.value.setupData
        val duration = setup.targetDuration.toIntOrNull() ?: return
        val interval = setup.intervalSeconds.toIntOrNull() ?: return
        val startTemp = setup.chargeTimeTemp.toFloatOrNull() ?: return

        _uiState.update { it.copy(isTimerRunning = true) }
        lastInterval = 0

        val totalSeconds = duration * 60

        _uiState.update { currentState ->
            val newData = currentState.intervalDataList + IntervalData(
                intervalNumber = 0,
                temperature = startTemp
            )
            currentState.copy(intervalDataList = newData)
        }

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { currentState ->
                    val newSeconds = currentState.elapsedSeconds + 1
                    val currentIntervalCount = newSeconds / interval
                    
                    if (currentIntervalCount > lastInterval) {
                        lastInterval = currentIntervalCount
                        onIntervalPassed(currentIntervalCount)
                    }

                    val burnerEventIndex = currentState.setupData.burnerPlan.indexOfFirst { it.seconds == newSeconds }
                    var pendingBIndex = currentState.pendingBurnerIndex
                    if (burnerEventIndex != -1) {
                        if (!currentState.showTemperatureDialog && !currentState.showRorDialog) {
                            onBurnerEventReached(burnerEventIndex)
                        } else {
                            pendingBIndex = burnerEventIndex
                        }
                    }

                    if (newSeconds >= totalSeconds && !currentState.showTemperatureDialog) {
                        stopTimer()
                        return@update currentState.copy(elapsedSeconds = totalSeconds, isTimerRunning = false)
                    }

                    currentState.copy(elapsedSeconds = newSeconds, pendingBurnerIndex = pendingBIndex)
                }
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
        _uiState.update { it.copy(elapsedSeconds = 0, intervalDataList = emptyList(), pendingBurnerIndex = -1, currentBurnerIndex = 0) }
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

    fun addTemperature(temperature: Float) {
        _uiState.update { currentState ->
            val newIntervalData = IntervalData(
                intervalNumber = currentState.currentInterval,
                temperature = temperature
            )
            val newData = currentState.intervalDataList + newIntervalData

            val previousData = currentState.intervalDataList
                .filter { it.intervalNumber < currentState.currentInterval }
                .maxByOrNull { it.intervalNumber }
            val rorValue = if (previousData != null) {
                val tempDiff = temperature - previousData.temperature
                val intervalDiff = currentState.currentInterval - previousData.intervalNumber
                if (intervalDiff > 0) tempDiff / intervalDiff else null
            } else null

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

    fun updateTemperatureAtInterval(interval: Int, temperature: Float) {
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
                intervalNumber = intervalNum.toFloat(),
                totalSeconds = secondsAtThisInterval,
                temperature = currentTemp,
                ror = rorValue
            )
        }.toMutableList()

        listOfNotNull(state.setupTurnPoint, state.setupYellowing, state.setupFirstCrack, state.setupEndRoast).forEach { ev ->
            points.add(ChartDataPoint(
                intervalNumber = ev.seconds.toFloat() / interval,
                totalSeconds = ev.seconds,
                temperature = ev.temperature,
                ror = null
            ))
        }

        return points.sortedBy { it.totalSeconds }
    }
}
