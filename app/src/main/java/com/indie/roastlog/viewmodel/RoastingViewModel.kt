package com.indie.roastlog.viewmodel

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

data class RoastingFormState(
    val beanType: String = "",
    val waterContent: String = "",
    val density: String = "",
    val weightIn: String = "",
    val weightOut: String = "",
    val roastType: String = "Medium",
    val isRoastTypeExpanded: Boolean = false,
    // Time & Temperature
    val chargeTimeTemp: String = "", // Charge Time (°C)
    val endTimeTemp: String = "", // End Time (°C)
    val roastTime: String = "", // Roast Time (menit)
    val devTime: String = "", // Dev Time (menit)
    // Event Suhu
    val turnPoint: String = "", // Turn Point (°C)
    val yellowing: String = "", // Yellowing (°C)
    val firstCrack: String = "", // First Crack (°C)
    // Parameter Mesin
    val airFlowPower: String = "", // Air Flow Power
    val rpmDrum: String = "", // RPM Drum
    val burnerPower: String = "", // Burner Power
    val ror: String = "", // ROR (Rate of Rise)
    // Timer & Chart
    val targetDuration: String = "", // in minutes
    val intervalSeconds: String = "60", // default 60 seconds
    val startTemperature: String = "", // initial temperature
    val elapsedSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val temperatureData: List<Pair<Int, Float>> = emptyList(),
    val showTemperatureDialog: Boolean = false,
    val currentInterval: Int = 0 // which interval we're currently at
) {
    fun canStartTimer(): Boolean {
        val duration = targetDuration.toIntOrNull()
        val interval = intervalSeconds.toIntOrNull()
        val startTemp = startTemperature.toFloatOrNull()
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

        val dataMap = state.temperatureData.toMap()
        val totalSeconds = duration * 60
        val maxIntervals = totalSeconds / interval

        val points = (0..maxIntervals).map { intervalNum ->
            val secondsAtThisInterval = intervalNum * interval
            ChartDataPoint(
                intervalNumber = intervalNum,
                totalSeconds = secondsAtThisInterval,
                temperature = dataMap[intervalNum]
            )
        }.toMutableList()

        val startTemp = state.startTemperature.toFloatOrNull()
        if (startTemp != null && points.isNotEmpty() && points[0].temperature == null) {
            points[0] = points[0].copy(temperature = startTemp)
        }

        return points
    }

    fun startTimer() {
        if (timerJob?.isActive == true) return

        val state = _uiState.value
        val duration = state.targetDuration.toIntOrNull()
        val interval = state.intervalSeconds.toIntOrNull()
        val startTemp = state.startTemperature.toFloatOrNull()
        if (duration == null || duration <= 0 ||
            interval == null || interval <= 0 ||
            startTemp == null || startTemp < 70 || startTemp > 240) return

        _uiState.update { it.copy(isTimerRunning = true) }
        lastInterval = 0

        val totalSeconds = duration * 60

        _uiState.update { currentState ->
            val newData = currentState.temperatureData + Pair(0, startTemp)
            currentState.copy(temperatureData = newData)
        }

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { currentState ->
                    val newSeconds = currentState.elapsedSeconds + 1
                    val currentIntervalCount = newSeconds / interval
                    val maxIntervals = totalSeconds / interval

                    if (currentIntervalCount > lastInterval && currentIntervalCount <= maxIntervals) {
                        lastInterval = currentIntervalCount
                        onIntervalPassed(currentIntervalCount)
                    }

                    currentState.copy(elapsedSeconds = newSeconds)
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
        _uiState.update { it.copy(elapsedSeconds = 0, temperatureData = emptyList()) }
    }

    private fun onIntervalPassed(interval: Int) {
        _uiState.update {
            it.copy(
                showTemperatureDialog = true,
                currentInterval = interval
            )
        }
    }

    fun dismissTemperatureDialog() {
        _uiState.update { it.copy(showTemperatureDialog = false) }
    }

    fun addTemperature(temperature: Float) {
        _uiState.update { currentState ->
            val newData = currentState.temperatureData + Pair(currentState.currentInterval, temperature)
            currentState.copy(
                temperatureData = newData,
                showTemperatureDialog = false
            )
        }
    }

    fun updateTemperatureAtInterval(interval: Int, temperature: Float) {
        _uiState.update { currentState ->
            val updated = currentState.temperatureData
                .filterNot { it.first == interval }
                .plus(Pair(interval, temperature))
                .sortedBy { it.first }

            currentState.copy(temperatureData = updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
