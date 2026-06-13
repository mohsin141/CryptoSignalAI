package com.cryptosignalai.data.repository

import android.content.Context
import com.cryptosignalai.data.local.AppDatabase
import com.cryptosignalai.data.local.BacktestLogEntity
import com.cryptosignalai.data.local.SignalEntity
import com.cryptosignalai.data.local.TradeResultEntity
import com.cryptosignalai.data.model.BacktestResult
import com.cryptosignalai.data.model.FinalSignal
import kotlinx.coroutines.flow.Flow

/** Persistence wrapper for signal history, backtest logs and trade results. */
class SignalRepository(context: Context) {

    private val dao = AppDatabase.get(context).signalDao()

    suspend fun saveSignal(signal: FinalSignal): Long = dao.insertSignal(
        SignalEntity(
            token = signal.token.name,
            timeframe = signal.timeframe.label,
            decision = signal.decision,
            confidence = signal.confidence,
            entry = signal.entry,
            stopLoss = signal.stopLoss,
            takeProfit = signal.takeProfit,
            risk = signal.risk,
            reason = signal.reason,
            backtestWinRate = signal.backtestWinRate,
            backtestSamples = signal.backtestSamples,
            createdAt = signal.createdAt
        )
    )

    suspend fun saveBacktestLog(token: String, timeframe: String, bias: String, r: BacktestResult) {
        dao.insertBacktestLog(
            BacktestLogEntity(
                token = token,
                timeframe = timeframe,
                bias = bias,
                winRate = r.winRate,
                samples = r.samples,
                avgReturn = r.avgReturn,
                approved = r.approved,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveTradeResult(result: TradeResultEntity): Long = dao.insertTradeResult(result)

    fun observeSignals(): Flow<List<SignalEntity>> = dao.observeSignals()
    fun observeBacktestLogs(): Flow<List<BacktestLogEntity>> = dao.observeBacktestLogs()
    fun observeTradeResults(): Flow<List<TradeResultEntity>> = dao.observeTradeResults()
    suspend fun latestSignal(): SignalEntity? = dao.latestSignal()
    suspend fun clearSignals() = dao.clearSignals()
}
