package com.indie.roastlog.ui.screens.form

import androidx.lifecycle.ViewModel
import com.indie.roastlog.ui.model.RoastingEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable

@Serializable
data class RoastingFormState(
    val beanType: String = "",
    val waterContent: String = "",
    val density: String = "",
    val weightIn: String = "",
    val roastType: String = "Medium",
    val isRoastTypeExpanded: Boolean = false,
    
    // Setup initial parameters
    val chargeTimeTemp: String = "220",
    val airFlowPower: String = "0",
    val rpmDrum: String = "0",
    val burnerPower: String = "0",
    
    // Event Setup (Manual target points)
    val setupTurnPoint: RoastingEvent? = null,
    val setupYellowing: RoastingEvent? = null,
    val setupFirstCrack: RoastingEvent? = null,
    val setupEndRoast: RoastingEvent? = null,
    
    // Timer & Burner Plan
    val targetDuration: String = "20",
    val intervalSeconds: String = "60",
    val burnerPlan: List<RoastingEvent> = emptyList()
)

class RoastingFormViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RoastingFormState())
    val uiState: StateFlow<RoastingFormState> = _uiState.asStateFlow()

    fun updateBeanType(value: String) { _uiState.update { it.copy(beanType = value) } }
    fun updateWaterContent(value: String) { _uiState.update { it.copy(waterContent = filterDecimal(value)) } }
    fun updateDensity(value: String) { _uiState.update { it.copy(density = filterDecimal(value)) } }
    fun updateWeightIn(value: String) { _uiState.update { it.copy(weightIn = filterDigits(value)) } }
    fun updateRoastType(value: String) { _uiState.update { it.copy(roastType = value, isRoastTypeExpanded = false) } }
    fun toggleRoastTypeExpanded(expanded: Boolean) { _uiState.update { it.copy(isRoastTypeExpanded = expanded) } }
    fun updateChargeTimeTemp(value: String) { _uiState.update { it.copy(chargeTimeTemp = filterDecimal(value)) } }
    fun updateAirFlowPower(value: String) { _uiState.update { it.copy(airFlowPower = filterDigits(value)) } }
    fun updateRpmDrum(value: String) { _uiState.update { it.copy(rpmDrum = filterDigits(value)) } }
    fun updateBurnerPower(value: String) { _uiState.update { it.copy(burnerPower = filterDigits(value)) } }
    fun updateTargetDuration(value: String) { _uiState.update { it.copy(targetDuration = filterDigits(value)) } }
    fun updateIntervalSeconds(value: String) { _uiState.update { it.copy(intervalSeconds = filterDigits(value)) } }

    fun addTurnPoint(temp: Float, sec: Int) { _uiState.update { it.copy(setupTurnPoint = RoastingEvent(temp, sec)) } }
    fun removeTurnPoint() { _uiState.update { it.copy(setupTurnPoint = null) } }
    fun addYellowing(temp: Float, sec: Int) { _uiState.update { it.copy(setupYellowing = RoastingEvent(temp, sec)) } }
    fun removeYellowing() { _uiState.update { it.copy(setupYellowing = null) } }
    fun addFirstCrack(temp: Float, sec: Int) { _uiState.update { it.copy(setupFirstCrack = RoastingEvent(temp, sec)) } }
    fun removeFirstCrack() { _uiState.update { it.copy(setupFirstCrack = null) } }
    fun addEndRoast(temp: Float, sec: Int) { _uiState.update { it.copy(setupEndRoast = RoastingEvent(temp, sec)) } }
    fun removeEndRoast() { _uiState.update { it.copy(setupEndRoast = null) } }

    fun addBurnerEvent(power: Float, sec: Int) {
        _uiState.update { it.copy(burnerPlan = (it.burnerPlan + RoastingEvent(power, sec)).sortedBy { e -> e.seconds }) }
    }
    fun removeBurnerEvent(event: RoastingEvent) {
        _uiState.update { it.copy(burnerPlan = it.burnerPlan.filter { e -> e != event }) }
    }

    private fun filterDigits(value: String) = value.filter { it.isDigit() }
    private fun filterDecimal(value: String) = value.filter { it.isDigit() || it == '.' }
}
