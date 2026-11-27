package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.MonthlyHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyHistoryDao {
    @Query("SELECT * FROM monthly_history WHERE month = :month")
    fun observeHistoryByMonth(month: Int): Flow<List<MonthlyHistoryEntity>>

    @Query("SELECT * FROM monthly_history WHERE month = :month")
    suspend fun getHistoryByMonthOnce(month: Int): List<MonthlyHistoryEntity>

    @Query("SELECT * FROM monthly_history WHERE accountKey IN (:keys)")
    fun observeHistoryForAccountKeys(keys: List<String>): Flow<List<MonthlyHistoryEntity>>

    @Query("SELECT * FROM monthly_history WHERE accountKey IN (:keys)")
    suspend fun getHistoryForAccountKeysOnce(keys: List<String>): List<MonthlyHistoryEntity>

    @Query("SELECT DISTINCT month FROM monthly_history")
    suspend fun getCachedMonthsOnce(): List<Int>

    @Query("DELETE FROM monthly_history WHERE month = :month")
    suspend fun deleteMonth(month: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<MonthlyHistoryEntity>)

    @Transaction
    suspend fun replaceMonth(month: Int, rows: List<MonthlyHistoryEntity>) {
        deleteMonth(month)
        if (rows.isNotEmpty()) {
            insertAll(rows)
        }
    }
}
