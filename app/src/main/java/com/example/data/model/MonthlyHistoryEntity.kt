package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "monthly_history",
    indices = [
        Index(value = ["month", "accountKey"]),
        Index(value = ["accountKey"])
    ]
)
data class MonthlyHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: Int,
    val accountKey: String,
    val kolek: String,
    val tunggakan: Double? = null
)
