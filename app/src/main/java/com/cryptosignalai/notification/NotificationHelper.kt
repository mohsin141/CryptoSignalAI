package com.cryptosignalai.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cryptosignalai.MainActivity
import com.cryptosignalai.R
import com.cryptosignalai.data.model.FinalSignal

/**
 * Local-only notifications. We deliberately notify ONLY for strong, validated
 * BUY/SELL signals (and trend reversals) at confidence >= 80% to avoid spam.
 *
 * Example:  BTC BUY  Confidence 87%  Entry 104200  SL 103500  TP 107000
 */
class NotificationHelper(private val context: Context) {

    init { ensureChannel() }

    fun notifySignal(signal: FinalSignal) {
        if (signal.decision != "BUY" && signal.decision != "SELL") return
        if (signal.confidence < MIN_CONFIDENCE) return

        val title = "${signal.token.name} ${signal.decision} • ${signal.confidence}%"
        val body = "Entry ${fmt(signal.entry)}  SL ${fmt(signal.stopLoss)}  TP ${fmt(signal.takeProfit)}  • Risk ${signal.risk}"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            context, signal.token.ordinal, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body + "\n" + signal.reason))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        NotificationManagerCompat.from(context)
            .notify(NOTIF_ID_BASE + signal.token.ordinal, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_signals),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_signals_desc)
                }
                mgr.createNotificationChannel(channel)
            }
        }
    }

    private fun fmt(v: Double): String = when {
        v == 0.0 -> "—"
        v >= 100 -> String.format("%,.0f", v)
        v >= 1 -> String.format("%.3f", v)
        else -> String.format("%.5f", v)
    }

    companion object {
        const val MIN_CONFIDENCE = 80
        private const val CHANNEL_ID = "signals_channel"
        private const val NOTIF_ID_BASE = 1000
    }
}
