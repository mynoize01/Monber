package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val cacheKey: String,
    val lastSyncedAt: Long = 0L,
    val rowCount: Int = 0,
    val label: String = ""
)
