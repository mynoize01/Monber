package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CustomerEntity
import com.example.data.util.DateTenorUtils
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.KolekSummaryData
import com.example.ui.viewmodel.MantriDetailData
import com.example.ui.viewmodel.RealisasiCustomerItem
import com.example.ui.viewmodel.RealisasiNetMetrics
import com.example.ui.viewmodel.UnitSummaryData
import com.example.update.AppUpdateState
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private data class MonthlyRealisasiPoint(
    val monthNumber: Int,
    val yearNumber: Int,
    val shortLabel: String,
    val fullLabel: String,
    val realisasi: Double,
    val netDisburs: Double,
    val debtors: Int,
    val hasData: Boolean,
    val customers: List<RealisasiCustomerItem> = emptyList()
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
        viewModel.loadAllMonthlyChartData()
        if (summaryDaily.debtors == 0) {
            viewModel.loadSummaryForPeriod("DAILY")
        }
        if (summaryAkhirBulan.debtors == 0) {
            viewModel.loadSummaryForPeriod("AKHIR_BULAN")
        }
    }

    LaunchedEffect(dailyCustomers.size, akhirBulanCustomers.size, monitoringCustomers.size) {
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
    var showRealisasiDetailSheet by remember { mutableStateOf(false) }
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

    // Dynamic fallback from live monitoringCustomers (INTRAKOMP)
    val fallbackUnitSummary = remember(monitoringCustomers) {
        UnitSummaryData(
            os = monitoringCustomers.sumOf { it.osPinjaman },
            debtors = monitoringCustomers.size
        )
    }
    val fallbackUnitKolek = remember(monitoringCustomers) {
        calculateKolekSummaryFromList(monitoringCustomers)
    }

    val kelolaanCustomers = remember(monitoringCustomers, loggedInPN, cleanPnDigits) {
        if (loggedInPN.isBlank()) {
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
    }
    val kelolaanSummaryFromDaily = remember(mantriSummaryDaily, kelolaanCustomers, displayName, cleanPnDigits) {
        val dailyMantri = findMantriDetail(mantriSummaryDaily)
        when {
            dailyMantri != null && dailyMantri.total.debtors > 0 -> dailyMantri.total
            kelolaanCustomers.isNotEmpty() -> UnitSummaryData(
                os = kelolaanCustomers.sumOf { it.osPinjaman },
                debtors = kelolaanCustomers.size
            )
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
        kelolaanCustomers,
        displayName,
        cleanPnDigits
    ) {
        if (kelolaanSummaryFromDaily.debtors > 0) {
            kelolaanSummaryFromDaily
        } else {
            findMantriDetail(mantriSummaryAkhirBulan)?.total
                ?: findMantriDetail(mantriSummaryBulanLalu)?.total
                ?: UnitSummaryData(
                    os = kelolaanCustomers.sumOf { it.osPinjaman },
                    debtors = kelolaanCustomers.size
                )
        }
    }
    val kelolaanActiveKolek = remember(
        mantriSummaryDaily,
        mantriSummaryAkhirBulan,
        mantriSummaryBulanLalu,
        kelolaanCustomers,
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
            calculateKolekSummaryFromList(kelolaanCustomers)
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
                hasData = metrics.debtors > 0 || metrics.realisasi > 0.0,
                customers = metrics.customers
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

    // Notifications summary (real app state only)
    val hasUpdateAvailable = appUpdateState is AppUpdateState.UpdateAvailable ||
        appUpdateState is AppUpdateState.ReadyToInstall ||
        appUpdateState is AppUpdateState.NeedsInstallPermission

    val notificationItems = remember(hasUpdateAvailable, dueTodayCount, kunjunganKeys, isShowingCachedOfflineData, appUpdateState) {
        val list = mutableListOf<Triple<String, String, () -> Unit>>()
        if (hasUpdateAvailable) {
            val ver = when (val st = appUpdateState) {
                is AppUpdateState.UpdateAvailable -> st.release.tagName
                is AppUpdateState.ReadyToInstall -> st.release.tagName
                is AppUpdateState.NeedsInstallPermission -> st.release.tagName
                else -> "baru"
            }
            list.add(Triple("Pembaruan Aplikasi", "Versi $ver siap diinstal. Ketuk untuk update.", onOpenUpdateDialog))
        }
        if (dueTodayCount > 0) {
            list.add(Triple("Jatuh Tempo Hari Ini", "$dueTodayCount nasabah jatuh tempo angsuran hari ini.", onOpenKelolaan))
        }
        if (kunjunganKeys.isNotEmpty()) {
            list.add(Triple("Rencana Kunjungan", "${kunjunganKeys.size} nasabah dijadwalkan untuk kunjungan ($activeVisitDateStr).", onOpenKunjungan))
        }
        if (isShowingCachedOfflineData) {
            list.add(Triple("Mode Offline Aktif", "Menampilkan data cache lokal Room. Ketuk untuk sinkron ulang.", { viewModel.refreshSpreadsheetData(force = true) }))
        }
        list
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

    val kelolaanSegmenSource = remember(
        dailyCustomers,
        akhirBulanCustomers,
        kelolaanCustomers,
        monitoringCustomers,
        loggedInPN,
        cleanPnDigits,
        displayName,
        effectiveScope
    ) {
        fun matchMantri(list: List<CustomerEntity>): List<CustomerEntity> {
            if (loggedInPN.isBlank()) return emptyList()
            return list.filter { cust ->
                val custPnDigits = cust.pnMantri.filter { it.isDigit() }.trimStart('0')
                (cleanPnDigits.isNotBlank() && custPnDigits == cleanPnDigits) ||
                    cust.pnMantri.trim().equals(loggedInPN.trim(), ignoreCase = true) ||
                    cust.namaMantri.contains(loggedInPN.trim(), ignoreCase = true) ||
                    cust.namaMantri.contains(displayName, ignoreCase = true)
            }
        }
        if (effectiveScope == BerandaScope.KELOLAAN && loggedInPN.isNotBlank()) {
            matchMantri(dailyCustomers)
                .ifEmpty { matchMantri(akhirBulanCustomers) }
                .ifEmpty { kelolaanCustomers }
        } else {
            dailyCustomers
                .ifEmpty { akhirBulanCustomers }
                .ifEmpty { monitoringCustomers }
        }
    }

    val segmenKelolaanGroups = remember(kelolaanSegmenSource) {
        kelolaanSegmenSource
            .groupBy { it.segmen.trim().uppercase().ifBlank { "LAINNYA" } }
            .map { (name, group) ->
                Triple(name, group.sumOf { it.osPinjaman }, group.size)
            }
            .sortedByDescending { it.second }
    }

    val savingsMaxByNormCif = remember(di319Rows) {
        val map = mutableMapOf<String, Double>()
        for (row in di319Rows) {
            val norm = normalizeBerandaCif(row.cif)
            if (norm.isNotEmpty()) {
                val prev = map[norm] ?: 0.0
                if (row.saldo > prev) map[norm] = row.saldo
            }
        }
        map
    }

    val (tusimSummary, tucilSummary, tunggakanUnder6Summary) = remember(
        kelolaanSegmenSource,
        dailyDataMap,
        savingsMaxByNormCif
    ) {
        val nowDate = LocalDate.now()
        var tusimOs = 0.0
        var tusimCount = 0
        var tucilOs = 0.0
        var tucilCount = 0
        var tg6Os = 0.0
        var tg6Count = 0

        for (cust in kelolaanSegmenSource) {
            val dailyCust = resolveBerandaDailyCustomer(cust, dailyDataMap)
            val currentTunggakan = dailyCust?.totalTunggakan ?: cust.totalTunggakan
            val currentUmurTgk = dailyCust?.umurTgk ?: cust.umurTgk
            val currentKolek = dailyCust?.kolek?.ifBlank { cust.kolek } ?: cust.kolek
            val currentOs = dailyCust?.osPinjaman ?: cust.osPinjaman

            if (currentTunggakan > 0.0) {
                val normCif = normalizeBerandaCif(cust.cif)
                if (normCif.isNotBlank() && (savingsMaxByNormCif[normCif] ?: 0.0) > 25_000.0) {
                    tusimOs += currentOs
                    tusimCount++
                }
                if (currentTunggakan < 100_000.0) {
                    tucilOs += currentOs
                    tucilCount++
                }
            }

            val hasArrears = currentTunggakan > 0.0 ||
                currentUmurTgk > 0 ||
                isNonLancarBerandaKolek(currentKolek)
            if (hasArrears) {
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
                val elapsed = DateTenorUtils.monthsElapsedSince(rawTglReal, nowDate)
                if (elapsed != null && elapsed < 6) {
                    tg6Os += currentOs
                    tg6Count++
                }
            }
        }

        Triple(
            UnitSummaryData(os = tusimOs, debtors = tusimCount),
            UnitSummaryData(os = tucilOs, debtors = tucilCount),
            UnitSummaryData(os = tg6Os, debtors = tg6Count)
        )
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Full-bleed custom wallpaper background (includes upper-right 3D people silhouettes & blue wave layers)
        Image(
            painter = painterResource(id = R.drawable.bg_beranda),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.matchParentSize()
        )

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
                            if (notificationItems.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp, end = 8.dp)
                                        .size(17.dp)
                                        .background(Color(0xFFEF4444), CircleShape)
                                        .border(1.5.dp, Color(0xFF005FAF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = notificationItems.size.coerceAtMost(9).toString(),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rencana Kunjungan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C2D12)
                        )
                        Text(
                            text = if (loggedInPN.isBlank()) {
                                "Login PN untuk mengelola rencana kunjungan"
                            } else {
                                "${kunjunganKeys.size} Nasabah • Tgl $activeVisitDateStr"
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Debitur",
                                        tint = Color(0xFF005FAF),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = formatter.format(kelolaanActiveSummary.debtors),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF005FAF),
                                        maxLines = 1
                                    )
                                }
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

            // 3 Horizontal Kolek Donut Info Buttons: Lancar, SML, NPL (% inside donut circle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BerandaKolekDonutButton(
                    title = "Lancar",
                    osText = formatFullIdr(lancarOs, formatter),
                    debText = formatter.format(lancarDeb),
                    percentage = lancarOsPct,
                    accentColor = Color(0xFF16A34A),
                    trackColor = Color(0xFFDCFCE7),
                    containerColor = Color(0xFFF0FDF4),
                    borderColor = Color(0xFFDCFCE7),
                    onClick = {
                        if (effectiveScope == BerandaScope.KELOLAAN) onOpenKelolaan() else onOpenMonitoring()
                        viewModel.resetActiveFiltersForQuickJump()
                        viewModel.filterKolek.value = "Lancar"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("beranda_kpi_lancar")
                )
                BerandaKolekDonutButton(
                    title = "SML",
                    osText = formatFullIdr(smlOs, formatter),
                    debText = formatter.format(smlDeb),
                    percentage = smlOsPct,
                    accentColor = Color(0xFFD97706),
                    trackColor = Color(0xFFFEF3C7),
                    containerColor = Color(0xFFFFFBEB),
                    borderColor = Color(0xFFFDE68A),
                    onClick = {
                        if (effectiveScope == BerandaScope.KELOLAAN) onOpenKelolaan() else onOpenMonitoring()
                        viewModel.resetActiveFiltersForQuickJump()
                        viewModel.filterKolek.value = "SML"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("beranda_kpi_sml")
                )
                BerandaKolekDonutButton(
                    title = "NPL",
                    osText = formatFullIdr(nplOs, formatter),
                    debText = formatter.format(nplDeb),
                    percentage = nplOsPct,
                    accentColor = Color(0xFFDC2626),
                    trackColor = Color(0xFFFEE2E2),
                    containerColor = Color(0xFFFEF2F2),
                    borderColor = Color(0xFFFEE2E2),
                    onClick = {
                        if (effectiveScope == BerandaScope.KELOLAAN) onOpenKelolaan() else onOpenMonitoring()
                        viewModel.resetActiveFiltersForQuickJump()
                        viewModel.filterKolek.value = "NPL"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("beranda_kpi_npl")
                )
            }

            // Segmen Kelolaan Section (3 columns per row showing OS and Debitur)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("beranda_segmen_kelolaan_section"),
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
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = Color(0xFF005FAF),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Segmen Kelolaan",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Text(
                        text = if (effectiveScope == BerandaScope.KELOLAAN) {
                            "${segmenKelolaanGroups.size} Segmen"
                        } else {
                            "Unit • ${segmenKelolaanGroups.size} Segmen"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }

                if (segmenKelolaanGroups.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = if (loggedInPN.isBlank()) {
                                "Login PN Mantri untuk melihat rincian segmen kelolaan"
                            } else {
                                "Data segmen kelolaan belum tersedia"
                            },
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp)
                        )
                    }
                } else {
                    val chunkedSegmen = segmenKelolaanGroups.chunked(3)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chunkedSegmen.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { (segName, segOs, segDeb) ->
                                    BerandaSegmenMiniCard(
                                        title = segName,
                                        osText = formatFullIdr(segOs, formatter),
                                        debText = formatter.format(segDeb),
                                        onClick = {
                                            if (effectiveScope == BerandaScope.KELOLAAN) onOpenKelolaan() else onOpenMonitoring()
                                            viewModel.resetActiveFiltersForQuickJump()
                                            viewModel.filterSegmen.value = segName
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("beranda_segmen_${segName.lowercase().replace(" ", "_")}")
                                    )
                                }
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Pantauan Khusus Section (below Segmen Kelolaan): Tusim, Tucil <100 rb, Tunggakan <6 bln
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BerandaAlertMiniCard(
                        title = "Tusim",
                        osText = formatFullIdr(tusimSummary.os, formatter),
                        debText = formatter.format(tusimSummary.debtors),
                        accentColor = Color(0xFF0F766E),
                        containerColor = Color(0xFFF0FDFA),
                        borderColor = Color(0xFF99F6E4),
                        onClick = {
                            if (effectiveScope == BerandaScope.KELOLAAN) onOpenKelolaan() else onOpenMonitoring()
                            viewModel.resetActiveFiltersForQuickJump()
                            viewModel.filterTusim.value = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("beranda_card_tusim")
                    )
                    BerandaAlertMiniCard(
                        title = "Tucil <100 rb",
                        osText = formatFullIdr(tucilSummary.os, formatter),
                        debText = formatter.format(tucilSummary.debtors),
                        accentColor = Color(0xFFC2410C),
                        containerColor = Color(0xFFFFF7ED),
                        borderColor = Color(0xFFFED7AA),
                        onClick = {
                            if (effectiveScope == BerandaScope.KELOLAAN) onOpenKelolaan() else onOpenMonitoring()
                            viewModel.resetActiveFiltersForQuickJump()
                            viewModel.filterTucil.value = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("beranda_card_tucil")
                    )
                    BerandaAlertMiniCard(
                        title = "Tunggakan <6 bln",
                        osText = formatFullIdr(tunggakanUnder6Summary.os, formatter),
                        debText = formatter.format(tunggakanUnder6Summary.debtors),
                        accentColor = Color(0xFFBE123C),
                        containerColor = Color(0xFFFFF1F2),
                        borderColor = Color(0xFFFECDD3),
                        onClick = {
                            if (effectiveScope == BerandaScope.KELOLAAN) onOpenKelolaan() else onOpenMonitoring()
                            viewModel.resetActiveFiltersForQuickJump()
                            viewModel.filterTunggakanKurang6Bln.value = true
                        },
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
                                    showRealisasiDetailSheet = false
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
                    showRealisasiDetailSheet = false
                },
                selectedMonthIndex = selectedChartMonthIndex,
                onSelectMonthIndex = { idx ->
                    selectedChartMonthIndex = idx
                    showRealisasiDetailSheet = true
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
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = Color(0xFF005FAF),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Notifikasi & Pengingat",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            },
            text = {
                if (notificationItems.isEmpty()) {
                    Text(
                        text = "Semua kondisi portofolio & aplikasi terpantau aman. Tidak ada notifikasi baru saat ini.",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        notificationItems.forEach { (title, desc, action) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showNotificationsDialog = false
                                        action()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (title.contains("Pembaruan")) Icons.Default.SystemUpdate else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFF005FAF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNotificationsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Tutup")
                }
            }
        )
    }

    val activeRealisasiPoint = realisasiPoints.getOrNull(selectedChartMonthIndex)
    if (showRealisasiDetailSheet && activeRealisasiPoint != null) {
        RealisasiDetailBottomSheet(
            point = activeRealisasiPoint,
            scopeLabel = effectiveScope.label,
            formatter = formatter,
            onDismiss = { showRealisasiDetailSheet = false }
        )
    }
}

@Composable
private fun BerandaKolekDonutButton(
    title: String,
    osText: String,
    debText: String,
    percentage: Double,
    accentColor: Color,
    trackColor: Color,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedSweep by animateFloatAsState(
        targetValue = ((percentage / 100.0) * 360.0).toFloat().coerceIn(0f, 360f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "kolek_donut_sweep_$title"
    )
    val pctText = if (percentage >= 99.95) {
        "100%"
    } else {
        "${String.format(Locale.US, "%.1f", percentage)}%"
    }

    Surface(
        modifier = modifier
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Debitur",
                        tint = accentColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = debText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 4.dp.toPx()
                        val arcSize = Size(size.width - strokeW, size.height - strokeW)
                        val topLeft = Offset(strokeW * 0.5f, strokeW * 0.5f)
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                        if (animatedSweep > 0f) {
                            drawArc(
                                color = accentColor,
                                startAngle = -90f,
                                sweepAngle = animatedSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Text(
                        text = pctText,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = osText,
                fontSize = adaptiveOsFontSize(osText, maxSp = 10f),
                letterSpacing = (-0.3).sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BerandaSegmenMiniCard(
    title: String,
    osText: String,
    debText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FBFF),
        border = BorderStroke(1.dp, Color(0xFFDBEAFE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF005FAF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 2.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Debitur",
                        tint = Color(0xFF005FAF),
                        modifier = Modifier.size(10.5.dp)
                    )
                    Text(
                        text = debText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        maxLines = 1
                    )
                }
            }
            Text(
                text = osText,
                fontSize = adaptiveOsFontSize(osText, maxSp = 10f),
                letterSpacing = (-0.3).sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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

                // Borderless Top Metrics: Realisasi (belakang) & Netdisburse (depan)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chart_metric_realisasi"),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
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
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 1
                            )
                        }
                        val realText = formatFullIdr(totalRealisasi, formatter)
                        Text(
                            text = realText,
                            fontSize = adaptiveOsFontSize(realText, maxSp = 12.5f),
                            letterSpacing = (-0.25).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Debitur",
                                tint = Color(0xFFBAE6FD),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "${formatter.format(totalDeb)} Debitur",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFBAE6FD),
                                maxLines = 1
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chart_metric_netdisburs"),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
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
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 1
                            )
                        }
                        val netText = formatSignedFullIdr(totalNetDisburs, formatter)
                        Text(
                            text = netText,
                            fontSize = adaptiveOsFontSize(netText, maxSp = 12.5f),
                            letterSpacing = (-0.25).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFA7F3D0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", netRatioPct)}% dari Realisasi",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6EE7B7),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectMonthIndex(selectedMonthIndex) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (pt.hasData) {
                                        "${pt.fullLabel} (${pt.debtors} Deb): Realisasi ${formatFullIdr(pt.realisasi, formatter)} • Netdisburs ${formatSignedFullIdr(pt.netDisburs, formatter)}"
                                    } else {
                                        "${pt.fullLabel}: Belum ada realisasi tercatat"
                                    },
                                    fontSize = 9.5.sp,
                                    lineHeight = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Lihat rincian nasabah",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
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
private fun RealisasiDetailBottomSheet(
    point: MonthlyRealisasiPoint,
    scopeLabel: String,
    formatter: NumberFormat,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember(point.yearNumber, point.monthNumber, scopeLabel) {
        mutableStateOf("")
    }

    val filteredCustomers by remember(point.customers, searchQuery) {
        derivedStateOf {
            val q = searchQuery.trim()
            if (q.isEmpty()) {
                point.customers
            } else {
                point.customers.filter { item ->
                    item.namaNasabah.contains(q, ignoreCase = true) ||
                        item.noRekening.contains(q, ignoreCase = true) ||
                        item.namaMantri.contains(q, ignoreCase = true) ||
                        item.segmen.contains(q, ignoreCase = true)
                }
            }
        }
    }

    val netPct = remember(point.realisasi, point.netDisburs) {
        if (point.realisasi > 0.0) (point.netDisburs / point.realisasi) * 100.0 else 0.0
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .testTag("beranda_realisasi_bottom_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rincian Realisasi ${point.fullLabel}",
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
                                text = "${formatter.format(point.debtors)} Debitur",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("beranda_realisasi_sheet_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup rincian realisasi",
                        tint = Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Stat Cards (Realisasi & Netdisburse)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Total Realisasi",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D4ED8)
                        )
                        val totalRealText = formatFullIdr(point.realisasi, formatter)
                        Text(
                            text = totalRealText,
                            fontSize = adaptiveOsFontSize(totalRealText, maxSp = 13f),
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
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Netdisburse (${String.format(Locale.US, "%.1f", netPct)}%)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF047857),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val totalNetText = formatSignedFullIdr(point.netDisburs, formatter)
                        Text(
                            text = totalNetText,
                            fontSize = adaptiveOsFontSize(totalNetText, maxSp = 13f),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (point.netDisburs >= 0.0) Color(0xFF065F46) else Color(0xFFBE123C),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (point.customers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("beranda_realisasi_sheet_search"),
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
                            "Hasil Pencarian (${filteredCustomers.size} dari ${point.customers.size})"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "Nominal Realisasi",
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

            if (point.customers.isEmpty()) {
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
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = Color(0xFF005FAF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Belum Ada Data Realisasi",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Tidak ada nasabah realisasi yang tercatat pada bulan ${point.fullLabel} untuk cakupan $scopeLabel.",
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .testTag("beranda_realisasi_sheet_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(
                        items = filteredCustomers,
                        key = { idx, item -> "${item.noRekening}_${item.namaNasabah}_$idx" }
                    ) { idx, item ->
                        RealisasiCustomerRowCard(
                            index = idx + 1,
                            item = item,
                            showMantriName = scopeLabel.equals("Unit", ignoreCase = true),
                            formatter = formatter,
                            modifier = Modifier.testTag("beranda_realisasi_sheet_item_$idx")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RealisasiCustomerRowCard(
    index: Int,
    item: RealisasiCustomerItem,
    showMantriName: Boolean,
    formatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    val metaParts = buildList {
        if (item.tglReal.isNotBlank()) add(formatShortDisplayDate(item.tglReal))
        if (item.noRekening.isNotBlank()) add(item.noRekening)
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
                verticalArrangement = Arrangement.spacedBy(2.dp)
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
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val realText = formatFullIdr(item.realisasi, formatter)
                Text(
                    text = realText,
                    fontSize = adaptiveOsFontSize(realText, maxSp = 12.5f),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0051B0),
                    maxLines = 1
                )
                val netText = formatSignedFullIdr(item.netDisburs, formatter)
                Text(
                    text = "Net: $netText",
                    fontSize = adaptiveOsFontSize(netText, maxSp = 10f),
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.netDisburs >= 0.0) Color(0xFF059669) else Color(0xFFBE123C),
                    maxLines = 1
                )
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
private fun BerandaAlertMiniCard(
    title: String,
    osText: String,
    debText: String,
    accentColor: Color,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 66.dp)
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
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 2.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Debitur",
                        tint = accentColor,
                        modifier = Modifier.size(10.5.dp)
                    )
                    Text(
                        text = debText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        maxLines = 1
                    )
                }
            }
            Text(
                text = osText,
                fontSize = adaptiveOsFontSize(osText, maxSp = 10f),
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
