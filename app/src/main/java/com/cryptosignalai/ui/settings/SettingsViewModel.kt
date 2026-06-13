package com.cryptosignalai.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cryptosignalai.data.model.AiProviderType
import com.cryptosignalai.data.remote.ai.AIProvider
import com.cryptosignalai.security.SecureStorage
import com.cryptosignalai.work.SignalScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderUiState(
    val type: AiProviderType,
    val draft: String = "",        // text field contents
    val saved: Boolean = false,    // a key is stored
    val editing: Boolean = true,   // field is editable
    val testing: Boolean = false,
    val testResult: String? = null
)

data class SettingsUiState(
    val selectedProvider: AiProviderType = AiProviderType.CLAUDE,
    val providers: Map<AiProviderType, ProviderUiState> = emptyMap(),
    val notificationsEnabled: Boolean = true,
    val backgroundEnabled: Boolean = true
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = SecureStorage.get(app)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private fun load(): SettingsUiState {
        val providers = AiProviderType.entries.associateWith { type ->
            val hasKey = storage.hasApiKey(type)
            ProviderUiState(
                type = type,
                draft = if (hasKey) MASK else "",
                saved = hasKey,
                editing = !hasKey
            )
        }
        return SettingsUiState(
            selectedProvider = storage.selectedProvider,
            providers = providers,
            notificationsEnabled = storage.notificationsEnabled,
            backgroundEnabled = storage.backgroundEnabled
        )
    }

    fun selectProvider(type: AiProviderType) {
        storage.selectedProvider = type
        _state.update { it.copy(selectedProvider = type) }
    }

    fun onDraftChange(type: AiProviderType, value: String) =
        updateProvider(type) { it.copy(draft = value, testResult = null) }

    fun saveKey(type: AiProviderType) {
        val draft = _state.value.providers[type]?.draft.orEmpty()
        if (draft.isBlank() || draft == MASK) return
        storage.saveApiKey(type, draft)
        updateProvider(type) { it.copy(draft = MASK, saved = true, editing = false, testResult = "Saved") }
    }

    fun editKey(type: AiProviderType) =
        updateProvider(type) { it.copy(draft = "", editing = true, testResult = null) }

    fun testConnection(type: AiProviderType) {
        val key = if (_state.value.providers[type]?.editing == true)
            _state.value.providers[type]?.draft else storage.getApiKey(type)
        if (key.isNullOrBlank() || key == MASK) {
            updateProvider(type) { it.copy(testResult = "Enter or save a key first") }
            return
        }
        updateProvider(type) { it.copy(testing = true, testResult = null) }
        viewModelScope.launch {
            val provider = AIProvider.create(type, key)
            val result = provider?.testConnection()
                ?: Result.failure(IllegalStateException("No provider"))
            val msg = result.fold({ it }, { "Failed: ${it.message}" })
            updateProvider(type) { it.copy(testing = false, testResult = msg) }
        }
    }

    fun setNotifications(enabled: Boolean) {
        storage.notificationsEnabled = enabled
        _state.update { it.copy(notificationsEnabled = enabled) }
    }

    fun setBackground(enabled: Boolean) {
        storage.backgroundEnabled = enabled
        if (enabled) SignalScheduler.schedule(getApplication())
        else SignalScheduler.cancel(getApplication())
        _state.update { it.copy(backgroundEnabled = enabled) }
    }

    private inline fun updateProvider(type: AiProviderType, transform: (ProviderUiState) -> ProviderUiState) {
        _state.update { s ->
            val current = s.providers[type] ?: ProviderUiState(type)
            s.copy(providers = s.providers + (type to transform(current)))
        }
    }

    companion object {
        private const val MASK = "••••••••••••"
    }
}
