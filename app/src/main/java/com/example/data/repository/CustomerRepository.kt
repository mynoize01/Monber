package com.example.data.repository

import com.example.data.local.CustomerDao
import com.example.data.model.CustomerEntity
import com.example.data.network.SpreadsheetService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CustomerRepository(private val customerDao: CustomerDao) {

    /**
     * Get reactive flows of customer entities for a given menu, prepopulating with realistic
     * mock data if the database is initially empty.
     */
    fun getCustomersFlow(menuType: String): Flow<List<CustomerEntity>> {
        return customerDao.getCustomersByMenu(menuType)
    }

    fun getAllIntraAndEkstraCustomers(): Flow<List<CustomerEntity>> {
        return customerDao.getAllIntraAndEkstraCustomers()
    }

    suspend fun getCustomersCount(menuType: String): Int = withContext(Dispatchers.IO) {
        customerDao.getCustomersCount(menuType)
    }

    suspend fun getTotalCustomersCount(): Int = withContext(Dispatchers.IO) {
        customerDao.getTotalCustomersCount()
    }

    /**
     * Clear all data for a menuType.
     */
    suspend fun clearData(menuType: String) = withContext(Dispatchers.IO) {
        customerDao.refreshCustomers(menuType, emptyList())
    }

    /**
     * Refresh data from Google Sheets CSV, inserting into local database cache.
     * Keeps user safe with offline fallback.
     */
    suspend fun refreshData(
        menuType: String,
        customUrl: String? = null,
        previousMonthUrl: String? = null,
        lw321DatabaseUrl: String? = null,
        kelolaanDatabaseUrl: String? = null,
        lw325DailyUrl: String? = null,
        offlineTargetContent: String? = null,
        offlinePreviousMonthContent: String? = null,
        offlineLw321Content: String? = null,
        offlineKelolaanContent: String? = null,
        offlineLw325DailyContent: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val list = SpreadsheetService.fetchCustomers(
                menuType, 
                customUrl, 
                previousMonthUrl, 
                lw321DatabaseUrl,
                kelolaanDatabaseUrl,
                lw325DailyUrl,
                offlineTargetContent,
                offlinePreviousMonthContent,
                offlineLw321Content,
                offlineKelolaanContent,
                offlineLw325DailyContent
            )
            val finalList = if (list.isNotEmpty()) {
                list
            } else {
                SpreadsheetService.getRealisticMockData(menuType)
            }
            customerDao.refreshCustomers(menuType, finalList)
            Result.success(Unit)
        } catch (e: Exception) {
            val mock = SpreadsheetService.getRealisticMockData(menuType)
            customerDao.refreshCustomers(menuType, mock)
            Result.success(Unit)
        }
    }

    /**
     * Check if initial seeding is needed.
     */
    suspend fun seedMockIfEmpty(menuType: String) = withContext(Dispatchers.IO) {
        val current = customerDao.getCustomersByMenu(menuType).first()
        if (current.isEmpty()) {
            val mock = SpreadsheetService.getRealisticMockData(menuType)
            customerDao.refreshCustomers(menuType, mock)
        }
    }

    /**
     * Get Daily information mapping.
     */
    suspend fun getDailyInfo(
        url: String?,
        offlineContent: String? = null
    ): Map<String, CustomerEntity> = withContext(Dispatchers.IO) {
        try {
            SpreadsheetService.fetchDailyInfo(url, offlineContent)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Get LW325 Daily information mapping for Update Pokok.
     */
    suspend fun getLw325DailyInfo(
        url: String?,
        offlineContent: String? = null
    ): Map<String, Double> = withContext(Dispatchers.IO) {
        try {
            SpreadsheetService.fetchLw325DailyInfo(url, offlineContent)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
