package com.cryptosignalai.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cryptosignalai.data.local.SignalEntity
import com.cryptosignalai.data.repository.SignalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SignalRepository(app)

    val signals: StateFlow<List<SignalEntity>> =
        repo.observeSignals().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clear() = viewModelScope.launch { repo.clearSignals() }
}
