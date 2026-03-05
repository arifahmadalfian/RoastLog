package com.indie.roastlog.ui.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.indie.roastlog.data.RoastDatabase
import com.indie.roastlog.data.RoastSessionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RoastingDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = RoastDatabase.getDatabase(application).roastDao()
    
    private val _session = MutableStateFlow<RoastSessionEntity?>(null)
    val session: StateFlow<RoastSessionEntity?> = _session.asStateFlow()

    fun loadSession(id: String) {
        viewModelScope.launch {
            val entity = dao.getSessionById(id.toLong())
            _session.value = entity
        }
    }
}
