package com.indie.roastlog.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indie.roastlog.ui.components.ChartDataPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IntervalData(
    val intervalNumber: Int,
    val temperature: Float,
    val airFlowPower: String = "",
    val rpmDrum: String = "",
    val burnerPower: String = ""
)

data class TurnPointEvent(
    val temperature: Float,
    val seconds: Int
)

data class RoastingFormState(
    val beanType: String = "",
    val waterContent: String = "",
    val density: String = "",
    val weightIn: String = "",
    val weightOut: String = "",
    val roastType: String = "Medium",
    val isRoastTypeExpanded: Boolean = false,
    // Time & Temperature
    val chargeTimeTemp: String = "220", // Charge Time (°C)
    val endTimeTemp: String = "auto", // End Time (°C)
    val roastTime: String = "auto", // Roast Time (menit)
    val devTime: String = "auto", // Dev Time (menit)
    // Event Suhu
    val turnPoint: String = "", // Turn Point (°C)
    val turnPoints: TurnPointEvent? = null,
    val yellowing: String = "", // Yellowing (°C)
    val firstCrack: String = "", // First Crack (°C)
    // Parameter Mesin
    val airFlowPower: String = "0", // Air Flow Power
    val rpmDrum: String = "0", // RPM Drum
    val burnerPower: String = "0", // Burner Power
    val ror: String = "", // ROR (Rate of Rise)
    // Timer & Chart
    val targetDuration: String = "20", // default 20 minutes
    val intervalSeconds: String = "60", // default 60 seconds (1 minute)
    val burnerIntervalSeconds: String = "210", // default 210 seconds (3 minutes 30 seconds)
    val startTemperature: String = "70", // default 70°C
    val elapsedSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val intervalDataList: List<IntervalData> = emptyList(),
    val showTemperatureDialog: Boolean = false,
    val currentInterval: Int = 0, // which interval we're currently at
    // ROR Dialog
    val showRorDialog: Boolean = false,
    val lastRorValue: Float? = null,
    // Burner Dialog
    val showBurnerDialog: Boolean = false,
    val pendingBurnerInterval: Int = -1, // pending burner interval to show after temp dialog
    val burnerValues: List<Int> = listOf(30, 70, 100), // default burner values
    val currentBurnerIndex: Int = 0 // which burner value to show next
) {
    fun canStartTimer(): Boolean {
        val duration = targetDuration.toIntOrNull()
        val interval = intervalSeconds.toIntOrNull()
        val startTemp = chargeTimeTemp.toFloatOrNull()
        return duration != null && duration > 0 &&
               interval != null && interval > 0 &&
               startTemp != null && startTemp >= 70 && startTemp <= 240
    }
}

class RoastingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RoastingFormState())
    val uiState: StateFlow<RoastingFormState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var lastInterval: Int = 0
    private var lastBurnerInterval: Int = -1

    fun updateBeanType(value: String) {
        _uiState.update { it.copy(beanType = value) }
    }

    fun updateWaterContent(value: String) {
        _uiState.update { it.copy(waterContent = value) }
    }

    fun updateDensity(value: String) {
        _uiState.update { it.copy(density = value) }
    }

    fun updateWeightIn(value: String) {
        _uiState.update { it.copy(weightIn = value) }
    }

    fun updateWeightOut(value: String) {
        _uiState.update { it.copy(weightOut = value) }
    }

    fun updateRoastType(value: String) {
        _uiState.update { it.copy(roastType = value, isRoastTypeExpanded = false) }
    }

    fun toggleRoastTypeExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isRoastTypeExpanded = expanded) }
    }

    // Time & Temperature
    fun updateChargeTimeTemp(value: String) {
        _uiState.update { it.copy(chargeTimeTemp = filterDecimal(value)) }
    }

    fun updateEndTimeTemp(value: String) {
        _uiState.update { it.copy(endTimeTemp = filterDecimal(value)) }
    }

    fun updateRoastTime(value: String) {
        _uiState.update { it.copy(roastTime = filterDigits(value)) }
    }

    fun updateDevTime(value: String) {
        _uiState.update { it.copy(devTime = filterDigits(value)) }
    }

    // Event Suhu
    fun updateTurnPoint(value: String) {
        _uiState.update { it.copy(turnPoint = filterDecimal(value)) }
    }

    fun addTurnPoint(temperature: Float, seconds: Int) {
        _uiState.update { currentState ->
            currentState.copy(turnPoints = TurnPointEvent(temperature, seconds), turnPoint = temperature.toString())
        }
    }

    fun removeTurnPoint() {
        _uiState.update { currentState ->
            currentState.copy(turnPoints = null)
        }
    }

    fun updateYellowing(value: String) {
        _uiState.update { it.copy(yellowing = filterDecimal(value)) }
    }

    fun updateFirstCrack(value: String) {
        _uiState.update { it.copy(firstCrack = filterDecimal(value)) }
    }

    // Parameter Mesin
    fun updateAirFlowPower(value: String) {
        _uiState.update { it.copy(airFlowPower = filterDigits(value)) }
    }

    fun updateRpmDrum(value: String) {
        _uiState.update { it.copy(rpmDrum = filterDigits(value)) }
    }

    fun updateBurnerPower(value: String) {
        _uiState.update { it.copy(burnerPower = filterDigits(value)) }
    }

    fun addBurnerValue(value: Int) {
        _uiState.update { currentState ->
            val newList = (currentState.burnerValues + value).sorted()
            currentState.copy(burnerValues = newList, burnerPower = value.toString())
        }
    }

    fun removeBurnerValue(value: Int) {
        _uiState.update { currentState ->
            val newList = currentState.burnerValues.filter { it != value }
            currentState.copy(burnerValues = newList)
        }
    }

    fun updateRor(value: String) {
        _uiState.update { it.copy(ror = filterDecimal(value)) }
    }

    // Timer
    fun updateTargetDuration(value: String) {
        _uiState.update { it.copy(targetDuration = filterDigits(value)) }
    }

    fun updateIntervalSeconds(value: String) {
        _uiState.update { it.copy(intervalSeconds = filterDigits(value)) }
    }

    fun updateBurnerIntervalSeconds(value: String) {
        _uiState.update { it.copy(burnerIntervalSeconds = filterDigits(value)) }
    }

    fun updateStartTemperature(value: String) {
        _uiState.update { it.copy(startTemperature = filterDecimal(value)) }
    }

    private fun filterDigits(value: String): String {
        return value.filter { it.isDigit() }
    }

    private fun filterDecimal(value: String): String {
        return value.filter { it.isDigit() || it == '.' }
            .let { text ->
                val firstDot = text.indexOf('.')
                if (firstDot == -1) text
                else text.substring(0, firstDot + 1) +
                     text.substring(firstDot + 1).replace(".", "")
            }
    }

    fun getChartData(): List<ChartDataPoint> {
        val state = _uiState.value
        val duration = state.targetDuration.toIntOrNull() ?: return emptyList()
        val interval = state.intervalSeconds.toIntOrNull() ?: 60
        if (duration <= 0 || interval <= 0) return emptyList()

        val dataMap = state.intervalDataList.associateBy { it.intervalNumber }
        val totalSeconds = duration * 60
        val maxIntervals = totalSeconds / interval

        val points = (0..maxIntervals).map { intervalNum ->
            val secondsAtThisInterval = intervalNum * interval
            val intervalData = dataMap[intervalNum]
            
            // Calculate ROR: temperature difference from previous interval
            val currentTemp = intervalData?.temperature
            val prevTemp = if (intervalNum > 0) dataMap[intervalNum - 1]?.temperature else null
            val rorValue = if (intervalNum > 0 && currentTemp != null && prevTemp != null) {
                currentTemp - prevTemp
            } else {
                null
            }
            
            ChartDataPoint(
                intervalNumber = intervalNum.toFloat(),
                totalSeconds = secondsAtThisInterval,
                temperature = currentTemp,
                ror = rorValue,
                airFlowPower = intervalData?.airFlowPower ?: "",
                rpmDrum = intervalData?.rpmDrum ?: "",
                burnerPower = intervalData?.burnerPower ?: ""
            )
        }.toMutableList()

        // Add Turn Points to the chart data
        state.turnPoints?.let { tp ->
            val floatInterval = tp.seconds.toFloat() / interval
            // Only add if not already at an interval (though adding it anyway is fine for the line chart)
            // But we don't have ROR/AirFlow for turn points easily, so we just add temperature
            points.add(ChartDataPoint(
                intervalNumber = floatInterval,
                totalSeconds = tp.seconds,
                temperature = tp.temperature,
                ror = null,
                airFlowPower = "",
                rpmDrum = "",
                burnerPower = ""
            ))
        }

        // Sort by total seconds to ensure proper line drawing
        val sortedPoints = points.sortedBy { it.totalSeconds }

        val startTemp = state.chargeTimeTemp.toFloatOrNull()
        
        // Re-calculate the first point's start temp if needed
        val finalPoints = sortedPoints.mapIndexed { index, point ->
            if (index == 0 && point.totalSeconds == 0 && point.temperature == null) {
                point.copy(temperature = startTemp)
            } else {
                point
            }
        }

        return finalPoints
    }

    fun startTimer() {
        if (timerJob?.isActive == true) return

        val state = _uiState.value
        val duration = state.targetDuration.toIntOrNull()
        val interval = state.intervalSeconds.toIntOrNull()
        val burnerInterval = state.burnerIntervalSeconds.toIntOrNull()
        val startTemp = state.chargeTimeTemp.toFloatOrNull()
        if (duration == null || duration <= 0 ||
            interval == null || interval <= 0 ||
            startTemp == null || startTemp < 70 || startTemp > 240) return

        _uiState.update { it.copy(isTimerRunning = true) }
        lastInterval = 0
        lastBurnerInterval = -1

        val totalSeconds = duration * 60

        _uiState.update { currentState ->
            val newData = currentState.intervalDataList + IntervalData(
                intervalNumber = 0,
                temperature = startTemp,
                airFlowPower = currentState.airFlowPower,
                rpmDrum = currentState.rpmDrum,
                burnerPower = currentState.burnerPower
            )
            currentState.copy(intervalDataList = newData)
        }

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { currentState ->
                    val newSeconds = currentState.elapsedSeconds + 1
                    val currentIntervalCount = newSeconds / interval
                    val maxIntervals = totalSeconds / interval

                    // Check for temperature interval popup (including final interval)
                    if (currentIntervalCount > lastInterval && currentIntervalCount <= maxIntervals) {
                        lastInterval = currentIntervalCount
                        onIntervalPassed(currentIntervalCount)
                    }

                    // Check for burner interval (only if not showing temp dialog)
                    val effectiveBurnerInterval = burnerInterval ?: 210
                    val currentBurnerCount = newSeconds / effectiveBurnerInterval
                    var pendingBurner = currentState.pendingBurnerInterval
                    if (currentBurnerCount > lastBurnerInterval && newSeconds >= effectiveBurnerInterval) {
                        lastBurnerInterval = currentBurnerCount
                        if (!currentState.showTemperatureDialog && !currentState.showRorDialog) {
                            // Show burner dialog immediately if no other dialog is showing
                            onBurnerIntervalPassed(currentBurnerCount)
                        } else {
                            // Mark as pending - will show when other dialogs are dismissed
                            pendingBurner = currentBurnerCount
                        }
                    }

                    // Auto stop timer if exceeded duration (after showing final popup)
                    // Only stop if no popup is currently showing
                    if (newSeconds >= totalSeconds && !currentState.showTemperatureDialog) {
                        // Cancel timer job and update state
                        timerJob?.cancel()
                        timerJob = null
                        return@update currentState.copy(
                            elapsedSeconds = totalSeconds,
                            isTimerRunning = false,
                            pendingBurnerInterval = pendingBurner
                        )
                    }

                    currentState.copy(elapsedSeconds = newSeconds, pendingBurnerInterval = pendingBurner)
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
        lastBurnerInterval = -1
        _uiState.update { it.copy(elapsedSeconds = 0, intervalDataList = emptyList(), pendingBurnerInterval = -1, currentBurnerIndex = 0) }
    }

    private fun onIntervalPassed(interval: Int) {
        _uiState.update {
            it.copy(
                showTemperatureDialog = true,
                currentInterval = interval
            )
        }
    }

    private fun onBurnerIntervalPassed(interval: Int) {
        _uiState.update { currentState ->
            // Check if we have more burner values to show
            val hasMoreValues = currentState.currentBurnerIndex < currentState.burnerValues.size
            if (hasMoreValues) {
                currentState.copy(
                    showBurnerDialog = true
                )
            } else {
                // No more values, don't show dialog
                currentState.copy(showBurnerDialog = false)
            }
        }
    }

    fun dismissBurnerDialog() {
        _uiState.update { currentState ->
            val newIndex = (currentState.currentBurnerIndex + 1).coerceAtMost(currentState.burnerValues.size)
            currentState.copy(
                showBurnerDialog = false,
                currentBurnerIndex = newIndex
            )
        }
    }

    fun dismissTemperatureDialog() {
        _uiState.update { currentState ->
            val totalSeconds = currentState.targetDuration.toIntOrNull()?.times(60) ?: 0
            val shouldStopTimer = currentState.elapsedSeconds >= totalSeconds && totalSeconds > 0
            
            if (shouldStopTimer) {
                timerJob?.cancel()
                timerJob = null
            }
            
            // Check if there's a pending burner interval to show and we have values left
            val hasMoreBurnerValues = currentState.currentBurnerIndex < currentState.burnerValues.size
            val showBurner = currentState.pendingBurnerInterval >= 0 && !shouldStopTimer && hasMoreBurnerValues
            
            currentState.copy(
                showTemperatureDialog = false,
                isTimerRunning = !shouldStopTimer,
                showBurnerDialog = showBurner,
                pendingBurnerInterval = if (showBurner) -1 else currentState.pendingBurnerInterval
            )
        }
    }

    fun addTemperature(temperature: Float) {
        _uiState.update { currentState ->
            val totalSeconds = currentState.targetDuration.toIntOrNull()?.times(60) ?: 0
            val shouldStopTimer = currentState.elapsedSeconds >= totalSeconds && totalSeconds > 0

            val newIntervalData = IntervalData(
                intervalNumber = currentState.currentInterval,
                temperature = temperature,
                airFlowPower = currentState.airFlowPower,
                rpmDrum = currentState.rpmDrum,
                burnerPower = currentState.burnerPower
            )
            val newData = currentState.intervalDataList + newIntervalData

            // Calculate ROR (Rate of Rise)
            val previousData = currentState.intervalDataList
                .filter { it.intervalNumber < currentState.currentInterval }
                .maxByOrNull { it.intervalNumber }
            val rorValue = if (previousData != null) {
                val tempDiff = temperature - previousData.temperature
                val intervalDiff = currentState.currentInterval - previousData.intervalNumber
                if (intervalDiff > 0) tempDiff / intervalDiff else null
            } else null

            if (shouldStopTimer) {
                timerJob?.cancel()
                timerJob = null
            }

            currentState.copy(
                intervalDataList = newData,
                showTemperatureDialog = false,
                isTimerRunning = !shouldStopTimer,
                showRorDialog = true,
                lastRorValue = rorValue
            )
        }
    }

    fun dismissRorDialog() {
        _uiState.update { currentState ->
            // Check if there's a pending burner interval to show and we have values left
            val hasMoreBurnerValues = currentState.currentBurnerIndex < currentState.burnerValues.size
            val showBurner = currentState.pendingBurnerInterval >= 0 && hasMoreBurnerValues
            currentState.copy(
                showRorDialog = false,
                lastRorValue = null,
                showBurnerDialog = showBurner,
                pendingBurnerInterval = if (showBurner) -1 else currentState.pendingBurnerInterval
            )
        }
    }

    fun updateTemperatureAtInterval(interval: Int, temperature: Float) {
        _uiState.update { currentState ->
            val existingData = currentState.intervalDataList.find { it.intervalNumber == interval }
            val updated = currentState.intervalDataList
                .filterNot { it.intervalNumber == interval }
                .plus(
                    IntervalData(
                        intervalNumber = interval,
                        temperature = temperature,
                        airFlowPower = existingData?.airFlowPower ?: currentState.airFlowPower,
                        rpmDrum = existingData?.rpmDrum ?: currentState.rpmDrum,
                        burnerPower = existingData?.burnerPower ?: currentState.burnerPower
                    )
                )
                .sortedBy { it.intervalNumber }

            currentState.copy(intervalDataList = updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
