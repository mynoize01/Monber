package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CustomerEntity
import com.example.data.model.desa
import com.example.data.model.mapEkstrakompSegmen
import com.example.data.model.SavingsAccount
import com.example.data.model.SimpananRow
import com.example.data.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.example.data.network.SpreadsheetService
import com.example.data.local.LocalSummaryCache
import com.example.update.AppUpdateInstallReceiver
import com.example.update.AppUpdateManager
import com.example.update.AppUpdateState
import com.example.update.GitHubReleaseInfo
import com.example.update.PackageInstallEvent

enum class SortColumn {
    NAMA_NASABAH, PLAFOND, OS_PINJAMAN, UPDATE_POKOK, SETOR_POKOK, SEGMEN, KOLEK, UMUR_TGK, KOL_BY_LW, NAMA_MANTRI, NPD, ALAMAT, DESA, ALIH_TAGIH, SISA_BUNGA, TOTAL_DH
}

enum class SortDirection {
    ASC, DESC
}

data class UnitSummaryData(
    val os: Double = 0.0,
    val debtors: Int = 0
)

data class KolekSummaryData(
    val lancar: UnitSummaryData = UnitSummaryData(),
    val sml: UnitSummaryData = UnitSummaryData(),
    val npl: UnitSummaryData = UnitSummaryData(),
    val lainnya: UnitSummaryData = UnitSummaryData()
)

data class MantriDetailData(
    val name: String = "",
    val total: UnitSummaryData = UnitSummaryData(),
    val lancar: UnitSummaryData = UnitSummaryData(),
    val sml: UnitSummaryData = UnitSummaryData(),
    val npl: UnitSummaryData = UnitSummaryData()
)

data class ToggleFilters(
    val sp: Boolean,
    val tus: Boolean,
    val tuc: Boolean,
    val jt: Boolean,
    val jtH: Int = 0
)

data class CustomerListMetadata(
    val loggedInPN: String,
    val selectedMenu: String,
    val kunjunganKeys: Set<String>,
    val activeVisitDate: String
)

data class FilterParams(
    val search: String,
    val kolek: String,
    val mantri: String,
    val bulan: String,
    val tahun: String,
    val osVal: String,
    val plVal: String,
    val tnVal: String,
    val segmen: String = "SEMUA",
    val kolekUpdate: String = "SEMUA",
    val kolekBlnLalu: String = "SEMUA",
    val npd: String = "SEMUA",
    val alamat: String = "",
    val setorPokok: Boolean = false,
    val tusim: Boolean = false,
    val tucil: Boolean = false,
    val jatuhTempoOnly: Boolean = false,
    val jatuhTempoH: Int = 0,
    val desa: String = "SEMUA"
)

const val DEFAULT_WA_DETAIL_TEMPLATE = """Selamat siang Bapak/Ibu *{nama},*
 
Terima kasih atas kepercayaan dan loyalitas Bapak/Ibu telah menggunakan produk BRI. 

Bapak/Ibu, kami ingin mengingatkan kembali bahwa kewajiban pembayaran pinjaman di BRI saat ini telah mengalami keterlambatan {keterlambatan} bulan. 
*Total Tunggakan:* *Rp.* *{total_tunggakan}* 
*Jatuh Tempo      :* *{jatuh_tempo}* 

Kami mohon Bapak/Ibu dapat segera melakukan pembayaran atau berkenan menghubungi kami untuk membahas penyelesaian kewajiban tersebut. Perlu kami sampaikan bahwa apabila kewajiban tersebut belum dapat diselesaikan, maka penyelesaian dapat ditempuh melalui jalur hukum sesuai dengan ketentuan yang berlaku. 

Untuk memberikan kemudahan bagi Bapak/Ibu, pembayaran angsuran pinjaman dapat dilakukan melalui BRImo, mesin ATM/CRM atau Agen BRILink terdekat. 

Apabila terdapat kendala, kami dengan senang hati siap membantu Bapak/Ibu untuk mencari solusi terbaik. 

_Terima kasih Bapak/Ibu atas perhatian dan kerjasamanya. Semoga Bapak/Ibu senantiasa diberikan kesehatan dan kesuksesan dalam mengembangkan usahanya._ 

*{unit}*
_*Mantri - {mantri}*_"""

private const val LEGACY_DEFAULT_WA_RENCANA_TEMPLATE = """{judul} mantri {mantri}
Tgl {tanggal}

SML3
{daftar_sml3}

NPL
{daftar_npl}"""

const val DEFAULT_WA_RENCANA_TEMPLATE = """{judul} mantri {mantri}
Tgl {tanggal}

{daftar_kolek}"""

class CustomerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = CustomerRepository(
        database.customerDao(),
        database.di319Dao(),
        database.syncMetadataDao()
    )

    private val sharedPrefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    val customSpreadsheetUrl = MutableStateFlow(sharedPrefs.getString("custom_spreadsheet_url", "") ?: "")
    
    val selectedPeriodSource = MutableStateFlow("AKHIR_BULAN") // "BULAN_LALU", "AKHIR_BULAN", "DAILY"
    val periodDateBulanLalu = MutableStateFlow(sharedPrefs.getString("period_date_bulan_lalu", "") ?: "")
    val periodDateAkhirBulan = MutableStateFlow(sharedPrefs.getString("period_date_akhir_bulan", "") ?: "")
    val periodDateDaily = MutableStateFlow(sharedPrefs.getString("period_date_daily", "") ?: "")
    val periodDateLw325 = MutableStateFlow(sharedPrefs.getString("period_date_lw325", "") ?: "")
    
    // Intrakomp category URLs
    val url1 = MutableStateFlow(sharedPrefs.getString("url_1", "") ?: "")
    val url2 = MutableStateFlow(sharedPrefs.getString("url_2", "") ?: "")
    val url3 = MutableStateFlow(sharedPrefs.getString("url_3", "") ?: "")
    val url4 = MutableStateFlow(sharedPrefs.getString("url_4", "") ?: "")
    val urlLw321BlnLalu = MutableStateFlow(sharedPrefs.getString("url_lw321_bln_lalu", "") ?: "")
    val urlLw325 = MutableStateFlow(sharedPrefs.getString("url_lw325", "") ?: "")
    
    // Ekstracomp category URLs
    val eUrl1 = MutableStateFlow(sharedPrefs.getString("e_url_1", "") ?: "")
    val eUrl2 = MutableStateFlow(sharedPrefs.getString("e_url_2", "") ?: "")
    
    // Kelolaan category URLs
    val kUrl1 = MutableStateFlow(sharedPrefs.getString("k_url_1", "") ?: "")
    val di319Url = MutableStateFlow(sharedPrefs.getString("di319_url", "") ?: "")
    val lw325DailyUrl = MutableStateFlow(sharedPrefs.getString("lw325_daily_url", "") ?: "")
    val linkDatabaseUrl = MutableStateFlow(sharedPrefs.getString("link_database_url", sharedPrefs.getString("url_4", "") ?: "") ?: "")

    // LW321 All 12 Months URLs (1..12)
    val lw321AllUrls: Map<Int, MutableStateFlow<String>> = (1..12).associateWith { month ->
        MutableStateFlow(sharedPrefs.getString("lw321_all_$month", "") ?: "")
    }

    // 5 Bottom Nav Menus: "MONITORING", "KELOLAAN", "RENCANA", "DASHBOARD", "SETTING"
    val selectedBottomNav = MutableStateFlow(
        sharedPrefs.getString("last_selected_bottom_nav", "MONITORING") ?: "MONITORING"
    )
    
    val isOfflineModeEnabled = MutableStateFlow(sharedPrefs.getBoolean("is_offline_mode_enabled", false))
    val githubUpdateRepo = MutableStateFlow(
        sharedPrefs.getString("github_update_repo", "")
            ?.ifBlank { "https://github.com/mynoize01/Monber.git" }
            ?: "https://github.com/mynoize01/Monber.git"
    )
    val appUpdateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val showUpdateDialog = MutableStateFlow(false)
    private var updateJob: kotlinx.coroutines.Job? = null
    val offlineNameUrl1 = MutableStateFlow(sharedPrefs.getString("offline_name_url_1", "") ?: "")
    val offlineNameUrl2 = MutableStateFlow(sharedPrefs.getString("offline_name_url_2", "") ?: "")
    val offlineNameUrl3 = MutableStateFlow(sharedPrefs.getString("offline_name_url_3", "") ?: "")
    val offlineNameUrl4 = MutableStateFlow(sharedPrefs.getString("offline_name_url_4", "") ?: "")
    val offlineNameEUrl1 = MutableStateFlow(sharedPrefs.getString("offline_name_e_url_1", "") ?: "")
    val offlineNameEUrl2 = MutableStateFlow(sharedPrefs.getString("offline_name_e_url_2", "") ?: "")
    val offlineNameKUrl1 = MutableStateFlow(sharedPrefs.getString("offline_name_k_url_1", "") ?: "")
    val offlineNameDi319Url = MutableStateFlow(sharedPrefs.getString("offline_name_di319_url", "") ?: "")
    val offlineNameLw325Daily = MutableStateFlow(sharedPrefs.getString("offline_name_lw325_daily", "") ?: "")
    
    val templateWaDetailDebitur = MutableStateFlow(
        sharedPrefs.getString("template_wa_detail_debitur", DEFAULT_WA_DETAIL_TEMPLATE) ?: DEFAULT_WA_DETAIL_TEMPLATE
    )
    val templateWaRencana = MutableStateFlow(
        run {
            val saved = sharedPrefs.getString("template_wa_rencana", DEFAULT_WA_RENCANA_TEMPLATE) ?: DEFAULT_WA_RENCANA_TEMPLATE
            val isLegacySml3NplOnly = saved.contains("{daftar_sml3}") &&
                saved.contains("{daftar_npl}") &&
                !saved.contains("{daftar_kolek}") &&
                !saved.contains("{daftar_sml2}") &&
                !saved.contains("{daftar_semua}")
            if (saved.trim() == LEGACY_DEFAULT_WA_RENCANA_TEMPLATE.trim() || isLegacySml3NplOnly) {
                sharedPrefs.edit().putString("template_wa_rencana", DEFAULT_WA_RENCANA_TEMPLATE).apply()
                DEFAULT_WA_RENCANA_TEMPLATE
            } else {
                saved
            }
        }
    )

    fun saveTemplateWaDetail(template: String) {
        val finalVal = template.ifBlank { DEFAULT_WA_DETAIL_TEMPLATE }
        templateWaDetailDebitur.value = finalVal
        sharedPrefs.edit().putString("template_wa_detail_debitur", finalVal).apply()
    }

    fun saveTemplateWaRencana(template: String) {
        val finalVal = template.ifBlank { DEFAULT_WA_RENCANA_TEMPLATE }
        templateWaRencana.value = finalVal
        sharedPrefs.edit().putString("template_wa_rencana", finalVal).apply()
    }

    fun resetTemplateWaDetail() {
        saveTemplateWaDetail(DEFAULT_WA_DETAIL_TEMPLATE)
    }

    fun resetTemplateWaRencana() {
        saveTemplateWaRencana(DEFAULT_WA_RENCANA_TEMPLATE)
    }

    val includeOsInRencanaWa = MutableStateFlow(
        sharedPrefs.getBoolean("include_os_in_rencana_wa_v2", sharedPrefs.getBoolean("include_os_in_rencana_wa", true))
    )

    fun setIncludeOsInRencanaWa(include: Boolean) {
        includeOsInRencanaWa.value = include
        sharedPrefs.edit()
            .putBoolean("include_os_in_rencana_wa", include)
            .putBoolean("include_os_in_rencana_wa_v2", include)
            .apply()
    }

    val allIntraAndEkstraCustomers: StateFlow<List<com.example.data.model.CustomerEntity>> = repository.getAllIntraAndEkstraCustomers()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monitoringCustomers: StateFlow<List<com.example.data.model.CustomerEntity>> = repository.getCustomersFlow("INTRAKOMP")
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val dailyDataMap = MutableStateFlow<Map<String, com.example.data.model.CustomerEntity>>(emptyMap())
    val summaryBulanLalu = MutableStateFlow(UnitSummaryData())
    val summaryAkhirBulan = MutableStateFlow(UnitSummaryData())
    val summaryDaily = MutableStateFlow(UnitSummaryData())
    val kolekSummaryBulanLalu = MutableStateFlow(KolekSummaryData())
    val kolekSummaryAkhirBulan = MutableStateFlow(KolekSummaryData())
    val kolekSummaryDaily = MutableStateFlow(KolekSummaryData())
    val mantriSummaryBulanLalu = MutableStateFlow<Map<String, MantriDetailData>>(emptyMap())
    val mantriSummaryAkhirBulan = MutableStateFlow<Map<String, MantriDetailData>>(emptyMap())
    val mantriSummaryDaily = MutableStateFlow<Map<String, MantriDetailData>>(emptyMap())
    val di319Rows = MutableStateFlow<List<com.example.data.model.Di319Row>>(emptyList())
    val bulanLaluCustomers = MutableStateFlow<List<com.example.data.model.CustomerEntity>>(emptyList())
    val akhirBulanCustomers = MutableStateFlow<List<com.example.data.model.CustomerEntity>>(emptyList())
    val dailyCustomers = MutableStateFlow<List<com.example.data.model.CustomerEntity>>(emptyList())

    val lastSyncMetadata: StateFlow<com.example.data.model.SyncMetadataEntity?> =
        repository.observeLastSyncMetadata()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val isShowingCachedOfflineData = MutableStateFlow(false)
    
    // Dashboard Period and Summaries (Decoupled from Monitoring and Kelolaan)
    val dashboardPeriodSource = MutableStateFlow("DAILY")
    val dashboardMonthlySummary = MutableStateFlow<Map<Int, UnitSummaryData>>(emptyMap())
    val dashboardMonthlyKolekSummary = MutableStateFlow<Map<Int, KolekSummaryData>>(emptyMap())
    val dashboardMonthlyMantriSummary = MutableStateFlow<Map<Int, Map<String, MantriDetailData>>>(emptyMap())

    // Independent Filter Mantri per Menu
    val filterMantriDashboard = MutableStateFlow("SEMUA")

    // 12-Month Kolek & Tunggakan History for Customer Detail Dialog (from LW321 All Links)
    private val monthlyLw321KolekCache = mutableMapOf<Int, Map<String, String>>()
    private val monthlyLw321TunggakanCache = mutableMapOf<Int, Map<String, Double>>()
    val monthlyKolekHistory = MutableStateFlow<Map<Int, String>>(emptyMap())
    val monthlyTunggakanHistory = MutableStateFlow<Map<Int, Double?>>(emptyMap())
    val isMonthlyKolekLoading = MutableStateFlow(false)
    private var lastMonthlyHistoryCustomerKey: String = ""

    // Prior Month Kolek Map for Dashboard Kolek Downgrade & Upgrade (from LW321 All)
    val priorMonthLw321KolekMap = MutableStateFlow<Map<String, String>>(emptyMap())
    
    val loggedInPN = MutableStateFlow(sharedPrefs.getString("logged_in_pn", "") ?: "")
    val loggedInMantriDetails: StateFlow<String> = loggedInPN.flatMapLatest { pn ->
        repository.getCustomersFlow("INTRAKOMP").map { customers ->
            if (pn.isBlank()) ""
            else {
                val cleanPn = pn.trim()
                val rawDigits = Regex("""\d{4,9}""").find(cleanPn)?.value ?: cleanPn
                val digits = rawDigits.trimStart('0').ifEmpty { rawDigits }

                val invalidWords = setOf("MIKRO", "UNIT", "KREDIT", "KUR", "KUPEDES", "BRITAMA", "AO", "MANTRI", "PETUGAS", "PENGELOLA", "SINGLEPN", "STATUS", "AKTIF", "PASIF", "TIDAK TERIDENTIFIKASI")

                fun cleanName(n: String): String {
                    val c = n.replace(Regex("[0-9]"), "").replace(Regex("^[-_\\s]+"), "").trim()
                    if (c.isBlank()) return ""
                    val up = c.uppercase()
                    if (invalidWords.contains(up) || invalidWords.any { up.startsWith("$it ") || up.endsWith(" $it") }) return ""
                    return c.split(" ").filter { it.isNotBlank() }
                        .joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }
                }

                val match = customers.find { cust ->
                    val custPn = cust.pnMantri.trim()
                    val custDigits = custPn.filter { it.isDigit() }.trimStart('0')
                    (custDigits.isNotBlank() && custDigits == digits) ||
                    (cust.namaMantri.isNotBlank() && cleanName(cust.namaMantri).isNotBlank() && cust.namaMantri.contains(cleanPn, ignoreCase = true))
                }

                val foundName = if (match != null && cleanName(match.namaMantri).isNotBlank()) {
                    cleanName(match.namaMantri)
                } else if (digits == "207803" || cleanPn.contains("207803")) {
                    "Nurul Mubin"
                } else if (cleanPn.contains(" - ") && cleanName(cleanPn.substringAfter(" - ")).isNotBlank()) {
                    cleanName(cleanPn.substringAfter(" - "))
                } else {
                    "Nurul Mubin"
                }

                "$digits - $foundName"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun saveLogin(pn: String) {
        val trimmed = pn.trim()
        sharedPrefs.edit().putString("logged_in_pn", trimmed).apply()
        loggedInPN.value = trimmed
        // User requested: "perbaiki menu kelolaan ketika sudah di input PN maka data langsung tampil"
        refreshSpreadsheetData()
    }

    fun logoff() {
        sharedPrefs.edit().remove("logged_in_pn").apply()
        loggedInPN.value = ""
    }

    fun saveLink(key: String, url: String) {
        val trimmed = url.trim()
        sharedPrefs.edit().putString(key, trimmed).apply()
        when(key) {
            "url_1" -> {
                url1.value = trimmed
                urlLw321BlnLalu.value = trimmed
                sharedPrefs.edit().putString("url_lw321_bln_lalu", trimmed).apply()
            }
            "url_2" -> url2.value = trimmed
            "url_3" -> url3.value = trimmed
            "url_4" -> url4.value = trimmed
            "url_lw321_bln_lalu" -> {
                urlLw321BlnLalu.value = trimmed
                url1.value = trimmed
                sharedPrefs.edit().putString("url_1", trimmed).apply()
            }
            "url_lw325" -> {
                urlLw325.value = trimmed
                eUrl1.value = trimmed
                sharedPrefs.edit().putString("e_url_1", trimmed).apply()
            }
            "e_url_1" -> {
                eUrl1.value = trimmed
                urlLw325.value = trimmed
                sharedPrefs.edit().putString("url_lw325", trimmed).apply()
            }
            "e_url_2" -> eUrl2.value = trimmed
            "k_url_1" -> kUrl1.value = trimmed
            "di319_url" -> di319Url.value = trimmed
            "lw325_daily_url" -> {
                lw325DailyUrl.value = trimmed
            }
            "link_database_url" -> {
                linkDatabaseUrl.value = trimmed
                if (url4.value.isBlank()) {
                    url4.value = trimmed
                    sharedPrefs.edit().putString("url_4", trimmed).apply()
                }
            }
        }
        fetchPeriodDates()
    }

    fun saveLw321AllLink(month: Int, url: String) {
        val trimmed = url.trim()
        val prevUrl = lw321AllUrls[month]?.value ?: ""
        sharedPrefs.edit().putString("lw321_all_$month", trimmed).apply()
        lw321AllUrls[month]?.value = trimmed
        if (prevUrl != trimmed) {
            monthlyLw321KolekCache.remove(month)
            monthlyLw321TunggakanCache.remove(month)
        }
        if (selectedPeriodSource.value == "BULAN_$month" || filterBulanLunas.value == String.format("%02d", month)) {
            refreshSpreadsheetData()
        }
    }

    fun setBottomNav(menu: String) {
        selectedBottomNav.value = menu
        sharedPrefs.edit().putString("last_selected_bottom_nav", menu).apply()
        when (menu) {
            "MONITORING" -> {
                val current = selectedMenu.value
                if (current !in listOf("INTRAKOMP", "EKSTRAKOMP", "SIMPANAN")) {
                    selectMenu("INTRAKOMP")
                }
            }
            "KELOLAAN" -> {
                selectMenu("KELOLAAN")
            }
            "RENCANA" -> selectMenu("KUNJUNGAN")
            "DASHBOARD" -> {
                if (dashboardPeriodSource.value != "DAILY") {
                    selectDashboardPeriodSource("DAILY")
                } else if (summaryDaily.value.debtors == 0) {
                    loadSummaryForPeriod("DAILY")
                }
            }
        }
    }

    /**
     * Period source handler exclusively for Dashboard menu (Bulan Lalu, Akhir Bulan, Daily, 01 Jan - 12 Des).
     * Does NOT affect Monitoring or Kelolaan menus.
     */
    fun selectDashboardPeriodSource(source: String) {
        dashboardPeriodSource.value = source
        loadSummaryForPeriod(source)
    }

    fun loadSummaryForPeriod(source: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isOffline = isOfflineModeEnabled.value
            when {
                source.startsWith("BULAN_") -> {
                    val m = source.removePrefix("BULAN_").toIntOrNull()
                    if (m != null && m in 1..12) {
                        loadDashboardMonthlyData(m)
                    }
                }
                source == "BULAN_LALU" -> {
                    try {
                        val content = if (isOffline) getOfflineFileContent("url_1") else null
                        val url = if (!isOffline) (url1.value.ifBlank { urlLw321BlnLalu.value }) else null
                        val map = if (!url.isNullOrBlank() || !content.isNullOrBlank()) repository.getDailyInfo(url, content) else emptyMap()
                        val unique = getDistinctCustomers(map)
                        if (unique.isNotEmpty()) {
                            repository.savePeriodCustomers("PERIOD_BULAN_LALU", unique)
                            bulanLaluCustomers.value = unique
                            summaryBulanLalu.value = UnitSummaryData(
                                os = unique.sumOf { it.osPinjaman },
                                debtors = unique.size
                            )
                            kolekSummaryBulanLalu.value = calculateKolekSummary(unique)
                            mantriSummaryBulanLalu.value = calculateMantriSummaries(unique)
                        } else {
                            val cachedPeriod = repository.getCachedPeriodCustomers("PERIOD_BULAN_LALU")
                            val fallbackList = if (cachedPeriod.isNotEmpty()) {
                                cachedPeriod
                            } else {
                                val current = repository.getCustomersFlow("INTRAKOMP").first().ifEmpty {
                                    customersList.value.filter { it.menuType == "INTRAKOMP" }.ifEmpty { customersList.value }
                                }
                                current.map { cust ->
                                    val bLalu = cust.kolekBlnLalu.ifBlank { cust.kolek }
                                    cust.copy(kolek = bLalu)
                                }
                            }
                            if (fallbackList.isNotEmpty()) {
                                bulanLaluCustomers.value = fallbackList
                                summaryBulanLalu.value = UnitSummaryData(
                                    os = fallbackList.sumOf { it.osPinjaman },
                                    debtors = fallbackList.size
                                )
                                kolekSummaryBulanLalu.value = calculateKolekSummary(fallbackList)
                                mantriSummaryBulanLalu.value = calculateMantriSummaries(fallbackList)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CustomerViewModel", "Failed to load bln lalu summary", e)
                        val cachedPeriod = repository.getCachedPeriodCustomers("PERIOD_BULAN_LALU")
                        val fallbackList = if (cachedPeriod.isNotEmpty()) {
                            cachedPeriod
                        } else {
                            val current = customersList.value.filter { it.menuType == "INTRAKOMP" }.ifEmpty { customersList.value }
                            current.map { cust ->
                                val bLalu = cust.kolekBlnLalu.ifBlank { cust.kolek }
                                cust.copy(kolek = bLalu)
                            }
                        }
                        if (fallbackList.isNotEmpty()) {
                            bulanLaluCustomers.value = fallbackList
                            summaryBulanLalu.value = UnitSummaryData(
                                os = fallbackList.sumOf { it.osPinjaman },
                                debtors = fallbackList.size
                            )
                            kolekSummaryBulanLalu.value = calculateKolekSummary(fallbackList)
                            mantriSummaryBulanLalu.value = calculateMantriSummaries(fallbackList)
                        }
                    }
                }
                source == "AKHIR_BULAN" -> {
                    try {
                        if (bulanLaluCustomers.value.isEmpty()) {
                            loadSummaryForPeriod("BULAN_LALU")
                        }
                        val content = if (isOffline) getOfflineFileContent("url_2") else null
                        val url = if (!isOffline) url2.value else null
                        val map = if (!url.isNullOrBlank() || !content.isNullOrBlank()) repository.getDailyInfo(url, content) else emptyMap()
                        val unique = getDistinctCustomers(map)
                        if (unique.isNotEmpty()) {
                            repository.savePeriodCustomers("PERIOD_AKHIR_BULAN", unique)
                            akhirBulanCustomers.value = unique
                            summaryAkhirBulan.value = UnitSummaryData(
                                os = unique.sumOf { it.osPinjaman },
                                debtors = unique.size
                            )
                            kolekSummaryAkhirBulan.value = calculateKolekSummary(unique)
                            mantriSummaryAkhirBulan.value = calculateMantriSummaries(unique)
                        } else {
                            val cachedPeriod = repository.getCachedPeriodCustomers("PERIOD_AKHIR_BULAN")
                            val current = if (cachedPeriod.isNotEmpty()) {
                                cachedPeriod
                            } else {
                                repository.getCustomersFlow("INTRAKOMP").first().ifEmpty {
                                    customersList.value.filter { it.menuType == "INTRAKOMP" }.ifEmpty { customersList.value }
                                }
                            }
                            if (current.isNotEmpty()) {
                                akhirBulanCustomers.value = current
                                summaryAkhirBulan.value = UnitSummaryData(
                                    os = current.sumOf { it.osPinjaman },
                                    debtors = current.size
                                )
                                kolekSummaryAkhirBulan.value = calculateKolekSummary(current)
                                mantriSummaryAkhirBulan.value = calculateMantriSummaries(current)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CustomerViewModel", "Failed to load akhir bln summary", e)
                        val cachedPeriod = repository.getCachedPeriodCustomers("PERIOD_AKHIR_BULAN")
                        if (cachedPeriod.isNotEmpty()) {
                            akhirBulanCustomers.value = cachedPeriod
                            summaryAkhirBulan.value = UnitSummaryData(
                                os = cachedPeriod.sumOf { it.osPinjaman },
                                debtors = cachedPeriod.size
                            )
                            kolekSummaryAkhirBulan.value = calculateKolekSummary(cachedPeriod)
                            mantriSummaryAkhirBulan.value = calculateMantriSummaries(cachedPeriod)
                        }
                    }
                }
                source == "DAILY" -> {
                    try {
                        if (akhirBulanCustomers.value.isEmpty()) {
                            launch { loadSummaryForPeriod("AKHIR_BULAN") }
                        }
                        val content = if (isOffline) getOfflineFileContent("url_3") else null
                        val url = if (!isOffline) url3.value else null
                        if (!url.isNullOrBlank() || !content.isNullOrBlank()) {
                            val map = repository.getDailyInfo(url, content)
                            val unique = getDistinctCustomers(map)
                            if (map.isNotEmpty() && unique.isNotEmpty()) {
                                dailyDataMap.value = map
                                repository.savePeriodCustomers("PERIOD_DAILY", unique)
                                dailyCustomers.value = unique
                                summaryDaily.value = UnitSummaryData(
                                    os = unique.sumOf { it.osPinjaman },
                                    debtors = unique.size
                                )
                                kolekSummaryDaily.value = calculateKolekSummary(unique)
                                mantriSummaryDaily.value = calculateMantriSummaries(unique)
                            } else if (dailyCustomers.value.isEmpty()) {
                                val cachedDaily = repository.getCachedPeriodCustomers("PERIOD_DAILY")
                                if (cachedDaily.isNotEmpty()) {
                                    dailyCustomers.value = cachedDaily
                                    if (dailyDataMap.value.isEmpty()) {
                                        dailyDataMap.value = buildLookupMapFromEntities(cachedDaily)
                                    }
                                    summaryDaily.value = UnitSummaryData(
                                        os = cachedDaily.sumOf { it.osPinjaman },
                                        debtors = cachedDaily.size
                                    )
                                    kolekSummaryDaily.value = calculateKolekSummary(cachedDaily)
                                    mantriSummaryDaily.value = calculateMantriSummaries(cachedDaily)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CustomerViewModel", "Failed to load daily summary", e)
                        val cachedDaily = repository.getCachedPeriodCustomers("PERIOD_DAILY")
                        if (cachedDaily.isNotEmpty() && dailyCustomers.value.isEmpty()) {
                            dailyCustomers.value = cachedDaily
                            if (dailyDataMap.value.isEmpty()) {
                                dailyDataMap.value = buildLookupMapFromEntities(cachedDaily)
                            }
                            summaryDaily.value = UnitSummaryData(
                                os = cachedDaily.sumOf { it.osPinjaman },
                                debtors = cachedDaily.size
                            )
                            kolekSummaryDaily.value = calculateKolekSummary(cachedDaily)
                            mantriSummaryDaily.value = calculateMantriSummaries(cachedDaily)
                        }
                    }
                }
            }
            persistCurrentSummaries()
        }
    }

    fun loadDashboardMonthlyData(m: Int) {
        if (dashboardMonthlySummary.value.containsKey(m)) return
        viewModelScope.launch(Dispatchers.IO) {
            val isOffline = isOfflineModeEnabled.value
            if (!isOffline && !repository.isNetworkAvailable(getApplication())) {
                isShowingCachedOfflineData.value = true
                return@launch
            }
            val url = lw321AllUrls[m]?.value ?: ""
            val offlineContent = if (isOffline) {
                getOfflineFileContent("lw321_all_$m") ?: getOfflineFileContent("url_2")
            } else null

            if (url.isNotBlank() || !offlineContent.isNullOrBlank()) {
                val monthMap = repository.getDailyInfo(if (isOffline) null else url, offlineContent)
                val uniqueMonth = getDistinctCustomers(monthMap)
                if (uniqueMonth.isNotEmpty()) {
                    val sumData = UnitSummaryData(
                        os = uniqueMonth.sumOf { it.osPinjaman },
                        debtors = uniqueMonth.size
                    )
                    val kolekData = calculateKolekSummary(uniqueMonth)
                    val mantriData = calculateMantriSummaries(uniqueMonth)

                    dashboardMonthlySummary.value = dashboardMonthlySummary.value + (m to sumData)
                    dashboardMonthlyKolekSummary.value = dashboardMonthlyKolekSummary.value + (m to kolekData)
                    dashboardMonthlyMantriSummary.value = dashboardMonthlyMantriSummary.value + (m to mantriData)
                    persistCurrentSummaries()
                }
            }
        }
    }

    /**
     * Load cached summaries and 12-month kolek history from local storage immediately on app open.
     */
    fun loadLocalCachedData() {
        try {
            val cached = LocalSummaryCache.loadSummaries(getApplication())
            if (cached != null) {
                if (cached.summaryBulanLalu.debtors > 0) summaryBulanLalu.value = cached.summaryBulanLalu
                if (cached.summaryAkhirBulan.debtors > 0) summaryAkhirBulan.value = cached.summaryAkhirBulan
                if (cached.summaryDaily.debtors > 0) summaryDaily.value = cached.summaryDaily

                if (cached.kolekSummaryBulanLalu.lancar.debtors > 0 || cached.kolekSummaryBulanLalu.npl.debtors > 0) {
                    kolekSummaryBulanLalu.value = cached.kolekSummaryBulanLalu
                }
                if (cached.kolekSummaryAkhirBulan.lancar.debtors > 0 || cached.kolekSummaryAkhirBulan.npl.debtors > 0) {
                    kolekSummaryAkhirBulan.value = cached.kolekSummaryAkhirBulan
                }
                if (cached.kolekSummaryDaily.lancar.debtors > 0 || cached.kolekSummaryDaily.npl.debtors > 0) {
                    kolekSummaryDaily.value = cached.kolekSummaryDaily
                }

                if (cached.mantriSummaryBulanLalu.isNotEmpty()) mantriSummaryBulanLalu.value = cached.mantriSummaryBulanLalu
                if (cached.mantriSummaryAkhirBulan.isNotEmpty()) mantriSummaryAkhirBulan.value = cached.mantriSummaryAkhirBulan
                if (cached.mantriSummaryDaily.isNotEmpty()) mantriSummaryDaily.value = cached.mantriSummaryDaily

                if (cached.dashboardMonthlySummary.isNotEmpty()) dashboardMonthlySummary.value = cached.dashboardMonthlySummary
                if (cached.dashboardMonthlyKolekSummary.isNotEmpty()) dashboardMonthlyKolekSummary.value = cached.dashboardMonthlyKolekSummary
                if (cached.dashboardMonthlyMantriSummary.isNotEmpty()) dashboardMonthlyMantriSummary.value = cached.dashboardMonthlyMantriSummary
            }

            val cachedKolekMaps = LocalSummaryCache.loadAllMonthlyKolekMaps(getApplication())
            if (cachedKolekMaps.isNotEmpty()) {
                monthlyLw321KolekCache.putAll(cachedKolekMaps)
            }
            val cachedTunggakanMaps = LocalSummaryCache.loadAllMonthlyTunggakanMaps(getApplication())
            if (cachedTunggakanMaps.isNotEmpty()) {
                monthlyLw321TunggakanCache.putAll(cachedTunggakanMaps)
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomerViewModel", "Error loading local cached summaries", e)
        }

        // Hydrate Room-backed datasets immediately on background IO thread so offline startup is seamless
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cachedDi319 = repository.getCachedDi319RowsOnce()
                if (cachedDi319.isNotEmpty() && di319Rows.value.isEmpty()) {
                    di319Rows.value = cachedDi319
                }

                val cachedDaily = repository.getCachedPeriodCustomers("PERIOD_DAILY")
                if (cachedDaily.isNotEmpty()) {
                    if (dailyCustomers.value.isEmpty()) {
                        dailyCustomers.value = cachedDaily
                    }
                    if (dailyDataMap.value.isEmpty()) {
                        dailyDataMap.value = buildLookupMapFromEntities(cachedDaily)
                    }
                    if (summaryDaily.value.debtors == 0) {
                        summaryDaily.value = UnitSummaryData(
                            os = cachedDaily.sumOf { it.osPinjaman },
                            debtors = cachedDaily.size
                        )
                        kolekSummaryDaily.value = calculateKolekSummary(cachedDaily)
                        mantriSummaryDaily.value = calculateMantriSummaries(cachedDaily)
                    }
                }

                val cachedBlnLalu = repository.getCachedPeriodCustomers("PERIOD_BULAN_LALU")
                if (cachedBlnLalu.isNotEmpty()) {
                    if (bulanLaluCustomers.value.isEmpty()) {
                        bulanLaluCustomers.value = cachedBlnLalu
                    }
                    if (summaryBulanLalu.value.debtors == 0) {
                        summaryBulanLalu.value = UnitSummaryData(
                            os = cachedBlnLalu.sumOf { it.osPinjaman },
                            debtors = cachedBlnLalu.size
                        )
                        kolekSummaryBulanLalu.value = calculateKolekSummary(cachedBlnLalu)
                        mantriSummaryBulanLalu.value = calculateMantriSummaries(cachedBlnLalu)
                    }
                }

                val cachedAkhirBln = repository.getCachedPeriodCustomers("PERIOD_AKHIR_BULAN")
                if (cachedAkhirBln.isNotEmpty()) {
                    if (akhirBulanCustomers.value.isEmpty()) {
                        akhirBulanCustomers.value = cachedAkhirBln
                    }
                    if (summaryAkhirBulan.value.debtors == 0) {
                        summaryAkhirBulan.value = UnitSummaryData(
                            os = cachedAkhirBln.sumOf { it.osPinjaman },
                            debtors = cachedAkhirBln.size
                        )
                        kolekSummaryAkhirBulan.value = calculateKolekSummary(cachedAkhirBln)
                        mantriSummaryAkhirBulan.value = calculateMantriSummaries(cachedAkhirBln)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CustomerViewModel", "Error loading Room cached datasets", e)
            }
        }
    }

    private fun buildLookupMapFromEntities(
        list: List<com.example.data.model.CustomerEntity>
    ): Map<String, com.example.data.model.CustomerEntity> {
        val resultMap = mutableMapOf<String, com.example.data.model.CustomerEntity>()
        list.forEach { item ->
            val rek = item.noRekening.trim()
            if (rek.isNotBlank()) {
                resultMap[rek] = item
                val formatted15 = SpreadsheetService.formatRekening15(rek)
                resultMap[formatted15] = item
                val cleanRek = rek.filter { it.isDigit() }
                if (cleanRek.isNotBlank()) {
                    resultMap[cleanRek] = item
                    val noZeroRek = cleanRek.trimStart('0')
                    if (noZeroRek.isNotBlank()) {
                        resultMap[noZeroRek] = item
                    }
                    if (cleanRek.length >= 15) {
                        resultMap[cleanRek.takeLast(15)] = item
                    }
                }
            }
            val cif = item.cif.trim()
            if (cif.isNotBlank()) {
                resultMap[cif] = item
                resultMap["CIF-$cif"] = item
                val cleanCif = cif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
                if (cleanCif.isNotBlank()) {
                    resultMap[cleanCif] = item
                    resultMap["CIF-$cleanCif"] = item
                }
            }
        }
        return resultMap
    }

    private fun formatSyncTimestamp(millis: Long): String {
        if (millis <= 0L) return ""
        return try {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID"))
            sdf.format(java.util.Date(millis))
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Persist current calculated summary metrics to local storage so they appear instantly on next open.
     */
    fun persistCurrentSummaries() {
        try {
            LocalSummaryCache.saveSummaries(
                context = getApplication(),
                summaryBulanLalu = summaryBulanLalu.value,
                summaryAkhirBulan = summaryAkhirBulan.value,
                summaryDaily = summaryDaily.value,
                kolekSummaryBulanLalu = kolekSummaryBulanLalu.value,
                kolekSummaryAkhirBulan = kolekSummaryAkhirBulan.value,
                kolekSummaryDaily = kolekSummaryDaily.value,
                mantriSummaryBulanLalu = mantriSummaryBulanLalu.value,
                mantriSummaryAkhirBulan = mantriSummaryAkhirBulan.value,
                mantriSummaryDaily = mantriSummaryDaily.value,
                dashboardMonthlySummary = dashboardMonthlySummary.value,
                dashboardMonthlyKolekSummary = dashboardMonthlyKolekSummary.value,
                dashboardMonthlyMantriSummary = dashboardMonthlyMantriSummary.value
            )
        } catch (e: Exception) {
            android.util.Log.e("CustomerViewModel", "Error persisting summaries", e)
        }
    }

    /**
     * Prefetch all 12 months for LW321 and save to local storage.
     * Skips network calls if monthly kolek map is already cached locally.
     */
    fun prefetchAllMonthlyKolek(forceRefresh: Boolean = false) {
        val isOffline = isOfflineModeEnabled.value
        for (m in 1..12) {
            try {
                var existingKolek = monthlyLw321KolekCache[m]
                if (existingKolek == null) {
                    existingKolek = LocalSummaryCache.loadMonthlyKolekMap(getApplication(), m)
                    if (existingKolek != null && existingKolek.isNotEmpty()) {
                        monthlyLw321KolekCache[m] = existingKolek
                    }
                }
                var existingTgk = monthlyLw321TunggakanCache[m]
                if (existingTgk == null) {
                    existingTgk = LocalSummaryCache.loadMonthlyTunggakanMap(getApplication(), m)
                    if (existingTgk != null && existingTgk.isNotEmpty()) {
                        monthlyLw321TunggakanCache[m] = existingTgk
                    }
                }
                if (!forceRefresh && existingKolek != null && existingKolek.isNotEmpty() && existingTgk != null && existingTgk.isNotEmpty()) {
                    continue // Skip network download if both caches are already populated
                }
                val url = lw321AllUrls[m]?.value ?: ""
                val offlineContent = if (isOffline) {
                    getOfflineFileContent("lw321_all_$m") ?: getOfflineFileContent("url_2")
                } else null

                if (url.isNotBlank() || !offlineContent.isNullOrBlank()) {
                    val (fetchedKolek, fetchedTgk) = SpreadsheetService.fetchLw321MonthlyKolekAndTunggakanMaps(
                        if (isOffline) null else url,
                        offlineContent
                    )
                    if (fetchedKolek.isNotEmpty()) {
                        monthlyLw321KolekCache[m] = fetchedKolek
                        LocalSummaryCache.saveMonthlyKolekMap(getApplication(), m, fetchedKolek)
                    }
                    if (fetchedTgk.isNotEmpty()) {
                        monthlyLw321TunggakanCache[m] = fetchedTgk
                        LocalSummaryCache.saveMonthlyTunggakanMap(getApplication(), m, fetchedTgk)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CustomerViewModel", "Prefetch failed for month $m", e)
            }
        }
    }

    /**
     * Load Kolek and Tunggakan across all 12 months for a specific customer from memory/local storage cache instantly.
     * When forceRefresh is true, re-fetch fresh data from Google Sheets/network.
     */
    fun loadMonthlyKolekForCustomer(noRekening: String, cif: String, forceRefresh: Boolean = false) {
        val currentKey = "${noRekening.trim()}|${cif.trim()}"
        if (currentKey != lastMonthlyHistoryCustomerKey) {
            lastMonthlyHistoryCustomerKey = currentKey
            monthlyKolekHistory.value = emptyMap()
            monthlyTunggakanHistory.value = emptyMap()
        }
        viewModelScope.launch(Dispatchers.IO) {
            isMonthlyKolekLoading.value = true
            val cleanRek = noRekening.filter { it.isDigit() }
            val noZeroRek = cleanRek.trimStart('0')
            val cleanCif = cif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
            val formattedRek = SpreadsheetService.formatRekening15(noRekening)
            val isOffline = isOfflineModeEnabled.value

            val resultMap = mutableMapOf<Int, String>()
            val tunggakanResultMap = mutableMapOf<Int, Double?>()

            for (m in 1..12) {
                var monthMap = if (forceRefresh) null else monthlyLw321KolekCache[m]
                if (monthMap == null && !forceRefresh) {
                    monthMap = LocalSummaryCache.loadMonthlyKolekMap(getApplication(), m)
                    if (monthMap != null) {
                        monthlyLw321KolekCache[m] = monthMap
                    }
                }

                var tgkMap = if (forceRefresh) null else monthlyLw321TunggakanCache[m]
                if (tgkMap == null && !forceRefresh) {
                    tgkMap = LocalSummaryCache.loadMonthlyTunggakanMap(getApplication(), m)
                    if (tgkMap != null) {
                        monthlyLw321TunggakanCache[m] = tgkMap
                    }
                }

                if (monthMap == null || tgkMap == null) {
                    val url = lw321AllUrls[m]?.value ?: ""
                    val offlineContent = if (isOffline) {
                        getOfflineFileContent("lw321_all_$m") ?: getOfflineFileContent("url_2")
                    } else null

                    if (url.isNotBlank() || !offlineContent.isNullOrBlank()) {
                        val (fetchedKolek, fetchedTgk) = SpreadsheetService.fetchLw321MonthlyKolekAndTunggakanMaps(
                            if (isOffline) null else url,
                            offlineContent
                        )
                        if (fetchedKolek.isNotEmpty()) {
                            monthMap = fetchedKolek
                            monthlyLw321KolekCache[m] = fetchedKolek
                            LocalSummaryCache.saveMonthlyKolekMap(getApplication(), m, fetchedKolek)
                        }
                        if (fetchedTgk.isNotEmpty()) {
                            tgkMap = fetchedTgk
                            monthlyLw321TunggakanCache[m] = fetchedTgk
                            LocalSummaryCache.saveMonthlyTunggakanMap(getApplication(), m, fetchedTgk)
                        }
                    }
                }

                val foundKolek = monthMap?.let { map ->
                    map[cleanRek]
                        ?: map[formattedRek]
                        ?: (if (noZeroRek.isNotBlank()) map[noZeroRek] else null)
                        ?: (if (cleanRek.length >= 15) map[cleanRek.takeLast(15)] else null)
                        ?: map[cleanCif]
                        ?: map["CIF-$cleanCif"]
                } ?: "-"

                val foundTgk = tgkMap?.let { map ->
                    map[cleanRek]
                        ?: map[formattedRek]
                        ?: (if (noZeroRek.isNotBlank()) map[noZeroRek] else null)
                        ?: (if (cleanRek.length >= 15) map[cleanRek.takeLast(15)] else null)
                        ?: map[cleanCif]
                        ?: map["CIF-$cleanCif"]
                }

                resultMap[m] = foundKolek
                tunggakanResultMap[m] = foundTgk
            }

            monthlyKolekHistory.value = resultMap
            monthlyTunggakanHistory.value = tunggakanResultMap
            isMonthlyKolekLoading.value = false
        }
    }

    /**
     * Load prior month's Kolek map from LW321 All (e.g. if report month is 8 -> load month 7).
     * Cached in monthlyLw321KolekCache and LocalSummaryCache to ensure smooth instant UI without stutter.
     */
    fun loadPriorMonthLw321KolekMap(month: Int) {
        if (month !in 1..12) return
        var cached = monthlyLw321KolekCache[month]
        if (cached == null) {
            cached = LocalSummaryCache.loadMonthlyKolekMap(getApplication(), month)
            if (cached != null) {
                monthlyLw321KolekCache[month] = cached
            }
        }
        if (cached != null && cached.isNotEmpty()) {
            priorMonthLw321KolekMap.value = cached
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val isOffline = isOfflineModeEnabled.value
            val url = lw321AllUrls[month]?.value ?: ""
            val offlineContent = if (isOffline) {
                getOfflineFileContent("lw321_all_$month") ?: getOfflineFileContent("url_2")
            } else null

            if (url.isNotBlank() || !offlineContent.isNullOrBlank()) {
                val fetchedMap = SpreadsheetService.fetchLw321MonthlyKolekMap(if (isOffline) null else url, offlineContent)
                if (fetchedMap.isNotEmpty()) {
                    monthlyLw321KolekCache[month] = fetchedMap
                    LocalSummaryCache.saveMonthlyKolekMap(getApplication(), month, fetchedMap)
                    priorMonthLw321KolekMap.value = fetchedMap
                }
            }
        }
    }

    fun selectPeriodSource(source: String) {
        selectedPeriodSource.value = source
        if (source.startsWith("BULAN_")) {
            val m = source.removePrefix("BULAN_").toIntOrNull()
            if (m != null && m in 1..12) {
                filterBulanLunas.value = String.format("%02d", m)
            }
        }
        refreshSpreadsheetData()
    }

    fun setFilterBulanLunas(bulan: String) {
        filterBulanLunas.value = bulan
        val monthNum = bulan.toIntOrNull()
        if (monthNum != null && monthNum in 1..12) {
            val monthUrl = lw321AllUrls[monthNum]?.value ?: ""
            if (monthUrl.isNotBlank()) {
                selectPeriodSource("BULAN_$monthNum")
                return
            }
        } else if (bulan == "SEMUA") {
            if (selectedPeriodSource.value.startsWith("BULAN_")) {
                selectPeriodSource("AKHIR_BULAN")
                return
            }
        }
    }

    fun extractDateFromCsvContent(csvContent: String): String {
        try {
            val lines = csvContent.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size >= 5) {
                val delim = if (lines[0].count { it == ';' } > lines[0].count { it == ',' }) ";" else ","
                val cols = lines[4].split(delim).map { it.replace("\"", "").trim() }
                if (cols.size > 1) {
                    val valB5 = cols[1]
                    if (valB5.isNotBlank()) {
                        return valB5
                    }
                }
            }
            for (i in 0 until lines.size.coerceAtMost(25)) {
                val delim = if (lines[0].count { it == ';' } > lines[0].count { it == ',' }) ";" else ","
                val cols = lines[i].split(delim).map { it.replace("\"", "").trim() }
                if (cols.size > 1) {
                    val valB = cols[1]
                    if (valB.contains("/") && valB.length >= 8) {
                        return valB
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomerViewModel", "Failed to parse date from CSV", e)
        }
        return ""
    }

    fun fetchPeriodDates() {
        viewModelScope.launch(Dispatchers.IO) {
            if (isOfflineModeEnabled.value) {
                val dailyContent = getOfflineFileContent("url_3")
                if (!dailyContent.isNullOrBlank()) {
                    val d = extractDateFromCsvContent(dailyContent)
                    if (d.isNotBlank()) {
                        sharedPrefs.edit().putString("period_date_daily", d).apply()
                        periodDateDaily.value = d
                    }
                }
                val blnLaluContent = getOfflineFileContent("url_1")
                if (!blnLaluContent.isNullOrBlank()) {
                    val d = extractDateFromCsvContent(blnLaluContent)
                    if (d.isNotBlank()) {
                        sharedPrefs.edit().putString("period_date_bulan_lalu", d).apply()
                        periodDateBulanLalu.value = d
                    }
                }
                val akhirBlnContent = getOfflineFileContent("url_2")
                if (!akhirBlnContent.isNullOrBlank()) {
                    val d = extractDateFromCsvContent(akhirBlnContent)
                    if (d.isNotBlank()) {
                        sharedPrefs.edit().putString("period_date_akhir_bulan", d).apply()
                        periodDateAkhirBulan.value = d
                    }
                }
                val eContent = getOfflineFileContent("e_url_1")
                if (!eContent.isNullOrBlank()) {
                    val d = extractDateFromCsvContent(eContent)
                    if (d.isNotBlank()) {
                        sharedPrefs.edit().putString("period_date_lw325", d).apply()
                        periodDateLw325.value = d
                    }
                }
                return@launch
            }

            val client = okhttp3.OkHttpClient()
            // 1. Bulan Lalu
            val u1 = url1.value
            if (u1.isNotBlank()) {
                val date = extractB5DatePriv(client, u1)
                if (date.isNotBlank()) {
                    sharedPrefs.edit().putString("period_date_bulan_lalu", date).apply()
                    periodDateBulanLalu.value = date
                }
            }
            // 2. Akhir Bulan
            val u2 = url2.value
            if (u2.isNotBlank()) {
                val date = extractB5DatePriv(client, u2)
                if (date.isNotBlank()) {
                    sharedPrefs.edit().putString("period_date_akhir_bulan", date).apply()
                    periodDateAkhirBulan.value = date
                }
            }
            // 3. Daily
            val u3 = url3.value
            if (u3.isNotBlank()) {
                val date = extractB5DatePriv(client, u3)
                if (date.isNotBlank()) {
                    sharedPrefs.edit().putString("period_date_daily", date).apply()
                    periodDateDaily.value = date
                }
            }
            // 4. LW325 (Ekstrakomp)
            val eu1 = eUrl1.value
            if (eu1.isNotBlank()) {
                val date = extractB5DatePriv(client, eu1)
                if (date.isNotBlank()) {
                    sharedPrefs.edit().putString("period_date_lw325", date).apply()
                    periodDateLw325.value = date
                }
            }
        }
    }

    private fun extractB5DatePriv(client: okhttp3.OkHttpClient, rawUrl: String): String {
        try {
            val url = SpreadsheetService.sanitizeSpreadsheetUrl(rawUrl)
            val request = okhttp3.Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val csvContent = response.body?.string() ?: ""
                    return extractDateFromCsvContent(csvContent)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomerViewModel", "Failed to extract B5 from $rawUrl", e)
        }
        return ""
    }

    fun saveCustomUrl(url: String) {
        val trimmed = url.trim()
        sharedPrefs.edit().putString("custom_spreadsheet_url", trimmed).apply()
        customSpreadsheetUrl.value = trimmed
        refreshSpreadsheetData()
    }

    fun toggleOfflineMode(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("is_offline_mode_enabled", enabled).apply()
        isOfflineModeEnabled.value = enabled
        refreshSpreadsheetData()
    }

    fun getOfflineFileContent(key: String): String? {
        return try {
            val file = java.io.File(getApplication<Application>().filesDir, "offline_$key.csv")
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun deleteOfflineFile(key: String) {
        try {
            val file = java.io.File(getApplication<Application>().filesDir, "offline_$key.csv")
            if (file.exists()) {
                file.delete()
            }
            sharedPrefs.edit().remove("offline_name_$key").apply()
            when (key) {
                "url_1" -> offlineNameUrl1.value = ""
                "url_2" -> offlineNameUrl2.value = ""
                "url_3" -> offlineNameUrl3.value = ""
                "url_4" -> offlineNameUrl4.value = ""
                "e_url_1" -> offlineNameEUrl1.value = ""
                "e_url_2" -> offlineNameEUrl2.value = ""
                "k_url_1" -> offlineNameKUrl1.value = ""
                "di319_url" -> offlineNameDi319Url.value = ""
                "lw325_daily_url" -> offlineNameLw325Daily.value = ""
            }
            refreshSpreadsheetData()
        } catch (e: Exception) {
            android.util.Log.e("CustomerViewModel", "Failed to delete offline file $key", e)
        }
    }

    fun saveOfflineFile(key: String, content: String, displayName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = java.io.File(getApplication<Application>().filesDir, "offline_$key.csv")
                file.writeText(content)
                sharedPrefs.edit().putString("offline_name_$key", displayName).apply()
                
                viewModelScope.launch(Dispatchers.Main) {
                    when (key) {
                        "url_1" -> offlineNameUrl1.value = displayName
                        "url_2" -> offlineNameUrl2.value = displayName
                        "url_3" -> offlineNameUrl3.value = displayName
                        "url_4" -> offlineNameUrl4.value = displayName
                        "e_url_1" -> offlineNameEUrl1.value = displayName
                        "e_url_2" -> offlineNameEUrl2.value = displayName
                        "k_url_1" -> offlineNameKUrl1.value = displayName
                        "di319_url" -> offlineNameDi319Url.value = displayName
                        "lw325_daily_url" -> offlineNameLw325Daily.value = displayName
                    }
                }
                
                val date = extractB5FromCsvContent(content)
                if (date.isNotBlank()) {
                    sharedPrefs.edit().putString("period_date_${getPeriodKey(key)}", date).apply()
                    viewModelScope.launch(Dispatchers.Main) {
                        when (key) {
                            "url_1" -> periodDateBulanLalu.value = date
                            "url_2" -> periodDateAkhirBulan.value = date
                            "url_3" -> periodDateDaily.value = date
                            "e_url_1" -> periodDateLw325.value = date
                        }
                    }
                }
                
                refreshSpreadsheetData()
            } catch (e: Exception) {
                android.util.Log.e("CustomerViewModel", "Failed to save offline file $key", e)
            }
        }
    }

    private fun getPeriodKey(key: String): String {
        return when (key) {
            "url_1" -> "bulan_lalu"
            "url_2" -> "akhir_bulan"
            "url_3" -> "daily"
            "e_url_1" -> "lw325"
            else -> ""
        }
    }

    private fun extractB5FromCsvContent(csvContent: String): String {
        try {
            val lines = csvContent.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size >= 5) {
                val delim = if (lines[0].count { it == ';' } > lines[0].count { it == ',' }) ";" else ","
                val cols = lines[4].split(delim).map { it.replace("\"", "").trim() }
                if (cols.size > 1) {
                    val valB5 = cols[1]
                    if (valB5.isNotBlank()) {
                        return valB5
                    }
                }
            }
            for (i in 0 until lines.size.coerceAtMost(25)) {
                val delim = if (lines[0].count { it == ';' } > lines[0].count { it == ',' }) ";" else ","
                val cols = lines[i].split(delim).map { it.replace("\"", "").trim() }
                if (cols.size > 1) {
                    val valB = cols[1]
                    if (valB.contains("/") && valB.length >= 8) {
                        return valB
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return ""
    }

    fun deleteCustomUrl() {
        sharedPrefs.edit().remove("custom_spreadsheet_url").apply()
        customSpreadsheetUrl.value = ""
        refreshSpreadsheetData()
    }

    fun isPastDate(dateStr: String): Boolean {
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val parsedDate = java.time.LocalDate.parse(dateStr, formatter)
            val today = java.time.LocalDate.now()
            parsedDate.isBefore(today)
        } catch (e: Exception) {
            false
        }
    }

    fun getActiveVisitDate(): String {
        val dateVal = filterBulanLunas.value
        return if (dateVal == "SEMUA" || dateVal.isBlank() || dateVal.length != 10) {
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("id", "ID")).format(java.util.Date())
        } else {
            dateVal
        }
    }

    fun toggleKunjungan(customer: CustomerEntity) {
        viewModelScope.launch {
            val key = if (customer.noRekening.isNotBlank()) customer.noRekening else if (customer.cif.isNotBlank()) customer.cif else customer.namaNasabah
            val D = getActiveVisitDate()
            if (isPastDate(D)) return@launch
            
            val isChecked = kunjunganKeys.value.contains(key)
            
            val deleted = (sharedPrefs.getStringSet("kunjungan_deleted_$D", emptySet()) ?: emptySet()).toMutableSet()
            val added = (sharedPrefs.getStringSet("kunjungan_added_$D", emptySet()) ?: emptySet()).toMutableSet()
            
            if (isChecked) {
                // Remove from kunjungan
                added.remove(key)
                deleted.add(key)
                // Automatically delete tindak lanjut
                deleteTindakLanjut(customer, isKunjungan = false)
                deleteTindakLanjut(customer, isKunjungan = true)
            } else {
                // Add to kunjungan
                added.add(key)
                deleted.remove(key)
                
                // Add to history
                val history = (sharedPrefs.getStringSet("kunjungan_history_$D", emptySet()) ?: emptySet()).toMutableSet()
                history.add(key)
                sharedPrefs.edit().putStringSet("kunjungan_history_$D", history).apply()
            }
            
            sharedPrefs.edit().putStringSet("kunjungan_deleted_$D", deleted).apply()
            sharedPrefs.edit().putStringSet("kunjungan_added_$D", added).apply()
            
            kunjunganUpdateTrigger.value = System.currentTimeMillis()
        }
    }

    fun addKunjungan(customer: CustomerEntity) {
        viewModelScope.launch {
            val key = if (customer.noRekening.isNotBlank()) customer.noRekening else if (customer.cif.isNotBlank()) customer.cif else customer.namaNasabah
            val D = getActiveVisitDate()
            if (isPastDate(D)) return@launch

            val deleted = (sharedPrefs.getStringSet("kunjungan_deleted_$D", emptySet()) ?: emptySet()).toMutableSet()
            val added = (sharedPrefs.getStringSet("kunjungan_added_$D", emptySet()) ?: emptySet()).toMutableSet()
            
            deleted.remove(key)
            if (!added.contains(key)) {
                added.add(key)
                sharedPrefs.edit().putStringSet("kunjungan_added_$D", added).apply()
            }
            sharedPrefs.edit().putStringSet("kunjungan_deleted_$D", deleted).apply()
            
            val history = (sharedPrefs.getStringSet("kunjungan_history_$D", emptySet()) ?: emptySet()).toMutableSet()
            history.add(key)
            sharedPrefs.edit().putStringSet("kunjungan_history_$D", history).apply()
            
            kunjunganUpdateTrigger.value = System.currentTimeMillis()
        }
    }

    val tindakLanjutMap = MutableStateFlow<Map<String, String>>(emptyMap())

    private fun loadTindakLanjutMap() {
        val all = sharedPrefs.all
        val map = mutableMapOf<String, String>()
        for ((k, v) in all) {
            if (k.startsWith("tindak_lanjut_") && !k.startsWith("tindak_lanjut_date_") && v is String) {
                map[k.removePrefix("tindak_lanjut_")] = v
            }
        }
        tindakLanjutMap.value = map
    }

    fun saveTindakLanjut(customer: CustomerEntity, option: String, isKunjungan: Boolean = false) {
        val key = if (customer.noRekening.isNotBlank()) customer.noRekening else if (customer.cif.isNotBlank()) customer.cif else customer.namaNasabah
        val prefKey = if (isKunjungan) "tindak_lanjut_kunjungan_$key" else "tindak_lanjut_$key"
        val datePrefKey = if (isKunjungan) "tindak_lanjut_date_kunjungan_$key" else "tindak_lanjut_date_$key"
        sharedPrefs.edit().putString(prefKey, option).apply()
        
        // Save today's date
        val todayStr = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(java.time.LocalDate.now())
        sharedPrefs.edit().putString(datePrefKey, todayStr).apply()
        
        val mapKey = if (isKunjungan) "kunjungan_$key" else key
        val current = tindakLanjutMap.value.toMutableMap()
        current[mapKey] = option
        tindakLanjutMap.value = current
    }

    fun deleteTindakLanjut(customer: CustomerEntity, isKunjungan: Boolean = false) {
        val key = if (customer.noRekening.isNotBlank()) customer.noRekening else if (customer.cif.isNotBlank()) customer.cif else customer.namaNasabah
        val prefKey = if (isKunjungan) "tindak_lanjut_kunjungan_$key" else "tindak_lanjut_$key"
        val datePrefKey = if (isKunjungan) "tindak_lanjut_date_kunjungan_$key" else "tindak_lanjut_date_$key"
        sharedPrefs.edit().remove(prefKey).remove(datePrefKey).apply()
        
        val mapKey = if (isKunjungan) "kunjungan_$key" else key
        val current = tindakLanjutMap.value.toMutableMap()
        current.remove(mapKey)
        tindakLanjutMap.value = current
    }

    fun resetBayarFullTindakLanjut(): Int {
        val editor = sharedPrefs.edit()
        val all = sharedPrefs.all
        val current = tindakLanjutMap.value.toMutableMap()
        var count = 0

        for ((k, v) in all) {
            if (k.startsWith("tindak_lanjut_") && !k.startsWith("tindak_lanjut_date_") && v is String) {
                if (v.equals("Bayar", ignoreCase = true) || v.equals("Bayar Full", ignoreCase = true) || v.equals("Bayar Lunas", ignoreCase = true)) {
                    val mapKey = k.removePrefix("tindak_lanjut_")
                    editor.remove(k)
                    editor.remove("tindak_lanjut_date_$mapKey")
                    current.remove(mapKey)
                    count++
                }
            }
        }
        editor.apply()
        tindakLanjutMap.value = current
        return count
    }

    fun clearAllKunjungan() {
        viewModelScope.launch {
            val D = getActiveVisitDate()
            if (isPastDate(D)) return@launch
            
            val keysToClear = kunjunganKeys.value
            val editor = sharedPrefs.edit()
            for (k in keysToClear) {
                editor.remove("tindak_lanjut_kunjungan_$k").remove("tindak_lanjut_date_kunjungan_$k")
            }
            editor.putStringSet("kunjungan_deleted_$D", keysToClear)
            editor.putStringSet("kunjungan_added_$D", emptySet())
            editor.putStringSet("kunjungan_history_$D", emptySet())
            editor.putBoolean("kunjungan_manually_cleared_$D", true)
            editor.apply()
            
            val current = tindakLanjutMap.value.toMutableMap()
            for (k in keysToClear) {
                current.remove("kunjungan_$k")
            }
            tindakLanjutMap.value = current
            
            kunjunganUpdateTrigger.value = System.currentTimeMillis()
        }
    }

    fun selectVisitDate(dateStr: String) {
        filterBulanLunas.value = dateStr
        sharedPrefs.edit().remove("kunjungan_manually_cleared_$dateStr").apply()
        viewModelScope.launch {
            generateKunjunganIfEmpty(dateStr)
        }
    }

    suspend fun generateKunjunganIfEmpty(dateStr: String) {
        if (dateStr == "SEMUA" || dateStr.isBlank() || isPastDate(dateStr)) return
        val D = dateStr
        if (sharedPrefs.getBoolean("kunjungan_manually_cleared_$D", false)) return

        val added = (sharedPrefs.getStringSet("kunjungan_added_$D", emptySet()) ?: emptySet()).toMutableSet()
        val deleted = (sharedPrefs.getStringSet("kunjungan_deleted_$D", emptySet()) ?: emptySet()).toMutableSet()

        if ((added - deleted).isNotEmpty()) {
            return
        }

        // Retrieve INTRAKOMP customers
        val intraList = repository.getCustomersFlow("INTRAKOMP").first()
        val pn = loggedInPN.value.trim()
        val targetDigits = Regex("""\d{4,9}""").find(pn)?.value ?: ""
        val pool = if (pn.isNotBlank()) {
            val filtered = intraList.filter { cust ->
                val custPn = cust.pnMantri.trim()
                val custDigits = custPn.filter { it.isDigit() }
                val matchPn = when {
                    custPn.isBlank() && targetDigits.isBlank() -> false
                    custPn.equals(pn, ignoreCase = true) -> true
                    targetDigits.isNotBlank() && (
                        custPn == targetDigits ||
                        custDigits == targetDigits ||
                        custDigits.trimStart('0') == targetDigits.trimStart('0') ||
                        (targetDigits.length >= 5 && custDigits.endsWith(targetDigits)) ||
                        (custDigits.length >= 5 && targetDigits.endsWith(custDigits))
                    ) -> true
                    else -> false
                }
                val matchName = cust.namaMantri.isNotBlank() && (
                    cust.namaMantri.contains(pn, ignoreCase = true) ||
                    (pn.contains(" - ") && cust.namaMantri.contains(pn.substringAfter(" - ").trim(), ignoreCase = true))
                )
                matchPn || matchName
            }
            if (filtered.isNotEmpty()) filtered else intraList
        } else {
            if (filterMantri.value != "SEMUA") {
                val targetClean = cleanMantriNameOnly(filterMantri.value)
                val filtered = intraList.filter { cust ->
                    val itemClean = cleanMantriNameOnly(cust.namaMantri)
                    if (targetClean == "Blank PN") {
                        cust.namaMantri.isBlank() || cust.namaMantri.equals("Blank PN", ignoreCase = true) || itemClean == "Blank PN"
                    } else {
                        itemClean.equals(targetClean, ignoreCase = true) || cust.namaMantri.equals(filterMantri.value, ignoreCase = true)
                    }
                }
                if (filtered.isNotEmpty()) filtered else intraList
            } else {
                intraList
            }
        }

        if (pool.isEmpty()) return

        // Group candidates by Desa (uppercase)
        val nonBlankDesaGroups = pool.filter { it.desa.isNotBlank() }.groupBy { it.desa.trim().uppercase() }
        val blankDesaList = pool.filter { it.desa.isBlank() }
        val preferredDesa = filterTahunLunas.value.trim().uppercase()

        // Prioritize preferred desa if selected, otherwise the desa with the most customers
        val sortedDesaEntries = nonBlankDesaGroups.entries.sortedWith(
            compareByDescending<Map.Entry<String, List<CustomerEntity>>> { 
                preferredDesa != "SEMUA" && it.key == preferredDesa 
            }.thenByDescending { 
                it.value.size 
            }
        )

        val chosenKeys = mutableSetOf<String>()
        // 1. Take up to 15 from prioritized desa(s)
        for (entry in sortedDesaEntries) {
            for (cust in entry.value) {
                val key = if (cust.noRekening.isNotBlank()) cust.noRekening else if (cust.cif.isNotBlank()) cust.cif else cust.namaNasabah
                if (key.isNotBlank()) {
                    chosenKeys.add(key)
                    if (chosenKeys.size >= 15) break
                }
            }
            if (chosenKeys.size >= 15) break
        }

        // 2. If still < 15, fallback to blank desa customers if any
        if (chosenKeys.size < 15) {
            for (cust in blankDesaList) {
                val key = if (cust.noRekening.isNotBlank()) cust.noRekening else if (cust.cif.isNotBlank()) cust.cif else cust.namaNasabah
                if (key.isNotBlank()) {
                    chosenKeys.add(key)
                    if (chosenKeys.size >= 15) break
                }
            }
        }

        if (chosenKeys.isNotEmpty()) {
            val editor = sharedPrefs.edit()
            editor.putStringSet("kunjungan_added_$D", chosenKeys)
            editor.putStringSet("kunjungan_history_$D", chosenKeys)
            editor.putStringSet("kunjungan_deleted_$D", emptySet())
            editor.apply()
            kunjunganUpdateTrigger.value = System.currentTimeMillis()
        }
    }

    private fun saveMenuFiltersToPrefs(menu: String, filters: FilterParams) {
        sharedPrefs.edit()
            .putString("filter_${menu}_search", filters.search)
            .putString("filter_${menu}_kolek", filters.kolek)
            .putString("filter_${menu}_mantri", filters.mantri)
            .putString("filter_${menu}_bulan", filters.bulan)
            .putString("filter_${menu}_tahun", filters.tahun)
            .putString("filter_${menu}_os", filters.osVal)
            .putString("filter_${menu}_plafond", filters.plVal)
            .putString("filter_${menu}_tenor", filters.tnVal)
            .putString("filter_${menu}_segmen", filters.segmen)
            .putString("filter_${menu}_kolek_update", filters.kolekUpdate)
            .putString("filter_${menu}_kolek_bln_lalu", filters.kolekBlnLalu)
            .putString("filter_${menu}_npd", filters.npd)
            .putString("filter_${menu}_alamat", filters.alamat)
            .putBoolean("filter_${menu}_setor_pokok", filters.setorPokok)
            .putBoolean("filter_${menu}_tusim", filters.tusim)
            .putBoolean("filter_${menu}_tucil", filters.tucil)
            .putBoolean("filter_${menu}_jt_only", filters.jatuhTempoOnly)
            .putInt("filter_${menu}_jt_h", filters.jatuhTempoH)
            .putString("filter_${menu}_desa", filters.desa)
            .apply()
    }

    private fun loadMenuFiltersFromPrefs(menu: String): FilterParams {
        return FilterParams(
            search = sharedPrefs.getString("filter_${menu}_search", "") ?: "",
            kolek = sharedPrefs.getString("filter_${menu}_kolek", "SEMUA") ?: "SEMUA",
            mantri = sharedPrefs.getString("filter_${menu}_mantri", "SEMUA") ?: "SEMUA",
            bulan = sharedPrefs.getString("filter_${menu}_bulan", "SEMUA") ?: "SEMUA",
            tahun = sharedPrefs.getString("filter_${menu}_tahun", "SEMUA") ?: "SEMUA",
            osVal = sharedPrefs.getString("filter_${menu}_os", "SEMUA") ?: "SEMUA",
            plVal = sharedPrefs.getString("filter_${menu}_plafond", "SEMUA") ?: "SEMUA",
            tnVal = sharedPrefs.getString("filter_${menu}_tenor", "SEMUA") ?: "SEMUA",
            segmen = sharedPrefs.getString("filter_${menu}_segmen", "SEMUA") ?: "SEMUA",
            kolekUpdate = sharedPrefs.getString("filter_${menu}_kolek_update", "SEMUA") ?: "SEMUA",
            kolekBlnLalu = sharedPrefs.getString("filter_${menu}_kolek_bln_lalu", "SEMUA") ?: "SEMUA",
            npd = sharedPrefs.getString("filter_${menu}_npd", "SEMUA") ?: "SEMUA",
            alamat = sharedPrefs.getString("filter_${menu}_alamat", "") ?: "",
            setorPokok = sharedPrefs.getBoolean("filter_${menu}_setor_pokok", false),
            tusim = sharedPrefs.getBoolean("filter_${menu}_tusim", false),
            tucil = sharedPrefs.getBoolean("filter_${menu}_tucil", false),
            jatuhTempoOnly = sharedPrefs.getBoolean("filter_${menu}_jt_only", false),
            jatuhTempoH = sharedPrefs.getInt("filter_${menu}_jt_h", 0),
            desa = sharedPrefs.getString("filter_${menu}_desa", "SEMUA") ?: "SEMUA"
        )
    }

    private val initialMenu = sharedPrefs.getString("last_selected_menu", "INTRAKOMP") ?: "INTRAKOMP"
    private val initialFilters = loadMenuFiltersFromPrefs(initialMenu)

    // UI state parameters
    val selectedMenu = MutableStateFlow(initialMenu)
    private val menuFilterCache = mutableMapOf<String, FilterParams>()
    private var isSwitchingMenu = false
    val searchQuery = MutableStateFlow(initialFilters.search)
    val filterKolek = MutableStateFlow(initialFilters.kolek)
    val filterMantri = MutableStateFlow(initialFilters.mantri)
    val filterBulanLunas = MutableStateFlow(initialFilters.bulan)
    val filterTahunLunas = MutableStateFlow(initialFilters.tahun)
    val filterOS = MutableStateFlow(initialFilters.osVal)
    val filterPlafondRange = MutableStateFlow(initialFilters.plVal)
    val filterTenorRange = MutableStateFlow(initialFilters.tnVal)
    val filterSegmen = MutableStateFlow(initialFilters.segmen)
    val filterKolekUpdate = MutableStateFlow(initialFilters.kolekUpdate)
    val filterKolekBlnLalu = MutableStateFlow(initialFilters.kolekBlnLalu)
    val filterNpd = MutableStateFlow(initialFilters.npd)
    val filterAlamat = MutableStateFlow(initialFilters.alamat)
    val filterSetorPokok = MutableStateFlow(initialFilters.setorPokok)
    val filterTusim = MutableStateFlow(initialFilters.tusim)
    val filterTucil = MutableStateFlow(initialFilters.tucil)
    val filterJatuhTempoOnly = MutableStateFlow(initialFilters.jatuhTempoOnly)
    val selectedJatuhTempoH = MutableStateFlow(initialFilters.jatuhTempoH)
    val filterDesa = MutableStateFlow(initialFilters.desa)

    val sortColumn = MutableStateFlow(
        try {
            val savedCol = sharedPrefs.getString("last_sort_column", null)
            if (savedCol != null) SortColumn.valueOf(savedCol) else SortColumn.NAMA_NASABAH
        } catch (e: Exception) {
            SortColumn.NAMA_NASABAH
        }
    )
    val sortDirection = MutableStateFlow(
        try {
            val savedDir = sharedPrefs.getString("last_sort_direction", null)
            if (savedDir != null) SortDirection.valueOf(savedDir) else SortDirection.ASC
        } catch (e: Exception) {
            SortDirection.ASC
        }
    )

    val activeVisitDateStr: StateFlow<String> = filterBulanLunas.map { dateVal ->
        if (dateVal == "SEMUA" || dateVal.isBlank() || dateVal.length != 10) {
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("id", "ID")).format(java.util.Date())
        } else {
            dateVal
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("id", "ID")).format(java.util.Date()))

    val kunjunganUpdateTrigger = MutableStateFlow(0L)

    val kunjunganHistorySet: StateFlow<Set<String>> = combine(activeVisitDateStr, kunjunganUpdateTrigger) { date, _ ->
        sharedPrefs.getStringSet("kunjungan_history_$date", emptySet()) ?: emptySet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val kunjunganDeletedSet: StateFlow<Set<String>> = combine(activeVisitDateStr, kunjunganUpdateTrigger) { date, _ ->
        sharedPrefs.getStringSet("kunjungan_deleted_$date", emptySet()) ?: emptySet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val kunjunganAddedSet: StateFlow<Set<String>> = combine(activeVisitDateStr, kunjunganUpdateTrigger) { date, _ ->
        sharedPrefs.getStringSet("kunjungan_added_$date", emptySet()) ?: emptySet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val kunjunganKeys: StateFlow<Set<String>> = combine(
        activeVisitDateStr,
        kunjunganDeletedSet,
        kunjunganAddedSet,
        kunjunganHistorySet
    ) { date, deleted, added, history ->
        if (isPastDate(date)) {
            history
        } else {
            added - deleted
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Intermediate helper state to compile additional filter params type-safely and avoid overflow
    private val filtersFlow = combine(
        combine(searchQuery, filterKolek, filterMantri) { s, k, m -> Triple(s, k, m) },
        combine(filterBulanLunas, filterTahunLunas, filterOS) { b, t, o -> Triple(b, t, o) },
        combine(filterPlafondRange, filterTenorRange, filterSegmen) { p, t, seg -> Triple(p, t, seg) },
        combine(filterKolekUpdate, filterNpd, filterAlamat, filterKolekBlnLalu, filterDesa) { ku, npd, al, kbl, des -> listOf(ku, npd, al, kbl, des) },
        combine(filterSetorPokok, filterTusim, filterTucil, filterJatuhTempoOnly, selectedJatuhTempoH) { sp, tus, tuc, jt, jh ->
            ToggleFilters(sp, tus, tuc, jt, jh)
        }
    ) { group1, group2, group3, group4, group5 ->
        FilterParams(
            search = group1.first,
            kolek = group1.second,
            mantri = group1.third,
            bulan = group2.first,
            tahun = group2.second,
            osVal = group2.third,
            plVal = group3.first,
            tnVal = group3.second,
            segmen = group3.third,
            kolekUpdate = group4[0],
            npd = group4[1],
            alamat = group4[2],
            kolekBlnLalu = group4[3],
            setorPokok = group5.sp,
            tusim = group5.tus,
            tucil = group5.tuc,
            jatuhTempoOnly = group5.jt,
            jatuhTempoH = group5.jtH,
            desa = group4[4]
        )
    }

    init {
        loadTindakLanjutMap()
        loadLocalCachedData()
        loadDailyDataForFilter()
        viewModelScope.launch {
            repository.getDi319Flow().collect { cachedRows ->
                if (cachedRows.isNotEmpty()) {
                    di319Rows.value = cachedRows
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val totalCustomers = repository.getTotalCustomersCount()
            val networkOk = repository.isNetworkAvailable(getApplication())
            if (totalCustomers == 0) {
                if (networkOk) {
                    val curMenu = selectedMenu.value
                    val fetchMenu = if (curMenu == "KELOLAAN" || curMenu == "KUNJUNGAN") "INTRAKOMP" else curMenu
                    if (fetchMenu != "SIMPANAN") {
                        refreshSpreadsheetData()
                    }
                } else {
                    repository.seedMockIfEmpty("INTRAKOMP")
                    repository.seedMockIfEmpty("EKSTRAKOMP")
                    isShowingCachedOfflineData.value = true
                    errorMessage.value = "Tidak ada koneksi internet dan belum ada data tersimpan."
                }
            } else if (!networkOk && !isOfflineModeEnabled.value) {
                isShowingCachedOfflineData.value = true
            }
            if (networkOk && periodDateDaily.value.isBlank() && periodDateAkhirBulan.value.isBlank()) {
                fetchPeriodDates()
            }
        }
        viewModelScope.launch {
            combine(activeVisitDateStr, kunjunganKeys) { date, keys -> date to keys }
                .collect { (date, keys) ->
                    if (!isPastDate(date) && keys.isNotEmpty()) {
                        val history = (sharedPrefs.getStringSet("kunjungan_history_$date", emptySet()) ?: emptySet()).toMutableSet()
                        if (history.addAll(keys)) {
                            sharedPrefs.edit().putStringSet("kunjungan_history_$date", history).apply()
                        }
                    }
                }
        }
        viewModelScope.launch {
            combine(filterBulanLunas, selectedMenu, repository.getCustomersFlow("INTRAKOMP")) { date, menu, intra ->
                Triple(date, menu, intra)
            }.collect { (date, menu, intra) ->
                if (menu == "KUNJUNGAN" && date != "SEMUA" && date.isNotBlank() && intra.isNotEmpty()) {
                    generateKunjunganIfEmpty(date)
                }
            }
        }
        viewModelScope.launch {
            combine(filtersFlow, selectedMenu) { filters, menu -> filters to menu }
                .collect { (filters, menu) ->
                    if (!isSwitchingMenu) {
                        menuFilterCache[menu] = filters
                        saveMenuFiltersToPrefs(menu, filters)
                    }
                }
        }
    }

    val isRefreshing = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)

    fun cleanMantriNameOnly(raw: String): String {
        if (raw.isBlank() || raw.equals("SEMUA", ignoreCase = true)) return raw
        var name = raw.trim()
        if (name.equals("Blank PN", ignoreCase = true) || name.equals("TANPA NAMA", ignoreCase = true)) {
            return "Blank PN"
        }
        name = name.replace(Regex("^(?i)(pn|ao|petugas|pic)\\s*[-/:]*\\s*"), "")
        if (name.contains("-")) {
            val parts = name.split("-").map { it.trim() }
            val textPart = parts.firstOrNull { part ->
                part.any { it.isLetter() } && !part.all { it.isDigit() || it.isWhitespace() }
            } ?: parts.lastOrNull { part -> part.any { it.isLetter() } }
            if (!textPart.isNullOrBlank()) {
                name = textPart
            }
        }
        name = name.replace(Regex("^[0-9\\s-/.]+\\s*"), "")
        name = name.replace(Regex("\\([0-9\\s-]+\\)"), "")
        name = name.replace(Regex("\\[[0-9\\s-]+\\]"), "")
        name = name.replace(Regex("^[^a-zA-Z]+"), "").trim()
        return if (name.isBlank() || name.equals("TIDAK TERIDENTIFIKASI", ignoreCase = true) || name.equals("MIKRO", ignoreCase = true) || name.equals("BLANK PN", ignoreCase = true)) {
            "Blank PN"
        } else {
            name.uppercase()
        }
    }

    // Dynamic list of unique Mantris based on active menu and dashboard data (tanpa No PN)
    val availableMantris: StateFlow<List<String>> = combine(
        selectedMenu.flatMapLatest { menu ->
            val actualMenu = when (menu) {
                "KELOLAAN", "DASHBOARD" -> "INTRAKOMP"
                else -> menu
            }
            repository.getCustomersFlow(actualMenu)
        },
        mantriSummaryBulanLalu,
        mantriSummaryAkhirBulan,
        mantriSummaryDaily
    ) { customers, bln, akhir, daily ->
        val fromCust = customers.map { cleanMantriNameOnly(it.namaMantri) }.filter { it.isNotBlank() }
        val fromSummaries = (bln.keys + akhir.keys + daily.keys).map { cleanMantriNameOnly(it) }.filter { it.isNotBlank() }
        val allDistinct = (fromCust + fromSummaries).distinct()
        val nonBlank = allDistinct.filter { it != "Blank PN" }.sorted()
        val hasBlank = allDistinct.any { it == "Blank PN" }
        listWithAll(if (hasBlank) nonBlank + listOf("Blank PN") else nonBlank)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("SEMUA"))

    fun extractMonthFromDate(dateStr: String): String? {
        val clean = dateStr.trim()
        if (clean.isBlank()) return null
        
        val lower = clean.lowercase()
        when {
            lower.contains("jan") -> return "01"
            lower.contains("feb") -> return "02"
            lower.contains("mar") -> return "03"
            lower.contains("apr") -> return "04"
            lower.contains("mei") || lower.contains("may") -> return "05"
            lower.contains("jun") -> return "06"
            lower.contains("jul") -> return "07"
            lower.contains("agu") || lower.contains("ags") || lower.contains("aug") -> return "08"
            lower.contains("sep") -> return "09"
            lower.contains("okt") || lower.contains("oct") -> return "10"
            lower.contains("nov") -> return "11"
            lower.contains("des") || lower.contains("dec") -> return "12"
        }
        
        val parts = clean.split('/', '-', '.')
        if (parts.size >= 3) {
            if (parts[2].length == 4 && parts[2].all { it.isDigit() }) {
                val m = parts[1].toIntOrNull()
                if (m != null && m in 1..12) return String.format("%02d", m)
            }
            if (parts[0].length == 4 && parts[0].all { it.isDigit() }) {
                val m = parts[1].toIntOrNull()
                if (m != null && m in 1..12) return String.format("%02d", m)
            }
        }
        
        return try {
            if (clean.length >= 5) {
                val mid = clean.substring(3, 5)
                if (mid.all { it.isDigit() }) mid else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun extractYearFromDate(dateStr: String): String? {
        val clean = dateStr.trim()
        if (clean.isBlank()) return null
        val parts = clean.split('/', '-', '.')
        if (parts.size >= 3) {
            if (parts[2].length == 4 && parts[2].all { it.isDigit() }) return parts[2]
            if (parts[0].length == 4 && parts[0].all { it.isDigit() }) return parts[0]
        }
        val yearRegex = "\\b(20\\d{2}|19\\d{2})\\b".toRegex()
        val match = yearRegex.find(clean)
        if (match != null) return match.value
        return if (clean.length >= 4) {
            val right4 = clean.takeLast(4)
            if (right4.all { it.isDigit() }) right4 else null
        } else null
    }

    // Dynamic list of unique Years based on active menu's customers tglLunas
    val availableYears: StateFlow<List<String>> = selectedMenu.flatMapLatest { menu ->
        val actualMenu = if (menu == "KELOLAAN") "INTRAKOMP" else menu
        repository.getCustomersFlow(actualMenu)
    }.combine(selectedMenu) { customers, _ ->
        val years = customers.mapNotNull {
            extractYearFromDate(it.tglLunas)
        }.filter { it.length == 4 && it.all { char -> char.isDigit() } }.distinct().sorted()
        listWithAll(years)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("SEMUA"))

    // Dynamic list of unique NPDs based on active menu's customers
    val availableNpds: StateFlow<List<String>> = selectedMenu.flatMapLatest { menu ->
        val actualMenu = if (menu == "KELOLAAN" || menu == "KUNJUNGAN") "INTRAKOMP" else menu
        repository.getCustomersFlow(actualMenu)
    }.combine(selectedMenu) { customers, _ ->
        val list = listWithAll(customers.map { it.npd.trim() }.filter { it.isNotBlank() }.distinct().sortedWith(compareBy { it.toIntOrNull() ?: Int.MAX_VALUE }))
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("SEMUA"))

    val availableYearsPh: StateFlow<List<String>> = repository.getCustomersFlow("EKSTRAKOMP").map { customers ->
        val years = customers.mapNotNull {
            val tglPh = it.extraColumns.split("||")
                .firstOrNull { it.startsWith("Tgl PH::") }
                ?.substringAfter("Tgl PH::") ?: ""
            extractYearFromDate(tglPh)
        }.filter { it.length == 4 && it.all { char -> char.isDigit() } }.distinct().sorted()
        listWithAll(years)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("SEMUA"))

    val availableYearsRealisasi: StateFlow<List<String>> = repository.getCustomersFlow("EKSTRAKOMP").map { customers ->
        val years = customers.mapNotNull {
            extractYearFromDate(it.tglReal)
        }.filter { it.length == 4 && it.all { char -> char.isDigit() } }.distinct().sorted()
        listWithAll(years)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("SEMUA"))

    val availableSegmensEkstrakomp: StateFlow<List<String>> = repository.getCustomersFlow("EKSTRAKOMP").map { customers ->
        val segmens = customers.map { mapEkstrakompSegmen(it.segmen) }.filter { it.isNotBlank() }.distinct().sorted()
        listWithAll(segmens)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("SEMUA"))

    val availableDesas: StateFlow<List<String>> = combine(
        selectedMenu,
        kunjunganKeys,
        repository.getCustomersFlow("INTRAKOMP"),
        repository.getCustomersFlow("EKSTRAKOMP"),
        loggedInPN
    ) { menu, keys, intra, ekstra, pn ->
        val all = intra + ekstra
        val targetList = if (menu == "KUNJUNGAN") {
            val pool = if (pn.isNotBlank()) {
                intra.filter { 
                    it.pnMantri.trim() == pn.trim() || 
                    it.namaMantri.contains(pn, ignoreCase = true) 
                }
            } else {
                intra
            }
            if (pool.isNotEmpty()) pool else all
        } else {
            all
        }
        val desas = targetList.mapNotNull { customer ->
            customer.extraColumns.split("||")
                .firstOrNull { it.startsWith("Desa::") }
                ?.substringAfter("Desa::")?.trim()?.uppercase()
        }.filter { it.isNotBlank() }.distinct().sorted()
        listWithAll(desas)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("SEMUA"))

    // Filtered, searched, and sorted customers list
    val customersList: StateFlow<List<CustomerEntity>> = combine(
        selectedMenu.flatMapLatest { menu ->
            if (menu == "KUNJUNGAN") {
                combine(
                    repository.getCustomersFlow("INTRAKOMP"),
                    repository.getCustomersFlow("EKSTRAKOMP")
                ) { intra, ekstra -> intra + ekstra }
            } else {
                val actualMenu = if (menu == "KELOLAAN") "INTRAKOMP" else menu
                repository.getCustomersFlow(actualMenu)
            }
        },
        filtersFlow,
        combine(sortColumn, sortDirection) { col, dir -> col to dir },
        combine(loggedInPN, selectedMenu, kunjunganKeys, activeVisitDateStr) { pn, menu, keys, date ->
            CustomerListMetadata(pn, menu, keys, date)
        },
        combine(dailyDataMap, di319Rows, tindakLanjutMap) { dMap, di319, tlMap -> Triple(dMap, di319, tlMap) }
    ) { rawList, filterParams, sortParams, details, extraData ->
        val loggedInPN = details.loggedInPN
        val selectedMenu = details.selectedMenu
        val keys = details.kunjunganKeys
        val activeDate = details.activeVisitDate
        val (dMap, di319List, tlMap) = extraData
        val search = filterParams.search
        val kolek = filterParams.kolek
        val mantri = filterParams.mantri
        val bulan = filterParams.bulan
        val tahun = filterParams.tahun
        val osVal = filterParams.osVal
        val plVal = filterParams.plVal
        val tnVal = filterParams.tnVal
        val kolekUpdate = filterParams.kolekUpdate
        val npd = filterParams.npd
        val alamat = filterParams.alamat
        val setorPokok = filterParams.setorPokok
        val tusim = filterParams.tusim
        val tucil = filterParams.tucil
        val (sortCol, sortDir) = sortParams
        
        var filteredList = rawList
        
        // Filter by loggedInPN if set AND on KELOLAAN menu
        if (selectedMenu == "KELOLAAN") {
            if (loggedInPN.isNotBlank()) {
                val rawPn = loggedInPN.trim()
                val targetDigits = Regex("""\d{4,9}""").find(rawPn)?.value ?: ""
                filteredList = filteredList.filter { cust ->
                    val custPn = cust.pnMantri.trim()
                    val custDigits = custPn.filter { it.isDigit() }
                    val matchPn = when {
                        custPn.isBlank() && targetDigits.isBlank() -> false
                        custPn.equals(rawPn, ignoreCase = true) -> true
                        targetDigits.isNotBlank() && (
                            custPn == targetDigits ||
                            custDigits == targetDigits ||
                            custDigits.trimStart('0') == targetDigits.trimStart('0') ||
                            (targetDigits.length >= 5 && custDigits.endsWith(targetDigits)) ||
                            (custDigits.length >= 5 && targetDigits.endsWith(custDigits))
                        ) -> true
                        else -> false
                    }
                    val matchName = cust.namaMantri.isNotBlank() && (
                        cust.namaMantri.contains(rawPn, ignoreCase = true) ||
                        (rawPn.contains(" - ") && cust.namaMantri.contains(rawPn.substringAfter(" - ").trim(), ignoreCase = true))
                    )
                    matchPn || matchName
                }
            } else {
                filteredList = emptyList()
            }
        }

        // If KUNJUNGAN menu:
        // Normally show visit plan (keys.contains(key))
        // When kolek filter is chosen (kolek != "SEMUA"), replace visit list with debtors from kelolaan (INTRAKOMP matching loggedInPN)
        if (selectedMenu == "KUNJUNGAN") {
            val isKolekFilterActive = kolek != "SEMUA"
            if (isKolekFilterActive) {
                val kelolaanPool = rawList.filter { cust ->
                    cust.menuType == "INTRAKOMP" && (
                        loggedInPN.isBlank() ||
                        cust.pnMantri.trim() == loggedInPN.trim() ||
                        cust.namaMantri.contains(loggedInPN.trim(), ignoreCase = true) ||
                        (Regex("""\d{4,9}""").find(loggedInPN.trim())?.value?.let { p ->
                            val cDigits = cust.pnMantri.filter { it.isDigit() }
                            cDigits == p || (p.length >= 5 && cDigits.endsWith(p))
                        } ?: false)
                    )
                }
                filteredList = when (kolek) {
                    "SEMUA_KELOLAAN" -> kelolaanPool
                    "SML" -> kelolaanPool.filter { it.kolek == "SML1" || it.kolek == "SML2" || it.kolek == "SML3" }
                    "NPL" -> kelolaanPool.filter { it.kolek == "Kurang Lancar" || it.kolek == "Diragukan" || it.kolek == "Macet" }
                    "Lancar" -> kelolaanPool.filter { it.kolek.equals("Lancar", ignoreCase = true) || it.kolek.equals("L", ignoreCase = true) }
                    "SML1" -> kelolaanPool.filter { it.kolek.equals("SML1", ignoreCase = true) || it.kolek.equals("SML 1", ignoreCase = true) }
                    "SML2" -> kelolaanPool.filter { it.kolek.equals("SML2", ignoreCase = true) || it.kolek.equals("SML 2", ignoreCase = true) }
                    "SML3" -> kelolaanPool.filter { it.kolek.equals("SML3", ignoreCase = true) || it.kolek.equals("SML 3", ignoreCase = true) }
                    "Kurang Lancar" -> kelolaanPool.filter { it.kolek.contains("Kurang", ignoreCase = true) || it.kolek.equals("KL", ignoreCase = true) }
                    "Diragukan" -> kelolaanPool.filter { it.kolek.contains("Diragukan", ignoreCase = true) || it.kolek.equals("D", ignoreCase = true) }
                    "Macet" -> kelolaanPool.filter { it.kolek.contains("Macet", ignoreCase = true) || it.kolek.equals("M", ignoreCase = true) }
                    else -> kelolaanPool.filter { it.kolek.equals(kolek, ignoreCase = true) }
                }
            } else {
                filteredList = filteredList.filter { item ->
                    val key = if (item.noRekening.isNotBlank()) item.noRekening else if (item.cif.isNotBlank()) item.cif else item.namaNasabah
                    keys.contains(key)
                }
            }
        }

        // 1. Search filter (Nama Nasabah, Nama Mantri, CIF, or Alamat)
        // Skip filtering visits table on KUNJUNGAN since the search bar is used as a popup selector to add customers
        if (search.isNotBlank() && selectedMenu != "KUNJUNGAN") {
            val q = search.trim().lowercase()
            filteredList = filteredList.filter {
                it.namaNasabah.lowercase().contains(q) || 
                it.namaMantri.lowercase().contains(q) ||
                it.cif.lowercase().contains(q) ||
                it.alamat.lowercase().contains(q)
            }
        }

        // 1b. NPD filter (specifically for KELOLAAN / KUNJUNGAN menu or general)
        if (npd != "SEMUA" && npd.isNotBlank()) {
            if (npd.startsWith("MULTI:")) {
                val selectedDates = npd.substringAfter("MULTI:").split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
                filteredList = filteredList.filter { selectedDates.contains(it.npd.trim().lowercase()) }
            } else {
                val qNpd = npd.trim().lowercase()
                filteredList = filteredList.filter { it.npd.trim().lowercase() == qNpd }
            }
        }

        // 1c. Alamat explicit filter input
        if (alamat.isNotBlank()) {
            val qAlamat = alamat.trim().lowercase()
            filteredList = filteredList.filter { it.alamat.lowercase().contains(qAlamat) }
        }

        // 1d. Setor Pokok filter (Filters customer whose principal loan has decreased)
        if (setorPokok) {
            filteredList = filteredList.filter { cust ->
                val dailyCust = dMap[cust.noRekening]
                dailyCust != null && dailyCust.osPinjaman < cust.osPinjaman
            }
        }

        // 1e. Tusim & Tucil filters (diatur kemudian - can be toggled but currently does not filter out any data)
        if (tusim) {
            val normalizeCifLocal = { rawCif: String ->
                var str = rawCif.trim().uppercase()
                if (str.startsWith("CIF")) {
                    str = str.substring(3).trim()
                }
                if (str.startsWith("-")) {
                    str = str.substring(1).trim()
                }
                str = str.filter { it.isLetterOrDigit() }
                str = str.dropWhile { it == '0' }
                str
            }
            
            filteredList = filteredList.filter { cust ->
                val dailyCust = dMap[cust.noRekening]
                val currentTunggakan = dailyCust?.totalTunggakan ?: cust.totalTunggakan
                if (currentTunggakan <= 0.0) {
                    false
                } else {
                    val custNormCif = normalizeCifLocal(cust.cif)
                    if (custNormCif.isBlank()) {
                        false
                    } else {
                        val custSavings = di319List.filter { 
                            val rowNorm = normalizeCifLocal(it.cif)
                            rowNorm.isNotEmpty() && rowNorm == custNormCif 
                        }.sortedByDescending { it.saldo }
                        
                        val saving1Saldo = custSavings.getOrNull(0)?.saldo ?: 0.0
                        val saving2Saldo = custSavings.getOrNull(1)?.saldo ?: 0.0
                        
                        (saving1Saldo > 25000.0 || saving2Saldo > 25000.0)
                    }
                }
            }
        }
        if (tucil) {
            filteredList = filteredList.filter { cust ->
                val dailyCust = dMap[cust.noRekening]
                val currentTunggakan = dailyCust?.totalTunggakan ?: cust.totalTunggakan
                currentTunggakan >= 1.0 && currentTunggakan <= 100000.0
            }
        }

        // 1f. Jatuh Tempo Only filter based on active visit date and H-x (0..7)
        if (filterParams.jatuhTempoOnly) {
            val targetDay = getTargetNpdDay(activeDate, filterParams.jatuhTempoH)
            filteredList = filteredList.filter { cust ->
                val d = cust.npd.trim().toIntOrNull() ?: cust.tglLunas.trim().split('/', '-', '.').firstOrNull()?.toIntOrNull()
                d == targetDay
            }
        }

        // 2. Kolek filter (skip KUNJUNGAN as it was already handled with kelolaan replacement)
        if (kolek != "SEMUA" && selectedMenu != "KUNJUNGAN") {
            filteredList = when (kolek) {
                "SML" -> filteredList.filter { it.kolek == "SML1" || it.kolek == "SML2" || it.kolek == "SML3" }
                "NPL" -> filteredList.filter { it.kolek == "Kurang Lancar" || it.kolek == "Diragukan" || it.kolek == "Macet" }
                else -> filteredList.filter { it.kolek == kolek }
            }
        }

        // 2a. Kolek Update filter
        if (kolekUpdate != "SEMUA") {
            fun parseKolekCodeAndRank(k: String): Pair<String, Int> {
                val s = k.trim().uppercase()
                return when {
                    s == "L" || s == "LANCAR" -> "L" to 1
                    s == "SML1" || s == "SML 1" || s == "SML-1" -> "SML1" to 2
                    s == "SML2" || s == "SML 2" || s == "SML-2" -> "SML2" to 3
                    s == "SML3" || s == "SML 3" || s == "SML-3" -> "SML3" to 4
                    s == "KL" || s == "KURANG LANCAR" -> "KL" to 5
                    s == "D" || s == "DIRAGUKAN" -> "D" to 6
                    s == "M" || s == "MACET" -> "M" to 7
                    else -> "" to 99
                }
            }

            filteredList = filteredList.filter { cust ->
                val rawRek = cust.noRekening.trim()
                val cr = rawRek.filter { it.isDigit() }
                val noZero = cr.trimStart('0')
                val formattedRek = com.example.data.network.SpreadsheetService.formatRekening15(cust.noRekening)
                val rawCif = cust.cif.trim()
                val cc = rawCif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }

                val dailyCust = dMap[rawRek]
                    ?: (if (cr.isNotBlank()) dMap[cr] else null)
                    ?: (if (noZero.isNotBlank()) dMap[noZero] else null)
                    ?: (if (cr.length >= 15) dMap[cr.takeLast(15)] else null)
                    ?: (if (formattedRek.isNotBlank()) dMap[formattedRek] else null)
                    ?: (if (rawCif.isNotBlank()) dMap[rawCif] else null)
                    ?: (if (cc.isNotBlank()) dMap[cc] ?: dMap["CIF-$cc"] else null)

                val hasNoDaily = dailyCust == null || dailyCust.kolek.isBlank() || dailyCust.osPinjaman <= 0.0

                val normalizedKolekUpdate = kolekUpdate.trim().uppercase()
                val custKey = if (cust.noRekening.isNotBlank()) cust.noRekening else if (cust.cif.isNotBlank()) cust.cif else cust.namaNasabah
                val tlGen = tlMap[custKey]?.trim() ?: ""
                val tlKunj = tlMap["kunjungan_$custKey"]?.trim() ?: ""

                when (normalizedKolekUpdate) {
                    "TETAP SML" -> tlGen.equals("Tetap SML", ignoreCase = true) || tlKunj.equals("Tetap SML", ignoreCase = true)
                    "BERGESER NPL" -> tlGen.equals("Bergeser NPL", ignoreCase = true) || tlKunj.equals("Bergeser NPL", ignoreCase = true)
                    "LUNAS" -> {
                        val (_, oldRank) = parseKolekCodeAndRank(cust.kolek)
                        hasNoDaily && oldRank in 1..6
                    }
                    "DH" -> {
                        val (_, oldRank) = parseKolekCodeAndRank(cust.kolek)
                        hasNoDaily && oldRank == 7
                    }
                    else -> {
                        val parts = when {
                            normalizedKolekUpdate.contains("➔") -> normalizedKolekUpdate.split("➔")
                            normalizedKolekUpdate.contains("->") -> normalizedKolekUpdate.split("->")
                            normalizedKolekUpdate.contains("-") && !normalizedKolekUpdate.startsWith("SML") -> normalizedKolekUpdate.split("-")
                            else -> null
                        }
                        if (parts != null && parts.size == 2) {
                            val reqOld = parts[0].trim()
                            val reqNew = parts[1].trim()
                            val (custOldCode, _) = if (dailyCust != null) {
                                parseKolekCodeAndRank(cust.kolek)
                            } else {
                                parseKolekCodeAndRank(cust.kolekBlnLalu.ifBlank { cust.kolek })
                            }
                            val (custNewCode, _) = if (dailyCust != null) {
                                parseKolekCodeAndRank(dailyCust.kolek)
                            } else {
                                parseKolekCodeAndRank(cust.kolek)
                            }
                            custOldCode.equals(reqOld, ignoreCase = true) && custNewCode.equals(reqNew, ignoreCase = true)
                        } else {
                            true
                        }
                    }
                }
            }
        }

        // 2a-2. Kolek Bulan Lalu filter
        val kolekBlnLaluFilter = filterParams.kolekBlnLalu
        if (kolekBlnLaluFilter != "SEMUA") {
            val isKolekL = { k: String ->
                val s = k.trim().uppercase()
                s == "L" || s == "LANCAR"
            }
            val isKolekSml1 = { k: String ->
                val s = k.trim().uppercase()
                s == "SML1" || s == "SML 1" || s == "SML-1"
            }
            val isKolekSml2 = { k: String ->
                val s = k.trim().uppercase()
                s == "SML2" || s == "SML 2" || s == "SML-2"
            }
            val isKolekSml3 = { k: String ->
                val s = k.trim().uppercase()
                s == "SML3" || s == "SML 3" || s == "SML-3"
            }
            val isKolekKl = { k: String ->
                val s = k.trim().uppercase()
                s == "KL" || s == "KURANG LANCAR"
            }
            val isKolekD = { k: String ->
                val s = k.trim().uppercase()
                s == "D" || s == "DIRAGUKAN"
            }
            val isKolekM = { k: String ->
                val s = k.trim().uppercase()
                s == "M" || s == "MACET"
            }

            filteredList = filteredList.filter { cust ->
                val oldK = cust.kolekBlnLalu
                val newK = cust.kolek
                when (kolekBlnLaluFilter) {
                    "L ➔ SML1" -> isKolekL(oldK) && isKolekSml1(newK)
                    "SML1 ➔ SML2" -> isKolekSml1(oldK) && isKolekSml2(newK)
                    "SML2 ➔ SML3" -> isKolekSml2(oldK) && isKolekSml3(newK)
                    "SML3 ➔ KL" -> isKolekSml3(oldK) && isKolekKl(newK)
                    "KL ➔ D" -> isKolekKl(oldK) && isKolekD(newK)
                    "D ➔ M" -> isKolekD(oldK) && isKolekM(newK)
                    else -> true
                }
            }
        }

        // 2b. Segmen filter
        val seg = filterParams.segmen
        if (seg != "SEMUA") {
            filteredList = filteredList.filter { 
                val currentSeg = if (selectedMenu == "EKSTRAKOMP" || it.menuType == "EKSTRAKOMP") mapEkstrakompSegmen(it.segmen) else it.segmen
                currentSeg.trim().uppercase() == seg.trim().uppercase()
            }
        }

        // 3. Mantri filter
        if (selectedMenu != "KELOLAAN" && selectedMenu != "KUNJUNGAN" && mantri != "SEMUA") {
            val targetClean = cleanMantriNameOnly(mantri)
            filteredList = filteredList.filter { item ->
                val itemClean = cleanMantriNameOnly(item.namaMantri)
                if (targetClean == "Blank PN") {
                    item.namaMantri.isBlank() ||
                    item.namaMantri.equals("Blank PN", ignoreCase = true) ||
                    item.namaMantri.equals("TIDAK TERIDENTIFIKASI", ignoreCase = true) ||
                    item.namaMantri.equals("TANPA NAMA", ignoreCase = true) ||
                    itemClean == "Blank PN"
                } else {
                    itemClean.equals(targetClean, ignoreCase = true) ||
                    item.namaMantri.equals(mantri, ignoreCase = true)
                }
            }
        }

        // 4. Bulan Lunas filter
        if (bulan != "SEMUA") {
            filteredList = if (selectedMenu == "KUNJUNGAN") {
                filteredList
            } else if (selectedMenu == "EKSTRAKOMP") {
                filteredList.filter {
                    val tglPh = it.extraColumns.split("||")
                        .firstOrNull { part -> part.startsWith("Tgl PH::") }
                        ?.substringAfter("Tgl PH::") ?: ""
                    extractYearFromDate(tglPh) == bulan
                }
            } else {
                val monthInt = bulan.toIntOrNull()
                val isMonthSourceLoaded = (monthInt != null && selectedPeriodSource.value == "BULAN_$monthInt")
                if (isMonthSourceLoaded) {
                    filteredList
                } else {
                    filteredList.filter {
                        extractMonthFromDate(it.tglLunas) == bulan
                    }
                }
            }
        }

        // 5. Tahun Lunas filter
        if (tahun != "SEMUA") {
            filteredList = if (selectedMenu == "KUNJUNGAN") {
                filteredList.filter { item ->
                    val itemDesa = item.extraColumns.split("||")
                        .firstOrNull { it.startsWith("Desa::") }
                        ?.substringAfter("Desa::")?.trim()?.uppercase() ?: ""
                    itemDesa == tahun.uppercase()
                }
            } else if (selectedMenu == "EKSTRAKOMP") {
                filteredList.filter {
                    extractYearFromDate(it.tglReal) == tahun
                }
            } else {
                filteredList.filter {
                    extractYearFromDate(it.tglLunas) == tahun
                }
            }
        }

        // 5b. Desa filter
        val desa = filterParams.desa
        if (desa != "SEMUA" && desa.isNotBlank()) {
            filteredList = filteredList.filter { item ->
                val itemDesa = item.extraColumns.split("||")
                    .firstOrNull { it.startsWith("Desa::") }
                    ?.substringAfter("Desa::")?.trim()?.uppercase() ?: ""
                itemDesa == desa.uppercase()
            }
        }

        // 6. OS Percentage filter (<25%, <50%, <75%)
        if (osVal != "SEMUA") {
            filteredList = if (selectedMenu == "EKSTRAKOMP") {
                filteredList.filter {
                    val sum = it.osPinjaman + it.pinalty
                    when (osVal) {
                        "< 1 Juta" -> sum < 1_000_000.0
                        "< 5 Juta (1-5 jt)" -> sum >= 1_000_000.0 && sum <= 5_000_000.0
                        "< 10 Juta (>5-10 jt)" -> sum > 5_000_000.0 && sum <= 10_000_000.0
                        "< 25 Juta (>10-25 jt)" -> sum > 10_000_000.0 && sum <= 25_000_000.0
                        "< 50 Juta (>25-50 Jt)" -> sum > 25_000_000.0 && sum <= 50_000_000.0
                        "< 100 Juta (>50-100 jt)" -> sum > 50_000_000.0 && sum <= 100_000_000.0
                        "> 100 Juta" -> sum > 100_000_000.0
                        else -> true
                    }
                }
            } else {
                filteredList.filter {
                    val ratio = if (it.plafond > 0.0) (it.osPinjaman / it.plafond) else 0.0
                    when (osVal) {
                        "<25%" -> ratio < 0.25
                        "<50%" -> ratio < 0.50
                        "<75%" -> ratio < 0.75
                        else -> true
                    }
                }
            }
        }

        // 7. Plafond Range filter (s.d 25 Juta, >25 - 50 Juta, >50 - 100 Juta, >100 Juta)
        if (plVal != "SEMUA") {
            filteredList = filteredList.filter {
                val amt = it.plafond
                when (plVal) {
                    "s.d 25 Juta" -> amt <= 25_000_000.0
                    ">25 - 50 Juta" -> amt > 25_000_000.0 && amt <= 50_000_000.0
                    ">50 - 100 Juta" -> amt > 50_000_000.0 && amt <= 100_000_000.0
                    ">100 Juta" -> amt > 100_000_000.0
                    else -> true
                }
            }
        }

        // 8. Tenor Range filter (<6 Bulan, <12 Bulan, > 12 Bulan, 1/2 Jalan)
        if (tnVal != "SEMUA") {
            filteredList = filteredList.filter {
                val elapsed = getMonthsElapsedSince(it.tglReal)
                val remaining = it.tenor - elapsed
                when (tnVal) {
                    "<6 Bulan" -> remaining < 6
                    "<12 Bulan" -> remaining < 12
                    "> 12 Bulan" -> elapsed > 12
                    "1/2 Jalan" -> {
                        val limit = it.tenor * 0.5f
                        elapsed >= limit || it.osPinjaman <= it.plafond * 0.5
                    }
                    else -> true
                }
            }
        }

        // 9. Sort Columns
        filteredList = when (sortCol) {
            SortColumn.NAMA_NASABAH -> {
                if (sortDir == SortDirection.ASC) filteredList.sortedBy { it.namaNasabah }
                else filteredList.sortedByDescending { it.namaNasabah }
            }
            SortColumn.PLAFOND -> {
                if (sortDir == SortDirection.ASC) filteredList.sortedBy { it.plafond }
                else filteredList.sortedByDescending { it.plafond }
            }
            SortColumn.OS_PINJAMAN -> {
                if (sortDir == SortDirection.ASC) filteredList.sortedBy { it.osPinjaman }
                else filteredList.sortedByDescending { it.osPinjaman }
            }
            SortColumn.UPDATE_POKOK -> {
                if (sortDir == SortDirection.ASC) filteredList.sortedBy { it.updatePokok }
                else filteredList.sortedByDescending { it.updatePokok }
            }
            SortColumn.SETOR_POKOK -> {
                if (sortDir == SortDirection.ASC) filteredList.sortedBy { it.setorPokok }
                else filteredList.sortedByDescending { it.setorPokok }
            }
            SortColumn.SEGMEN -> {
                if (sortDir == SortDirection.ASC) filteredList.sortedBy { it.segmen }
                else filteredList.sortedByDescending { it.segmen }
            }
            SortColumn.KOLEK -> {
                if (sortDir == SortDirection.ASC) filteredList.sortedBy { it.kolek }
                else filteredList.sortedByDescending { it.kolek }
            }
            SortColumn.UMUR_TGK -> {
                if (sortDir == SortDirection.ASC) filteredList.sortedBy { it.umurTgk }
                else filteredList.sortedByDescending { it.umurTgk }
            }
            SortColumn.KOL_BY_LW -> {
                if (sortDir == SortDirection.ASC) filteredList.sortedBy { it.kolByLw }
                else filteredList.sortedByDescending { it.kolByLw }
            }
            SortColumn.NAMA_MANTRI -> {
                if (sortDir == SortDirection.ASC) filteredList.sortedBy { it.namaMantri }
                else filteredList.sortedByDescending { it.namaMantri }
            }
            SortColumn.NPD -> {
                if (sortDir == SortDirection.ASC) filteredList.sortedBy { it.npd.toIntOrNull() ?: Int.MAX_VALUE }
                else filteredList.sortedByDescending { it.npd.toIntOrNull() ?: Int.MIN_VALUE }
            }
            SortColumn.ALAMAT -> {
                if (sortDir == SortDirection.ASC) filteredList.sortedBy { it.alamat }
                else filteredList.sortedByDescending { it.alamat }
            }
            SortColumn.DESA -> {
                if (sortDir == SortDirection.ASC) filteredList.sortedBy { it.desa.uppercase() }
                else filteredList.sortedByDescending { it.desa.uppercase() }
            }
            SortColumn.ALIH_TAGIH -> {
                if (sortDir == SortDirection.ASC) filteredList.sortedBy { it.pinalty }
                else filteredList.sortedByDescending { it.pinalty }
            }
            SortColumn.SISA_BUNGA -> {
                if (sortDir == SortDirection.ASC) filteredList.sortedBy { it.tunggakanBunga }
                else filteredList.sortedByDescending { it.tunggakanBunga }
            }
            SortColumn.TOTAL_DH -> {
                if (sortDir == SortDirection.ASC) filteredList.sortedBy { it.totalTunggakan }
                else filteredList.sortedByDescending { it.totalTunggakan }
            }
        }

        filteredList
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getTargetNpdDay(activeDateStr: String, hOffset: Int): Int {
        val cal = java.util.Calendar.getInstance()
        try {
            val parts = activeDateStr.split("/")
            if (parts.size >= 3) {
                val d = parts[0].toInt()
                val m = parts[1].toInt() - 1
                val y = parts[2].toInt()
                cal.set(y, m, d)
            }
        } catch (e: Exception) {}
        cal.add(java.util.Calendar.DAY_OF_MONTH, hOffset)
        return cal.get(java.util.Calendar.DAY_OF_MONTH)
    }

    val dueHCounts: StateFlow<Map<Int, Int>> = combine(
        selectedMenu.flatMapLatest { menu ->
            if (menu == "KUNJUNGAN") {
                combine(
                    repository.getCustomersFlow("INTRAKOMP"),
                    repository.getCustomersFlow("EKSTRAKOMP")
                ) { intra, ekstra -> intra + ekstra }
            } else {
                val actualMenu = if (menu == "KELOLAAN") "INTRAKOMP" else menu
                repository.getCustomersFlow(actualMenu)
            }
        },
        filtersFlow,
        combine(loggedInPN, selectedMenu, kunjunganKeys, activeVisitDateStr) { pn, menu, keys, date ->
            CustomerListMetadata(pn, menu, keys, date)
        }
    ) { rawList, filterParams, details ->
        val activeDate = details.activeVisitDate
        val selectedMenu = details.selectedMenu
        val loggedInPN = details.loggedInPN
        val keys = details.kunjunganKeys

        var baseList = if (selectedMenu == "KELOLAAN" && loggedInPN.isNotBlank()) {
            val targetDigits = Regex("""\d{4,9}""").find(loggedInPN.trim())?.value ?: ""
            rawList.filter { item ->
                val custPn = item.pnMantri.trim()
                val custDigits = custPn.filter { it.isDigit() }
                when {
                    custPn.isBlank() && targetDigits.isBlank() -> false
                    custPn.equals(loggedInPN, ignoreCase = true) -> true
                    targetDigits.isNotBlank() && (
                        custPn == targetDigits ||
                        custDigits == targetDigits ||
                        custDigits.trimStart('0') == targetDigits.trimStart('0') ||
                        (targetDigits.length >= 5 && custDigits.endsWith(targetDigits)) ||
                        (custDigits.length >= 5 && targetDigits.endsWith(custDigits))
                    ) -> true
                    else -> item.namaMantri.contains(loggedInPN, ignoreCase = true)
                }
            }
        } else if (selectedMenu == "KUNJUNGAN") {
            val isKolekFilterActive = filterParams.kolek != "SEMUA"
            if (isKolekFilterActive) {
                val kelolaanPool = rawList.filter { cust ->
                    cust.menuType == "INTRAKOMP" && (
                        loggedInPN.isBlank() ||
                        cust.pnMantri.trim() == loggedInPN.trim() ||
                        cust.namaMantri.contains(loggedInPN.trim(), ignoreCase = true) ||
                        (Regex("""\d{4,9}""").find(loggedInPN.trim())?.value?.let { p ->
                            val cDigits = cust.pnMantri.filter { it.isDigit() }
                            cDigits == p || (p.length >= 5 && cDigits.endsWith(p))
                        } ?: false)
                    )
                }
                when (filterParams.kolek) {
                    "SEMUA_KELOLAAN" -> kelolaanPool
                    "SML" -> kelolaanPool.filter { it.kolek == "SML1" || it.kolek == "SML2" || it.kolek == "SML3" }
                    "NPL" -> kelolaanPool.filter { it.kolek == "Kurang Lancar" || it.kolek == "Diragukan" || it.kolek == "Macet" }
                    "Lancar" -> kelolaanPool.filter { it.kolek.equals("Lancar", ignoreCase = true) || it.kolek.equals("L", ignoreCase = true) }
                    "SML1" -> kelolaanPool.filter { it.kolek.equals("SML1", ignoreCase = true) || it.kolek.equals("SML 1", ignoreCase = true) }
                    "SML2" -> kelolaanPool.filter { it.kolek.equals("SML2", ignoreCase = true) || it.kolek.equals("SML 2", ignoreCase = true) }
                    "SML3" -> kelolaanPool.filter { it.kolek.equals("SML3", ignoreCase = true) || it.kolek.equals("SML 3", ignoreCase = true) }
                    "Kurang Lancar" -> kelolaanPool.filter { it.kolek.contains("Kurang", ignoreCase = true) || it.kolek.equals("KL", ignoreCase = true) }
                    "Diragukan" -> kelolaanPool.filter { it.kolek.contains("Diragukan", ignoreCase = true) || it.kolek.equals("D", ignoreCase = true) }
                    "Macet" -> kelolaanPool.filter { it.kolek.contains("Macet", ignoreCase = true) || it.kolek.equals("M", ignoreCase = true) }
                    else -> kelolaanPool.filter { it.kolek.equals(filterParams.kolek, ignoreCase = true) }
                }
            } else {
                rawList.filter { item ->
                    val key = if (item.noRekening.isNotBlank()) item.noRekening else if (item.cif.isNotBlank()) item.cif else item.namaNasabah
                    keys.contains(key)
                }
            }
        } else {
            rawList
        }

        if (filterParams.search.isNotBlank() && selectedMenu != "KUNJUNGAN") {
            val q = filterParams.search.trim().lowercase()
            baseList = baseList.filter {
                it.namaNasabah.lowercase().contains(q) ||
                it.namaMantri.lowercase().contains(q) ||
                it.cif.lowercase().contains(q) ||
                it.alamat.lowercase().contains(q) ||
                it.noRekening.lowercase().contains(q)
            }
        }

        if (selectedMenu != "KELOLAAN" && selectedMenu != "KUNJUNGAN" && filterParams.mantri != "SEMUA") {
            val targetClean = cleanMantriNameOnly(filterParams.mantri)
            baseList = baseList.filter { item ->
                val itemClean = cleanMantriNameOnly(item.namaMantri)
                if (targetClean == "Blank PN") {
                    item.namaMantri.isBlank() ||
                    item.namaMantri.equals("TANPA NAMA", ignoreCase = true) ||
                    item.namaMantri.equals("BLANK PN", ignoreCase = true) ||
                    item.pnMantri.isBlank() ||
                    itemClean == "Blank PN"
                } else {
                    itemClean.equals(targetClean, ignoreCase = true)
                }
            }
        }

        (0..7).associateWith { h ->
            val targetDay = getTargetNpdDay(activeDate, h)
            baseList.count { cust ->
                val d = cust.npd.trim().toIntOrNull() ?: cust.tglLunas.trim().split('/', '-', '.').firstOrNull()?.toIntOrNull()
                d == targetDay
            }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), (0..7).associateWith { 0 })

    val dueTodayCount: StateFlow<Int> = dueHCounts.map { it[0] ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Summary statistics block
    val summaryStats = customersList.combine(selectedMenu) { list, _ ->
        val count = list.size
        val totalPlafond = list.sumOf { it.plafond }
        val totalOs = list.sumOf { it.osPinjaman }
        
        val isKl = { k: String -> val c = k.trim().uppercase(); c == "KL" || c == "KURANG LANCAR" || c.contains("KURANG") || c == "3" }
        val isD = { k: String -> val c = k.trim().uppercase(); c == "DIRAGUKAN" || c == "D" || c.contains("DIRAGUKAN") || c == "4" }
        val isM = { k: String -> val c = k.trim().uppercase(); c == "MACET" || c == "M" || c.contains("MACET") || c == "5" }
        val klCount = list.count { isKl(it.kolek) }
        val dCount = list.count { isD(it.kolek) }
        val mCount = list.count { isM(it.kolek) }
        val nplCount = klCount + dCount + mCount
        
        val isLancar = { k: String -> val c = k.trim().uppercase(); c == "LANCAR" || c == "L" || c == "1" || (c.contains("LANCAR") && !c.contains("KURANG")) }
        val lancarCount = list.count { isLancar(it.kolek) }
        val sml1Count = list.count { val c = it.kolek.trim().uppercase(); c == "SML1" || c == "SML 1" || c == "SML-1" }
        val sml2Count = list.count { val c = it.kolek.trim().uppercase(); c == "SML2" || c == "SML 2" || c == "SML-2" }
        val sml3Count = list.count { val c = it.kolek.trim().uppercase(); c == "SML3" || c == "SML 3" || c == "SML-3" }
        val smlTotalCount = sml1Count + sml2Count + sml3Count

        val osLancar = list.filter { isLancar(it.kolek) }.sumOf { it.osPinjaman }
        val osSml1 = list.filter { val c = it.kolek.trim().uppercase(); c == "SML1" || c == "SML 1" || c == "SML-1" }.sumOf { it.osPinjaman }
        val osSml2 = list.filter { val c = it.kolek.trim().uppercase(); c == "SML2" || c == "SML 2" || c == "SML-2" }.sumOf { it.osPinjaman }
        val osSml3 = list.filter { val c = it.kolek.trim().uppercase(); c == "SML3" || c == "SML 3" || c == "SML-3" }.sumOf { it.osPinjaman }
        val osSmlTotal = osSml1 + osSml2 + osSml3

        val osKl = list.filter { isKl(it.kolek) }.sumOf { it.osPinjaman }
        val osD = list.filter { isD(it.kolek) }.sumOf { it.osPinjaman }
        val osM = list.filter { isM(it.kolek) }.sumOf { it.osPinjaman }
        val osNplTotal = osKl + osD + osM

        // Segment grouping across all active customers
        val segmentGroups = list.groupBy { it.segmen.trim().uppercase().ifBlank { "LAINNYA" } }
            .map { (name, group) ->
                name to (group.sumOf { it.osPinjaman } to group.size)
            }
            .sortedByDescending { it.second.first }

        val seg1Name = segmentGroups.getOrNull(0)?.first ?: "KUPEDES"
        val seg1Os = segmentGroups.getOrNull(0)?.second?.first ?: 0.0
        val seg1Count = segmentGroups.getOrNull(0)?.second?.second ?: 0
        
        val seg2Name = segmentGroups.getOrNull(1)?.first ?: "KUR"
        val seg2Os = segmentGroups.getOrNull(1)?.second?.first ?: 0.0
        val seg2Count = segmentGroups.getOrNull(1)?.second?.second ?: 0

        val seg3Name = segmentGroups.getOrNull(2)?.first ?: "BRIGUNA"
        val seg3Os = segmentGroups.getOrNull(2)?.second?.first ?: 0.0
        val seg3Count = segmentGroups.getOrNull(2)?.second?.second ?: 0

        val allSegmentsList = list.groupBy { it.segmen.trim().uppercase().ifBlank { "LAINNYA" } }
            .map { (name, group) ->
                Triple(name, group.sumOf { it.osPinjaman }, group.size)
            }
            .sortedByDescending { it.second }

        Summary(
            totalCount = count,
            totalPlafond = totalPlafond,
            totalOs = totalOs,
            nplCount = nplCount,
            lancarCount = lancarCount,
            sml1Count = sml1Count,
            sml2Count = sml2Count,
            sml3Count = sml3Count,
            osLancar = osLancar,
            osSmlTotal = osSmlTotal,
            smlTotalCount = smlTotalCount,
            osSml1 = osSml1,
            osSml2 = osSml2,
            osSml3 = osSml3,
            osNplTotal = osNplTotal,
            osKl = osKl,
            klCount = klCount,
            osD = osD,
            dCount = dCount,
            osM = osM,
            mCount = mCount,
            seg1Name = seg1Name,
            seg1Os = seg1Os,
            seg1Count = seg1Count,
            seg2Name = seg2Name,
            seg2Os = seg2Os,
            seg2Count = seg2Count,
            seg3Name = seg3Name,
            seg3Os = seg3Os,
            seg3Count = seg3Count,
            allSegments = allSegmentsList
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Summary(0, 0.0, 0.0, 0, 0, 0, 0, 0))

    // Helper flow that aggregates both types of database customers
    val allCustomersList: StateFlow<List<CustomerEntity>> = combine(
        repository.getCustomersFlow("INTRAKOMP"),
        repository.getCustomersFlow("EKSTRAKOMP")
    ) { intra, ekstra ->
        intra + ekstra
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val simpananSortColumn = MutableStateFlow<String>(
        sharedPrefs.getString("last_simpanan_sort_column", "NONE") ?: "NONE"
    )
    val simpananSortDirection = MutableStateFlow<SortDirection>(
        try {
            val saved = sharedPrefs.getString("last_simpanan_sort_direction", null)
            if (saved != null) SortDirection.valueOf(saved) else SortDirection.ASC
        } catch (e: Exception) {
            SortDirection.ASC
        }
    )

    fun toggleSimpananSort(column: String) {
        if (simpananSortColumn.value == column) {
            simpananSortDirection.value = if (simpananSortDirection.value == SortDirection.ASC) {
                SortDirection.DESC
            } else {
                SortDirection.ASC
            }
        } else {
            simpananSortColumn.value = column
            simpananSortDirection.value = SortDirection.DESC
        }
        sharedPrefs.edit()
            .putString("last_simpanan_sort_column", simpananSortColumn.value)
            .putString("last_simpanan_sort_direction", simpananSortDirection.value.name)
            .apply()
    }

    // Simpanan tab data flow
    val simpananList: StateFlow<List<SimpananRow>> = combine(
        di319Rows,
        allCustomersList,
        searchQuery,
        simpananSortColumn,
        simpananSortDirection
    ) { di319List, allCustomers, search, sortCol, sortDir ->
        // Helper to normalize a CIF to unique matchable key
        val getNormalizedCif = { raw: String ->
            var str = raw.trim().uppercase()
            if (str.startsWith("CIF")) {
                str = str.substring(3).trim()
            }
            if (str.startsWith("-")) {
                str = str.substring(1).trim()
            }
            str = str.filter { it.isLetterOrDigit() }
            str = str.dropWhile { it == '0' }
            str
        }

        // Map all customers in LW321 (INTRAKOMP) and LW325 (EKSTRAKOMP) to their normalized keys separately
        val intraCustomers = allCustomers.filter { it.menuType == "INTRAKOMP" }
        val ekstraCustomers = allCustomers.filter { it.menuType == "EKSTRAKOMP" }

        val intraCifsSet = intraCustomers.map { getNormalizedCif(it.cif) }.filter { it.isNotEmpty() }.toSet()
        val ekstraCifsSet = ekstraCustomers.map { getNormalizedCif(it.cif) }.filter { it.isNotEmpty() }.toSet()

        val rows = di319List.map { item ->
            val itemNorm = getNormalizedCif(item.cif)
            
            // Perform robust lookup:
            // 1. Check if the CIF from the savings row exists in the LW321 INTRAKOMP customer database
            var hasLoan = itemNorm.isNotEmpty() && itemNorm in intraCifsSet
            
            // 2. If not matched (does not display "1"), search for matched CIF in LW325 Akhir Bulan (EKSTRAKOMP)
            if (!hasLoan) {
                hasLoan = itemNorm.isNotEmpty() && itemNorm in ekstraCifsSet
            }
            
            val kode1 = if (hasLoan) "1" else ""

            SimpananRow(
                cif = item.cif,
                namaNasabah = item.namaNasabah,
                noTabungan = item.noRekening,
                balance = item.saldo,
                pinjaman = 0.0,
                kode1 = kode1,
                shortBalance = "",
                shortCif = "",
                shortNama = "",
                shortNoTabungan = "",
                kode2 = "",
                rumus1 = "",
                rumus2 = 0.0,
                rumus3 = "",
                rumus4 = ""
            )
        }
        
        val filtered = if (search.isNotBlank()) {
            val terms = search.trim().lowercase().split("\\s+".toRegex())
            rows.filter { row ->
                terms.all { term ->
                    row.cif.lowercase().contains(term) ||
                    row.namaNasabah.lowercase().contains(term) ||
                    row.noTabungan.lowercase().contains(term)
                }
            }
        } else {
            rows
        }

        if (sortCol != "NONE") {
            when (sortCol) {
                "SALDO" -> {
                    if (sortDir == SortDirection.ASC) filtered.sortedBy { it.balance }
                    else filtered.sortedByDescending { it.balance }
                }
                "KODE1" -> {
                    if (sortDir == SortDirection.ASC) filtered.sortedBy { it.kode1 }
                    else filtered.sortedByDescending { it.kode1 }
                }
                else -> filtered
            }
        } else {
            filtered
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Set default URLs if empty
        if (url1.value.isBlank() || url1.value.contains("1aGEOYQv1bLzzlYVd")) {
            saveLink("url_1", "https://docs.google.com/spreadsheets/d/1Uil_mrHwe-Yz7e1cHBthZNrkl99AyLsu/edit?usp=drive_link&ouid=112982864281396524406&rtpof=true&sd=true")
        }
        if (url2.value.isBlank()) {
            saveLink("url_2", "https://docs.google.com/spreadsheets/d/19xhMkUKSMTGshsZlXbO1v5vhHZrpBQxr/edit?usp=drive_link&ouid=112982864281396524406&rtpof=true&sd=true")
        }
        if (url3.value.isBlank()) {
            saveLink("url_3", "https://docs.google.com/spreadsheets/d/127cIESu-HL0pVbb7jCJ10wP5FLS_idg0/edit?usp=drive_link&ouid=112982864281396524406&rtpof=true&sd=true")
        }
        if (eUrl1.value.isBlank() && urlLw325.value.isBlank()) {
            saveLink("e_url_1", "https://docs.google.com/spreadsheets/d/1y8pz6qoZO2EJ3hMlwZwkjss8DZ0E2p6Q/edit?usp=drive_link&ouid=112982864281396524406&rtpof=true&sd=true")
        }

        // Set requested database links
        if (url4.value.isBlank()) {
            saveLink("url_4", "https://docs.google.com/spreadsheets/d/1NTDVayg_hXbx_qdd1aSm0Q7SY6o_o1NY/edit?usp=drive_link&ouid=112982864281396524406&rtpof=true&sd=true")
        }
        if (eUrl2.value.isBlank()) {
            saveLink("e_url_2", "https://docs.google.com/spreadsheets/d/1SWTIr_S9fAI7UKHsw26KovJnXXZzxEAE/edit?usp=drive_link&ouid=112982864281396524406&rtpof=true&sd=true")
        }
        if (kUrl1.value.isBlank() || kUrl1.value.contains("drive_link")) {
            saveLink("k_url_1", "https://docs.google.com/spreadsheets/d/1hEMC8jlu3M6GLpBTZQJVvgyRpBTcNK-6/edit?usp=drivesdk&ouid=112982864281396524406&rtpof=true&sd=true")
        }
        if (di319Url.value.isBlank()) {
            saveLink("di319_url", "https://docs.google.com/spreadsheets/d/1MjEhFYv6SZc-8lY0q5ODNiSqmwtzZD3b/edit?usp=drive_link&ouid=112982864281396524406&rtpof=true&sd=true")
        }

        viewModelScope.launch {
            filterMantri.collect { m ->
                if (!isSwitchingMenu) {
                    val menu = selectedMenu.value
                    val cur = menuFilterCache[menu] ?: loadMenuFiltersFromPrefs(menu)
                    if (cur.mantri != m) {
                        val updated = cur.copy(mantri = m)
                        menuFilterCache[menu] = updated
                        saveMenuFiltersToPrefs(menu, updated)
                    }
                }
            }
        }
        viewModelScope.launch {
            filterKolekUpdate.collect { ku ->
                if (ku != "SEMUA" && dailyDataMap.value.isEmpty()) {
                    loadDailyDataForFilter()
                }
            }
        }
        viewModelScope.launch {
            AppUpdateInstallReceiver.events.collect { event ->
                when (event) {
                    is PackageInstallEvent.PromptLaunched -> {
                        showUpdateDialog.value = false
                        val current = appUpdateState.value
                        if (current is AppUpdateState.Installing) {
                            appUpdateState.value = AppUpdateState.ReadyToInstall(current.release, current.apkFile)
                        }
                    }
                    is PackageInstallEvent.Succeeded -> {
                        val currentVer = AppUpdateManager.getCurrentVersionName(getApplication())
                        appUpdateState.value = AppUpdateState.UpToDate(currentVer)
                        showUpdateDialog.value = false
                    }
                    is PackageInstallEvent.Failed -> {
                        appUpdateState.value = AppUpdateState.Error(event.message)
                        showUpdateDialog.value = true
                    }
                }
            }
        }
        if (githubUpdateRepo.value.isBlank()) {
            saveGithubUpdateRepo("https://github.com/mynoize01/Monber.git")
        }
        checkForUpdates(autoInstallOnFind = false, isAutoBackgroundCheck = true)
    }

    fun loadDailyDataForFilter() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (dailyDataMap.value.isEmpty()) {
                    val cachedDaily = repository.getCachedPeriodCustomers("PERIOD_DAILY")
                    if (cachedDaily.isNotEmpty()) {
                        dailyCustomers.value = cachedDaily
                        dailyDataMap.value = buildLookupMapFromEntities(cachedDaily)
                    }
                }
                val isOffline = isOfflineModeEnabled.value
                val content = if (isOffline) getOfflineFileContent("url_3") else null
                val url = if (!isOffline) url3.value else null
                val canFetch = !content.isNullOrBlank() || (!url.isNullOrBlank() && repository.isNetworkAvailable(getApplication()))
                if (canFetch) {
                    val map = repository.getDailyInfo(url, content)
                    if (map.isNotEmpty()) {
                        dailyDataMap.value = map
                        val unique = getDistinctCustomers(map)
                        if (unique.isNotEmpty()) {
                            dailyCustomers.value = unique
                            repository.savePeriodCustomers("PERIOD_DAILY", unique)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectMenu(menu: String) {
        val oldMenu = selectedMenu.value
        if (oldMenu == menu) return

        // Save old menu's filters
        val currentFilters = FilterParams(
            search = searchQuery.value,
            kolek = filterKolek.value,
            mantri = filterMantri.value,
            bulan = filterBulanLunas.value,
            tahun = filterTahunLunas.value,
            osVal = filterOS.value,
            plVal = filterPlafondRange.value,
            tnVal = filterTenorRange.value,
            segmen = filterSegmen.value,
            kolekUpdate = filterKolekUpdate.value,
            kolekBlnLalu = filterKolekBlnLalu.value,
            npd = filterNpd.value,
            alamat = filterAlamat.value,
            setorPokok = filterSetorPokok.value,
            tusim = filterTusim.value,
            tucil = filterTucil.value,
            jatuhTempoOnly = filterJatuhTempoOnly.value,
            jatuhTempoH = selectedJatuhTempoH.value,
            desa = filterDesa.value
        )
        menuFilterCache[oldMenu] = currentFilters
        saveMenuFiltersToPrefs(oldMenu, currentFilters)

        isSwitchingMenu = true
        try {
            // Load new menu's filters or use default
            val newFilters = menuFilterCache[menu] ?: loadMenuFiltersFromPrefs(menu)

            searchQuery.value = newFilters.search
            filterKolek.value = newFilters.kolek
            filterMantri.value = newFilters.mantri
            filterBulanLunas.value = newFilters.bulan
            filterTahunLunas.value = newFilters.tahun
            filterOS.value = newFilters.osVal
            filterPlafondRange.value = newFilters.plVal
            filterTenorRange.value = newFilters.tnVal
            filterSegmen.value = newFilters.segmen
            filterKolekUpdate.value = newFilters.kolekUpdate
            filterKolekBlnLalu.value = newFilters.kolekBlnLalu
            filterNpd.value = newFilters.npd
            filterAlamat.value = newFilters.alamat
            filterSetorPokok.value = newFilters.setorPokok
            filterTusim.value = newFilters.tusim
            filterTucil.value = newFilters.tucil
            filterJatuhTempoOnly.value = newFilters.jatuhTempoOnly
            selectedJatuhTempoH.value = newFilters.jatuhTempoH
            filterDesa.value = newFilters.desa

            selectedMenu.value = menu
            menuFilterCache[menu] = newFilters
            saveMenuFiltersToPrefs(menu, newFilters)
        } finally {
            isSwitchingMenu = false
        }

        sharedPrefs.edit().putString("last_selected_menu", menu).apply()
        checkAndRefreshIfEmpty(menu)
    }

    fun checkAndRefreshIfEmpty(menu: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isOffline = isOfflineModeEnabled.value
            val hasNetwork = repository.isNetworkAvailable(getApplication())
            val actualMenu = if (menu == "KELOLAAN" || menu == "KUNJUNGAN") "INTRAKOMP" else menu
            if (actualMenu == "SIMPANAN") {
                if (di319Rows.value.isEmpty()) {
                    val cached = repository.getCachedDi319RowsOnce()
                    if (cached.isNotEmpty()) {
                        di319Rows.value = cached
                        if (!isOffline && !hasNetwork) {
                            isShowingCachedOfflineData.value = true
                        }
                    } else if (isOffline || hasNetwork) {
                        refreshSpreadsheetData()
                    } else {
                        isShowingCachedOfflineData.value = true
                        errorMessage.value = "Koneksi internet terputus dan belum ada cache Simpanan DI319."
                    }
                } else if (!isOffline && !hasNetwork) {
                    isShowingCachedOfflineData.value = true
                }
                return@launch
            }
            val count = repository.getCustomersCount(actualMenu)
            if (count == 0) {
                if (isOffline || hasNetwork) {
                    refreshSpreadsheetData()
                } else {
                    isShowingCachedOfflineData.value = true
                }
            } else if (!isOffline && !hasNetwork) {
                isShowingCachedOfflineData.value = true
            }
        }
    }

    private var syncJob: kotlinx.coroutines.Job? = null

    /**
     * Trigger fresh data download from Google Sheets
     */
    fun refreshSpreadsheetData(force: Boolean = false) {
        if (!force && syncJob?.isActive == true) {
            return
        }
        if (force && syncJob?.isActive == true) {
            syncJob?.cancel()
        }
        syncJob = viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            errorMessage.value = null
            
            val activeMenu = selectedMenu.value
            val isOffline = isOfflineModeEnabled.value

            if (!isOffline && !repository.isNetworkAvailable(getApplication())) {
                val cachedDi319 = repository.getCachedDi319RowsOnce()
                if (cachedDi319.isNotEmpty() && di319Rows.value.isEmpty()) {
                    di319Rows.value = cachedDi319
                }
                val cachedDaily = repository.getCachedPeriodCustomers("PERIOD_DAILY")
                if (cachedDaily.isNotEmpty() && dailyDataMap.value.isEmpty()) {
                    dailyCustomers.value = cachedDaily
                    dailyDataMap.value = buildLookupMapFromEntities(cachedDaily)
                }
                val lastMeta = repository.getLastSyncMetadata()
                val timeSuffix = lastMeta?.let { formatSyncTimestamp(it.lastSyncedAt) }?.takeIf { it.isNotBlank() }
                isShowingCachedOfflineData.value = true
                errorMessage.value = if (timeSuffix != null) {
                    "Koneksi internet terputus. Menampilkan data cache Room terakhir ($timeSuffix)."
                } else {
                    "Koneksi internet terputus. Menampilkan data cache Room terakhir yang tersimpan."
                }
                isRefreshing.value = false
                return@launch
            }

            if (activeMenu == "SIMPANAN") {
                try {
                    val di319List: List<com.example.data.model.Di319Row> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        if (isOffline) {
                            val di319OfflineContent = getOfflineFileContent("di319_url")
                            if (!di319OfflineContent.isNullOrBlank()) {
                                SpreadsheetService.fetchDi319Data(null, di319OfflineContent)
                            } else {
                                emptyList()
                            }
                        } else {
                            val url = di319Url.value
                            if (url.isNotBlank()) {
                                SpreadsheetService.fetchDi319Data(url, null)
                            } else {
                                emptyList()
                            }
                        }
                    }
                    if (di319List.isNotEmpty()) {
                        di319Rows.value = di319List
                        repository.saveDi319Rows(di319List)
                        isShowingCachedOfflineData.value = false
                    } else {
                        val cachedDi319 = repository.getCachedDi319RowsOnce()
                        if (cachedDi319.isNotEmpty()) {
                            di319Rows.value = cachedDi319
                            isShowingCachedOfflineData.value = true
                            val lastMeta = repository.getLastSyncMetadata()
                            val timeStr = lastMeta?.let { formatSyncTimestamp(it.lastSyncedAt) }?.takeIf { it.isNotBlank() }
                            errorMessage.value = if (timeStr != null) {
                                "Gagal menyegarkan Simpanan DI319. Menampilkan cache Room terakhir ($timeStr)."
                            } else {
                                "Gagal menyegarkan Simpanan DI319. Menampilkan cache Room terakhir."
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CustomerViewModel", "Failed to directly load di319 mapping", e)
                    val cachedDi319 = repository.getCachedDi319RowsOnce()
                    if (cachedDi319.isNotEmpty()) {
                        di319Rows.value = cachedDi319
                        isShowingCachedOfflineData.value = true
                        errorMessage.value = "Koneksi bermasalah — menampilkan data Simpanan DI319 dari cache Room."
                    } else {
                        errorMessage.value = "Gagal memuat data Simpanan DI319: ${e.message}"
                    }
                }
                isRefreshing.value = false
                return@launch
            }
            
            val fetchMenu = if (activeMenu == "KELOLAAN") "INTRAKOMP" else activeMenu
            
            var offlineTargetContent: String? = null
            var offlinePreviousMonthContent: String? = null
            var offlineLw321Content: String? = null
            var offlineKelolaanContent: String? = null
            
            val targetUrl: String?
            val previousMonthUrl: String?
            val lw321Url: String?
            val kelolaanUrl: String?
            
            if (isOffline) {
                targetUrl = null
                previousMonthUrl = null
                lw321Url = null
                kelolaanUrl = null
                
                offlineTargetContent = when (fetchMenu) {
                    "INTRAKOMP" -> {
                        if (selectedBottomNav.value == "MONITORING" || selectedBottomNav.value == "KELOLAAN") {
                            getOfflineFileContent("url_2")
                        } else {
                            when {
                                selectedPeriodSource.value.startsWith("BULAN_") -> {
                                    val m = selectedPeriodSource.value.removePrefix("BULAN_").toIntOrNull() ?: 1
                                    getOfflineFileContent("lw321_all_$m") ?: getOfflineFileContent("url_2")
                                }
                                selectedPeriodSource.value == "BULAN_LALU" -> getOfflineFileContent("url_1")
                                selectedPeriodSource.value == "DAILY" -> getOfflineFileContent("url_3")
                                else -> getOfflineFileContent("url_2") // "AKHIR_BULAN"
                            }
                        }
                    }
                    "EKSTRAKOMP" -> getOfflineFileContent("e_url_1") ?: getOfflineFileContent("url_lw325")
                    else -> getOfflineFileContent("k_url_1")
                }
                
                if (fetchMenu == "INTRAKOMP") {
                    offlinePreviousMonthContent = getOfflineFileContent("url_1")
                    offlineLw321Content = getOfflineFileContent("url_4")
                    offlineKelolaanContent = getOfflineFileContent("k_url_1")
                } else if (fetchMenu == "EKSTRAKOMP") {
                    offlinePreviousMonthContent = null
                    offlineLw321Content = getOfflineFileContent("e_url_2")
                    offlineKelolaanContent = null
                } else {
                    offlinePreviousMonthContent = null
                    offlineLw321Content = null
                    offlineKelolaanContent = null
                }
            } else {
                targetUrl = when (fetchMenu) {
                    "INTRAKOMP" -> {
                        if (selectedBottomNav.value == "MONITORING" || selectedBottomNav.value == "KELOLAAN") {
                            url2.value
                        } else {
                            when {
                                selectedPeriodSource.value.startsWith("BULAN_") -> {
                                    val m = selectedPeriodSource.value.removePrefix("BULAN_").toIntOrNull() ?: 1
                                    val monthUrl = lw321AllUrls[m]?.value ?: ""
                                    if (monthUrl.isNotBlank()) monthUrl else url2.value
                                }
                                selectedPeriodSource.value == "BULAN_LALU" -> url1.value
                                selectedPeriodSource.value == "DAILY" -> url3.value
                                else -> url2.value // "AKHIR_BULAN"
                            }
                        }
                    }
                    "EKSTRAKOMP" -> {
                        // Selalu mengambil data dari link monthly LW325 Akhir Bulan (e_url_1)
                        if (eUrl1.value.isNotBlank()) eUrl1.value else urlLw325.value
                    }
                    else -> customSpreadsheetUrl.value
                }
                previousMonthUrl = if (fetchMenu == "INTRAKOMP") {
                    if (selectedBottomNav.value == "MONITORING" || selectedBottomNav.value == "KELOLAAN") {
                        urlLw321BlnLalu.value
                    } else if (selectedPeriodSource.value.startsWith("BULAN_")) {
                        val m = selectedPeriodSource.value.removePrefix("BULAN_").toIntOrNull() ?: 1
                        if (m > 1) lw321AllUrls[m - 1]?.value?.takeIf { it.isNotBlank() } ?: urlLw321BlnLalu.value else urlLw321BlnLalu.value
                    } else {
                        urlLw321BlnLalu.value
                    }
                } else null
                if (fetchMenu == "INTRAKOMP") {
                    lw321Url = url4.value
                    kelolaanUrl = kUrl1.value
                } else if (fetchMenu == "EKSTRAKOMP") {
                    lw321Url = eUrl2.value
                    kelolaanUrl = null
                } else {
                    lw321Url = null
                    kelolaanUrl = null
                }
            }
            
            if (!isOffline && (targetUrl == null || targetUrl.isBlank())) {
                isRefreshing.value = false
                return@launch
            } else if (isOffline && (offlineTargetContent == null || offlineTargetContent.isBlank())) {
                isRefreshing.value = false
                errorMessage.value = "Mode Offline Aktif: Harap pilih/upload file CSV terlebih dahulu di menu popup Offline."
                return@launch
            }
            
            val result = repository.refreshData(
                fetchMenu, 
                targetUrl, 
                previousMonthUrl, 
                lw321Url,
                kelolaanUrl,
                if (fetchMenu == "EKSTRAKOMP") lw325DailyUrl.value else null,
                offlineTargetContent,
                offlinePreviousMonthContent,
                offlineLw321Content,
                offlineKelolaanContent,
                if (fetchMenu == "EKSTRAKOMP") getOfflineFileContent("lw325_daily_url") else null
            )
            
            try {
                val dMap = if (isOffline) {
                    val dailyOfflineContent = getOfflineFileContent("url_3")
                    if (!dailyOfflineContent.isNullOrBlank()) {
                        repository.getDailyInfo(null, dailyOfflineContent)
                    } else {
                        emptyMap()
                    }
                } else {
                    val dailyUrl = url3.value
                    if (dailyUrl.isNotBlank()) {
                        repository.getDailyInfo(dailyUrl, null)
                    } else {
                        emptyMap()
                    }
                }
                if (dMap.isNotEmpty()) {
                    dailyDataMap.value = dMap
                }
                fetchPeriodDates()
                
                try {
                    val blnLaluMap = if (isOffline) {
                        val content = getOfflineFileContent("url_1")
                        if (!content.isNullOrBlank()) repository.getDailyInfo(null, content) else emptyMap()
                    } else {
                        val url = url1.value
                        if (url.isNotBlank()) repository.getDailyInfo(url, null) else emptyMap()
                    }
                    val uniqueBlnLalu = getDistinctCustomers(blnLaluMap)
                    if (uniqueBlnLalu.isNotEmpty()) {
                        repository.savePeriodCustomers("PERIOD_BULAN_LALU", uniqueBlnLalu)
                        bulanLaluCustomers.value = uniqueBlnLalu
                        summaryBulanLalu.value = UnitSummaryData(
                            os = uniqueBlnLalu.sumOf { it.osPinjaman },
                            debtors = uniqueBlnLalu.size
                        )
                        kolekSummaryBulanLalu.value = calculateKolekSummary(uniqueBlnLalu)
                        mantriSummaryBulanLalu.value = calculateMantriSummaries(uniqueBlnLalu)
                    } else if (bulanLaluCustomers.value.isEmpty()) {
                        val cachedBlnLalu = repository.getCachedPeriodCustomers("PERIOD_BULAN_LALU")
                        val fallbackList = if (cachedBlnLalu.isNotEmpty()) {
                            cachedBlnLalu
                        } else {
                            val current = customersList.value.filter { it.menuType == "INTRAKOMP" }.ifEmpty { customersList.value }
                            current.map { cust ->
                                val bLalu = cust.kolekBlnLalu.ifBlank { cust.kolek }
                                cust.copy(kolek = bLalu)
                            }
                        }
                        if (fallbackList.isNotEmpty()) {
                            bulanLaluCustomers.value = fallbackList
                            summaryBulanLalu.value = UnitSummaryData(
                                os = fallbackList.sumOf { it.osPinjaman },
                                debtors = fallbackList.size
                            )
                            kolekSummaryBulanLalu.value = calculateKolekSummary(fallbackList)
                            mantriSummaryBulanLalu.value = calculateMantriSummaries(fallbackList)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CustomerViewModel", "Failed to load bln lalu summary", e)
                }

                try {
                    val akhirBlnMap = if (isOffline) {
                        val content = getOfflineFileContent("url_2")
                        if (!content.isNullOrBlank()) repository.getDailyInfo(null, content) else emptyMap()
                    } else {
                        val url = url2.value
                        if (url.isNotBlank()) repository.getDailyInfo(url, null) else emptyMap()
                    }
                    val uniqueAkhirBln = getDistinctCustomers(akhirBlnMap)
                    if (uniqueAkhirBln.isNotEmpty()) {
                        repository.savePeriodCustomers("PERIOD_AKHIR_BULAN", uniqueAkhirBln)
                        akhirBulanCustomers.value = uniqueAkhirBln
                        summaryAkhirBulan.value = UnitSummaryData(
                            os = uniqueAkhirBln.sumOf { it.osPinjaman },
                            debtors = uniqueAkhirBln.size
                        )
                        kolekSummaryAkhirBulan.value = calculateKolekSummary(uniqueAkhirBln)
                        mantriSummaryAkhirBulan.value = calculateMantriSummaries(uniqueAkhirBln)
                    } else if (akhirBulanCustomers.value.isEmpty()) {
                        val cachedAkhirBln = repository.getCachedPeriodCustomers("PERIOD_AKHIR_BULAN")
                        val current = if (cachedAkhirBln.isNotEmpty()) {
                            cachedAkhirBln
                        } else {
                            customersList.value.filter { it.menuType == "INTRAKOMP" }.ifEmpty { customersList.value }
                        }
                        if (current.isNotEmpty()) {
                            akhirBulanCustomers.value = current
                            summaryAkhirBulan.value = UnitSummaryData(
                                os = current.sumOf { it.osPinjaman },
                                debtors = current.size
                            )
                            kolekSummaryAkhirBulan.value = calculateKolekSummary(current)
                            mantriSummaryAkhirBulan.value = calculateMantriSummaries(current)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CustomerViewModel", "Failed to load akhir bln summary", e)
                }

                try {
                    val uniqueDaily = getDistinctCustomers(dMap)
                    if (uniqueDaily.isNotEmpty()) {
                        repository.savePeriodCustomers("PERIOD_DAILY", uniqueDaily)
                        dailyCustomers.value = uniqueDaily
                        summaryDaily.value = UnitSummaryData(
                            os = uniqueDaily.sumOf { it.osPinjaman },
                            debtors = uniqueDaily.size
                        )
                        kolekSummaryDaily.value = calculateKolekSummary(uniqueDaily)
                        mantriSummaryDaily.value = calculateMantriSummaries(uniqueDaily)
                    } else if (dailyCustomers.value.isEmpty()) {
                        val cachedDaily = repository.getCachedPeriodCustomers("PERIOD_DAILY")
                        if (cachedDaily.isNotEmpty()) {
                            dailyCustomers.value = cachedDaily
                            if (dailyDataMap.value.isEmpty()) {
                                dailyDataMap.value = buildLookupMapFromEntities(cachedDaily)
                            }
                            summaryDaily.value = UnitSummaryData(
                                os = cachedDaily.sumOf { it.osPinjaman },
                                debtors = cachedDaily.size
                            )
                            kolekSummaryDaily.value = calculateKolekSummary(cachedDaily)
                            mantriSummaryDaily.value = calculateMantriSummaries(cachedDaily)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CustomerViewModel", "Failed to load daily summary", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("CustomerViewModel", "Failed to load daily mapping", e)
            }

            try {
                val di319List: List<com.example.data.model.Di319Row> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    if (isOffline) {
                        val di319OfflineContent = getOfflineFileContent("di319_url")
                        if (!di319OfflineContent.isNullOrBlank()) {
                            SpreadsheetService.fetchDi319Data(null, di319OfflineContent)
                        } else {
                            emptyList()
                        }
                    } else {
                        val url = di319Url.value
                        if (url.isNotBlank()) {
                            SpreadsheetService.fetchDi319Data(url, null)
                        } else {
                            emptyList()
                        }
                    }
                }
                if (di319List.isNotEmpty()) {
                    di319Rows.value = di319List
                    repository.saveDi319Rows(di319List)
                } else if (di319Rows.value.isEmpty()) {
                    val cachedDi319 = repository.getCachedDi319RowsOnce()
                    if (cachedDi319.isNotEmpty()) {
                        di319Rows.value = cachedDi319
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CustomerViewModel", "Failed to load di319 mapping", e)
            }

            persistCurrentSummaries()

            if (result.isFailure) {
                isShowingCachedOfflineData.value = true
                val lastMeta = repository.getLastSyncMetadata()
                val timeStr = lastMeta?.let { formatSyncTimestamp(it.lastSyncedAt) }?.takeIf { it.isNotBlank() }
                errorMessage.value = if (isOffline) {
                    "Gagal memuat data offline: " + (result.exceptionOrNull()?.message ?: "Format CSV salah")
                } else if (timeStr != null) {
                    "Koneksi terputus — menampilkan data cache Room terakhir ($timeStr)."
                } else {
                    "Koneksi terputus — menampilkan data cache Room terakhir."
                }
            } else {
                isShowingCachedOfflineData.value = false
            }
            isRefreshing.value = false
        }
    }

    fun toggleSort(column: SortColumn) {
        if (sortColumn.value == column) {
            // Flip direction
            sortDirection.value = if (sortDirection.value == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
        } else {
            sortColumn.value = column
            sortDirection.value = SortDirection.DESC // Default to DESC (often largest Plafond/OS is first)
        }
        sharedPrefs.edit()
            .putString("last_sort_column", sortColumn.value.name)
            .putString("last_sort_direction", sortDirection.value.name)
            .apply()
    }

    fun selectSortColumn(column: SortColumn) {
        if (sortColumn.value == column) {
            sortDirection.value = if (sortDirection.value == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
        } else {
            sortColumn.value = column
            sortDirection.value = when (column) {
                SortColumn.NAMA_NASABAH,
                SortColumn.DESA,
                SortColumn.KOLEK,
                SortColumn.NPD,
                SortColumn.SEGMEN,
                SortColumn.ALAMAT,
                SortColumn.NAMA_MANTRI -> SortDirection.ASC
                else -> SortDirection.DESC
            }
        }
        sharedPrefs.edit()
            .putString("last_sort_column", sortColumn.value.name)
            .putString("last_sort_direction", sortDirection.value.name)
            .apply()
    }

    private fun listWithAll(origin: List<String>): List<String> {
        val result = mutableListOf("SEMUA")
        result.addAll(origin)
        return result
    }

    fun getMonthsElapsedSince(tglReal: String): Int {
        if (tglReal.isBlank()) return 0
        try {
            val parts = tglReal.split("/")
            if (parts.size >= 3) {
                val day = parts[0].trim().toIntOrNull() ?: 1
                val month = parts[1].trim().toIntOrNull() ?: 1
                val year = parts[2].trim().toIntOrNull() ?: 2026
                
                val startCal = java.util.Calendar.getInstance().apply {
                    set(year, month - 1, day, 0, 0, 0)
                }
                val nowCal = java.util.Calendar.getInstance()
                
                val diffYear = nowCal.get(java.util.Calendar.YEAR) - startCal.get(java.util.Calendar.YEAR)
                val diffMonth = nowCal.get(java.util.Calendar.MONTH) - startCal.get(java.util.Calendar.MONTH)
                
                val elapsed = diffYear * 12 + diffMonth
                return if (elapsed < 0) 0 else elapsed
            }
        } catch (e: Exception) {
            // ignore
        }
        return 0
    }

    fun getDistinctCustomers(map: Map<String, com.example.data.model.CustomerEntity>): List<com.example.data.model.CustomerEntity> {
        return map.values.distinctBy {
            when {
                it.noRekening.isNotBlank() -> it.noRekening.filter { c -> c.isDigit() }.ifBlank { it.noRekening }
                it.cif.isNotBlank() -> it.cif.filter { c -> c.isLetterOrDigit() }.ifBlank { it.cif }
                else -> "${it.namaNasabah}_${it.plafond}"
            }
        }
    }

    fun calculateKolekSummary(customers: Collection<com.example.data.model.CustomerEntity>): KolekSummaryData {
        var lancarOs = 0.0
        var lancarDeb = 0
        var smlOs = 0.0
        var smlDeb = 0
        var nplOs = 0.0
        var nplDeb = 0
        var lainnyaOs = 0.0
        var lainnyaDeb = 0

        customers.forEach { customer ->
            val clean = customer.kolek.trim().uppercase()
            val group = when {
                clean == "KL" || clean.contains("KURANG") || clean == "D" || clean.contains("DIRAGUKAN") || clean == "M" || clean.contains("MACET") || clean == "3" || clean == "4" || clean == "5" || clean == "NPL" -> "NPL"
                clean.startsWith("SML") || clean.contains("DPK") || clean == "2" -> "SML"
                clean == "L" || clean == "LANCAR" || clean == "1" || (clean.contains("LANCAR") && !clean.contains("KURANG")) -> "LANCAR"
                else -> "LAINNYA"
            }
            val os = customer.osPinjaman
            when (group) {
                "LANCAR" -> {
                    lancarOs += os
                    lancarDeb++
                }
                "SML" -> {
                    smlOs += os
                    smlDeb++
                }
                "NPL" -> {
                    nplOs += os
                    nplDeb++
                }
                else -> {
                    lainnyaOs += os
                    lainnyaDeb++
                }
            }
        }
        return KolekSummaryData(
            lancar = UnitSummaryData(os = lancarOs, debtors = lancarDeb),
            sml = UnitSummaryData(os = smlOs, debtors = smlDeb),
            npl = UnitSummaryData(os = nplOs, debtors = nplDeb),
            lainnya = UnitSummaryData(os = lainnyaOs, debtors = lainnyaDeb)
        )
    }

    fun calculateKolekSummary(customerMap: Map<String, com.example.data.model.CustomerEntity>): KolekSummaryData =
        calculateKolekSummary(getDistinctCustomers(customerMap))

    fun calculateMantriSummaries(customers: Collection<com.example.data.model.CustomerEntity>): Map<String, MantriDetailData> {
        val result = mutableMapOf<String, MantriDetailData>()
        val mainList = customersList.value.filter { it.menuType == "INTRAKOMP" }.ifEmpty { customersList.value }
        val mainByRek = mutableMapOf<String, com.example.data.model.CustomerEntity>()
        val mainByCif = mutableMapOf<String, com.example.data.model.CustomerEntity>()
        val mainByPn = mutableMapOf<String, com.example.data.model.CustomerEntity>()

        mainList.forEach { cust ->
            val cleanRek = cust.noRekening.replace("-", "").replace(".", "").trim()
            if (cust.noRekening.isNotBlank()) mainByRek[cust.noRekening.trim()] = cust
            if (cleanRek.isNotBlank()) mainByRek[cleanRek] = cust
            if (cust.cif.isNotBlank()) mainByCif[cust.cif.trim().uppercase()] = cust
            if (cust.pnMantri.isNotBlank()) mainByPn[cust.pnMantri.trim()] = cust
        }

        val listToProcess = if (customers.isNotEmpty()) customers else mainList

        listToProcess.forEach { customer ->
            val cleanRek = customer.noRekening.replace("-", "").replace(".", "").trim()
            val mainCust = mainByRek[customer.noRekening.trim()]
                ?: mainByRek[cleanRek]
                ?: (if (customer.cif.isNotBlank()) mainByCif[customer.cif.trim().uppercase()] else null)

            val mantriNameFromCustomer = customer.namaMantri.trim()
            val mantriNameFromMain = mainCust?.namaMantri?.trim() ?: ""
            val pnMantriFromCustomer = customer.pnMantri.trim()
            val mantriNameFromPn = if (pnMantriFromCustomer.isNotBlank()) mainByPn[pnMantriFromCustomer]?.namaMantri?.trim() else null

            var rawName = when {
                mantriNameFromCustomer.isNotBlank() &&
                        !mantriNameFromCustomer.equals("TIDAK TERIDENTIFIKASI", ignoreCase = true) &&
                        !mantriNameFromCustomer.equals("TANPA NAMA", ignoreCase = true) &&
                        !mantriNameFromCustomer.equals("Blank PN", ignoreCase = true) &&
                        mantriNameFromCustomer.any { it.isLetter() } -> mantriNameFromCustomer

                mantriNameFromMain.isNotBlank() &&
                        !mantriNameFromMain.equals("TIDAK TERIDENTIFIKASI", ignoreCase = true) &&
                        !mantriNameFromMain.equals("TANPA NAMA", ignoreCase = true) &&
                        !mantriNameFromMain.equals("Blank PN", ignoreCase = true) -> mantriNameFromMain

                !mantriNameFromPn.isNullOrBlank() &&
                        !mantriNameFromPn.equals("Blank PN", ignoreCase = true) -> mantriNameFromPn

                else -> "Blank PN"
            }

            if (rawName.contains("-")) {
                val parts = rawName.split("-")
                val textPart = parts.lastOrNull { p -> p.trim().any { it.isLetter() } }?.trim()
                if (!textPart.isNullOrBlank()) {
                    rawName = textPart
                }
            }
            rawName = rawName.replace(Regex("[0-9]"), "").replace(Regex("^[- .]+"), "").trim().uppercase()
            val name = if (rawName.isBlank() || rawName.equals("BLANK PN", ignoreCase = true) || rawName == "TIDAK TERIDENTIFIKASI" || rawName == "TANPA NAMA") "Blank PN" else rawName

            val cleanKolek = customer.kolek.trim().uppercase()
            val group = when {
                cleanKolek == "KL" || cleanKolek.contains("KURANG") || cleanKolek == "D" || cleanKolek.contains("DIRAGUKAN") || cleanKolek == "M" || cleanKolek.contains("MACET") || cleanKolek == "3" || cleanKolek == "4" || cleanKolek == "5" || cleanKolek == "NPL" -> "NPL"
                cleanKolek.startsWith("SML") || cleanKolek.contains("DPK") || cleanKolek == "2" -> "SML"
                cleanKolek == "L" || cleanKolek == "LANCAR" || cleanKolek == "1" || (cleanKolek.contains("LANCAR") && !cleanKolek.contains("KURANG")) -> "LANCAR"
                else -> "LANCAR"
            }

            val current = result[name] ?: MantriDetailData(name = name)

            val os = customer.osPinjaman
            val updatedTotal = current.total.copy(os = current.total.os + os, debtors = current.total.debtors + 1)
            val updatedLancar = if (group == "LANCAR") current.lancar.copy(os = current.lancar.os + os, debtors = current.lancar.debtors + 1) else current.lancar
            val updatedSml = if (group == "SML") current.sml.copy(os = current.sml.os + os, debtors = current.sml.debtors + 1) else current.sml
            val updatedNpl = if (group == "NPL") current.npl.copy(os = current.npl.os + os, debtors = current.npl.debtors + 1) else current.npl

            result[name] = current.copy(
                total = updatedTotal,
                lancar = updatedLancar,
                sml = updatedSml,
                npl = updatedNpl
            )
        }
        return result
    }

    fun calculateMantriSummaries(customerMap: Map<String, com.example.data.model.CustomerEntity>): Map<String, MantriDetailData> =
        calculateMantriSummaries(getDistinctCustomers(customerMap))

    data class Summary(
        val totalCount: Int,
        val totalPlafond: Double,
        val totalOs: Double,
        val nplCount: Int,
        val lancarCount: Int,
        val sml1Count: Int,
        val sml2Count: Int,
        val sml3Count: Int,
        val osLancar: Double = 0.0,
        val osSmlTotal: Double = 0.0,
        val smlTotalCount: Int = 0,
        val osSml1: Double = 0.0,
        val osSml2: Double = 0.0,
        val osSml3: Double = 0.0,
        val osNplTotal: Double = 0.0,
        val osKl: Double = 0.0,
        val klCount: Int = 0,
        val osD: Double = 0.0,
        val dCount: Int = 0,
        val osM: Double = 0.0,
        val mCount: Int = 0,
        val seg1Name: String = "KUPEDES",
        val seg1Os: Double = 0.0,
        val seg1Count: Int = 0,
        val seg2Name: String = "KUR",
        val seg2Os: Double = 0.0,
        val seg2Count: Int = 0,
        val seg3Name: String = "BRIGUNA",
        val seg3Os: Double = 0.0,
        val seg3Count: Int = 0,
        val allSegments: List<Triple<String, Double, Int>> = emptyList()
    )

    fun saveGithubUpdateRepo(repo: String) {
        val cleaned = repo.trim()
        githubUpdateRepo.value = cleaned
        sharedPrefs.edit().putString("github_update_repo", cleaned).apply()
    }

    fun openUpdateDialog() {
        showUpdateDialog.value = true
    }

    fun dismissUpdateDialog() {
        showUpdateDialog.value = false
    }

    fun clearUpdateStatus() {
        val current = appUpdateState.value
        if (current !is AppUpdateState.Checking &&
            current !is AppUpdateState.Downloading &&
            current !is AppUpdateState.Installing
        ) {
            appUpdateState.value = AppUpdateState.Idle
        }
    }

    fun checkForUpdates(
        autoInstallOnFind: Boolean = false,
        isAutoBackgroundCheck: Boolean = false
    ) {
        val repo = githubUpdateRepo.value.trim().ifBlank { "https://github.com/mynoize01/Monber.git" }
        if (githubUpdateRepo.value.isBlank()) {
            saveGithubUpdateRepo(repo)
        }

        val curState = appUpdateState.value
        if (curState is AppUpdateState.Checking ||
            curState is AppUpdateState.Downloading ||
            curState is AppUpdateState.Installing
        ) {
            return
        }

        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            if (!isAutoBackgroundCheck) {
                appUpdateState.value = AppUpdateState.Checking
            }
            val result = AppUpdateManager.fetchLatestRelease(repo)
            result.onSuccess { release ->
                val appContext = getApplication<Application>()
                val currentVersion = AppUpdateManager.getCurrentVersionName(appContext)
                val currentVersionCode = AppUpdateManager.getCurrentVersionCode(appContext)
                if (AppUpdateManager.isNewerUpdate(release, currentVersion, currentVersionCode)) {
                    if (isAutoBackgroundCheck) {
                        AppUpdateManager.showUpdateNotification(appContext, release)
                    }
                    showUpdateDialog.value = true
                    if (autoInstallOnFind && !release.apkDownloadUrl.isNullOrBlank()) {
                        startDownloadAndInstallInternal(release)
                    } else {
                        appUpdateState.value = AppUpdateState.UpdateAvailable(release)
                    }
                } else {
                    appUpdateState.value = if (isAutoBackgroundCheck) {
                        AppUpdateState.Idle
                    } else {
                        AppUpdateState.UpToDate(currentVersion)
                    }
                }
            }.onFailure { error ->
                appUpdateState.value = if (isAutoBackgroundCheck) {
                    AppUpdateState.Idle
                } else {
                    AppUpdateState.Error(error.message ?: "Gagal memeriksa pembaruan dari GitHub.")
                }
            }
        }
    }

    fun downloadAndInstallUpdate(release: GitHubReleaseInfo) {
        val cur = appUpdateState.value
        if (cur is AppUpdateState.Downloading || cur is AppUpdateState.Installing) return
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            startDownloadAndInstallInternal(release)
        }
    }

    private suspend fun startDownloadAndInstallInternal(release: GitHubReleaseInfo) {
        val appContext = getApplication<Application>()
        val totalMbEstimate = if (release.apkSizeBytes > 0) release.apkSizeBytes / (1024f * 1024f) else 0f
        appUpdateState.value = AppUpdateState.Downloading(
            release = release,
            progressPercent = 0,
            downloadedMb = 0f,
            totalMb = totalMbEstimate
        )

        val downloadResult = AppUpdateManager.downloadApk(
            context = appContext,
            release = release,
            onProgress = { percent, downloadedMb, totalMb ->
                appUpdateState.value = AppUpdateState.Downloading(
                    release = release,
                    progressPercent = percent,
                    downloadedMb = downloadedMb,
                    totalMb = totalMb
                )
            }
        )

        downloadResult.onSuccess { apkFile ->
            if (AppUpdateManager.canRequestPackageInstalls(appContext)) {
                appUpdateState.value = AppUpdateState.Installing(release, apkFile)
                val launched = AppUpdateManager.installApk(appContext, apkFile)
                if (!launched) {
                    appUpdateState.value = AppUpdateState.Error("Gagal membuka penginstal aplikasi.")
                    showUpdateDialog.value = true
                }
            } else {
                appUpdateState.value = AppUpdateState.NeedsInstallPermission(release, apkFile)
                showUpdateDialog.value = true
            }
        }.onFailure { error ->
            appUpdateState.value = AppUpdateState.Error(error.message ?: "Gagal mengunduh file APK pembaruan.")
            showUpdateDialog.value = true
        }
    }

    fun retryInstallAfterPermissionGranted() {
        val appContext = getApplication<Application>()
        when (val state = appUpdateState.value) {
            is AppUpdateState.NeedsInstallPermission -> {
                if (AppUpdateManager.canRequestPackageInstalls(appContext)) {
                    viewModelScope.launch {
                        appUpdateState.value = AppUpdateState.Installing(state.release, state.apkFile)
                        val launched = AppUpdateManager.installApk(appContext, state.apkFile)
                        if (!launched) {
                            appUpdateState.value = AppUpdateState.Error("Gagal membuka penginstal aplikasi.")
                            showUpdateDialog.value = true
                        }
                    }
                }
            }
            is AppUpdateState.ReadyToInstall -> {
                if (AppUpdateManager.canRequestPackageInstalls(appContext)) {
                    viewModelScope.launch {
                        appUpdateState.value = AppUpdateState.Installing(state.release, state.apkFile)
                        val launched = AppUpdateManager.installApk(appContext, state.apkFile)
                        if (!launched) {
                            appUpdateState.value = AppUpdateState.Error("Gagal membuka penginstal aplikasi.")
                            showUpdateDialog.value = true
                        }
                    }
                } else {
                    appUpdateState.value = AppUpdateState.NeedsInstallPermission(state.release, state.apkFile)
                    showUpdateDialog.value = true
                }
            }
            else -> Unit
        }
    }
}
