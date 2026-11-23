package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.Di319Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface Di319Dao {
    @Query("SELECT * FROM di319_rows")
    fun getAllDi319Rows(): Flow<List<Di319Entity>>

    @Query("SELECT * FROM di319_rows")
    suspend fun getAllDi319RowsOnce(): List<Di319Entity>

    @Query("SELECT COUNT(*) FROM di319_rows")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<Di319Entity>)

    @Query("DELETE FROM di319_rows")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(rows: List<Di319Entity>) {
        deleteAll()
        insertAll(rows)
    }
}
