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
    val intervalDataList: List<IntervalData> = emptyList(), // For chart (regular intervals)
    val eventMarks: List<RoastingEvent> = emptyList(), // For events at specific times (Turn P, Yellow, etc.)
    val showTemperatureDialog: Boolean = false,
    val currentInterval: Int = 0,
    
    // ROR Dialog
    val showRorDialog: Boolean = false,
    val lastRorValue: Int? = null,
    
    // Burner Dialog
    val showBurnerDialog: Boolean = false,
    val pendingBurnerIndex: Int = -1,
    val currentBurnerIndex: Int = -1,
    val confirmedBurnerIndex: Int = -1,  // Track which burner events have been confirmed
    
    // Air Flow Dialog
    val showAirFlowDialog: Boolean = false,
    val pendingAirFlowIndex: Int = -1,
    val currentAirFlowIndex: Int = -1,
    val confirmedAirFlowIndex: Int = -1,  // Track which air flow events have been confirmed
    
    // RPM Dialog  
    val showRpmDialog: Boolean = false,
    val pendingRpmIndex: Int = -1,
    val currentRpmIndex: Int = -1,
    val confirmedRpmIndex: Int = -1  // Track which RPM events have been confirmed
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
        
        // Build list of all data points with their exact timestamps
        val points = mutableListOf(
            0 to state.setupData.chargeTimeTemp.toInt()
        ) // (seconds, temperature)
        
        // Add interval data points
        state.intervalDataList.forEach { 
            points.add(it.intervalNumber * intervalSec to it.temperature) 
        }
        
        // Add event mark points (Turn Point, Yellowing, First Crack, End Roast)
        listOfNotNull(state.actualTurnPoint, state.actualYellowing, state.actualFirstCrack, state.actualEndRoast)
            .forEach { points.add(it.seconds to it.temperature) }
        
        // Sort by time and remove duplicates
        val uniquePoints = points.sortedBy { it.first }
            .groupBy { it.first }
            .map { (_, temps) -> temps.maxByOrNull { it.second } ?: temps.first() }
            
        // Find the most recent point BEFORE current time
        val prevPoint = uniquePoints.lastOrNull { it.first < seconds }
        
        return if (prevPoint != null) {
            (temp - prevPoint.second)
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

                    // Check Burner events
                    val burnerEventIndex = currentState.setupData.burnerPlan.indexOfFirst { it.seconds == currentSeconds }
                    var pendingBIndex = currentState.pendingBurnerIndex
                    if (burnerEventIndex != -1) {
                        if (!currentState.showTemperatureDialog && !currentState.showRorDialog) {
                            onBurnerEventReached(burnerEventIndex)
                        } else {
                            pendingBIndex = burnerEventIndex
                        }
                    }

                    // Check Air Flow events
                    val airFlowEventIndex = currentState.setupData.airFlowPlan.indexOfFirst { it.seconds == currentSeconds }
                    var pendingAFIndex = currentState.pendingAirFlowIndex
                    if (airFlowEventIndex != -1) {
                        if (!currentState.showTemperatureDialog && !currentState.showRorDialog) {
                            onAirFlowEventReached(airFlowEventIndex)
                        } else {
                            pendingAFIndex = airFlowEventIndex
                        }
                    }

                    // Check RPM events
                    val rpmEventIndex = currentState.setupData.rpmPlan.indexOfFirst { it.seconds == currentSeconds }
                    var pendingRIndex = currentState.pendingRpmIndex
                    if (rpmEventIndex != -1) {
                        if (!currentState.showTemperatureDialog && !currentState.showRorDialog) {
                            onRpmEventReached(rpmEventIndex)
                        } else {
                            pendingRIndex = rpmEventIndex
                        }
                    }

                    if (currentMillis >= totalMillis && !currentState.showTemperatureDialog) {
                        stopTimer()
                        return@update currentState.copy(elapsedMillis = totalMillis, isTimerRunning = false)
                    }

                    currentState.copy(
                        elapsedMillis = currentMillis,
                        pendingBurnerIndex = pendingBIndex,
                        pendingAirFlowIndex = pendingAFIndex,
                        pendingRpmIndex = pendingRIndex
                    )
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
        _uiState.update {
            it.copy(
                elapsedMillis = 0L,
                intervalDataList = emptyList(),
                pendingBurnerIndex = -1,
                currentBurnerIndex = -1,
                confirmedBurnerIndex = -1,
                pendingAirFlowIndex = -1,
                currentAirFlowIndex = -1,
                confirmedAirFlowIndex = -1,
                pendingRpmIndex = -1,
                currentRpmIndex = -1,
                confirmedRpmIndex = -1
            )
        }
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

    private fun onAirFlowEventReached(index: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                showAirFlowDialog = true,
                currentAirFlowIndex = index
            )
        }
    }

    private fun onRpmEventReached(index: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                showRpmDialog = true,
                currentRpmIndex = index
            )
        }
    }

    fun dismissBurnerDialog() {
        _uiState.update { 
            it.copy(
                showBurnerDialog = false,
                confirmedBurnerIndex = it.currentBurnerIndex  // Confirm this event
            ) 
        }
    }

    fun dismissAirFlowDialog() {
        _uiState.update { 
            it.copy(
                showAirFlowDialog = false,
                confirmedAirFlowIndex = it.currentAirFlowIndex  // Confirm this event
            ) 
        }
    }

    fun dismissRpmDialog() {
        _uiState.update { 
            it.copy(
                showRpmDialog = false,
                confirmedRpmIndex = it.currentRpmIndex  // Confirm this event
            ) 
        }
    }

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
            val showAirFlow = !showBurner && currentState.pendingAirFlowIndex >= 0
            val showRpm = !showBurner && !showAirFlow && currentState.pendingRpmIndex >= 0
            
            currentState.copy(
                showTemperatureDialog = false,
                showBurnerDialog = showBurner,
                currentBurnerIndex = if (showBurner) currentState.pendingBurnerIndex else currentState.currentBurnerIndex,
                pendingBurnerIndex = if (showBurner) -1 else currentState.pendingBurnerIndex,
                showAirFlowDialog = showAirFlow,
                currentAirFlowIndex = if (showAirFlow) currentState.pendingAirFlowIndex else currentState.currentAirFlowIndex,
                pendingAirFlowIndex = if (showAirFlow) -1 else currentState.pendingAirFlowIndex,
                showRpmDialog = showRpm,
                currentRpmIndex = if (showRpm) currentState.pendingRpmIndex else currentState.currentRpmIndex,
                pendingRpmIndex = if (showRpm) -1 else currentState.pendingRpmIndex
            )
        }
    }

    fun dismissRorDialog() {
        _uiState.update { currentState ->
            val showBurner = currentState.pendingBurnerIndex >= 0
            val showAirFlow = !showBurner && currentState.pendingAirFlowIndex >= 0
            val showRpm = !showBurner && !showAirFlow && currentState.pendingRpmIndex >= 0
            
            currentState.copy(
                showRorDialog = false,
                lastRorValue = null,
                showBurnerDialog = showBurner,
                currentBurnerIndex = if (showBurner) currentState.pendingBurnerIndex else currentState.currentBurnerIndex,
                pendingBurnerIndex = if (showBurner) -1 else currentState.pendingBurnerIndex,
                showAirFlowDialog = showAirFlow,
                currentAirFlowIndex = if (showAirFlow) currentState.pendingAirFlowIndex else currentState.currentAirFlowIndex,
                pendingAirFlowIndex = if (showAirFlow) -1 else currentState.pendingAirFlowIndex,
                showRpmDialog = showRpm,
                currentRpmIndex = if (showRpm) currentState.pendingRpmIndex else currentState.currentRpmIndex,
                pendingRpmIndex = if (showRpm) -1 else currentState.pendingRpmIndex
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

        // Build regular interval points (0, 1, 2, 3...) - evenly spaced
        val points = (0..maxIntervals).map { intervalNum ->
            val secondsAtThisInterval = intervalNum * interval
            val intervalData = dataMap[intervalNum]
            val currentTemp = intervalData?.temperature
            val prevTemp = if (intervalNum > 0) dataMap[intervalNum - 1]?.temperature else null
            val rorValue = if (intervalNum > 0 && currentTemp != null && prevTemp != null) currentTemp - prevTemp else null

            // Find matching plan events by exact seconds
            val airFlowEvent = state.airFlowPlan.find { it.seconds == secondsAtThisInterval }
            val rpmEvent = state.rpmPlan.find { it.seconds == secondsAtThisInterval }
            val burnerEvent = state.burnerPlan.find { it.seconds == secondsAtThisInterval }

            ChartDataPoint(
                intervalNumber = intervalNum.toFloat(), // Even spacing for regular intervals
                totalSeconds = secondsAtThisInterval,
                temperature = currentTemp,
                ror = rorValue,
                airFlowPower = airFlowEvent?.temperature?.toString() ?: "",
                rpmDrum = rpmEvent?.temperature?.toString() ?: "",
                burnerPower = burnerEvent?.temperature?.toString() ?: ""
            )
        }.toMutableList()

        // Add event marks at their exact positions ( between intervals)
        val runningState = _uiState.value
        listOfNotNull(runningState.actualTurnPoint, runningState.actualYellowing, runningState.actualFirstCrack, runningState.actualEndRoast).forEach { ev ->
            val eventIntervalNum = ev.seconds.toFloat() / interval
            
            // Find matching plan events by exact seconds for event marks
            val airFlowEvent = state.airFlowPlan.find { it.seconds == ev.seconds }
            val rpmEvent = state.rpmPlan.find { it.seconds == ev.seconds }
            val burnerEvent = state.burnerPlan.find { it.seconds == ev.seconds }
            
            points.add(ChartDataPoint(
                intervalNumber = eventIntervalNum, // Exact position for events
                totalSeconds = ev.seconds,
                temperature = ev.temperature,
                ror = null,
                airFlowPower = airFlowEvent?.temperature?.toString() ?: "",
                rpmDrum = rpmEvent?.temperature?.toString() ?: "",
                burnerPower = burnerEvent?.temperature?.toString() ?: ""
            ))
        }

        return points.sortedBy { it.totalSeconds }
    }

    fun getChartRor(): List<ChartDataPoint> {
        val positions = getAllChartPositions()
        if (positions.isEmpty()) return emptyList()

        return positions.map { (intervalNum, seconds) ->
            val rorValue = getRorValueForChart(seconds)

            ChartDataPoint(
                intervalNumber = intervalNum,
                totalSeconds = seconds,
                temperature = null,
                ror = rorValue,
                airFlowPower = "",
                rpmDrum = "",
                burnerPower = ""
            )
        }
    }

    private fun getRorValueForChart(seconds: Int): Int? {
        // At second 0, RoR is 0 (starting point, no previous data)
        if (seconds == 0) return 0
        
        val currentTemp = getTemperatureAtSeconds(seconds) ?: return null
        val prevTemp = getPreviousTemperature(seconds) ?: return null
        return currentTemp - prevTemp
    }

    private fun getTemperatureAtSeconds(seconds: Int): Int? {
        val state = _uiState.value
        val intervalSec = state.setupData.intervalSeconds.toIntOrNull() ?: 60

        // Build list of all data points with their exact timestamps
        val points = mutableListOf<Pair<Int, Int>>()

        // Add charge time temp at 0 seconds
        val chargeTemp = state.setupData.chargeTimeTemp.toIntOrNull()
        if (chargeTemp != null) {
            points.add(0 to chargeTemp)
        }

        // Add interval data points
        state.intervalDataList.forEach {
            points.add(it.intervalNumber * intervalSec to it.temperature)
        }

        // Add event mark points
        listOfNotNull(state.actualTurnPoint, state.actualYellowing, state.actualFirstCrack, state.actualEndRoast)
            .forEach { points.add(it.seconds to it.temperature) }

        // Find exact match or interpolate
        val sortedPoints = points.sortedBy { it.first }

        // Check for exact match
        sortedPoints.find { it.first == seconds }?.let { return it.second }

        // Find surrounding points for interpolation
        val before = sortedPoints.lastOrNull { it.first < seconds }
        val after = sortedPoints.firstOrNull { it.first > seconds }

        return when {
            before != null && after != null -> {
                // Linear interpolation
                val ratio = (seconds - before.first).toFloat() / (after.first - before.first)
                (before.second + ratio * (after.second - before.second)).toInt()
            }
            before != null -> before.second
            after != null -> after.second
            else -> null
        }
    }

    private fun getPreviousTemperature(seconds: Int): Int? {
        val state = _uiState.value
        val intervalSec = state.setupData.intervalSeconds.toIntOrNull() ?: 60

        val points = mutableListOf<Pair<Int, Int>>()

        val chargeTemp = state.setupData.chargeTimeTemp.toIntOrNull()
        if (chargeTemp != null) {
            points.add(0 to chargeTemp)
        }

        state.intervalDataList.forEach {
            points.add(it.intervalNumber * intervalSec to it.temperature)
        }

        listOfNotNull(state.actualTurnPoint, state.actualYellowing, state.actualFirstCrack, state.actualEndRoast)
            .forEach { points.add(it.seconds to it.temperature) }

        return points.sortedBy { it.first }.lastOrNull { it.first < seconds }?.second
    }

    fun getChartAirFlow(): List<ChartDataPoint> {
        val state = _uiState.value.setupData
        val runningState = _uiState.value
        val positions = getAllChartPositions()
        if (positions.isEmpty()) return emptyList()

        val sortedPlan = state.airFlowPlan.sortedBy { it.seconds }

        return positions.map { (intervalNum, seconds) ->
            // Show value when dialog is showing or has been confirmed
            val applicableValue = getValueAtTimeFromPlan(sortedPlan, seconds, runningState.currentAirFlowIndex)

            ChartDataPoint(
                intervalNumber = intervalNum,
                totalSeconds = seconds,
                temperature = null,
                ror = null,
                airFlowPower = applicableValue?.toString() ?: "",
                rpmDrum = "",
                burnerPower = ""
            )
        }
    }

    fun getChartRpm(): List<ChartDataPoint> {
        val state = _uiState.value.setupData
        val runningState = _uiState.value
        val positions = getAllChartPositions()
        if (positions.isEmpty()) return emptyList()

        val sortedPlan = state.rpmPlan.sortedBy { it.seconds }

        return positions.map { (intervalNum, seconds) ->
            val applicableValue = getValueAtTimeFromPlan(sortedPlan, seconds, runningState.currentRpmIndex)

            ChartDataPoint(
                intervalNumber = intervalNum,
                totalSeconds = seconds,
                temperature = null,
                ror = null,
                airFlowPower = "",
                rpmDrum = applicableValue?.toString() ?: "",
                burnerPower = ""
            )
        }
    }

    fun getChartBurner(): List<ChartDataPoint> {
        val state = _uiState.value.setupData
        val runningState = _uiState.value
        val positions = getAllChartPositions()
        if (positions.isEmpty()) return emptyList()

        val sortedPlan = state.burnerPlan.sortedBy { it.seconds }

        return positions.map { (intervalNum, seconds) ->
            val applicableValue = getValueAtTimeFromPlan(sortedPlan, seconds, runningState.currentBurnerIndex)

            ChartDataPoint(
                intervalNumber = intervalNum,
                totalSeconds = seconds,
                temperature = null,
                ror = null,
                airFlowPower = "",
                rpmDrum = "",
                burnerPower = applicableValue?.toString() ?: ""
            )
        }
    }

    private fun getValueAtTimeFromPlan(sortedPlan: List<RoastingEvent>, seconds: Int, confirmedIndex: Int): Int? {
        // No events confirmed yet, return null (empty value)
        if (confirmedIndex < 0 || sortedPlan.isEmpty()) return null

        // Only show value at exact plan event times that have been confirmed
        val confirmedEvents = sortedPlan.take(confirmedIndex + 1)
        val exactMatch = confirmedEvents.find { it.seconds == seconds }
        
        return exactMatch?.temperature
    }

    private fun getAllChartPositions(): List<Pair<Float, Int>> {
        val state = _uiState.value.setupData
        val runningState = _uiState.value
        val duration = state.targetDuration.toIntOrNull() ?: return emptyList()
        val interval = state.intervalSeconds.toIntOrNull() ?: 60
        if (duration <= 0 || interval <= 0) return emptyList()

        val totalSeconds = duration * 60
        val maxIntervals = totalSeconds / interval

        val positions = mutableListOf<Pair<Float, Int>>()

        // Add regular interval positions
        for (i in 0..maxIntervals) {
            positions.add(i.toFloat() to (i * interval))
        }

        // Add event mark positions (fractional positions)
        listOfNotNull(
            runningState.actualTurnPoint,
            runningState.actualYellowing,
            runningState.actualFirstCrack,
            runningState.actualEndRoast
        ).forEach { ev ->
            val eventIntervalNum = ev.seconds.toFloat() / interval
            // Only add if not already in list
            if (positions.none { it.first == eventIntervalNum }) {
                positions.add(eventIntervalNum to ev.seconds)
            }
        }

        return positions.sortedBy { it.first }
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
