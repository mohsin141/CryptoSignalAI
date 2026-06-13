package com.cryptosignalai.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cryptosignalai.data.repository.SignalRepository
import com.cryptosignalai.domain.pipeline.SignalPipeline
import com.cryptosignalai.notification.NotificationHelper
import com.cryptosignalai.security.SecureStorage

/**
 * Periodic background check (every 15 min via WorkManager). Runs the full
 * validation pipeline for the user's currently selected token/timeframe and
 * fires a notification only on a strong, validated, high-confidence signal.
 *
 * Continues to run when the app is closed (WorkManager survives process death).
 */
class SignalWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val storage = SecureStorage.get(applicationContext)
        if (!storage.backgroundEnabled) return Result.success()

        val providerType = storage.selectedProvider
        val key = storage.getApiKey(providerType) ?: return Result.success()

        return try {
            val pipeline = SignalPipeline()
            val outcome = pipeline.run(
                storage.selectedToken, storage.selectedTimeframe, providerType, key
            )
            val final = outcome.final

            runCatching { SignalRepository(applicationContext).saveSignal(final) }

            if (storage.notificationsEnabled &&
                (final.decision == "BUY" || final.decision == "SELL") &&
                final.confidence >= NotificationHelper.MIN_CONFIDENCE
            ) {
                NotificationHelper(applicationContext).notifySignal(final)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
