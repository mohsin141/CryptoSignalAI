package com.cryptosignalai.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cryptosignalai.data.model.FinalSignal
import com.cryptosignalai.data.model.Timeframe
import com.cryptosignalai.data.model.Token
import com.cryptosignalai.data.repository.SignalRepository
import com.cryptosignalai.domain.pipeline.SignalPipeline
import com.cryptosignalai.notification.NotificationHelper
import com.cryptosignalai.security.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val token: Token = Token.BTC,
    val timeframe: Timeframe = Timeframe.H1,
    val loading: Boolean = false,
    val signal: FinalSignal? = null,
    val stageLog: List<String> = emptyList(),
    val error: String? = null
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = SecureStorage.get(app)
    private val pipeline = SignalPipeline()
    private val signalRepo = SignalRepository(app)
    private val notifier = NotificationHelper(app)

    private val _state = MutableStateFlow(
        HomeUiState(token = storage.selectedToken, timeframe = storage.selectedTimeframe)
    )
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun selectToken(token: Token) {
        storage.selectedToken = token
        _state.update { it.copy(token = token) }
    }

    fun selectTimeframe(tf: Timeframe) {
        storage.selectedTimeframe = tf
        _state.update { it.copy(timeframe = tf) }
    }

    fun refresh() {
        val s = _state.value
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val providerType = storage.selectedProvider
            val key = storage.getApiKey(providerType)
            val outcome = pipeline.run(s.token, s.timeframe, providerType, key)
            val final = outcome.final

            // Persist history + backtest log
            runCatching { signalRepo.saveSignal(final) }

            // Notify only on strong, high-confidence directional signals
            if (storage.notificationsEnabled &&
                (final.decision == "BUY" || final.decision == "SELL") &&
                final.confidence >= NotificationHelper.MIN_CONFIDENCE
            ) {
                notifier.notifySignal(final)
            }

            _state.update {
                it.copy(loading = false, signal = final, stageLog = outcome.stageLog, error = null)
            }
        }
    }
}
