package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.data.local.CachedSummaryDao
import com.example.data.local.CustomerDao
import com.example.data.local.Di319Dao
import com.example.data.local.MonthlyHistoryDao
import com.example.data.local.SyncMetadataDao
import com.example.data.model.CachedSummaryEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.Di319Row
import com.example.data.model.MonthlyHistoryEntity
import com.example.data.model.SyncMetadataEntity
import com.example.data.model.toEntity
import com.example.data.network.SpreadsheetService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val di319Dao: Di319Dao? = null,
    private val syncMetadataDao: SyncMetadataDao? = null,
    private val cachedSummaryDao: CachedSummaryDao? = null,
    private val monthlyHistoryDao: MonthlyHistoryDao? = null
) {

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun observeNetworkConnectivity(context: Context): Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(isNetworkAvailable(context))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                trySend(hasInternet)
            }
        }

        trySend(isNetworkAvailable(context))
        val registered = runCatching {
            cm.registerDefaultNetworkCallback(callback)
            true
        }.getOrDefault(false)

        awaitClose {
            if (registered) {
                runCatching { cm.unregisterNetworkCallback(callback) }
            }
        }
    }.distinctUntilChanged()

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

    suspend fun getSyncMetadataForKey(cacheKey: String): SyncMetadataEntity? = withContext(Dispatchers.IO) {
        syncMetadataDao?.getMetadata(cacheKey)
    }

    suspend fun saveSummaryJson(cacheKey: String, payloadJson: String) = withContext(Dispatchers.IO) {
        val dao = cachedSummaryDao ?: return@withContext
        if (payloadJson.isBlank()) return@withContext
        dao.upsert(
            CachedSummaryEntity(
                cacheKey = cacheKey,
                payloadJson = payloadJson,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getSummaryJsonOnce(cacheKey: String): String? = withContext(Dispatchers.IO) {
        cachedSummaryDao?.getSummaryOnce(cacheKey)?.payloadJson
    }

    fun observeSummaryJson(cacheKey: String): Flow<String?> {
        return cachedSummaryDao?.observeSummary(cacheKey)?.map { it?.payloadJson } ?: emptyFlow()
    }

    suspend fun saveMonthlyHistory(
        month: Int,
        kolekMap: Map<String, String>,
        tunggakanMap: Map<String, Double>
    ) = withContext(Dispatchers.IO) {
        val dao = monthlyHistoryDao ?: return@withContext
        if (month !in 1..12 || (kolekMap.isEmpty() && tunggakanMap.isEmpty())) return@withContext

        val allKeys = LinkedHashSet<String>(kolekMap.size + tunggakanMap.size)
        allKeys.addAll(kolekMap.keys)
        allKeys.addAll(tunggakanMap.keys)

        val entities = allKeys.mapNotNull { key ->
            if (key.isBlank()) return@mapNotNull null
            MonthlyHistoryEntity(
                month = month,
                accountKey = key,
                kolek = kolekMap[key].orEmpty(),
                tunggakan = tunggakanMap[key]
            )
        }
        if (entities.isEmpty()) return@withContext

        dao.replaceMonth(month, entities)
        syncMetadataDao?.upsert(
            SyncMetadataEntity(
                cacheKey = "MONTHLY_HISTORY_$month",
                lastSyncedAt = System.currentTimeMillis(),
                rowCount = entities.size,
                label = "LW321_M_$month"
            )
        )
    }

    suspend fun getMonthlyHistoryMapsOnce(month: Int): Pair<Map<String, String>, Map<String, Double>> =
        withContext(Dispatchers.IO) {
            val dao = monthlyHistoryDao ?: return@withContext (emptyMap<String, String>() to emptyMap<String, Double>())
            if (month !in 1..12) return@withContext (emptyMap<String, String>() to emptyMap<String, Double>())
            val rows = dao.getHistoryByMonthOnce(month)
            if (rows.isEmpty()) return@withContext (emptyMap<String, String>() to emptyMap<String, Double>())
            val kolekMap = HashMap<String, String>(rows.size)
            val tgkMap = HashMap<String, Double>(rows.size)
            rows.forEach { row ->
                if (row.kolek.isNotBlank()) {
                    kolekMap[row.accountKey] = row.kolek
                }
                if (row.tunggakan != null) {
                    tgkMap[row.accountKey] = row.tunggakan
                }
            }
            kolekMap to tgkMap
        }

    suspend fun getCustomerMonthlyHistoryOnce(
        keys: List<String>
    ): Pair<Map<Int, String>, Map<Int, Double?>> = withContext(Dispatchers.IO) {
        val dao = monthlyHistoryDao ?: return@withContext (emptyMap<Int, String>() to emptyMap<Int, Double?>())
        val cleanedKeys = keys.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (cleanedKeys.isEmpty()) return@withContext (emptyMap<Int, String>() to emptyMap<Int, Double?>())

        val rows = dao.getHistoryForAccountKeysOnce(cleanedKeys)
        if (rows.isEmpty()) return@withContext (emptyMap<Int, String>() to emptyMap<Int, Double?>())

        val byMonth = rows.groupBy { it.month }
        val kolekResult = HashMap<Int, String>()
        val tgkResult = HashMap<Int, Double?>()

        byMonth.forEach { (month, monthRows) ->
            for (k in cleanedKeys) {
                val match = monthRows.firstOrNull { it.accountKey == k }
                if (match != null) {
                    if (match.kolek.isNotBlank() && !kolekResult.containsKey(month)) {
                        kolekResult[month] = match.kolek
                    }
                    if (match.tunggakan != null && !tgkResult.containsKey(month)) {
                        tgkResult[month] = match.tunggakan
                    }
                }
            }
        }
        kolekResult to tgkResult
    }

    suspend fun getCachedHistoryMonthsOnce(): Set<Int> = withContext(Dispatchers.IO) {
        monthlyHistoryDao?.getCachedMonthsOnce()?.toSet() ?: emptySet()
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

    suspend fun copyPeriodToMenu(
        periodMenuType: String,
        targetMenuType: String = "INTRAKOMP"
    ): Boolean = withContext(Dispatchers.IO) {
        val cachedPeriod = customerDao.getCustomersByMenuOnce(periodMenuType)
        if (cachedPeriod.isEmpty()) return@withContext false

        val existingTarget = customerDao.getCustomersByMenuOnce(targetMenuType)
        val byRek = HashMap<String, CustomerEntity>(existingTarget.size)
        val byCif = HashMap<String, CustomerEntity>(existingTarget.size)
        existingTarget.forEach { item ->
            val rekKey = item.noRekening.filter { it.isDigit() }.ifBlank { item.noRekening.trim() }
            if (rekKey.isNotBlank()) byRek[rekKey] = item
            val cifKey = item.cif.trim().uppercase()
            if (cifKey.isNotBlank()) byCif[cifKey] = item
        }

        val normalized = cachedPeriod.map { item ->
            val rekKey = item.noRekening.filter { it.isDigit() }.ifBlank { item.noRekening.trim() }
            val cifKey = item.cif.trim().uppercase()
            val match = byRek[rekKey] ?: byCif[cifKey]
            item.copy(
                id = 0,
                menuType = targetMenuType,
                alamat = item.alamat.ifBlank { match?.alamat ?: "" },
                kolekBlnLalu = item.kolekBlnLalu.ifBlank { match?.kolekBlnLalu ?: "" },
                extraColumns = if (item.extraColumns.contains("Desa::") || item.extraColumns.contains("No HP")) {
                    item.extraColumns
                } else {
                    match?.extraColumns?.takeIf { it.isNotBlank() } ?: item.extraColumns
                }
            )
        }
        customerDao.refreshCustomers(targetMenuType, normalized)
        true
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

    /**
     * Get LW325 Daily parsed customers list (for Dashboard Nasabah DH New).
     */
    suspend fun getLw325DailyCustomers(
        url: String?,
        offlineContent: String? = null,
        lw321DatabaseUrl: String? = null,
        offlineLw321Content: String? = null
    ): List<CustomerEntity> = withContext(Dispatchers.IO) {
        try {
            SpreadsheetService.fetchLw325DailyCustomers(
                url = url,
                offlineContent = offlineContent,
                lw321DatabaseUrl = lw321DatabaseUrl,
                offlineLw321Content = offlineLw321Content
            )
        } catch (e: Exception) {
            emptyList()
        }
    }
}
