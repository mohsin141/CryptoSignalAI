package com.cryptosignalai.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.cryptosignalai.data.model.AiProviderType
import com.cryptosignalai.data.model.Timeframe
import com.cryptosignalai.data.model.Token

/**
 * Encrypted local storage for sensitive data (AI API keys) and lightweight
 * user preferences. Keys are encrypted at rest via Android Keystore-backed
 * AES-256 using Jetpack Security (EncryptedSharedPreferences).
 *
 * Nothing leaves the device except direct calls the user explicitly configures.
 */
class SecureStorage(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ---- AI API keys -------------------------------------------------------

    fun saveApiKey(provider: AiProviderType, key: String) {
        prefs.edit().putString(keyName(provider), key.trim()).apply()
    }

    fun getApiKey(provider: AiProviderType): String? =
        prefs.getString(keyName(provider), null)?.takeIf { it.isNotBlank() }

    fun hasApiKey(provider: AiProviderType): Boolean = !getApiKey(provider).isNullOrBlank()

    fun clearApiKey(provider: AiProviderType) {
        prefs.edit().remove(keyName(provider)).apply()
    }

    // ---- Selected provider / token / timeframe -----------------------------

    var selectedProvider: AiProviderType
        get() = runCatching {
            AiProviderType.valueOf(prefs.getString(KEY_PROVIDER, AiProviderType.CLAUDE.name)!!)
        }.getOrDefault(AiProviderType.CLAUDE)
        set(value) = prefs.edit().putString(KEY_PROVIDER, value.name).apply()

    var selectedToken: Token
        get() = Token.fromName(prefs.getString(KEY_TOKEN, Token.BTC.name)!!)
        set(value) = prefs.edit().putString(KEY_TOKEN, value.name).apply()

    var selectedTimeframe: Timeframe
        get() = Timeframe.fromLabel(prefs.getString(KEY_TIMEFRAME, Timeframe.H1.label)!!)
        set(value) = prefs.edit().putString(KEY_TIMEFRAME, value.label).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFS, value).apply()

    var backgroundEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND, true)
        set(value) = prefs.edit().putBoolean(KEY_BACKGROUND, value).apply()

    private fun keyName(provider: AiProviderType) = "api_key_${provider.name}"

    companion object {
        const val PREFS_NAME = "cryptosignalai_secure_prefs"
        private const val KEY_PROVIDER = "selected_provider"
        private const val KEY_TOKEN = "selected_token"
        private const val KEY_TIMEFRAME = "selected_timeframe"
        private const val KEY_NOTIFS = "notifications_enabled"
        private const val KEY_BACKGROUND = "background_enabled"

        @Volatile private var INSTANCE: SecureStorage? = null
        fun get(context: Context): SecureStorage =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureStorage(context).also { INSTANCE = it }
            }
    }
}
