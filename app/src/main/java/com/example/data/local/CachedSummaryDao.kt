package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CachedSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedSummaryDao {
    @Query("SELECT * FROM cached_summaries WHERE cacheKey = :cacheKey LIMIT 1")
    fun observeSummary(cacheKey: String): Flow<CachedSummaryEntity?>

    @Query("SELECT * FROM cached_summaries WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getSummaryOnce(cacheKey: String): CachedSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedSummaryEntity)

    @Query("DELETE FROM cached_summaries")
    suspend fun clearAll()
}
