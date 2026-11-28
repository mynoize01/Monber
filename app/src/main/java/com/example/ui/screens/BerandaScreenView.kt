package com.example.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CustomerEntity
import com.example.data.util.DateTenorUtils
import com.example.ui.components.LazyLoadingFooterRow
import com.example.ui.components.rememberLazyPagedItems
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.KolekSummaryData
import com.example.ui.viewmodel.MantriDetailData
import com.example.ui.viewmodel.RealisasiNetMetrics
import com.example.ui.viewmodel.UnitSummaryData
import com.example.update.AppUpdateState
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class MonthlyRealisasiPoint(
    val monthNumber: Int,
    val yearNumber: Int,
    val shortLabel: String,
    val fullLabel: String,
    val realisasi: Double,
    val netDisburs: Double,
    val debtors: Int,
    val hasData: Boolean
)

private data class MonthlyKolekTrendPoint(
    val monthNumber: Int,
    val yearNumber: Int,
    val shortLabel: String,
    val fullLabel: String,
    val totalOs: Double,
    val smlOs: Double,
    val smlDeb: Int,
    val smlPct: Double,
    val nplOs: Double,
    val nplDeb: Int,
    val nplPct: Double,
    val hasData: Boolean
)

private enum class KolekTrendFocus {
    ALL,
    SML,
    NPL
}

private enum class BerandaScope(val label: String) {
    UNIT("Unit"),
    KELOLAAN("Kelolaan")
}

private enum class PantauanCategory(
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val containerColor: Color,
    val borderColor: Color
) {
    TUSIM(
        title = "Tusim",
        subtitle = "Nasabah bertunggakan dengan saldo tabungan di atas Rp 25.000",
        accentColor = Color(0xFF0F766E),
        containerColor = Color(0xFFF0FDFA),
        borderColor = Color(0xFF99F6E4)
    ),
    TUCIL(
        title = "Tucil <100 rb",
        subtitle = "Nasabah dengan nominal tunggakan di bawah Rp 100.000",
        accentColor = Color(0xFFC2410C),
        containerColor = Color(0xFFFFF7ED),
        borderColor = Color(0xFFFED7AA)
    ),
    TUNGGAKAN_UNDER_6(
        title = "Tunggakan <6 bln",
        subtitle = "Pinjaman baru berjalan kurang dari 6 bulan yang sudah menunggak",
        accentColor = Color(0xFFBE123C),
        containerColor = Color(0xFFFFF1F2),
        borderColor = Color(0xFFFECDD3)
    )
}

private data class BerandaAlertCustomerItem(
    val namaNasabah: String,
    val noRekening: String,
    val kolek: String,
    val osPinjaman: Double,
    val totalTunggakan: Double,
    val saldoTabungan: Double,
    val umurTunggakanHari: Int,
    val lamaPinjamanBulan: Int?,
    val namaMantri: String,
    val segmen: String
)

private data class BerandaAlertCategoryData(
    val summary: UnitSummaryData = UnitSummaryData(),
    val customers: List<BerandaAlertCustomerItem> = emptyList()
)

private enum class NotifFilterTab(val label: String) {
    SEMUA("Semua"),
    JATUH_TEMPO("Jatuh Tempo"),
    KUNJUNGAN("Kunjungan")
}

private data class BerandaNotificationCustomerItem(
    val namaNasabah: String,
    val noRekening: String,
    val kolek: String,
    val osPinjaman: Double,
    val totalTunggakan: Double,
    val npd: String,
    val namaMantri: String,
    val segmen: String,
    val isJatuhTempo: Boolean,
    val isKunjungan: Boolean
)

private data class BerandaUnitBaseDerived(
    val fallbackUnitSummary: UnitSummaryData = UnitSummaryData(),
    val fallbackUnitKolek: KolekSummaryData = KolekSummaryData(),
    val kelolaanCustomers: List<CustomerEntity> = emptyList(),
    val kelolaanCustomersSummary: UnitSummaryData = UnitSummaryData(),
    val kelolaanCustomersKolek: KolekSummaryData = KolekSummaryData()
)

private class BerandaUnitBaseCacheEntry(
    val monitoring: List<CustomerEntity>,
    val loggedInPN: String,
    val cleanPnDigits: String,
    val displayName: String,
    val result: BerandaUnitBaseDerived
)

private data class BerandaPortfolioDerived(
    val kelolaanSegmenSource: List<CustomerEntity> = emptyList(),
    val resolvedBerandaCustomers: List<CustomerEntity> = emptyList(),
    val segmenKelolaanGroups: List<Triple<String, Double, Int>> = emptyList(),
    val tusimData: BerandaAlertCategoryData = BerandaAlertCategoryData(),
    val tucilData: BerandaAlertCategoryData = BerandaAlertCategoryData(),
    val tunggakanUnder6Data: BerandaAlertCategoryData = BerandaAlertCategoryData(),
    val notificationCustomers: List<BerandaNotificationCustomerItem> = emptyList()
)

private class BerandaPortfolioCacheEntry(
    val monitoring: List<CustomerEntity>,
    val daily: List<CustomerEntity>,
    val akhir: List<CustomerEntity>,
    val kelolaanCustomers: List<CustomerEntity>,
    val dailyDataMap: Map<String, CustomerEntity>,
    val di319Rows: List<com.example.data.model.Di319Row>,
    val kunjunganKeys: Set<String>,
    val loggedInPN: String,
    val cleanPnDigits: String,
    val displayName: String,
    val effectiveScope: BerandaScope,
    val result: BerandaPortfolioDerived
)

@Volatile private var cachedBerandaBgBitmap: ImageBitmap? = null
@Volatile private var cachedUnitBaseEntry: BerandaUnitBaseCacheEntry? = null
@Volatile private var cachedPortfolioEntry: BerandaPortfolioCacheEntry? = null

@Composable
fun BerandaScreenView(
    viewModel: CustomerViewModel,
    formatter: NumberFormat,
    onOpenGlobalSearch: () -> Unit,
    onOpenKelolaan: () -> Unit,
    onOpenKunjungan: () -> Unit,
    onOpenMonitoring: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenUpdateDialog: () -> Unit,
    onOpenCustomerListPage: (BerandaCustomerListPageRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val loggedInPN by viewModel.loggedInPN.collectAsState()
    val loggedInMantriDetails by viewModel.loggedInMantriDetails.collectAsState()
    val summaryDaily by viewModel.summaryDaily.collectAsState()
    val summaryAkhirBulan by viewModel.summaryAkhirBulan.collectAsState()
    val summaryBulanLalu by viewModel.summaryBulanLalu.collectAsState()
    val kolekSummaryDaily by viewModel.kolekSummaryDaily.collectAsState()
    val kolekSummaryAkhirBulan by viewModel.kolekSummaryAkhirBulan.collectAsState()
    val kolekSummaryBulanLalu by viewModel.kolekSummaryBulanLalu.collectAsState()
    val mantriSummaryDaily by viewModel.mantriSummaryDaily.collectAsState()
    val mantriSummaryAkhirBulan by viewModel.mantriSummaryAkhirBulan.collectAsState()
    val mantriSummaryBulanLalu by viewModel.mantriSummaryBulanLalu.collectAsState()

    val dashboardMonthlySummary by viewModel.dashboardMonthlySummary.collectAsState()
    val dashboardMonthlyKolekSummary by viewModel.dashboardMonthlyKolekSummary.collectAsState()
    val dashboardMonthlyMantriSummary by viewModel.dashboardMonthlyMantriSummary.collectAsState()
    val dashboardMonthlyRealisasi by viewModel.dashboardMonthlyRealisasi.collectAsState()

    val periodDateDaily by viewModel.periodDateDaily.collectAsState()
    val periodDateAkhirBulan by viewModel.periodDateAkhirBulan.collectAsState()
    val periodDateBulanLalu by viewModel.periodDateBulanLalu.collectAsState()

    val monitoringCustomers by viewModel.monitoringCustomers.collectAsState()
    val dailyCustomers by viewModel.dailyCustomers.collectAsState()
    val akhirBulanCustomers by viewModel.akhirBulanCustomers.collectAsState()
    val dailyDataMap by viewModel.dailyDataMap.collectAsState()
    val di319Rows by viewModel.di319Rows.collectAsState()
    val kunjunganKeys by viewModel.kunjunganKeys.collectAsState()
    val activeVisitDateStr by viewModel.activeVisitDateStr.collectAsState()
    val dueTodayCount by viewModel.dueTodayCount.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isShowingCachedOfflineData by viewModel.isShowingCachedOfflineData.collectAsState()
    val appUpdateState by viewModel.appUpdateState.collectAsState()

    LaunchedEffect(Unit) {
        delay(220)
        viewModel.loadAllMonthlyChartData()
        if (summaryDaily.debtors == 0) {
            viewModel.loadSummaryForPeriod("DAILY")
        }
        if (summaryAkhirBulan.debtors == 0) {
            viewModel.loadSummaryForPeriod("AKHIR_BULAN")
        }
    }

    LaunchedEffect(dailyCustomers.size, akhirBulanCustomers.size, monitoringCustomers.size) {
        delay(240)
        viewModel.refreshMonthlyRealisasiChart()
    }

    val cleanPnDigits = remember(loggedInPN) {
        val raw = loggedInPN.trim()
        val digits = Regex("""\d{4,9}""").find(raw)?.value ?: raw
        digits.trimStart('0').ifEmpty { digits }
    }

    val displayName = remember(loggedInPN, loggedInMantriDetails, cleanPnDigits) {
        when {
            loggedInPN.isBlank() -> "Mantri BRI"
            loggedInMantriDetails.contains(" - ") ->
                loggedInMantriDetails.substringAfter(" - ").trim().ifBlank { "Nurul Mubin" }
            loggedInMantriDetails.isNotBlank() && !loggedInMantriDetails.contains("Mikro", ignoreCase = true) ->
                loggedInMantriDetails.trim()
            cleanPnDigits == "207803" -> "Nurul Mubin"
            else -> "Mantri $cleanPnDigits"
        }
    }

    var berandaScope by remember(loggedInPN) {
        mutableStateOf(if (loggedInPN.isNotBlank()) BerandaScope.KELOLAAN else BerandaScope.UNIT)
    }
    var realisasiPeriodMonths by remember { mutableIntStateOf(6) }
    var selectedChartMonthIndex by remember { mutableIntStateOf(-1) }
    var showKualitasSheet by remember { mutableStateOf(false) }
    var showSegmenSheet by remember { mutableStateOf(false) }
    var activePantauanCategory by remember { mutableStateOf<PantauanCategory?>(null) }
    var trendPeriodMonths by remember { mutableIntStateOf(6) }
    var selectedTrendMonthIndex by remember { mutableIntStateOf(-1) }
    var trendFocus by remember { mutableStateOf(KolekTrendFocus.ALL) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    // Helper to look up Mantri stats cleanly inside MantriDetailData map
    fun findMantriDetail(map: Map<String, MantriDetailData>): MantriDetailData? {
        if (loggedInPN.isBlank() || map.isEmpty()) return null
        val targetClean = cleanMantriNameOnly(displayName)
        return map.entries.firstOrNull { entry ->
            val keyClean = cleanMantriNameOnly(entry.key)
            keyClean.equals(targetClean, ignoreCase = true) ||
                entry.key.contains(displayName, ignoreCase = true) ||
                (cleanPnDigits.isNotBlank() && entry.key.contains(cleanPnDigits))
        }?.value
    }

    // Compute Unit fallback & Kelolaan base off the main thread with instant reference cache
    val matchedUnitBaseCache = remember(monitoringCustomers, loggedInPN, cleanPnDigits, displayName) {
        val cached = cachedUnitBaseEntry
        if (cached != null &&
            cached.monitoring === monitoringCustomers &&
            cached.loggedInPN == loggedInPN &&
            cached.cleanPnDigits == cleanPnDigits &&
            cached.displayName == displayName
        ) {
            cached.result
        } else {
            null
        }
    }
    val unitBaseDerived by produceState(
        initialValue = matchedUnitBaseCache ?: cachedUnitBaseEntry?.result ?: BerandaUnitBaseDerived(),
        monitoringCustomers,
        loggedInPN,
        cleanPnDigits,
        displayName
    ) {
        if (matchedUnitBaseCache != null) {
            value = matchedUnitBaseCache
            return@produceState
        }
        val computed = withContext(Dispatchers.Default) {
            val unitSum = UnitSummaryData(
                os = monitoringCustomers.sumOf { it.osPinjaman },
                debtors = monitoringCustomers.size
            )
            val unitKol = calculateKolekSummaryFromList(monitoringCustomers)
            val kelolaanList = if (loggedInPN.isBlank()) {
                emptyList()
            } else {
                monitoringCustomers.filter { cust ->
                    val custPnDigits = cust.pnMantri.filter { it.isDigit() }.trimStart('0')
                    (cleanPnDigits.isNotBlank() && custPnDigits == cleanPnDigits) ||
                        cust.pnMantri.trim().equals(loggedInPN.trim(), ignoreCase = true) ||
                        cust.namaMantri.contains(loggedInPN.trim(), ignoreCase = true) ||
                        cust.namaMantri.contains(displayName, ignoreCase = true)
                }
            }
            val kelolaanSum = UnitSummaryData(
                os = kelolaanList.sumOf { it.osPinjaman },
                debtors = kelolaanList.size
            )
            val kelolaanKol = calculateKolekSummaryFromList(kelolaanList)
            BerandaUnitBaseDerived(
                fallbackUnitSummary = unitSum,
                fallbackUnitKolek = unitKol,
                kelolaanCustomers = kelolaanList,
                kelolaanCustomersSummary = kelolaanSum,
                kelolaanCustomersKolek = kelolaanKol
            )
        }
        cachedUnitBaseEntry = BerandaUnitBaseCacheEntry(
            monitoring = monitoringCustomers,
            loggedInPN = loggedInPN,
            cleanPnDigits = cleanPnDigits,
            displayName = displayName,
            result = computed
        )
        value = computed
    }

    val fallbackUnitSummary = unitBaseDerived.fallbackUnitSummary
    val fallbackUnitKolek = unitBaseDerived.fallbackUnitKolek
    val kelolaanCustomers = unitBaseDerived.kelolaanCustomers

    val kelolaanSummaryFromDaily = remember(mantriSummaryDaily, unitBaseDerived, displayName, cleanPnDigits) {
        val dailyMantri = findMantriDetail(mantriSummaryDaily)
        when {
            dailyMantri != null && dailyMantri.total.debtors > 0 -> dailyMantri.total
            unitBaseDerived.kelolaanCustomersSummary.debtors > 0 -> unitBaseDerived.kelolaanCustomersSummary
            else -> {
                val akhirMantri = findMantriDetail(mantriSummaryAkhirBulan)
                akhirMantri?.total ?: UnitSummaryData()
            }
        }
    }

    // Active Unit summary (Daily -> Akhir Bulan -> Bulan Lalu -> monitoringCustomers)
    val unitActiveSummary = remember(summaryDaily, summaryAkhirBulan, summaryBulanLalu, fallbackUnitSummary) {
        when {
            summaryDaily.debtors > 0 -> summaryDaily
            summaryAkhirBulan.debtors > 0 -> summaryAkhirBulan
            summaryBulanLalu.debtors > 0 -> summaryBulanLalu
            else -> fallbackUnitSummary
        }
    }
    val unitActiveKolek = remember(kolekSummaryDaily, kolekSummaryAkhirBulan, kolekSummaryBulanLalu, fallbackUnitKolek) {
        when {
            kolekSummaryDaily.lancar.debtors > 0 || kolekSummaryDaily.npl.debtors > 0 -> kolekSummaryDaily
            kolekSummaryAkhirBulan.lancar.debtors > 0 || kolekSummaryAkhirBulan.npl.debtors > 0 -> kolekSummaryAkhirBulan
            kolekSummaryBulanLalu.lancar.debtors > 0 || kolekSummaryBulanLalu.npl.debtors > 0 -> kolekSummaryBulanLalu
            else -> fallbackUnitKolek
        }
    }

    // Active Kelolaan summary & kolek
    val kelolaanActiveSummary = remember(
        kelolaanSummaryFromDaily,
        mantriSummaryAkhirBulan,
        mantriSummaryBulanLalu,
        unitBaseDerived,
        displayName,
        cleanPnDigits
    ) {
        if (kelolaanSummaryFromDaily.debtors > 0) {
            kelolaanSummaryFromDaily
        } else {
            findMantriDetail(mantriSummaryAkhirBulan)?.total
                ?: findMantriDetail(mantriSummaryBulanLalu)?.total
                ?: unitBaseDerived.kelolaanCustomersSummary
        }
    }
    val kelolaanActiveKolek = remember(
        mantriSummaryDaily,
        mantriSummaryAkhirBulan,
        mantriSummaryBulanLalu,
        unitBaseDerived,
        displayName,
        cleanPnDigits
    ) {
        val mDaily = findMantriDetail(mantriSummaryDaily)
        val mAkhir = findMantriDetail(mantriSummaryAkhirBulan)
        val mBln = findMantriDetail(mantriSummaryBulanLalu)
        val target = when {
            mDaily != null && mDaily.total.debtors > 0 -> mDaily
            mAkhir != null && mAkhir.total.debtors > 0 -> mAkhir
            mBln != null && mBln.total.debtors > 0 -> mBln
            else -> null
        }
        if (target != null) {
            KolekSummaryData(
                lancar = target.lancar,
                sml = target.sml,
                npl = target.npl
            )
        } else {
            unitBaseDerived.kelolaanCustomersKolek
        }
    }

    // Resolve active scope data (if Kelolaan chosen & logged in with data, use Kelolaan, else Unit)
    val effectiveScope = if (berandaScope == BerandaScope.KELOLAAN && loggedInPN.isNotBlank() && kelolaanActiveSummary.debtors > 0) {
        BerandaScope.KELOLAAN
    } else {
        BerandaScope.UNIT
    }

    val activeScopeSummary = if (effectiveScope == BerandaScope.KELOLAAN) kelolaanActiveSummary else unitActiveSummary
    val activeScopeKolek = if (effectiveScope == BerandaScope.KELOLAAN) kelolaanActiveKolek else unitActiveKolek

    val latestDateText = remember(periodDateDaily, periodDateAkhirBulan, periodDateBulanLalu) {
        formatShortDisplayDate(
            periodDateDaily.ifBlank { periodDateAkhirBulan.ifBlank { periodDateBulanLalu } }
        )
    }

    // Build 3/6/9/12-month rolling timeline for Realisasi & Netdisburse
    val realisasiPoints = remember(
        effectiveScope,
        realisasiPeriodMonths,
        dashboardMonthlyRealisasi,
        summaryDaily,
        summaryAkhirBulan,
        summaryBulanLalu,
        periodDateDaily,
        periodDateAkhirBulan,
        periodDateBulanLalu,
        displayName,
        cleanPnDigits,
        loggedInPN
    ) {
        val shortMonthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")
        val fullMonthNames = listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )

        val cal = Calendar.getInstance()
        val fallbackY = cal.get(Calendar.YEAR)
        val fallbackM = cal.get(Calendar.MONTH) + 1

        fun parseYm(raw: String): Pair<Int, Int>? {
            val parsed = DateTenorUtils.parseYearMonthDay(raw) ?: return null
            return parsed.first to parsed.second
        }

        val dailyYm = if (summaryDaily.debtors > 0) parseYm(periodDateDaily) else null
        val akhirYm = if (summaryAkhirBulan.debtors > 0) parseYm(periodDateAkhirBulan) else null
        val blnLaluYm = if (summaryBulanLalu.debtors > 0) parseYm(periodDateBulanLalu) else null

        val (endYear, endMonth) = dailyYm ?: akhirYm ?: blnLaluYm ?: (fallbackY to fallbackM)
        val targetCleanName = cleanMantriNameOnly(displayName)

        fun findMantriRealisasi(map: Map<String, RealisasiNetMetrics>): RealisasiNetMetrics? {
            if (loggedInPN.isBlank() || map.isEmpty()) return null
            return map.entries.firstOrNull { entry ->
                val keyClean = cleanMantriNameOnly(entry.key)
                keyClean.equals(targetCleanName, ignoreCase = true) ||
                    entry.key.contains(displayName, ignoreCase = true) ||
                    (cleanPnDigits.isNotBlank() && entry.key.contains(cleanPnDigits))
            }?.value
        }

        val monthCount = realisasiPeriodMonths.coerceIn(3, 12)
        (-(monthCount - 1)..0).map { offset ->
            val totalMonths = endYear * 12 + (endMonth - 1) + offset
            val yr = totalMonths.floorDiv(12)
            val mo = (totalMonths % 12 + 12) % 12 + 1
            val snapshot = dashboardMonthlyRealisasi[yr * 100 + mo]

            val metrics = if (effectiveScope == BerandaScope.KELOLAAN) {
                snapshot?.byMantri?.let { findMantriRealisasi(it) } ?: RealisasiNetMetrics()
            } else {
                snapshot?.unit ?: RealisasiNetMetrics()
            }

            MonthlyRealisasiPoint(
                monthNumber = mo,
                yearNumber = yr,
                shortLabel = shortMonthNames[mo - 1],
                fullLabel = "${fullMonthNames[mo - 1]} $yr",
                realisasi = metrics.realisasi,
                netDisburs = metrics.netDisburs,
                debtors = metrics.debtors,
                hasData = metrics.debtors > 0 || metrics.realisasi > 0.0
            )
        }
    }

    // Build 3/6/9/12-month historical SML & NPL trend timeline
    val kolekTrendPoints = remember(
        effectiveScope,
        trendPeriodMonths,
        dashboardMonthlySummary,
        dashboardMonthlyKolekSummary,
        dashboardMonthlyMantriSummary,
        summaryDaily,
        summaryAkhirBulan,
        summaryBulanLalu,
        kolekSummaryDaily,
        kolekSummaryAkhirBulan,
        kolekSummaryBulanLalu,
        mantriSummaryDaily,
        mantriSummaryAkhirBulan,
        mantriSummaryBulanLalu,
        periodDateDaily,
        periodDateAkhirBulan,
        periodDateBulanLalu,
        displayName,
        cleanPnDigits,
        loggedInPN
    ) {
        val shortMonthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")
        val fullMonthNames = listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )

        val cal = Calendar.getInstance()
        val fallbackY = cal.get(Calendar.YEAR)
        val fallbackM = cal.get(Calendar.MONTH) + 1

        fun parseYm(raw: String): Pair<Int, Int>? {
            val parsed = DateTenorUtils.parseYearMonthDay(raw) ?: return null
            return parsed.first to parsed.second
        }

        val dailyYm = if (summaryDaily.debtors > 0) parseYm(periodDateDaily) else null
        val akhirYm = if (summaryAkhirBulan.debtors > 0) parseYm(periodDateAkhirBulan) else null
        val blnLaluYm = if (summaryBulanLalu.debtors > 0) parseYm(periodDateBulanLalu) else null

        val (endYear, endMonth) = dailyYm ?: akhirYm ?: blnLaluYm ?: (fallbackY to fallbackM)
        val monthCount = trendPeriodMonths.coerceIn(3, 12)

        (-(monthCount - 1)..0).map { offset ->
            val totalMonths = endYear * 12 + (endMonth - 1) + offset
            val yr = totalMonths.floorDiv(12)
            val mo = (totalMonths % 12 + 12) % 12 + 1
            val ym = yr to mo

            val (totalOs, smlData, nplData) = if (effectiveScope == BerandaScope.KELOLAAN) {
                val mantriDetail = when {
                    dailyYm == ym -> findMantriDetail(mantriSummaryDaily)
                        ?: dashboardMonthlyMantriSummary[mo]?.let { findMantriDetail(it) }
                    akhirYm == ym -> findMantriDetail(mantriSummaryAkhirBulan)
                        ?: dashboardMonthlyMantriSummary[mo]?.let { findMantriDetail(it) }
                    blnLaluYm == ym -> findMantriDetail(mantriSummaryBulanLalu)
                        ?: dashboardMonthlyMantriSummary[mo]?.let { findMantriDetail(it) }
                    else -> dashboardMonthlyMantriSummary[mo]?.let { findMantriDetail(it) }
                }
                if (mantriDetail != null) {
                    val tOs = mantriDetail.total.os.takeIf { it > 0.0 }
                        ?: (mantriDetail.lancar.os + mantriDetail.sml.os + mantriDetail.npl.os)
                    Triple(tOs, mantriDetail.sml, mantriDetail.npl)
                } else {
                    Triple(0.0, UnitSummaryData(), UnitSummaryData())
                }
            } else {
                val (uSum, kSum) = when {
                    dailyYm == ym && summaryDaily.debtors > 0 -> summaryDaily to kolekSummaryDaily
                    akhirYm == ym && summaryAkhirBulan.debtors > 0 -> summaryAkhirBulan to kolekSummaryAkhirBulan
                    blnLaluYm == ym && summaryBulanLalu.debtors > 0 -> summaryBulanLalu to kolekSummaryBulanLalu
                    else -> (dashboardMonthlySummary[mo] ?: UnitSummaryData()) to
                        (dashboardMonthlyKolekSummary[mo] ?: KolekSummaryData())
                }
                val tOs = uSum.os.takeIf { it > 0.0 }
                    ?: (kSum.lancar.os + kSum.sml.os + kSum.npl.os)
                Triple(tOs, kSum.sml, kSum.npl)
            }

            val smlPct = if (totalOs > 0.0) (smlData.os / totalOs) * 100.0 else 0.0
            val nplPct = if (totalOs > 0.0) (nplData.os / totalOs) * 100.0 else 0.0
            val hasData = totalOs > 0.0 || smlData.debtors > 0 || nplData.debtors > 0

            MonthlyKolekTrendPoint(
                monthNumber = mo,
                yearNumber = yr,
                shortLabel = shortMonthNames[mo - 1],
                fullLabel = "${fullMonthNames[mo - 1]} $yr",
                totalOs = totalOs,
                smlOs = smlData.os,
                smlDeb = smlData.debtors,
                smlPct = smlPct,
                nplOs = nplData.os,
                nplDeb = nplData.debtors,
                nplPct = nplPct,
                hasData = hasData
            )
        }
    }

    // Notifications summary (real app state + customer notifications)
    val hasUpdateAvailable = appUpdateState is AppUpdateState.UpdateAvailable ||
        appUpdateState is AppUpdateState.ReadyToInstall ||
        appUpdateState is AppUpdateState.NeedsInstallPermission

    val updateVersionTag = when (val st = appUpdateState) {
        is AppUpdateState.UpdateAvailable -> st.release.tagName
        is AppUpdateState.ReadyToInstall -> st.release.tagName
        is AppUpdateState.NeedsInstallPermission -> st.release.tagName
        else -> ""
    }

    val totalOs = activeScopeSummary.os.takeIf { it > 0.0 }
        ?: (activeScopeKolek.lancar.os + activeScopeKolek.sml.os + activeScopeKolek.npl.os)

    val lancarOs = activeScopeKolek.lancar.os
    val lancarDeb = activeScopeKolek.lancar.debtors
    val lancarOsPct = if (totalOs > 0.0) (lancarOs / totalOs) * 100.0 else 0.0

    val smlOs = activeScopeKolek.sml.os
    val smlDeb = activeScopeKolek.sml.debtors
    val smlOsPct = if (totalOs > 0.0) (smlOs / totalOs) * 100.0 else 0.0

    val nplOs = activeScopeKolek.npl.os
    val nplDeb = activeScopeKolek.npl.debtors
    val nplOsPct = if (totalOs > 0.0) (nplOs / totalOs) * 100.0 else 0.0

    // Coalesce heavy customer resolution, segmen grouping, alert categories, and notifications onto Dispatchers.Default
    val matchedPortfolioCache = remember(
        monitoringCustomers,
        dailyCustomers,
        akhirBulanCustomers,
        kelolaanCustomers,
        dailyDataMap,
        di319Rows,
        kunjunganKeys,
        loggedInPN,
        cleanPnDigits,
        displayName,
        effectiveScope
    ) {
        val cached = cachedPortfolioEntry
        if (cached != null &&
            cached.monitoring === monitoringCustomers &&
            cached.daily === dailyCustomers &&
            cached.akhir === akhirBulanCustomers &&
            cached.kelolaanCustomers === kelolaanCustomers &&
            cached.dailyDataMap === dailyDataMap &&
            cached.di319Rows === di319Rows &&
            cached.kunjunganKeys == kunjunganKeys &&
            cached.loggedInPN == loggedInPN &&
            cached.cleanPnDigits == cleanPnDigits &&
            cached.displayName == displayName &&
            cached.effectiveScope == effectiveScope
        ) {
            cached.result
        } else {
            null
        }
    }

    val portfolioDerived by produceState(
        initialValue = matchedPortfolioCache ?: cachedPortfolioEntry?.result ?: BerandaPortfolioDerived(),
        monitoringCustomers,
        dailyCustomers,
        akhirBulanCustomers,
        kelolaanCustomers,
        dailyDataMap,
        di319Rows,
        kunjunganKeys,
        loggedInPN,
        cleanPnDigits,
        displayName,
        effectiveScope
    ) {
        if (matchedPortfolioCache != null) {
            value = matchedPortfolioCache
            return@produceState
        }
        val computed = withContext(Dispatchers.Default) {
            val mainByRek = HashMap<String, CustomerEntity>(monitoringCustomers.size * 2)
            val mainByCif = HashMap<String, CustomerEntity>(monitoringCustomers.size)
            for (m in monitoringCustomers) {
                val rawRek = m.noRekening.trim()
                val cleanRek = rawRek.replace("-", "").replace(".", "").trim()
                if (rawRek.isNotBlank()) mainByRek[rawRek] = m
                if (cleanRek.isNotBlank()) mainByRek[cleanRek] = m
                val cifKey = m.cif.trim().uppercase()
                if (cifKey.isNotBlank()) mainByCif[cifKey] = m
            }

            fun matchMantri(list: List<CustomerEntity>): List<CustomerEntity> {
                if (loggedInPN.isBlank()) return emptyList()
                val targetCleanName = cleanMantriNameOnly(displayName)
                return list.filter { cust ->
                    val rawRek = cust.noRekening.trim()
                    val cleanRek = rawRek.replace("-", "").replace(".", "").trim()
                    val mainCust = mainByRek[rawRek]
                        ?: mainByRek[cleanRek]
                        ?: (if (cust.cif.isNotBlank()) mainByCif[cust.cif.trim().uppercase()] else null)

                    val custPnDigits = cust.pnMantri.filter { it.isDigit() }.trimStart('0')
                    val mainPnDigits = mainCust?.pnMantri?.filter { it.isDigit() }?.trimStart('0') ?: ""
                    val custMantriClean = cleanMantriNameOnly(cust.namaMantri)
                    val mainMantriClean = mainCust?.let { cleanMantriNameOnly(it.namaMantri) } ?: ""

                    (cleanPnDigits.isNotBlank() && (custPnDigits == cleanPnDigits || mainPnDigits == cleanPnDigits)) ||
                        cust.pnMantri.trim().equals(loggedInPN.trim(), ignoreCase = true) ||
                        (mainCust != null && mainCust.pnMantri.trim().equals(loggedInPN.trim(), ignoreCase = true)) ||
                        cust.namaMantri.contains(loggedInPN.trim(), ignoreCase = true) ||
                        cust.namaMantri.contains(displayName, ignoreCase = true) ||
                        (targetCleanName.isNotBlank() && (
                            custMantriClean.equals(targetCleanName, ignoreCase = true) ||
                                mainMantriClean.equals(targetCleanName, ignoreCase = true)
                            ))
                }
            }

            val segmenSource = if (effectiveScope == BerandaScope.KELOLAAN && loggedInPN.isNotBlank()) {
                matchMantri(dailyCustomers)
                    .ifEmpty { matchMantri(akhirBulanCustomers) }
                    .ifEmpty { kelolaanCustomers }
            } else {
                dailyCustomers
                    .ifEmpty { akhirBulanCustomers }
                    .ifEmpty { monitoringCustomers }
            }

            val savingsMaxByNormCif = HashMap<String, Double>(di319Rows.size)
            for (row in di319Rows) {
                val norm = normalizeBerandaCif(row.cif)
                if (norm.isNotEmpty()) {
                    val prev = savingsMaxByNormCif[norm] ?: 0.0
                    if (row.saldo > prev) savingsMaxByNormCif[norm] = row.saldo
                }
            }

            val nowDate = LocalDate.now()
            val todayDayOfMonth = nowDate.dayOfMonth

            val resolvedCustomers = ArrayList<CustomerEntity>(segmenSource.size)
            val segmenBuckets = HashMap<String, Pair<Double, Int>>()

            var tusimOs = 0.0
            var tusimCount = 0
            val tusimList = mutableListOf<BerandaAlertCustomerItem>()

            var tucilOs = 0.0
            var tucilCount = 0
            val tucilList = mutableListOf<BerandaAlertCustomerItem>()

            var tg6Os = 0.0
            var tg6Count = 0
            val tg6List = mutableListOf<BerandaAlertCustomerItem>()

            val seenNotifKeys = HashSet<String>()
            val notifResult = mutableListOf<BerandaNotificationCustomerItem>()

            for (cust in segmenSource) {
                val dailyCust = resolveBerandaDailyCustomer(cust, dailyDataMap)
                val resolvedCust = if (dailyCust != null) {
                    cust.copy(
                        kolek = dailyCust.kolek.ifBlank { cust.kolek },
                        osPinjaman = dailyCust.osPinjaman,
                        totalTunggakan = dailyCust.totalTunggakan,
                        tunggakanPokok = dailyCust.tunggakanPokok,
                        tunggakanBunga = dailyCust.tunggakanBunga,
                        umurTgk = dailyCust.umurTgk
                    )
                } else {
                    cust
                }
                resolvedCustomers.add(resolvedCust)

                val segKey = cust.segmen.trim().uppercase().ifBlank { "LAINNYA" }
                val prevSeg = segmenBuckets[segKey]
                segmenBuckets[segKey] = if (prevSeg == null) {
                    cust.osPinjaman to 1
                } else {
                    (prevSeg.first + cust.osPinjaman) to (prevSeg.second + 1)
                }

                val currentTunggakan = resolvedCust.totalTunggakan
                val currentUmurTgk = resolvedCust.umurTgk
                val currentKolek = resolvedCust.kolek
                val currentOs = resolvedCust.osPinjaman
                val normCif = normalizeBerandaCif(cust.cif)
                val savingsSaldo = if (normCif.isNotBlank()) savingsMaxByNormCif[normCif] ?: 0.0 else 0.0
                val mantriLabel = cleanMantriNameOnly(cust.namaMantri).ifBlank { cust.namaMantri.trim() }

                val rawTglReal = cust.tglReal.ifBlank {
                    cust.extraColumns.split("||")
                        .firstOrNull { part ->
                            part.startsWith("Tgl Real::", ignoreCase = true) ||
                                part.startsWith("Tgl Realisasi::", ignoreCase = true) ||
                                part.startsWith("TGL_REAL::", ignoreCase = true)
                        }
                        ?.substringAfter("::")
                        ?.trim()
                        ?: ""
                }
                val elapsedMonths = DateTenorUtils.monthsElapsedSince(rawTglReal, nowDate)

                fun buildAlertItem(): BerandaAlertCustomerItem = BerandaAlertCustomerItem(
                    namaNasabah = cust.namaNasabah.trim().ifBlank { "Tanpa Nama" },
                    noRekening = cust.noRekening.trim(),
                    kolek = currentKolek.trim().ifBlank { "-" },
                    osPinjaman = currentOs,
                    totalTunggakan = currentTunggakan,
                    saldoTabungan = savingsSaldo,
                    umurTunggakanHari = currentUmurTgk,
                    lamaPinjamanBulan = elapsedMonths,
                    namaMantri = mantriLabel,
                    segmen = cust.segmen.trim()
                )

                if (currentTunggakan > 0.0) {
                    if (normCif.isNotBlank() && savingsSaldo > 25_000.0) {
                        tusimOs += currentOs
                        tusimCount++
                        tusimList.add(buildAlertItem())
                    }
                    if (currentTunggakan < 100_000.0) {
                        tucilOs += currentOs
                        tucilCount++
                        tucilList.add(buildAlertItem())
                    }
                }

                val hasArrears = currentTunggakan > 0.0 ||
                    currentUmurTgk > 0 ||
                    isNonLancarBerandaKolek(currentKolek)
                if (hasArrears && elapsedMonths != null && elapsedMonths < 6) {
                    tg6Os += currentOs
                    tg6Count++
                    tg6List.add(buildAlertItem())
                }

                // Notification evaluation (within active scope)
                val custKey = if (cust.noRekening.isNotBlank()) cust.noRekening else if (cust.cif.isNotBlank()) cust.cif else cust.namaNasabah
                if (seenNotifKeys.add(custKey)) {
                    val npdDay = cust.npd.trim().toIntOrNull()
                        ?: cust.tglLunas.trim().split('/', '-', '.').firstOrNull()?.toIntOrNull()
                    val isDueToday = npdDay == todayDayOfMonth
                    val isVisitPlanned = kunjunganKeys.contains(custKey)
                    if (isDueToday || isVisitPlanned) {
                        notifResult.add(
                            BerandaNotificationCustomerItem(
                                namaNasabah = cust.namaNasabah.trim().ifBlank { "Tanpa Nama" },
                                noRekening = cust.noRekening.trim(),
                                kolek = currentKolek.trim().ifBlank { "-" },
                                osPinjaman = currentOs,
                                totalTunggakan = currentTunggakan,
                                npd = cust.npd.trim().ifBlank { npdDay?.toString() ?: "" },
                                namaMantri = mantriLabel,
                                segmen = cust.segmen.trim(),
                                isJatuhTempo = isDueToday,
                                isKunjungan = isVisitPlanned
                            )
                        )
                    }
                }
            }

            if (kunjunganKeys.isNotEmpty()) {
                for (cust in monitoringCustomers) {
                    val custKey = if (cust.noRekening.isNotBlank()) cust.noRekening else if (cust.cif.isNotBlank()) cust.cif else cust.namaNasabah
                    if (!seenNotifKeys.add(custKey)) continue
                    if (!kunjunganKeys.contains(custKey)) continue

                    val dailyCust = resolveBerandaDailyCustomer(cust, dailyDataMap)
                    val currentTunggakan = dailyCust?.totalTunggakan ?: cust.totalTunggakan
                    val currentKolek = dailyCust?.kolek?.ifBlank { cust.kolek } ?: cust.kolek
                    val currentOs = dailyCust?.osPinjaman ?: cust.osPinjaman
                    val mantriLabel = cleanMantriNameOnly(cust.namaMantri).ifBlank { cust.namaMantri.trim() }
                    val npdDay = cust.npd.trim().toIntOrNull()
                        ?: cust.tglLunas.trim().split('/', '-', '.').firstOrNull()?.toIntOrNull()

                    notifResult.add(
                        BerandaNotificationCustomerItem(
                            namaNasabah = cust.namaNasabah.trim().ifBlank { "Tanpa Nama" },
                            noRekening = cust.noRekening.trim(),
                            kolek = currentKolek.trim().ifBlank { "-" },
                            osPinjaman = currentOs,
                            totalTunggakan = currentTunggakan,
                            npd = cust.npd.trim().ifBlank { npdDay?.toString() ?: "" },
                            namaMantri = mantriLabel,
                            segmen = cust.segmen.trim(),
                            isJatuhTempo = false,
                            isKunjungan = true
                        )
                    )
                }
            }

            val itemComparator = compareByDescending<BerandaAlertCustomerItem> { it.totalTunggakan }
                .thenByDescending { it.osPinjaman }
                .thenBy { it.namaNasabah.lowercase() }

            val sortedSegmen = segmenBuckets.entries
                .map { (name, pair) -> Triple(name, pair.first, pair.second) }
                .sortedByDescending { it.second }

            val sortedNotifications = notifResult.sortedWith(
                compareByDescending<BerandaNotificationCustomerItem> { it.isKunjungan && it.isJatuhTempo }
                    .thenByDescending { it.isKunjungan }
                    .thenByDescending { it.totalTunggakan }
                    .thenByDescending { it.osPinjaman }
                    .thenBy { it.namaNasabah.lowercase() }
            )

            BerandaPortfolioDerived(
                kelolaanSegmenSource = segmenSource,
                resolvedBerandaCustomers = resolvedCustomers,
                segmenKelolaanGroups = sortedSegmen,
                tusimData = BerandaAlertCategoryData(
                    summary = UnitSummaryData(os = tusimOs, debtors = tusimCount),
                    customers = tusimList.sortedWith(itemComparator)
                ),
                tucilData = BerandaAlertCategoryData(
                    summary = UnitSummaryData(os = tucilOs, debtors = tucilCount),
                    customers = tucilList.sortedWith(itemComparator)
                ),
                tunggakanUnder6Data = BerandaAlertCategoryData(
                    summary = UnitSummaryData(os = tg6Os, debtors = tg6Count),
                    customers = tg6List.sortedWith(itemComparator)
                ),
                notificationCustomers = sortedNotifications
            )
        }

        cachedPortfolioEntry = BerandaPortfolioCacheEntry(
            monitoring = monitoringCustomers,
            daily = dailyCustomers,
            akhir = akhirBulanCustomers,
            kelolaanCustomers = kelolaanCustomers,
            dailyDataMap = dailyDataMap,
            di319Rows = di319Rows,
            kunjunganKeys = kunjunganKeys,
            loggedInPN = loggedInPN,
            cleanPnDigits = cleanPnDigits,
            displayName = displayName,
            effectiveScope = effectiveScope,
            result = computed
        )
        value = computed
    }

    val resolvedBerandaCustomers = portfolioDerived.resolvedBerandaCustomers
    val segmenKelolaanGroups = portfolioDerived.segmenKelolaanGroups
    val tusimData = portfolioDerived.tusimData
    val tucilData = portfolioDerived.tucilData
    val tunggakanUnder6Data = portfolioDerived.tunggakanUnder6Data
    val tusimSummary = tusimData.summary
    val tucilSummary = tucilData.summary
    val tunggakanUnder6Summary = tunggakanUnder6Data.summary
    val notificationCustomers = portfolioDerived.notificationCustomers

    val bellBadgeCount = notificationCustomers.size +
        (if (hasUpdateAvailable) 1 else 0) +
        (if (isShowingCachedOfflineData) 1 else 0)

    val bgBitmap by produceState<ImageBitmap?>(initialValue = cachedBerandaBgBitmap, context) {
        if (value != null) return@produceState
        val loaded = withContext(Dispatchers.IO) {
            val resId = context.resources.getIdentifier("bg_beranda", "drawable", context.packageName)
            if (resId != 0) {
                runCatching {
                    BitmapFactory.decodeResource(context.resources, resId)?.asImageBitmap()
                }.getOrNull()
            } else {
                null
            }
        }
        if (loaded != null) {
            cachedBerandaBgBitmap = loaded
            value = loaded
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Lightweight base gradient + async cached wallpaper background for zero-jank frame 1 transition
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFFEDF3FC))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF003B8E),
                                Color(0xFF005EC4),
                                Color(0xFF0F7EE6)
                            )
                        )
                    )
            )
        }
        bgBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.matchParentSize()
            )
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Frozen Transparent Hero Overlay aligned with the dark-blue upper region of bg_beranda
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarTopPadding + 8.dp, bottom = 20.dp)
                    .padding(horizontal = 18.dp)
            ) {
                // Top Brand & Action Icons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.monber9),
                            contentDescription = "Logo MONBER!",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .height(56.dp)
                                .wrapContentWidth()
                        )
                        Text(
                            text = "By nUbiin__",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(
                                onClick = { showNotificationsDialog = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("beranda_notification_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Notifikasi",
                                    tint = Color.White,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                            if (bellBadgeCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 7.dp, end = 6.dp)
                                        .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                                        .background(Color(0xFFEF4444), CircleShape)
                                        .border(1.5.dp, Color(0xFF005FAF), CircleShape)
                                        .padding(horizontal = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (bellBadgeCount > 99) "99+" else bellBadgeCount.toString(),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onOpenGlobalSearch,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("beranda_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari Nasabah",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Greeting kept on the left so the right-side 3D people artwork in bg_beranda stays clear
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.58f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = loggedInPN.isBlank(), onClick = onOpenLogin)
                ) {
                    Text(
                        text = "Selamat Datang,",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 22.sp
                    )
                    Text(
                        text = displayName,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "Kelola nasabah, pantau tunggakan\ndengan lebih mudah",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color.White.copy(alpha = 0.90f)
                    )
                }
            }

            // Sync progress indicator pinned right below the frozen hero
            if (isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = Color(0xFF005FAF),
                    trackColor = Color(0xFFE0ECFA)
                )
            }

            // 2. Scrollable area from Kelolaan Nasabah button card downward
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .verticalScroll(scrollState)
            ) {
                // Offline mini notice banner when applicable
                AnimatedVisibility(
                    visible = isShowingCachedOfflineData,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                    ),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                    )
                ) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.refreshSpreadsheetData(force = true) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Offline",
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Mode offline • Menampilkan cache lokal Room",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E3A8A),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Sinkron",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF005FAF)
                            )
                        }
                    }
                }

                // Main White Dashboard Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 6.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
            // Top Action Card (swapped): Rencana Kunjungan
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .testTag("beranda_kunjungan_button")
                    .clickable(onClick = onOpenKunjungan),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFFFF8F1),
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFFED7AA))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFFEDD5), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = "Kunjungan",
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rencana Kunjungan",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C2D12),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 6.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (loggedInPN.isNotBlank()) {
                                BerandaDebtorCountBadge(
                                    countText = formatter.format(kunjunganKeys.size),
                                    accentColor = if (kunjunganKeys.isNotEmpty()) Color(0xFFEA580C) else Color(0xFF94A3B8),
                                    contentDescription = "Total Nasabah Kunjungan"
                                )
                            }
                        }
                        Text(
                            text = if (loggedInPN.isBlank()) {
                                "Login PN untuk mengelola rencana kunjungan"
                            } else {
                                "Tgl $activeVisitDateStr"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF9A3412),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEA580C)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Kunjungan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Second Action Card (swapped): Kelolaan Nasabah
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .testTag("beranda_kelolaan_card")
                    .clickable(onClick = onOpenKelolaan),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFF8FBFF),
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, Color(0xFFE2ECF8))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFE1EEFF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Kelolaan Nasabah",
                            tint = Color(0xFF005FAF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kelolaan Nasabah",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (loggedInPN.isNotBlank()) {
                                BerandaDebtorCountBadge(
                                    countText = formatter.format(kelolaanActiveSummary.debtors),
                                    accentColor = if (kelolaanActiveSummary.debtors > 0) Color(0xFF005FAF) else Color(0xFF94A3B8),
                                    contentDescription = "Total Nasabah Kelolaan"
                                )
                            }
                        }
                        val kelolaanSubtitle = if (loggedInPN.isBlank()) {
                            "Login PN Mantri untuk melihat kelolaan pribadi"
                        } else {
                            val dateSuffix = if (latestDateText.isNotBlank()) " • $latestDateText" else ""
                            "${formatFullIdr(kelolaanActiveSummary.os, formatter)}$dateSuffix"
                        }
                        Text(
                            text = kelolaanSubtitle,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Buka Kelolaan",
                        tint = Color(0xFF005FAF),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 2 Side-by-Side Popup Action Buttons: Kualitas & Segmen (with total-data badge indicators)
            val kualitasTotalDeb = (lancarDeb + smlDeb + nplDeb).takeIf { it > 0 } ?: activeScopeSummary.debtors
            val segmenTotalCount = segmenKelolaanGroups.size
            val segmenTotalDeb = remember(segmenKelolaanGroups) { segmenKelolaanGroups.sumOf { it.third } }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 66.dp)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showKualitasSheet = true }
                        .testTag("beranda_btn_kualitas"),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FBFF),
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 11.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(Color(0xFFDBEAFE), RoundedCornerShape(9.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = "Kualitas Kredit",
                                        tint = Color(0xFF005FAF),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Text(
                                    text = "Kualitas",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            BerandaDebtorCountBadge(
                                countText = formatter.format(kualitasTotalDeb),
                                accentColor = if (kualitasTotalDeb > 0) Color(0xFF005FAF) else Color(0xFF94A3B8),
                                contentDescription = "Total Data Kualitas",
                                modifier = Modifier.testTag("beranda_badge_kualitas")
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SML ${String.format(Locale.US, "%.1f", smlOsPct)}% • NPL ${String.format(Locale.US, "%.1f", nplOsPct)}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Lihat Kualitas",
                                tint = Color(0xFF005FAF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 66.dp)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showSegmenSheet = true }
                        .testTag("beranda_btn_segmen"),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FBFF),
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, Color(0xFFE0E7FF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 11.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(Color(0xFFE0E7FF), RoundedCornerShape(9.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = "Segmen Kelolaan",
                                        tint = Color(0xFF4338CA),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Text(
                                    text = "Segmen",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            BerandaDebtorCountBadge(
                                countText = formatter.format(segmenTotalDeb),
                                accentColor = if (segmenTotalDeb > 0) Color(0xFF4338CA) else Color(0xFF94A3B8),
                                contentDescription = "Total Data Segmen",
                                modifier = Modifier.testTag("beranda_badge_segmen")
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatter.format(segmenTotalCount)} Segmen • ${effectiveScope.label}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Lihat Segmen",
                                tint = Color(0xFF4338CA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Pantauan Khusus Section (below Kualitas & Segmen): Tusim, Tucil <100 rb, Tunggakan <6 bln
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("beranda_pantauan_khusus_section"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Pantauan Tunggakan",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Text(
                        text = effectiveScope.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BerandaAlertMiniCard(
                        title = "Tusim",
                        osText = formatFullIdr(tusimSummary.os, formatter),
                        debText = formatter.format(tusimSummary.debtors),
                        hasDebtors = tusimSummary.debtors > 0,
                        accentColor = Color(0xFF0F766E),
                        containerColor = Color(0xFFF0FDFA),
                        borderColor = Color(0xFF99F6E4),
                        onClick = { activePantauanCategory = PantauanCategory.TUSIM },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("beranda_card_tusim")
                    )
                    BerandaAlertMiniCard(
                        title = "Tucil <100 rb",
                        osText = formatFullIdr(tucilSummary.os, formatter),
                        debText = formatter.format(tucilSummary.debtors),
                        hasDebtors = tucilSummary.debtors > 0,
                        accentColor = Color(0xFFC2410C),
                        containerColor = Color(0xFFFFF7ED),
                        borderColor = Color(0xFFFED7AA),
                        onClick = { activePantauanCategory = PantauanCategory.TUCIL },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("beranda_card_tucil")
                    )
                    BerandaAlertMiniCard(
                        title = "Tunggakan <6 bln",
                        osText = formatFullIdr(tunggakanUnder6Summary.os, formatter),
                        debText = formatter.format(tunggakanUnder6Summary.debtors),
                        hasDebtors = tunggakanUnder6Summary.debtors > 0,
                        accentColor = Color(0xFFBE123C),
                        containerColor = Color(0xFFFFF1F2),
                        borderColor = Color(0xFFFECDD3),
                        onClick = { activePantauanCategory = PantauanCategory.TUNGGAKAN_UNDER_6 },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("beranda_card_tunggakan_under_6")
                    )
                }
            }

            // Realisasi & Netdisburse Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color(0xFF005FAF),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Realisasi & Netdisburse",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (loggedInPN.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFEBF4FF),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable {
                                    berandaScope = if (berandaScope == BerandaScope.KELOLAAN) {
                                        BerandaScope.UNIT
                                    } else {
                                        BerandaScope.KELOLAAN
                                    }
                                    selectedChartMonthIndex = -1
                                }
                                .testTag("beranda_scope_toggle")
                        ) {
                            Text(
                                text = effectiveScope.label,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF005FAF),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenDashboard)
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .testTag("beranda_lihat_detail"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lihat Detail",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF005FAF)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Lihat Detail Dashboard",
                            tint = Color(0xFF005FAF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Blue Gradient Rounded Bar Chart Card: Realisasi (belakang) & Netdisburse (depan)
            RealisasiBarChartCard(
                points = realisasiPoints,
                selectedPeriodMonths = realisasiPeriodMonths,
                onSelectPeriodMonths = { months ->
                    realisasiPeriodMonths = months
                    selectedChartMonthIndex = -1
                },
                selectedMonthIndex = selectedChartMonthIndex,
                onSelectMonthIndex = { idx ->
                    selectedChartMonthIndex = if (selectedChartMonthIndex == idx) -1 else idx
                },
                scopeLabel = effectiveScope.label,
                formatter = formatter
            )

            // Tren Historis NPL & SML Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Tren Historis NPL & SML",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = effectiveScope.label,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Dual Line Chart Card: Portfolio Health (SML & NPL Historical Trend)
            PortfolioHealthLineChartCard(
                points = kolekTrendPoints,
                selectedPeriodMonths = trendPeriodMonths,
                onSelectPeriodMonths = { months ->
                    trendPeriodMonths = months
                    selectedTrendMonthIndex = -1
                },
                selectedMonthIndex = selectedTrendMonthIndex,
                onSelectMonthIndex = { idx ->
                    selectedTrendMonthIndex = if (selectedTrendMonthIndex == idx) -1 else idx
                },
                activeFocus = trendFocus,
                onSelectFocus = { target ->
                    trendFocus = if (trendFocus == target) KolekTrendFocus.ALL else target
                },
                scopeLabel = effectiveScope.label,
                formatter = formatter
            )
        }
            }
        }
    }

    if (showNotificationsDialog) {
        NotifikasiNasabahBottomSheet(
            customers = notificationCustomers,
            scopeLabel = effectiveScope.label,
            activeVisitDateStr = activeVisitDateStr,
            hasUpdateAvailable = hasUpdateAvailable,
            updateVersionTag = updateVersionTag,
            isOfflineMode = isShowingCachedOfflineData,
            formatter = formatter,
            onOpenUpdateDialog = {
                showNotificationsDialog = false
                onOpenUpdateDialog()
            },
            onRetrySync = {
                showNotificationsDialog = false
                viewModel.refreshSpreadsheetData(force = true)
            },
            onDismiss = { showNotificationsDialog = false }
        )
    }

    if (showKualitasSheet) {
        KualitasPopupBottomSheet(
            totalOs = totalOs,
            lancarOs = lancarOs,
            lancarDeb = lancarDeb,
            lancarPct = lancarOsPct,
            smlOs = smlOs,
            smlDeb = smlDeb,
            smlPct = smlOsPct,
            nplOs = nplOs,
            nplDeb = nplDeb,
            nplPct = nplOsPct,
            scopeLabel = effectiveScope.label,
            formatter = formatter,
            onSelectKolek = { targetKolek ->
                showKualitasSheet = false
                onOpenCustomerListPage(
                    BerandaCustomerListPageRequest(
                        filterType = BerandaListFilterType.KUALITAS,
                        filterValue = targetKolek,
                        scopeLabel = effectiveScope.label,
                        customers = resolvedBerandaCustomers
                    )
                )
            },
            onDismiss = { showKualitasSheet = false }
        )
    }

    if (showSegmenSheet) {
        SegmenPopupBottomSheet(
            segmenGroups = segmenKelolaanGroups,
            totalOs = totalOs,
            scopeLabel = effectiveScope.label,
            isLoggedIn = loggedInPN.isNotBlank(),
            formatter = formatter,
            onSelectSegmen = { segName ->
                showSegmenSheet = false
                onOpenCustomerListPage(
                    BerandaCustomerListPageRequest(
                        filterType = BerandaListFilterType.SEGMEN,
                        filterValue = segName,
                        scopeLabel = effectiveScope.label,
                        customers = resolvedBerandaCustomers
                    )
                )
            },
            onDismiss = { showSegmenSheet = false }
        )
    }

    activePantauanCategory?.let { category ->
        val categoryData = when (category) {
            PantauanCategory.TUSIM -> tusimData
            PantauanCategory.TUCIL -> tucilData
            PantauanCategory.TUNGGAKAN_UNDER_6 -> tunggakanUnder6Data
        }
        PantauanNasabahBottomSheet(
            category = category,
            data = categoryData,
            scopeLabel = effectiveScope.label,
            formatter = formatter,
            onDismiss = { activePantauanCategory = null }
        )
    }
}

@Composable
private fun RealisasiBarChartCard(
    points: List<MonthlyRealisasiPoint>,
    selectedPeriodMonths: Int,
    onSelectPeriodMonths: (Int) -> Unit,
    selectedMonthIndex: Int,
    onSelectMonthIndex: (Int) -> Unit,
    scopeLabel: String,
    formatter: NumberFormat
) {
    val totalRealisasi = remember(points) { points.sumOf { it.realisasi } }
    val totalNetDisburs = remember(points) { points.sumOf { it.netDisburs } }
    val totalDeb = remember(points) { points.sumOf { it.debtors } }
    val netRatioPct = remember(totalRealisasi, totalNetDisburs) {
        if (totalRealisasi > 0.0) (totalNetDisburs / totalRealisasi) * 100.0 else 0.0
    }

    var chartAnimationKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedPeriodMonths, scopeLabel, points.size) {
        chartAnimationKey++
    }
    val barGrowthProgress by animateFloatAsState(
        targetValue = if (chartAnimationKey >= 0) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "realisasi_bar_growth"
    )

    val periodOptions = listOf(3, 6, 9, 12)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("beranda_portfolio_chart_card"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0058BE),
        shadowElevation = 5.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0B6DE0),
                            Color(0xFF0051B0),
                            Color(0xFF003B85)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Header inside chart card: Scope Caption + 3/6/9/12 Bulan Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Periode $selectedPeriodMonths Bln • $scopeLabel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            periodOptions.forEach { months ->
                                val isSelected = selectedPeriodMonths == months
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    modifier = Modifier
                                        .heightIn(min = 28.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { onSelectPeriodMonths(months) }
                                        .testTag("chart_period_${months}m")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${months}B",
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (isSelected) Color(0xFF0051B0) else Color.White.copy(alpha = 0.90f),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Borderless Top Metrics: Realisasi (kiri) & Netdisburse (kanan) sejajar secara horizontal
                // Info debitur dan % dari Realisasi berada di sebelah kanan info angkanya
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chart_metric_realisasi"),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 10.dp, height = 7.dp)
                                    .background(Color(0xFFBAE6FD).copy(alpha = 0.50f), RoundedCornerShape(999.dp))
                                    .border(0.8.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
                            )
                            Text(
                                text = "Realisasi",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 1
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val realText = formatFullIdr(totalRealisasi, formatter)
                            Text(
                                text = realText,
                                fontSize = adaptiveOsFontSize(realText, maxSp = 11.4f),
                                letterSpacing = (-0.3).sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.14f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Debitur",
                                        tint = Color(0xFFBAE6FD),
                                        modifier = Modifier.size(9.dp)
                                    )
                                    Text(
                                        text = "${formatter.format(totalDeb)} Deb",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE0F2FE),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(34.dp)
                            .background(Color.White.copy(alpha = 0.18f))
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chart_metric_netdisburs"),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 10.dp, height = 7.dp)
                                    .background(Color(0xFF34D399), RoundedCornerShape(999.dp))
                            )
                            Text(
                                text = "Netdisburse",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 1
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val netText = formatSignedFullIdr(totalNetDisburs, formatter)
                            Text(
                                text = netText,
                                fontSize = adaptiveOsFontSize(netText, maxSp = 11.4f),
                                letterSpacing = (-0.3).sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFA7F3D0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", netRatioPct)}%",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA7F3D0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                    }
                }

                // Selected Month Inspection Tooltip (on tap)
                val inspectedPoint = points.getOrNull(selectedMonthIndex)
                AnimatedVisibility(
                    visible = inspectedPoint != null,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                    ),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                    )
                ) {
                    inspectedPoint?.let { pt ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.24f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (pt.hasData) {
                                    "${pt.fullLabel} (${formatter.format(pt.debtors)} Debitur): Realisasi ${formatFullIdr(pt.realisasi, formatter)} • Netdisburs ${formatSignedFullIdr(pt.netDisburs, formatter)}"
                                } else {
                                    "${pt.fullLabel}: Belum ada realisasi tercatat"
                                },
                                fontSize = 9.5.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Rounded Layered Bar Chart Canvas:
                // Realisasi rendered BEHIND (shadow/backdrop) and Netdisburse rendered IN FRONT
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                        .testTag("beranda_realisasi_bar_chart")
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                if (points.isNotEmpty() && size.width > 0) {
                                    val padPx = 4.dp.toPx()
                                    val plotW = (size.width.toFloat() - padPx * 2f).coerceAtLeast(1f)
                                    val slotW = plotW / points.size.toFloat()
                                    val idx = ((offset.x - padPx) / slotW).toInt().coerceIn(0, points.lastIndex)
                                    onSelectMonthIndex(idx)
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val topPad = 8.dp.toPx()
                        val bottomPad = 4.dp.toPx()
                        val chartH = (h - topPad - bottomPad).coerceAtLeast(12f)
                        val count = points.size.coerceAtLeast(1)
                        val leftPad = 4.dp.toPx()
                        val rightPad = 4.dp.toPx()
                        val plotW = (w - leftPad - rightPad).coerceAtLeast(12f)
                        val slotW = plotW / count.toFloat()
                        val bottomY = topPad + chartH

                        // Subtle horizontal guide lines
                        for (g in 0..3) {
                            val gy = topPad + (chartH / 3f) * g
                            drawLine(
                                color = Color.White.copy(alpha = 0.14f),
                                start = Offset(leftPad, gy),
                                end = Offset(w - rightPad, gy),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val maxVal = points.maxOfOrNull {
                            maxOf(it.realisasi, it.netDisburs.coerceAtLeast(0.0))
                        }?.takeIf { it > 0.0 } ?: 1.0

                        val backBarW = (slotW * 0.54f).coerceIn(10.dp.toPx(), 26.dp.toPx())
                        val frontBarW = (backBarW * 0.72f).coerceIn(7.dp.toPx(), 19.dp.toPx())
                        val layerOffset = ((backBarW - frontBarW) * 0.45f + 1.5.dp.toPx()).coerceAtMost(slotW * 0.14f)

                        points.forEachIndexed { idx, pt ->
                            val cx = leftPad + slotW * idx + slotW * 0.5f
                            val isSelectedBar = idx == selectedMonthIndex
                            val dimAlpha = if (selectedMonthIndex == -1 || isSelectedBar) 1f else 0.45f

                            if (isSelectedBar) {
                                val highlightW = (slotW * 0.84f).coerceAtLeast(backBarW + 6.dp.toPx())
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.12f),
                                    topLeft = Offset(cx - highlightW * 0.5f, topPad * 0.5f),
                                    size = Size(highlightW, chartH + topPad * 0.5f),
                                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                                )
                            }

                            val realVal = pt.realisasi.coerceAtLeast(0.0)
                            val netVal = pt.netDisburs.coerceAtLeast(0.0)

                            if (realVal <= 0.0 && netVal <= 0.0) {
                                val emptyH = 4.dp.toPx()
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.18f * dimAlpha),
                                    topLeft = Offset(cx - backBarW * 0.45f, bottomY - emptyH),
                                    size = Size(backBarW * 0.9f, emptyH),
                                    cornerRadius = CornerRadius(emptyH * 0.5f, emptyH * 0.5f)
                                )
                            } else {
                                // 1. Back Bar: Realisasi (shadow/backdrop rounded bar behind Netdisburse)
                                if (realVal > 0.0) {
                                    val backH = ((realVal / maxVal).toFloat() * chartH * barGrowthProgress)
                                        .coerceAtLeast(backBarW)
                                        .coerceAtMost(chartH)
                                    val backTop = bottomY - backH
                                    val backLeft = cx - backBarW * 0.5f - layerOffset * 0.55f
                                    val backRadius = CornerRadius(backBarW * 0.5f, backBarW * 0.5f)

                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFFE0F2FE).copy(alpha = 0.44f * dimAlpha),
                                                Color(0xFF93C5FD).copy(alpha = 0.22f * dimAlpha)
                                            ),
                                            startY = backTop,
                                            endY = bottomY
                                        ),
                                        topLeft = Offset(backLeft, backTop),
                                        size = Size(backBarW, backH),
                                        cornerRadius = backRadius
                                    )
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.42f * dimAlpha),
                                        topLeft = Offset(backLeft, backTop),
                                        size = Size(backBarW, backH),
                                        cornerRadius = backRadius,
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }

                                // 2. Front Bar: Netdisburse (vivid emerald rounded bar in front of Realisasi)
                                if (netVal > 0.0) {
                                    val frontH = ((netVal / maxVal).toFloat() * chartH * barGrowthProgress)
                                        .coerceAtLeast(frontBarW)
                                        .coerceAtMost(chartH)
                                    val frontTop = bottomY - frontH
                                    val frontLeft = cx - frontBarW * 0.5f + layerOffset * 0.65f
                                    val frontRadius = CornerRadius(frontBarW * 0.5f, frontBarW * 0.5f)

                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF6EE7B7).copy(alpha = dimAlpha),
                                                Color(0xFF10B981).copy(alpha = dimAlpha)
                                            ),
                                            startY = frontTop,
                                            endY = bottomY
                                        ),
                                        topLeft = Offset(frontLeft, frontTop),
                                        size = Size(frontBarW, frontH),
                                        cornerRadius = frontRadius
                                    )
                                }
                            }
                        }
                    }
                }

                // Month X-Axis Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEachIndexed { idx, pt ->
                        val isActive = idx == selectedMonthIndex || (selectedMonthIndex == -1 && idx == points.lastIndex)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectMonthIndex(idx) }
                                .testTag("beranda_realisasi_month_label_$idx")
                        ) {
                            Text(
                                text = pt.shortLabel,
                                fontSize = if (points.size <= 6) 10.sp else 8.5.sp,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isActive) Color.White else if (pt.hasData) Color.White.copy(alpha = 0.80f) else Color.White.copy(alpha = 0.42f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimatedBerandaBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable (requestDismiss: (afterDismiss: (() -> Unit)?) -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val initialSlidePx = remember(density) { with(density) { 44.dp.toPx() } }
    val exitSlidePx = remember(density) { with(density) { 56.dp.toPx() } }

    val contentAlpha = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(initialSlidePx) }
    var isDismissing by remember { mutableStateOf(false) }
    var hasBeenVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch {
            contentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
        }
        launch {
            contentOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    LaunchedEffect(sheetState.currentValue, sheetState.targetValue) {
        if (sheetState.currentValue != SheetValue.Hidden) {
            hasBeenVisible = true
        } else if (hasBeenVisible && sheetState.targetValue == SheetValue.Hidden && !isDismissing) {
            launch {
                contentAlpha.animateTo(0f, tween(durationMillis = 180, easing = FastOutSlowInEasing))
            }
            launch {
                contentOffsetY.animateTo(exitSlidePx, tween(durationMillis = 200, easing = FastOutSlowInEasing))
            }
        }
    }

    val requestDismiss: (afterDismiss: (() -> Unit)?) -> Unit = { afterDismiss ->
        if (!isDismissing) {
            isDismissing = true
            coroutineScope.launch {
                val fadeJob = launch {
                    contentAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing)
                    )
                }
                val slideJob = launch {
                    contentOffsetY.animateTo(
                        targetValue = exitSlidePx,
                        animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing)
                    )
                }
                try {
                    sheetState.hide()
                    fadeJob.join()
                    slideJob.join()
                    onDismiss()
                    afterDismiss?.invoke()
                } catch (_: CancellationException) {
                    // Sheet disposed externally
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isDismissing) {
                isDismissing = true
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = contentAlpha.value
                    translationY = contentOffsetY.value
                }
        ) {
            content(requestDismiss)
        }
    }
}

@Composable
private fun KualitasPopupBottomSheet(
    totalOs: Double,
    lancarOs: Double,
    lancarDeb: Int,
    lancarPct: Double,
    smlOs: Double,
    smlDeb: Int,
    smlPct: Double,
    nplOs: Double,
    nplDeb: Int,
    nplPct: Double,
    scopeLabel: String,
    formatter: NumberFormat,
    onSelectKolek: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val totalDeb = lancarDeb + smlDeb + nplDeb
    val safeSumOs = (lancarOs + smlOs + nplOs).coerceAtLeast(1.0)

    AnimatedBerandaBottomSheet(onDismiss = onDismiss) { requestDismiss ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .testTag("beranda_kualitas_bottom_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kualitas Kredit",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFEBF4FF)
                        ) {
                            Text(
                                text = scopeLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF005FAF),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Total Debitur",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = formatter.format(totalDeb),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                        Text(
                            text = "• ${formatFullIdr(totalOs, formatter)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = { requestDismiss(null) },
                    modifier = Modifier.testTag("beranda_kualitas_sheet_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup rincian kualitas",
                        tint = Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simple stacked proportion bar (Lancar, SML, NPL)
            val lWeight = (lancarOs / safeSumOs).toFloat().coerceAtLeast(0.01f)
            val sWeight = (smlOs / safeSumOs).toFloat().coerceAtLeast(0f)
            val nWeight = (nplOs / safeSumOs).toFloat().coerceAtLeast(0f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFF1F5F9)),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (lancarOs > 0.0 || (smlOs == 0.0 && nplOs == 0.0)) {
                    Box(
                        modifier = Modifier
                            .weight(lWeight)
                            .fillMaxHeight()
                            .background(Color(0xFF16A34A))
                    )
                }
                if (sWeight > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(sWeight)
                            .fillMaxHeight()
                            .background(Color(0xFFD97706))
                    )
                }
                if (nWeight > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(nWeight)
                            .fillMaxHeight()
                            .background(Color(0xFFDC2626))
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KualitasPopupItemCard(
                    title = "Lancar",
                    subtitle = "Kolektibilitas 1 (Lancar)",
                    osText = formatFullIdr(lancarOs, formatter),
                    debText = formatter.format(lancarDeb),
                    percentage = lancarPct,
                    accentColor = Color(0xFF16A34A),
                    containerColor = Color(0xFFF0FDF4),
                    borderColor = Color(0xFFBBF7D0),
                    onClick = { requestDismiss { onSelectKolek("Lancar") } },
                    modifier = Modifier.testTag("beranda_popup_kualitas_lancar")
                )
                KualitasPopupItemCard(
                    title = "SML",
                    subtitle = "Dalam Perhatian Khusus (DPK)",
                    osText = formatFullIdr(smlOs, formatter),
                    debText = formatter.format(smlDeb),
                    percentage = smlPct,
                    accentColor = Color(0xFFD97706),
                    containerColor = Color(0xFFFFFBEB),
                    borderColor = Color(0xFFFDE68A),
                    onClick = { requestDismiss { onSelectKolek("SML") } },
                    modifier = Modifier.testTag("beranda_popup_kualitas_sml")
                )
                KualitasPopupItemCard(
                    title = "NPL",
                    subtitle = "Kurang Lancar, Diragukan & Macet",
                    osText = formatFullIdr(nplOs, formatter),
                    debText = formatter.format(nplDeb),
                    percentage = nplPct,
                    accentColor = Color(0xFFDC2626),
                    containerColor = Color(0xFFFEF2F2),
                    borderColor = Color(0xFFFECACA),
                    onClick = { requestDismiss { onSelectKolek("NPL") } },
                    modifier = Modifier.testTag("beranda_popup_kualitas_npl")
                )
            }
        }
    }
}

@Composable
private fun KualitasPopupItemCard(
    title: String,
    subtitle: String,
    osText: String,
    debText: String,
    percentage: Double,
    accentColor: Color,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pctText = if (percentage >= 99.95) {
        "100%"
    } else {
        "${String.format(Locale.US, "%.1f", percentage)}%"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color.White, CircleShape)
                    .border(2.dp, accentColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pctText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    maxLines = 1
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Jumlah Debitur",
                        tint = accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = debText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        maxLines = 1
                    )
                }
                Text(
                    text = osText,
                    fontSize = adaptiveOsFontSize(osText, maxSp = 12.5f),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SegmenPopupBottomSheet(
    segmenGroups: List<Triple<String, Double, Int>>,
    totalOs: Double,
    scopeLabel: String,
    isLoggedIn: Boolean,
    formatter: NumberFormat,
    onSelectSegmen: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val segmenTotalOs = remember(segmenGroups, totalOs) {
        segmenGroups.sumOf { it.second }.takeIf { it > 0.0 } ?: totalOs
    }
    val totalDeb = remember(segmenGroups) { segmenGroups.sumOf { it.third } }

    AnimatedBerandaBottomSheet(onDismiss = onDismiss) { requestDismiss ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .testTag("beranda_segmen_bottom_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Segmen Kelolaan",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFE0E7FF)
                        ) {
                            Text(
                                text = "$scopeLabel • ${segmenGroups.size} Segmen",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4338CA),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Total Debitur",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = formatter.format(totalDeb),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { requestDismiss(null) },
                    modifier = Modifier.testTag("beranda_segmen_sheet_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup rincian segmen",
                        tint = Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FBFF),
                border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total OS Segmen",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                    val totalSegmenOsText = formatFullIdr(segmenTotalOs, formatter)
                    Text(
                        text = totalSegmenOsText,
                        fontSize = adaptiveOsFontSize(totalSegmenOsText, maxSp = 13f),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF005FAF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (segmenGroups.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFE0E7FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = Color(0xFF4338CA),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = if (isLoggedIn) "Data Segmen Belum Tersedia" else "Belum Login PN Mantri",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isLoggedIn) {
                                "Data rincian segmen pinjaman belum dimuat untuk cakupan $scopeLabel."
                            } else {
                                "Login PN Mantri untuk melihat rincian segmen kelolaan pribadi."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val segmenListState = rememberLazyListState()
                val pagedSegmenGroups = rememberLazyPagedItems(
                    items = segmenGroups,
                    listState = segmenListState,
                    pageSize = 20,
                    prefetchDistance = 5,
                    resetKey = scopeLabel
                )
                LazyColumn(
                    state = segmenListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .testTag("beranda_segmen_sheet_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(
                        items = pagedSegmenGroups.visibleItems,
                        key = { idx, item -> "${item.first}_$idx" }
                    ) { idx, (segName, segOs, segDeb) ->
                        val sharePct = if (segmenTotalOs > 0.0) (segOs / segmenTotalOs) * 100.0 else 0.0
                        val progressRatio = (sharePct / 100.0).toFloat().coerceIn(0f, 1f)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { requestDismiss { onSelectSegmen(segName) } }
                                .testTag("beranda_segmen_${segName.lowercase().replace(" ", "_")}"),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(Color(0xFFE0E7FF), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${idx + 1}",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF4338CA)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = segName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", sharePct)}% dari Total OS",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Jumlah Debitur",
                                                tint = Color(0xFF005FAF),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = formatter.format(segDeb),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                        val segOsText = formatFullIdr(segOs, formatter)
                                        Text(
                                            text = segOsText,
                                            fontSize = adaptiveOsFontSize(segOsText, maxSp = 12f),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF005FAF),
                                            maxLines = 1
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFFE2E8F0))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = progressRatio.coerceAtLeast(0.02f))
                                            .fillMaxHeight()
                                            .background(Color(0xFF005FAF), RoundedCornerShape(999.dp))
                                    )
                                }
                            }
                        }
                    }

                    if (pagedSegmenGroups.hasMore) {
                        item(key = "segmen_sheet_lazy_footer") {
                            LazyLoadingFooterRow(
                                visibleCount = pagedSegmenGroups.visibleItems.size,
                                totalCount = pagedSegmenGroups.totalCount,
                                accentColor = Color(0xFF4338CA)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PantauanNasabahBottomSheet(
    category: PantauanCategory,
    data: BerandaAlertCategoryData,
    scopeLabel: String,
    formatter: NumberFormat,
    onDismiss: () -> Unit
) {
    var searchQuery by remember(category, scopeLabel) { mutableStateOf("") }

    val filteredCustomers by remember(data.customers, searchQuery) {
        derivedStateOf {
            val q = searchQuery.trim()
            if (q.isEmpty()) {
                data.customers
            } else {
                data.customers.filter { item ->
                    item.namaNasabah.contains(q, ignoreCase = true) ||
                        item.noRekening.contains(q, ignoreCase = true) ||
                        item.namaMantri.contains(q, ignoreCase = true) ||
                        item.segmen.contains(q, ignoreCase = true)
                }
            }
        }
    }

    val totalTunggakan = remember(data.customers) {
        data.customers.sumOf { it.totalTunggakan }
    }

    AnimatedBerandaBottomSheet(onDismiss = onDismiss) { requestDismiss ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .testTag("beranda_pantauan_bottom_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = category.containerColor,
                            border = BorderStroke(1.dp, category.borderColor)
                        ) {
                            Text(
                                text = scopeLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = category.accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Jumlah Debitur",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${formatter.format(data.summary.debtors)} Debitur",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { requestDismiss(null) },
                    modifier = Modifier.testTag("beranda_pantauan_sheet_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup daftar nasabah",
                        tint = Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = category.subtitle,
                fontSize = 11.5.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = category.containerColor,
                    border = BorderStroke(1.dp, category.borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Total OS",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = category.accentColor
                        )
                        val totalOsText = formatFullIdr(data.summary.os, formatter)
                        Text(
                            text = totalOsText,
                            fontSize = adaptiveOsFontSize(totalOsText, maxSp = 13f),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Total Tunggakan",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                        val totalTgkText = formatFullIdr(totalTunggakan, formatter)
                        Text(
                            text = totalTgkText,
                            fontSize = adaptiveOsFontSize(totalTgkText, maxSp = 13f),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFBE123C),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (data.customers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("beranda_pantauan_sheet_search"),
                    placeholder = {
                        Text(
                            text = "Cari nama nasabah, rekening, atau mantri...",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus pencarian",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = category.accentColor,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "Daftar Nasabah (${filteredCustomers.size})"
                        } else {
                            "Hasil Pencarian (${filteredCustomers.size} dari ${data.customers.size})"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "Tunggakan & OS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (data.customers.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(category.containerColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = category.accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Tidak Ada Nasabah ${category.title}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Saat ini tidak ada nasabah yang masuk kategori ${category.title} pada cakupan $scopeLabel.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ditemukan nasabah yang cocok dengan \"$searchQuery\".",
                        fontSize = 12.5.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val pantauanListState = rememberLazyListState()
                val trimmedPantauanQuery = searchQuery.trim()
                LaunchedEffect(trimmedPantauanQuery) {
                    if (pantauanListState.firstVisibleItemIndex > 0) {
                        pantauanListState.scrollToItem(0)
                    }
                }
                val pagedPantauan = rememberLazyPagedItems(
                    items = filteredCustomers,
                    listState = pantauanListState,
                    pageSize = 30,
                    prefetchDistance = 6,
                    resetKey = category to trimmedPantauanQuery
                )
                LazyColumn(
                    state = pantauanListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .testTag("beranda_pantauan_sheet_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(
                        items = pagedPantauan.visibleItems,
                        key = { idx, item -> "${item.noRekening}_${item.namaNasabah}_$idx" }
                    ) { idx, item ->
                        PantauanCustomerCard(
                            index = idx + 1,
                            category = category,
                            item = item,
                            showMantriName = scopeLabel.equals("Unit", ignoreCase = true),
                            formatter = formatter,
                            modifier = Modifier
                                .animateItem()
                                .testTag("beranda_pantauan_sheet_item_$idx")
                        )
                    }

                    if (pagedPantauan.hasMore) {
                        item(key = "pantauan_sheet_lazy_footer") {
                            LazyLoadingFooterRow(
                                visibleCount = pagedPantauan.visibleItems.size,
                                totalCount = pagedPantauan.totalCount,
                                accentColor = category.accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PantauanCustomerCard(
    index: Int,
    category: PantauanCategory,
    item: BerandaAlertCustomerItem,
    showMantriName: Boolean,
    formatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    val metaParts = buildList {
        if (item.noRekening.isNotBlank()) add(item.noRekening)
        if (item.kolek.isNotBlank() && item.kolek != "-") add(item.kolek)
        if (item.segmen.isNotBlank()) add(item.segmen)
        if (showMantriName && item.namaMantri.isNotBlank() && item.namaMantri != "Blank PN") {
            add(item.namaMantri)
        }
    }

    val highlightTag = when (category) {
        PantauanCategory.TUSIM -> "Saldo Tabungan: ${formatFullIdr(item.saldoTabungan, formatter)}"
        PantauanCategory.TUCIL -> if (item.umurTunggakanHari > 0) "Umur Tgk: ${item.umurTunggakanHari} hari" else "Tunggakan ringan"
        PantauanCategory.TUNGGAKAN_UNDER_6 -> {
            val ageText = item.lamaPinjamanBulan?.let { "$it bln" } ?: "<6 bln"
            if (item.umurTunggakanHari > 0) "Usia Pinjaman: $ageText • Tgk ${item.umurTunggakanHari} hr" else "Usia Pinjaman: $ageText"
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(category.containerColor, CircleShape)
                    .border(1.dp, category.borderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = category.accentColor,
                    maxLines = 1
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.namaNasabah,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (metaParts.isNotEmpty()) {
                    Text(
                        text = metaParts.joinToString(" • "),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = category.containerColor,
                    border = BorderStroke(0.8.dp, category.borderColor)
                ) {
                    Text(
                        text = highlightTag,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = category.accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val tgkText = formatFullIdr(item.totalTunggakan, formatter)
                Text(
                    text = tgkText,
                    fontSize = adaptiveOsFontSize(tgkText, maxSp = 12f),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFBE123C),
                    maxLines = 1
                )
                val osText = formatFullIdr(item.osPinjaman, formatter)
                Text(
                    text = "OS $osText",
                    fontSize = adaptiveOsFontSize(osText, maxSp = 10f),
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun NotifikasiNasabahBottomSheet(
    customers: List<BerandaNotificationCustomerItem>,
    scopeLabel: String,
    activeVisitDateStr: String,
    hasUpdateAvailable: Boolean,
    updateVersionTag: String,
    isOfflineMode: Boolean,
    formatter: NumberFormat,
    onOpenUpdateDialog: () -> Unit,
    onRetrySync: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember(scopeLabel) { mutableStateOf("") }
    var activeTab by remember(scopeLabel) { mutableStateOf(NotifFilterTab.SEMUA) }

    val dueTodayList = remember(customers) { customers.filter { it.isJatuhTempo } }
    val kunjunganList = remember(customers) { customers.filter { it.isKunjungan } }
    val dueTodayOs = remember(dueTodayList) { dueTodayList.sumOf { it.osPinjaman } }
    val kunjunganOs = remember(kunjunganList) { kunjunganList.sumOf { it.osPinjaman } }

    val filteredCustomers by remember(customers, activeTab, searchQuery) {
        derivedStateOf {
            val tabFiltered = when (activeTab) {
                NotifFilterTab.SEMUA -> customers
                NotifFilterTab.JATUH_TEMPO -> customers.filter { it.isJatuhTempo }
                NotifFilterTab.KUNJUNGAN -> customers.filter { it.isKunjungan }
            }
            val q = searchQuery.trim()
            if (q.isEmpty()) {
                tabFiltered
            } else {
                tabFiltered.filter { item ->
                    item.namaNasabah.contains(q, ignoreCase = true) ||
                        item.noRekening.contains(q, ignoreCase = true) ||
                        item.namaMantri.contains(q, ignoreCase = true) ||
                        item.segmen.contains(q, ignoreCase = true)
                }
            }
        }
    }

    AnimatedBerandaBottomSheet(onDismiss = onDismiss) { requestDismiss ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .testTag("beranda_notif_bottom_sheet")
        ) {
            // Header (mirrors RealisasiDetailBottomSheet)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notifikasi Nasabah",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFEBF4FF)
                        ) {
                            Text(
                                text = scopeLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF005FAF),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Jumlah Debitur",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${formatter.format(customers.size)} Debitur",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { requestDismiss(null) },
                    modifier = Modifier.testTag("beranda_notif_sheet_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup notifikasi nasabah",
                        tint = Color(0xFF475569)
                    )
                }
            }

            if (hasUpdateAvailable || isOfflineMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hasUpdateAvailable) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { requestDismiss(onOpenUpdateDialog) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = Color(0xFF005FAF),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Pembaruan versi ${updateVersionTag.ifBlank { "baru" }} siap diinstal",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E3A8A),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFF005FAF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    if (isOfflineMode) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { requestDismiss(onRetrySync) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Mode offline aktif • Ketuk untuk sinkron ulang",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2 Summary Stat Cards (Jatuh Tempo Hari Ini & Rencana Kunjungan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isJtSelected = activeTab == NotifFilterTab.JATUH_TEMPO
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            activeTab = if (isJtSelected) NotifFilterTab.SEMUA else NotifFilterTab.JATUH_TEMPO
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(
                        width = if (isJtSelected) 1.6.dp else 1.dp,
                        color = if (isJtSelected) Color(0xFF005FAF) else Color(0xFFBFDBFE)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Jatuh Tempo",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D4ED8)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Debitur Jatuh Tempo",
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = formatter.format(dueTodayList.size),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8)
                                )
                            }
                        }
                        val jtOsText = formatFullIdr(dueTodayOs, formatter)
                        Text(
                            text = jtOsText,
                            fontSize = adaptiveOsFontSize(jtOsText, maxSp = 12.5f),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val isKunjSelected = activeTab == NotifFilterTab.KUNJUNGAN
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            activeTab = if (isKunjSelected) NotifFilterTab.SEMUA else NotifFilterTab.KUNJUNGAN
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF7ED),
                    border = BorderStroke(
                        width = if (isKunjSelected) 1.6.dp else 1.dp,
                        color = if (isKunjSelected) Color(0xFFEA580C) else Color(0xFFFED7AA)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kunjungan",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFC2410C)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Debitur Kunjungan",
                                    tint = Color(0xFFC2410C),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = formatter.format(kunjunganList.size),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC2410C)
                                )
                            }
                        }
                        val kOsText = formatFullIdr(kunjunganOs, formatter)
                        Text(
                            text = kOsText,
                            fontSize = adaptiveOsFontSize(kOsText, maxSp = 12.5f),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (customers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))

                // Filter Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NotifFilterTab.entries.forEach { tab ->
                        val selected = activeTab == tab
                        val count = when (tab) {
                            NotifFilterTab.SEMUA -> customers.size
                            NotifFilterTab.JATUH_TEMPO -> dueTodayList.size
                            NotifFilterTab.KUNJUNGAN -> kunjunganList.size
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) Color(0xFF005FAF) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { activeTab = tab }
                        ) {
                            Text(
                                text = "${tab.label} ($count)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("beranda_notif_sheet_search"),
                    placeholder = {
                        Text(
                            text = "Cari nama nasabah, rekening, atau mantri...",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus pencarian",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF005FAF),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "Daftar Nasabah (${filteredCustomers.size})"
                        } else {
                            "Hasil Pencarian (${filteredCustomers.size} dari ${customers.size})"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "OS & Tunggakan",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            AnimatedContent(
                targetState = Triple(customers.isEmpty(), filteredCustomers.isEmpty(), activeTab),
                transitionSpec = {
                    (fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                        slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it / 12 })
                        .togetherWith(
                            fadeOut(tween(150, easing = FastOutSlowInEasing)) +
                                slideOutVertically(tween(150, easing = FastOutSlowInEasing)) { -it / 16 }
                        )
                },
                label = "notif_list_tab_transition"
            ) { (allEmpty, filteredEmpty, currentTab) ->
                if (allEmpty) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFFEBF4FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFF005FAF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "Tidak Ada Notifikasi Nasabah",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Belum ada nasabah jatuh tempo hari ini atau rencana kunjungan untuk tanggal $activeVisitDateStr.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (filteredEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "Tidak ditemukan nasabah yang cocok dengan \"$searchQuery\"."
                            } else {
                                "Tidak ada nasabah pada kategori ${currentTab.label}."
                            },
                            fontSize = 12.5.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val notifListState = rememberLazyListState()
                    val notifResetKey = activeTab to searchQuery.trim()
                    LaunchedEffect(notifResetKey) {
                        if (notifListState.firstVisibleItemIndex > 0) {
                            notifListState.scrollToItem(0)
                        }
                    }
                    val pagedNotifs = rememberLazyPagedItems(
                        items = filteredCustomers,
                        listState = notifListState,
                        pageSize = 30,
                        prefetchDistance = 6,
                        resetKey = notifResetKey
                    )
                    LazyColumn(
                        state = notifListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .testTag("beranda_notif_sheet_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        itemsIndexed(
                            items = pagedNotifs.visibleItems,
                            key = { idx, item -> "${item.noRekening}_${item.namaNasabah}_$idx" }
                        ) { idx, item ->
                            NotifikasiCustomerCard(
                                index = idx + 1,
                                item = item,
                                activeVisitDateStr = activeVisitDateStr,
                                showMantriName = scopeLabel.equals("Unit", ignoreCase = true),
                                formatter = formatter,
                                modifier = Modifier
                                    .animateItem()
                                    .testTag("beranda_notif_sheet_item_$idx")
                            )
                        }

                        if (pagedNotifs.hasMore) {
                            item(key = "notif_sheet_lazy_footer") {
                                LazyLoadingFooterRow(
                                    visibleCount = pagedNotifs.visibleItems.size,
                                    totalCount = pagedNotifs.totalCount
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotifikasiCustomerCard(
    index: Int,
    item: BerandaNotificationCustomerItem,
    activeVisitDateStr: String,
    showMantriName: Boolean,
    formatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    val metaParts = buildList {
        if (item.noRekening.isNotBlank()) add(item.noRekening)
        if (item.kolek.isNotBlank() && item.kolek != "-") add(item.kolek)
        if (item.segmen.isNotBlank()) add(item.segmen)
        if (showMantriName && item.namaMantri.isNotBlank() && item.namaMantri != "Blank PN") {
            add(item.namaMantri)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFFDBEAFE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF005FAF),
                    maxLines = 1
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.namaNasabah,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (metaParts.isNotEmpty()) {
                    Text(
                        text = metaParts.joinToString(" • "),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.isJatuhTempo) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(0.8.dp, Color(0xFFBFDBFE))
                        ) {
                            Text(
                                text = if (item.npd.isNotBlank()) "Jatuh Tempo Tgl ${item.npd}" else "Jatuh Tempo",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (item.isKunjungan) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF7ED),
                            border = BorderStroke(0.8.dp, Color(0xFFFED7AA))
                        ) {
                            Text(
                                text = "Kunjungan ($activeVisitDateStr)",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC2410C),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val osText = formatFullIdr(item.osPinjaman, formatter)
                Text(
                    text = osText,
                    fontSize = adaptiveOsFontSize(osText, maxSp = 12f),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0051B0),
                    maxLines = 1
                )
                if (item.totalTunggakan > 0.0) {
                    val tgkText = formatFullIdr(item.totalTunggakan, formatter)
                    Text(
                        text = "Tgk: $tgkText",
                        fontSize = adaptiveOsFontSize(tgkText, maxSp = 10f),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFBE123C),
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "Lancar",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF059669),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun PortfolioHealthLineChartCard(
    points: List<MonthlyKolekTrendPoint>,
    selectedPeriodMonths: Int,
    onSelectPeriodMonths: (Int) -> Unit,
    selectedMonthIndex: Int,
    onSelectMonthIndex: (Int) -> Unit,
    activeFocus: KolekTrendFocus,
    onSelectFocus: (KolekTrendFocus) -> Unit,
    scopeLabel: String,
    formatter: NumberFormat
) {
    val smlColor = Color(0xFFD97706)
    val nplColor = Color(0xFFE11D48)

    val validPoints = remember(points) { points.filter { it.hasData } }
    val activeIdx = if (selectedMonthIndex in points.indices && points[selectedMonthIndex].hasData) {
        selectedMonthIndex
    } else {
        points.indexOfLast { it.hasData }
    }
    val currentPoint = points.getOrNull(activeIdx)
    val prevPoint = remember(points, activeIdx) {
        if (activeIdx > 0) {
            (activeIdx - 1 downTo 0).firstNotNullOfOrNull { i ->
                points[i].takeIf { it.hasData }
            }
        } else null
    }

    val smlDelta = if (currentPoint != null && prevPoint != null) currentPoint.smlPct - prevPoint.smlPct else null
    val nplDelta = if (currentPoint != null && prevPoint != null) currentPoint.nplPct - prevPoint.nplPct else null

    var lineAnimKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedPeriodMonths, scopeLabel, validPoints.size) {
        lineAnimKey++
    }
    val lineProgress by animateFloatAsState(
        targetValue = if (lineAnimKey >= 0) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "portfolio_health_line_progress"
    )

    val rawMaxPct = remember(validPoints, activeFocus) {
        validPoints.maxOfOrNull { pt ->
            when (activeFocus) {
                KolekTrendFocus.SML -> pt.smlPct
                KolekTrendFocus.NPL -> pt.nplPct
                KolekTrendFocus.ALL -> maxOf(pt.smlPct, pt.nplPct)
            }
        } ?: 0.0
    }
    val yMaxPct = remember(rawMaxPct) {
        val scaled = (rawMaxPct * 1.22).coerceAtLeast(1.0)
        when {
            scaled <= 2.0 -> 2.0
            scaled <= 5.0 -> kotlin.math.ceil(scaled)
            else -> kotlin.math.ceil(scaled / 2.0) * 2.0
        }
    }

    val periodOptions = listOf(3, 6, 9, 12)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("beranda_health_line_chart_card"),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card Header: Caption + 3B/6B/9B/12B selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentPoint != null) {
                        "Rasio vs OS • ${currentPoint.fullLabel}"
                    } else {
                        "Rasio SML & NPL ($selectedPeriodMonths Bln)"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        periodOptions.forEach { months ->
                            val isSelected = selectedPeriodMonths == months
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (isSelected) Color(0xFF005FAF) else Color.Transparent,
                                modifier = Modifier
                                    .heightIn(min = 28.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { onSelectPeriodMonths(months) }
                                    .testTag("health_chart_period_${months}m")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${months}B",
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Interactive SML & NPL Metric Headers (tap to highlight/isolate a line)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HealthMetricSummaryHeader(
                    label = "SML",
                    pctText = "${String.format(Locale.US, "%.2f", currentPoint?.smlPct ?: 0.0)}%",
                    osText = formatFullIdr(currentPoint?.smlOs ?: 0.0, formatter),
                    debCount = currentPoint?.smlDeb ?: 0,
                    deltaPct = smlDelta,
                    accentColor = smlColor,
                    isFocused = activeFocus == KolekTrendFocus.ALL || activeFocus == KolekTrendFocus.SML,
                    isExplicitlySelected = activeFocus == KolekTrendFocus.SML,
                    formatter = formatter,
                    onClick = { onSelectFocus(KolekTrendFocus.SML) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("health_metric_sml")
                )

                HealthMetricSummaryHeader(
                    label = "NPL",
                    pctText = "${String.format(Locale.US, "%.2f", currentPoint?.nplPct ?: 0.0)}%",
                    osText = formatFullIdr(currentPoint?.nplOs ?: 0.0, formatter),
                    debCount = currentPoint?.nplDeb ?: 0,
                    deltaPct = nplDelta,
                    accentColor = nplColor,
                    isFocused = activeFocus == KolekTrendFocus.ALL || activeFocus == KolekTrendFocus.NPL,
                    isExplicitlySelected = activeFocus == KolekTrendFocus.NPL,
                    formatter = formatter,
                    onClick = { onSelectFocus(KolekTrendFocus.NPL) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("health_metric_npl")
                )
            }

            // Selected Month Inline Detail Banner (when user taps a specific month)
            val tappedPoint = points.getOrNull(selectedMonthIndex)
            AnimatedVisibility(
                visible = tappedPoint != null,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                )
            ) {
                tappedPoint?.let { pt ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (pt.hasData) {
                                "${pt.fullLabel}: SML ${String.format(Locale.US, "%.2f", pt.smlPct)}% (${formatFullIdr(pt.smlOs, formatter)}) • NPL ${String.format(Locale.US, "%.2f", pt.nplPct)}% (${formatFullIdr(pt.nplOs, formatter)})"
                            } else {
                                "${pt.fullLabel}: Belum ada data posisi OS tercatat"
                            },
                            fontSize = 9.5.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (validPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada data historis SML & NPL untuk periode ini.",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                // Dual Line Chart Canvas with Y-Axis % Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .width(34.dp)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", yMaxPct)}%",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", yMaxPct * 0.5)}%",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "0%",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("beranda_health_line_chart_canvas")
                            .pointerInput(points) {
                                detectTapGestures { offset ->
                                    if (points.isNotEmpty() && size.width > 0) {
                                        val padPx = 8.dp.toPx()
                                        val plotW = (size.width.toFloat() - padPx * 2f).coerceAtLeast(1f)
                                        val idx = if (points.size == 1) {
                                            0
                                        } else {
                                            val step = plotW / (points.size - 1).toFloat()
                                            kotlin.math.round((offset.x - padPx) / step).toInt()
                                                .coerceIn(0, points.lastIndex)
                                        }
                                        onSelectMonthIndex(idx)
                                    }
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val topPad = 10.dp.toPx()
                            val bottomPad = 10.dp.toPx()
                            val leftPad = 8.dp.toPx()
                            val rightPad = 8.dp.toPx()
                            val chartH = (h - topPad - bottomPad).coerceAtLeast(12f)
                            val plotW = (w - leftPad - rightPad).coerceAtLeast(12f)
                            val bottomY = topPad + chartH

                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            for (g in 0..2) {
                                val gy = topPad + (chartH / 2f) * g
                                drawLine(
                                    color = Color(0xFFE2E8F0),
                                    start = Offset(leftPad, gy),
                                    end = Offset(w - rightPad, gy),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = if (g < 2) dashEffect else null
                                )
                            }

                            fun xAt(index: Int): Float {
                                return if (points.size <= 1) {
                                    leftPad + plotW * 0.5f
                                } else {
                                    leftPad + (index.toFloat() / (points.size - 1).toFloat()) * plotW
                                }
                            }

                            fun yAt(pct: Double): Float {
                                val ratio = (pct / yMaxPct).toFloat().coerceIn(0f, 1f)
                                return bottomY - (ratio * chartH * lineProgress)
                            }

                            if (selectedMonthIndex in points.indices) {
                                val selX = xAt(selectedMonthIndex)
                                drawLine(
                                    color = Color(0xFF94A3B8).copy(alpha = 0.5f),
                                    start = Offset(selX, topPad),
                                    end = Offset(selX, bottomY),
                                    strokeWidth = 1.2.dp.toPx(),
                                    pathEffect = dashEffect
                                )
                            }

                            fun drawSeries(
                                color: Color,
                                isHighlighted: Boolean,
                                selector: (MonthlyKolekTrendPoint) -> Double
                            ) {
                                val seriesAlpha = if (isHighlighted) 1f else 0.22f
                                val strokePx = if (isHighlighted) 2.6.dp.toPx() else 1.8.dp.toPx()

                                val coords = points.mapIndexedNotNull { idx, pt ->
                                    if (!pt.hasData) null else idx to Offset(xAt(idx), yAt(selector(pt)))
                                }
                                if (coords.isEmpty()) return

                                if (coords.size >= 2) {
                                    val linePath = Path()
                                    val fillPath = Path()
                                    linePath.moveTo(coords[0].second.x, coords[0].second.y)
                                    fillPath.moveTo(coords[0].second.x, bottomY)
                                    fillPath.lineTo(coords[0].second.x, coords[0].second.y)

                                    for (i in 0 until coords.lastIndex) {
                                        val p1 = coords[i].second
                                        val p2 = coords[i + 1].second
                                        val midX = (p1.x + p2.x) * 0.5f
                                        linePath.cubicTo(midX, p1.y, midX, p2.y, p2.x, p2.y)
                                        fillPath.cubicTo(midX, p1.y, midX, p2.y, p2.x, p2.y)
                                    }

                                    fillPath.lineTo(coords.last().second.x, bottomY)
                                    fillPath.close()

                                    if (isHighlighted) {
                                        drawPath(
                                            path = fillPath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    color.copy(alpha = 0.16f),
                                                    color.copy(alpha = 0.01f)
                                                ),
                                                startY = topPad,
                                                endY = bottomY
                                            )
                                        )
                                    }

                                    drawPath(
                                        path = linePath,
                                        color = color.copy(alpha = seriesAlpha),
                                        style = Stroke(
                                            width = strokePx,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }

                                val defaultDotR = if (points.size <= 6) 3.8.dp.toPx() else 3.dp.toPx()
                                coords.forEach { (idx, ptOffset) ->
                                    val isNodeActive = idx == activeIdx
                                    val outerR = if (isNodeActive && isHighlighted) defaultDotR + 2.dp.toPx() else defaultDotR
                                    if (isNodeActive && isHighlighted) {
                                        drawCircle(
                                            color = color.copy(alpha = 0.20f),
                                            radius = outerR + 4.dp.toPx(),
                                            center = ptOffset
                                        )
                                    }
                                    drawCircle(
                                        color = color.copy(alpha = seriesAlpha),
                                        radius = outerR,
                                        center = ptOffset
                                    )
                                    drawCircle(
                                        color = Color.White.copy(alpha = seriesAlpha),
                                        radius = (outerR * 0.45f).coerceAtLeast(1.4.dp.toPx()),
                                        center = ptOffset
                                    )
                                }
                            }

                            // Draw SML first, then NPL in front
                            drawSeries(
                                color = smlColor,
                                isHighlighted = activeFocus == KolekTrendFocus.ALL || activeFocus == KolekTrendFocus.SML
                            ) { it.smlPct }

                            drawSeries(
                                color = nplColor,
                                isHighlighted = activeFocus == KolekTrendFocus.ALL || activeFocus == KolekTrendFocus.NPL
                            ) { it.nplPct }
                        }
                    }
                }

                // Month X-Axis Labels aligned with canvas plot area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEachIndexed { idx, pt ->
                        val isActive = idx == activeIdx
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectMonthIndex(idx) }
                                .testTag("beranda_health_month_label_$idx")
                        ) {
                            Text(
                                text = pt.shortLabel,
                                fontSize = if (points.size <= 6) 10.sp else 8.5.sp,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isActive) Color(0xFF0F172A) else if (pt.hasData) Color(0xFF64748B) else Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthMetricSummaryHeader(
    label: String,
    pctText: String,
    osText: String,
    debCount: Int,
    deltaPct: Double?,
    accentColor: Color,
    isFocused: Boolean,
    isExplicitlySelected: Boolean,
    formatter: NumberFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isExplicitlySelected) accentColor.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
        border = BorderStroke(
            width = if (isExplicitlySelected) 1.5.dp else 1.dp,
            color = if (isExplicitlySelected) accentColor.copy(alpha = 0.65f) else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = accentColor.copy(alpha = if (isFocused) 1f else 0.35f),
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFocused) Color(0xFF0F172A) else Color(0xFF94A3B8)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Debitur",
                        tint = accentColor.copy(alpha = if (isFocused) 0.9f else 0.4f),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = formatter.format(debCount),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pctText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor.copy(alpha = if (isFocused) 1f else 0.45f),
                    maxLines = 1
                )

                if (deltaPct != null && abs(deltaPct) >= 0.05) {
                    val isDown = deltaPct < 0.0
                    val deltaColor = if (isDown) Color(0xFF059669) else Color(0xFFE11D48)
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = deltaColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (isDown) {
                                    Icons.AutoMirrored.Filled.TrendingDown
                                } else {
                                    Icons.AutoMirrored.Filled.TrendingUp
                                },
                                contentDescription = if (isDown) "Turun" else "Naik",
                                tint = deltaColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "${if (deltaPct > 0) "+" else ""}${String.format(Locale.US, "%.1f", deltaPct)}%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = deltaColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Text(
                text = osText,
                fontSize = adaptiveOsFontSize(osText, maxSp = 10.5f),
                letterSpacing = (-0.25).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BerandaDebtorCountBadge(
    countText: String,
    accentColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 10.dp,
    fontSize: TextUnit = 10.sp,
    horizontalPadding: Dp = 6.dp,
    verticalPadding: Dp = 2.dp
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accentColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = countText,
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BerandaAlertMiniCard(
    title: String,
    osText: String,
    debText: String,
    hasDebtors: Boolean,
    accentColor: Color,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    fontSize = 9.5.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                )

                BerandaDebtorCountBadge(
                    countText = debText,
                    accentColor = if (hasDebtors) accentColor else Color(0xFF94A3B8),
                    contentDescription = "Debitur $title",
                    iconSize = 8.dp,
                    fontSize = 8.5.sp,
                    horizontalPadding = 4.5.dp,
                    verticalPadding = 1.5.dp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = osText,
                fontSize = adaptiveOsFontSize(osText, maxSp = 12f),
                letterSpacing = (-0.3).sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun normalizeBerandaCif(rawCif: String): String {
    var str = rawCif.trim().uppercase()
    if (str.startsWith("CIF")) str = str.substring(3).trim()
    if (str.startsWith("-")) str = str.substring(1).trim()
    return str.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
}

private fun resolveBerandaDailyCustomer(
    cust: CustomerEntity,
    dailyMap: Map<String, CustomerEntity>
): CustomerEntity? {
    if (dailyMap.isEmpty()) return null
    val rawRek = cust.noRekening.trim()
    val cleanRek = rawRek.filter { it.isDigit() }
    val noZeroRek = cleanRek.trimStart('0')
    val formatted15 = com.example.data.network.SpreadsheetService.formatRekening15(rawRek)
    return dailyMap[rawRek]
        ?: (if (cleanRek.isNotBlank()) dailyMap[cleanRek] else null)
        ?: (if (noZeroRek.isNotBlank()) dailyMap[noZeroRek] else null)
        ?: (if (formatted15.isNotBlank()) dailyMap[formatted15] else null)
}

private fun isNonLancarBerandaKolek(kolek: String): Boolean {
    val k = kolek.trim().uppercase()
    return k.startsWith("SML") || k.contains("DPK") || k == "2" ||
        k == "KL" || k.contains("KURANG") || k == "D" || k.contains("DIRAGUKAN") ||
        k == "M" || k.contains("MACET") || k == "3" || k == "4" || k == "5" || k == "NPL"
}

private fun formatFullIdr(value: Double, formatter: NumberFormat): String {
    if (value <= 0.0) return "Rp 0"
    return "Rp ${formatDoubleClean(value, formatter)}"
}

private fun formatSignedFullIdr(value: Double, formatter: NumberFormat): String {
    if (abs(value) < 0.5) return "Rp 0"
    val formatted = formatDoubleClean(abs(value), formatter)
    return if (value < 0.0) "-Rp $formatted" else "Rp $formatted"
}

private fun adaptiveOsFontSize(text: String, maxSp: Float = 10f): TextUnit {
    return when {
        text.length >= 18 -> (maxSp - 1.7f).coerceAtLeast(8.2f).sp
        text.length >= 15 -> (maxSp - 1.1f).coerceAtLeast(8.8f).sp
        text.length >= 12 -> (maxSp - 0.5f).coerceAtLeast(9.2f).sp
        else -> maxSp.sp
    }
}

private fun formatShortDisplayDate(raw: String): String {
    val clean = raw.trim()
    if (clean.isBlank()) return ""
    val parts = clean.split("/", "-", ".").filter { it.isNotBlank() }
    if (parts.size >= 3) {
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")
        val day = if (parts[0].length == 4) parts[2].toIntOrNull() else parts[0].toIntOrNull()
        val month = parts[1].toIntOrNull()
        if (day != null && month != null && month in 1..12) {
            return "$day ${monthNames[month - 1]}"
        }
    }
    return clean
}

private fun calculateKolekSummaryFromList(customers: List<com.example.data.model.CustomerEntity>): KolekSummaryData {
    var lOs = 0.0; var lDeb = 0
    var sOs = 0.0; var sDeb = 0
    var nOs = 0.0; var nDeb = 0
    var oOs = 0.0; var oDeb = 0

    for (cust in customers) {
        val k = cust.kolek.trim().uppercase()
        when {
            k == "KL" || k.contains("KURANG") || k == "D" || k.contains("DIRAGUKAN") ||
                k == "M" || k.contains("MACET") || k == "3" || k == "4" || k == "5" || k == "NPL" -> {
                nOs += cust.osPinjaman
                nDeb++
            }
            k.startsWith("SML") || k.contains("DPK") || k == "2" -> {
                sOs += cust.osPinjaman
                sDeb++
            }
            k == "L" || k == "LANCAR" || k == "1" || (k.contains("LANCAR") && !k.contains("KURANG")) -> {
                lOs += cust.osPinjaman
                lDeb++
            }
            else -> {
                oOs += cust.osPinjaman
                oDeb++
            }
        }
    }
    return KolekSummaryData(
        lancar = UnitSummaryData(lOs, lDeb),
        sml = UnitSummaryData(sOs, sDeb),
        npl = UnitSummaryData(nOs, nDeb),
        lainnya = UnitSummaryData(oOs, oDeb)
    )
}
