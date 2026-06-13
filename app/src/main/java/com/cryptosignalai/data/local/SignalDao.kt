package com.cryptosignalai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalDao {

    @Insert
    suspend fun insertSignal(signal: SignalEntity): Long

    @Query("SELECT * FROM signals ORDER BY createdAt DESC LIMIT :limit")
    fun observeSignals(limit: Int = 200): Flow<List<SignalEntity>>

    @Query("SELECT * FROM signals WHERE token = :token ORDER BY createdAt DESC LIMIT :limit")
    suspend fun signalsForToken(token: String, limit: Int = 50): List<SignalEntity>

    @Query("SELECT * FROM signals ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestSignal(): SignalEntity?

    @Insert
    suspend fun insertBacktestLog(log: BacktestLogEntity): Long

    @Query("SELECT * FROM backtest_logs ORDER BY createdAt DESC LIMIT :limit")
    fun observeBacktestLogs(limit: Int = 200): Flow<List<BacktestLogEntity>>

    @Insert
    suspend fun insertTradeResult(result: TradeResultEntity): Long

    @Query("SELECT * FROM trade_results ORDER BY closedAt DESC")
    fun observeTradeResults(): Flow<List<TradeResultEntity>>

    @Query("DELETE FROM signals")
    suspend fun clearSignals()
}
