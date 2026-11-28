package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.data.model.desa
import com.example.ui.components.AnimatedFadeSlideDialog
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.MantriDetailData
import com.example.ui.viewmodel.SortColumn
import com.example.ui.viewmodel.SortDirection
import com.example.ui.viewmodel.UnitSummaryData
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun getMonthsBetweenDates(startDateStr: String, endDateStr: String): Int? {
    if (startDateStr.isBlank() || endDateStr.isBlank()) return null
    return try {
        val parseDate = { str: String ->
            val clean = str.trim().split(" ").firstOrNull() ?: ""
            val parts = clean.split('/', '-', '.')
            if (parts.size >= 3) {
                if (parts[0].length == 4) {
                    val y = parts[0].toIntOrNull() ?: 2026
                    val m = parts[1].toIntOrNull() ?: 1
                    val d = parts[2].toIntOrNull() ?: 1
                    java.time.LocalDate.of(y, m.coerceIn(1, 12), d.coerceIn(1, 28))
                } else {
                    val p0 = parts[0].toIntOrNull() ?: 1
                    val p1 = parts[1].toIntOrNull() ?: 1
                    var y = parts[2].toIntOrNull() ?: 2026
                    if (y < 100) y += 2000
                    val (d, m) = if (p1 > 12 && p0 <= 12) p1 to p0 else p0 to p1
                    java.time.LocalDate.of(y, m.coerceIn(1, 12), d.coerceIn(1, 28))
                }
            } else null
        }
        val start = parseDate(startDateStr) ?: return null
        val end = parseDate(endDateStr) ?: java.time.LocalDate.now()
        if (start.isAfter(end)) return 0
        java.time.temporal.ChronoUnit.MONTHS.between(start, end).toInt()
    } catch (e: Exception) {
        null
    }
}

@Composable
fun DashboardUnitView(
    viewModel: CustomerViewModel,
    formatter: NumberFormat,
    onCustomerClick: (CustomerEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboardPeriodSource by viewModel.dashboardPeriodSource.collectAsState()
    val periodDateBulanLalu by viewModel.periodDateBulanLalu.collectAsState()
    val periodDateAkhirBulan by viewModel.periodDateAkhirBulan.collectAsState()
    val periodDateDaily by viewModel.periodDateDaily.collectAsState()

    val summaryBlnLalu by viewModel.summaryBulanLalu.collectAsState()
    val summaryAkhirBln by viewModel.summaryAkhirBulan.collectAsState()
    val summaryDaily by viewModel.summaryDaily.collectAsState()

    val kolekSummaryBlnLalu by viewModel.kolekSummaryBulanLalu.collectAsState()
    val kolekSummaryAkhirBln by viewModel.kolekSummaryAkhirBulan.collectAsState()
    val kolekSummaryDaily by viewModel.kolekSummaryDaily.collectAsState()

    val mantriSummaryBlnLalu by viewModel.mantriSummaryBulanLalu.collectAsState()
    val mantriSummaryAkhirBln by viewModel.mantriSummaryAkhirBulan.collectAsState()
    val mantriSummaryDaily by viewModel.mantriSummaryDaily.collectAsState()

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val currentCustomers by viewModel.customersList.collectAsState()
    val dailyDataMap by viewModel.dailyDataMap.collectAsState()
    val availableMantris by viewModel.availableMantris.collectAsState()
    val filterMantri by viewModel.filterMantriDashboard.collectAsState()

    val bulanLaluCustomers by viewModel.bulanLaluCustomers.collectAsState()
    val akhirBulanCustomers by viewModel.akhirBulanCustomers.collectAsState()
    val dailyCustomers by viewModel.dailyCustomers.collectAsState()
    val lw325DailyCustomers by viewModel.lw325DailyCustomers.collectAsState()
    val priorMonthLw321KolekMap by viewModel.priorMonthLw321KolekMap.collectAsState()

    // State untuk Popup Tabel Nasabah
    var showCustomerPopup by remember { mutableStateOf(false) }
    var popupDialogTitle by remember { mutableStateOf("") }
    var popupDialogCustomers by remember { mutableStateOf<List<CustomerEntity>>(emptyList()) }

    // State untuk Popup Tabel Nasabah Realisasi (Net Disburs)
    var showRealizedPopup by remember { mutableStateOf(false) }
    var realizedPopupMantriName by remember { mutableStateOf("") }
    var realizedPopupItems by remember { mutableStateOf<List<RealizedCustomerItem>>(emptyList()) }

    // State untuk Hide / Show bagian Segmen, Downgrade, Upgrade
    var isSegmenExpanded by remember { mutableStateOf(false) }
    var isDowngradeExpanded by remember { mutableStateOf(false) }
    var isUpgradeExpanded by remember { mutableStateOf(false) }

    val dateBln = remember(periodDateBulanLalu) {
        periodDateBulanLalu.ifBlank { "31/08/2026" }
    }
    val dateAkhir = remember(periodDateAkhirBulan) {
        periodDateAkhirBulan.ifBlank { "30/09/2026" }
    }
    val dateDaily = remember(periodDateDaily) {
        periodDateDaily.ifBlank { "23/10/2026" }
    }

    LaunchedEffect(Unit) {
        if (summaryDaily.debtors == 0 || dailyCustomers.isEmpty()) {
            viewModel.loadSummaryForPeriod("DAILY")
        }
        if (summaryAkhirBln.debtors == 0 || akhirBulanCustomers.isEmpty()) {
            viewModel.loadSummaryForPeriod("AKHIR_BULAN")
        }
        if (summaryBlnLalu.debtors == 0 || bulanLaluCustomers.isEmpty()) {
            viewModel.loadSummaryForPeriod("BULAN_LALU")
        }
        if (lw325DailyCustomers.isEmpty()) {
            viewModel.loadLw325DailyCustomers()
        }
    }

    // Trigger pemuatan data LW321 All bulan sebelumnya secara asynchronous & non-blocking
    LaunchedEffect(dashboardPeriodSource, periodDateBulanLalu, dateBln) {
        if (dashboardPeriodSource == "BULAN_LALU") {
            val (blnStr, _) = extractMonthAndYear(dateBln)
            val currentM = blnStr.toIntOrNull() ?: 8
            val priorM = if (currentM == 1) 12 else currentM - 1
            viewModel.loadPriorMonthLw321KolekMap(priorM)
            if (bulanLaluCustomers.isEmpty()) {
                viewModel.loadSummaryForPeriod("BULAN_LALU")
            }
        } else if (dashboardPeriodSource == "AKHIR_BULAN") {
            if (bulanLaluCustomers.isEmpty()) {
                viewModel.loadSummaryForPeriod("BULAN_LALU")
            }
            if (akhirBulanCustomers.isEmpty()) {
                viewModel.loadSummaryForPeriod("AKHIR_BULAN")
            }
        } else if (dashboardPeriodSource == "DAILY") {
            if (akhirBulanCustomers.isEmpty()) {
                viewModel.loadSummaryForPeriod("AKHIR_BULAN")
            }
            if (dailyCustomers.isEmpty()) {
                viewModel.loadSummaryForPeriod("DAILY")
            }
        }
    }

    // Dynamic list of all mantri names across all available data sources (cleaned without PN)
    val allMantriNames = remember(mantriSummaryBlnLalu, mantriSummaryAkhirBln, mantriSummaryDaily) {
        val all = (mantriSummaryBlnLalu.keys + mantriSummaryAkhirBln.keys + mantriSummaryDaily.keys)
            .map { cleanMantriNameOnly(it) }
            .filter { it.isNotBlank() && it != "SEMUA" && it != "TANPA NAMA" }
            .distinct()
        val nonBlank = all.filter { it != "Blank PN" }.sorted()
        val hasBlank = all.any { it == "Blank PN" }
        if (hasBlank) nonBlank + listOf("Blank PN") else nonBlank
    }

    // Combined options for dropdown (clean without PN)
    val mantriOptions = remember(availableMantris, allMantriNames) {
        val merged = (availableMantris.filter { it != "SEMUA" } + allMantriNames)
            .map { cleanMantriNameOnly(it) }
            .filter { it.isNotBlank() && it != "TANPA NAMA" }
            .distinct()
        val nonBlank = merged.filter { it != "Blank PN" }.sorted()
        val hasBlank = merged.any { it == "Blank PN" }
        listOf("SEMUA") + (if (hasBlank) nonBlank + listOf("Blank PN") else nonBlank)
    }

    fun findMantriData(map: Map<String, MantriDetailData>, targetName: String): MantriDetailData? {
        if (targetName == "SEMUA") return null
        val cleanTarget = cleanMantriNameOnly(targetName)
        return map[targetName] ?: map.entries.firstOrNull {
            cleanMantriNameOnly(it.key).equals(cleanTarget, ignoreCase = true)
        }?.value
    }

    val selectedMantriBln = if (filterMantri != "SEMUA") findMantriData(mantriSummaryBlnLalu, filterMantri) else null
    val selectedMantriAkhir = if (filterMantri != "SEMUA") findMantriData(mantriSummaryAkhirBln, filterMantri) else null
    val selectedMantriDaily = if (filterMantri != "SEMUA") findMantriData(mantriSummaryDaily, filterMantri) else null

    // Perhitungan MoM adalah membandingkan data Bulan Lalu dengan Akhir bulan
    val momOs = if (filterMantri != "SEMUA") {
        (selectedMantriAkhir?.total?.os ?: 0.0) - (selectedMantriBln?.total?.os ?: 0.0)
    } else {
        summaryAkhirBln.os - summaryBlnLalu.os
    }

    val mtdOs = if (filterMantri != "SEMUA") {
        (selectedMantriDaily?.total?.os ?: 0.0) - (selectedMantriAkhir?.total?.os ?: 0.0)
    } else {
        summaryDaily.os - summaryAkhirBln.os
    }

    val momLancar = if (filterMantri != "SEMUA") {
        (selectedMantriAkhir?.lancar?.os ?: 0.0) - (selectedMantriBln?.lancar?.os ?: 0.0)
    } else {
        kolekSummaryAkhirBln.lancar.os - kolekSummaryBlnLalu.lancar.os
    }
    val mtdLancar = if (filterMantri != "SEMUA") {
        (selectedMantriDaily?.lancar?.os ?: 0.0) - (selectedMantriAkhir?.lancar?.os ?: 0.0)
    } else {
        kolekSummaryDaily.lancar.os - kolekSummaryAkhirBln.lancar.os
    }

    val momSml = if (filterMantri != "SEMUA") {
        (selectedMantriAkhir?.sml?.os ?: 0.0) - (selectedMantriBln?.sml?.os ?: 0.0)
    } else {
        kolekSummaryAkhirBln.sml.os - kolekSummaryBlnLalu.sml.os
    }
    val mtdSml = if (filterMantri != "SEMUA") {
        (selectedMantriDaily?.sml?.os ?: 0.0) - (selectedMantriAkhir?.sml?.os ?: 0.0)
    } else {
        kolekSummaryDaily.sml.os - kolekSummaryAkhirBln.sml.os
    }

    val momNpl = if (filterMantri != "SEMUA") {
        (selectedMantriAkhir?.npl?.os ?: 0.0) - (selectedMantriBln?.npl?.os ?: 0.0)
    } else {
        kolekSummaryAkhirBln.npl.os - kolekSummaryBlnLalu.npl.os
    }
    val mtdNpl = if (filterMantri != "SEMUA") {
        (selectedMantriDaily?.npl?.os ?: 0.0) - (selectedMantriAkhir?.npl?.os ?: 0.0)
    } else {
        kolekSummaryDaily.npl.os - kolekSummaryAkhirBln.npl.os
    }

    val dynamicSummary = remember(currentCustomers) {
        UnitSummaryData(
            os = currentCustomers.sumOf { it.osPinjaman },
            debtors = currentCustomers.size
        )
    }

    val dynamicKolekSummary = remember(currentCustomers) {
        val lancar = currentCustomers.filter { it.kolek == "1" || it.kolek.contains("LANCAR", ignoreCase = true) }
        val sml = currentCustomers.filter { it.kolek == "2" || it.kolek.contains("DPK", ignoreCase = true) || it.kolek.contains("PERHATIAN", ignoreCase = true) }
        val npl = currentCustomers.filter { it.kolek in listOf("3", "4", "5", "KL", "D", "M") || it.kolek.contains("KURANG", ignoreCase = true) || it.kolek.contains("DIRAGUKAN", ignoreCase = true) || it.kolek.contains("MACET", ignoreCase = true) }
        com.example.ui.viewmodel.KolekSummaryData(
            lancar = UnitSummaryData(os = lancar.sumOf { it.osPinjaman }, debtors = lancar.size),
            sml = UnitSummaryData(os = sml.sumOf { it.osPinjaman }, debtors = sml.size),
            npl = UnitSummaryData(os = npl.sumOf { it.osPinjaman }, debtors = npl.size)
        )
    }

    val activeSummary = remember(dashboardPeriodSource, summaryBlnLalu, summaryAkhirBln, summaryDaily, dynamicSummary, filterMantri, selectedMantriBln, selectedMantriAkhir, selectedMantriDaily) {
        if (filterMantri != "SEMUA") {
            when (dashboardPeriodSource) {
                "BULAN_LALU" -> selectedMantriBln?.total ?: UnitSummaryData()
                "DAILY" -> selectedMantriDaily?.total ?: UnitSummaryData()
                else -> selectedMantriAkhir?.total ?: UnitSummaryData()
            }
        } else {
            when (dashboardPeriodSource) {
                "BULAN_LALU" -> if (summaryBlnLalu.debtors > 0) summaryBlnLalu else dynamicSummary
                "DAILY" -> if (summaryDaily.debtors > 0) summaryDaily else dynamicSummary
                else -> if (summaryAkhirBln.debtors > 0) summaryAkhirBln else dynamicSummary
            }
        }
    }

    val activeKolekSummary = remember(dashboardPeriodSource, kolekSummaryBlnLalu, kolekSummaryAkhirBln, kolekSummaryDaily, dynamicKolekSummary, filterMantri, selectedMantriBln, selectedMantriAkhir, selectedMantriDaily) {
        if (filterMantri != "SEMUA") {
            val mantriDetail = when (dashboardPeriodSource) {
                "BULAN_LALU" -> selectedMantriBln
                "DAILY" -> selectedMantriDaily
                else -> selectedMantriAkhir
            }
            if (mantriDetail != null) {
                com.example.ui.viewmodel.KolekSummaryData(
                    lancar = mantriDetail.lancar,
                    sml = mantriDetail.sml,
                    npl = mantriDetail.npl
                )
            } else {
                com.example.ui.viewmodel.KolekSummaryData()
            }
        } else {
            when (dashboardPeriodSource) {
                "BULAN_LALU" -> if (kolekSummaryBlnLalu.lancar.debtors > 0) kolekSummaryBlnLalu else dynamicKolekSummary
                "DAILY" -> if (kolekSummaryDaily.lancar.debtors > 0) kolekSummaryDaily else dynamicKolekSummary
                else -> if (kolekSummaryAkhirBln.lancar.debtors > 0) kolekSummaryAkhirBln else dynamicKolekSummary
            }
        }
    }

    // Nasabah dinamis mengikuti pilihan periode (Bulan Lalu, Akhir Bulan, Daily)
    val activePeriodCustomers = remember(
        dashboardPeriodSource,
        bulanLaluCustomers,
        akhirBulanCustomers,
        dailyCustomers,
        currentCustomers,
        dailyDataMap,
        filterMantri
    ) {
        val list = when (dashboardPeriodSource) {
            "BULAN_LALU" -> {
                if (bulanLaluCustomers.isNotEmpty()) bulanLaluCustomers
                else currentCustomers.map { it.copy(kolek = it.kolekBlnLalu.ifBlank { it.kolek }) }
            }
            "DAILY" -> {
                if (dailyCustomers.isNotEmpty()) dailyCustomers
                else {
                    val dList = dailyDataMap.values.distinctBy { it.noRekening.ifBlank { it.cif } }
                    if (dList.isNotEmpty()) dList else currentCustomers
                }
            }
            else -> {
                if (akhirBulanCustomers.isNotEmpty()) akhirBulanCustomers
                else currentCustomers
            }
        }
        if (filterMantri != "SEMUA") {
            val cleanSelected = cleanMantriNameOnly(filterMantri)
            list.filter {
                val mName = cleanMantriNameOnly(it.namaMantri)
                val pName = cleanMantriNameOnly(it.namaMantriPrakarsa)
                val target = if (mName.isNotBlank() && mName != "TANPA NAMA") mName else if (pName.isNotBlank() && pName != "TANPA NAMA") pName else "Blank PN"
                target.equals(cleanSelected, ignoreCase = true)
            }
        } else {
            list
        }
    }

    // Kolektabilitas SML1,2,3, KL, D, M dinamis mengikuti pilihan periode
    val isSml1 = { c: CustomerEntity ->
        val k = c.kolek.trim().uppercase()
        k == "SML1" || k == "SML 1" || k == "SML-1" || k == "2.1" || k.contains("DPK1") || (k.contains("SML") && c.umurTgk in 1..30)
    }
    val isSml2 = { c: CustomerEntity ->
        val k = c.kolek.trim().uppercase()
        k == "SML2" || k == "SML 2" || k == "SML-2" || k == "2.2" || k.contains("DPK2") || (k.contains("SML") && c.umurTgk in 31..60)
    }
    val isSml3 = { c: CustomerEntity ->
        val k = c.kolek.trim().uppercase()
        k == "SML3" || k == "SML 3" || k == "SML-3" || k == "2.3" || k.contains("DPK3") || (k.contains("SML") && c.umurTgk > 60)
    }
    val isKl = { c: CustomerEntity ->
        val k = c.kolek.trim().uppercase()
        k == "3" || k == "KL" || k == "KURANG LANCAR" || (k.contains("KURANG") && k.contains("LANCAR"))
    }
    val isD = { c: CustomerEntity ->
        val k = c.kolek.trim().uppercase()
        k == "4" || k == "D" || k == "DIRAGUKAN" || k.contains("DIRAGUKAN")
    }
    val isM = { c: CustomerEntity ->
        val k = c.kolek.trim().uppercase()
        k == "5" || k == "M" || k == "MACET" || k.contains("MACET")
    }

    data class KolekBuckets(
        val sml1: List<CustomerEntity>,
        val sml2: List<CustomerEntity>,
        val sml3: List<CustomerEntity>,
        val kl: List<CustomerEntity>,
        val d: List<CustomerEntity>,
        val m: List<CustomerEntity>
    )

    val (sml1List, sml2List, sml3List, klList, dList, mList) = remember(activePeriodCustomers) {
        val s1 = ArrayList<CustomerEntity>()
        val s2 = ArrayList<CustomerEntity>()
        val s3 = ArrayList<CustomerEntity>()
        val kl = ArrayList<CustomerEntity>()
        val d = ArrayList<CustomerEntity>()
        val m = ArrayList<CustomerEntity>()
        for (c in activePeriodCustomers) {
            when {
                isSml1(c) -> s1.add(c)
                isSml2(c) -> s2.add(c)
                isSml3(c) -> s3.add(c)
                isKl(c) -> kl.add(c)
                isD(c) -> d.add(c)
                isM(c) -> m.add(c)
            }
        }
        KolekBuckets(s1, s2, s3, kl, d, m)
    }

    val sml1Os = sml1List.sumOf { it.osPinjaman }
    val sml1Deb = sml1List.size
    val sml2Os = sml2List.sumOf { it.osPinjaman }
    val sml2Deb = sml2List.size
    val sml3Os = sml3List.sumOf { it.osPinjaman }
    val sml3Deb = sml3List.size

    val klOs = klList.sumOf { it.osPinjaman }
    val klDeb = klList.size
    val dOs = dList.sumOf { it.osPinjaman }
    val dDeb = dList.size
    val mOs = mList.sumOf { it.osPinjaman }
    val mDeb = mList.size

    // Segmen Kredit dinamis mengikuti pilihan periode
    val segmenGroups = remember(activePeriodCustomers) {
        activePeriodCustomers.groupBy { it.segmen.trim().uppercase().ifBlank { "LAINNYA" } }
            .map { (name, group) ->
                Triple(name, group.sumOf { it.osPinjaman }, group)
            }
            .sortedByDescending { it.second }
    }

    // Perhitungan Kolek Downgrade & Upgrade dinamis:
    // 1. Akhir Bulan: membandingkan kolek Bulan Lalu dengan kolek Akhir Bulan
    // 2. Bulan Lalu: membandingkan dengan Bulan sebelumnya (LW321 All, misal bulan 8 membandingkan dengan bulan 7)
    // 3. Daily: membandingkan kolek Akhir Bulan dengan kolek Daily
    data class TransitionGroupResult(
        val downgradeMap: Map<String, List<CustomerEntity>>,
        val upgradeMap: Map<String, List<CustomerEntity>>,
        val lunasList: List<CustomerEntity>,
        val nplLunasList: List<CustomerEntity>,
        val tunggakanKurang6BlnList: List<CustomerEntity> = emptyList(),
        val tukcilList: List<CustomerEntity> = emptyList(),
        val dhNewList: List<CustomerEntity> = emptyList()
    )

    val transitionResult by produceState(
        initialValue = TransitionGroupResult(emptyMap(), emptyMap(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
        dashboardPeriodSource,
        bulanLaluCustomers,
        akhirBulanCustomers,
        dailyCustomers,
        lw325DailyCustomers,
        currentCustomers,
        dailyDataMap,
        priorMonthLw321KolekMap,
        filterMantri
    ) {
        value = withContext(Dispatchers.Default) {
        val downgradeMap = mutableMapOf<String, MutableList<CustomerEntity>>()
        val upgradeMap = mutableMapOf<String, MutableList<CustomerEntity>>()
        val lunasList = mutableListOf<CustomerEntity>()
        val nplLunasList = mutableListOf<CustomerEntity>()

        val cleanSel = if (filterMantri != "SEMUA") cleanMantriNameOnly(filterMantri) else ""

        fun filterByMantri(list: List<CustomerEntity>): List<CustomerEntity> {
            if (cleanSel.isBlank()) return list
            return list.filter { cleanMantriNameOnly(it.namaMantri).equals(cleanSel, ignoreCase = true) }
        }

        when (dashboardPeriodSource) {
            "BULAN_LALU" -> {
                val base = filterByMantri(if (bulanLaluCustomers.isNotEmpty()) bulanLaluCustomers else currentCustomers)
                base.forEach { cust ->
                    val cleanRek = cust.noRekening.filter { it.isDigit() }
                    val cleanCif = cust.cif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
                    val formattedRek = com.example.data.network.SpreadsheetService.formatRekening15(cust.noRekening)

                    val priorK = priorMonthLw321KolekMap[cleanRek]
                        ?: priorMonthLw321KolekMap[formattedRek]
                        ?: priorMonthLw321KolekMap[cleanCif]
                        ?: priorMonthLw321KolekMap["CIF-$cleanCif"]
                        ?: cust.kolekBlnLalu.ifBlank { cust.kolek }

                    val currK = cust.kolek.ifBlank { cust.kolekBlnLalu }

                    val (oldCode, oldRank) = parseKolekCodeAndRank(priorK)
                    val (newCode, newRank) = parseKolekCodeAndRank(currK)

                    if (oldRank in 1..7 && newRank in 1..7) {
                        if (newRank > oldRank) {
                            downgradeMap.getOrPut("$oldCode ➔ $newCode") { mutableListOf() }.add(cust)
                        } else if (newRank < oldRank) {
                            upgradeMap.getOrPut("$oldCode ➔ $newCode") { mutableListOf() }.add(cust)
                        }
                    }
                }
            }
            "DAILY" -> {
                val baseAkhir = filterByMantri(if (akhirBulanCustomers.isNotEmpty()) akhirBulanCustomers else currentCustomers)
                val dailyRekSet = mutableSetOf<String>()
                dailyDataMap.values.forEach { d ->
                    if (d.noRekening.isNotBlank()) dailyRekSet.add(d.noRekening)
                    val cr = d.noRekening.filter { it.isDigit() }
                    if (cr.isNotBlank()) dailyRekSet.add(cr)
                    val cc = d.cif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
                    if (cc.isNotBlank()) {
                        dailyRekSet.add(cc)
                        dailyRekSet.add("CIF-$cc")
                    }
                }

                baseAkhir.forEach { cust ->
                    val cr = cust.noRekening.filter { it.isDigit() }
                    val cc = cust.cif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }

                    val dCust = dailyDataMap[cust.noRekening]
                        ?: (if (cr.isNotBlank()) dailyDataMap[cr] else null)
                        ?: (if (cc.isNotBlank()) dailyDataMap[cc] else null)

                    if (dCust != null) {
                        val oldK = cust.kolek
                        val newK = dCust.kolek
                        val (oldCode, oldRank) = parseKolekCodeAndRank(oldK)
                        val (newCode, newRank) = parseKolekCodeAndRank(newK)

                        val custWithOld = cust.copy(kolek = newK, kolekBlnLalu = oldK)
                        if (newRank > oldRank) {
                            downgradeMap.getOrPut("$oldCode ➔ $newCode") { mutableListOf() }.add(custWithOld)
                        } else if (newRank < oldRank) {
                            upgradeMap.getOrPut("$oldCode ➔ $newCode") { mutableListOf() }.add(custWithOld)
                        }
                    } else if (dailyDataMap.isNotEmpty()) {
                    val exists = dailyRekSet.contains(cust.noRekening) ||
                            (cr.isNotBlank() && dailyRekSet.contains(cr)) ||
                            (cc.isNotBlank() && dailyRekSet.contains(cc))
                    if (!exists) {
                        val custWithBln = cust.copy(kolekBlnLalu = cust.kolek)
                        if (isNplKolek(cust.kolek)) {
                            nplLunasList.add(custWithBln)
                        } else {
                            lunasList.add(custWithBln)
                        }
                    }
                }
            }
        }
        else -> {
            // AKHIR_BULAN: Membandingkan Bulan Lalu dengan Akhir Bulan
            val baseBlnLalu = filterByMantri(
                if (bulanLaluCustomers.isNotEmpty()) bulanLaluCustomers
                else currentCustomers.map { it.copy(kolek = it.kolekBlnLalu.ifBlank { it.kolek }) }
            )
            val currAkhir = filterByMantri(
                if (akhirBulanCustomers.isNotEmpty()) akhirBulanCustomers
                else currentCustomers
            )

            val blnMap = mutableMapOf<String, CustomerEntity>()
            baseBlnLalu.forEach { bCust ->
                val rawRek = bCust.noRekening.trim()
                if (rawRek.isNotBlank()) blnMap[rawRek] = bCust
                val cr = rawRek.filter { it.isDigit() }
                if (cr.isNotBlank()) {
                    blnMap[cr] = bCust
                    val noZero = cr.trimStart('0')
                    if (noZero.isNotBlank()) blnMap[noZero] = bCust
                    if (cr.length >= 15) blnMap[cr.takeLast(15)] = bCust
                }
                val formattedRek = com.example.data.network.SpreadsheetService.formatRekening15(bCust.noRekening)
                if (formattedRek.isNotBlank()) blnMap[formattedRek] = bCust
                val rawCif = bCust.cif.trim()
                if (rawCif.isNotBlank()) blnMap[rawCif] = bCust
                val cc = rawCif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
                if (cc.isNotBlank()) {
                    blnMap[cc] = bCust
                    blnMap["CIF-$cc"] = bCust
                }
            }

            val akhirRekSet = mutableSetOf<String>()
            currAkhir.forEach { c ->
                val rawRek = c.noRekening.trim()
                if (rawRek.isNotBlank()) akhirRekSet.add(rawRek)
                val cr = rawRek.filter { it.isDigit() }
                if (cr.isNotBlank()) {
                    akhirRekSet.add(cr)
                    val noZero = cr.trimStart('0')
                    if (noZero.isNotBlank()) akhirRekSet.add(noZero)
                    if (cr.length >= 15) akhirRekSet.add(cr.takeLast(15))
                }
                val formattedRek = com.example.data.network.SpreadsheetService.formatRekening15(c.noRekening)
                if (formattedRek.isNotBlank()) akhirRekSet.add(formattedRek)
                val rawCif = c.cif.trim()
                if (rawCif.isNotBlank()) akhirRekSet.add(rawCif)
                val cc = rawCif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
                if (cc.isNotBlank()) {
                    akhirRekSet.add(cc)
                    akhirRekSet.add("CIF-$cc")
                }
            }

            // Perbandingan transisi Kolek Upgrade & Downgrade
            currAkhir.forEach { cust ->
                val rawRek = cust.noRekening.trim()
                val cr = rawRek.filter { it.isDigit() }
                val noZero = cr.trimStart('0')
                val formattedRek = com.example.data.network.SpreadsheetService.formatRekening15(cust.noRekening)
                val rawCif = cust.cif.trim()
                val cc = rawCif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }

                val blnCust = blnMap[rawRek]
                    ?: (if (cr.isNotBlank()) blnMap[cr] else null)
                    ?: (if (noZero.isNotBlank()) blnMap[noZero] else null)
                    ?: (if (cr.length >= 15) blnMap[cr.takeLast(15)] else null)
                    ?: (if (formattedRek.isNotBlank()) blnMap[formattedRek] else null)
                    ?: (if (rawCif.isNotBlank()) blnMap[rawCif] else null)
                    ?: (if (cc.isNotBlank()) blnMap[cc] ?: blnMap["CIF-$cc"] else null)

                val oldK = when {
                    blnCust != null && blnCust.kolek.isNotBlank() -> blnCust.kolek
                    cust.kolekBlnLalu.isNotBlank() -> cust.kolekBlnLalu
                    else -> priorMonthLw321KolekMap[cr]
                        ?: (if (noZero.isNotBlank()) priorMonthLw321KolekMap[noZero] else null)
                        ?: priorMonthLw321KolekMap[formattedRek]
                        ?: (if (cc.isNotBlank()) priorMonthLw321KolekMap[cc] ?: priorMonthLw321KolekMap["CIF-$cc"] else null)
                        ?: cust.kolek
                }

                val newK = cust.kolek

                val (oldCode, oldRank) = parseKolekCodeAndRank(oldK)
                val (newCode, newRank) = parseKolekCodeAndRank(newK)

                if (oldRank in 1..7 && newRank in 1..7) {
                    val custWithOld = cust.copy(kolekBlnLalu = oldK)
                    if (newRank > oldRank) {
                        // Perbankan kredit: nasabah Lancar (0 hari DPK) tidak dapat langsung anjlok ke SML2..M (31+ hari DPK) dalam 1 bulan
                        // L-SML2, L-SML3, L-KL, L-D, L-M merupakan anomali akibat ketiadaan data riil Bulan Lalu
                        val isInvalidDirectDowngradeFromL = (oldCode == "L" && newCode in listOf("SML2", "SML3", "KL", "D", "M"))
                        if (!isInvalidDirectDowngradeFromL) {
                            downgradeMap.getOrPut("$oldCode ➔ $newCode") { mutableListOf() }.add(custWithOld)
                        }
                    } else if (newRank < oldRank) {
                        upgradeMap.getOrPut("$oldCode ➔ $newCode") { mutableListOf() }.add(custWithOld)
                    }
                }
            }

            // Nasabah hilang di Akhir Bulan: Lunas vs DH
            baseBlnLalu.forEach { bCust ->
                val rawRek = bCust.noRekening.trim()
                val cr = rawRek.filter { it.isDigit() }
                val noZero = cr.trimStart('0')
                val formattedRek = com.example.data.network.SpreadsheetService.formatRekening15(bCust.noRekening)
                val rawCif = bCust.cif.trim()
                val cc = rawCif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }

                val exists = akhirRekSet.contains(rawRek) ||
                        (cr.isNotBlank() && akhirRekSet.contains(cr)) ||
                        (noZero.isNotBlank() && akhirRekSet.contains(noZero)) ||
                        (cr.length >= 15 && akhirRekSet.contains(cr.takeLast(15))) ||
                        (formattedRek.isNotBlank() && akhirRekSet.contains(formattedRek)) ||
                        (rawCif.isNotBlank() && akhirRekSet.contains(rawCif)) ||
                        (cc.isNotBlank() && akhirRekSet.contains(cc)) ||
                        (cc.isNotBlank() && akhirRekSet.contains("CIF-$cc"))
                if (!exists && currAkhir.isNotEmpty()) {
                    val bCustWithBln = bCust.copy(kolekBlnLalu = bCust.kolek)
                    if (isNplKolek(bCust.kolek)) {
                        nplLunasList.add(bCustWithBln)
                    } else {
                        lunasList.add(bCustWithBln)
                    }
                }
            }
        }
        }

        val refDateStr = when (dashboardPeriodSource) {
            "BULAN_LALU" -> dateBln
            "DAILY" -> dateDaily
            else -> dateAkhir
        }

        val activeList = when (dashboardPeriodSource) {
            "BULAN_LALU" -> filterByMantri(if (bulanLaluCustomers.isNotEmpty()) bulanLaluCustomers else currentCustomers)
            "DAILY" -> filterByMantri(if (dailyCustomers.isNotEmpty()) dailyCustomers else (if (akhirBulanCustomers.isNotEmpty()) akhirBulanCustomers else currentCustomers))
            else -> filterByMantri(if (akhirBulanCustomers.isNotEmpty()) akhirBulanCustomers else currentCustomers)
        }

        val tunggakanKurang6BlnList = activeList.filter { cust ->
            if (cust.osPinjaman <= 0.0) return@filter false
            val (_, rank) = parseKolekCodeAndRank(cust.kolek)
            val isTunggakan = (rank in 2..7) || cust.tunggakanPokok > 0.0 || cust.tunggakanBunga > 0.0 || cust.totalTunggakan > 0.0 || cust.umurTgk > 0
            if (!isTunggakan) return@filter false

            val rawTglReal = cust.tglReal.ifBlank {
                cust.extraColumns.split("||")
                    .firstOrNull { part -> part.startsWith("Tgl Real::") || part.startsWith("Tgl Realisasi::") || part.startsWith("TGL_REAL::") }
                    ?.substringAfter("::")?.trim() ?: ""
            }
            if (rawTglReal.isBlank()) return@filter false

            val months = getMonthsBetweenDates(rawTglReal, refDateStr)
            months != null && months in 0..5
        }

        val tukcilList = activeList.filter { cust ->
            if (cust.osPinjaman <= 0.0) return@filter false
            val dailyCust = dailyDataMap[cust.noRekening.trim()]
                ?: (if (cust.noRekening.isNotBlank()) dailyDataMap[cust.noRekening.filter { it.isDigit() }] else null)
            val effectiveTunggakan = when {
                dailyCust != null && dailyCust.totalTunggakan > 0.0 -> dailyCust.totalTunggakan
                cust.totalTunggakan > 0.0 -> cust.totalTunggakan
                dailyCust != null && (dailyCust.tunggakanPokok + dailyCust.tunggakanBunga) > 0.0 -> dailyCust.tunggakanPokok + dailyCust.tunggakanBunga
                (cust.tunggakanPokok + cust.tunggakanBunga) > 0.0 -> cust.tunggakanPokok + cust.tunggakanBunga
                else -> 0.0
            }
            effectiveTunggakan in 0.01..100000.0
        }

        val currentYearMonth = java.time.YearMonth.now()
        val dhNewList = filterByMantri(lw325DailyCustomers).filter { cust ->
            val tglPh = extractTglPhFromExtraColumns(cust.extraColumns)
            isDateInYearMonth(tglPh, currentYearMonth)
        }

        TransitionGroupResult(downgradeMap, upgradeMap, lunasList, nplLunasList, tunggakanKurang6BlnList, tukcilList, dhNewList)
        }
    }

    val downgradeData = transitionResult.downgradeMap
    val upgradeData = transitionResult.upgradeMap
    val lunasData = transitionResult.lunasList
    val nplLunasData = transitionResult.nplLunasList
    val tunggakanKurang6BlnData = transitionResult.tunggakanKurang6BlnList
    val tukcilData = transitionResult.tukcilList
    val dhNewData = transitionResult.dhNewList

    val totalDowngradeOs = remember(downgradeData) {
        downgradeData.values.flatten().sumOf { it.osPinjaman }
    }
    val totalDowngradeDeb = remember(downgradeData) {
        downgradeData.values.flatten().size
    }

    val totalUpgradeOs = remember(upgradeData) {
        upgradeData.values.flatten().sumOf { it.osPinjaman }
    }
    val totalUpgradeDeb = remember(upgradeData) {
        upgradeData.values.flatten().size
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header / Dropdown Mantri
            DashboardMantriDropdown(
                selectedValue = filterMantri,
                options = mantriOptions,
                onSelect = { selectedName ->
                    viewModel.filterMantriDashboard.value = selectedName
                }
            )
            // Pilihan Periode Dashboard: Bulan Lalu, Akhir Bulan, Daily Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PeriodPill(
                        title = "Bulan Lalu",
                        date = dateBln,
                        isSelected = dashboardPeriodSource == "BULAN_LALU",
                        onClick = { viewModel.selectDashboardPeriodSource("BULAN_LALU") },
                        modifier = Modifier.weight(1f)
                    )
                    PeriodPill(
                        title = "Akhir Bulan",
                        date = dateAkhir,
                        isSelected = dashboardPeriodSource == "AKHIR_BULAN",
                        onClick = { viewModel.selectDashboardPeriodSource("AKHIR_BULAN") },
                        modifier = Modifier.weight(1f)
                    )
                    PeriodPill(
                        title = "Daily",
                        date = dateDaily,
                        isSelected = dashboardPeriodSource == "DAILY",
                        onClick = { viewModel.selectDashboardPeriodSource("DAILY") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Scrollable container for everything below period buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

            // 2. Primary KPI Metric Card: Total OS
            DashboardTotalOsCard(
                title = if (filterMantri != "SEMUA") "Total OS (${cleanMantriNameOnly(filterMantri)})" else "Total OS",
                osValue = activeSummary.os,
                debtorCount = activeSummary.debtors,
                momValue = momOs,
                mtdValue = mtdOs,
                formatter = formatter,
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Lancar, SML, NPL (1 Baris, 3 Kolom)
            val lancarPct = if (activeSummary.os > 0) (activeKolekSummary.lancar.os / activeSummary.os) * 100 else 0.0
            val smlPct = if (activeSummary.os > 0) (activeKolekSummary.sml.os / activeSummary.os) * 100 else 0.0
            val nplPct = if (activeSummary.os > 0) (activeKolekSummary.npl.os / activeSummary.os) * 100 else 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardColumnKpiCard(
                    title = "Lancar",
                    osValue = activeKolekSummary.lancar.os,
                    debtorCount = activeKolekSummary.lancar.debtors,
                    percentage = lancarPct,
                    momValue = momLancar,
                    mtdValue = mtdLancar,
                    formatter = formatter,
                    accentColor = Color(0xFF2E7D32),
                    bgColor = Color(0xFFE8F5E9),
                    icon = Icons.Default.CheckCircle,
                    isInverseDelta = false,
                    modifier = Modifier.weight(1f)
                )

                DashboardColumnKpiCard(
                    title = "SML",
                    osValue = activeKolekSummary.sml.os,
                    debtorCount = activeKolekSummary.sml.debtors,
                    percentage = smlPct,
                    momValue = momSml,
                    mtdValue = mtdSml,
                    formatter = formatter,
                    accentColor = Color(0xFFE65100),
                    bgColor = Color(0xFFFFF3E0),
                    icon = Icons.Default.Warning,
                    isInverseDelta = true,
                    modifier = Modifier.weight(1f)
                )

                DashboardColumnKpiCard(
                    title = "NPL",
                    osValue = activeKolekSummary.npl.os,
                    debtorCount = activeKolekSummary.npl.debtors,
                    percentage = nplPct,
                    momValue = momNpl,
                    mtdValue = mtdNpl,
                    formatter = formatter,
                    accentColor = Color(0xFFC62828),
                    bgColor = Color(0xFFFFEBEE),
                    icon = Icons.Default.Error,
                    isInverseDelta = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // 4. Rincian Kolektabilitas (SML1,2,3, KL, D, M) - Dinamis & Bisa Dipilih untuk Popup Tabel
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rincian Kolektabilitas",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF00386B)
                    )
                    Text(
                        text = "Klik kartu untuk melihat tabel nasabah",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 9.sp,
                            color = Color(0xFF64748B)
                        )
                    )
                }

                // Baris 1: SML1, SML2, SML3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KolekDetailMiniCard(
                        title = "SML 1",
                        osValue = sml1Os,
                        debtorCount = sml1Deb,
                        accentColor = Color(0xFFE65100),
                        bgColor = Color(0xFFFFF8E1),
                        formatter = formatter,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            popupDialogTitle = "Daftar Nasabah Kolektabilitas SML 1"
                            popupDialogCustomers = sml1List
                            showCustomerPopup = true
                        }
                    )
                    KolekDetailMiniCard(
                        title = "SML 2",
                        osValue = sml2Os,
                        debtorCount = sml2Deb,
                        accentColor = Color(0xFFD84315),
                        bgColor = Color(0xFFFFF3E0),
                        formatter = formatter,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            popupDialogTitle = "Daftar Nasabah Kolektabilitas SML 2"
                            popupDialogCustomers = sml2List
                            showCustomerPopup = true
                        }
                    )
                    KolekDetailMiniCard(
                        title = "SML 3",
                        osValue = sml3Os,
                        debtorCount = sml3Deb,
                        accentColor = Color(0xFFBF360C),
                        bgColor = Color(0xFFFBE9E7),
                        formatter = formatter,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            popupDialogTitle = "Daftar Nasabah Kolektabilitas SML 3"
                            popupDialogCustomers = sml3List
                            showCustomerPopup = true
                        }
                    )
                }

                // Baris 2: Kurang Lancar, Diragukan, Macet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KolekDetailMiniCard(
                        title = "Kurang Lancar",
                        osValue = klOs,
                        debtorCount = klDeb,
                        accentColor = Color(0xFFC62828),
                        bgColor = Color(0xFFFFEBEE),
                        formatter = formatter,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            popupDialogTitle = "Daftar Nasabah Kolektabilitas Kurang Lancar"
                            popupDialogCustomers = klList
                            showCustomerPopup = true
                        }
                    )
                    KolekDetailMiniCard(
                        title = "Diragukan",
                        osValue = dOs,
                        debtorCount = dDeb,
                        accentColor = Color(0xFFB71C1C),
                        bgColor = Color(0xFFFFEBEE),
                        formatter = formatter,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            popupDialogTitle = "Daftar Nasabah Kolektabilitas Diragukan"
                            popupDialogCustomers = dList
                            showCustomerPopup = true
                        }
                    )
                    KolekDetailMiniCard(
                        title = "Macet",
                        osValue = mOs,
                        debtorCount = mDeb,
                        accentColor = Color(0xFF880E4F),
                        bgColor = Color(0xFFFCE4EC),
                        formatter = formatter,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            popupDialogTitle = "Daftar Nasabah Kolektabilitas Macet"
                            popupDialogCustomers = mList
                            showCustomerPopup = true
                        }
                    )
                }
            }

            // 5. Rincian Segmen Kredit (Collapsible / Bisa Disembunyikan)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSegmenExpanded = !isSegmenExpanded }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                text = "Rincian Segmen Kredit",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                ),
                                color = Color(0xFF00386B)
                            )
                        }

                        Icon(
                            imageVector = if (isSegmenExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isSegmenExpanded) "Sembunyikan" else "Tampilkan",
                            tint = Color(0xFF005FAF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isSegmenExpanded,
                        enter = expandVertically(
                            expandFrom = Alignment.Top,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                        ),
                        exit = shrinkVertically(
                            shrinkTowards = Alignment.Top,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)

                            val chunkedSegments = segmenGroups.chunked(3)
                            if (chunkedSegments.isEmpty()) {
                                Text(
                                    text = "Tidak ada data segmen untuk periode ini",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            } else {
                                chunkedSegments.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowItems.forEach { (segName, segOs, segList) ->
                                            KolekDetailMiniCard(
                                                title = segName,
                                                osValue = segOs,
                                                debtorCount = segList.size,
                                                accentColor = Color(0xFF005FAF),
                                                bgColor = Color(0xFFF0F7FF),
                                                formatter = formatter,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    popupDialogTitle = "Daftar Nasabah Segmen $segName"
                                                    popupDialogCustomers = segList
                                                    showCustomerPopup = true
                                                }
                                            )
                                        }
                                        // Pad empty space if chunk has fewer than 3 items
                                        repeat(3 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }            // 6. Kolek Downgrade (Pemburukan Kolek) - Collapsible
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFFFCDD2))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDowngradeExpanded = !isDowngradeExpanded }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Kolek Downgrade (Pemburukan)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = Color(0xFFC62828)
                                )
                                Text(
                                    text = "Total : Rp ${formatDouble(totalDowngradeOs, formatter)} | $totalDowngradeDeb Deb",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isDowngradeExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isDowngradeExpanded) "Sembunyikan" else "Tampilkan",
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isDowngradeExpanded,
                        enter = expandVertically(
                            expandFrom = Alignment.Top,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                        ),
                        exit = shrinkVertically(
                            shrinkTowards = Alignment.Top,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HorizontalDivider(color = Color(0xFFFFEBEE), thickness = 0.8.dp)

                            val downgradeOrder = listOf(
                                "L ➔ SML1", "L ➔ SML2", "L ➔ SML3", "L ➔ KL", "L ➔ D", "L ➔ M",
                                "SML1 ➔ SML2", "SML1 ➔ SML3", "SML1 ➔ KL", "SML1 ➔ D", "SML1 ➔ M",
                                "SML2 ➔ SML3", "SML2 ➔ KL", "SML2 ➔ D", "SML2 ➔ M",
                                "SML3 ➔ KL", "SML3 ➔ D", "SML3 ➔ M",
                                "KL ➔ D", "KL ➔ M",
                                "D ➔ M"
                            )
                            val activeDowngrades = remember(downgradeData) {
                                val present = downgradeData.entries.filter { it.value.isNotEmpty() }.associate { it.key to it.value }
                                val sorted = mutableListOf<Map.Entry<String, List<CustomerEntity>>>()
                                downgradeOrder.forEach { k ->
                                    if (present.containsKey(k)) {
                                        sorted.add(java.util.AbstractMap.SimpleEntry(k, present[k]!!))
                                    }
                                }
                                present.forEach { (k, v) ->
                                    if (!downgradeOrder.contains(k)) {
                                        sorted.add(java.util.AbstractMap.SimpleEntry(k, v))
                                    }
                                }
                                sorted
                            }

                            if (activeDowngrades.isEmpty() && lunasData.isEmpty() && nplLunasData.isEmpty() && tunggakanKurang6BlnData.isEmpty() && tukcilData.isEmpty() && dhNewData.isEmpty()) {
                                Text(
                                    text = "Tidak ada pemburukan kolek pada periode ini",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                activeDowngrades.chunked(3).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowItems.forEach { (key, list) ->
                                            KolekDetailMiniCard(
                                                title = key,
                                                osValue = list.sumOf { it.osPinjaman },
                                                debtorCount = list.size,
                                                accentColor = Color(0xFFC62828),
                                                bgColor = Color(0xFFFFEBEE),
                                                formatter = formatter,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    popupDialogTitle = "Daftar Nasabah Downgrade: $key"
                                                    popupDialogCustomers = list
                                                    showCustomerPopup = true
                                                }
                                            )
                                        }
                                        repeat(3 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }

                                if (lunasData.isNotEmpty() || nplLunasData.isNotEmpty() || tunggakanKurang6BlnData.isNotEmpty() || tukcilData.isNotEmpty() || dhNewData.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    HorizontalDivider(color = Color(0xFFFFCDD2), thickness = 0.8.dp)
                                    Text(
                                        text = "Info Nasabah Selesai / Run-Off & Tunggakan Baru:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFB71C1C)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        KolekDetailMiniCard(
                                            title = "Nasabah Lunas",
                                            osValue = lunasData.sumOf { it.osPinjaman },
                                            debtorCount = lunasData.size,
                                            accentColor = Color(0xFF00796B),
                                            bgColor = Color(0xFFE0F2F1),
                                            formatter = formatter,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("card_nasabah_lunas"),
                                            onClick = {
                                                popupDialogTitle = "Daftar Nasabah Lunas (${lunasData.size} Deb)"
                                                popupDialogCustomers = lunasData
                                                showCustomerPopup = true
                                            }
                                        )
                                        KolekDetailMiniCard(
                                            title = "Nasabah NPL Lunas",
                                            osValue = nplLunasData.sumOf { it.osPinjaman },
                                            debtorCount = nplLunasData.size,
                                            accentColor = Color(0xFF6A1B9A),
                                            bgColor = Color(0xFFF3E5F5),
                                            formatter = formatter,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("card_nasabah_npl_lunas"),
                                            onClick = {
                                                popupDialogTitle = "Daftar Nasabah NPL Lunas (${nplLunasData.size} Deb)"
                                                popupDialogCustomers = nplLunasData
                                                showCustomerPopup = true
                                            }
                                        )
                                        KolekDetailMiniCard(
                                            title = "Tunggakan < 6 bln",
                                            osValue = tunggakanKurang6BlnData.sumOf { it.osPinjaman },
                                            debtorCount = tunggakanKurang6BlnData.size,
                                            accentColor = Color(0xFFE65100),
                                            bgColor = Color(0xFFFFF3E0),
                                            formatter = formatter,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("card_tunggakan_kurang_6bln"),
                                            onClick = {
                                                popupDialogTitle = "Daftar Tunggakan < 6 Bulan (${tunggakanKurang6BlnData.size} Deb)"
                                                popupDialogCustomers = tunggakanKurang6BlnData
                                                showCustomerPopup = true
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        KolekDetailMiniCard(
                                            title = "Tukcil <100 rb",
                                            osValue = tukcilData.sumOf { it.osPinjaman },
                                            debtorCount = tukcilData.size,
                                            accentColor = Color(0xFF0288D1),
                                            bgColor = Color(0xFFE1F5FE),
                                            formatter = formatter,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("card_tukcil"),
                                            onClick = {
                                                popupDialogTitle = "Daftar Nasabah Tukcil <100 rb (${tukcilData.size} Deb)"
                                                popupDialogCustomers = tukcilData
                                                showCustomerPopup = true
                                            }
                                        )
                                        KolekDetailMiniCard(
                                            title = "Nasabah DH New",
                                            osValue = dhNewData.sumOf { it.osPinjaman },
                                            debtorCount = dhNewData.size,
                                            accentColor = Color(0xFFB71C1C),
                                            bgColor = Color(0xFFFFEBEE),
                                            formatter = formatter,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("card_nasabah_dh_new"),
                                            onClick = {
                                                popupDialogTitle = "Daftar Nasabah DH New (${dhNewData.size} Deb)"
                                                popupDialogCustomers = dhNewData
                                                showCustomerPopup = true
                                            }
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. Kolek Upgrade (Perbaikan Kolek) - Collapsible
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFC8E6C9))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isUpgradeExpanded = !isUpgradeExpanded }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Kolek Upgrade (Perbaikan)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "Total : Rp ${formatDouble(totalUpgradeOs, formatter)} | $totalUpgradeDeb Deb",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isUpgradeExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isUpgradeExpanded) "Sembunyikan" else "Tampilkan",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isUpgradeExpanded,
                        enter = expandVertically(
                            expandFrom = Alignment.Top,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                        ),
                        exit = shrinkVertically(
                            shrinkTowards = Alignment.Top,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HorizontalDivider(color = Color(0xFFE8F5E9), thickness = 0.8.dp)

                            val upgradeOrder = listOf(
                                "M ➔ D", "M ➔ KL", "M ➔ SML3", "M ➔ SML2", "M ➔ SML1", "M ➔ L",
                                "D ➔ KL", "D ➔ SML3", "D ➔ SML2", "D ➔ SML1", "D ➔ L",
                                "KL ➔ SML3", "KL ➔ SML2", "KL ➔ SML1", "KL ➔ L",
                                "SML3 ➔ SML2", "SML3 ➔ SML1", "SML3 ➔ L",
                                "SML2 ➔ SML1", "SML2 ➔ L",
                                "SML1 ➔ L"
                            )
                            val activeUpgrades = remember(upgradeData) {
                                val present = upgradeData.entries.filter { it.value.isNotEmpty() }.associate { it.key to it.value }
                                val sorted = mutableListOf<Map.Entry<String, List<CustomerEntity>>>()
                                upgradeOrder.forEach { k ->
                                    if (present.containsKey(k)) {
                                        sorted.add(java.util.AbstractMap.SimpleEntry(k, present[k]!!))
                                    }
                                }
                                present.forEach { (k, v) ->
                                    if (!upgradeOrder.contains(k)) {
                                        sorted.add(java.util.AbstractMap.SimpleEntry(k, v))
                                    }
                                }
                                sorted
                            }

                            if (activeUpgrades.isEmpty()) {
                                Text(
                                    text = "Tidak ada perbaikan kolek pada periode ini",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                activeUpgrades.chunked(3).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowItems.forEach { (key, list) ->
                                            KolekDetailMiniCard(
                                                title = key,
                                                osValue = list.sumOf { it.osPinjaman },
                                                debtorCount = list.size,
                                                accentColor = Color(0xFF2E7D32),
                                                bgColor = Color(0xFFE8F5E9),
                                                formatter = formatter,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    popupDialogTitle = "Daftar Nasabah Upgrade: $key"
                                                    popupDialogCustomers = list
                                                    showCustomerPopup = true
                                                }
                                            )
                                        }
                                        repeat(3 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }

                                // Rekapan Total Perpindahan Kolek Upgrade (Dinamis: yg 0 tidak ditampilkan)
                                // Dijumlahkan langsung dari semua info upgrade yang tampil sesuai kategorinya:
                                // - Bergeser ke Lancar: semua kartu yg bergeser menjadi kolek lancar (berakhir dengan ➔ L)
                                // - Bergeser ke SML: dari NPL(KL,D,M) ke SML, dari SML3 ke 2 atau ke 1, dari sml2 ke 1
                                // - NPL Membaik: semua kartu nasabah NPL (KL, D, M) yg bergeser jadi membaik
                                val recapUpgradeItems = remember(activeUpgrades) {
                                    val list = mutableListOf<Triple<String, Double, List<CustomerEntity>>>()

                                    // 1. Bergeser ke Lancar (semua kartu upgrade yg bergeser menjadi Lancar: berakhir dengan ➔ L)
                                    val keL = activeUpgrades.filter { entry ->
                                        val upper = entry.key.trim().uppercase()
                                        val parts = upper.split(Regex("\\s*(➔|->)\\s*"))
                                        val toK = if (parts.size >= 2) parts[1].trim() else ""
                                        toK == "L" || toK == "LANCAR" || upper.endsWith("➔ L") || upper.endsWith("-> L")
                                    }.flatMap { it.value }
                                    if (keL.isNotEmpty()) {
                                        list.add(Triple("Bergeser ke Lancar", keL.sumOf { it.osPinjaman }, keL))
                                    }

                                    // 2. Bergeser ke SML (semua kartu upgrade yg tujuannya SML: ➔ SML1, ➔ SML2, ➔ SML3)
                                    val keSml = activeUpgrades.filter { entry ->
                                        val upper = entry.key.trim().uppercase()
                                        val parts = upper.split(Regex("\\s*(➔|->)\\s*"))
                                        val toK = if (parts.size >= 2) parts[1].trim() else ""
                                        toK.startsWith("SML") || toK.startsWith("DPK")
                                    }.flatMap { it.value }
                                    if (keSml.isNotEmpty()) {
                                        list.add(Triple("Bergeser ke SML", keSml.sumOf { it.osPinjaman }, keSml))
                                    }

                                    // 3. NPL Membaik: semua kartu dari NPL (KL, D, M) yg bergeser jadi membaik
                                    val nplMembaik = activeUpgrades.filter { entry ->
                                        val upper = entry.key.trim().uppercase()
                                        val parts = upper.split(Regex("\\s*(➔|->)\\s*"))
                                        if (parts.size >= 2) {
                                            val fromK = parts[0].trim()
                                            val toK = parts[1].trim()
                                            val isFromNpl = fromK in listOf("KL", "D", "M")
                                            val rankFrom = parseKolekCodeAndRank(fromK).second
                                            val rankTo = parseKolekCodeAndRank(toK).second
                                            isFromNpl && rankTo < rankFrom
                                        } else false
                                    }.flatMap { it.value }
                                    if (nplMembaik.isNotEmpty()) {
                                        list.add(Triple("NPL Membaik", nplMembaik.sumOf { it.osPinjaman }, nplMembaik))
                                    }

                                    list
                                }

                                if (recapUpgradeItems.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    HorizontalDivider(color = Color(0xFFC8E6C9), thickness = 0.8.dp)
                                    Text(
                                        text = "Rekapan Total Perpindahan:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF1B5E20)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        recapUpgradeItems.forEach { (label, os, custList) ->
                                            KolekDetailMiniCard(
                                                title = label,
                                                osValue = os,
                                                debtorCount = custList.size,
                                                accentColor = Color(0xFF1565C0),
                                                bgColor = Color(0xFFE3F2FD),
                                                formatter = formatter,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    popupDialogTitle = "Daftar Nasabah $label (${custList.size} Deb)"
                                                    popupDialogCustomers = custList
                                                    showCustomerPopup = true
                                                }
                                            )
                                        }
                                        repeat(3 - recapUpgradeItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }



            // 8. Detail Kinerja Per Mantri
            val displayedMantris = remember(allMantriNames, filterMantri) {
                if (filterMantri != "SEMUA") {
                    val cleanSelected = cleanMantriNameOnly(filterMantri)
                    allMantriNames.filter { cleanMantriNameOnly(it).equals(cleanSelected, ignoreCase = true) }
                } else {
                    allMantriNames
                }
            }

            val currentReportDate = when (dashboardPeriodSource) {
                "BULAN_LALU" -> dateBln
                "DAILY" -> dateDaily
                else -> dateAkhir
            }
            val (reportM, reportY) = remember(currentReportDate) {
                extractMonthAndYear(currentReportDate)
            }

            // Pre-calculate realisasi & net disburs per mantri asynchronously for instant dashboard render
            val mantriNetDisbursMap by produceState(
                initialValue = emptyMap<String, List<RealizedCustomerItem>>(),
                activePeriodCustomers,
                bulanLaluCustomers,
                akhirBulanCustomers,
                dashboardPeriodSource,
                reportM,
                reportY
            ) {
                value = withContext(Dispatchers.Default) {
                // Determine baseline dataset based on active period
                val baselineCustomers = when (dashboardPeriodSource) {
                    "DAILY" -> if (akhirBulanCustomers.isNotEmpty()) akhirBulanCustomers else bulanLaluCustomers
                    "AKHIR_BULAN" -> bulanLaluCustomers
                    "BULAN_LALU" -> emptyList<CustomerEntity>()
                    else -> emptyList()
                }

                // Map previous month for fast lookup
                val blnLookup = mutableMapOf<String, CustomerEntity>()
                val blnNameMap = mutableMapOf<String, CustomerEntity>()
                val blnNameCount = mutableMapOf<String, Int>()

                fun indexCustomer(bCust: CustomerEntity, overwrite: Boolean = false) {
                    val rawRek = bCust.noRekening.trim()
                    if (rawRek.isNotBlank() && (overwrite || !blnLookup.containsKey(rawRek))) blnLookup[rawRek] = bCust
                    val cr = rawRek.filter { it.isDigit() }
                    if (cr.isNotBlank()) {
                        if (overwrite || !blnLookup.containsKey(cr)) blnLookup[cr] = bCust
                        val noZero = cr.trimStart('0')
                        if (noZero.isNotBlank() && (overwrite || !blnLookup.containsKey(noZero))) blnLookup[noZero] = bCust
                        if (cr.length >= 15 && (overwrite || !blnLookup.containsKey(cr.takeLast(15)))) blnLookup[cr.takeLast(15)] = bCust
                    }
                    val formattedRek = com.example.data.network.SpreadsheetService.formatRekening15(bCust.noRekening)
                    if (formattedRek.isNotBlank() && (overwrite || !blnLookup.containsKey(formattedRek))) blnLookup[formattedRek] = bCust
                    val rawCif = bCust.cif.trim()
                    if (rawCif.isNotBlank() && (overwrite || !blnLookup.containsKey(rawCif))) blnLookup[rawCif] = bCust
                    val cc = rawCif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
                    if (cc.isNotBlank()) {
                        if (overwrite || !blnLookup.containsKey(cc)) blnLookup[cc] = bCust
                        if (overwrite || !blnLookup.containsKey("CIF-$cc")) blnLookup["CIF-$cc"] = bCust
                    }
                    val cleanName = bCust.namaNasabah.trim().lowercase()
                    val cleanMantri = cleanMantriNameOnly(bCust.namaMantri).lowercase()
                    if (cleanName.isNotBlank() && cleanMantri.isNotBlank()) {
                        val key = "$cleanName|$cleanMantri"
                        if (overwrite || !blnLookup.containsKey(key)) blnLookup[key] = bCust
                    }
                    if (cleanName.isNotBlank()) {
                        blnNameMap[cleanName] = bCust
                        blnNameCount[cleanName] = (blnNameCount[cleanName] ?: 0) + 1
                    }
                }

                // Index baseline customers
                baselineCustomers.forEach { indexCustomer(it, overwrite = true) }

                fun findBlnCust(cust: CustomerEntity): CustomerEntity? {
                    if (baselineCustomers.isEmpty()) return null
                    val rawRek = cust.noRekening.trim()
                    val cr = rawRek.filter { it.isDigit() }
                    val noZero = cr.trimStart('0')
                    val formattedRek = com.example.data.network.SpreadsheetService.formatRekening15(cust.noRekening)
                    val rawCif = cust.cif.trim()
                    val cc = rawCif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
                    val cleanName = cust.namaNasabah.trim().lowercase()
                    val cleanMantri = cleanMantriNameOnly(cust.namaMantri).lowercase()

                    val match = blnLookup[rawRek]
                        ?: (if (cr.isNotBlank()) blnLookup[cr] else null)
                        ?: (if (noZero.isNotBlank()) blnLookup[noZero] else null)
                        ?: (if (cr.length >= 15) blnLookup[cr.takeLast(15)] else null)
                        ?: (if (formattedRek.isNotBlank()) blnLookup[formattedRek] else null)
                        ?: (if (rawCif.isNotBlank()) blnLookup[rawCif] else null)
                        ?: (if (cc.isNotBlank()) blnLookup[cc] ?: blnLookup["CIF-$cc"] else null)
                        ?: (if (cleanName.isNotBlank() && cleanMantri.isNotBlank()) blnLookup["$cleanName|$cleanMantri"] else null)
                        ?: (if (cleanName.isNotBlank() && blnNameCount[cleanName] == 1) blnNameMap[cleanName] else null)

                    // Safety guard: If baseline contains identical newly-realized record (e.g. from synthetic fallback)
                    if (match != null && match.noRekening.isNotBlank() && match.noRekening == cust.noRekening && match.tglReal == cust.tglReal) {
                        return null
                    }
                    return match
                }

                val map = mutableMapOf<String, MutableList<RealizedCustomerItem>>()
                val inReportMonth = activePeriodCustomers.filter { cust ->
                    if (cust.tglReal.isBlank()) false
                    else {
                        val (m, y) = extractMonthAndYear(cust.tglReal)
                        if (reportM.isNotBlank() && reportY.isNotBlank()) {
                            m == reportM && y == reportY
                        } else if (reportM.isNotBlank()) {
                            m == reportM
                        } else false
                    }
                }

                inReportMonth.forEach { cust ->
                    val mName = cleanMantriNameOnly(cust.namaMantri)
                    val pName = cleanMantriNameOnly(cust.namaMantriPrakarsa)
                    val targetName = if (mName.isNotBlank() && mName != "TANPA NAMA") mName else if (pName.isNotBlank() && pName != "TANPA NAMA") pName else "Blank PN"
                    if (targetName.isNotBlank()) {
                        val prevCust = findBlnCust(cust)
                        val isNasabahBaru = (prevCust == null)
                        val osBlnLalu = prevCust?.osPinjaman ?: 0.0
                        val netDisburs = if (isNasabahBaru) cust.plafond else (cust.plafond - osBlnLalu)

                        val item = RealizedCustomerItem(
                            customer = cust,
                            plafondBaru = cust.plafond,
                            osBlnLalu = osBlnLalu,
                            netDisburs = netDisburs,
                            isNasabahBaru = isNasabahBaru
                        )
                        map.getOrPut(targetName) { mutableListOf() }.add(item)
                    }
                }
                map
                }
            }

            val mantriRealisasiMap = remember(mantriNetDisbursMap) {
                val map = mutableMapOf<String, Pair<Double, Int>>()
                mantriNetDisbursMap.forEach { (targetName, items) ->
                    map[targetName] = items.sumOf { it.plafondBaru } to items.size
                }
                map
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = Color(0xFF005FAF),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (filterMantri != "SEMUA") "Rincian Posisi: ${cleanMantriNameOnly(filterMantri)}" else "Rincian Posisi Per Mantri (${displayedMantris.size} Mantri)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00386B)
                        )
                    }

                    if (filterMantri != "SEMUA") {
                        TextButton(onClick = { viewModel.filterMantriDashboard.value = "SEMUA" }) {
                            Text("Semua Mantri", fontSize = 12.sp, color = Color(0xFF005FAF), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (displayedMantris.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (filterMantri != "SEMUA") "Tidak ada data untuk mantri ${cleanMantriNameOnly(filterMantri)}" else "Belum ada data mantri. Silakan sinkronkan spreadsheet data.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    displayedMantris.forEach { mantriName ->
                        val bln = findMantriData(mantriSummaryBlnLalu, mantriName) ?: MantriDetailData()
                        val akhir = findMantriData(mantriSummaryAkhirBln, mantriName) ?: MantriDetailData()
                        val daily = findMantriData(mantriSummaryDaily, mantriName) ?: MantriDetailData()

                        val cleanTarget = cleanMantriNameOnly(mantriName)
                        val realisasiPair = mantriRealisasiMap[cleanTarget]
                            ?: mantriRealisasiMap.entries.firstOrNull { it.key.equals(cleanTarget, ignoreCase = true) }?.value
                            ?: (0.0 to 0)

                        val realizedItems = mantriNetDisbursMap[cleanTarget]
                            ?: mantriNetDisbursMap.entries.firstOrNull { it.key.equals(cleanTarget, ignoreCase = true) }?.value
                            ?: emptyList()

                        MantriPositionCard(
                            mantriName = mantriName,
                            bln = bln,
                            akhir = akhir,
                            daily = daily,
                            realisasiPlafond = realisasiPair.first,
                            realisasiDeb = realisasiPair.second,
                            realizedItems = realizedItems,
                            onShowRealizedDialog = { name, items ->
                                realizedPopupMantriName = name
                                realizedPopupItems = items
                                showRealizedPopup = true
                            },
                            dateBlnLalu = dateBln,
                            dateAkhirBulan = dateAkhir,
                            dateDaily = dateDaily,
                            activePeriodSource = dashboardPeriodSource,
                            formatter = formatter,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
    }

    // Popup Tabel Nasabah (Tabel sama seperti pada Menu Monitoring)
    if (showCustomerPopup) {
        CustomerTablePopupDialog(
            title = popupDialogTitle,
            customers = popupDialogCustomers,
            onDismiss = { showCustomerPopup = false },
            onCustomerClick = onCustomerClick,
            formatter = formatter,
            dailyDataMap = dailyDataMap
        )
    }

    // Popup Tabel Nasabah Realisasi (Net Disburs)
    if (showRealizedPopup) {
        RealizedCustomersPopupDialog(
            mantriName = realizedPopupMantriName,
            items = realizedPopupItems,
            onDismiss = { showRealizedPopup = false },
            formatter = formatter,
            onCustomerClick = onCustomerClick
        )
    }
}

/**
 * Pembersih nama mantri pengelola agar tidak menampilkan No PN di depannya.
 */
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

fun extractMonthAndYear(dateStr: String): Pair<String, String> {
    val s = dateStr.trim()
    if (s.isBlank()) return "" to ""

    // 1. Check for month names (Indonesian or English)
    val upper = s.uppercase()
    val monthList = listOf(
        listOf("JANUARI", "JANUARY", "JAN") to "01",
        listOf("FEBRUARI", "FEBRUARY", "FEB") to "02",
        listOf("MARET", "MARCH", "MAR") to "03",
        listOf("APRIL", "APR") to "04",
        listOf("MEI", "MAY") to "05",
        listOf("JUNI", "JUNE", "JUN") to "06",
        listOf("JULI", "JULY", "JUL") to "07",
        listOf("AGUSTUS", "AUGUST", "AGU", "AGS", "AGT", "AUG") to "08",
        listOf("SEPTEMBER", "SEPT", "SEP") to "09",
        listOf("OKTOBER", "OCTOBER", "OKT", "OCT") to "10",
        listOf("NOVEMBER", "NOV", "NOP") to "11",
        listOf("DESEMBER", "DECEMBER", "DES", "DEC") to "12"
    )

    for ((keywords, monthNum) in monthList) {
        if (keywords.any { upper.contains(it) }) {
            val year4Match = Regex("\\b(20\\d{2})\\b").find(s)
            val year = if (year4Match != null) {
                year4Match.value
            } else {
                val year2Match = Regex("\\b(\\d{2})\\b").findAll(s).map { it.value }.lastOrNull { it != monthNum }
                if (year2Match != null) "20$year2Match" else ""
            }
            return monthNum to year
        }
    }

    // 2. Numeric formats with delimiters: /, -, ., space
    val parts = s.replace(Regex("[^0-9]+"), " ").trim().split(" ").filter { it.isNotBlank() }
    if (parts.size >= 3) {
        if (parts[0].length == 4) {
            val y = parts[0]
            val m = parts[1].padStart(2, '0')
            return m to y
        } else {
            val p0 = parts[0].toIntOrNull()
            val p1 = parts[1].toIntOrNull()
            val mNum = if (p0 != null && p1 != null && p1 > 12 && p0 in 1..12) p0 else p1
            val m = (mNum?.toString() ?: parts[1]).padStart(2, '0')
            var y = parts[2]
            if (y.length == 2) y = "20$y"
            return m to y
        }
    } else if (parts.size == 2) {
        if (parts[0].length == 4) {
            return parts[1].padStart(2, '0') to parts[0]
        } else {
            val m = parts[0].padStart(2, '0')
            var y = parts[1]
            if (y.length == 2) y = "20$y"
            return m to y
        }
    }

    // 3. Compact numeric format (yyyyMMdd or ddMMyyyy)
    if (s.length == 8 && s.all { it.isDigit() }) {
        if (s.startsWith("20")) {
            val y = s.substring(0, 4)
            val m = s.substring(4, 6)
            return m to y
        } else {
            val m = s.substring(2, 4)
            val y = s.substring(4, 8)
            return m to y
        }
    }

    // 4. Excel date serial number fallback
    val serial = s.toDoubleOrNull()
    if (serial != null && serial in 35000.0..55000.0) {
        try {
            val date = java.time.LocalDate.of(1899, 12, 30).plusDays(serial.toLong())
            return String.format("%02d", date.monthValue) to date.year.toString()
        } catch (_: Exception) {}
    }

    return "" to ""
}


fun parseKolekCodeAndRank(k: String): Pair<String, Int> {
    val s = k.trim().uppercase()
    return when {
        s == "L" || s == "LANCAR" || s == "1" || (s.contains("LANCAR") && !s.contains("KURANG")) -> "L" to 1
        s == "SML1" || s == "SML 1" || s == "SML-1" || s == "2.1" || s.contains("DPK1") -> "SML1" to 2
        s == "SML2" || s == "SML 2" || s == "SML-2" || s == "2.2" || s.contains("DPK2") -> "SML2" to 3
        s == "SML3" || s == "SML 3" || s == "SML-3" || s == "2.3" || s.contains("DPK3") -> "SML3" to 4
        s == "SML" || s == "2" || s.contains("DPK") -> "SML1" to 2
        s == "KL" || s == "3" || s == "KURANG LANCAR" || (s.contains("KURANG") && s.contains("LANCAR")) -> "KL" to 5
        s == "D" || s == "4" || s == "DIRAGUKAN" || s.contains("DIRAGUKAN") -> "D" to 6
        s == "M" || s == "5" || s == "MACET" || s.contains("MACET") -> "M" to 7
        else -> "" to 99
    }
}

fun isNplKolek(kolek: String): Boolean {
    val (_, rank) = parseKolekCodeAndRank(kolek)
    return rank in 5..7 || kolek.trim().uppercase().contains("NPL")
}

fun extractTglPhFromExtraColumns(extraColumns: String): String {
    if (extraColumns.isBlank()) return ""
    return extraColumns.split("||")
        .firstOrNull { it.startsWith("Tgl PH::") }
        ?.substringAfter("Tgl PH::")
        ?.trim()
        ?: ""
}

fun isDateInYearMonth(
    dateStr: String,
    targetMonth: java.time.YearMonth = java.time.YearMonth.now()
): Boolean {
    val clean = dateStr.trim()
    if (clean.isBlank() || clean == "-") return false
    val (mStr, yStr) = extractMonthAndYear(clean)
    val month = mStr.toIntOrNull() ?: return false
    val year = yStr.toIntOrNull() ?: return false
    return month == targetMonth.monthValue && year == targetMonth.year
}

@Composable
fun DashboardMantriDropdown(
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val cleanSelected = cleanMantriNameOnly(selectedValue)
    val isFiltered = cleanSelected != "SEMUA"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFiltered) Color(0xFFFFF3E0) else Color(0xFFF1F5F9)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isFiltered) Color(0xFFE65100) else Color(0xFFCBD5E1)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isFiltered) Color(0xFFE65100) else Color(0xFF005FAF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isFiltered) cleanSelected else "Semua Mantri",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isFiltered) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = if (isFiltered) Color(0xFFBF360C) else Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isFiltered) {
                        IconButton(
                            onClick = { onSelect("SEMUA") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Reset filter",
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = if (isFiltered) Color(0xFFE65100) else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(Color.White)
                    .heightIn(max = 300.dp)
            ) {
                options.forEach { opt ->
                    val cleanOpt = cleanMantriNameOnly(opt)
                    val isOptSelected = cleanOpt == cleanSelected
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (cleanOpt == "SEMUA") "Semua Mantri" else cleanOpt,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isOptSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                ),
                                color = if (isOptSelected) Color(0xFF005FAF) else Color(0xFF1E293B)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (cleanOpt == "SEMUA") Icons.Default.People else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isOptSelected) Color(0xFF005FAF) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = if (isOptSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF005FAF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        onClick = {
                            onSelect(cleanOpt)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodPill(
    title: String,
    date: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFF005FAF) else Color(0xFFF1F5F9))
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) Color(0xFF00386B) else Color(0xFFCBD5E1),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) Color.White else Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardTotalOsCard(
    title: String,
    osValue: Double,
    debtorCount: Int,
    momValue: Double,
    mtdValue: Double,
    formatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF5FF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF005FAF).copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Row: Title on Left, Info Deb on Top Right (Icon orang tanpa kata Deb)
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
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color(0xFF005FAF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color(0xFF005FAF)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFEBEE),
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFFC62828).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "$debtorCount",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFC62828)
                        )
                    }
                }
            }

            // Middle: OS Value sejajar dengan info MoM dan MTD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rp ${formatDouble(osValue, formatter)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFF001C3B),
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MoM ${formatMomMtd(momValue)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = if (momValue > 0) Color(0xFF2E7D32) else if (momValue < 0) Color(0xFFC62828) else Color(0xFF64748B)
                    )
                    Text(
                        text = "MTD ${formatMomMtd(mtdValue)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = if (mtdValue > 0) Color(0xFF2E7D32) else if (mtdValue < 0) Color(0xFFC62828) else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardColumnKpiCard(
    title: String,
    osValue: Double,
    debtorCount: Int,
    percentage: Double? = null,
    momValue: Double,
    mtdValue: Double,
    formatter: NumberFormat,
    accentColor: Color,
    bgColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isInverseDelta: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header Row: Title & % on Left, Icon Orang + Debtor Count on Right (tanpa kata Deb)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        ),
                        color = accentColor,
                        maxLines = 1
                    )
                    if (percentage != null) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", percentage)}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.5.sp
                            ),
                            color = accentColor,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "$debtorCount",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp
                        ),
                        color = Color(0xFFC62828),
                        maxLines = 1
                    )
                }
            }

            // Middle: OS Value
            Text(
                text = "Rp ${formatDouble(osValue, formatter)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                ),
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Bottom: Info MoM dan MTD sejajar 1 baris
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val momCol = if (isInverseDelta) {
                    if (momValue > 0) Color(0xFFC62828) else if (momValue < 0) Color(0xFF2E7D32) else Color(0xFF64748B)
                } else {
                    if (momValue > 0) Color(0xFF2E7D32) else if (momValue < 0) Color(0xFFC62828) else Color(0xFF64748B)
                }
                val mtdCol = if (isInverseDelta) {
                    if (mtdValue > 0) Color(0xFFC62828) else if (mtdValue < 0) Color(0xFF2E7D32) else Color(0xFF64748B)
                } else {
                    if (mtdValue > 0) Color(0xFF2E7D32) else if (mtdValue < 0) Color(0xFFC62828) else Color(0xFF64748B)
                }

                Text(
                    text = "MoM ${formatMomMtd(momValue)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = momCol,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "MTD ${formatMomMtd(mtdValue)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = mtdCol,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun KolekDetailMiniCard(
    title: String,
    osValue: Double,
    debtorCount: Int,
    accentColor: Color,
    bgColor: Color,
    formatter: NumberFormat,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.5.sp
                    ),
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "$debtorCount",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp
                        ),
                        color = accentColor
                    )
                }
            }

            Text(
                text = "Rp ${formatDouble(osValue, formatter)}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp
                ),
                color = Color(0xFF1E293B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MantriPositionCard(
    mantriName: String,
    bln: MantriDetailData,
    akhir: MantriDetailData,
    daily: MantriDetailData,
    realisasiPlafond: Double,
    realisasiDeb: Int,
    realizedItems: List<RealizedCustomerItem> = emptyList(),
    onShowRealizedDialog: (String, List<RealizedCustomerItem>) -> Unit = { _, _ -> },
    dateBlnLalu: String,
    dateAkhirBulan: String,
    dateDaily: String,
    activePeriodSource: String,
    formatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val activeData = when (activePeriodSource) {
        "BULAN_LALU" -> bln
        "DAILY" -> daily
        else -> akhir
    }

    val displayOs = if (activeData.total.os > 0) activeData.total.os else if (daily.total.os > 0) daily.total.os else akhir.total.os
    val displayDebtors = if (activeData.total.debtors > 0) activeData.total.debtors else if (daily.total.debtors > 0) daily.total.debtors else akhir.total.debtors

    // Perhitungan MoM: Bulan Lalu dengan Akhir Bulan
    val momOs = akhir.total.os - bln.total.os
    val mtdOs = daily.total.os - akhir.total.os

    val momSml = akhir.sml.os - bln.sml.os
    val mtdSml = daily.sml.os - akhir.sml.os

    val momNpl = akhir.npl.os - bln.npl.os
    val mtdNpl = daily.npl.os - akhir.npl.os

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Nama Mantri di atas, di bawahnya info OS & Deb (horizontal). Sisi kanan: info Realisasi + chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEBF5FF),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF005FAF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = cleanMantriNameOnly(mantriName),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = Color(0xFF00386B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Posisi di bawah Nama Mantri sejajar horizontal (contoh: OS : Rp. 3.000.000 | 320 Deb)
                        Text(
                            text = "OS : Rp ${formatDouble(displayOs, formatter)} | $displayDebtors Deb",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.5.sp
                            ),
                            color = Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Posisi sebelah kanan: Info Jumlah Realisasi (sesuai periode yang dipilih) + Icon Hide/Show (^)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Realisasi: Rp ${formatDouble(realisasiPlafond, formatter)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp
                            ),
                            color = Color(0xFF2E7D32),
                            maxLines = 1
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "$realisasiDeb Debitur",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp
                                ),
                                color = Color(0xFFC62828)
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Sembunyikan tabel" else "Tampilkan tabel",
                        tint = Color(0xFF005FAF),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Konten yang bisa di-hide / show (default hide)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)

                    // Tabel simetris 4 kolom dengan tinggi kolom lebih kecil
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Table Header (Simetris 4 Kolom dengan padding rapat)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF00386B))
                                    .padding(vertical = 1.5.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Periode",
                                    modifier = Modifier.weight(0.70f),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "Lancar",
                                    modifier = Modifier.weight(1.10f),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = "SML",
                                    modifier = Modifier.weight(1.10f),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = "NPL",
                                    modifier = Modifier.weight(1.10f),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.End
                                )
                            }

                            // Row 1: Bulan Lalu
                            MantriTableRow(
                                dateText = dateBlnLalu,
                                lancar = bln.lancar,
                                sml = bln.sml,
                                npl = bln.npl,
                                formatter = formatter,
                                isHighlighted = activePeriodSource == "BULAN_LALU",
                                isEven = false
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)

                            // Row 2: Akhir Bulan
                            MantriTableRow(
                                dateText = dateAkhirBulan,
                                lancar = akhir.lancar,
                                sml = akhir.sml,
                                npl = akhir.npl,
                                formatter = formatter,
                                isHighlighted = activePeriodSource == "AKHIR_BULAN",
                                isEven = true
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)

                            // Row 3: Daily Info
                            MantriTableRow(
                                dateText = dateDaily,
                                lancar = daily.lancar,
                                sml = daily.sml,
                                npl = daily.npl,
                                formatter = formatter,
                                isHighlighted = activePeriodSource == "DAILY",
                                isEven = false
                            )
                        }
                    }

                    // Dibawah tabel: Info MOM dan MTD OS, SML dan NPL (tanpa icon panah, sejajar horizontal)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 1. Metric OS
                            TrendMetricColumn(
                                label = "TOTAL OS",
                                momDelta = momOs,
                                mtdDelta = mtdOs,
                                isInverse = false,
                                modifier = Modifier.weight(1f)
                            )

                            VerticalDivider(
                                modifier = Modifier
                                    .height(28.dp)
                                    .padding(horizontal = 2.dp),
                                color = Color(0xFFE2E8F0)
                            )

                            // 2. Metric SML
                            TrendMetricColumn(
                                label = "SML",
                                momDelta = momSml,
                                mtdDelta = mtdSml,
                                isInverse = true,
                                modifier = Modifier.weight(1f)
                            )

                            VerticalDivider(
                                modifier = Modifier
                                    .height(28.dp)
                                    .padding(horizontal = 2.dp),
                                color = Color(0xFFE2E8F0)
                            )

                            // 3. Metric NPL
                            TrendMetricColumn(
                                label = "NPL",
                                momDelta = momNpl,
                                mtdDelta = mtdNpl,
                                isInverse = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Dibawah kolom info MoM dan MTD: Info Net Disburs
                    val totalNetDisburs = remember(realizedItems) { realizedItems.sumOf { it.netDisburs } }
                    val newDebCount = remember(realizedItems) { realizedItems.count { it.isNasabahBaru } }
                    val suplesiDebCount = remember(realizedItems) { realizedItems.size - newDebCount }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = androidx.compose.foundation.BorderStroke(0.7.dp, Color(0xFFBBF7D0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = Color(0xFF166534), fontSize = 10.sp)) {
                                        append("Net Disburs ")
                                    }
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = if (totalNetDisburs >= 0) Color(0xFF166534) else Color(0xFFC62828), fontSize = 10.sp)) {
                                        append("Rp. ${formatDouble(totalNetDisburs, formatter)} ")
                                    }
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF15803D), fontSize = 9.sp)) {
                                        append("(${realizedItems.size} Deb) ")
                                    }
                                    withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = Color(0xFF475569), fontSize = 9.sp)) {
                                        append("$suplesiDebCount Suplesi | $newDebCount Baru")
                                    }
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            // Icon > untuk memunculkan popup tabel nasabah yg di realisasikan
                            IconButton(
                                onClick = { onShowRealizedDialog(mantriName, realizedItems) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Tabel Nasabah Realisasi",
                                    tint = Color(0xFF166534),
                                    modifier = Modifier.size(18.dp)
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
private fun TrendMetricColumn(
    label: String,
    momDelta: Double,
    mtdDelta: Double,
    isInverse: Boolean,
    modifier: Modifier = Modifier
) {
    val momCol = if (isInverse) {
        if (momDelta > 0) Color(0xFFC62828) else if (momDelta < 0) Color(0xFF2E7D32) else Color(0xFF64748B)
    } else {
        if (momDelta > 0) Color(0xFF2E7D32) else if (momDelta < 0) Color(0xFFC62828) else Color(0xFF64748B)
    }
    val mtdCol = if (isInverse) {
        if (mtdDelta > 0) Color(0xFFC62828) else if (mtdDelta < 0) Color(0xFF2E7D32) else Color(0xFF64748B)
    } else {
        if (mtdDelta > 0) Color(0xFF2E7D32) else if (mtdDelta < 0) Color(0xFFC62828) else Color(0xFF64748B)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.sp
            ),
            color = Color(0xFF00386B)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MoM ${formatMomMtd(momDelta)}",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = momCol,
                maxLines = 1
            )
            Text(
                text = "MTD ${formatMomMtd(mtdDelta)}",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = mtdCol,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MantriTableRow(
    dateText: String,
    lancar: UnitSummaryData,
    sml: UnitSummaryData,
    npl: UnitSummaryData,
    formatter: NumberFormat,
    isHighlighted: Boolean,
    isEven: Boolean
) {
    val bg = if (isHighlighted) Color(0xFFE0F2FE) else if (isEven) Color(0xFFF8FAFC) else Color.White

    fun formatCellText(data: UnitSummaryData): String {
        return "${formatDouble(data.os, formatter)} | ${data.debtors}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(vertical = 1.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateText,
            modifier = Modifier.weight(0.70f),
            fontSize = 9.5.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isHighlighted) Color(0xFF005FAF) else Color(0xFF1E293B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatCellText(lancar),
            modifier = Modifier.weight(1.10f),
            fontSize = 9.2.sp,
            textAlign = TextAlign.End,
            color = Color(0xFF2E7D32),
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatCellText(sml),
            modifier = Modifier.weight(1.10f),
            fontSize = 9.2.sp,
            textAlign = TextAlign.End,
            color = Color(0xFFE65100),
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatCellText(npl),
            modifier = Modifier.weight(1.10f),
            fontSize = 9.2.sp,
            textAlign = TextAlign.End,
            color = Color(0xFFC62828),
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Popup Tabel Nasabah yang menampilkan format tabel identik dengan menu Monitoring.
 */
@Composable
fun CustomerTablePopupDialog(
    title: String,
    customers: List<CustomerEntity>,
    onDismiss: () -> Unit,
    onCustomerClick: (CustomerEntity) -> Unit = {},
    formatter: NumberFormat,
    dailyDataMap: Map<String, CustomerEntity> = emptyMap()
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortCol by remember { mutableStateOf(SortColumn.NAMA_NASABAH) }
    var sortDir by remember { mutableStateOf(SortDirection.ASC) }
    var selectedCustomerForDetail by remember { mutableStateOf<CustomerEntity?>(null) }

    val targetMenuType = remember(customers) { customers.firstOrNull { it.menuType.isNotBlank() }?.menuType ?: "INTRAKOMP" }

    val sortedList = remember(customers, sortCol, sortDir) {
        when (sortCol) {
            SortColumn.NAMA_NASABAH -> {
                if (sortDir == SortDirection.ASC) customers.sortedBy { it.namaNasabah.trim().lowercase() }
                else customers.sortedByDescending { it.namaNasabah.trim().lowercase() }
            }
            SortColumn.PLAFOND -> {
                if (sortDir == SortDirection.ASC) customers.sortedBy { it.plafond }
                else customers.sortedByDescending { it.plafond }
            }
            SortColumn.OS_PINJAMAN -> {
                if (sortDir == SortDirection.ASC) customers.sortedBy { it.osPinjaman }
                else customers.sortedByDescending { it.osPinjaman }
            }
            SortColumn.UPDATE_POKOK -> {
                if (sortDir == SortDirection.ASC) customers.sortedBy { it.updatePokok }
                else customers.sortedByDescending { it.updatePokok }
            }
            SortColumn.SETOR_POKOK -> {
                if (sortDir == SortDirection.ASC) customers.sortedBy { it.setorPokok }
                else customers.sortedByDescending { it.setorPokok }
            }
            SortColumn.SEGMEN -> {
                if (sortDir == SortDirection.ASC) customers.sortedBy { it.segmen.trim().lowercase() }
                else customers.sortedByDescending { it.segmen.trim().lowercase() }
            }
            SortColumn.KOLEK -> {
                if (sortDir == SortDirection.ASC) {
                    customers.sortedWith(compareBy({ parseKolekCodeAndRank(it.kolek).second }, { it.kolek }))
                } else {
                    customers.sortedWith(compareByDescending<CustomerEntity> { parseKolekCodeAndRank(it.kolek).second }.thenByDescending { it.kolek })
                }
            }
            SortColumn.UMUR_TGK -> {
                if (sortDir == SortDirection.ASC) customers.sortedBy { it.umurTgk }
                else customers.sortedByDescending { it.umurTgk }
            }
            SortColumn.KOL_BY_LW -> {
                if (sortDir == SortDirection.ASC) customers.sortedBy { it.kolByLw }
                else customers.sortedByDescending { it.kolByLw }
            }
            SortColumn.NAMA_MANTRI -> {
                if (sortDir == SortDirection.ASC) customers.sortedBy { it.namaMantri.trim().lowercase() }
                else customers.sortedByDescending { it.namaMantri.trim().lowercase() }
            }
            SortColumn.NPD -> {
                if (sortDir == SortDirection.ASC) customers.sortedBy { it.npd.filter { c -> c.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE }
                else customers.sortedByDescending { it.npd.filter { c -> c.isDigit() }.toIntOrNull() ?: Int.MIN_VALUE }
            }
            SortColumn.ALAMAT -> {
                if (sortDir == SortDirection.ASC) customers.sortedBy { it.alamat.trim().lowercase() }
                else customers.sortedByDescending { it.alamat.trim().lowercase() }
            }
            SortColumn.DESA -> {
                if (sortDir == SortDirection.ASC) customers.sortedBy { it.desa.trim().lowercase() }
                else customers.sortedByDescending { it.desa.trim().lowercase() }
            }
            SortColumn.ALIH_TAGIH -> {
                if (sortDir == SortDirection.ASC) customers.sortedBy { it.pinalty }
                else customers.sortedByDescending { it.pinalty }
            }
            SortColumn.SISA_BUNGA -> {
                if (sortDir == SortDirection.ASC) customers.sortedBy { it.tunggakanBunga }
                else customers.sortedByDescending { it.tunggakanBunga }
            }
            SortColumn.TOTAL_DH -> {
                val getEffectiveTunggakan: (CustomerEntity) -> Double = { cust ->
                    val dCust = if (dailyDataMap.isNotEmpty()) {
                        dailyDataMap[cust.noRekening.trim()]
                            ?: (if (cust.noRekening.isNotBlank()) dailyDataMap[cust.noRekening.filter { it.isDigit() }] else null)
                    } else null
                    when {
                        dCust != null && dCust.totalTunggakan > 0.0 -> dCust.totalTunggakan
                        cust.totalTunggakan > 0.0 -> cust.totalTunggakan
                        dCust != null && (dCust.tunggakanPokok + dCust.tunggakanBunga) > 0.0 -> dCust.tunggakanPokok + dCust.tunggakanBunga
                        (cust.tunggakanPokok + cust.tunggakanBunga) > 0.0 -> cust.tunggakanPokok + cust.tunggakanBunga
                        else -> 0.0
                    }
                }
                if (sortDir == SortDirection.ASC) customers.sortedBy(getEffectiveTunggakan)
                else customers.sortedByDescending(getEffectiveTunggakan)
            }
        }
    }

    val filteredCustomers = remember(sortedList, searchQuery) {
        if (searchQuery.isBlank()) sortedList
        else {
            val q = searchQuery.trim().lowercase()
            sortedList.filter {
                it.namaNasabah.lowercase().contains(q) ||
                it.noRekening.lowercase().contains(q) ||
                it.cif.lowercase().contains(q) ||
                it.namaMantri.lowercase().contains(q) ||
                it.alamat.lowercase().contains(q)
            }
        }
    }

    val totalOs = remember(filteredCustomers) { filteredCustomers.sumOf { it.osPinjaman } }
    val totalDeb = filteredCustomers.size

    AnimatedFadeSlideDialog(onDismiss = onDismiss) { requestDismiss ->
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar Dialog
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF00386B))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Total OS: Rp ${formatDouble(totalOs, formatter)} | $totalDeb Debitur",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = requestDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Kolom Pencarian Cepat - Ukuran dan desain identik dengan searchbar menu monitoring
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = Color(0xFF001C3B),
                            fontWeight = FontWeight.SemiBold
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(33.dp),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFEEF4FC), shape = RoundedCornerShape(16.dp))
                                    .border(0.8.dp, Color(0xFFFFCCB0), shape = RoundedCornerShape(16.dp))
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Cari",
                                    tint = Color(0xFF005FAF),
                                    modifier = Modifier.size(15.dp).padding(end = 4.dp)
                                )
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Cari nama, rekening, CIF, mantri...",
                                            color = Color(0xFF64748B),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        innerTextField()
                                    }
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Hapus",
                                            tint = Color(0xFF43474E),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                // Render CustomerTable (Tabel Menu Monitoring)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (filteredCustomers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Tidak ada nasabah yang cocok",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    } else {
                        CustomerTable(
                            customersList = filteredCustomers,
                            selectedMenu = targetMenuType,
                            sortCol = sortCol,
                            sortDir = sortDir,
                            onHeaderClick = { clickedCol ->
                                if (sortCol == clickedCol) {
                                    sortDir = if (sortDir == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
                                } else {
                                    sortCol = clickedCol
                                    sortDir = when (clickedCol) {
                                        SortColumn.NAMA_NASABAH, SortColumn.NAMA_MANTRI, SortColumn.SEGMEN, SortColumn.ALAMAT, SortColumn.DESA -> SortDirection.ASC
                                        else -> SortDirection.DESC
                                    }
                                }
                            },
                            onCustomerClick = { customer ->
                                onCustomerClick(customer)
                            },
                            formatter = formatter,
                            showJatuhTempoFilter = false,
                            dailyDataMap = dailyDataMap,
                            showTunggakanColumn = title.contains("Tukcil", ignoreCase = true) || targetMenuType == "KELOLAAN" || targetMenuType == "KUNJUNGAN"
                        )
                    }
                }
            }
        }
    }

    if (selectedCustomerForDetail != null) {
        CustomerQuickDetailDialog(
            customer = selectedCustomerForDetail!!,
            onDismiss = { selectedCustomerForDetail = null },
            formatter = formatter
        )
    }
}

@Composable
fun CustomerQuickDetailDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    formatter: NumberFormat
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(customer.namaNasabah, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("No Rekening: ${customer.noRekening}", fontSize = 12.sp)
                Text("CIF: ${customer.cif}", fontSize = 12.sp)
                Text("Plafond: Rp ${formatDouble(customer.plafond, formatter)}", fontSize = 12.sp)
                Text("OS Pinjaman: Rp ${formatDouble(customer.osPinjaman, formatter)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Kolektabilitas: ${customer.kolek}", fontSize = 12.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                Text("Mantri: ${customer.namaMantri}", fontSize = 12.sp)
                Text("Segmen: ${customer.segmen}", fontSize = 12.sp)
                if (customer.alamat.isNotBlank()) {
                    Text("Alamat: ${customer.alamat}", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

fun formatMomMtd(amount: Double): String {
    val sign = if (amount > 0) "+" else if (amount < 0) "-" else ""
    val absVal = kotlin.math.abs(amount)
    val truncatedMillions = (absVal / 10_000.0).toLong() / 100.0
    val symbols = DecimalFormatSymbols.getInstance().apply {
        decimalSeparator = ','
        groupingSeparator = '.'
    }
    val df = DecimalFormat("#,##0.00", symbols)
    return "$sign${df.format(truncatedMillions)}"
}

fun formatDouble(value: Double, formatter: NumberFormat): String {
    return try {
        formatter.format(value)
    } catch (e: Exception) {
        value.toString()
    }
}
