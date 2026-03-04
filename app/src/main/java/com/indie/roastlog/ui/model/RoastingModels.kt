package com.indie.roastlog.ui.model

import kotlinx.serialization.Serializable

@Serializable
data class IntervalData(
    val intervalNumber: Int,
    val temperature: Float,
    val airFlowPower: String = "",
    val rpmDrum: String = "",
    val burnerPower: String = ""
)

@Serializable
data class RoastingEvent(
    val temperature: Float,
    val seconds: Int
) {
    val time: String
        get() {
            val m = seconds / 60
            val s = seconds % 60
            return "%02d.%02d".format(m, s)
        }
}
