package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE cacheKey = :cacheKey LIMIT 1")
    fun observeMetadata(cacheKey: String): Flow<SyncMetadataEntity?>

    @Query("SELECT * FROM sync_metadata WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getMetadata(cacheKey: String): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SyncMetadataEntity)
}
