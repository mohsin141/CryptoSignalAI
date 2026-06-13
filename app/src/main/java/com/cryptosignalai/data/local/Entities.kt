package com.cryptosignalai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Persisted final signal (signal history). */
@Entity(tableName = "signals")
data class SignalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val token: String,
    val timeframe: String,
    val decision: String,
    val confidence: Int,
    val entry: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val risk: String,
    val reason: String,
    val backtestWinRate: Double,
    val backtestSamples: Int,
    val createdAt: Long
)

/** Persisted backtest run log. */
@Entity(tableName = "backtest_logs")
data class BacktestLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val token: String,
    val timeframe: String,
    val bias: String,
    val winRate: Double,
    val samples: Int,
    val avgReturn: Double,
    val approved: Boolean,
    val createdAt: Long
)

/** Persisted (optional, user-recorded) trade outcome for performance tracking. */
@Entity(tableName = "trade_results")
data class TradeResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val signalId: Long,
    val token: String,
    val decision: String,
    val entry: Double,
    val exit: Double,
    val pnlPercent: Double,
    val win: Boolean,
    val closedAt: Long
)
