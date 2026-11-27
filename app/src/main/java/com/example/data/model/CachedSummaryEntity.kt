package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_summaries")
data class CachedSummaryEntity(
    @PrimaryKey val cacheKey: String,
    val payloadJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)
