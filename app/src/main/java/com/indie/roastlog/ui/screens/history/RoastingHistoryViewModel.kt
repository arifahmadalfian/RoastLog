package com.indie.roastlog.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.indie.roastlog.data.RoastDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.*

data class RoastHistoryItem(
    val id: String,
    val date: Date,
    val beanType: String,
    val weightIn: String,
    val roastType: String
)

class RoastingHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = RoastDatabase.getDatabase(application).roastDao()

    val historyList: StateFlow<List<RoastHistoryItem>> = dao.getAllSessions()
        .map { entities ->
            entities.map { entity ->
                RoastHistoryItem(
                    id = entity.id.toString(),
                    date = Date(entity.date),
                    beanType = entity.beanType,
                    weightIn = entity.weightIn,
                    roastType = entity.roastType
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
