package com.indie.roastlog.ui.screens.history

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

data class RoastHistoryItem(
    val id: String,
    val date: Date,
    val beanType: String,
    val weightIn: String,
    val roastType: String
)

class RoastingHistoryViewModel : ViewModel() {
    private val _historyList = MutableStateFlow(listOf(
        RoastHistoryItem("1", Date(), "Arabica Gayo", "1000", "Medium"),
        RoastHistoryItem("2", Date(), "Robusta Java", "500", "Dark")
    ))
    val historyList: StateFlow<List<RoastHistoryItem>> = _historyList.asStateFlow()
}
