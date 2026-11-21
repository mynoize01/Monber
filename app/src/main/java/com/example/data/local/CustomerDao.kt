package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE menuType = :menuType")
    fun getCustomersByMenu(menuType: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE menuType IN ('INTRAKOMP', 'EKSTRAKOMP')")
    fun getAllIntraAndEkstraCustomers(): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Query("DELETE FROM customers WHERE menuType = :menuType")
    suspend fun deleteAllByMenu(menuType: String)

    @Query("SELECT COUNT(*) FROM customers WHERE menuType = :menuType")
    suspend fun getCustomersCount(menuType: String): Int

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun getTotalCustomersCount(): Int

    @Transaction
    suspend fun refreshCustomers(menuType: String, customers: List<CustomerEntity>) {
        deleteAllByMenu(menuType)
        insertAll(customers)
    }
}
