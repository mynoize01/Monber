package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.CustomerDao
import com.example.data.local.Di319Dao
import com.example.data.local.SyncMetadataDao
import com.example.data.model.CustomerEntity
import com.example.data.model.Di319Row
import com.example.data.model.SyncMetadataEntity
import com.example.data.model.toEntity
import com.example.data.network.SpreadsheetService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val di319Dao: Di319Dao? = null,
    private val syncMetadataDao: SyncMetadataDao? = null
) {

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Reactive Room Flow of customer entities for a given menu.
     */
    fun getCustomersFlow(menuType: String): Flow<List<CustomerEntity>> {
        return customerDao.getCustomersByMenu(menuType)
    }

    fun getAllIntraAndEkstraCustomers(): Flow<List<CustomerEntity>> {
        return customerDao.getAllIntraAndEkstraCustomers()
    }

    fun getDi319Flow(): Flow<List<Di319Row>> {
        return di319Dao?.getAllDi319Rows()?.map { entities ->
            entities.map { it.toDi319Row() }
        } ?: emptyFlow()
    }

    fun observeLastSyncMetadata(): Flow<SyncMetadataEntity?> {
        return syncMetadataDao?.observeMetadata("GLOBAL_SYNC") ?: emptyFlow()
    }

    suspend fun getLastSyncMetadata(): SyncMetadataEntity? = withContext(Dispatchers.IO) {
        syncMetadataDao?.getMetadata("GLOBAL_SYNC")
    }

    suspend fun getCustomersCount(menuType: String): Int = withContext(Dispatchers.IO) {
        customerDao.getCustomersCount(menuType)
    }

    suspend fun getTotalCustomersCount(): Int = withContext(Dispatchers.IO) {
        customerDao.getCustomersCount("INTRAKOMP") + customerDao.getCustomersCount("EKSTRAKOMP")
    }

    suspend fun getCachedPeriodCustomers(periodMenuType: String): List<CustomerEntity> =
        withContext(Dispatchers.IO) {
            customerDao.getCustomersByMenuOnce(periodMenuType)
        }

    suspend fun savePeriodCustomers(
        periodMenuType: String,
        customers: List<CustomerEntity>
    ) = withContext(Dispatchers.IO) {
        if (customers.isEmpty()) return@withContext
        val tagged = customers.map { it.copy(id = 0, menuType = periodMenuType) }
        customerDao.refreshCustomers(periodMenuType, tagged)
        syncMetadataDao?.upsert(
            SyncMetadataEntity(
                cacheKey = periodMenuType,
                lastSyncedAt = System.currentTimeMillis(),
                rowCount = tagged.size,
                label = periodMenuType
            )
        )
    }

    suspend fun saveDi319Rows(rows: List<Di319Row>) = withContext(Dispatchers.IO) {
        if (rows.isEmpty()) return@withContext
        val dao = di319Dao ?: return@withContext
        dao.replaceAll(rows.map { it.toEntity() })
        val now = System.currentTimeMillis()
        syncMetadataDao?.upsert(
            SyncMetadataEntity(
                cacheKey = "SIMPANAN",
                lastSyncedAt = now,
                rowCount = rows.size,
                label = "DI319"
            )
        )
        syncMetadataDao?.upsert(
            SyncMetadataEntity(
                cacheKey = "GLOBAL_SYNC",
                lastSyncedAt = now,
                rowCount = rows.size,
                label = "DI319"
            )
        )
    }

    suspend fun getCachedDi319RowsOnce(): List<Di319Row> = withContext(Dispatchers.IO) {
        di319Dao?.getAllDi319RowsOnce()?.map { it.toDi319Row() } ?: emptyList()
    }

    suspend fun updateGlobalSyncMetadata(rowCount: Int, label: String) = withContext(Dispatchers.IO) {
        syncMetadataDao?.upsert(
            SyncMetadataEntity(
                cacheKey = "GLOBAL_SYNC",
                lastSyncedAt = System.currentTimeMillis(),
                rowCount = rowCount,
                label = label
            )
        )
    }

    /**
     * Clear all data for a menuType.
     */
    suspend fun clearData(menuType: String) = withContext(Dispatchers.IO) {
        customerDao.refreshCustomers(menuType, emptyList())
    }

    /**
     * Refresh data from Google Sheets CSV, saving non-empty results into Room cache.
     * Preserves existing Room cached records whenever offline or network fails.
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
            if (list.isNotEmpty()) {
                val normalizedList = list.map { it.copy(id = 0, menuType = menuType) }
                customerDao.refreshCustomers(menuType, normalizedList)
                val now = System.currentTimeMillis()
                syncMetadataDao?.upsert(
                    SyncMetadataEntity(
                        cacheKey = menuType,
                        lastSyncedAt = now,
                        rowCount = normalizedList.size,
                        label = menuType
                    )
                )
                syncMetadataDao?.upsert(
                    SyncMetadataEntity(
                        cacheKey = "GLOBAL_SYNC",
                        lastSyncedAt = now,
                        rowCount = normalizedList.size,
                        label = menuType
                    )
                )
                Result.success(Unit)
            } else {
                val cachedCount = customerDao.getCustomersCount(menuType)
                if (cachedCount > 0) {
                    Result.failure(
                        IllegalStateException("Koneksi internet terputus atau data kosong. Menampilkan $cachedCount data tersimpan.")
                    )
                } else {
                    Result.failure(
                        IllegalStateException("Tidak dapat memuat data dari server dan belum ada data cache tersimpan.")
                    )
                }
            }
        } catch (e: Exception) {
            val cachedCount = customerDao.getCustomersCount(menuType)
            if (cachedCount > 0) {
                Result.failure(
                    IllegalStateException("Koneksi terputus (${e.localizedMessage ?: "offline"}). Menampilkan $cachedCount data cache terakhir.")
                )
            } else {
                Result.failure(e)
            }
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
