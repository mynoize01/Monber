package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.KolekSummaryData
import com.example.ui.viewmodel.MantriDetailData
import com.example.ui.viewmodel.UnitSummaryData
import com.example.update.AppUpdateState
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private data class MonthlyPortfolioPoint(
    val monthNumber: Int,
    val shortLabel: String,
    val fullLabel: String,
    val os: Double,
    val sml: Double,
    val npl: Double,
    val hasData: Boolean
)

private enum class PortfolioChartFilter(val label: String) {
    ALL("Semua"),
    OS("OS"),
    SML("SML"),
    NPL("NPL")
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

    val periodDateDaily by viewModel.periodDateDaily.collectAsState()
    val periodDateAkhirBulan by viewModel.periodDateAkhirBulan.collectAsState()
    val periodDateBulanLalu by viewModel.periodDateBulanLalu.collectAsState()

    val monitoringCustomers by viewModel.monitoringCustomers.collectAsState()
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
    var chartFilter by remember { mutableStateOf(PortfolioChartFilter.ALL) }
    var selectedChartMonthIndex by remember { mutableIntStateOf(-1) }
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

    // Build 12-month rolling timeline for OS, SML, and NPL
    val monthlyPoints = remember(
        effectiveScope,
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
        activeScopeSummary,
        activeScopeKolek
    ) {
        val shortMonthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")
        val fullMonthNames = listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )

        val dailyMonth = extractMonthAndYear(periodDateDaily).first.toIntOrNull()
        val akhirMonth = extractMonthAndYear(periodDateAkhirBulan).first.toIntOrNull()
        val blnLaluMonth = extractMonthAndYear(periodDateBulanLalu).first.toIntOrNull()

        val endMonth = when {
            dailyMonth != null && summaryDaily.debtors > 0 -> dailyMonth
            akhirMonth != null && summaryAkhirBulan.debtors > 0 -> akhirMonth
            blnLaluMonth != null && summaryBulanLalu.debtors > 0 -> blnLaluMonth
            dashboardMonthlySummary.isNotEmpty() -> dashboardMonthlySummary.keys.maxOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)
            else -> Calendar.getInstance().get(Calendar.MONTH) + 1
        }.coerceIn(1, 12)

        val rollingMonths = (0..11).map { offset ->
            ((endMonth - 11 + offset + 11) % 12) + 1
        }

        rollingMonths.map { m ->
            var osVal = 0.0
            var smlVal = 0.0
            var nplVal = 0.0
            var found = false

            if (effectiveScope == BerandaScope.KELOLAAN) {
                val liveDailyMantri = if (m == dailyMonth) findMantriDetail(mantriSummaryDaily) else null
                val mMap = dashboardMonthlyMantriSummary[m]
                val monthlyMantri = if (mMap != null) findMantriDetail(mMap) else null
                val akhirMantri = if (m == akhirMonth) findMantriDetail(mantriSummaryAkhirBulan) else null
                val blnLaluMantri = if (m == blnLaluMonth) findMantriDetail(mantriSummaryBulanLalu) else null

                val picked = when {
                    liveDailyMantri != null && liveDailyMantri.total.debtors > 0 -> liveDailyMantri
                    monthlyMantri != null && monthlyMantri.total.debtors > 0 -> monthlyMantri
                    akhirMantri != null && akhirMantri.total.debtors > 0 -> akhirMantri
                    blnLaluMantri != null && blnLaluMantri.total.debtors > 0 -> blnLaluMantri
                    m == endMonth && activeScopeSummary.debtors > 0 -> MantriDetailData(
                        name = displayName,
                        total = activeScopeSummary,
                        lancar = activeScopeKolek.lancar,
                        sml = activeScopeKolek.sml,
                        npl = activeScopeKolek.npl
                    )
                    else -> null
                }
                if (picked != null && (picked.total.os > 0.0 || picked.total.debtors > 0)) {
                    osVal = picked.total.os
                    smlVal = picked.sml.os
                    nplVal = picked.npl.os
                    found = true
                }
            } else {
                val monthlySum = dashboardMonthlySummary[m]
                val monthlyKolek = dashboardMonthlyKolekSummary[m]

                when {
                    m == dailyMonth && summaryDaily.debtors > 0 -> {
                        osVal = summaryDaily.os
                        smlVal = kolekSummaryDaily.sml.os
                        nplVal = kolekSummaryDaily.npl.os
                        found = true
                    }
                    monthlySum != null && monthlySum.debtors > 0 -> {
                        osVal = monthlySum.os
                        smlVal = monthlyKolek?.sml?.os ?: 0.0
                        nplVal = monthlyKolek?.npl?.os ?: 0.0
                        found = true
                    }
                    m == akhirMonth && summaryAkhirBulan.debtors > 0 -> {
                        osVal = summaryAkhirBulan.os
                        smlVal = kolekSummaryAkhirBulan.sml.os
                        nplVal = kolekSummaryAkhirBulan.npl.os
                        found = true
                    }
                    m == blnLaluMonth && summaryBulanLalu.debtors > 0 -> {
                        osVal = summaryBulanLalu.os
                        smlVal = kolekSummaryBulanLalu.sml.os
                        nplVal = kolekSummaryBulanLalu.npl.os
                        found = true
                    }
                    m == endMonth && activeScopeSummary.debtors > 0 -> {
                        osVal = activeScopeSummary.os
                        smlVal = activeScopeKolek.sml.os
                        nplVal = activeScopeKolek.npl.os
                        found = true
                    }
                }
            }

            MonthlyPortfolioPoint(
                monthNumber = m,
                shortLabel = shortMonthNames[m - 1],
                fullLabel = fullMonthNames[m - 1],
                os = osVal,
                sml = smlVal,
                npl = nplVal,
                hasData = found
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

    val totalDebtors = activeScopeSummary.debtors
    val lancarDebtors = activeScopeKolek.lancar.debtors.coerceAtMost(totalDebtors)
    val tunggakanDebtors = (totalDebtors - lancarDebtors).coerceAtLeast(0)
    val lancarPct = if (totalDebtors > 0) (lancarDebtors.toDouble() / totalDebtors) * 100.0 else 0.0
    val tunggakanPct = if (totalDebtors > 0) (tunggakanDebtors.toDouble() / totalDebtors) * 100.0 else 0.0

    val lancarOs = activeScopeKolek.lancar.os
    val tunggakanOs = activeScopeKolek.sml.os + activeScopeKolek.npl.os

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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 1. Transparent Hero Overlay aligned with the dark-blue upper region of bg_beranda
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarTopPadding + 8.dp, bottom = 26.dp)
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

        // Sync progress indicator if refreshing
        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = Color(0xFF005FAF),
                trackColor = Color(0xFFE0ECFA)
            )
        }

        // Offline / Update mini notice banner inside white sheet when applicable
        AnimatedVisibility(visible = isShowingCachedOfflineData) {
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

        // 2. Main White Dashboard Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 2.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Action Card: Kelolaan Nasabah (matching reference top pill card)
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
                        Text(
                            text = "Kelolaan Nasabah",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        val kelolaanSubtitle = if (loggedInPN.isBlank()) {
                            "Login PN Mantri untuk melihat kelolaan pribadi"
                        } else {
                            val dateSuffix = if (latestDateText.isNotBlank()) " • $latestDateText" else ""
                            "${formatDoubleClean(kelolaanActiveSummary.os, formatter)} • ${kelolaanActiveSummary.debtors} Deb$dateSuffix"
                        }
                        Text(
                            text = kelolaanSubtitle,
                            fontSize = 12.sp,
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

            // Moved Kunjungan Button inside Beranda (Quick Action Pill Card)
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

            // 3 Pastel KPI Cards Row: Total Nasabah, Tunggakan, Lancar (matching reference image)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BerandaKpiPastelCard(
                    title = "Total Nasabah",
                    value = formatter.format(totalDebtors),
                    icon = Icons.Default.Groups,
                    iconTint = Color(0xFF005FAF),
                    containerColor = Color(0xFFEEF5FF),
                    borderColor = Color(0xFFDBEAFE),
                    modifier = Modifier.weight(1f),
                    testTag = "beranda_kpi_total_nasabah"
                )
                BerandaKpiPastelCard(
                    title = "Tunggakan",
                    value = formatBerandaMillion(tunggakanOs),
                    icon = Icons.Default.Warning,
                    iconTint = Color(0xFFF59E0B),
                    containerColor = Color(0xFFFFF7EC),
                    borderColor = Color(0xFFFDE6C4),
                    modifier = Modifier.weight(1f),
                    testTag = "beranda_kpi_tunggakan"
                )
                BerandaKpiPastelCard(
                    title = "Lancar",
                    value = formatBerandaMillion(lancarOs),
                    icon = Icons.Default.CheckCircle,
                    iconTint = Color(0xFF16A34A),
                    containerColor = Color(0xFFECFDF3),
                    borderColor = Color(0xFFD1FAE5),
                    modifier = Modifier.weight(1f),
                    testTag = "beranda_kpi_lancar"
                )
            }

            // Ringkasan Portofolio Section Header
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
                        text = "Ringkasan Portofolio",
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

            // Blue Gradient 12-Month Portfolio Chart Card (OS, SML, NPL)
            Portfolio12MonthChartCard(
                points = monthlyPoints,
                selectedFilter = chartFilter,
                onSelectFilter = { newFilter ->
                    chartFilter = if (chartFilter == newFilter && newFilter != PortfolioChartFilter.ALL) {
                        PortfolioChartFilter.ALL
                    } else {
                        newFilter
                    }
                },
                selectedMonthIndex = selectedChartMonthIndex,
                onSelectMonthIndex = { idx ->
                    selectedChartMonthIndex = if (selectedChartMonthIndex == idx) -1 else idx
                },
                scopeLabel = effectiveScope.label,
                currentOs = activeScopeSummary.os,
                currentSml = activeScopeKolek.sml.os,
                currentNpl = activeScopeKolek.npl.os,
                formatter = formatter
            )

            // Bottom 2 Donut Progress Cards: Nasabah Lancar & Nasabah Tunggakan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BerandaDonutSummaryCard(
                    title = "Nasabah Lancar",
                    countText = formatter.format(lancarDebtors),
                    percentage = lancarPct,
                    accentColor = Color(0xFF16A34A),
                    trackColor = Color(0xFFDCFCE7),
                    containerColor = Color(0xFFF0FDF4),
                    borderColor = Color(0xFFDCFCE7),
                    onClick = {
                        viewModel.filterKolek.value = "Lancar"
                        if (effectiveScope == BerandaScope.KELOLAAN) onOpenKelolaan() else onOpenMonitoring()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("beranda_card_nasabah_lancar")
                )
                BerandaDonutSummaryCard(
                    title = "Nasabah Tunggakan",
                    countText = formatter.format(tunggakanDebtors),
                    percentage = tunggakanPct,
                    accentColor = Color(0xFFDC2626),
                    trackColor = Color(0xFFFEE2E2),
                    containerColor = Color(0xFFFEF2F2),
                    borderColor = Color(0xFFFEE2E2),
                    onClick = {
                        viewModel.filterKolek.value = "SEMUA"
                        if (effectiveScope == BerandaScope.KELOLAAN) onOpenKelolaan() else onOpenMonitoring()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("beranda_card_nasabah_tunggakan")
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
}

@Composable
private fun BerandaKpiPastelCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    containerColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        modifier = modifier
            .heightIn(min = 90.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF475569),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Portfolio12MonthChartCard(
    points: List<MonthlyPortfolioPoint>,
    selectedFilter: PortfolioChartFilter,
    onSelectFilter: (PortfolioChartFilter) -> Unit,
    selectedMonthIndex: Int,
    onSelectMonthIndex: (Int) -> Unit,
    scopeLabel: String,
    currentOs: Double,
    currentSml: Double,
    currentNpl: Double,
    formatter: NumberFormat
) {
    val osLineColor = Color(0xFF7DD3FC) // Crisp bright cyan-white
    val smlLineColor = Color(0xFFFBBF24) // Warm amber gold
    val nplLineColor = Color(0xFFFF6B81) // Coral red

    val populatedPoints = remember(points) { points.filter { it.hasData } }
    val latestPoint = populatedPoints.lastOrNull()
    val prevPoint = if (populatedPoints.size >= 2) populatedPoints[populatedPoints.size - 2] else null

    val displayOs = latestPoint?.os?.takeIf { it > 0.0 } ?: currentOs
    val displaySml = latestPoint?.sml?.takeIf { it > 0.0 } ?: currentSml
    val displayNpl = latestPoint?.npl?.takeIf { it > 0.0 } ?: currentNpl

    val activeMetricValue = when (selectedFilter) {
        PortfolioChartFilter.SML -> displaySml
        PortfolioChartFilter.NPL -> displayNpl
        else -> displayOs
    }
    val prevMetricValue = when (selectedFilter) {
        PortfolioChartFilter.SML -> prevPoint?.sml
        PortfolioChartFilter.NPL -> prevPoint?.npl
        else -> prevPoint?.os
    }
    val deltaPct = if (prevMetricValue != null && prevMetricValue > 0.0) {
        ((activeMetricValue - prevMetricValue) / prevMetricValue) * 100.0
    } else null

    val smlSharePct = if (displayOs > 0.0) (displaySml / displayOs) * 100.0 else 0.0
    val nplSharePct = if (displayOs > 0.0) (displayNpl / displayOs) * 100.0 else 0.0

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
                // Header: Title + Headline Number + Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (selectedFilter) {
                                PortfolioChartFilter.ALL -> "OS, SML & NPL • 12 Bln ($scopeLabel)"
                                PortfolioChartFilter.OS -> "Total OS • 12 Bln ($scopeLabel)"
                                PortfolioChartFilter.SML -> "Kredit SML • 12 Bln ($scopeLabel)"
                                PortfolioChartFilter.NPL -> "Kredit NPL • 12 Bln ($scopeLabel)"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatCompactIdr(activeMetricValue),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (deltaPct != null) {
                                val isUp = deltaPct >= 0.0
                                val isGood = if (selectedFilter == PortfolioChartFilter.SML || selectedFilter == PortfolioChartFilter.NPL) !isUp else isUp
                                val arrow = if (isUp) "▲" else "▼"
                                val badgeColor = if (isGood) Color(0xFF4ADE80) else Color(0xFFFF8A80)
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color.Black.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = "$arrow ${String.format(Locale.US, "%.1f", abs(deltaPct))}%",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Compact 12 Bln pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "12 Bln",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // 3-Item Legend / Interactive Toggle Chips: OS, SML, NPL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PortfolioSeriesChip(
                        label = "OS",
                        valueText = formatBerandaMillion(displayOs),
                        dotColor = osLineColor,
                        isSelected = selectedFilter == PortfolioChartFilter.ALL || selectedFilter == PortfolioChartFilter.OS,
                        isFocused = selectedFilter == PortfolioChartFilter.OS,
                        onClick = { onSelectFilter(PortfolioChartFilter.OS) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chart_chip_os")
                    )
                    PortfolioSeriesChip(
                        label = "SML (${String.format(Locale.US, "%.1f", smlSharePct)}%)",
                        valueText = formatBerandaMillion(displaySml),
                        dotColor = smlLineColor,
                        isSelected = selectedFilter == PortfolioChartFilter.ALL || selectedFilter == PortfolioChartFilter.SML,
                        isFocused = selectedFilter == PortfolioChartFilter.SML,
                        onClick = { onSelectFilter(PortfolioChartFilter.SML) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chart_chip_sml")
                    )
                    PortfolioSeriesChip(
                        label = "NPL (${String.format(Locale.US, "%.1f", nplSharePct)}%)",
                        valueText = formatBerandaMillion(displayNpl),
                        dotColor = nplLineColor,
                        isSelected = selectedFilter == PortfolioChartFilter.ALL || selectedFilter == PortfolioChartFilter.NPL,
                        isFocused = selectedFilter == PortfolioChartFilter.NPL,
                        onClick = { onSelectFilter(PortfolioChartFilter.NPL) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chart_chip_npl")
                    )
                }

                // Selected Month Inspection Tooltip (on tap)
                val inspectedPoint = points.getOrNull(selectedMonthIndex)
                AnimatedVisibility(visible = inspectedPoint != null) {
                    inspectedPoint?.let { pt ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.24f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (pt.hasData) {
                                    "${pt.fullLabel}: OS ${formatBerandaMillion(pt.os)} • SML ${formatBerandaMillion(pt.sml)} • NPL ${formatBerandaMillion(pt.npl)}"
                                } else {
                                    "${pt.fullLabel}: Belum ada data bulanan LW321"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // 12-Month Line Chart Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(118.dp)
                        .testTag("beranda_12m_chart")
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                if (points.isNotEmpty() && size.width > 0) {
                                    val step = size.width.toFloat() / (points.size - 1).coerceAtLeast(1)
                                    val idx = ((offset.x + step * 0.5f) / step).toInt().coerceIn(0, points.lastIndex)
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
                        val chartH = (h - topPad - bottomPad).coerceAtLeast(10f)
                        val count = points.size.coerceAtLeast(2)
                        val leftPad = 8.dp.toPx()
                        val rightPad = 8.dp.toPx()
                        val plotW = (w - leftPad - rightPad).coerceAtLeast(10f)
                        val stepX = plotW / (count - 1).toFloat()

                        // Horizontal subtle guide lines
                        for (g in 0..3) {
                            val gy = topPad + (chartH / 3f) * g
                            drawLine(
                                color = Color.White.copy(alpha = 0.14f),
                                start = Offset(leftPad, gy),
                                end = Offset(w - rightPad, gy),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Highlight vertical cursor if a month is tapped
                        if (selectedMonthIndex in points.indices) {
                            val cursorX = leftPad + selectedMonthIndex * stepX
                            drawLine(
                                color = Color.White.copy(alpha = 0.45f),
                                start = Offset(cursorX, topPad),
                                end = Offset(cursorX, topPad + chartH),
                                strokeWidth = 1.2.dp.toPx()
                            )
                        }

                        fun drawSeries(
                            values: List<Double>,
                            validFlags: List<Boolean>,
                            lineColor: Color,
                            bandMinFrac: Float,
                            bandMaxFrac: Float,
                            drawAreaFill: Boolean
                        ) {
                            val validVals = values.filterIndexed { idx, v -> validFlags[idx] && v >= 0.0 }
                            if (validVals.isEmpty()) return
                            val rawMin = validVals.minOrNull() ?: 0.0
                            val rawMax = validVals.maxOrNull() ?: 0.0
                            val range = (rawMax - rawMin).takeIf { it > 0.0 } ?: (rawMax * 0.2).coerceAtLeast(1.0)

                            val coords = mutableListOf<Pair<Int, Offset>>()
                            points.forEachIndexed { idx, _ ->
                                if (validFlags[idx]) {
                                    val normalized = if (rawMax == rawMin) {
                                        0.5f
                                    } else {
                                        ((values[idx] - rawMin) / range).toFloat().coerceIn(0f, 1f)
                                    }
                                    val bandY = bandMinFrac + normalized * (bandMaxFrac - bandMinFrac)
                                    val py = topPad + chartH * (1f - bandY)
                                    val px = leftPad + idx * stepX
                                    coords.add(idx to Offset(px, py))
                                }
                            }
                            if (coords.isEmpty()) return

                            if (coords.size >= 2) {
                                val linePath = Path().apply {
                                    moveTo(coords.first().second.x, coords.first().second.y)
                                    for (i in 0 until coords.lastIndex) {
                                        val p0 = coords[i].second
                                        val p1 = coords[i + 1].second
                                        val midX = (p0.x + p1.x) * 0.5f
                                        cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                    }
                                }

                                if (drawAreaFill) {
                                    val areaPath = Path().apply {
                                        addPath(linePath)
                                        lineTo(coords.last().second.x, topPad + chartH)
                                        lineTo(coords.first().second.x, topPad + chartH)
                                        close()
                                    }
                                    drawPath(
                                        path = areaPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                lineColor.copy(alpha = 0.28f),
                                                lineColor.copy(alpha = 0.02f)
                                            ),
                                            startY = topPad,
                                            endY = topPad + chartH
                                        )
                                    )
                                }

                                drawPath(
                                    path = linePath,
                                    color = lineColor,
                                    style = Stroke(
                                        width = 2.4.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            coords.forEach { (idx, pt) ->
                                val isPointSelected = idx == selectedMonthIndex
                                if (isPointSelected) {
                                    drawCircle(
                                        color = lineColor.copy(alpha = 0.35f),
                                        radius = 7.dp.toPx(),
                                        center = pt
                                    )
                                }
                                drawCircle(
                                    color = lineColor,
                                    radius = if (isPointSelected) 4.2.dp.toPx() else 3.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = Color(0xFF004B9B),
                                    radius = if (isPointSelected) 1.8.dp.toPx() else 1.2.dp.toPx(),
                                    center = pt
                                )
                            }
                        }

                        val flags = points.map { it.hasData }
                        when (selectedFilter) {
                            PortfolioChartFilter.ALL -> {
                                // Clear visual bands so OS, SML, and NPL trends never collapse into one flat line
                                drawSeries(
                                    values = points.map { it.npl },
                                    validFlags = flags,
                                    lineColor = nplLineColor,
                                    bandMinFrac = 0.06f,
                                    bandMaxFrac = 0.36f,
                                    drawAreaFill = false
                                )
                                drawSeries(
                                    values = points.map { it.sml },
                                    validFlags = flags,
                                    lineColor = smlLineColor,
                                    bandMinFrac = 0.34f,
                                    bandMaxFrac = 0.66f,
                                    drawAreaFill = false
                                )
                                drawSeries(
                                    values = points.map { it.os },
                                    validFlags = flags,
                                    lineColor = osLineColor,
                                    bandMinFrac = 0.64f,
                                    bandMaxFrac = 0.94f,
                                    drawAreaFill = true
                                )
                            }
                            PortfolioChartFilter.OS -> {
                                drawSeries(
                                    values = points.map { it.os },
                                    validFlags = flags,
                                    lineColor = osLineColor,
                                    bandMinFrac = 0.12f,
                                    bandMaxFrac = 0.90f,
                                    drawAreaFill = true
                                )
                            }
                            PortfolioChartFilter.SML -> {
                                drawSeries(
                                    values = points.map { it.sml },
                                    validFlags = flags,
                                    lineColor = smlLineColor,
                                    bandMinFrac = 0.12f,
                                    bandMaxFrac = 0.90f,
                                    drawAreaFill = true
                                )
                            }
                            PortfolioChartFilter.NPL -> {
                                drawSeries(
                                    values = points.map { it.npl },
                                    validFlags = flags,
                                    lineColor = nplLineColor,
                                    bandMinFrac = 0.12f,
                                    bandMaxFrac = 0.90f,
                                    drawAreaFill = true
                                )
                            }
                        }
                    }
                }

                // 12-Month X-Axis Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEachIndexed { idx, pt ->
                        val isActive = idx == selectedMonthIndex || (selectedMonthIndex == -1 && idx == points.lastIndex)
                        Text(
                            text = pt.shortLabel,
                            fontSize = 8.5.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isActive) Color.White else if (pt.hasData) Color.White.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.42f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectMonthIndex(idx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortfolioSeriesChip(
    label: String,
    valueText: String,
    dotColor: Color,
    isSelected: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = when {
            isFocused -> Color.White.copy(alpha = 0.24f)
            isSelected -> Color.White.copy(alpha = 0.14f)
            else -> Color.White.copy(alpha = 0.06f)
        },
        border = if (isFocused) BorderStroke(1.dp, dotColor) else BorderStroke(0.6.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(dotColor, CircleShape)
                )
                Text(
                    text = label,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = if (isSelected) 0.94f else 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = valueText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = if (isSelected) 1f else 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BerandaDonutSummaryCard(
    title: String,
    countText: String,
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
        animationSpec = tween(durationMillis = 550),
        label = "donut_sweep_$title"
    )

    Surface(
        modifier = modifier
            .heightIn(min = 84.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = countText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1
                )
                Text(
                    text = "${String.format(Locale.US, "%.1f", percentage)}%",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1
                )
            }

            Canvas(modifier = Modifier.size(46.dp)) {
                val strokeW = 7.dp.toPx()
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
        }
    }
}

private fun formatBerandaMillion(value: Double): String {
    if (value <= 0.0) return "0"
    val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    return when {
        value >= 1_000_000.0 -> {
            val millions = value / 1_000_000.0
            val df = if (millions >= 100.0) {
                DecimalFormat("#,##0", symbols)
            } else {
                DecimalFormat("#,##0.#", symbols)
            }
            "${df.format(millions)} Jt"
        }
        else -> {
            val df = DecimalFormat("#,##0", symbols)
            df.format(value)
        }
    }
}

private fun formatCompactIdr(value: Double): String {
    if (value <= 0.0) return "Rp 0"
    val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    return when {
        value >= 1_000_000_000.0 -> {
            val m = value / 1_000_000_000.0
            "Rp ${DecimalFormat("#,##0.##", symbols).format(m)} M"
        }
        value >= 1_000_000.0 -> {
            val jt = value / 1_000_000.0
            "Rp ${DecimalFormat("#,##0.#", symbols).format(jt)} Jt"
        }
        else -> "Rp ${DecimalFormat("#,##0", symbols).format(value)}"
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
