package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import com.example.R
import com.example.ui.components.AnimatedFadeSlideDialog
import com.example.ui.components.BriWaveHeaderBackground
import com.example.ui.components.CurvedAnimatedBottomBar
import com.example.ui.components.HeaderPocketSearchBar
import com.example.ui.components.HeaderPocketSearchButton
import com.example.ui.components.LazyLoadingFooterRow
import com.example.ui.components.PnLoginPopupDialog
import com.example.ui.components.rememberLazyPagedItems
import com.example.ui.components.TableLiquidSearchBar
import com.example.data.model.CustomerEntity
import com.example.data.model.desa
import com.example.data.model.mapEkstrakompSegmen
import com.example.data.util.DateTenorUtils
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.SortColumn
import com.example.ui.viewmodel.SortDirection
import com.example.ui.viewmodel.MantriDetailData
import com.example.ui.viewmodel.UnitSummaryData
import java.text.NumberFormat
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.example.update.AppUpdateManager
import com.example.update.AppUpdateState

fun formatDoubleClean(value: Double, formatter: NumberFormat): String {
    val formatted = formatter.format(value)
    return formatStringValueClean(formatted)
}

fun formatDoubleWith2Decimals(value: Double): String {
    if (value == 0.0) return "0"
    val symbols = java.text.DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val df = java.text.DecimalFormat("#,##0.00", symbols)
    return df.format(value)
}

fun formatDoubleInMillions(value: Double, formatter: NumberFormat? = null): String {
    if (value == 0.0) return "0"
    val millions = value / 1_000_000.0
    val symbols = java.text.DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val df = java.text.DecimalFormat("#,##0.##", symbols)
    return df.format(millions)
}

fun formatStringValueClean(str: String): String {
    var cleaned = str.trim()
    if (cleaned.endsWith(".00") || cleaned.endsWith(",00")) {
        cleaned = cleaned.substring(0, cleaned.length - 3)
    } else if (cleaned.endsWith(".0") || cleaned.endsWith(",0")) {
        cleaned = cleaned.substring(0, cleaned.length - 2)
    }
    return cleaned
}

fun getMonthsBetween(npdStr: String): Int {
    try {
        if (npdStr.isBlank()) return 0
        val clean = npdStr.trim().split(" ").firstOrNull() ?: ""
        if (clean.length >= 10) {
            val day = clean.substring(0, 2).toIntOrNull() ?: 1
            val month = clean.substring(3, 5).toIntOrNull() ?: 1
            val year = clean.substring(clean.length - 4).toIntOrNull() ?: 2026
            val npdDate = java.time.LocalDate.of(year, month, day)
            val today = java.time.LocalDate.now()
            if (today.isBefore(npdDate)) return 0
            val months = java.time.temporal.ChronoUnit.MONTHS.between(npdDate, today).toInt()
            return if (months <= 0) {
                if (today.isAfter(npdDate)) 1 else 0
            } else {
                months
            }
        }
    } catch (e: Exception) {
        // Fallback
    }
    return 0
}

fun toProperCase(s: String): String {
    return s.lowercase().split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
    }
}

fun calculateElapsedMonthsSinceReal(tglReal: String): Int? =
    DateTenorUtils.monthsElapsedSince(tglReal)

fun calculateRemainingMonthsUntilLunas(tglLunas: String): Int? =
    DateTenorUtils.monthsRemainingUntil(tglLunas)

fun simplifyAddress(rawAlamat: String): String {
    if (rawAlamat.isBlank()) return rawAlamat

    val tokens = rawAlamat.trim().split(Regex("\\s+"))
    if (tokens.size <= 1) return rawAlamat

    val seenWords = mutableSetOf<String>()
    val resultTokens = mutableListOf<String>()

    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        val clean = token.trim { !it.isLetterOrDigit() }.uppercase()
        val hasDigits = token.any { it.isDigit() }

        if (hasDigits || clean in setOf("RT", "RW", "NO", "NOMOR", "BLOK", "KM", "GANG", "GG", "JL", "JLN", "JALAN")) {
            resultTokens.add(token)
            i++
        } else if (clean in setOf("KP", "KAMPUNG", "DS", "DESA", "KEC", "KECAMATAN", "KAB", "KABUPATEN", "DUSUN")) {
            if (i + 1 < tokens.size) {
                val nextToken = tokens[i + 1]
                val nextClean = nextToken.trim { !it.isLetterOrDigit() }.uppercase()
                if (nextClean.isNotBlank() && !nextToken.any { it.isDigit() } && nextClean in seenWords) {
                    // Next word is already seen duplicate! Skip both prefix and duplicate word
                    i += 2
                    continue
                }
            }
            resultTokens.add(token)
            i++
        } else if (clean.isNotBlank()) {
            if (seenWords.add(clean)) {
                resultTokens.add(token)
            } else {
                // Word already seen, skip duplicate!
            }
            i++
        } else {
            resultTokens.add(token)
            i++
        }
    }

    var result = resultTokens.joinToString(" ")
    result = result.replace(Regex(",\\s*,"), ",")
        .replace(Regex("\\s+,"), ",")
        .replace(Regex("[,\\s]+$"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    return if (result.isNotBlank()) result else rawAlamat
}

fun formatShortNominal(amount: Double): String {
    if (amount <= 0) return "0jt"
    val juta = kotlin.math.ceil(amount / 1_000_000.0).toLong()
    return "${juta}jt"
}

fun getWaKolekCategory(cust: CustomerEntity): String {
    val k = cust.kolek.trim().uppercase()
    return when {
        k == "LANCAR" || k == "L" || k == "1" || (k.contains("LANCAR") && !k.contains("KURANG")) -> "LANCAR"
        k == "SML1" || k == "SML 1" || k == "SML-1" || k == "2.1" || k.contains("SML1") || k.contains("SML 1") || k.contains("SML-1") || k.contains("DPK1") || k.contains("DPK 1") || k.contains("1-30") -> "SML1"
        k == "SML2" || k == "SML 2" || k == "SML-2" || k == "2.2" || k.contains("SML2") || k.contains("SML 2") || k.contains("SML-2") || k.contains("DPK2") || k.contains("DPK 2") || k.contains("31-60") -> "SML2"
        k == "SML3" || k == "SML 3" || k == "SML-3" || k == "2.3" || k.contains("SML3") || k.contains("SML 3") || k.contains("SML-3") || k.contains("DPK3") || k.contains("DPK 3") || k.contains("61-90") -> "SML3"
        k == "SML" || k == "DPK" || k == "2" -> {
            when {
                cust.umurTgk in 1..30 -> "SML1"
                cust.umurTgk in 31..60 -> "SML2"
                cust.umurTgk > 60 -> "SML3"
                else -> "SML"
            }
        }
        k == "KL" || k.contains("KURANG") || k == "D" || k.contains("DIRAGUKAN") || k == "M" || k.contains("MACET") || k == "NPL" || k == "3" || k == "4" || k == "5" -> "NPL"
        k.isNotBlank() -> k
        else -> "LAINNYA"
    }
}

fun generateAktivitasMessage(
    isRencana: Boolean,
    mantriName: String,
    customers: List<CustomerEntity>,
    tindakLanjutMap: Map<String, String> = emptyMap(),
    customTemplate: String? = null,
    includeOs: Boolean = true
): String {
    val today = java.time.LocalDate.now()
    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val actualTodayStr = today.format(formatter)
    
    val title = if (isRencana) "Rencana aktivitas" else "Hasil aktivitas"

    fun formatCustomerLine(index: Int, cust: CustomerEntity, includeKolekSuffix: Boolean = false): String {
        val key = if (cust.noRekening.isNotBlank()) cust.noRekening else if (cust.cif.isNotBlank()) cust.cif else cust.namaNasabah
        val tl = tindakLanjutMap["kunjungan_$key"] ?: tindakLanjutMap[key]
        val suffix = if (!isRencana && !tl.isNullOrBlank() && tl != "-") " - $tl" else ""
        val osVal = if (cust.osPinjaman > 0) cust.osPinjaman else cust.totalTunggakan
        val osStr = if (includeOs && osVal > 0) " ${formatShortNominal(osVal)}" else ""
        val kolekSuffix = if (includeKolekSuffix) " (${cust.kolek})" else ""
        return "${index + 1}. ${toProperCase(cust.namaNasabah)}$osStr$kolekSuffix$suffix"
    }

    fun buildSectionList(list: List<CustomerEntity>): String {
        if (list.isEmpty()) return "-"
        return list.mapIndexed { index, cust -> formatCustomerLine(index, cust) }.joinToString("\n")
    }

    val groupedByKolek = customers.groupBy { getWaKolekCategory(it) }
    val priorityOrder = listOf("LANCAR", "SML1", "SML2", "SML3", "SML", "NPL")
    val orderedCategories = priorityOrder.filter { groupedByKolek.containsKey(it) } +
        groupedByKolek.keys.filter { it !in priorityOrder }

    val sbPerKolek = StringBuilder()
    if (orderedCategories.isNotEmpty()) {
        orderedCategories.forEachIndexed { idx, category ->
            val groupList = groupedByKolek[category].orEmpty()
            if (groupList.isNotEmpty()) {
                if (idx > 0) sbPerKolek.append("\n\n")
                sbPerKolek.append(category).append("\n")
                sbPerKolek.append(buildSectionList(groupList))
            }
        }
    } else {
        sbPerKolek.append("-")
    }

    val sbLancar = buildSectionList(groupedByKolek["LANCAR"].orEmpty())
    val sbSml1 = buildSectionList(groupedByKolek["SML1"].orEmpty())
    val sbSml2 = buildSectionList(groupedByKolek["SML2"].orEmpty())
    val sbSml3 = buildSectionList(groupedByKolek["SML3"].orEmpty())
    val sbNpl = buildSectionList(groupedByKolek["NPL"].orEmpty())

    val sbSemua = if (customers.isNotEmpty()) {
        customers.mapIndexed { index, cust -> formatCustomerLine(index, cust, includeKolekSuffix = true) }.joinToString("\n")
    } else {
        "-"
    }

    val activeTemplate = customTemplate?.trim()
    if (!activeTemplate.isNullOrBlank() && (activeTemplate.contains("{") && activeTemplate.contains("}"))) {
        var resolvedTemplate = activeTemplate
        val hasSml2Customers = groupedByKolek["SML2"].orEmpty().isNotEmpty()
        if (hasSml2Customers &&
            !resolvedTemplate.contains("{daftar_kolek}") &&
            !resolvedTemplate.contains("{daftar_sml2}") &&
            !resolvedTemplate.contains("{daftar_semua}")
        ) {
            val sml3Regex = Regex("""(?im)^(\s*\*?SML\s*3\*?\s*\r?\n)?\s*\{daftar_sml3\}""")
            val nplRegex = Regex("""(?im)^(\s*\*?NPL\*?\s*\r?\n)?\s*\{daftar_npl\}""")
            resolvedTemplate = when {
                sml3Regex.containsMatchIn(resolvedTemplate) ->
                    resolvedTemplate.replace(sml3Regex) { match -> "SML2\n{daftar_sml2}\n\n${match.value}" }
                nplRegex.containsMatchIn(resolvedTemplate) ->
                    resolvedTemplate.replace(nplRegex) { match -> "SML2\n{daftar_sml2}\n\n${match.value}" }
                else -> "$resolvedTemplate\n\nSML2\n{daftar_sml2}"
            }
        }
        return resolvedTemplate
            .replace("{judul}", title)
            .replace("{mantri}", mantriName)
            .replace("{tanggal}", actualTodayStr)
            .replace("{daftar_kolek}", sbPerKolek.toString().trimEnd())
            .replace("{daftar_lancar}", sbLancar)
            .replace("{daftar_sml1}", sbSml1)
            .replace("{daftar_sml2}", sbSml2)
            .replace("{daftar_sml3}", sbSml3)
            .replace("{daftar_npl}", sbNpl)
            .replace("{daftar_semua}", sbSemua)
            .trimEnd()
    }
    
    val sb = StringBuilder()
    sb.append("$title mantri $mantriName\n")
    sb.append("Tgl $actualTodayStr\n\n")
    sb.append(sbPerKolek.toString().trimEnd())
    
    return sb.toString().trimEnd()
}

fun sendWhatsAppGroupMessage(
    context: android.content.Context,
    message: String
) {
    try {
        val uriString = "whatsapp://send?text=${java.net.URLEncoder.encode(message, "UTF-8")}"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uriString))
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            fallbackIntent.data = android.net.Uri.parse("https://api.whatsapp.com/send?text=${java.net.URLEncoder.encode(message, "UTF-8")}")
            context.startActivity(fallbackIntent)
        } catch (e2: Exception) {
            android.widget.Toast.makeText(context, "Tidak dapat membuka WhatsApp!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

fun sendWhatsAppMessage(
    context: android.content.Context,
    targetHp: String,
    customer: CustomerEntity,
    formatter: java.text.NumberFormat,
    loggedInDetails: String,
    overrideTotalTunggakan: Double? = null,
    customTemplate: String? = null
) {
    var cleanPhone = targetHp.replace(Regex("[^0-9]"), "")
    if (cleanPhone.startsWith("8")) {
        cleanPhone = "0" + cleanPhone
    }
    if (cleanPhone.isBlank() || cleanPhone.length < 6) {
        android.widget.Toast.makeText(context, "Nomor HP debitur tidak valid!", android.widget.Toast.LENGTH_LONG).show()
        return
    }
    
    val formattedPhone = if (cleanPhone.startsWith("0")) {
        "62" + cleanPhone.substring(1)
    } else {
        cleanPhone
    }

    val calendar = java.util.Calendar.getInstance()
    val currentMonth = String.format("%02d", calendar.get(java.util.Calendar.MONTH) + 1)
    val currentYear = calendar.get(java.util.Calendar.YEAR).toString()
    val tglNpd = customer.npd.trim().ifBlank { "01" }
    val jatuhTempo = "$tglNpd/$currentMonth/$currentYear"
    
    val npdFull = customer.extraColumns.split("||")
        .firstOrNull { it.startsWith("NPD_FULL::") }
        ?.substringAfter("NPD_FULL::") ?: ""
    var keterlambatanBulan = if (npdFull.isNotBlank()) {
        getMonthsBetween(npdFull)
    } else {
        0
    }
    if (keterlambatanBulan <= 0 && customer.umurTgk > 0) {
        keterlambatanBulan = (customer.umurTgk / 30).coerceAtLeast(1)
    }

    val finalTotalTunggakan = overrideTotalTunggakan ?: customer.totalTunggakan

    val mantriName = if (loggedInDetails.isNotBlank()) {
        if (loggedInDetails.contains(" - ")) loggedInDetails.substringAfter(" - ").trim() else loggedInDetails.trim()
    } else {
        "Nurul Mubin"
    }

    val sharedPrefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
    val template = customTemplate?.ifBlank { null }
        ?: sharedPrefs.getString("template_wa_detail_debitur", com.example.ui.viewmodel.DEFAULT_WA_DETAIL_TEMPLATE)
        ?: com.example.ui.viewmodel.DEFAULT_WA_DETAIL_TEMPLATE

    val pesanan = template
        .replace("{nama}", customer.namaNasabah)
        .replace("{keterlambatan}", keterlambatanBulan.toString())
        .replace("{total_tunggakan}", formatDoubleClean(finalTotalTunggakan, formatter))
        .replace("{jatuh_tempo}", jatuhTempo)
        .replace("{no_rekening}", customer.noRekening)
        .replace("{cif}", customer.cif)
        .replace("{plafon}", formatDoubleClean(customer.plafond, formatter))
        .replace("{os_pinjaman}", formatDoubleClean(customer.osPinjaman, formatter))
        .replace("{unit}", "BRI UNIT KATAPANG SOREANG")
        .replace("{mantri}", mantriName)
        .replace("{alamat}", customer.alamat)
    
    try {
        val uriString = "whatsapp://send?phone=$formattedPhone&text=${java.net.URLEncoder.encode(pesanan, "UTF-8")}"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uriString))
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            fallbackIntent.data = android.net.Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${java.net.URLEncoder.encode(pesanan, "UTF-8")}")
            context.startActivity(fallbackIntent)
        } catch (e2: Exception) {
            android.widget.Toast.makeText(context, "Tidak dapat membuka WhatsApp maupun Browser!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: CustomerViewModel,
    modifier: Modifier = Modifier
) {
    val selectedMenu by viewModel.selectedMenu.collectAsState()
    val selectedPeriodSource by viewModel.selectedPeriodSource.collectAsState()
    val periodDateBulanLalu by viewModel.periodDateBulanLalu.collectAsState()
    val periodDateAkhirBulan by viewModel.periodDateAkhirBulan.collectAsState()
    val periodDateDaily by viewModel.periodDateDaily.collectAsState()
    val periodDateLw325 by viewModel.periodDateLw325.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val filterKolek by viewModel.filterKolek.collectAsState()
    val filterMantri by viewModel.filterMantri.collectAsState()
    val filterBulanLunas by viewModel.filterBulanLunas.collectAsState()
    val filterTahunLunas by viewModel.filterTahunLunas.collectAsState()
    val filterOS by viewModel.filterOS.collectAsState()
    val filterPlafondRange by viewModel.filterPlafondRange.collectAsState()
    val filterTenorRange by viewModel.filterTenorRange.collectAsState()
    val filterSegmen by viewModel.filterSegmen.collectAsState()
    val filterKolekUpdate by viewModel.filterKolekUpdate.collectAsState()
    val filterKolekBlnLalu by viewModel.filterKolekBlnLalu.collectAsState()
    val filterNpd by viewModel.filterNpd.collectAsState()
    val filterAlamat by viewModel.filterAlamat.collectAsState()
    val filterSetorPokok by viewModel.filterSetorPokok.collectAsState()
    val filterTusim by viewModel.filterTusim.collectAsState()
    val filterTucil by viewModel.filterTucil.collectAsState()
    val filterTunggakanKurang6Bln by viewModel.filterTunggakanKurang6Bln.collectAsState()
    val filterDesa by viewModel.filterDesa.collectAsState()
    val availableNpds by viewModel.availableNpds.collectAsState()
    val customersList by viewModel.customersList.collectAsState()
    val availableMantris by viewModel.availableMantris.collectAsState()
    val availableYearsPh by viewModel.availableYearsPh.collectAsState()
    val availableYearsRealisasi by viewModel.availableYearsRealisasi.collectAsState()
    val availableSegmensEkstrakomp by viewModel.availableSegmensEkstrakomp.collectAsState()
    val availableYears by viewModel.availableYears.collectAsState()
    val summaryStats by viewModel.summaryStats.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isShowingCachedOfflineData by viewModel.isShowingCachedOfflineData.collectAsStateWithLifecycle()
    val lastSyncMetadata by viewModel.lastSyncMetadata.collectAsStateWithLifecycle()
    val sortCol by viewModel.sortColumn.collectAsState()
    val sortDir by viewModel.sortDirection.collectAsState()
    val loggedInPN by viewModel.loggedInPN.collectAsState()
    val loggedInDetails by viewModel.loggedInMantriDetails.collectAsState()
    val kunjunganKeys by viewModel.kunjunganKeys.collectAsState()
    val activeVisitDateStr by viewModel.activeVisitDateStr.collectAsState()
    val allCustomers by viewModel.allCustomersList.collectAsState()
    val monitoringCustomers by viewModel.monitoringCustomers.collectAsState()
    val tindakLanjutMap by viewModel.tindakLanjutMap.collectAsState()
    val availableDesas by viewModel.availableDesas.collectAsState()
    val dueTodayCount by viewModel.dueTodayCount.collectAsState()
    val dueHCounts by viewModel.dueHCounts.collectAsState()
    val selectedJatuhTempoH by viewModel.selectedJatuhTempoH.collectAsState()
    val filterJatuhTempoOnly by viewModel.filterJatuhTempoOnly.collectAsState()
    val templateWaRencana by viewModel.templateWaRencana.collectAsState()
    val templateWaDetailDebitur by viewModel.templateWaDetailDebitur.collectAsState()
    val includeOsInRencanaWa by viewModel.includeOsInRencanaWa.collectAsState()

    var showDetailDialogForCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showSpLetterDialog by remember { mutableStateOf(false) }
    var selectedSpType by remember { mutableStateOf(com.example.ui.util.SpType.SP1) }
    var selectedSpCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showGlobalSearchPopup by remember { mutableStateOf(false) }
    var globalSearchQuery by remember { mutableStateOf("") }
    val globalSearchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(showGlobalSearchPopup) {
        if (showGlobalSearchPopup) {
            delay(80L)
            runCatching { globalSearchFocusRequester.requestFocus() }
        }
    }
    BackHandler(enabled = showGlobalSearchPopup) {
        showGlobalSearchPopup = false
        globalSearchQuery = ""
    }
    var showTindakLanjutPopupForCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showDetailUnitDialog by remember { mutableStateOf(false) }
    var waPhoneSelectorNumbers by remember { mutableStateOf<List<String>?>(null) }
    var waPhoneSelectorCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showFilterPanel by remember { mutableStateOf(false) }
    var showSimpananFilterBottomSheet by remember { mutableStateOf(false) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showOfflineDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showConfirmLogoff by remember { mutableStateOf(false) }
    var showTusimPopup by remember { mutableStateOf(false) }
    var showNpdMultiSelectDialog by remember { mutableStateOf(false) }
    val customSpreadsheetUrl by viewModel.customSpreadsheetUrl.collectAsState()
    val url1 by viewModel.url1.collectAsState()
    val url2 by viewModel.url2.collectAsState()
    val url3 by viewModel.url3.collectAsState()
    val url4 by viewModel.url4.collectAsState()
    val eUrl1 by viewModel.eUrl1.collectAsState()
    val eUrl2 by viewModel.eUrl2.collectAsState()
    val kUrl1 by viewModel.kUrl1.collectAsState()
    val di319Url by viewModel.di319Url.collectAsState()
    
    val isOfflineModeEnabled by viewModel.isOfflineModeEnabled.collectAsState()
    val offlineNameUrl1 by viewModel.offlineNameUrl1.collectAsState()
    val offlineNameUrl2 by viewModel.offlineNameUrl2.collectAsState()
    val offlineNameUrl3 by viewModel.offlineNameUrl3.collectAsState()
    val offlineNameUrl4 by viewModel.offlineNameUrl4.collectAsState()
    val offlineNameEUrl1 by viewModel.offlineNameEUrl1.collectAsState()
    val offlineNameEUrl2 by viewModel.offlineNameEUrl2.collectAsState()
    val offlineNameKUrl1 by viewModel.offlineNameKUrl1.collectAsState()
    val offlineNameDi319Url by viewModel.offlineNameDi319Url.collectAsState()
    val dailyDataMap by viewModel.dailyDataMap.collectAsState()
    val di319Rows by viewModel.di319Rows.collectAsState()
    val savingsMap = remember(di319Rows) {
        di319Rows.groupBy { row ->
            val clean = row.cif.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
            clean.dropWhile { it == '0' }
        }.mapValues { entry ->
            entry.value.map { com.example.data.model.SavingsAccount(noRekening = it.noRekening, saldo = it.saldo) }
        }
    }
    val tusimPopupItems = remember(allCustomers, savingsMap, loggedInPN) {
        val result = mutableListOf<TusimPopupItem>()
        val filteredCustomers = allCustomers.filter { cust ->
            cust.menuType == "INTRAKOMP" && (
                loggedInPN.isBlank() || 
                cust.pnMantri.trim() == loggedInPN.trim() || 
                cust.namaMantri.contains(loggedInPN, ignoreCase = true)
            )
        }
        for (cust in filteredCustomers) {
            val clean = cust.cif.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
            val customerCifKey = clean.dropWhile { it == '0' }
            val savingsList = savingsMap[customerCifKey] ?: emptyList()
            
            val matchedSavings = savingsList.filter { it.saldo > 25000.0 }
            for (saving in matchedSavings) {
                result.add(
                    TusimPopupItem(
                        namaNasabah = cust.namaNasabah,
                        noPinjaman = cust.noRekening,
                        noSimpanan = saving.noRekening,
                        saldo = saving.saldo
                    )
                )
            }
        }
        result.sortByDescending { it.saldo }
        result
    }
    val simpananList by viewModel.simpananList.collectAsState()
    val simpananSortColumn by viewModel.simpananSortColumn.collectAsState()
    val simpananSortDirection by viewModel.simpananSortDirection.collectAsState()
    val selectedBottomNav by viewModel.selectedBottomNav.collectAsState()
    var visualBottomNav by remember {
        mutableStateOf(if (selectedBottomNav == "RENCANA") "BERANDA" else selectedBottomNav)
    }
    LaunchedEffect(selectedBottomNav) {
        visualBottomNav = if (selectedBottomNav == "RENCANA") "BERANDA" else selectedBottomNav
    }
    var activeBerandaListPage by remember { mutableStateOf<BerandaCustomerListPageRequest?>(null) }
    var displayedBerandaListPage by remember { mutableStateOf<BerandaCustomerListPageRequest?>(null) }
    if (activeBerandaListPage != null) {
        displayedBerandaListPage = activeBerandaListPage
    }
    BackHandler(
        enabled = activeBerandaListPage != null &&
            !showGlobalSearchPopup &&
            showDetailDialogForCustomer == null &&
            !showSettingsDialog &&
            !showLoginDialog
    ) {
        activeBerandaListPage = null
    }
    BackHandler(
        enabled = selectedBottomNav == "RENCANA" &&
            activeBerandaListPage == null &&
            !showGlobalSearchPopup &&
            showDetailDialogForCustomer == null &&
            !showSettingsDialog &&
            !showLoginDialog
    ) {
        viewModel.setBottomNav("BERANDA")
    }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val appUpdateState by viewModel.appUpdateState.collectAsState()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()

    val updateNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val state = appUpdateState
        if (granted && state is AppUpdateState.UpdateAvailable) {
            AppUpdateManager.showUpdateNotification(context, state.release)
        }
    }

    val updateInstallPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.retryInstallAfterPermissionGranted()
    }

    LaunchedEffect(appUpdateState) {
        val state = appUpdateState
        if (state is AppUpdateState.UpdateAvailable &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !AppUpdateManager.hasNotificationPermission(context)
        ) {
            updateNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.checkForUpdatesOnAppStart()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val calendarInstance = remember { java.util.Calendar.getInstance() }
    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDateStr = String.format("%02d/%02d/%4d", dayOfMonth, month + 1, year)
                viewModel.selectVisitDate(selectedDateStr)
            },
            calendarInstance.get(java.util.Calendar.YEAR),
            calendarInstance.get(java.util.Calendar.MONTH),
            calendarInstance.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    // Indon Locale for Number Format
    val formatter = remember {
        NumberFormat.getNumberInstance(Locale("in", "ID")).apply {
            maximumFractionDigits = 0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                val navItems = remember {
                    listOf(
                        BottomNavItemData("BERANDA", "Beranda", Icons.Filled.Home, Icons.Outlined.Home),
                        BottomNavItemData("KELOLAAN", "Kelolaan", Icons.Filled.People, Icons.Outlined.People),
                        BottomNavItemData("MONITORING", "Monitoring", Icons.Filled.Monitor, Icons.Outlined.Monitor),
                        BottomNavItemData("DASHBOARD", "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
                        BottomNavItemData("SETTING", "Setting", Icons.Filled.Settings, Icons.Outlined.Settings)
                    )
                }
                CurvedAnimatedBottomBar(
                    items = navItems,
                    selectedId = visualBottomNav,
                    onSelect = { targetNav ->
                        if (visualBottomNav == targetNav && selectedBottomNav == targetNav) return@CurvedAnimatedBottomBar
                        visualBottomNav = targetNav
                        viewModel.setBottomNav(targetNav)
                    }
                )
            }
        ) { innerPadding ->
        val isBerandaScreen = selectedBottomNav == "BERANDA"
        val isSettingScreen = selectedBottomNav == "SETTING"
        val saveableStateHolder = rememberSaveableStateHolder()
        val renderNonFullBleedHeader: @Composable () -> Unit = {
            Column(modifier = Modifier.fillMaxWidth()) {
            // Unstretched normal Setting wave background clipped cleanly at the header bottom border
            val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
            ) {
                BriWaveHeaderBackground(
                    modifier = Modifier.matchParentSize(),
                    seamlessTopBlend = true,
                    bottomCurveColor = Color.White,
                    referenceHeight = 142.dp + statusBarTopPadding
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 8.dp)
                ) {
                    AnimatedContent(
                        targetState = showGlobalSearchPopup,
                        transitionSpec = {
                            val headerSpring = spring<IntOffset>(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium,
                                visibilityThreshold = IntOffset.VisibilityThreshold
                            )
                            slideInHorizontally(animationSpec = headerSpring) { it / 3 } togetherWith
                                slideOutHorizontally(animationSpec = headerSpring) { it / 3 } using
                                SizeTransform(clip = true, sizeAnimationSpec = { _, _ -> snap() })
                        },
                        label = "header_search_transition"
                    ) { isSearchExpanded ->
                        if (isSearchExpanded) {
                            HeaderPocketSearchBar(
                                query = globalSearchQuery,
                                onQueryChange = { globalSearchQuery = it },
                                onClose = {
                                    showGlobalSearchPopup = false
                                    globalSearchQuery = ""
                                    keyboardController?.hide()
                                },
                                focusRequester = globalSearchFocusRequester,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(vertical = 4.dp)
                                        .padding(end = 56.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.logobaru1),
                                        contentDescription = "Logo MonBer",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                        modifier = Modifier
                                            .height(30.dp)
                                            .wrapContentWidth()
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "By nUbiin__",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.88f),
                                        maxLines = 1
                                    )
                                }

                                HeaderPocketSearchButton(
                                    onClick = { showGlobalSearchPopup = true },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                }
            }

            if (isRefreshing) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = Color.White,
                    trackColor = Color(0xFF005FAF)
                )
            } else {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color(0xFF004B96).copy(alpha = 0.22f)
                )
            }

            // Automatic In-App Update Notification Banner if available and dialog was dismissed
            val availableUpdateRelease = (appUpdateState as? AppUpdateState.UpdateAvailable)?.release
            androidx.compose.animation.AnimatedVisibility(
                visible = availableUpdateRelease != null && !showUpdateDialog,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                )
            ) {
                availableUpdateRelease?.let { release ->
                    Surface(
                        color = Color(0xFFFFF7ED),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openUpdateDialog() }
                            .testTag("update_available_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Pembaruan Tersedia",
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Versi baru ${release.tagName} tersedia",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF9A3412),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Update",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEA580C)
                            )
                        }
                    }
                }
            }

            // Room Offline Cache Indicator Banner (shown across Dashboard & Data views when offline)
            androidx.compose.animation.AnimatedVisibility(
                visible = isShowingCachedOfflineData && errorMessage == null,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                )
            ) {
                val syncTimeLabel = remember(lastSyncMetadata) {
                    val ts = lastSyncMetadata?.lastSyncedAt ?: 0L
                    if (ts > 0L) {
                        java.text.SimpleDateFormat("dd MMM HH:mm", Locale("id", "ID"))
                            .format(java.util.Date(ts))
                    } else null
                }
                Surface(
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offline_cache_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Mode Cache Offline",
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (syncTimeLabel != null) {
                                "Offline • Cache Room ($syncTimeLabel)"
                            } else {
                                "Offline • Menampilkan data cache Room terakhir"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E3A8A),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        TextButton(
                            onClick = { viewModel.refreshSpreadsheetData(force = true) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("retry_sync_button")
                        ) {
                            Text(
                                text = "Sinkronkan",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8)
                            )
                        }
                    }
                }
            }
        }
        }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .background(MaterialTheme.colorScheme.background)
                    .clipToBounds()
            ) {
                fun navOrderIndex(nav: String): Int = when (nav) {
                    "BERANDA" -> 0
                    "RENCANA" -> 1
                    "KELOLAAN" -> 2
                    "MONITORING" -> 3
                    "DASHBOARD" -> 4
                    "SETTING" -> 5
                    else -> 2
                }
                val sharedHeaderNavs = remember { setOf("RENCANA", "KELOLAAN", "MONITORING", "DASHBOARD") }
                fun shellModeFor(nav: String): String = when (nav) {
                    "BERANDA" -> "BERANDA"
                    "SETTING" -> "SETTING"
                    else -> "SHARED_HEADER"
                }
                fun shellOrderIndex(mode: String): Int = when (mode) {
                    "BERANDA" -> 0
                    "SHARED_HEADER" -> 2
                    "SETTING" -> 5
                    else -> 2
                }
                val activeSharedNavState = remember {
                    mutableStateOf(if (selectedBottomNav in sharedHeaderNavs) selectedBottomNav else "KELOLAAN")
                }
                if (selectedBottomNav in sharedHeaderNavs) {
                    activeSharedNavState.value = selectedBottomNav
                }
                val currentShellMode = shellModeFor(selectedBottomNav)

                AnimatedContent(
                    targetState = currentShellMode,
                    transitionSpec = {
                        val isForward = shellOrderIndex(targetState) >= shellOrderIndex(initialState)
                        (fadeIn(
                            animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                        ) + slideInHorizontally(
                            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> (if (isForward) 1 else -1) * (fullWidth / 14) }
                        )) togetherWith fadeOut(
                            animationSpec = tween(durationMillis = 100, easing = FastOutLinearInEasing)
                        ) using SizeTransform(
                            clip = false,
                            sizeAnimationSpec = { _, _ -> snap() }
                        )
                    },
                    label = "main_shell_transition"
                ) { shellMode ->
                    when (shellMode) {
                        "BERANDA" -> {
                            saveableStateHolder.SaveableStateProvider("BERANDA") {
                                BerandaScreenView(
                                    viewModel = viewModel,
                                    formatter = formatter,
                                    onOpenGlobalSearch = {
                                        globalSearchQuery = ""
                                        showGlobalSearchPopup = true
                                    },
                                    onOpenKelolaan = { viewModel.setBottomNav("KELOLAAN") },
                                    onOpenKunjungan = { viewModel.setBottomNav("RENCANA") },
                                    onOpenMonitoring = { viewModel.setBottomNav("MONITORING") },
                                    onOpenDashboard = { viewModel.setBottomNav("DASHBOARD") },
                                    onOpenLogin = { showLoginDialog = true },
                                    onOpenUpdateDialog = { viewModel.openUpdateDialog() },
                                    onOpenCustomerListPage = { request ->
                                        activeBerandaListPage = request
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        "SETTING" -> {
                            saveableStateHolder.SaveableStateProvider("SETTING") {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    SettingsScreenView(
                                        viewModel = viewModel,
                                        onOpenAnalytics = { showDetailUnitDialog = true },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.TopCenter)
                                            .statusBarsPadding()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                            .zIndex(20f)
                                    ) {
                                        AnimatedContent(
                                            targetState = showGlobalSearchPopup,
                                            transitionSpec = {
                                                val searchSpring = spring<IntOffset>(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium,
                                                    visibilityThreshold = IntOffset.VisibilityThreshold
                                                )
                                                slideInHorizontally(animationSpec = searchSpring) { it / 3 } togetherWith
                                                    slideOutHorizontally(animationSpec = searchSpring) { it / 3 } using
                                                    SizeTransform(clip = true, sizeAnimationSpec = { _, _ -> snap() })
                                            },
                                            label = "setting_header_search_transition"
                                        ) { isSearchExpanded ->
                                            if (isSearchExpanded) {
                                                HeaderPocketSearchBar(
                                                    query = globalSearchQuery,
                                                    onQueryChange = { globalSearchQuery = it },
                                                    onClose = {
                                                        showGlobalSearchPopup = false
                                                        globalSearchQuery = ""
                                                        keyboardController?.hide()
                                                    },
                                                    focusRequester = globalSearchFocusRequester,
                                                    modifier = Modifier.padding(vertical = 6.dp)
                                                )
                                            } else {
                                                Box(modifier = Modifier.fillMaxWidth()) {
                                                    HeaderPocketSearchButton(
                                                        onClick = { showGlobalSearchPopup = true },
                                                        modifier = Modifier.align(Alignment.TopEnd)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            // Shared static header for KELOLAAN, MONITORING, DASHBOARD, and RENCANA;
                            // only the content below the header slides/transitions.
                            Column(modifier = Modifier.fillMaxSize()) {
                                renderNonFullBleedHeader()

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clipToBounds()
                                ) {
                                    AnimatedContent(
                                        targetState = activeSharedNavState.value,
                                        transitionSpec = {
                                            val isForward = navOrderIndex(targetState) >= navOrderIndex(initialState)
                                            (fadeIn(
                                                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                                            ) + slideInHorizontally(
                                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                                                initialOffsetX = { fullWidth -> (if (isForward) 1 else -1) * (fullWidth / 14) }
                                            )) togetherWith fadeOut(
                                                animationSpec = tween(durationMillis = 100, easing = FastOutLinearInEasing)
                                            ) using SizeTransform(
                                                clip = false,
                                                sizeAnimationSpec = { _, _ -> snap() }
                                            )
                                        },
                                        label = "shared_header_body_transition"
                                    ) { currentNav ->
                                        saveableStateHolder.SaveableStateProvider(currentNav) {
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                if (currentNav == "DASHBOARD") {
                                                    DashboardUnitView(
                                                        viewModel = viewModel,
                                                        formatter = formatter,
                                                        onCustomerClick = { showDetailDialogForCustomer = it },
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                    if (currentNav == "MONITORING") {
                        TabsHeader(
                            selectedMenu = selectedMenu,
                            onMenuSelect = { viewModel.selectMenu(it) },
                            isMonitoringOnly = true
                        )
                    } else if (currentNav == "KELOLAAN") {
                        val kelolaanTotalOs = remember(customersList) { customersList.sumOf { it.osPinjaman } }
                        KelolaanHeaderBanner(
                            loggedInPN = loggedInPN,
                            loggedInMantriDetails = loggedInDetails,
                            totalOs = kelolaanTotalOs,
                            totalDeb = customersList.size,
                            formatter = formatter,
                            onLoginClick = { showLoginDialog = true }
                        )
                    } else if (currentNav == "RENCANA") {
                        val activeDateFull = if (filterBulanLunas == "SEMUA" || filterBulanLunas.isBlank() || filterBulanLunas.length != 10) {
                            java.text.SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(java.util.Date())
                        } else {
                            filterBulanLunas
                        }
                        RencanaHeaderBanner(
                            activeVisitDate = activeDateFull,
                            onBackClick = { viewModel.setBottomNav("BERANDA") }
                        )
                    }

            // Alert for Error/Sync Notification
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Pemberitahuan",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        if (isShowingCachedOfflineData) {
                            TextButton(
                                onClick = { viewModel.refreshSpreadsheetData(force = true) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("error_retry_sync_button")
                            ) {
                                Text(
                                    text = "Coba Lagi",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.errorMessage.value = null },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Dynamic Navigation controls (Sleek Search Input & Filter Button)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Search Input Field - Liquid circular badge & pill design (Reference Image 2)
                        TableLiquidSearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.searchQuery.value = it },
                            placeholder = if (selectedMenu == "KUNJUNGAN") "Cari nasabah Intrakomp & Ekstrakomp..." else "Cari nama, CIF, atau alamat...",
                            focusRequester = searchFocusRequester,
                            onClearClick = {
                                viewModel.searchQuery.value = ""
                                coroutineScope.launch {
                                    runCatching { searchFocusRequester.requestFocus() }
                                    keyboardController?.show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Filter toggle button aligned cleanly with the 34.dp pill bar
                        Box(
                            modifier = Modifier.height(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    if (selectedMenu == "SIMPANAN") {
                                        showSimpananFilterBottomSheet = true
                                    } else {
                                        showFilterPanel = !showFilterPanel
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Tampilkan saringan",
                                    tint = if (selectedMenu == "SIMPANAN") {
                                        if (showSimpananFilterBottomSheet) Color(0xFFFF7043) else Color(0xFF005FAF)
                                    } else {
                                        if (showFilterPanel) Color(0xFFFF7043) else Color(0xFF005FAF)
                                    },
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    // Popup Search Results for KUNJUNGAN menu
                    var exitingSearchKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
                    LaunchedEffect(searchQuery, selectedMenu) {
                        if (selectedMenu != "KUNJUNGAN" || searchQuery.isBlank()) {
                            exitingSearchKeys = emptySet()
                        }
                    }

                    val matchedCustomers = remember(searchQuery, allCustomers, kunjunganKeys, exitingSearchKeys, selectedMenu) {
                        if (selectedMenu == "KUNJUNGAN" && searchQuery.isNotBlank()) {
                            val q = searchQuery.trim().lowercase()
                            allCustomers
                                .asSequence()
                                .filter {
                                    it.namaNasabah.lowercase().contains(q) ||
                                    it.cif.lowercase().contains(q) ||
                                    it.alamat.lowercase().contains(q)
                                }
                                .distinctBy { rencanaCustomerKey(it) }
                                .filter { customer ->
                                    val key = rencanaCustomerKey(customer)
                                    key in exitingSearchKeys || key !in kunjunganKeys
                                }
                                .take(10)
                                .toList()
                        } else {
                            emptyList()
                        }
                    }

                    if (selectedMenu == "KUNJUNGAN" && matchedCustomers.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 38.dp)
                                .shadow(6.dp, RoundedCornerShape(12.dp))
                                .zIndex(10f),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD5E3F5))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 250.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp)
                            ) {
                                matchedCustomers.forEachIndexed { index, customer ->
                                    val custKey = rencanaCustomerKey(customer)
                                    val isExiting = custKey in exitingSearchKeys
                                    val isEkstrakomp = customer.menuType.equals("EKSTRAKOMP", ignoreCase = true)
                                    val cifText = customer.cif.trim()
                                    val kolekText = if (isEkstrakomp) "DH" else getShortKolekLabel(customer.kolek).trim()
                                    val (kolekBgColor, kolekTextColor) = resolveRencanaSearchKolekColors(kolekText)
                                    val mantriText = cleanMantriNameOnly(customer.namaMantri).trim()
                                    val displayAlamat = customer.alamat.trim()

                                    val onAddCustomer = {
                                        if (!isExiting) {
                                            if (viewModel.isPastDate(activeVisitDateStr)) {
                                                Toast.makeText(
                                                    context,
                                                    "Tidak dapat menambah rencana pada tanggal yang sudah berlalu",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                exitingSearchKeys = exitingSearchKeys + custKey
                                                viewModel.addKunjungan(customer)
                                                coroutineScope.launch {
                                                    delay(280)
                                                    exitingSearchKeys = exitingSearchKeys - custKey
                                                }
                                            }
                                        }
                                    }

                                    key(custKey) {
                                        AnimatedVisibility(
                                            visible = !isExiting,
                                            exit = slideOutHorizontally(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    visibilityThreshold = IntOffset.VisibilityThreshold
                                                )
                                            ) { fullWidth -> fullWidth / 2 } +
                                                shrinkVertically(
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable(onClick = onAddCustomer)
                                                        .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
                                                        .testTag("rencana_search_item_$index"),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(end = 6.dp),
                                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        @OptIn(ExperimentalLayoutApi::class)
                                                        FlowRow(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            Text(
                                                                text = customer.namaNasabah,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF001C3B),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.align(Alignment.CenterVertically)
                                                            )
                                                            if (cifText.isNotBlank()) {
                                                                RencanaSearchBadge(
                                                                    text = cifText,
                                                                    modifier = Modifier.align(Alignment.CenterVertically)
                                                                )
                                                            }
                                                            if (kolekText.isNotBlank()) {
                                                                RencanaSearchBadge(
                                                                    text = kolekText,
                                                                    bgColor = kolekBgColor,
                                                                    textColor = kolekTextColor,
                                                                    modifier = Modifier.align(Alignment.CenterVertically)
                                                                )
                                                            }
                                                            if (mantriText.isNotBlank()) {
                                                                RencanaSearchBadge(
                                                                    text = mantriText,
                                                                    modifier = Modifier.align(Alignment.CenterVertically)
                                                                )
                                                            }
                                                        }
                                                        if (displayAlamat.isNotBlank()) {
                                                            Text(
                                                                text = displayAlamat,
                                                                fontSize = 10.5.sp,
                                                                color = Color(0xFF64748B),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = onAddCustomer,
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .testTag("rencana_search_add_button_$index")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = "Tambah ke Kunjungan",
                                                            tint = Color(0xFF005FAF),
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                                HorizontalDivider(color = Color(0xFFEEF4FC), thickness = 1.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Expanded filter options panel
                AnimatedVisibility(
                    visible = showFilterPanel && selectedMenu != "SIMPANAN",
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
                            .padding(top = 10.dp)
                            .background(Color(0xFFF4F7FB), shape = RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFD5E3F5), shape = RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val listBulan = listOf("SEMUA", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12")
                        val monthMapping = mapOf(
                            "SEMUA" to "Bulan Lunas",
                            "01" to "Januari",
                            "02" to "Februari",
                            "03" to "Maret",
                            "04" to "April",
                            "05" to "Mei",
                            "06" to "Juni",
                            "07" to "Juli",
                            "08" to "Agustus",
                            "09" to "September",
                            "10" to "Oktober",
                            "11" to "November",
                            "12" to "Desember"
                        )

                        val hasActiveFilters = searchQuery.isNotBlank() ||
                                (selectedMenu != "KELOLAAN" && selectedMenu != "KUNJUNGAN" && filterMantri != "SEMUA") ||
                                filterBulanLunas != "SEMUA" ||
                                filterTahunLunas != "SEMUA" ||
                                filterOS != "SEMUA" ||
                                filterPlafondRange != "SEMUA" ||
                                filterTenorRange != "SEMUA" ||
                                filterKolek != "SEMUA" ||
                                filterKolekUpdate != "SEMUA" ||
                                filterKolekBlnLalu != "SEMUA" ||
                                filterSegmen != "SEMUA" ||
                                filterNpd != "SEMUA" ||
                                filterDesa != "SEMUA" ||
                                filterAlamat.isNotBlank() ||
                                filterSetorPokok ||
                                filterTusim ||
                                filterTucil ||
                                filterTunggakanKurang6Bln
 
                        // 1. Filter Mantri OR 3 Toggle Switches + Desa (If Kelolaan Menu)
                        if (selectedMenu == "KELOLAAN") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ToggleFilterSwitch(
                                    label = "Setor Pokok",
                                    checked = filterSetorPokok,
                                    onCheckedChange = { viewModel.filterSetorPokok.value = it },
                                    modifier = Modifier.weight(1f)
                                )
                                ToggleFilterSwitch(
                                    label = "Tusim",
                                    checked = filterTusim,
                                    onCheckedChange = { 
                                        viewModel.filterTusim.value = it 
                                        if (it) {
                                            showTusimPopup = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ToggleFilterSwitch(
                                    label = "Tucil",
                                    checked = filterTucil,
                                    onCheckedChange = { viewModel.filterTucil.value = it },
                                    modifier = Modifier.weight(1f)
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    SleekFilterDropdown(
                                        selectedValue = filterDesa,
                                        options = availableDesas,
                                        displayOption = { if (it == "SEMUA") "Desa" else it },
                                        onSelect = { viewModel.filterDesa.value = it }
                                    )
                                }
                            }
                        } else if (selectedMenu == "KUNJUNGAN") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val mName = if (loggedInDetails.isNotBlank()) {
                                            if (loggedInDetails.contains(" - ")) {
                                                loggedInDetails.substringAfter(" - ").trim()
                                            } else {
                                                loggedInDetails.trim()
                                            }
                                        } else {
                                            "Nurul Mubin"
                                        }
                                        val message = generateAktivitasMessage(
                                            isRencana = true, 
                                            mantriName = mName, 
                                            customers = customersList, 
                                            tindakLanjutMap = tindakLanjutMap,
                                            customTemplate = templateWaRencana,
                                            includeOs = includeOsInRencanaWa
                                        )
                                        sendWhatsAppGroupMessage(context, message)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0288D1)
                                    )
                                ) {
                                    Text("Rencana Aktivitas", color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        val mName = if (loggedInDetails.isNotBlank()) {
                                            if (loggedInDetails.contains(" - ")) {
                                                loggedInDetails.substringAfter(" - ").trim()
                                            } else {
                                                loggedInDetails.trim()
                                            }
                                        } else {
                                            "Nurul Mubin"
                                        }
                                        val message = generateAktivitasMessage(
                                            isRencana = false, 
                                            mantriName = mName, 
                                            customers = customersList, 
                                            tindakLanjutMap = tindakLanjutMap,
                                            customTemplate = templateWaRencana,
                                            includeOs = includeOsInRencanaWa
                                        )
                                        sendWhatsAppGroupMessage(context, message)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E7D32)
                                    )
                                ) {
                                    Text("Hasil Aktivitas", color = Color.White)
                                }
                            }
                        } else {
                            // 1. Filter Mantri (1 Kolom Memanjang)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                SleekFilterDropdown(
                                    selectedValue = filterMantri,
                                    options = availableMantris,
                                    displayOption = { if (it == "SEMUA") "Semua Mantri" else if (it.isBlank()) "Blank PN" else it },
                                    onSelect = { viewModel.filterMantri.value = it }
                                )
                            }
                        }

                        // 2. Filter Bulan lunas, filter tahun, filter Tgl NPD, filter Kolek (4 kolom)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (selectedMenu == "KUNJUNGAN") {
                                    androidx.compose.material3.Button(
                                        onClick = { datePickerDialog.show() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (filterBulanLunas != "SEMUA") Color(0xFFF57C00) else Color(0xFF005FAF),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth().height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (filterBulanLunas == "SEMUA") "Tanggal" else filterBulanLunas,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else if (selectedMenu == "EKSTRAKOMP") {
                                    SleekFilterDropdown(
                                        selectedValue = filterBulanLunas,
                                        options = availableYearsPh,
                                        displayOption = { if (it == "SEMUA") "Tahun PH" else it },
                                        onSelect = { viewModel.filterBulanLunas.value = it }
                                    )
                                } else {
                                    SleekFilterDropdown(
                                        selectedValue = filterBulanLunas,
                                        options = listBulan,
                                        displayOption = { monthMapping[it] ?: "Bulan Lunas" },
                                        onSelect = { viewModel.setFilterBulanLunas(it) }
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                if (selectedMenu == "KUNJUNGAN") {
                                    var expandedDesa by remember { mutableStateOf(false) }
                                    val isDesaSelected = filterTahunLunas != "SEMUA"
                                    androidx.compose.material3.Button(
                                        onClick = { expandedDesa = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDesaSelected) Color(0xFFFF7043) else Color(0xFF005FAF),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth().height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (filterTahunLunas == "SEMUA") "Desa" else filterTahunLunas,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = expandedDesa,
                                        onDismissRequest = { expandedDesa = false },
                                        modifier = Modifier
                                            .background(Color.White)
                                            .heightIn(max = 240.dp)
                                    ) {
                                        availableDesas.forEach { option ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = if (option == "SEMUA") "Semua Desa" else option,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (option == filterTahunLunas) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (option == filterTahunLunas) Color(0xFF005FAF) else Color.DarkGray
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.filterTahunLunas.value = option
                                                    expandedDesa = false
                                                }
                                            )
                                        }
                                    }
                                } else if (selectedMenu == "EKSTRAKOMP") {
                                    SleekFilterDropdown(
                                        selectedValue = filterTahunLunas,
                                        options = availableYearsRealisasi,
                                        displayOption = { if (it == "SEMUA") "Tahun Real" else it },
                                        onSelect = { viewModel.filterTahunLunas.value = it }
                                    )
                                } else {
                                    SleekFilterDropdown(
                                        selectedValue = filterTahunLunas,
                                        options = availableYears,
                                        displayOption = { if (it == "SEMUA") "Tahun Lunas" else it },
                                        onSelect = { viewModel.filterTahunLunas.value = it }
                                    )
                                }
                            }
                            if (selectedMenu == "KELOLAAN" || selectedMenu == "INTRAKOMP") {
                                Box(modifier = Modifier.weight(1f)) {
                                    val filteredNpdList = remember(availableNpds) {
                                        availableNpds.filter { it != "SEMUA" }
                                    }
                                    SleekFilterDropdown(
                                        selectedValue = filterNpd,
                                        options = listOf("SEMUA", "PILIH_BEBERAPA") + filteredNpdList,
                                        displayOption = { opt ->
                                            when {
                                                opt == "SEMUA" -> "Tgl NPD"
                                                opt == "PILIH_BEBERAPA" -> "Pilih Beberapa..."
                                                opt.startsWith("MULTI:") -> {
                                                    val count = opt.substringAfter("MULTI:").split(",").filter { it.isNotEmpty() }.size
                                                    "NPD: $count Tgl"
                                                }
                                                else -> "NPD: $opt"
                                            }
                                        },
                                        onSelect = { opt ->
                                            if (opt == "PILIH_BEBERAPA") {
                                                showNpdMultiSelectDialog = true
                                            } else {
                                                viewModel.filterNpd.value = opt
                                            }
                                        }
                                    )
                                }
                            }
                            if (selectedMenu == "KUNJUNGAN") {
                                Box(modifier = Modifier.weight(1f)) {
                                    var expandedKolek by remember { mutableStateOf(false) }
                                    val isKolekSelected = filterKolek != "SEMUA"
                                    val kolekBgColor = if (isKolekSelected) Color(0xFFFF7043) else Color(0xFF005FAF)

                                    androidx.compose.material3.Button(
                                        onClick = { expandedKolek = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = kolekBgColor,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth().height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (filterKolek == "SEMUA" || filterKolek == "SEMUA_KELOLAAN") "Semua Kolek" else filterKolek,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = expandedKolek,
                                        onDismissRequest = { expandedKolek = false },
                                        modifier = Modifier
                                            .background(Color.White)
                                            .heightIn(max = 240.dp)
                                    ) {
                                        val options = listOf(
                                            "SEMUA" to "Daftar Rencana (Default)",
                                            "SEMUA_KELOLAAN" to "Semua Kolek (Kelolaan)",
                                            "Lancar" to "Lancar",
                                            "SML1" to "SML1",
                                            "SML2" to "SML2",
                                            "SML3" to "SML3",
                                            "Kurang Lancar" to "Kurang Lancar",
                                            "Diragukan" to "Diragukan",
                                            "Macet" to "Macet"
                                        )
                                        options.forEach { (optionKey, optionLabel) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = optionLabel,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (optionKey == filterKolek) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (optionKey == filterKolek) Color(0xFF005FAF) else Color.DarkGray
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.filterKolek.value = optionKey
                                                    expandedKolek = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                if (selectedMenu == "KUNJUNGAN") {
                                    var showDeleteKunjunganWarning by remember { mutableStateOf(false) }
 
                                    androidx.compose.material3.Button(
                                        onClick = { showDeleteKunjunganWarning = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFD32F2F),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth().height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus",
                                                modifier = Modifier.size(13.dp),
                                                tint = Color.White
                                            )
                                            Text(
                                                text = "Hapus",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
 
                                    if (showDeleteKunjunganWarning) {
                                        AlertDialog(
                                            onDismissRequest = { showDeleteKunjunganWarning = false },
                                            title = {
                                                Text(
                                                    "Konfirmasi Hapus",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                                                )
                                            },
                                            text = {
                                                Text(
                                                    "Apakah Anda yakin ingin menghapus seluruh daftar kunjungan?",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            confirmButton = {
                                                TextButton(
                                                    onClick = {
                                                        viewModel.clearAllKunjungan()
                                                        showDeleteKunjunganWarning = false
                                                    }
                                                ) {
                                                    Text("Hapus Semua", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showDeleteKunjunganWarning = false }) {
                                                    Text("Batal", color = Color.Gray, fontWeight = FontWeight.Bold)
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            containerColor = Color.White
                                        )
                                    }
                                } else if (selectedMenu == "EKSTRAKOMP") {
                                    SleekFilterDropdown(
                                        selectedValue = filterSegmen,
                                        options = availableSegmensEkstrakomp,
                                        displayOption = { if (it == "SEMUA") "Segmen" else it },
                                        onSelect = { viewModel.filterSegmen.value = it }
                                    )
                                } else {
                                    SleekFilterDropdown(
                                        selectedValue = filterKolek,
                                        options = listOf("SEMUA", "Lancar", "SML1", "SML2", "SML3", "Kurang Lancar", "Diragukan", "Macet"),
                                        displayOption = { if (it == "SEMUA") "Semua Kolek" else it },
                                        onSelect = { viewModel.filterKolek.value = it }
                                    )
                                }
                            }
                        }

                        if (selectedMenu != "KUNJUNGAN") {
                            // 3. Filter OS, Filter Plafond, Filter Tenor, Geser Kolek (4 Kolom sejajar secara horizontal untuk Kelolaan & Intrakomp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (selectedMenu == "EKSTRAKOMP") {
                                        SleekFilterDropdown(
                                            selectedValue = filterOS,
                                            options = listOf("SEMUA", "< 1 Juta", "< 5 Juta (1-5 jt)", "< 10 Juta (>5-10 jt)", "< 25 Juta (>10-25 jt)", "< 50 Juta (>25-50 Jt)", "< 100 Juta (>50-100 jt)", "> 100 Juta"),
                                            displayOption = { if (it == "SEMUA") "Sisa Pokok" else it },
                                            onSelect = { viewModel.filterOS.value = it }
                                        )
                                    } else {
                                        SleekFilterDropdown(
                                            selectedValue = filterOS,
                                            options = listOf("SEMUA", "<25%", "<50%", "<75%"),
                                            displayOption = { if (it == "SEMUA") "Semua OS" else it },
                                            onSelect = { viewModel.filterOS.value = it }
                                        )
                                    }
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    SleekFilterDropdown(
                                        selectedValue = filterPlafondRange,
                                        options = listOf("SEMUA", "s.d 25 Juta", ">25 - 50 Juta", ">50 - 100 Juta", ">100 Juta"),
                                        displayOption = { if (it == "SEMUA") "Semua Plafond" else it },
                                        onSelect = { viewModel.filterPlafondRange.value = it }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    SleekFilterDropdown(
                                        selectedValue = filterTenorRange,
                                        options = listOf("SEMUA", "<6 Bulan", "<12 Bulan", "> 12 Bulan", "1/2 Jalan"),
                                        displayOption = { if (it == "SEMUA") "Semua Tenor" else it },
                                        onSelect = { viewModel.filterTenorRange.value = it }
                                    )
                                }
                                if (selectedMenu == "KELOLAAN" || selectedMenu == "INTRAKOMP") {
                                    Box(modifier = Modifier.weight(1f)) {
                                        val currentKolekUpdateValue = when (filterKolekUpdate) {
                                            "L ➔ SML1", "L-SML1" -> "L -> SML1"
                                            "SML1 ➔ SML2", "Sml1-sml2", "SML1-SML2" -> "SML1 -> SML2"
                                            "SML2 ➔ SML3", "Sml2-sml3", "SML2-SML3" -> "SML2 -> SML3"
                                            "SML3 ➔ KL", "Sml3-kl", "SML3-KL" -> "SML3 -> KL"
                                            "KL ➔ D", "Kl-D", "KL-D" -> "KL -> D"
                                            "D ➔ M", "D-M" -> "D -> M"
                                            else -> filterKolekUpdate
                                        }
                                        SleekFilterDropdown(
                                            selectedValue = currentKolekUpdateValue,
                                            options = listOf(
                                                "SEMUA",
                                                "L -> SML1",
                                                "SML1 -> SML2",
                                                "SML2 -> SML3",
                                                "SML3 -> KL",
                                                "KL -> D",
                                                "D -> M",
                                                "TETAP SML",
                                                "BERGESER NPL"
                                            ),
                                            displayOption = { if (it == "SEMUA") "Geser Kolek" else it },
                                            onSelect = { selectedValue ->
                                                viewModel.filterKolekUpdate.value = selectedValue
                                            }
                                        )
                                    }
                                }
                            }

                            // 4. Reset Filter button (Dynamic button color depending on active filters)
                            Button(
                                onClick = {
                                    viewModel.searchQuery.value = ""
                                    viewModel.filterKolek.value = "SEMUA"
                                    viewModel.filterKolekUpdate.value = "SEMUA"
                                    viewModel.filterKolekBlnLalu.value = "SEMUA"
                                    viewModel.filterMantri.value = "SEMUA"
                                    viewModel.setFilterBulanLunas("SEMUA")
                                    viewModel.filterTahunLunas.value = "SEMUA"
                                    viewModel.filterOS.value = "SEMUA"
                                    viewModel.filterPlafondRange.value = "SEMUA"
                                    viewModel.filterTenorRange.value = "SEMUA"
                                    viewModel.filterSegmen.value = "SEMUA"
                                    viewModel.filterNpd.value = "SEMUA"
                                    viewModel.filterAlamat.value = ""
                                    viewModel.filterSetorPokok.value = false
                                    viewModel.filterTusim.value = false
                                    viewModel.filterTucil.value = false
                                    viewModel.filterTunggakanKurang6Bln.value = false
                                    viewModel.filterDesa.value = "SEMUA"
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hasActiveFilters) Color(0xFFD32F2F) else Color.White,
                                    contentColor = if (hasActiveFilters) Color.White else Color(0xFFD32F2F)
                                ),
                                border = if (!hasActiveFilters) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F)) else null,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                        text = "RESET FILTER",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
                                    )
                            }
                        }
                    }
                }
            }

            // Quick reset button if any filters are active
            val hasActiveFilters = selectedMenu != "SIMPANAN" && (searchQuery.isNotEmpty() ||
                    filterKolek != "SEMUA" ||
                    filterKolekUpdate != "SEMUA" ||
                    (selectedMenu != "KELOLAAN" && filterMantri != "SEMUA") ||
                    filterBulanLunas != "SEMUA" ||
                    filterTahunLunas != "SEMUA" ||
                    filterOS != "SEMUA" ||
                    filterPlafondRange != "SEMUA" ||
                    filterTenorRange != "SEMUA" ||
                    filterSegmen != "SEMUA" ||
                    filterNpd != "SEMUA" ||
                    filterDesa != "SEMUA" ||
                    filterAlamat.isNotEmpty() ||
                    filterSetorPokok ||
                    filterTusim ||
                    filterTucil ||
                    filterTunggakanKurang6Bln)

            if (hasActiveFilters && !showFilterPanel) {
                Text(
                    text = if (filterTunggakanKurang6Bln) "Reset Semua Filter (Tunggakan <6 Bln)" else "Reset Semua Filter",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(start = 16.dp, bottom = 8.dp)
                        .clickable {
                            viewModel.searchQuery.value = ""
                            viewModel.filterKolek.value = "SEMUA"
                            viewModel.filterKolekUpdate.value = "SEMUA"
                            viewModel.filterKolekBlnLalu.value = "SEMUA"
                            viewModel.filterMantri.value = "SEMUA"
                            viewModel.setFilterBulanLunas("SEMUA")
                            viewModel.filterTahunLunas.value = "SEMUA"
                            viewModel.filterOS.value = "SEMUA"
                            viewModel.filterPlafondRange.value = "SEMUA"
                            viewModel.filterTenorRange.value = "SEMUA"
                            viewModel.filterSegmen.value = "SEMUA"
                            viewModel.filterNpd.value = "SEMUA"
                            viewModel.filterDesa.value = "SEMUA"
                            viewModel.filterAlamat.value = ""
                            viewModel.filterSetorPokok.value = false
                            viewModel.filterTusim.value = false
                            viewModel.filterTucil.value = false
                            viewModel.filterTunggakanKurang6Bln.value = false
                        }
                )
            }

            // Responsive Data Table component
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if ((selectedMenu == "KELOLAAN" || selectedMenu == "KUNJUNGAN") && loggedInPN.isBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Akses Terbatas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Silakan login menggunakan PN Mantri Anda untuk melihat data Kelolaan atau Kunjungan.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.Button(onClick = { showLoginDialog = true }) {
                            Text("Login PN Mantri")
                        }
                    }
                } else if (isRefreshing && selectedMenu != "KUNJUNGAN") {
                    CustomerTableSkeleton(
                        selectedMenu = selectedMenu,
                        loggedInPN = loggedInPN
                    )
                } else if (selectedMenu == "SIMPANAN") {
                    if (simpananList.isEmpty()) {
                        EmptyState()
                    } else {
                        SimpananTable(
                            simpananList = simpananList,
                            formatter = formatter,
                            sortCol = simpananSortColumn,
                            sortDir = simpananSortDirection,
                            onSortClick = { viewModel.toggleSimpananSort(it) }
                        )
                    }
                } else if (customersList.isEmpty() && selectedMenu != "KUNJUNGAN") {
                    EmptyState()
                } else if (selectedMenu == "KUNJUNGAN") {
                    RencanaCustomerCardList(
                        customersList = customersList,
                        onCustomerClick = { showDetailDialogForCustomer = it },
                        onSaveTindakLanjut = { cust, tl -> viewModel.saveTindakLanjut(cust, tl, isKunjungan = true) },
                        onDeleteTindakLanjut = { cust -> viewModel.deleteTindakLanjut(cust, isKunjungan = true) },
                        sortCol = sortCol,
                        sortDir = sortDir,
                        onSelectSort = { viewModel.selectSortColumn(it) },
                        formatter = formatter,
                        tindakLanjutMap = tindakLanjutMap,
                        dailyDataMap = dailyDataMap
                    )
                } else {
                    CustomerTable(
                        customersList = customersList,
                        selectedMenu = selectedMenu,
                        sortCol = sortCol,
                        sortDir = sortDir,
                        onHeaderClick = { viewModel.toggleSort(it) },
                        onCustomerClick = { showDetailDialogForCustomer = it },
                        formatter = formatter,
                        kunjunganKeys = kunjunganKeys,
                        onToggleKunjungan = { viewModel.toggleKunjungan(it) },
                        onShowTindakLanjutPopup = { showTindakLanjutPopupForCustomer = it },
                        tindakLanjutMap = tindakLanjutMap,
                        dueTodayCount = dueTodayCount,
                        dueHCounts = dueHCounts,
                        selectedJatuhTempoH = selectedJatuhTempoH,
                        filterJatuhTempoOnly = filterJatuhTempoOnly,
                        onSelectJatuhTempoH = { h ->
                            viewModel.selectedJatuhTempoH.value = h
                            viewModel.filterJatuhTempoOnly.value = true
                        },
                        onToggleJatuhTempoOnly = { viewModel.filterJatuhTempoOnly.value = !filterJatuhTempoOnly },
                        loggedInPN = loggedInPN,
                        onResetBayarFull = { viewModel.resetBayarFullTindakLanjut() },
                        dailyDataMap = dailyDataMap
                    )
                }
            }
        }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

    androidx.compose.animation.AnimatedVisibility(
        visible = showGlobalSearchPopup,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntOffset.VisibilityThreshold
            )
        ) { -it / 5 } + fadeIn(
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        ),
        exit = slideOutVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
                visibilityThreshold = IntOffset.VisibilityThreshold
            )
        ) { -it / 5 } + fadeOut(
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        ),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .zIndex(99f)
    ) {
        if (isBerandaScreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showGlobalSearchPopup = false
                        globalSearchQuery = ""
                    }
                    .statusBarsPadding()
                    .padding(top = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                ) {
                    GlobalSearchPopup(
                        query = globalSearchQuery,
                        onQueryChange = { globalSearchQuery = it },
                        allCustomers = allCustomers,
                        formatter = formatter,
                        onCustomerClick = { customer ->
                            showDetailDialogForCustomer = customer
                        },
                        onDismiss = {
                            showGlobalSearchPopup = false
                            globalSearchQuery = ""
                        },
                        showInputField = true,
                        focusRequester = globalSearchFocusRequester
                    )
                }
            }
        } else if (isSettingScreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showGlobalSearchPopup = false
                        globalSearchQuery = ""
                        keyboardController?.hide()
                    }
                    .statusBarsPadding()
                    .padding(top = 52.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                ) {
                    GlobalSearchPopup(
                        query = globalSearchQuery,
                        onQueryChange = { globalSearchQuery = it },
                        allCustomers = allCustomers,
                        formatter = formatter,
                        onCustomerClick = { customer ->
                            showDetailDialogForCustomer = customer
                        },
                        onDismiss = {
                            showGlobalSearchPopup = false
                            globalSearchQuery = ""
                            keyboardController?.hide()
                        },
                        showInputField = false
                    )
                }
            }
        } else {
            GlobalSearchPopup(
                query = globalSearchQuery,
                onQueryChange = { globalSearchQuery = it },
                allCustomers = allCustomers,
                formatter = formatter,
                onCustomerClick = { customer ->
                    showDetailDialogForCustomer = customer
                },
                onDismiss = {
                    showGlobalSearchPopup = false
                    globalSearchQuery = ""
                },
                showInputField = false
            )
        }
    }
}
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = activeBerandaListPage != null,
        enter = slideInHorizontally(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntOffset.VisibilityThreshold
            ),
            initialOffsetX = { fullWidth -> fullWidth }
        ) + fadeIn(
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        ),
        exit = slideOutHorizontally(
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            targetOffsetX = { fullWidth -> fullWidth }
        ) + fadeOut(
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        ),
        modifier = Modifier
            .fillMaxSize()
            .zIndex(50f)
    ) {
        displayedBerandaListPage?.let { pageRequest ->
            BerandaFilteredCustomerListPage(
                request = pageRequest,
                formatter = formatter,
                onBack = { activeBerandaListPage = null },
                onCustomerClick = { customer ->
                    showDetailDialogForCustomer = customer
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showSimpananFilterBottomSheet) {
        val simpananSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val simpananSheetScope = rememberCoroutineScope()
        val density = LocalDensity.current
        val initialSlidePx = remember(density) { with(density) { 44.dp.toPx() } }
        val exitSlidePx = remember(density) { with(density) { 56.dp.toPx() } }
        val sheetContentAlpha = remember { Animatable(0f) }
        val sheetContentOffsetY = remember { Animatable(initialSlidePx) }
        var isSimpananDismissing by remember { mutableStateOf(false) }
        var simpananHasBeenVisible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            launch {
                sheetContentAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                )
            }
            launch {
                sheetContentOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

        LaunchedEffect(simpananSheetState.currentValue, simpananSheetState.targetValue) {
            if (simpananSheetState.currentValue != SheetValue.Hidden) {
                simpananHasBeenVisible = true
            } else if (simpananHasBeenVisible && simpananSheetState.targetValue == SheetValue.Hidden && !isSimpananDismissing) {
                launch {
                    sheetContentAlpha.animateTo(0f, tween(durationMillis = 180, easing = FastOutSlowInEasing))
                }
                launch {
                    sheetContentOffsetY.animateTo(exitSlidePx, tween(durationMillis = 200, easing = FastOutSlowInEasing))
                }
            }
        }

        val dismissSimpananSheet: () -> Unit = {
            if (!isSimpananDismissing) {
                isSimpananDismissing = true
                simpananSheetScope.launch {
                    val fadeJob = launch {
                        sheetContentAlpha.animateTo(0f, tween(durationMillis = 190, easing = FastOutSlowInEasing))
                    }
                    val slideJob = launch {
                        sheetContentOffsetY.animateTo(exitSlidePx, tween(durationMillis = 210, easing = FastOutSlowInEasing))
                    }
                    try {
                        simpananSheetState.hide()
                        fadeJob.join()
                        slideJob.join()
                        showSimpananFilterBottomSheet = false
                    } catch (_: CancellationException) {
                        // Sheet disposed externally
                    }
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = {
                if (!isSimpananDismissing) {
                    isSimpananDismissing = true
                    showSimpananFilterBottomSheet = false
                }
            },
            sheetState = simpananSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .graphicsLayer {
                        alpha = sheetContentAlpha.value
                        translationY = sheetContentOffsetY.value
                    }
            ) {
                // Header of Bottom Sheet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Filter Kode 1 - Simpanan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF005FAF)
                        )
                        Text(
                            text = "Diurutkan berdasarkan CIF & Saldo Terbesar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = dismissSimpananSheet) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val filteredAndSortedListWithKode2 = remember(simpananList) {
                    val sorted = simpananList
                        .filter { it.kode1 == "1" }
                        .sortedWith(
                            compareBy<com.example.data.model.SimpananRow> {
                                com.example.ui.utils.ExcelUtils.normalizeCif(it.cif)
                            }.thenByDescending { it.balance }
                        )
                    val counters = mutableMapOf<String, Int>()
                    sorted.map { row ->
                        val normCif = com.example.ui.utils.ExcelUtils.normalizeCif(row.cif)
                        val currentCount = counters.getOrDefault(normCif, 0) + 1
                        counters[normCif] = currentCount
                        Pair(row, currentCount)
                    }
                }
                
                if (filteredAndSortedListWithKode2.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tidak ada nasabah dengan Kode 1",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCCB0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Column Headers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF005FAF))
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "NAMA NASABAH",
                                    modifier = Modifier.weight(1.6f),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "CIF",
                                    modifier = Modifier.weight(1.2f),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "NO TABUNGAN",
                                    modifier = Modifier.weight(1.4f),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "SALDO",
                                    modifier = Modifier.weight(1.4f),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.End,
                                    color = Color.White
                                )
                                Text(
                                    text = "KODE 2",
                                    modifier = Modifier.weight(0.8f),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center,
                                    color = Color.White
                                )
                            }
                            
                            HorizontalDivider(color = Color(0xFFFFCCB0))
                            
                            val simpananListState = rememberLazyListState()
                            val pagedSimpananList = rememberLazyPagedItems(
                                items = filteredAndSortedListWithKode2,
                                listState = simpananListState,
                                pageSize = 30,
                                prefetchDistance = 6
                            )
                            LazyColumn(
                                state = simpananListState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(
                                    items = pagedSimpananList.visibleItems,
                                    key = { index, pair -> "${pair.first.cif}_${pair.first.noTabungan}_$index" }
                                ) { index, pair ->
                                    val row = pair.first
                                    val kode2 = pair.second
                                    val rowBg = if (index % 2 == 0) Color(0xFFEEF4FC) else Color(0xFFD5E3F5)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem()
                                            .background(rowBg)
                                            .padding(vertical = 8.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = row.namaNasabah.uppercase(),
                                            modifier = Modifier.weight(1.6f),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                            color = Color(0xFF001C3B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = row.cif,
                                            modifier = Modifier.weight(1.2f),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                                            color = Color(0xFF001C3B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = row.noTabungan,
                                            modifier = Modifier.weight(1.4f),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                                            color = Color(0xFF001C3B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%,.0f", row.balance),
                                            modifier = Modifier.weight(1.4f),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                            textAlign = TextAlign.End,
                                            color = Color(0xFF001C3B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = kode2.toString(),
                                            modifier = Modifier.weight(0.8f),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                            textAlign = TextAlign.Center,
                                            color = Color(0xFF001C3B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (index < pagedSimpananList.visibleItems.lastIndex) {
                                        HorizontalDivider(color = Color(0xFFFFCCB0).copy(alpha = 0.3f))
                                    }
                                }

                                if (pagedSimpananList.hasMore) {
                                    item(key = "simpanan_sheet_lazy_footer") {
                                        LazyLoadingFooterRow(
                                            visibleCount = pagedSimpananList.visibleItems.size,
                                            totalCount = pagedSimpananList.totalCount
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog Sheet for Customers
    // Detail Dialog Sheet for Customers
    if (showDetailUnitDialog) {
        @Composable
        fun DetailUnitDashboard(
            customersList: List<CustomerEntity>,
            dailyDataMap: Map<String, CustomerEntity>,
            onDismiss: () -> Unit
        ) {
            val intrakompCustomers = remember(customersList) { customersList.filter { it.menuType == "INTRAKOMP" } }
            
            val blnLaluSummary by viewModel.summaryBulanLalu.collectAsState()
            val akhirBlnSummary by viewModel.summaryAkhirBulan.collectAsState()
            val dailySummary by viewModel.summaryDaily.collectAsState()

            val kolekSummaryBulanLalu by viewModel.kolekSummaryBulanLalu.collectAsState()
            val kolekSummaryAkhirBulan by viewModel.kolekSummaryAkhirBulan.collectAsState()
            val kolekSummaryDaily by viewModel.kolekSummaryDaily.collectAsState()

            val mantriBL by viewModel.mantriSummaryBulanLalu.collectAsState()
            val mantriAB by viewModel.mantriSummaryAkhirBulan.collectAsState()
            val mantriDaily by viewModel.mantriSummaryDaily.collectAsState()

            val blnLaluDate by viewModel.periodDateBulanLalu.collectAsState()
            val akhirBlnDate by viewModel.periodDateAkhirBulan.collectAsState()
            val dailyDate by viewModel.periodDateDaily.collectAsState()

            val blnLaluCustomersList by viewModel.bulanLaluCustomers.collectAsState()
            val akhirBulanCustomersList by viewModel.akhirBulanCustomers.collectAsState()
            var showDetailRealizedPopup by remember { mutableStateOf<Pair<String, List<RealizedCustomerItem>>?>(null) }

            val (blnLookup, blnNameMap, blnNameCount) = remember(blnLaluCustomersList, akhirBulanCustomersList) {
                val map = mutableMapOf<String, CustomerEntity>()
                val nameMap = mutableMapOf<String, CustomerEntity>()
                val nameCount = mutableMapOf<String, Int>()

                fun indexCustomer(bCust: CustomerEntity, overwrite: Boolean = false) {
                    val rawRek = bCust.noRekening.trim()
                    if (rawRek.isNotBlank() && (overwrite || !map.containsKey(rawRek))) map[rawRek] = bCust
                    val cr = rawRek.filter { it.isDigit() }
                    if (cr.isNotBlank()) {
                        if (overwrite || !map.containsKey(cr)) map[cr] = bCust
                        val noZero = cr.trimStart('0')
                        if (noZero.isNotBlank() && (overwrite || !map.containsKey(noZero))) map[noZero] = bCust
                        if (cr.length >= 15 && (overwrite || !map.containsKey(cr.takeLast(15)))) map[cr.takeLast(15)] = bCust
                    }
                    val formattedRek = com.example.data.network.SpreadsheetService.formatRekening15(bCust.noRekening)
                    if (formattedRek.isNotBlank() && (overwrite || !map.containsKey(formattedRek))) map[formattedRek] = bCust
                    val rawCif = bCust.cif.trim()
                    if (rawCif.isNotBlank() && (overwrite || !map.containsKey(rawCif))) map[rawCif] = bCust
                    val cc = rawCif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
                    if (cc.isNotBlank()) {
                        if (overwrite || !map.containsKey(cc)) map[cc] = bCust
                        if (overwrite || !map.containsKey("CIF-$cc")) map["CIF-$cc"] = bCust
                    }
                    val cleanName = bCust.namaNasabah.trim().lowercase()
                    val cleanMantri = cleanMantriNameOnly(bCust.namaMantri).lowercase()
                    if (cleanName.isNotBlank() && cleanMantri.isNotBlank()) {
                        val key = "$cleanName|$cleanMantri"
                        if (overwrite || !map.containsKey(key)) map[key] = bCust
                    }
                    if (cleanName.isNotBlank()) {
                        nameMap[cleanName] = bCust
                        nameCount[cleanName] = (nameCount[cleanName] ?: 0) + 1
                    }
                }

                // Index both lists: akhirBulan first, then blnLalu (or vice-versa)
                akhirBulanCustomersList.forEach { indexCustomer(it, overwrite = false) }
                blnLaluCustomersList.forEach { indexCustomer(it, overwrite = true) }

                Triple(map, nameMap, nameCount)
            }

            fun findBlnCust(cust: CustomerEntity): CustomerEntity? {
                val rawRek = cust.noRekening.trim()
                val cr = rawRek.filter { it.isDigit() }
                val noZero = cr.trimStart('0')
                val formattedRek = com.example.data.network.SpreadsheetService.formatRekening15(cust.noRekening)
                val rawCif = cust.cif.trim()
                val cc = rawCif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
                val cleanName = cust.namaNasabah.trim().lowercase()
                val cleanMantri = cleanMantriNameOnly(cust.namaMantri).lowercase()

                return blnLookup[rawRek]
                    ?: (if (cr.isNotBlank()) blnLookup[cr] else null)
                    ?: (if (noZero.isNotBlank()) blnLookup[noZero] else null)
                    ?: (if (cr.length >= 15) blnLookup[cr.takeLast(15)] else null)
                    ?: (if (formattedRek.isNotBlank()) blnLookup[formattedRek] else null)
                    ?: (if (rawCif.isNotBlank()) blnLookup[rawCif] else null)
                    ?: (if (cc.isNotBlank()) blnLookup[cc] ?: blnLookup["CIF-$cc"] else null)
                    ?: (if (cleanName.isNotBlank() && cleanMantri.isNotBlank()) blnLookup["$cleanName|$cleanMantri"] else null)
                    ?: (if (cleanName.isNotBlank() && blnNameCount[cleanName] == 1) blnNameMap[cleanName] else null)
            }

            // Helper to get Kolek Group
            fun getKolekGroup(k: String): String {
                val clean = k.trim().uppercase()
                return when {
                    clean == "KL" || clean.contains("KURANG") || clean == "D" || clean.contains("DIRAGUKAN") || clean == "M" || clean.contains("MACET") || clean == "3" || clean == "4" || clean == "5" || clean == "NPL" -> "NPL"
                    clean.startsWith("SML") || clean.contains("DPK") || clean == "2" -> "SML"
                    clean == "L" || clean == "LANCAR" || clean == "1" || (clean.contains("LANCAR") && !clean.contains("KURANG")) -> "LANCAR"
                    else -> "LAINNYA"
                }
            }

            // Kualitas Kredit Breakdown
            val kolekBreakdown = remember(intrakompCustomers, dailyDataMap) {
                // Grouping: {Group: {DebtorCount, TotalOs}}
                val dailyData = intrakompCustomers.map { customer ->
                    val daily = dailyDataMap[customer.noRekening]
                    Triple(
                        getKolekGroup(daily?.kolek ?: customer.kolek),
                        daily?.osPinjaman ?: customer.osPinjaman,
                        1
                    )
                }.groupBy({ it.first }, { Pair(it.second, it.third) })
                
                dailyData.mapValues { entry ->
                    val totalOs = entry.value.sumOf { it.first }
                    val totalDebtor = entry.value.sumOf { it.second }
                    Pair(totalOs, totalDebtor)
                }
            }
            
            // Segmen Breakdown
            val segmenBreakdown = remember(intrakompCustomers, dailyDataMap) {
                intrakompCustomers.groupBy { it.segmen }.mapValues { entry ->
                    val customerList = entry.value
                    val grouped = customerList.groupBy { getKolekGroup(dailyDataMap[it.noRekening]?.kolek ?: it.kolek) }
                        .mapValues { subEntry -> subEntry.value.sumOf { dailyDataMap[it.noRekening]?.osPinjaman ?: it.osPinjaman } }
                    grouped
                }
            }
            
            // Helper for compact text style
            val smallTextStyle = LocalTextStyle.current.copy(fontSize = 9.sp)

            androidx.compose.ui.window.Dialog(
                onDismissRequest = onDismiss,
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(modifier = Modifier.fillMaxSize().padding(8.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Dashboard INTRAKOMP", style = MaterialTheme.typography.titleMedium, fontSize = 14.sp)
                            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, "Tutup", modifier = Modifier.size(16.dp)) }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Main Stats Table
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(12.dp)),
                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Ikhtisar Posisi Unit",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )

                                 // Calculate differences and formats
                                 val osDiff1 = akhirBlnSummary.os - blnLaluSummary.os
                                 val debDiff1 = akhirBlnSummary.debtors - blnLaluSummary.debtors
                                 
                                 val osIcon1 = if (osDiff1 > 0) "▲" else if (osDiff1 < 0) "▼" else ""
                                 val osColorTheme1 = if (osDiff1 > 0) Color(0xFF2E7D32) else if (osDiff1 < 0) Color(0xFFC62828) else Color.Gray
                                 val debColorTheme1 = if (debDiff1 > 0) Color(0xFF2E7D32) else if (debDiff1 < 0) Color(0xFFC62828) else Color.Gray
                                 
                                 val osDiffText1 = if (osDiff1 != 0.0) "$osIcon1${formatDoubleInMillions(Math.abs(osDiff1), formatter)}" else "0"
                                 val debDiffText1 = if (debDiff1 != 0) "${if (debDiff1 > 0) "▲" else "▼"}${Math.abs(debDiff1)}" else "0"

                                 val osDiff2 = dailySummary.os - akhirBlnSummary.os
                                 val debDiff2 = dailySummary.debtors - akhirBlnSummary.debtors
                                 
                                 val osIcon2 = if (osDiff2 > 0) "▲" else if (osDiff2 < 0) "▼" else ""
                                 val osColorTheme2 = if (osDiff2 > 0) Color(0xFF2E7D32) else if (osDiff2 < 0) Color(0xFFC62828) else Color.Gray
                                 val debColorTheme2 = if (debDiff2 > 0) Color(0xFF2E7D32) else if (debDiff2 < 0) Color(0xFFC62828) else Color.Gray
                                 
                                 val osDiffText2 = if (osDiff2 != 0.0) "$osIcon2${formatDoubleInMillions(Math.abs(osDiff2), formatter)}" else "0"
                                 val debDiffText2 = if (debDiff2 != 0) "${if (debDiff2 > 0) "▲" else "▼"}${Math.abs(debDiff2)}" else "0"

                                 // Header Row
                                 Row(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(6.dp))
                                         .padding(vertical = 6.dp, horizontal = 6.dp),
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     Text("Posisi", modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                     Text(blnLaluDate.ifBlank { "-" }, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                     Text(akhirBlnDate.ifBlank { "-" }, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                     Text(dailyDate.ifBlank { "-" }, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                     Text("MOM", modifier = Modifier.weight(1.1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                     Text("MTD", modifier = Modifier.weight(1.1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                 }

                                 // OS Pinjaman Row
                                 Row(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .padding(horizontal = 6.dp, vertical = 5.dp),
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     Text("OS (Jt)", modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                     Text(
                                         text = formatDoubleInMillions(blnLaluSummary.os, formatter),
                                         modifier = Modifier.weight(1.0f),
                                         textAlign = TextAlign.End,
                                         style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 8.5.sp)
                                     )
                                     Text(
                                         text = formatDoubleInMillions(akhirBlnSummary.os, formatter),
                                         modifier = Modifier.weight(1.0f),
                                         textAlign = TextAlign.End,
                                         style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 8.5.sp)
                                     )
                                     Text(
                                         text = formatDoubleInMillions(dailySummary.os, formatter),
                                         modifier = Modifier.weight(1.0f),
                                         textAlign = TextAlign.End,
                                         style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp)
                                     )
                                     Text(
                                         text = osDiffText1,
                                         modifier = Modifier.weight(1.1f),
                                         textAlign = TextAlign.End,
                                         style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = osColorTheme1)
                                     )
                                     Text(
                                         text = osDiffText2,
                                         modifier = Modifier.weight(1.1f),
                                         textAlign = TextAlign.End,
                                         style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = osColorTheme2)
                                     )
                                 }

                                 androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                 // Debitur Row
                                 Row(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .padding(horizontal = 6.dp, vertical = 5.dp),
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     Text("Deb", modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                     Text(
                                         text = "${blnLaluSummary.debtors}",
                                         modifier = Modifier.weight(1.0f),
                                         textAlign = TextAlign.End,
                                         style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 8.5.sp)
                                     )
                                     Text(
                                         text = "${akhirBlnSummary.debtors}",
                                         modifier = Modifier.weight(1.0f),
                                         textAlign = TextAlign.End,
                                         style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 8.5.sp)
                                     )
                                     Text(
                                         text = "${dailySummary.debtors}",
                                         modifier = Modifier.weight(1.0f),
                                         textAlign = TextAlign.End,
                                         style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp)
                                     )
                                     Text(
                                         text = debDiffText1,
                                         modifier = Modifier.weight(1.1f),
                                         textAlign = TextAlign.End,
                                         style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = debColorTheme1)
                                     )
                                     Text(
                                         text = debDiffText2,
                                         modifier = Modifier.weight(1.1f),
                                         textAlign = TextAlign.End,
                                         style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = debColorTheme2)
                                     )
                                 }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Kualitas Kredit
                        Text("Kualitas Kredit (Lancar, SML, NPL)", style = MaterialTheme.typography.titleSmall, fontSize = 12.sp)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(12.dp)),
                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                // Table Header 1 (Dates and MOM/MTD)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp))
                                        .padding(vertical = 3.dp, horizontal = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Posisi", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.5.sp))
                                    Text(blnLaluDate.ifBlank { "-" }, modifier = Modifier.weight(1.55f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.5.sp))
                                    Text(akhirBlnDate.ifBlank { "-" }, modifier = Modifier.weight(1.55f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.5.sp))
                                    Text(dailyDate.ifBlank { "-" }, modifier = Modifier.weight(1.55f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.5.sp))
                                    Text("MOM", modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.5.sp))
                                    Text("MTD", modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.5.sp))
                                }

                                // Table Header 2 (OS & Deb sub-columns)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), shape = RoundedCornerShape(2.dp))
                                        .padding(vertical = 2.dp, horizontal = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("", modifier = Modifier.weight(1.1f))
                                    Text("OS", modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.sp, color = Color.Gray))
                                    Text("Deb", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.sp, color = Color.Gray))
                                    Text("OS", modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.sp, color = Color.Gray))
                                    Text("Deb", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.sp, color = Color.Gray))
                                    Text("OS", modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.sp, color = Color.Gray))
                                    Text("Deb", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.sp, color = Color.Gray))
                                    Text("OS", modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.sp, color = Color.Gray))
                                    Text("OS", modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.sp, color = Color.Gray))
                                }

                                // Rows for Lancar, SML, NPL
                                listOf("LANCAR", "SML", "NPL").forEach { group ->
                                    val isPositiveGood = (group == "LANCAR")
                                    val blVal = when (group) {
                                        "LANCAR" -> kolekSummaryBulanLalu.lancar
                                        "SML" -> kolekSummaryBulanLalu.sml
                                        else -> kolekSummaryBulanLalu.npl
                                    }
                                    val abVal = when (group) {
                                        "LANCAR" -> kolekSummaryAkhirBulan.lancar
                                        "SML" -> kolekSummaryAkhirBulan.sml
                                        else -> kolekSummaryAkhirBulan.npl
                                    }
                                    val dailyVal = when (group) {
                                        "LANCAR" -> kolekSummaryDaily.lancar
                                        "SML" -> kolekSummaryDaily.sml
                                        else -> kolekSummaryDaily.npl
                                    }

                                    val momOs = abVal.os - blVal.os
                                    val mtdOs = dailyVal.os - abVal.os

                                    fun fmtDiff(diff: Double, isGood: Boolean): Pair<String, Color> {
                                        if (diff == 0.0) return Pair("0", Color.Gray)
                                        val icon = if (diff > 0) "▲" else "▼"
                                        val text = "$icon${formatDoubleInMillions(Math.abs(diff), formatter)}"
                                        val color = if (diff > 0) {
                                            if (isGood) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        } else {
                                            if (isGood) Color(0xFFC62828) else Color(0xFF2E7D32)
                                        }
                                        return Pair(text, color)
                                    }

                                    val (momOsTxt, momOsClr) = fmtDiff(momOs, isPositiveGood)
                                    val (mtdOsTxt, mtdOsClr) = fmtDiff(mtdOs, isPositiveGood)

                                    val categoryLabel = when (group) {
                                        "LANCAR" -> "Lancar"
                                        "SML" -> "SML"
                                        else -> "NPL"
                                    }

                                    val labelColor = when (group) {
                                        "LANCAR" -> Color(0xFF2E7D32)
                                        "SML" -> Color(0xFFE65100)
                                        else -> Color(0xFFC62828)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 2.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = categoryLabel,
                                            modifier = Modifier.weight(1.1f),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = labelColor)
                                        )
                                        // Bln Lalu OS & Deb
                                        Text(formatDoubleInMillions(blVal.os, formatter), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Medium))
                                        Text("${blVal.debtors}", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 7.5.sp, color = Color.Gray))
                                        
                                        // Akhir Bln OS & Deb
                                        Text(formatDoubleInMillions(abVal.os, formatter), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Medium))
                                        Text("${abVal.debtors}", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 7.5.sp, color = Color.Gray))

                                        // Daily OS & Deb
                                        Text(formatDoubleInMillions(dailyVal.os, formatter), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold))
                                        Text("${dailyVal.debtors}", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 7.5.sp, color = Color.Gray, fontWeight = FontWeight.Bold))

                                        // MOM OS
                                        Text(momOsTxt, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = momOsClr))

                                        // MTD OS
                                        Text(mtdOsTxt, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = mtdOsClr))
                                    }

                                    if (group != "NPL") {
                                        androidx.compose.material3.HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            modifier = Modifier.padding(vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Kualitas Kredit Per Mantri
                        Text("Kualitas Kredit Per Mantri", style = MaterialTheme.typography.titleSmall, fontSize = 12.sp)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(12.dp)),
                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            val mantriScrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val sourceCustomers = remember(customersList) {
                                    val intra = customersList.filter { it.menuType == "INTRAKOMP" }
                                    if (intra.isNotEmpty()) intra else customersList
                                }

                                fun cleanMantriName(raw: String): String {
                                    var name = raw.trim().uppercase()
                                    if (name.equals("Blank PN", ignoreCase = true) || name.equals("TANPA NAMA", ignoreCase = true)) {
                                        return "Blank PN"
                                    }
                                    if (name.contains("-")) {
                                        val parts = name.split("-")
                                        val textPart = parts.lastOrNull { p -> p.trim().any { it.isLetter() } }?.trim()
                                        if (!textPart.isNullOrBlank()) {
                                            name = textPart
                                        }
                                    }
                                    name = name.replace(Regex("[0-9]"), "").replace(Regex("^[- .]+"), "").trim().uppercase()
                                    return if (name.isBlank() || name == "TIDAK TERIDENTIFIKASI" || name == "TANPA NAMA" || name.equals("BLANK PN", ignoreCase = true)) "Blank PN" else name
                                }

                                fun calculateMantriSinglePeriod(
                                    list: List<CustomerEntity>,
                                    osExtractor: (CustomerEntity) -> Double,
                                    kolekExtractor: (CustomerEntity) -> String
                                ): MantriDetailData {
                                    var totOs = 0.0
                                    var totDeb = 0
                                    var lcrOs = 0.0
                                    var lcrDeb = 0
                                    var smlOs = 0.0
                                    var smlDeb = 0
                                    var nplOs = 0.0
                                    var nplDeb = 0

                                    list.forEach { cust ->
                                        val os = osExtractor(cust)
                                        val kolekRaw = kolekExtractor(cust).trim().uppercase()
                                        val group = when {
                                            kolekRaw == "KL" || kolekRaw.contains("KURANG") || kolekRaw == "D" || kolekRaw.contains("DIRAGUKAN") || kolekRaw == "M" || kolekRaw.contains("MACET") || kolekRaw == "3" || kolekRaw == "4" || kolekRaw == "5" || kolekRaw == "NPL" -> "NPL"
                                            kolekRaw.startsWith("SML") || kolekRaw.contains("DPK") || kolekRaw == "2" -> "SML"
                                            kolekRaw == "L" || kolekRaw == "LANCAR" || kolekRaw == "1" || (kolekRaw.contains("LANCAR") && !kolekRaw.contains("KURANG")) -> "LANCAR"
                                            else -> "LANCAR"
                                        }

                                        totOs += os
                                        totDeb += 1

                                        when (group) {
                                            "LANCAR" -> {
                                                lcrOs += os
                                                lcrDeb += 1
                                            }
                                            "SML" -> {
                                                smlOs += os
                                                smlDeb += 1
                                            }
                                            "NPL" -> {
                                                nplOs += os
                                                nplDeb += 1
                                            }
                                        }
                                    }

                                    return MantriDetailData(
                                        name = "",
                                        total = UnitSummaryData(os = totOs, debtors = totDeb),
                                        lancar = UnitSummaryData(os = lcrOs, debtors = lcrDeb),
                                        sml = UnitSummaryData(os = smlOs, debtors = smlDeb),
                                        npl = UnitSummaryData(os = nplOs, debtors = nplDeb)
                                    )
                                }

                                val mantriDataMap = remember(sourceCustomers, dailyDataMap, mantriBL, mantriAB, mantriDaily) {
                                    val result = mutableMapOf<String, Triple<MantriDetailData, MantriDetailData, MantriDetailData>>()
                                    val allNames = mutableSetOf<String>()

                                    sourceCustomers.forEach { cust ->
                                        val name = cleanMantriName(cust.namaMantri)
                                        allNames.add(name)
                                    }

                                    (mantriBL.keys + mantriAB.keys + mantriDaily.keys).forEach { key ->
                                        val name = cleanMantriName(key)
                                        allNames.add(name)
                                    }

                                    allNames.sorted().forEach { name ->
                                        val vmBL = mantriBL[name]
                                            ?: mantriBL.entries.firstOrNull { cleanMantriName(it.key) == name }?.value

                                        val vmAB = mantriAB[name]
                                            ?: mantriAB.entries.firstOrNull { cleanMantriName(it.key) == name }?.value

                                        val vmDaily = mantriDaily[name]
                                            ?: mantriDaily.entries.firstOrNull { cleanMantriName(it.key) == name }?.value

                                        val custsForMantri = sourceCustomers.filter {
                                            cleanMantriName(it.namaMantri) == name
                                        }

                                        val blData = if (vmBL != null && vmBL.total.debtors > 0) vmBL
                                        else calculateMantriSinglePeriod(
                                            custsForMantri,
                                            osExtractor = { it.osPinjaman },
                                            kolekExtractor = { if (it.kolekBlnLalu.isNotBlank()) it.kolekBlnLalu else it.kolek }
                                        )

                                        val abData = if (vmAB != null && vmAB.total.debtors > 0) vmAB
                                        else calculateMantriSinglePeriod(
                                            custsForMantri,
                                            osExtractor = { it.osPinjaman },
                                            kolekExtractor = { it.kolek }
                                        )

                                        val dailyData = if (vmDaily != null && vmDaily.total.debtors > 0) vmDaily
                                        else calculateMantriSinglePeriod(
                                            custsForMantri,
                                            osExtractor = { cust ->
                                                val cleanRek = cust.noRekening.replace("-", "").replace(".", "").trim()
                                                val dCust = dailyDataMap[cust.noRekening]
                                                    ?: dailyDataMap[cleanRek]
                                                    ?: dailyDataMap.entries.firstOrNull { entry -> entry.key.replace("-", "").replace(".", "").trim() == cleanRek }?.value
                                                dCust?.osPinjaman ?: cust.osPinjaman
                                            },
                                            kolekExtractor = { cust ->
                                                val cleanRek = cust.noRekening.replace("-", "").replace(".", "").trim()
                                                val dCust = dailyDataMap[cust.noRekening]
                                                    ?: dailyDataMap[cleanRek]
                                                    ?: dailyDataMap.entries.firstOrNull { entry -> entry.key.replace("-", "").replace(".", "").trim() == cleanRek }?.value
                                                if (dCust != null && dCust.kolek.isNotBlank()) dCust.kolek else cust.kolek
                                            }
                                        )

                                        result[name] = Triple(blData, abData, dailyData)
                                    }
                                    result
                                }

                                val allMantris = remember(mantriDataMap) { mantriDataMap.keys.sorted() }

                                if (allMantris.isEmpty()) {
                                    Text(
                                        text = "Tidak ada data mantri",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                } else {
                                    allMantris.forEach { mantriName ->
                                        val (blVal, abVal, dailyVal) = mantriDataMap[mantriName]!!

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            // Mantri Name Header Row
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "MANTRI: $mantriName",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onPrimary)
                                                )
                                            }

                                            // Table Header for this Mantri
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp))
                                                    .padding(vertical = 3.dp, horizontal = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Posisi", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                                Text("Tot OS", modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                                Text("Deb", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                                Text("Lancar", modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                                Text("Deb", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                                Text("SML", modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                                Text("Deb", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                                Text("NPL", modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                                Text("Deb", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp))
                                            }

                                            // Helper functions for MOM & MTD calculations
                                            val momTotOs = abVal.total.os - blVal.total.os
                                            val momTotDeb = abVal.total.debtors - blVal.total.debtors
                                            val momLcrOs = abVal.lancar.os - blVal.lancar.os
                                            val momLcrDeb = abVal.lancar.debtors - blVal.lancar.debtors
                                            val momSmlOs = abVal.sml.os - blVal.sml.os
                                            val momSmlDeb = abVal.sml.debtors - blVal.sml.debtors
                                            val momNplOs = abVal.npl.os - blVal.npl.os
                                            val momNplDeb = abVal.npl.debtors - blVal.npl.debtors

                                            val mtdTotOs = dailyVal.total.os - abVal.total.os
                                            val mtdTotDeb = dailyVal.total.debtors - abVal.total.debtors
                                            val mtdLcrOs = dailyVal.lancar.os - abVal.lancar.os
                                            val mtdLcrDeb = dailyVal.lancar.debtors - abVal.lancar.debtors
                                            val mtdSmlOs = dailyVal.sml.os - abVal.sml.os
                                            val mtdSmlDeb = dailyVal.sml.debtors - abVal.sml.debtors
                                            val mtdNplOs = dailyVal.npl.os - abVal.npl.os
                                            val mtdNplDeb = dailyVal.npl.debtors - abVal.npl.debtors

                                            fun fmtOsDiff(diff: Double, isPositiveGood: Boolean = true): Pair<String, Color> {
                                                if (diff == 0.0) return Pair("0", Color.Gray)
                                                val icon = if (diff > 0) "▲" else "▼"
                                                val text = "$icon${formatDoubleInMillions(Math.abs(diff), formatter)}"
                                                val color = if (diff > 0) {
                                                    if (isPositiveGood) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                } else {
                                                    if (isPositiveGood) Color(0xFFC62828) else Color(0xFF2E7D32)
                                                }
                                                return Pair(text, color)
                                            }

                                            fun fmtDebDiff(diff: Int, isPositiveGood: Boolean = true): Pair<String, Color> {
                                                if (diff == 0) return Pair("0", Color.Gray)
                                                val icon = if (diff > 0) "▲" else "▼"
                                                val text = "$icon${Math.abs(diff)}"
                                                val color = if (diff > 0) {
                                                    if (isPositiveGood) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                } else {
                                                    if (isPositiveGood) Color(0xFFC62828) else Color(0xFF2E7D32)
                                                }
                                                return Pair(text, color)
                                            }

                                            // 1. Bulan Lalu row
                                            Row(
                                                modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9), shape = RoundedCornerShape(2.dp)).padding(horizontal = 2.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = blnLaluDate.ifBlank { "-" },
                                                    modifier = Modifier.weight(1.1f),
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                )
                                                Text(formatDoubleInMillions(blVal.total.os, formatter), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black, fontWeight = FontWeight.Medium))
                                                Text("${blVal.total.debtors}", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black))
                                                Text(formatDoubleInMillions(blVal.lancar.os, formatter), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black))
                                                Text("${blVal.lancar.debtors}", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black))
                                                Text(formatDoubleInMillions(blVal.sml.os, formatter), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black))
                                                Text("${blVal.sml.debtors}", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black))
                                                Text(formatDoubleInMillions(blVal.npl.os, formatter), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black))
                                                Text("${blVal.npl.debtors}", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black))
                                            }

                                            // 2. Akhir Bulan row
                                            Row(
                                                modifier = Modifier.fillMaxWidth().background(Color(0xFFE2E8F0), shape = RoundedCornerShape(2.dp)).padding(horizontal = 2.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = akhirBlnDate.ifBlank { "-" },
                                                    modifier = Modifier.weight(1.1f),
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                )
                                                Text(formatDoubleInMillions(abVal.total.os, formatter), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black, fontWeight = FontWeight.Medium))
                                                Text("${abVal.total.debtors}", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black))
                                                Text(formatDoubleInMillions(abVal.lancar.os, formatter), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black))
                                                Text("${abVal.lancar.debtors}", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black))
                                                Text(formatDoubleInMillions(abVal.sml.os, formatter), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black))
                                                Text("${abVal.sml.debtors}", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black))
                                                Text(formatDoubleInMillions(abVal.npl.os, formatter), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black))
                                                Text("${abVal.npl.debtors}", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black))
                                            }

                                            // 3. Daily row
                                            Row(
                                                modifier = Modifier.fillMaxWidth().background(Color(0xFFE0F2FE), shape = RoundedCornerShape(2.dp)).padding(horizontal = 2.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = dailyDate.ifBlank { "-" },
                                                    modifier = Modifier.weight(1.1f),
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                )
                                                Text(formatDoubleInMillions(dailyVal.total.os, formatter), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black, fontWeight = FontWeight.Bold))
                                                Text("${dailyVal.total.debtors}", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black, fontWeight = FontWeight.Bold))
                                                Text(formatDoubleInMillions(dailyVal.lancar.os, formatter), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black, fontWeight = FontWeight.Bold))
                                                Text("${dailyVal.lancar.debtors}", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black, fontWeight = FontWeight.Bold))
                                                Text(formatDoubleInMillions(dailyVal.sml.os, formatter), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black, fontWeight = FontWeight.Bold))
                                                Text("${dailyVal.sml.debtors}", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black, fontWeight = FontWeight.Bold))
                                                Text(formatDoubleInMillions(dailyVal.npl.os, formatter), modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black, fontWeight = FontWeight.Bold))
                                                Text("${dailyVal.npl.debtors}", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, color = Color.Black, fontWeight = FontWeight.Bold))
                                            }

                                            // 4. MOM row
                                            val (momTotOsTxt, momTotOsClr) = fmtOsDiff(momTotOs, true)
                                            val (momTotDebTxt, momTotDebClr) = fmtDebDiff(momTotDeb, true)
                                            val (momLcrOsTxt, momLcrOsClr) = fmtOsDiff(momLcrOs, true)
                                            val (momLcrDebTxt, momLcrDebClr) = fmtDebDiff(momLcrDeb, true)
                                            val (momSmlOsTxt, momSmlOsClr) = fmtOsDiff(momSmlOs, false)
                                            val (momSmlDebTxt, momSmlDebClr) = fmtDebDiff(momSmlDeb, false)
                                            val (momNplOsTxt, momNplOsClr) = fmtOsDiff(momNplOs, false)
                                            val (momNplDebTxt, momNplDebClr) = fmtDebDiff(momNplDeb, false)

                                            Row(
                                                modifier = Modifier.fillMaxWidth().background(Color(0xFFF3E8FF), shape = RoundedCornerShape(2.dp)).padding(horizontal = 2.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "MOM",
                                                    modifier = Modifier.weight(1.1f),
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                                )
                                                Text(momTotOsTxt, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = momTotOsClr))
                                                Text(momTotDebTxt, modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = momTotDebClr))
                                                Text(momLcrOsTxt, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = momLcrOsClr))
                                                Text(momLcrDebTxt, modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = momLcrDebClr))
                                                Text(momSmlOsTxt, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = momSmlOsClr))
                                                Text(momSmlDebTxt, modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = momSmlDebClr))
                                                Text(momNplOsTxt, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = momNplOsClr))
                                                Text(momNplDebTxt, modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = momNplDebClr))
                                            }

                                            // 5. MTD row
                                            val (mtdTotOsTxt, mtdTotOsClr) = fmtOsDiff(mtdTotOs, true)
                                            val (mtdTotDebTxt, mtdTotDebClr) = fmtDebDiff(mtdTotDeb, true)
                                            val (mtdLcrOsTxt, mtdLcrOsClr) = fmtOsDiff(mtdLcrOs, true)
                                            val (mtdLcrDebTxt, mtdLcrDebClr) = fmtDebDiff(mtdLcrDeb, true)
                                            val (mtdSmlOsTxt, mtdSmlOsClr) = fmtOsDiff(mtdSmlOs, false)
                                            val (mtdSmlDebTxt, mtdSmlDebClr) = fmtDebDiff(mtdSmlDeb, false)
                                            val (mtdNplOsTxt, mtdNplOsClr) = fmtOsDiff(mtdNplOs, false)
                                            val (mtdNplDebTxt, mtdNplDebClr) = fmtDebDiff(mtdNplDeb, false)

                                            Row(
                                                modifier = Modifier.fillMaxWidth().background(Color(0xFFFEF3C7), shape = RoundedCornerShape(2.dp)).padding(horizontal = 2.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "MTD",
                                                    modifier = Modifier.weight(1.1f),
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                                )
                                                Text(mtdTotOsTxt, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = mtdTotOsClr))
                                                Text(mtdTotDebTxt, modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = mtdTotDebClr))
                                                Text(mtdLcrOsTxt, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = mtdLcrOsClr))
                                                Text(mtdLcrDebTxt, modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = mtdLcrDebClr))
                                                Text(mtdSmlOsTxt, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = mtdSmlOsClr))
                                                Text(mtdSmlDebTxt, modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = mtdSmlDebClr))
                                                Text(mtdNplOsTxt, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = mtdNplOsClr))
                                                Text(mtdNplDebTxt, modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = mtdNplDebClr))
                                            }

                                            // 6. Net Disburs row
                                            val custsForThisMantri = remember(sourceCustomers, mantriName) {
                                                sourceCustomers.filter { cleanMantriName(it.namaMantri) == mantriName }
                                            }
                                            val (repM, repY) = remember(dailyDate, akhirBlnDate) {
                                                val refDate = dailyDate.ifBlank { akhirBlnDate }
                                                extractMonthAndYear(refDate)
                                            }
                                            val mantriRealizedItems = remember(custsForThisMantri, blnLookup, repM, repY) {
                                                val inMonth = custsForThisMantri.filter { cust ->
                                                    if (cust.tglReal.isBlank()) false
                                                    else {
                                                        val (m, y) = extractMonthAndYear(cust.tglReal)
                                                        if (repM.isNotBlank() && repY.isNotBlank()) {
                                                            m == repM && y == repY
                                                        } else if (repM.isNotBlank()) {
                                                            m == repM
                                                        } else false
                                                    }
                                                }
                                                inMonth.map { cust ->
                                                    val prev = findBlnCust(cust)
                                                    val isNew = (prev == null)
                                                    val osLalu = prev?.osPinjaman ?: 0.0
                                                    val netD = if (isNew) cust.plafond else (cust.plafond - osLalu)
                                                    RealizedCustomerItem(
                                                        customer = cust,
                                                        plafondBaru = cust.plafond,
                                                        osBlnLalu = osLalu,
                                                        netDisburs = netD,
                                                        isNasabahBaru = isNew
                                                    )
                                                }
                                            }
                                            val netDisbursTotal = remember(mantriRealizedItems) { mantriRealizedItems.sumOf { it.netDisburs } }
                                            val realizedDebCount = mantriRealizedItems.size
                                            val newDebCount = remember(mantriRealizedItems) { mantriRealizedItems.count { it.isNasabahBaru } }
                                            val suplesiDebCount = mantriRealizedItems.size - newDebCount

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFFDCFCE7), shape = RoundedCornerShape(2.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = buildAnnotatedString {
                                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF166534), fontSize = 8.5.sp)) {
                                                            append("Net Disburs ")
                                                        }
                                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF166534), fontSize = 8.5.sp)) {
                                                            append("Rp. ${formatDoubleInMillions(netDisbursTotal, formatter)} ")
                                                        }
                                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF15803D), fontSize = 8.sp)) {
                                                            append("($realizedDebCount Deb) ")
                                                        }
                                                        withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = Color(0xFF475569), fontSize = 7.5.sp)) {
                                                            append("$suplesiDebCount Suplesi | $newDebCount Baru")
                                                        }
                                                    },
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = { showDetailRealizedPopup = Pair(mantriName, mantriRealizedItems) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ChevronRight,
                                                        contentDescription = "Tabel Nasabah Realisasi",
                                                        tint = Color(0xFF166534),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            androidx.compose.material3.HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Segmen
                        Text("Segmen Kredit", style = MaterialTheme.typography.titleSmall, fontSize = 12.sp)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                segmenBreakdown.toList().sortedByDescending { it.second.values.sum() }.forEach { (segmen, kolBreakdown) ->
                                    val totalOsSegmen = kolBreakdown.values.sum()
                                    Text(segmen.ifBlank { "N/A" }, style = smallTextStyle, fontWeight = FontWeight.Bold)
                                    
                                    Column(modifier = Modifier.padding(start = 8.dp)) {
                                        listOf("LANCAR", "SML", "NPL").forEach { group ->
                                            val os = kolBreakdown[group] ?: 0.0
                                            val pct = if (totalOsSegmen > 0) (os / totalOsSegmen * 100) else 0.0
                                            if (os > 0) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(group, style = smallTextStyle)
                                                    Text("%.1f%%".format(pct), style = smallTextStyle)
                                                    Text(formatDoubleClean(os, formatter), style = smallTextStyle)
                                                }
                                            }
                                        }
                                    }
                                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            showDetailRealizedPopup?.let { (mName, items) ->
                RealizedCustomersPopupDialog(
                    mantriName = mName,
                    items = items,
                    onDismiss = { showDetailRealizedPopup = null },
                    formatter = formatter
                )
            }
        }
        DetailUnitDashboard(
            customersList = customersList,
            dailyDataMap = dailyDataMap,
            onDismiss = { showDetailUnitDialog = false }
        )
    }

    showDetailDialogForCustomer?.let { customer ->

        val monthlyKolekHistory by viewModel.monthlyKolekHistory.collectAsState()
        val monthlyTunggakanHistory by viewModel.monthlyTunggakanHistory.collectAsState()
        val isMonthlyKolekLoading by viewModel.isMonthlyKolekLoading.collectAsState()

        LaunchedEffect(customer.noRekening, customer.cif) {
            viewModel.loadMonthlyKolekForCustomer(customer.noRekening, customer.cif)
        }

        val blnLaluCustomersList by viewModel.bulanLaluCustomers.collectAsState()
        val priorMap by viewModel.priorMonthLw321KolekMap.collectAsState()
        val allCustomersList by viewModel.customersList.collectAsState()
        val periodDateAkhirBulan by viewModel.periodDateAkhirBulan.collectAsState()
        val periodDateBulanLalu by viewModel.periodDateBulanLalu.collectAsState()

        val resolvedKolekBlnLalu = remember(customer, blnLaluCustomersList, priorMap, allCustomersList, monthlyKolekHistory, periodDateAkhirBulan, periodDateBulanLalu) {
            val cleanRek = customer.noRekening.filter { it.isDigit() }
            val noZero = cleanRek.trimStart('0')
            val cleanCif = customer.cif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
            val formattedRek = com.example.data.network.SpreadsheetService.formatRekening15(customer.noRekening)

            // 1. Cek dari daftar nasabah Bulan Lalu (bulanLaluCustomers)
            val fromBlnList = blnLaluCustomersList.firstOrNull { b ->
                val bRek = b.noRekening.filter { it.isDigit() }
                val bNoZero = bRek.trimStart('0')
                val bCif = b.cif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
                val bFormatted = com.example.data.network.SpreadsheetService.formatRekening15(b.noRekening)
                (cleanRek.isNotBlank() && bRek == cleanRek) ||
                (noZero.isNotBlank() && bNoZero == noZero) ||
                (cleanRek.length >= 15 && bRek.length >= 15 && cleanRek.takeLast(15) == bRek.takeLast(15)) ||
                (formattedRek.isNotBlank() && bFormatted == formattedRek) ||
                (cleanCif.isNotBlank() && bCif == cleanCif) ||
                (customer.noRekening.isNotBlank() && b.noRekening.trim() == customer.noRekening.trim())
            }?.kolek?.takeIf { it.isNotBlank() && it != "-" }

            // 2. Cek dari field customer.kolekBlnLalu (misalnya diset dari kartu Downgrade / Upgrade)
            val fromField = customer.kolekBlnLalu.takeIf { it.isNotBlank() && it != "-" }

            // 3. Cek dari priorMonthLw321KolekMap
            val fromPriorMap = priorMap[cleanRek]
                ?: (if (noZero.isNotBlank()) priorMap[noZero] else null)
                ?: (if (cleanRek.length >= 15) priorMap[cleanRek.takeLast(15)] else null)
                ?: priorMap[formattedRek]
                ?: (if (cleanCif.isNotBlank()) priorMap[cleanCif] ?: priorMap["CIF-$cleanCif"] else null)

            // 4. Cek dari monthlyKolekHistory (LW321 12 bulan)
            val fromHistory = if (monthlyKolekHistory.isNotEmpty()) {
                val refDate = periodDateAkhirBulan.ifBlank { periodDateBulanLalu }.ifBlank { customer.tglReal }
                val mStr = refDate.replace("-", "/").replace(".", "/").split("/").getOrNull(1)?.trim() ?: ""
                val currentM = mStr.toIntOrNull() ?: 8
                val prevM = if (currentM == 1) 12 else currentM - 1
                monthlyKolekHistory[prevM]?.takeIf { it.isNotBlank() && it != "-" }
            } else null

            // 5. Cek dari master customersList jika customer saat ini berasal dari dailyDataMap
            val fromMasterList = allCustomersList.firstOrNull { m ->
                val mRek = m.noRekening.filter { it.isDigit() }
                val mNoZero = mRek.trimStart('0')
                val mCif = m.cif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
                val mFormatted = com.example.data.network.SpreadsheetService.formatRekening15(m.noRekening)
                (cleanRek.isNotBlank() && mRek == cleanRek) ||
                (noZero.isNotBlank() && mNoZero == noZero) ||
                (cleanRek.length >= 15 && mRek.length >= 15 && cleanRek.takeLast(15) == mRek.takeLast(15)) ||
                (mFormatted.isNotBlank() && mFormatted == formattedRek) ||
                (cleanCif.isNotBlank() && mCif == cleanCif) ||
                (customer.noRekening.isNotBlank() && m.noRekening.trim() == customer.noRekening.trim())
            }?.kolekBlnLalu?.takeIf { it.isNotBlank() && it != "-" }

            fromBlnList ?: fromField ?: fromPriorMap ?: fromHistory ?: fromMasterList ?: "-"
        }

        val monitoringMatchedCustomer = remember(customer, monitoringCustomers) {
            val cleanRek = customer.noRekening.filter { it.isDigit() }
            val noZero = cleanRek.trimStart('0')
            val cleanCif = customer.cif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
            val formattedRek = com.example.data.network.SpreadsheetService.formatRekening15(customer.noRekening)

            monitoringCustomers.firstOrNull { m ->
                val mRek = m.noRekening.filter { it.isDigit() }
                val mNoZero = mRek.trimStart('0')
                val mCif = m.cif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
                val mFormatted = com.example.data.network.SpreadsheetService.formatRekening15(m.noRekening)
                (cleanRek.isNotBlank() && mRek == cleanRek) ||
                (noZero.isNotBlank() && mNoZero == noZero) ||
                (cleanRek.length >= 15 && mRek.length >= 15 && cleanRek.takeLast(15) == mRek.takeLast(15)) ||
                (mFormatted.isNotBlank() && mFormatted == formattedRek) ||
                (cleanCif.isNotBlank() && mCif == cleanCif) ||
                (customer.noRekening.isNotBlank() && m.noRekening.trim() == customer.noRekening.trim()) ||
                (customer.namaNasabah.isNotBlank() && m.namaNasabah.trim().equals(customer.namaNasabah.trim(), ignoreCase = true))
            }
        }

        val resolvedMasterCustomer = remember(customer, allCustomers, allCustomersList) {
            val cleanRek = customer.noRekening.filter { it.isDigit() }
            val noZero = cleanRek.trimStart('0')
            val cleanCif = customer.cif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
            val formattedRek = com.example.data.network.SpreadsheetService.formatRekening15(customer.noRekening)

            val pool = (allCustomers + allCustomersList).distinctBy { it.noRekening.ifBlank { it.cif } }
            pool.firstOrNull { m ->
                val mRek = m.noRekening.filter { it.isDigit() }
                val mNoZero = mRek.trimStart('0')
                val mCif = m.cif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
                val mFormatted = com.example.data.network.SpreadsheetService.formatRekening15(m.noRekening)
                (cleanRek.isNotBlank() && mRek == cleanRek) ||
                (noZero.isNotBlank() && mNoZero == noZero) ||
                (cleanRek.length >= 15 && mRek.length >= 15 && cleanRek.takeLast(15) == mRek.takeLast(15)) ||
                (mFormatted.isNotBlank() && mFormatted == formattedRek) ||
                (cleanCif.isNotBlank() && mCif == cleanCif) ||
                (customer.noRekening.isNotBlank() && m.noRekening.trim() == customer.noRekening.trim()) ||
                (customer.namaNasabah.isNotBlank() && m.namaNasabah.trim().equals(customer.namaNasabah.trim(), ignoreCase = true))
            }
        }

        val resolvedAddress = remember(customer, monitoringMatchedCustomer, resolvedMasterCustomer, selectedMenu, selectedBottomNav) {
            val monitoringAlamat = monitoringMatchedCustomer?.alamat?.trim().orEmpty()
            val monitoringDesa = monitoringMatchedCustomer?.desa?.trim().orEmpty()
            val masterAlamat = resolvedMasterCustomer?.alamat?.trim().orEmpty()
            val masterDesa = resolvedMasterCustomer?.desa?.trim().orEmpty()
            val custAlamat = customer.alamat.trim()
            val custDesa = customer.desa.trim()

            // Jika di menu DASHBOARD atau jika ditemukan di monitoring (INTRAKOMP), ambil data alamat sama persis seperti pada menu monitoring
            val rawAddress = when {
                monitoringAlamat.isNotBlank() && !monitoringAlamat.equals("JL. RAYA KATAPANG NO. 12", ignoreCase = true) -> monitoringAlamat
                masterAlamat.isNotBlank() && !masterAlamat.equals("JL. RAYA KATAPANG NO. 12", ignoreCase = true) -> masterAlamat
                custAlamat.isNotBlank() && !custAlamat.equals("JL. RAYA KATAPANG NO. 12", ignoreCase = true) -> custAlamat
                monitoringDesa.isNotBlank() -> "Desa $monitoringDesa"
                masterDesa.isNotBlank() -> "Desa $masterDesa"
                custDesa.isNotBlank() -> "Desa $custDesa"
                monitoringAlamat.isNotBlank() -> monitoringAlamat
                masterAlamat.isNotBlank() -> masterAlamat
                custAlamat.isNotBlank() -> custAlamat
                else -> "-"
            }

            if (customer.menuType == "EKSTRAKOMP" || selectedMenu == "EKSTRAKOMP") {
                simplifyAddress(rawAddress)
            } else {
                rawAddress
            }
        }

        val isEkstrakomp = customer.menuType == "EKSTRAKOMP" || selectedMenu == "EKSTRAKOMP"
        val dailyInfo = remember(customer, dailyDataMap, isEkstrakomp) {
            if (isEkstrakomp) {
                null
            } else {
                val rawRek = customer.noRekening.trim()
                val cleanRek = rawRek.filter { it.isDigit() }
                val cleanCif = customer.cif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
                
                dailyDataMap[rawRek]
                    ?: (if (cleanRek.isNotBlank()) dailyDataMap[cleanRek] else null)
                    ?: (if (cleanRek.length >= 15) dailyDataMap[cleanRek.takeLast(15)] else null)
                    ?: (if (cleanRek.isNotBlank()) dailyDataMap[cleanRek.trimStart('0')] else null)
                    ?: (if (customer.cif.isNotBlank()) dailyDataMap[customer.cif] else null)
                    ?: (if (cleanCif.isNotBlank()) dailyDataMap[cleanCif] ?: dailyDataMap["CIF-$cleanCif"] else null)
                    ?: (if (cleanRek.isNotBlank()) dailyDataMap.entries.firstOrNull { it.key.filter { c -> c.isDigit() } == cleanRek }?.value else null)
            }
        }
        val hasOsDecreased = dailyInfo != null && dailyInfo.osPinjaman < customer.osPinjaman

        val customerCifKey = remember(customer) {
            val clean = customer.cif.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
            clean.dropWhile { it == '0' }
        }
        val savingsList = remember(customerCifKey, savingsMap) {
            savingsMap[customerCifKey] ?: emptyList()
        }
        val sortedSavings = remember(savingsList) {
            savingsList.sortedByDescending { it.saldo }
        }
        val saving1 = sortedSavings.getOrNull(0)
        val saving2 = sortedSavings.getOrNull(1)

        val custKey = remember(customer) {
            if (customer.noRekening.isNotBlank()) customer.noRekening else if (customer.cif.isNotBlank()) customer.cif else customer.namaNasabah
        }
        val generalTindakLanjutVal = tindakLanjutMap[custKey] ?: "-"
        val kunjunganTindakLanjutVal = tindakLanjutMap["kunjungan_$custKey"] ?: "-"

        val jumlahAngsuran = remember(customer, dailyInfo) {
            val rate = customer.rate
            val tenor = DateTenorUtils.effectiveTenor(customer.tenor, customer.tglReal, customer.tglLunas) ?: customer.tenor
            val plafond = customer.plafond
            val rawInstallment = if (tenor <= 0) {
                0.0
            } else if (rate <= 0.0) {
                plafond / tenor.toDouble()
            } else {
                val rPart = (rate / 12.0) / 100.0
                val denom = 1.0 - java.lang.Math.pow(1.0 + rPart, -tenor.toDouble())
                if (denom == 0.0) {
                    0.0
                } else {
                    plafond * (rPart / denom)
                }
            }
            val flagClean = customer.flag.trim().uppercase()
            val isFlagN = flagClean == "N" || flagClean == "FLAG N" || flagClean.endsWith(" N")
            if (rawInstallment > 0.0 && isFlagN) {
                kotlin.math.ceil(kotlin.math.round(rawInstallment) / 100.0) * 100.0
            } else {
                rawInstallment
            }
        }

        val minAngsuran = remember(customer, dailyInfo, jumlahAngsuran) {
            val currentKolekRaw = (dailyInfo?.kolek ?: customer.kolek).trim().uppercase()
            val currentKolekNormalized = currentKolekRaw.replace(" ", "")
            val isSml3 = currentKolekNormalized == "SML3" || currentKolekNormalized == "SML-3"
            val isKl = currentKolekNormalized == "KL" || currentKolekRaw == "KURANG LANCAR"

            if (isSml3 || isKl) {
                val newTotalTunggakan = if (dailyInfo != null) {
                    if (dailyInfo.totalTunggakan > 0.0) {
                        dailyInfo.totalTunggakan
                    } else {
                        dailyInfo.tunggakanPokok + dailyInfo.tunggakanBunga + dailyInfo.pinalty
                    }
                } else {
                    null
                }

                val displayTotalTunggakan = if (newTotalTunggakan != null && newTotalTunggakan != customer.totalTunggakan) {
                    newTotalTunggakan
                } else {
                    customer.totalTunggakan
                }

                if (isKl) {
                    displayTotalTunggakan - (jumlahAngsuran * 3.0)
                } else {
                    displayTotalTunggakan - (jumlahAngsuran * 2.0)
                }
            } else {
                null
            }
        }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showDetailDialogForCustomer = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(10.dp)),
                color = Color.White,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header (Nama Nasabah dan Alamat) berwarna Biru - padded tightly
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF005FAF))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val isBergeserNpl = generalTindakLanjutVal == "Bergeser NPL" || kunjunganTindakLanjutVal == "Bergeser NPL"
                            val isDetailLunasOrHilang = !isEkstrakomp && (customer.osPinjaman <= 0.0 || (dailyDataMap.isNotEmpty() && (dailyInfo == null || dailyInfo.osPinjaman <= 0.0)))
                            Text(
                                text = customer.namaNasabah.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    textDecoration = if (isDetailLunasOrHilang) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                                ),
                                fontWeight = FontWeight.Bold,
                                color = if (isBergeserNpl) Color(0xFFFF5252) else if (isDetailLunasOrHilang) Color.White.copy(alpha = 0.7f) else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            val displayAlamat = resolvedAddress
                            Text(
                                text = displayAlamat,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = Color.White.copy(alpha = 0.90f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Content Section with white background
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Section 1: INFORMASI REKENING
                            Text(
                                text = "INFORMASI REKENING",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 0.5.sp),
                                color = Color(0xFFD32F2F)
                            )

                            val elapsedMonths = remember(customer.tglReal) { calculateElapsedMonthsSinceReal(customer.tglReal) }
                            val realAnnotated = remember(customer.tglReal, elapsedMonths) {
                                buildAnnotatedString {
                                    append(if (customer.tglReal.isNotBlank()) customer.tglReal else "-")
                                    if (elapsedMonths != null) {
                                        append(" ")
                                        withStyle(SpanStyle(color = Color(0xFF005FAF), fontWeight = FontWeight.Bold)) {
                                            append("($elapsedMonths)")
                                        }
                                    }
                                }
                            }

                            val remainingMonths = remember(customer.tglLunas) { calculateRemainingMonthsUntilLunas(customer.tglLunas) }
                            val lunasAnnotated = remember(customer.tglLunas, remainingMonths) {
                                buildAnnotatedString {
                                    append(if (customer.tglLunas.isNotBlank()) customer.tglLunas else "-")
                                    if (remainingMonths != null) {
                                        append(" ")
                                        withStyle(SpanStyle(color = Color(0xFF005FAF), fontWeight = FontWeight.Bold)) {
                                            append("($remainingMonths)")
                                        }
                                    }
                                }
                            }

                            if (customer.menuType == "EKSTRAKOMP") {
                                val tglPh = remember(customer.extraColumns) {
                                    customer.extraColumns.split("||")
                                        .firstOrNull { it.startsWith("Tgl PH::") }
                                        ?.substringAfter("Tgl PH::") ?: "-"
                                }
                                // Baris 1: CIF, Segmen - Rate (%), Tenor
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItemCol(label = "CIF", value = customer.cif)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItemCol(label = "Segmen", value = if (customer.menuType == "EKSTRAKOMP") mapEkstrakompSegmen(customer.segmen) else customer.segmen)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        val effectiveTenorVal = remember(customer.tenor, customer.tglReal, customer.tglLunas) {
                                            DateTenorUtils.effectiveTenor(customer.tenor, customer.tglReal, customer.tglLunas) ?: customer.tenor
                                        }
                                        DetailItemCol(label = "Tenor", value = "$effectiveTenorVal Bulan")
                                    }
                                }
                                
                                // Baris 2: Tanggal Realisasi, Tanggal Lunas, Tgl PH
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItemCol(label = "Tanggal Realisasi", annotatedValue = realAnnotated)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItemCol(label = "Tanggal Lunas", annotatedValue = lunasAnnotated)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItemCol(label = "Tgl PH", value = tglPh.ifBlank { "-" })
                                    }
                                }
                            } else {
                                // Baris 1: CIF, Segmen - Rate (%), Jumlah Angsuran
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItemCol(label = "CIF", value = customer.cif)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItemCol(label = "Segmen - Rate (%)", value = "${customer.segmen} - ${customer.rate}%")
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItemCol(label = "Jumlah Angsuran", value = formatDoubleClean(jumlahAngsuran, formatter), showWarningIcon = true)
                                    }
                                }
                                
                                // Baris 2: Tanggal Realisasi, Tanggal Lunas, Tenor
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItemCol(label = "Tanggal Realisasi", annotatedValue = realAnnotated)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItemCol(label = "Tanggal Lunas", annotatedValue = lunasAnnotated)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        val effectiveTenorVal = remember(customer.tenor, customer.tglReal, customer.tglLunas) {
                                            DateTenorUtils.effectiveTenor(customer.tenor, customer.tglReal, customer.tglLunas) ?: customer.tenor
                                        }
                                        DetailItemCol(label = "Tenor", value = "$effectiveTenorVal Bulan")
                                    }
                                }
                            }

                            // Baris 3: No Rekening, Plafond, OS Pinjaman
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val displayedNoRekeningNum = if (customer.noRekening.length > 15) customer.noRekening.substring(customer.noRekening.length - 15) else customer.noRekening
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 0.5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "NO REKENING",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                                                color = Color(0xFF005FAF),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(displayedNoRekeningNum))
                                                    android.widget.Toast.makeText(context, "No Rekening disalin!", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(14.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy No Rekening",
                                                    modifier = Modifier.size(10.dp),
                                                    tint = Color(0xFF005FAF)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = displayedNoRekeningNum,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = Color(0xFF1D1B20),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItemCol(label = "Plafond", value = formatDoubleClean(customer.plafond, formatter))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    val isGoneFromDaily = !isEkstrakomp && dailyInfo == null && dailyDataMap.isNotEmpty() && customer.osPinjaman > 0.0
                                    DetailItemColWithChange(
                                        label = "OS Pinjaman",
                                        oldValue = formatDoubleClean(customer.osPinjaman, formatter),
                                        newValue = if (!isEkstrakomp && dailyInfo != null) {
                                            formatDoubleClean(dailyInfo.osPinjaman, formatter)
                                        } else if (isGoneFromDaily) {
                                            "0"
                                        } else {
                                            null
                                        },
                                        showChange = !isEkstrakomp && (dailyInfo != null || isGoneFromDaily),
                                        onCopyClick = {
                                            val copyValue = if (!isEkstrakomp && dailyInfo != null) dailyInfo.osPinjaman else if (isGoneFromDaily) 0.0 else customer.osPinjaman
                                            val copyText = formatDoubleClean(copyValue, formatter)
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(copyText))
                                            android.widget.Toast.makeText(context, "OS Pinjaman disalin!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }

                            if (customer.menuType == "INTRAKOMP" || customer.menuType == "EKSTRAKOMP") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItemCol(label = "No Simpanan 1", value = saving1?.noRekening ?: "-")
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItemCol(label = "Saldo 1", value = if (saving1 != null) formatDoubleClean(saving1.saldo, formatter) else "-")
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "TINDAK LANJUT",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                                            color = Color(0xFF005FAF),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val dropdownOptions = listOf(
                                            "Bayar Full",
                                            "Bayar Sebagian",
                                            "Tetap SML",
                                            "Bergeser NPL",
                                            "Restruk",
                                            "Janji Bayar"
                                        )
                                        val dropdownColors = mapOf(
                                            "Bayar Lunas" to Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32)),
                                            "Bayar 1x angsuran" to Pair(Color(0xFFE0F2F1), Color(0xFF00695C)),
                                            "Bayar Sebagian" to Pair(Color(0xFFE3F2FD), Color(0xFF1565C0)),
                                            "Janji Bayar" to Pair(Color(0xFFFFF3E0), Color(0xFFE65100)),
                                            "Tidak Ditempat" to Pair(Color(0xFFF3E5F5), Color(0xFF6A1B9A)),
                                            "Pindah Alamat" to Pair(Color(0xFFE8EAF6), Color(0xFF283593)),
                                            "Meninggal Dunia" to Pair(Color(0xFFECEFF1), Color(0xFF37474F)),
                                            "Bayar Full" to Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32)),
                                            "Tetap SML" to Pair(Color(0xFFFFF3E0), Color(0xFFE65100)),
                                            "Bergeser NPL" to Pair(Color(0xFFFFEBEE), Color(0xFFC62828)),
                                            "Restruk" to Pair(Color(0xFFF3E5F5), Color(0xFF6A1B9A))
                                        )
                                        var dropdownExpanded by remember { mutableStateOf(false) }
                                        Box {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(4.dp))
                                                    .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp))
                                                    .clickable { dropdownExpanded = true }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                val (bgOpt, txtOpt) = dropdownColors[generalTindakLanjutVal] ?: Pair(Color.Transparent, Color(0xFF1D1B20))
                                                Text(
                                                    text = if (generalTindakLanjutVal.isBlank() || generalTindakLanjutVal == "-") "Pilih..." else generalTindakLanjutVal,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                                    color = txtOpt,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Icon(
                                                    imageVector = if (dropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Dropdown",
                                                    tint = Color(0xFF005FAF),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }

                                            androidx.compose.material3.DropdownMenu(
                                                expanded = dropdownExpanded,
                                                onDismissRequest = { dropdownExpanded = false },
                                                modifier = Modifier
                                                    .width(140.dp)
                                                    .background(Color.White)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    dropdownOptions.forEach { opt ->
                                                        val (_, txt) = dropdownColors[opt] ?: Pair(Color.White, Color.Black)
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(25.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .clickable {
                                                                    viewModel.saveTindakLanjut(customer, opt, isKunjungan = false)
                                                                    dropdownExpanded = false
                                                                }
                                                                .padding(horizontal = 6.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            Text(
                                                                text = opt,
                                                                style = MaterialTheme.typography.bodySmall.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 10.sp
                                                                ),
                                                                color = txt,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                    if (generalTindakLanjutVal.isNotBlank() && generalTindakLanjutVal != "-") {
                                                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.6.dp)
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(25.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .clickable {
                                                                    viewModel.deleteTindakLanjut(customer, isKunjungan = false)
                                                                    dropdownExpanded = false
                                                                }
                                                                .padding(horizontal = 6.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            Text(
                                                                text = "Bersihkan",
                                                                style = MaterialTheme.typography.bodySmall.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 10.sp
                                                                ),
                                                                color = Color(0xFFC62828),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if ((customer.menuType == "INTRAKOMP" || customer.menuType == "EKSTRAKOMP") && saving2 != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItemCol(label = "No Simpanan 2", value = saving2.noRekening)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItemCol(label = "Saldo 2", value = formatDoubleClean(saving2.saldo, formatter))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        // Empty spacer to maintain vertical column alignment
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))

                            // Section 2: STATUS KOLEKTIBILITAS
                            Text(
                                text = "STATUS KOLEKTIBILITAS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 0.5.sp),
                                color = Color(0xFFD32F2F)
                            )
                            if (customer.menuType != "EKSTRAKOMP") {
                                // Baris 1: Kolek Saat ini, Kolek Bulan Lalu, Flag
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val isGoneFromDaily = !isEkstrakomp && dailyInfo == null && dailyDataMap.isNotEmpty() && customer.osPinjaman > 0.0
                                        DetailItemColWithChange(
                                            label = "Kolek Saat Ini",
                                            oldValue = customer.kolek,
                                            newValue = if (!isEkstrakomp && dailyInfo != null) dailyInfo.kolek else if (isGoneFromDaily) "Lunas" else null,
                                            showChange = !isEkstrakomp && (dailyInfo != null || isGoneFromDaily)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItemCol(label = "Kolek Bulan Lalu", value = resolvedKolekBlnLalu)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        DetailItemCol(label = "Flag", value = customer.flag)
                                    }
                                }
                            }

                            // Baris 2: Mantri Pengelola, Mantri Prakarsa
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val cleanPn = customer.pnMantri.trim().filter { it.isDigit() }.trimStart('0').ifEmpty { customer.pnMantri.trim() }
                                    val rawName = if (customer.namaMantri.equals("Mikro", ignoreCase = true) || customer.namaMantri == "TIDAK TERIDENTIFIKASI" || customer.namaMantri.isBlank()) {
                                        if (cleanPn == "207803") "Nurul Mubin" else customer.namaMantri
                                    } else {
                                        customer.namaMantri
                                    }
                                    // User requested: "pada info detail debitur nama mantri pengelola tampilkan nama mantri saja hilangkan bagian no PN nya"
                                    val nameWithoutPn = if (rawName.contains("-")) {
                                        rawName.substringAfter("-").trim()
                                    } else {
                                        rawName.replace(Regex("^[0-9\\s-_]+"), "").trim()
                                    }
                                    val mantriDisplay = if (nameWithoutPn.isNotBlank() && nameWithoutPn != "TIDAK TERIDENTIFIKASI") {
                                        nameWithoutPn
                                    } else if (cleanPn == "207803") {
                                        "Nurul Mubin"
                                    } else if (rawName.isNotBlank() && !rawName.all { it.isDigit() }) {
                                        rawName
                                    } else {
                                        "-"
                                    }
                                    DetailItemCol(label = "Mantri Pengelola", value = mantriDisplay)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItemCol(label = "Mantri Prakarsa", value = customer.namaMantriPrakarsa)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    if (minAngsuran != null) {
                                        DetailItemCol(label = "Min Angsuran", value = formatDoubleClean(minAngsuran, formatter), valueColor = Color(0xFFD32F2F), showWarningIcon = true)
                                    } else {
                                        Spacer(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }

                            // 12 Kotak Kolek LW321 Bulanan (Januari - Desember)
                            var selectedHistoryMonth by remember(customer) { mutableStateOf<Int?>(null) }
                            var selectedHistoryIdx by remember(customer) { mutableStateOf(0) }
                            val currentMonth = java.time.LocalDate.now().monthValue
                            val monthsOrder = remember(currentMonth) { (0 until 12).map { i -> ((currentMonth - 1 + i) % 12) + 1 } }
                            val normalizeShortKolek: (String) -> String = remember {
                                { rawKol ->
                                    val rawClean = rawKol.trim().uppercase().replace(" ", "").replace("-", "")
                                    when {
                                        rawKol.isBlank() || rawKol == "-" -> "-"
                                        rawClean == "SML1" || rawClean == "DPK1" || rawClean == "2.1" || rawClean == "21" -> "2.1"
                                        rawClean == "SML2" || rawClean == "DPK2" || rawClean == "2.2" || rawClean == "22" -> "2.2"
                                        rawClean == "SML3" || rawClean == "DPK3" || rawClean == "2.3" || rawClean == "23" -> "2.3"
                                        rawClean == "1" || rawClean.contains("LANCAR") || rawClean == "L" -> "1"
                                        rawClean == "2" || rawClean.contains("SML") || rawClean.contains("DPK") -> "2"
                                        rawClean == "3" || rawClean.contains("KURANG") || rawClean == "KL" -> "3"
                                        rawClean == "4" || rawClean.contains("DIRAGUKAN") || rawClean == "D" -> "4"
                                        rawClean == "5" || rawClean.contains("MACET") || rawClean == "M" -> "5"
                                        else -> rawKol.take(3)
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "HISTORI KOLEK LW321 (12 BULAN)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 0.5.sp),
                                            color = Color(0xFF005FAF)
                                        )
                                        IconButton(
                                            onClick = {
                                                viewModel.loadMonthlyKolekForCustomer(customer.noRekening, customer.cif, forceRefresh = true)
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Refresh Riwayat Kolek",
                                                tint = Color(0xFF005FAF),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    if (isMonthlyKolekLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = Color(0xFF005FAF))
                                    }
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val selMonth = selectedHistoryMonth
                                    if (selMonth != null) {
                                        val monthNamesShort = remember {
                                            listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
                                        }
                                        val monthLabel = monthNamesShort.getOrElse(selMonth - 1) { "Bln $selMonth" }
                                        val selTgk = monthlyTunggakanHistory[selMonth]
                                        val selKol = normalizeShortKolek(monthlyKolekHistory[selMonth] ?: "-")

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .layout { measurable, constraints ->
                                                    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                                                    val slotCenterX = ((selectedHistoryIdx + 0.5f) / 12f * constraints.maxWidth).toInt()
                                                    val maxX = (constraints.maxWidth - placeable.width).coerceAtLeast(0)
                                                    val x = (slotCenterX - placeable.width / 2).coerceIn(0, maxX)
                                                    layout(constraints.maxWidth, placeable.height) {
                                                        placeable.placeRelative(x, 0)
                                                    }
                                                }
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.White,
                                                shadowElevation = 4.dp,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF005FAF)),
                                                modifier = Modifier
                                                    .clickable { selectedHistoryMonth = null }
                                                    .testTag("kolek_history_tunggakan_popup")
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "$monthLabel (Bln $selMonth)",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            color = Color(0xFF005FAF)
                                                        )
                                                        if (selKol != "-") {
                                                            Text(
                                                                text = "• Kol $selKol",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.SemiBold
                                                                ),
                                                                color = Color(0xFF475569)
                                                            )
                                                        }
                                                    }
                                                    val valueText = when {
                                                        isMonthlyKolekLoading && selTgk == null && !monthlyTunggakanHistory.containsKey(selMonth) -> "Memuat..."
                                                        selTgk != null -> formatDoubleClean(selTgk, formatter)
                                                        else -> "-"
                                                    }
                                                    Text(
                                                        text = "Tunggakan: $valueText",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = if (selTgk != null && selTgk > 0.0) Color(0xFFD32F2F) else Color(0xFF0F172A)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        monthsOrder.forEachIndexed { index, monthNum ->
                                            val rawKol = monthlyKolekHistory[monthNum] ?: "-"
                                            val shortKol = normalizeShortKolek(rawKol)
                                            val (bgColor, textColor, borderColor) = when {
                                                shortKol == "1" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Color(0xFFA5D6A7))
                                                shortKol == "2" || shortKol == "2.1" || shortKol == "2.2" || shortKol == "2.3" -> Triple(Color(0xFFFFF9C4), Color(0xFFF57F17), Color(0xFFFFEE58))
                                                shortKol == "3" -> Triple(Color(0xFFFFE0B2), Color(0xFFE65100), Color(0xFFFFB74D))
                                                shortKol == "4" -> Triple(Color(0xFFFFCCBC), Color(0xFFD84315), Color(0xFFFF8A65))
                                                shortKol == "5" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), Color(0xFFEF9A9A))
                                                else -> Triple(Color(0xFFF8FAFC), Color(0xFF94A3B8), Color(0xFFE2E8F0))
                                            }
                                            val isSelected = selectedHistoryMonth == monthNum

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        if (isSelected) {
                                                            selectedHistoryMonth = null
                                                        } else {
                                                            selectedHistoryMonth = monthNum
                                                            selectedHistoryIdx = index
                                                        }
                                                    }
                                                    .padding(vertical = 1.dp)
                                                    .testTag("kolek_history_month_$monthNum"),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(1.dp)
                                            ) {
                                                Text(
                                                    text = "$monthNum",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                                                    color = if (isSelected) Color(0xFF005FAF) else Color(0xFF475569)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(20.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(bgColor)
                                                        .border(
                                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                                            color = if (isSelected) Color(0xFF005FAF) else borderColor,
                                                            shape = RoundedCornerShape(3.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = shortKol,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = if (shortKol.length > 2) 7.5.sp else 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = textColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))

                            // Section 3: INFORMASI TUNGGAKAN
                            Text(
                                text = "INFORMASI TUNGGAKAN",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 0.5.sp),
                                color = Color(0xFFD32F2F)
                            )
                            // Baris 1: Tunggakan Pokok, Tunggakan Bunga, Penalty (Denda) (3 kolom)
                            val extraColsForHp = customer.extraColumns
                            val hpPairs = extraColsForHp.split("||").mapNotNull {
                                val parts = it.split("::")
                                if (parts.size >= 2) parts[0].trim() to parts[1].trim() else null
                            }.toMap()
                            val noHp1 = hpPairs["No HP 1"] ?: ""
                            val noHp2 = hpPairs["No HP 2"] ?: ""

                            val custKey = if (customer.noRekening.isNotBlank()) customer.noRekening else if (customer.cif.isNotBlank()) customer.cif else customer.namaNasabah
                            val tindakLanjutVal = tindakLanjutMap[custKey] ?: "-"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                             ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val isGoneFromDaily = !isEkstrakomp && dailyInfo == null && dailyDataMap.isNotEmpty() && customer.tunggakanPokok > 0.0
                                    DetailItemColWithChange(
                                        label = "Tunggakan Pokok",
                                        oldValue = formatDoubleClean(customer.tunggakanPokok, formatter),
                                        newValue = if (!isEkstrakomp && dailyInfo != null) formatDoubleClean(dailyInfo.tunggakanPokok, formatter) else if (isGoneFromDaily) "0" else null,
                                        showChange = !isEkstrakomp && (dailyInfo != null || isGoneFromDaily)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    val isGoneFromDaily = !isEkstrakomp && dailyInfo == null && dailyDataMap.isNotEmpty() && customer.tunggakanBunga > 0.0
                                    DetailItemColWithChange(
                                        label = "Tunggakan Bunga",
                                        oldValue = formatDoubleClean(customer.tunggakanBunga, formatter),
                                        newValue = if (!isEkstrakomp && dailyInfo != null) formatDoubleClean(dailyInfo.tunggakanBunga, formatter) else if (isGoneFromDaily) "0" else null,
                                        showChange = !isEkstrakomp && (dailyInfo != null || isGoneFromDaily)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    val isGoneFromDaily = !isEkstrakomp && dailyInfo == null && dailyDataMap.isNotEmpty() && customer.pinalty > 0.0
                                    DetailItemColWithChange(
                                        label = "Penalty (Denda)",
                                        oldValue = formatDoubleClean(customer.pinalty, formatter),
                                        newValue = if (!isEkstrakomp && dailyInfo != null) formatDoubleClean(dailyInfo.pinalty, formatter) else if (isGoneFromDaily) "0" else null,
                                        showChange = !isEkstrakomp && (dailyInfo != null || isGoneFromDaily)
                                    )
                                }
                            }



                            

                            val listPairs = remember(customer.extraColumns) {
                                customer.extraColumns.split("||").mapNotNull {
                                    val parts = it.split("::")
                                    if (parts.size >= 2) parts[0].trim() to parts[1].trim() else null
                                }.filter { 
                                    it.first != "isKelolaan" &&
                                    it.first != "No HP 1" &&
                                    it.first != "No HP 2" &&
                                    it.first != "UpdatePokok" &&
                                    it.first != "SetorPokok"
                                }
                            }

                            if (listPairs.isNotEmpty() && customer.menuType != "EKSTRAKOMP" && customer.menuType != "INTRAKOMP") {
                                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))

                                Text(
                                    text = "INFORMASI TAMBAHAN",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 0.5.sp),
                                    color = Color(0xFF005FAF)
                                )

                                listPairs.chunked(2).forEach { rowPairs ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowPairs.forEach { pair ->
                                            Column(modifier = Modifier.weight(1f)) {
                                                DetailItemCol(label = pair.first.uppercase(), value = formatStringValueClean(pair.second))
                                            }
                                        }
                                        if (rowPairs.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            val hasTunggakanChange = dailyInfo != null && (
                                dailyInfo.tunggakanPokok != customer.tunggakanPokok ||
                                dailyInfo.tunggakanBunga != customer.tunggakanBunga ||
                                dailyInfo.pinalty != customer.pinalty
                            )

                            val newTotalTunggakan = if (!isEkstrakomp && dailyInfo != null) {
                                if (dailyInfo.totalTunggakan > 0.0) {
                                    dailyInfo.totalTunggakan
                                } else {
                                    dailyInfo.tunggakanPokok + dailyInfo.tunggakanBunga + dailyInfo.pinalty
                                }
                            } else {
                                null
                            }

                            val isTotalGoneFromDaily = !isEkstrakomp && dailyInfo == null && dailyDataMap.isNotEmpty() && customer.totalTunggakan > 0.0
                            val updatedTotalTunggakan = when {
                                isTotalGoneFromDaily -> 0.0
                                newTotalTunggakan != null && newTotalTunggakan != customer.totalTunggakan -> newTotalTunggakan
                                else -> null
                            }
                            val totalTunggakanAnnotated = androidx.compose.ui.text.buildAnnotatedString {
                                if (updatedTotalTunggakan != null) {
                                    withStyle(
                                        style = androidx.compose.ui.text.SpanStyle(
                                            color = Color(0xFF8E8E93),
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                            fontWeight = FontWeight.Medium
                                        )
                                    ) {
                                        append(formatDoubleClean(customer.totalTunggakan, formatter))
                                    }
                                    withStyle(
                                        style = androidx.compose.ui.text.SpanStyle(
                                            color = Color(0xFF6B7280),
                                            fontWeight = FontWeight.Medium
                                        )
                                    ) {
                                        append(" -> ")
                                    }
                                    withStyle(
                                        style = androidx.compose.ui.text.SpanStyle(
                                            color = Color(0xFFC2185B),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    ) {
                                        append(formatDoubleClean(updatedTotalTunggakan, formatter))
                                    }
                                } else {
                                    append(formatDoubleClean(customer.totalTunggakan, formatter))
                                }
                            }

                            // Total Tunggakan row (Pink background as requested "warna merah muda")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFCE4EC), shape = RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total Tunggakan",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                    color = Color(0xFFC2185B)
                                )
                                Text(
                                    text = totalTunggakanAnnotated,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 11.sp),
                                    color = Color(0xFFC2185B)
                                )
                            }

                            // Display No HP 1, No HP 2, and Tindak Lanjut horizontally below Total Tunggakan (3 columns)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItemCol(
                                        label = "No HP 1",
                                        value = if (noHp1.isNotBlank()) formatStringValueClean(noHp1) else "-"
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItemCol(
                                        label = "No HP 2",
                                        value = if (noHp2.isNotBlank()) formatStringValueClean(noHp2) else "-"
                                    )
                                }
                                Column(modifier = Modifier.weight(1.2f)) {
                                    if (selectedMenu == "KUNJUNGAN") {
                                        Text(
                                            text = "TL KUNJUNGAN",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                                            color = Color(0xFF005FAF),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val dropdownOptions = listOf(
                                            "Bayar Lunas",
                                            "Bayar 1x angsuran",
                                            "Bayar Sebagian",
                                            "Janji Bayar",
                                            "Tidak Ditempat",
                                            "Pindah Alamat",
                                            "Meninggal Dunia"
                                        )
                                        val dropdownColors = mapOf(
                                            "Bayar Lunas" to Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32)),
                                            "Bayar 1x angsuran" to Pair(Color(0xFFE0F2F1), Color(0xFF00695C)),
                                            "Bayar Sebagian" to Pair(Color(0xFFE3F2FD), Color(0xFF1565C0)),
                                            "Janji Bayar" to Pair(Color(0xFFFFF3E0), Color(0xFFE65100)),
                                            "Tidak Ditempat" to Pair(Color(0xFFF3E5F5), Color(0xFF6A1B9A)),
                                            "Pindah Alamat" to Pair(Color(0xFFE8EAF6), Color(0xFF283593)),
                                            "Meninggal Dunia" to Pair(Color(0xFFECEFF1), Color(0xFF37474F))
                                        )
                                        var dropdownExpanded by remember { mutableStateOf(false) }
                                        Box {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(4.dp))
                                                    .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp))
                                                    .clickable { dropdownExpanded = true }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                val (bgOpt, txtOpt) = dropdownColors[kunjunganTindakLanjutVal] ?: Pair(Color.Transparent, Color(0xFF1D1B20))
                                                Text(
                                                    text = if (kunjunganTindakLanjutVal.isBlank() || kunjunganTindakLanjutVal == "-") "Pilih..." else kunjunganTindakLanjutVal,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                                    color = txtOpt,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Icon(
                                                    imageVector = if (dropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Dropdown",
                                                    tint = Color(0xFF005FAF),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }

                                            androidx.compose.material3.DropdownMenu(
                                                expanded = dropdownExpanded,
                                                onDismissRequest = { dropdownExpanded = false },
                                                modifier = Modifier
                                                    .width(140.dp)
                                                    .background(Color.White)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    dropdownOptions.forEach { opt ->
                                                        val (_, txt) = dropdownColors[opt] ?: Pair(Color.White, Color.Black)
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(25.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .clickable {
                                                                    viewModel.saveTindakLanjut(customer, opt, isKunjungan = true)
                                                                    dropdownExpanded = false
                                                                }
                                                                .padding(horizontal = 6.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            Text(
                                                                text = opt,
                                                                style = MaterialTheme.typography.bodySmall.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 10.sp
                                                                ),
                                                                color = txt,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                    if (kunjunganTindakLanjutVal.isNotBlank() && kunjunganTindakLanjutVal != "-") {
                                                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.6.dp)
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(25.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .clickable {
                                                                    viewModel.deleteTindakLanjut(customer, isKunjungan = true)
                                                                    dropdownExpanded = false
                                                                }
                                                                .padding(horizontal = 6.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            Text(
                                                                text = "Bersihkan",
                                                                style = MaterialTheme.typography.bodySmall.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 10.sp
                                                                ),
                                                                color = Color(0xFFC62828),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }


                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: WhatsApp Button & Ekstrakomp Buttons (SP1, SP2, SP3, Surat Panggilan)
                        val context = androidx.compose.ui.platform.LocalContext.current
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            IconButton(
                                onClick = {
                                    val numbers = mutableListOf<String>()
                                    val extraCols = customer.extraColumns
                                    if (extraCols.isNotBlank()) {
                                        val listPairs = extraCols.split("||").mapNotNull {
                                            val p = it.split("::")
                                            if (p.size >= 2) p[0].trim() to p[1].trim() else null
                                        }
                                        val phonePairs = listPairs.filter { pair ->
                                            val label = pair.first.lowercase()
                                            label.contains("hp") || label.contains("telp") || label.contains("telepon") || label.contains("wa") || label.contains("phone") || label.contains("kontak")
                                        }
                                        phonePairs.forEach { p ->
                                            val cleanVal = p.second.replace(Regex("[^0-9]"), "")
                                            if (cleanVal.length >= 6 && !numbers.contains(p.second)) {
                                                numbers.add(p.second)
                                            }
                                        }
                                    }
                                    
                                    val overrideTunggakan = if (dailyInfo != null) {
                                        if (dailyInfo.totalTunggakan > 0.0) {
                                            dailyInfo.totalTunggakan
                                        } else {
                                            dailyInfo.tunggakanPokok + dailyInfo.tunggakanBunga + dailyInfo.pinalty
                                        }
                                    } else {
                                        customer.totalTunggakan
                                    }

                                    if (numbers.size > 1) {
                                        waPhoneSelectorNumbers = numbers
                                        waPhoneSelectorCustomer = customer.copy(alamat = resolvedAddress)
                                    } else if (numbers.size == 1) {
                                        sendWhatsAppMessage(context, numbers[0], customer.copy(alamat = resolvedAddress), formatter, loggedInDetails, overrideTunggakan, templateWaDetailDebitur)
                                    } else {
                                        android.widget.Toast.makeText(context, "No HP tidak ditemukan", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_whatsapp),
                                    contentDescription = "Kirim Pesan WhatsApp",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            if (selectedMenu == "EKSTRAKOMP" || customer.menuType == "EKSTRAKOMP") {
                                val spButtons = listOf("SP1", "SP2", "SP3", "Panggilan")
                                spButtons.forEach { btnLabel ->
                                    androidx.compose.material3.OutlinedButton(
                                        onClick = {
                                            selectedSpType = com.example.ui.util.SpType.fromLabel(btnLabel)
                                            selectedSpCustomer = customer.copy(alamat = resolvedAddress)
                                            showSpLetterDialog = true
                                        },
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 1.dp, vertical = 0.dp),
                                        modifier = Modifier.width(if (btnLabel == "Panggilan") 46.dp else 28.dp).height(26.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFF005FAF)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF005FAF).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = btnLabel,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // Right: Other Actions
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedMenu == "KELOLAAN" || selectedMenu == "KUNJUNGAN" || (loggedInPN.isNotBlank() && customer.menuType == "INTRAKOMP" && (customer.pnMantri.trim() == loggedInPN.trim() || customer.namaMantri.contains(loggedInPN, ignoreCase = true)))) {
                                val uniqueKey = if (customer.noRekening.isNotBlank()) customer.noRekening else if (customer.cif.isNotBlank()) customer.cif else customer.namaNasabah
                                val isChecked = kunjunganKeys.contains(uniqueKey)
                                TextButton(
                                    onClick = {
                                        viewModel.toggleKunjungan(customer)
                                    },
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = if (isChecked) "Hapus Kunjungan" else "Tambah Kunjungan",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isChecked) Color(0xFFD32F2F) else Color(0xFF005FAF)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.material3.Button(
                                onClick = { showDetailDialogForCustomer = null },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF005FAF),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Tutup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSpLetterDialog && selectedSpCustomer != null) {
        SpLetterDialog(
            customer = selectedSpCustomer!!,
            spType = selectedSpType,
            onDismiss = {
                showSpLetterDialog = false
                selectedSpCustomer = null
            }
        )
    }

    showTindakLanjutPopupForCustomer?.let { customer ->
        var showManualInputMode by remember(customer) { mutableStateOf(false) }
        val custKey = if (customer.noRekening.isNotBlank()) customer.noRekening else if (customer.cif.isNotBlank()) customer.cif else customer.namaNasabah
        val mapKey = if (selectedMenu == "KUNJUNGAN") "kunjungan_$custKey" else custKey
        var manualTlText by remember(customer) { mutableStateOf(tindakLanjutMap[mapKey].orEmpty()) }

        val options = if (selectedMenu == "KUNJUNGAN") {
            listOf(
                "Bayar Lunas",
                "Bayar 1x angsuran",
                "Bayar Sebagian",
                "Janji Bayar",
                "Tidak Ditempat",
                "Pindah Alamat",
                "Meninggal Dunia"
            )
        } else {
            listOf(
                "Bayar Full",
                "Bayar Sebagian",
                "Tetap SML",
                "Bergeser NPL",
                "Restruk",
                "Janji Bayar"
            )
        }
        val optionsColors = mapOf(
            "Bayar Lunas" to Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32)),
            "Bayar 1x angsuran" to Pair(Color(0xFFE0F2F1), Color(0xFF00695C)),
            "Bayar Sebagian" to Pair(Color(0xFFE3F2FD), Color(0xFF1565C0)),
            "Janji Bayar" to Pair(Color(0xFFFFF3E0), Color(0xFFE65100)),
            "Tidak Ditempat" to Pair(Color(0xFFF3E5F5), Color(0xFF6A1B9A)),
            "Pindah Alamat" to Pair(Color(0xFFE8EAF6), Color(0xFF283593)),
            "Meninggal Dunia" to Pair(Color(0xFFECEFF1), Color(0xFF37474F)),
            "Bayar Full" to Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32)),
            "Tetap SML" to Pair(Color(0xFFFFF3E0), Color(0xFFE65100)),
            "Bergeser NPL" to Pair(Color(0xFFFFEBEE), Color(0xFFC62828)),
            "Restruk" to Pair(Color(0xFFF3E5F5), Color(0xFF6A1B9A))
        )
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showTindakLanjutPopupForCustomer = null }
        ) {
            Box(
                modifier = Modifier
                    .width(230.dp)
                    .background(Color.White, shape = RoundedCornerShape(14.dp))
                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), shape = RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (showManualInputMode) "Input Manual Tindak Lanjut" else "Pilih Tindak Lanjut",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF005FAF),
                            fontSize = 13.sp
                        )
                    )

                    if (showManualInputMode) {
                        OutlinedTextField(
                            value = manualTlText,
                            onValueChange = { manualTlText = it },
                            placeholder = { Text("Tulis tindak lanjut...", fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showManualInputMode = false },
                                modifier = Modifier.weight(1f).height(34.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Kembali", fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    val trimmed = manualTlText.trim()
                                    if (trimmed.isNotEmpty()) {
                                        viewModel.saveTindakLanjut(customer, trimmed, isKunjungan = (selectedMenu == "KUNJUNGAN"))
                                        showTindakLanjutPopupForCustomer = null
                                    }
                                },
                                enabled = manualTlText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                                modifier = Modifier.weight(1f).height(34.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Simpan", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            options.forEach { opt ->
                                val (containerCol, textCol) = optionsColors[opt] ?: Pair(Color(0xFFF3F4F6), Color(0xFF1F2937))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .background(containerCol, shape = RoundedCornerShape(6.dp))
                                        .clickable {
                                            viewModel.saveTindakLanjut(customer, opt, isKunjungan = (selectedMenu == "KUNJUNGAN"))
                                            showTindakLanjutPopupForCustomer = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = textCol,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                                    .background(Color(0xFFEBF5FF), shape = RoundedCornerShape(6.dp))
                                    .clickable { showManualInputMode = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Input Manual",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF005FAF),
                                        fontSize = 11.5.sp
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                                    .background(Color(0xFFFFEBEE), shape = RoundedCornerShape(6.dp))
                                    .clickable {
                                        viewModel.deleteTindakLanjut(customer, isKunjungan = (selectedMenu == "KUNJUNGAN"))
                                        showTindakLanjutPopupForCustomer = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Bersihkan",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828),
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }

                        TextButton(
                            onClick = { showTindakLanjutPopupForCustomer = null },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Batal",
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
    AnimatedVisibility(
        visible = showSettingsDrawer,
        enter = slideInHorizontally(initialOffsetX = { -it }),
        exit = slideOutHorizontally(targetOffsetX = { -it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { showSettingsDrawer = false }
        ) {
            Box(
                modifier = Modifier
                    .padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                    .fillMaxHeight()
                    .fillMaxWidth(0.85f) // 85% width from left to right
                    .shadow(elevation = 16.dp, shape = RectangleShape, clip = false)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(enabled = false) { /* Prevent clicks through */ }
                    .align(Alignment.CenterStart)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Sidebar Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF005FAF))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val loggedInPN by viewModel.loggedInPN.collectAsState()
                                val loggedInDetails by viewModel.loggedInMantriDetails.collectAsState()
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.logobaru1),
                                        contentDescription = "Logo MonBer",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                        modifier = Modifier
                                            .height(36.dp)
                                            .wrapContentWidth()
                                    )
                                    Column {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = "MonBer App ",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "*Beta Tester",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                                fontWeight = FontWeight.Normal,
                                                color = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.padding(bottom = 2.dp)
                                            )
                                        }
                                        if (loggedInDetails.isNotBlank()) {
                                            Text(
                                                text = loggedInDetails,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = { showLoginDialog = true }) {
                                    Icon(Icons.Default.Person, "Login", tint = Color.White)
                                }
                            }
                        }
                    }
                    


                    // Confirmation Logoff Dialog

                    
                    // 2. Sidebar Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Select Menu block (MOVED TO TOP) - nested Column to minimize distance
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PILIH MENU",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = { showDetailUnitDialog = true },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        "Detail Unit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("INTRAKOMP", "EKSTRAKOMP").forEach { tab ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (selectedMenu == tab) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (selectedMenu == tab) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                viewModel.selectMenu(tab)
                                                // DO NOT NAVIGATE
                                            }
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tab,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedMenu == tab) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Section: Periode Selection
                        Text(
                            text = "PERIODE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        var periodDropdownExpanded by remember { mutableStateOf(false) }
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                                    .clickable(enabled = selectedMenu != "EKSTRAKOMP") { periodDropdownExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        val periodLabel = if (selectedMenu == "EKSTRAKOMP") "Periode LW325" else when {
                                            selectedPeriodSource.startsWith("BULAN_") -> {
                                                val m = selectedPeriodSource.removePrefix("BULAN_").toIntOrNull() ?: 1
                                                val mNames = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
                                                "LW321 " + mNames.getOrElse(m - 1) { "Bulan $m" }
                                            }
                                            selectedPeriodSource == "BULAN_LALU" -> "Bulan Lalu"
                                            selectedPeriodSource == "DAILY" -> "Daily"
                                            else -> "Akhir Bulan"
                                        }
                                        val periodDate = if (selectedMenu == "EKSTRAKOMP") periodDateLw325 else when {
                                            selectedPeriodSource.startsWith("BULAN_") -> {
                                                val m = selectedPeriodSource.removePrefix("BULAN_").toIntOrNull() ?: 1
                                                val mNames = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
                                                mNames.getOrElse(m - 1) { "Bulan $m" }
                                            }
                                            selectedPeriodSource == "BULAN_LALU" -> periodDateBulanLalu
                                            selectedPeriodSource == "DAILY" -> periodDateDaily
                                            else -> periodDateAkhirBulan
                                        }
                                        Text(
                                            text = periodLabel,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (periodDate.isNotBlank()) {
                                            Text(
                                                text = "($periodDate)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                            )
                                        } else {
                                            Text(
                                                text = if (selectedMenu == "EKSTRAKOMP") "Belum ada periode LW325" else "Tap untuk pilih periode",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                                if (selectedMenu != "EKSTRAKOMP") {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            DropdownMenu(
                                expanded = periodDropdownExpanded && selectedMenu != "EKSTRAKOMP",
                                onDismissRequest = { periodDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                val cleanBulanLalu = if (periodDateBulanLalu.isNotBlank()) " ($periodDateBulanLalu)" else ""
                                val cleanAkhirBulan = if (periodDateAkhirBulan.isNotBlank()) " ($periodDateAkhirBulan)" else ""
                                val cleanDaily = if (periodDateDaily.isNotBlank()) " ($periodDateDaily)" else ""
                                
                                DropdownMenuItem(
                                    text = { Text("Lw321 Bulan Lalu$cleanBulanLalu", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)) },
                                    leadingIcon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        viewModel.selectPeriodSource("BULAN_LALU")
                                        periodDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Lw321 Akhir Bulan$cleanAkhirBulan", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)) },
                                    leadingIcon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        viewModel.selectPeriodSource("AKHIR_BULAN")
                                        periodDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Lw321 Daily$cleanDaily", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)) },
                                    leadingIcon = { Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        viewModel.selectPeriodSource("DAILY")
                                        periodDropdownExpanded = false
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "LW321 ALL (JANUARI - DESEMBER)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )

                                val monthNamesList = listOf(
                                    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                                    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                                )
                                monthNamesList.forEachIndexed { idx, name ->
                                    val monthIdx = idx + 1
                                    val isConfigured = (viewModel.lw321AllUrls[monthIdx]?.value ?: "").isNotBlank()
                                    val isCurrent = selectedPeriodSource == "BULAN_$monthIdx"
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "LW321 $name",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified
                                                    )
                                                )
                                                if (isConfigured) {
                                                    Text(
                                                        text = "Tersedia",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                                    )
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isCurrent) Icons.Default.CheckCircle else Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        onClick = {
                                            viewModel.selectPeriodSource("BULAN_$monthIdx")
                                            periodDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        val totalOs = summaryStats.totalOs
                        val lancarPct = if (totalOs > 0) (summaryStats.osLancar / totalOs * 100) else 0.0
                        val smlPct = if (totalOs > 0) (summaryStats.osSmlTotal / totalOs * 100) else 0.0
                        val nplPct = if (totalOs > 0) (summaryStats.osNplTotal / totalOs * 100) else 0.0

                        // Section 1: Total OS Kelolaan
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Total OS Kelolaan",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFF00386B)
                            )
                            InfoBox(
                                title = "OS Pinjaman",
                                osVal = summaryStats.totalOs,
                                debtorCount = summaryStats.totalCount,
                                percentage = null,
                                formatter = formatter,
                                isSelected = filterKolek == "SEMUA" && filterSegmen == "SEMUA",
                                onClick = {
                                    viewModel.filterKolek.value = "SEMUA"
                                    viewModel.filterSegmen.value = "SEMUA"
                                    showSettingsDrawer = false
                                }
                            )
                        }

                        // Section 2: Kolek Lancar
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Kolek Lancar",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFF15803D)
                            )
                            InfoBox(
                                title = "OS Lancar",
                                osVal = summaryStats.osLancar,
                                debtorCount = summaryStats.lancarCount,
                                percentage = lancarPct,
                                formatter = formatter,
                                isSelected = filterKolek == "Lancar",
                                onClick = {
                                    viewModel.filterKolek.value = "Lancar"
                                    viewModel.filterSegmen.value = "SEMUA"
                                    showSettingsDrawer = false
                                }
                            )
                        }

                        // Section 3: Kolek SML
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Kolek SML",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFFD97706)
                            )
                            InfoBox(
                                title = "OS SML (sml1+2+3)",
                                osVal = summaryStats.osSmlTotal,
                                debtorCount = summaryStats.smlTotalCount,
                                percentage = smlPct,
                                formatter = formatter,
                                isSelected = filterKolek == "SML",
                                onClick = {
                                    viewModel.filterKolek.value = "SML"
                                    viewModel.filterSegmen.value = "SEMUA"
                                    showSettingsDrawer = false
                                }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MiniInfoBox(
                                    title = "SML1",
                                    osVal = summaryStats.osSml1,
                                    debtorCount = summaryStats.sml1Count,
                                    formatter = formatter,
                                    isSelected = filterKolek == "SML1",
                                    onClick = {
                                        viewModel.filterKolek.value = "SML1"
                                        viewModel.filterSegmen.value = "SEMUA"
                                        showSettingsDrawer = false
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                MiniInfoBox(
                                    title = "SML2",
                                    osVal = summaryStats.osSml2,
                                    debtorCount = summaryStats.sml2Count,
                                    formatter = formatter,
                                    isSelected = filterKolek == "SML2",
                                    onClick = {
                                        viewModel.filterKolek.value = "SML2"
                                        viewModel.filterSegmen.value = "SEMUA"
                                        showSettingsDrawer = false
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                MiniInfoBox(
                                    title = "SML3",
                                    osVal = summaryStats.osSml3,
                                    debtorCount = summaryStats.sml3Count,
                                    formatter = formatter,
                                    isSelected = filterKolek == "SML3",
                                    onClick = {
                                        viewModel.filterKolek.value = "SML3"
                                        viewModel.filterSegmen.value = "SEMUA"
                                        showSettingsDrawer = false
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Section 4: Kolek NPL
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Kolek NPL",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFFDC2626)
                            )
                            InfoBox(
                                title = "OS NPL (K.Lancar+Diragukan+Macet)",
                                osVal = summaryStats.osNplTotal,
                                debtorCount = summaryStats.nplCount,
                                percentage = nplPct,
                                formatter = formatter,
                                isSelected = filterKolek == "NPL",
                                onClick = {
                                    viewModel.filterKolek.value = "NPL"
                                    viewModel.filterSegmen.value = "SEMUA"
                                    showSettingsDrawer = false
                                }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MiniInfoBox(
                                    title = "Kurang Lancar",
                                    osVal = summaryStats.osKl,
                                    debtorCount = summaryStats.klCount,
                                    formatter = formatter,
                                    isSelected = filterKolek == "Kurang Lancar",
                                    onClick = {
                                        viewModel.filterKolek.value = "Kurang Lancar"
                                        viewModel.filterSegmen.value = "SEMUA"
                                        showSettingsDrawer = false
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                MiniInfoBox(
                                    title = "Diragukan",
                                    osVal = summaryStats.osD,
                                    debtorCount = summaryStats.dCount,
                                    formatter = formatter,
                                    isSelected = filterKolek == "Diragukan",
                                    onClick = {
                                        viewModel.filterKolek.value = "Diragukan"
                                        viewModel.filterSegmen.value = "SEMUA"
                                        showSettingsDrawer = false
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                MiniInfoBox(
                                    title = "Macet",
                                    osVal = summaryStats.osM,
                                    debtorCount = summaryStats.mCount,
                                    formatter = formatter,
                                    isSelected = filterKolek == "Macet",
                                    onClick = {
                                        viewModel.filterKolek.value = "Macet"
                                        viewModel.filterSegmen.value = "SEMUA"
                                        showSettingsDrawer = false
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Section 5: Segmen Kredit
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Segmen Kredit",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFF00386B)
                            )
                            if (summaryStats.allSegments.isNotEmpty()) {
                                summaryStats.allSegments.chunked(3).forEach { rowSegments ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowSegments.forEach { segInfo ->
                                            MiniInfoBox(
                                                title = segInfo.first,
                                                osVal = segInfo.second,
                                                debtorCount = segInfo.third,
                                                formatter = formatter,
                                                isSelected = filterSegmen.trim().uppercase() == segInfo.first.trim().uppercase(),
                                                onClick = {
                                                    viewModel.filterSegmen.value = if (filterSegmen.trim().uppercase() == segInfo.first.trim().uppercase()) "SEMUA" else segInfo.first
                                                    viewModel.filterKolek.value = "SEMUA"
                                                    showSettingsDrawer = false
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (rowSegments.size < 3) {
                                            repeat(3 - rowSegments.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    MiniInfoBox(
                                        title = summaryStats.seg1Name,
                                        osVal = summaryStats.seg1Os,
                                        debtorCount = summaryStats.seg1Count,
                                        formatter = formatter,
                                        isSelected = filterSegmen.trim().uppercase() == summaryStats.seg1Name.trim().uppercase(),
                                        onClick = {
                                            viewModel.filterSegmen.value = if (filterSegmen.trim().uppercase() == summaryStats.seg1Name.trim().uppercase()) "SEMUA" else summaryStats.seg1Name
                                            viewModel.filterKolek.value = "SEMUA"
                                            showSettingsDrawer = false
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    MiniInfoBox(
                                        title = summaryStats.seg2Name,
                                        osVal = summaryStats.seg2Os,
                                        debtorCount = summaryStats.seg2Count,
                                        formatter = formatter,
                                        isSelected = filterSegmen.trim().uppercase() == summaryStats.seg2Name.trim().uppercase(),
                                        onClick = {
                                            viewModel.filterSegmen.value = if (filterSegmen.trim().uppercase() == summaryStats.seg2Name.trim().uppercase()) "SEMUA" else summaryStats.seg2Name
                                            viewModel.filterKolek.value = "SEMUA"
                                            showSettingsDrawer = false
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    MiniInfoBox(
                                        title = summaryStats.seg3Name,
                                        osVal = summaryStats.seg3Os,
                                        debtorCount = summaryStats.seg3Count,
                                        formatter = formatter,
                                        isSelected = filterSegmen.trim().uppercase() == summaryStats.seg3Name.trim().uppercase(),
                                        onClick = {
                                            viewModel.filterSegmen.value = if (filterSegmen.trim().uppercase() == summaryStats.seg3Name.trim().uppercase()) "SEMUA" else summaryStats.seg3Name
                                            viewModel.filterKolek.value = "SEMUA"
                                            showSettingsDrawer = false
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        if (selectedMenu == "INTRAKOMP") {
                            // Section 6: Kolek Update (Downgrade & Upgrade)
                            val (kolekDowngradeRowsState, kolekUpgradeListState) = remember(customersList, dailyDataMap) {
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

                                var lToSml1Os = 0.0; var lToSml1Count = 0
                                var sml1ToSml2Os = 0.0; var sml1ToSml2Count = 0
                                var sml2ToSml3Os = 0.0; var sml2ToSml3Count = 0
                                
                                var sml3ToKlOs = 0.0; var sml3ToKlCount = 0
                                var klToDOs = 0.0; var klToDCount = 0
                                var dToMOs = 0.0; var dToMCount = 0
                                
                                var lunasOs = 0.0; var lunasCount = 0
                                var dhOs = 0.0; var dhCount = 0

                                val upgradeMap = mutableMapOf<String, Pair<Double, Int>>()

                                customersList.forEach { cust ->
                                    val oldK = cust.kolek
                                    val dailyCust = dailyDataMap[cust.noRekening]
                                    val hasNoDaily = dailyCust == null || dailyCust.kolek.isBlank() || dailyCust.osPinjaman <= 0.0
                                    val (oldCode, oldRank) = parseKolekCodeAndRank(oldK)

                                    if (hasNoDaily) {
                                        if (oldRank == 7) {
                                            dhOs += cust.osPinjaman
                                            dhCount++
                                        } else if (oldRank in 1..6) {
                                            lunasOs += cust.osPinjaman
                                            lunasCount++
                                        }
                                    } else {
                                        val (newCode, newRank) = parseKolekCodeAndRank(dailyCust!!.kolek)
                                        if (oldRank in 1..7 && newRank in 1..7) {
                                            if (newRank > oldRank) {
                                                when {
                                                    oldCode == "L" && newCode == "SML1" -> { lToSml1Os += cust.osPinjaman; lToSml1Count++ }
                                                    oldCode == "SML1" && newCode == "SML2" -> { sml1ToSml2Os += cust.osPinjaman; sml1ToSml2Count++ }
                                                    oldCode == "SML2" && newCode == "SML3" -> { sml2ToSml3Os += cust.osPinjaman; sml2ToSml3Count++ }
                                                    oldCode == "SML3" && newCode == "KL" -> { sml3ToKlOs += cust.osPinjaman; sml3ToKlCount++ }
                                                    oldCode == "KL" && newCode == "D" -> { klToDOs += cust.osPinjaman; klToDCount++ }
                                                    oldCode == "D" && newCode == "M" -> { dToMOs += cust.osPinjaman; dToMCount++ }
                                                }
                                            } else if (newRank < oldRank) {
                                                val key = "$oldCode ➔ $newCode"
                                                val cur = upgradeMap.getOrDefault(key, 0.0 to 0)
                                                upgradeMap[key] = (cur.first + cust.osPinjaman) to (cur.second + 1)
                                            }
                                        }
                                    }
                                }

                                val downgradeRows = listOf(
                                    // Baris 1
                                    listOf(
                                        Triple("L ➔ SML1", lToSml1Os, lToSml1Count),
                                        Triple("SML1 ➔ SML2", sml1ToSml2Os, sml1ToSml2Count),
                                        Triple("SML2 ➔ SML3", sml2ToSml3Os, sml2ToSml3Count)
                                    ),
                                    // Baris 2
                                    listOf(
                                        Triple("SML3 ➔ KL", sml3ToKlOs, sml3ToKlCount),
                                        Triple("KL ➔ D", klToDOs, klToDCount),
                                        Triple("D ➔ M", dToMOs, dToMCount)
                                    ),
                                    // Baris 3
                                    listOf(
                                        Triple("Lunas", lunasOs, lunasCount),
                                        Triple("DH", dhOs, dhCount)
                                    )
                                )

                                val primaryUpgradeKeys = listOf(
                                    "SML1 ➔ L",
                                    "SML2 ➔ SML1",
                                    "SML3 ➔ SML2",
                                    "KL ➔ SML3",
                                    "D ➔ KL",
                                    "M ➔ KL"
                                )

                                val finalUpgrades = mutableListOf<Triple<String, Double, Int>>()
                                primaryUpgradeKeys.forEach { key ->
                                    val pair = upgradeMap[key] ?: (0.0 to 0)
                                    finalUpgrades.add(Triple(key, pair.first, pair.second))
                                }

                                upgradeMap.forEach { (key, pair) ->
                                    if (key !in primaryUpgradeKeys && pair.second > 0) {
                                        finalUpgrades.add(Triple(key, pair.first, pair.second))
                                    }
                                }

                                Pair(downgradeRows, finalUpgrades)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Kolek Update (Downgrade)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFF00386B)
                                )
                                
                                // Baris 1 (3 kolom)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    kolekDowngradeRowsState[0].forEach { info ->
                                        MiniInfoBox(
                                            title = info.first,
                                            osVal = info.second,
                                            debtorCount = info.third,
                                            formatter = formatter,
                                            isSelected = (filterKolekUpdate == info.first),
                                            onClick = {
                                                val nextVal = if (filterKolekUpdate == info.first) "SEMUA" else info.first
                                                viewModel.filterKolekUpdate.value = nextVal
                                                showSettingsDrawer = false
                                                android.widget.Toast.makeText(
                                                    context,
                                                    if (nextVal == "SEMUA") "Filter Kolek Update Dihapus" else "Filter: ${info.first}",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                
                                // Baris 2 (3 kolom)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    kolekDowngradeRowsState[1].forEach { info ->
                                        MiniInfoBox(
                                            title = info.first,
                                            osVal = info.second,
                                            debtorCount = info.third,
                                            formatter = formatter,
                                            isSelected = (filterKolekUpdate == info.first),
                                            onClick = {
                                                val nextVal = if (filterKolekUpdate == info.first) "SEMUA" else info.first
                                                viewModel.filterKolekUpdate.value = nextVal
                                                showSettingsDrawer = false
                                                android.widget.Toast.makeText(
                                                    context,
                                                    if (nextVal == "SEMUA") "Filter Kolek Update Dihapus" else "Filter: ${info.first}",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                
                                // Baris 3 (2 kolom)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    kolekDowngradeRowsState[2].forEach { info ->
                                        MiniInfoBox(
                                            title = info.first,
                                            osVal = info.second,
                                            debtorCount = info.third,
                                            formatter = formatter,
                                            isSelected = (filterKolekUpdate == info.first),
                                            onClick = {
                                                val nextVal = if (filterKolekUpdate == info.first) "SEMUA" else info.first
                                                viewModel.filterKolekUpdate.value = nextVal
                                                showSettingsDrawer = false
                                                android.widget.Toast.makeText(
                                                    context,
                                                    if (nextVal == "SEMUA") "Filter Kolek Update Dihapus" else "Filter: ${info.first}",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Section 6b: Kolek Update (Upgrade)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Kolek Update (Upgrade)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFF00386B)
                                )

                                kolekUpgradeListState.chunked(3).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowItems.forEach { info ->
                                            MiniInfoBox(
                                                title = info.first,
                                                osVal = info.second,
                                                debtorCount = info.third,
                                                formatter = formatter,
                                                isSelected = (filterKolekUpdate == info.first),
                                                onClick = {
                                                    val nextVal = if (filterKolekUpdate == info.first) "SEMUA" else info.first
                                                    viewModel.filterKolekUpdate.value = nextVal
                                                    showSettingsDrawer = false
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        if (nextVal == "SEMUA") "Filter Kolek Update Dihapus" else "Filter: ${info.first}",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        repeat(3 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Section 7: Kolek Bulan Lalu
                            val kolekBlnLaluRowsState = remember(customersList) {
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

                                var lToSml1Os = 0.0; var lToSml1Count = 0
                                var sml1ToSml2Os = 0.0; var sml1ToSml2Count = 0
                                var sml2ToSml3Os = 0.0; var sml2ToSml3Count = 0
                                 
                                var sml3ToKlOs = 0.0; var sml3ToKlCount = 0
                                var klToDOs = 0.0; var klToDCount = 0
                                var dToMOs = 0.0; var dToMCount = 0

                                customersList.forEach { cust ->
                                    val oldK = cust.kolekBlnLalu
                                    val newK = cust.kolek
                                    when {
                                        isKolekL(oldK) && isKolekSml1(newK) -> {
                                            lToSml1Os += cust.osPinjaman
                                            lToSml1Count++
                                        }
                                        isKolekSml1(oldK) && isKolekSml2(newK) -> {
                                            sml1ToSml2Os += cust.osPinjaman
                                            sml1ToSml2Count++
                                        }
                                        isKolekSml2(oldK) && isKolekSml3(newK) -> {
                                            sml2ToSml3Os += cust.osPinjaman
                                            sml2ToSml3Count++
                                        }
                                        isKolekSml3(oldK) && isKolekKl(newK) -> {
                                            sml3ToKlOs += cust.osPinjaman
                                            sml3ToKlCount++
                                        }
                                        isKolekKl(oldK) && isKolekD(newK) -> {
                                            klToDOs += cust.osPinjaman
                                            klToDCount++
                                        }
                                        isKolekD(oldK) && isKolekM(newK) -> {
                                            dToMOs += cust.osPinjaman
                                            dToMCount++
                                        }
                                    }
                                }

                                listOf(
                                    // Baris 1
                                    listOf(
                                        Triple("L ➔ SML1", lToSml1Os, lToSml1Count),
                                        Triple("SML1 ➔ SML2", sml1ToSml2Os, sml1ToSml2Count),
                                        Triple("SML2 ➔ SML3", sml2ToSml3Os, sml2ToSml3Count)
                                    ),
                                    // Baris 2
                                    listOf(
                                        Triple("SML3 ➔ KL", sml3ToKlOs, sml3ToKlCount),
                                        Triple("KL ➔ D", klToDOs, klToDCount),
                                        Triple("D ➔ M", dToMOs, dToMCount)
                                    )
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Kolek Bulan Lalu",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFF00386B)
                                )
                                
                                // Baris 1 (3 kolom)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    kolekBlnLaluRowsState[0].forEach { info ->
                                        MiniInfoBox(
                                            title = info.first,
                                            osVal = info.second,
                                            debtorCount = info.third,
                                            formatter = formatter,
                                            isSelected = (filterKolekBlnLalu == info.first),
                                            onClick = {
                                                val nextVal = if (filterKolekBlnLalu == info.first) "SEMUA" else info.first
                                                viewModel.filterKolekBlnLalu.value = nextVal
                                                showSettingsDrawer = false
                                                android.widget.Toast.makeText(
                                                    context,
                                                    if (nextVal == "SEMUA") "Filter Kolek Bulan Lalu Dihapus" else "Filter: ${info.first}",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                
                                // Baris 2 (3 kolom)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    kolekBlnLaluRowsState[1].forEach { info ->
                                        MiniInfoBox(
                                            title = info.first,
                                            osVal = info.second,
                                            debtorCount = info.third,
                                            formatter = formatter,
                                            isSelected = (filterKolekBlnLalu == info.first),
                                            onClick = {
                                                val nextVal = if (filterKolekBlnLalu == info.first) "SEMUA" else info.first
                                                viewModel.filterKolekBlnLalu.value = nextVal
                                                showSettingsDrawer = false
                                                android.widget.Toast.makeText(
                                                    context,
                                                    if (nextVal == "SEMUA") "Filter Kolek Bulan Lalu Dihapus" else "Filter: ${info.first}",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    // 3. Sidebar Footer (Settings Buttons)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Online URL Button
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (!isOfflineModeEnabled) MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    showSettingsDialog = true
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "Online URL",
                                tint = if (!isOfflineModeEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Online URL",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (!isOfflineModeEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Masukan Link spreadsheet",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 9.sp),
                                    color = (if (!isOfflineModeEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Mode Offline Button
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isOfflineModeEnabled) MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    showOfflineDialog = true
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Icon(
                                imageVector = Icons.Default.SdCard,
                                contentDescription = "Mode Offline",
                                tint = if (isOfflineModeEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Mode Offline",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isOfflineModeEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Pilih file dari penyimpanan",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 9.sp),
                                    color = (if (isOfflineModeEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNpdMultiSelectDialog) {
        val npdDates = remember(availableNpds) {
            availableNpds.filter { it != "SEMUA" && it.isNotBlank() }.toSet()
        }
        val initialSelected = remember(filterNpd) {
            if (filterNpd.startsWith("MULTI:")) {
                filterNpd.substringAfter("MULTI:").split(",").filter { it.isNotEmpty() }.toSet()
            } else if (filterNpd != "SEMUA" && filterNpd.isNotBlank()) {
                setOf(filterNpd)
            } else {
                emptySet<String>()
            }
        }
        var tempSelected by remember { mutableStateOf<Set<String>>(initialSelected) }

        AlertDialog(
            onDismissRequest = { showNpdMultiSelectDialog = false },
            title = {
                Column {
                    Text(
                        "Pilih Kalender NPD",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = if (tempSelected.isEmpty()) "Belum ada tanggal dipilih" else "${tempSelected.size} Tanggal Dipilih: ${tempSelected.sortedWith(compareBy { it.toIntOrNull() ?: Int.MAX_VALUE }).joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = { tempSelected = npdDates },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ada Data (${npdDates.size})", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                        }
                        TextButton(
                            onClick = { tempSelected = (1..31).map { it.toString() }.toSet() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pilih (1-31)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                        }
                        TextButton(
                            onClick = { tempSelected = emptySet<String>() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hapus", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Red))
                        }
                    }

                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 7-column Calendar Grid (days 1 through 31)
                    val days = (1..31).map { it.toString() }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        days.chunked(7).forEach { weekChunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                weekChunk.forEach { dayStr ->
                                    val isSelected = tempSelected.contains(dayStr)
                                    val hasData = npdDates.contains(dayStr)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    hasData -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                                    else -> Color(0xFFF1F5F9)
                                                }
                                            )
                                            .border(
                                                width = if (isSelected) 0.dp else if (hasData) 1.dp else 0.5.dp,
                                                color = if (isSelected) Color.Transparent else if (hasData) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = RoundedCornerShape(50)
                                            )
                                            .clickable {
                                                tempSelected = if (isSelected) tempSelected - dayStr else tempSelected + dayStr
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayStr,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (isSelected || hasData) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 11.sp
                                                ),
                                                color = when {
                                                    isSelected -> Color.White
                                                    hasData -> MaterialTheme.colorScheme.primary
                                                    else -> Color.DarkGray
                                                }
                                            )
                                            if (hasData && !isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(3.dp)
                                                        .clip(RoundedCornerShape(50))
                                                        .background(MaterialTheme.colorScheme.primary)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (weekChunk.size < 7) {
                                    repeat(7 - weekChunk.size) {
                                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    }
                                }
                            }
                        }
                    }

                    val nonNumericNpds = npdDates.filter { day -> day.toIntOrNull() == null || day.toInt() < 1 || day.toInt() > 31 }
                    if (nonNumericNpds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tanggal Lainnya:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            nonNumericNpds.forEach { otherDate ->
                                val isSelected = tempSelected.contains(otherDate)
                                androidx.compose.material3.Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable {
                                        tempSelected = if (isSelected) tempSelected - otherDate else tempSelected + otherDate
                                    }
                                ) {
                                    Text(
                                        text = otherDate,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White else Color.DarkGray,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempSelected.isEmpty()) {
                            viewModel.filterNpd.value = "SEMUA"
                        } else if (tempSelected.size == 1) {
                            viewModel.filterNpd.value = tempSelected.first()
                        } else {
                            viewModel.filterNpd.value = "MULTI:" + tempSelected.sortedWith(compareBy { it.toIntOrNull() ?: Int.MAX_VALUE }).joinToString(",")
                        }
                        showNpdMultiSelectDialog = false
                    }
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNpdMultiSelectDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    // Global access & login dialogs
    if (showLoginDialog) {
        PnLoginPopupDialog(
            initialPn = viewModel.loggedInPN.value,
            onDismiss = { showLoginDialog = false },
            onLogin = { pn ->
                viewModel.saveLogin(pn)
                showLoginDialog = false
            },
            onLogoffClick = { showConfirmLogoff = true }
        )
    }

    if (showConfirmLogoff) {
        AlertDialog(
            onDismissRequest = { showConfirmLogoff = false },
            title = { Text("Konfirmasi") },
            text = { Text("Apakah Anda yakin ingin keluar dari sesi login ini?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.logoff()
                    showConfirmLogoff = false
                    showLoginDialog = false
                }) { Text("Ya") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmLogoff = false }) { Text("Tidak") }
            }
        )
    }

    if (showTusimPopup) {
        AnimatedFadeSlideDialog(onDismiss = { showTusimPopup = false }) { requestDismiss ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daftar TUSIM (Simpanan > 25.000)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001C3B)
                        )
                        IconButton(onClick = requestDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (tusimPopupItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada nasabah dengan simpanan > 25.000",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    } else {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF005FAF))
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Nama Nasabah",
                                modifier = Modifier.weight(1.4f),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = Color.White
                            )
                            Text(
                                text = "No. Pinjaman",
                                modifier = Modifier.weight(1.2f),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = Color.White
                            )
                            Text(
                                text = "No. Simpanan",
                                modifier = Modifier.weight(1.2f),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = Color.White
                            )
                            Text(
                                text = "Saldo",
                                modifier = Modifier.weight(1.0f),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                        
                        HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
                        
                        val tusimListState = rememberLazyListState()
                        val pagedTusimItems = rememberLazyPagedItems(
                            items = tusimPopupItems,
                            listState = tusimListState,
                            pageSize = 30,
                            prefetchDistance = 6
                        )
                        LazyColumn(
                            state = tusimListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                        ) {
                            itemsIndexed(pagedTusimItems.visibleItems) { idx, item ->
                                val rowBg = if (idx % 2 == 0) Color.White else Color(0xFFF5F9FD)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                        .background(rowBg)
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.namaNasabah.uppercase(),
                                        modifier = Modifier.weight(1.4f),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = Color(0xFF001C3B),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.noPinjaman,
                                        modifier = Modifier.weight(1.2f),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = Color(0xFF001C3B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.noSimpanan,
                                        modifier = Modifier.weight(1.2f),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = Color(0xFF001C3B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatDoubleClean(item.saldo, formatter),
                                        modifier = Modifier.weight(1.0f),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = Color(0xFF2E7D32),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                        maxLines = 1
                                    )
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                            }

                            if (pagedTusimItems.hasMore) {
                                item(key = "tusim_popup_lazy_footer") {
                                    LazyLoadingFooterRow(
                                        visibleCount = pagedTusimItems.visibleItems.size,
                                        totalCount = pagedTusimItems.totalCount
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = requestDismiss) {
                            Text("TUTUP", color = Color(0xFF005FAF), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        var inputUrl by remember { mutableStateOf(customSpreadsheetUrl) }
        val isSaved = customSpreadsheetUrl.isNotBlank()
        
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Pengaturan Sumber Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    
                    // Unified Input Field Component
                    @Composable
                    fun UrlField(
                        key: String,
                        label: String,
                        value: String,
                        onValueChange: (String) -> Unit
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = value,
                                    onValueChange = onValueChange,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface),
                                    singleLine = true,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.White, shape = RoundedCornerShape(4.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            innerTextField()
                                        }
                                    }
                                )
                                IconButton(
                                    onClick = {
                                        val clipData = clipboardManager.getText()?.text ?: ""
                                        onValueChange(clipData)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { onValueChange("") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    Text("Links Intrakomp:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    UrlField("url_1", "LW321 Bulan lalu", url1) { viewModel.saveLink("url_1", it) }
                    UrlField("url_2", "LW321 Akhir Bulan", url2) { viewModel.saveLink("url_2", it) }
                    UrlField("url_3", "LW321 Daily", url3) { viewModel.saveLink("url_3", it) }

                    Text("Links Ekstracomp:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    UrlField("e_url_1", "LW325 Akhir Bulan", eUrl1) { viewModel.saveLink("e_url_1", it) }

                    Text("Link Database:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    UrlField("url_4", "Database LW321", url4) { viewModel.saveLink("url_4", it) }
                    UrlField("e_url_2", "Database LW325", eUrl2) { viewModel.saveLink("e_url_2", it) }
                    UrlField("k_url_1", "Database Kelolaan", kUrl1) { viewModel.saveLink("k_url_1", it) }
                    UrlField("di319_url", "Di319", di319Url) { viewModel.saveLink("di319_url", it) }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("link_dialog_close_button")
                ) {
                    Text("Tutup")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.refreshSpreadsheetData(force = true)
                        showSettingsDialog = false
                        android.widget.Toast.makeText(
                            context,
                            "Menyinkronkan data dari link...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("link_dialog_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refresh Data")
                }
            }
        )
    }

    if (waPhoneSelectorNumbers != null && waPhoneSelectorCustomer != null) {
        val selCustomer = waPhoneSelectorCustomer!!
        val selNumbers = waPhoneSelectorNumbers!!
        
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                waPhoneSelectorNumbers = null
                waPhoneSelectorCustomer = null
            },
            title = {
                Text(
                    text = "Kirim Pesan WhatsApp",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF005FAF)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Debitur memiliki lebih dari 1 nomor HP terdaftar. Silakan pilih nomor tujuan untuk mengirim pesan kepada ${selCustomer.namaNasabah}:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    selNumbers.forEachIndexed { index, phone ->
                        val dailyInfo = dailyDataMap[selCustomer.noRekening]
                        val overrideTunggakan = if (dailyInfo != null) {
                            if (dailyInfo.totalTunggakan > 0.0) {
                                dailyInfo.totalTunggakan
                            } else {
                                dailyInfo.tunggakanPokok + dailyInfo.tunggakanBunga + dailyInfo.pinalty
                            }
                        } else {
                            selCustomer.totalTunggakan
                        }
                        androidx.compose.material3.Card(
                            onClick = {
                                sendWhatsAppMessage(
                                    context = context,
                                    targetHp = phone,
                                    customer = selCustomer,
                                    formatter = formatter,
                                    loggedInDetails = loggedInDetails,
                                    overrideTotalTunggakan = overrideTunggakan,
                                    customTemplate = templateWaDetailDebitur
                                )
                                waPhoneSelectorNumbers = null
                                waPhoneSelectorCustomer = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Opsi Nomor ${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = phone,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    painter = painterResource(id = com.example.R.drawable.ic_whatsapp),
                                    contentDescription = "Pilih",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        waPhoneSelectorNumbers = null
                        waPhoneSelectorCustomer = null
                    }
                ) {
                    Text("Batal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showOfflineDialog) {
        var activeFileKey by remember { mutableStateOf("") }
        
        val fileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val stringData = inputStream.bufferedReader().use { it.readText() }
                        var name = "file.csv"
                        val cursor = context.contentResolver.query(uri, null, null, null, null)
                        cursor?.use { c ->
                            if (c.moveToFirst()) {
                                val nameCol = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameCol != -1) {
                                    name = c.getString(nameCol)
                                }
                            }
                        }
                        if (activeFileKey.isNotBlank()) {
                            viewModel.saveOfflineFile(activeFileKey, stringData, name)
                            Toast.makeText(context, "Berhasil memuat file: $name", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal memuat file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        
        AlertDialog(
            onDismissRequest = { showOfflineDialog = false },
            containerColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pengaturan Mode Offline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = isOfflineModeEnabled,
                        onCheckedChange = { viewModel.toggleOfflineMode(it) }
                    )
                }
            },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Status: " + (if (isOfflineModeEnabled) "AKTIF (Mengambil file lokal di HP)" else "NONAKTIF (Mengambil spreadsheet online)"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOfflineModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    @Composable
                    fun OfflineFileField(
                        key: String,
                        label: String,
                        currentFileName: String
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = if (currentFileName.isNotBlank()) currentFileName else "Belum ada file terpilih",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (currentFileName.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                
                                Button(
                                    onClick = {
                                        activeFileKey = key
                                        fileLauncher.launch("*/*")
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("Pilih", style = MaterialTheme.typography.labelSmall)
                                }
                                
                                if (currentFileName.isNotBlank()) {
                                    IconButton(
                                        onClick = { viewModel.deleteOfflineFile(key) },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text("Files Intrakomp:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    OfflineFileField("url_1", "LW321 Bulan lalu (url_1)", offlineNameUrl1)
                    OfflineFileField("url_2", "LW321 Akhir Bulan (url_2)", offlineNameUrl2)
                    OfflineFileField("url_3", "LW321 Daily (url_3)", offlineNameUrl3)

                    Text("Files Ekstracomp:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    OfflineFileField("e_url_1", "LW325 Akhir Bulan (e_url_1)", offlineNameEUrl1)

                    Text("File Database:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    OfflineFileField("url_4", "Database LW321 (url_4)", offlineNameUrl4)
                    OfflineFileField("e_url_2", "Database LW325 (e_url_2)", offlineNameEUrl2)
                    OfflineFileField("k_url_1", "Database Kelolaan (k_url_1)", offlineNameKUrl1)
                    OfflineFileField("di319_url", "Di319 (di319_url)", offlineNameDi319Url)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showOfflineDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Tutup")
                }
            }
        )
    }

    if (showUpdateDialog) {
        when (val state = appUpdateState) {
            is AppUpdateState.UpdateAvailable -> {
                val release = state.release
                val currentVer = remember(context) { AppUpdateManager.getCurrentVersionName(context) }
                AlertDialog(
                    onDismissRequest = { viewModel.dismissUpdateDialog() },
                    containerColor = Color.White,
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Peringatan Versi Baru",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "Peringatan Versi Baru",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00386B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_update_warning_title")
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFFBEB),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCD34D)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Tersedia versi aplikasi yang lebih baru (${release.tagName}).",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "Segera perbarui aplikasi untuk mendapatkan fitur terbaru, keamanan, dan perbaikan sistem.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF78350F)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0F9FF),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Versi Saat Ini",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = "v$currentVer",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF334155)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Versi Terbaru",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF0369A1)
                                        )
                                        Text(
                                            text = release.tagName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF005FAF)
                                        )
                                    }
                                }
                            }
                            if (release.apkSizeBytes > 0) {
                                Text(
                                    text = String.format("Ukuran APK: %.1f MB", release.apkSizeBytes / (1024f * 1024f)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF005FAF),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (release.apkDownloadUrl.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFF7ED),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Asset file .apk belum ditemukan pada rilis GitHub ${release.tagName}. Pastikan file APK sudah terlampir di GitHub Releases, lalu ketuk Periksa Ulang.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF9A3412),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            if (release.releaseNotes.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 150.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .verticalScroll(rememberScrollState())
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Catatan Pembaruan:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF334155)
                                        )
                                        Text(
                                            text = release.releaseNotes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        if (!release.apkDownloadUrl.isNullOrBlank()) {
                            Button(
                                onClick = { viewModel.downloadAndInstallUpdate(release) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("dialog_update_install_button")
                            ) {
                                Text("Update Sekarang", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.checkForUpdates(
                                        autoInstallOnFind = true,
                                        isAutoBackgroundCheck = false
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("dialog_update_retry_check_button")
                            ) {
                                Text("Periksa Ulang", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.dismissUpdateDialog() },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("dialog_update_later_button")
                        ) {
                            Text("Nanti")
                        }
                    }
                )
            }
            is AppUpdateState.Downloading -> {
                AlertDialog(
                    onDismissRequest = { /* Keep visible or allow minimizing cleanly */ viewModel.dismissUpdateDialog() },
                    containerColor = Color.White,
                    title = {
                        Text(
                            text = "Mengunduh Pembaruan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00386B)
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Mengunduh ${state.release.tagName} (${state.progressPercent}%)...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155)
                            )
                            LinearProgressIndicator(
                                progress = { (state.progressPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF005FAF),
                                trackColor = Color(0xFFDBEAFE)
                            )
                            val sizeLabel = if (state.totalMb > 0f) {
                                String.format("%.1f MB / %.1f MB", state.downloadedMb, state.totalMb)
                            } else {
                                String.format("%.1f MB", state.downloadedMb)
                            }
                            Text(
                                text = sizeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.dismissUpdateDialog() },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Sembunyikan")
                        }
                    }
                )
            }
            is AppUpdateState.Installing -> {
                AlertDialog(
                    onDismissRequest = { /* Keep visible briefly while PackageInstaller commits */ },
                    containerColor = Color.White,
                    title = {
                        Text(
                            text = "Menyiapkan Pemasangan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00386B)
                        )
                    },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color(0xFF005FAF),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Memproses paket instalasi ${state.release.tagName}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    },
                    confirmButton = {}
                )
            }
            is AppUpdateState.NeedsInstallPermission -> {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissUpdateDialog() },
                    containerColor = Color.White,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SecurityUpdateWarning,
                            contentDescription = "Izin Instalasi",
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Izin Instalasi Diperlukan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00386B)
                        )
                    },
                    text = {
                        Text(
                            text = "Aktifkan izin \"Instal aplikasi tidak dikenal\" untuk memasang pembaruan ${state.release.tagName}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                updateInstallPermissionLauncher.launch(
                                    AppUpdateManager.createInstallPermissionIntent(context)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("dialog_update_permission_button")
                        ) {
                            Text("Buka Pengaturan", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.dismissUpdateDialog() },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Batal")
                        }
                    }
                )
            }
            is AppUpdateState.ReadyToInstall -> {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissUpdateDialog() },
                    containerColor = Color.White,
                    title = {
                        Text(
                            text = "APK Siap Diinstal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00386B)
                        )
                    },
                    text = {
                        Text(
                            text = "File APK versi ${state.release.tagName} berhasil diunduh. Ketuk Instal Sekarang untuk membuka dialog instalasi aplikasi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (AppUpdateManager.canRequestPackageInstalls(context)) {
                                    viewModel.retryInstallAfterPermissionGranted()
                                } else {
                                    updateInstallPermissionLauncher.launch(
                                        AppUpdateManager.createInstallPermissionIntent(context)
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("dialog_update_ready_install_button")
                        ) {
                            Text("Instal Sekarang", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.dismissUpdateDialog() },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Tutup")
                        }
                    }
                )
            }
            is AppUpdateState.Error -> {
                AlertDialog(
                    onDismissRequest = {
                        viewModel.dismissUpdateDialog()
                        viewModel.clearUpdateStatus()
                    },
                    containerColor = Color.White,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SecurityUpdateWarning,
                            contentDescription = "Pembaruan Gagal",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Pembaruan Gagal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                    },
                    text = {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.checkForUpdates(
                                    autoInstallOnFind = true,
                                    isAutoBackgroundCheck = false
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("dialog_update_error_retry_button")
                        ) {
                            Text("Coba Lagi", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                viewModel.dismissUpdateDialog()
                                viewModel.clearUpdateStatus()
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Tutup")
                        }
                    }
                )
            }
            else -> Unit
        }
    }
    }
}

data class BottomNavItemData(
    val id: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun TabsHeader(
    selectedMenu: String,
    onMenuSelect: (String) -> Unit,
    isMonitoringOnly: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant) // background matches header area bg-[#F3EDF7]
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFE7E0EC)) // matching bg-[#E7E0EC]
                .padding(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                val tabs = if (isMonitoringOnly) {
                    listOf("INTRAKOMP", "EKSTRAKOMP", "SIMPANAN")
                } else {
                    listOf("INTRAKOMP", "EKSTRAKOMP", "SIMPANAN", "KELOLAAN", "KUNJUNGAN")
                }
                tabs.forEach { tab ->
                    val active = selectedMenu == tab
                    val bgCol = if (active) Color.White else Color.Transparent
                    val textCol = if (active) Color(0xFF1D1B20) else Color(0xFF49454F)
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(bgCol)
                            .clickable { onMenuSelect(tab) }
                            .padding(vertical = 8.dp)
                            .testTag("tab_$tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when(tab) {
                                "INTRAKOMP" -> "Intrakomp"
                                "EKSTRAKOMP" -> "Ekstrakomp"
                                "KELOLAAN" -> "Kelolaan"
                                "KUNJUNGAN" -> "Kunjungan"
                                else -> "Simpanan"
                            },
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = textCol,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KelolaanHeaderBanner(
    loggedInPN: String,
    loggedInMantriDetails: String,
    totalOs: Double = 0.0,
    totalDeb: Int = 0,
    formatter: NumberFormat = NumberFormat.getNumberInstance(Locale("id", "ID")),
    onLoginClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFEBF5FF))
                .border(1.dp, Color(0xFF005FAF).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF005FAF), modifier = Modifier.size(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "Kelolaan Nasabah",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        color = Color(0xFF00386B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (loggedInPN.isNotBlank() || totalDeb > 0) {
                        Text(
                            text = "OS ${formatDoubleClean(totalOs, formatter)} • $totalDeb Deb",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            color = Color(0xFF005FAF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // User requested: "tampilkan info PN aktif disebelah kanan sejajar dengan kata Kelolaan Nasabah tampilkan contoh : 207803 - Nurul Mubin"
            // User requested: "pada tampilan info PN login hilangkan background warna hijau dan tampilkan nama mantrinya juga bukan 00207803 - Mikro"
            if (loggedInPN.isNotBlank()) {
                val cleanPn = loggedInPN.filter { it.isDigit() }.trimStart('0').ifEmpty { loggedInPN.trim() }
                val displayInfo = if (loggedInMantriDetails.isNotBlank() && loggedInMantriDetails.contains(" - ") && !loggedInMantriDetails.contains("Mikro", ignoreCase = true)) {
                    val p = loggedInMantriDetails.split(" - ", limit = 2)
                    val pDigits = p[0].filter { it.isDigit() }.trimStart('0').ifEmpty { p[0].trim() }
                    "$pDigits - ${p[1].trim()}"
                } else if (cleanPn == "207803") {
                    "207803 - Nurul Mubin"
                } else if (loggedInMantriDetails.isNotBlank() && !loggedInMantriDetails.contains("Mikro", ignoreCase = true)) {
                    "$cleanPn - $loggedInMantriDetails"
                } else {
                    "$cleanPn - Nurul Mubin"
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clickable { onLoginClick() }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF005FAF),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = displayInfo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00386B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Button(
                    onClick = onLoginClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Login PN", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun RencanaHeaderBanner(
    activeVisitDate: String,
    onBackClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF7ED))
                .border(1.dp, Color(0xFFEA580C).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("rencana_back_to_beranda_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali ke Beranda",
                    tint = Color(0xFF7C2D12),
                    modifier = Modifier.size(20.dp)
                )
            }
            Icon(Icons.Default.EventNote, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Rencana Kunjungan & Aktivitas Harian", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF7C2D12))
                Text(
                    text = "Tanggal Kunjungan : $activeVisitDate",
                    fontSize = 10.sp,
                    color = Color(0xFF9A3412),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

fun formatCompactCurrency(value: Double): String {
    return when {
        value >= 1_000_000_000 -> {
            val num = value / 1_000_000_000
            String.format(Locale.US, "Rp%.1fM", num).replace(".0", "")
        }
        value >= 1_000_000 -> {
            val num = value / 1_000_000
            String.format(Locale.US, "Rp%.1fJt", num).replace(".0", "")
        }
        else -> {
            String.format(Locale.US, "Rp%,.0f", value)
        }
    }
}

/**
 * A truly compact, high-efficiency, and responsive table view.
 * Locks the 'Nama Nasabah' column to the left (frozen/sticky Column), and allows all other
 * columns to overflow and scroll horizontally in perfect synchronization using a shared ScrollState.
 */
@Composable
fun CustomerTable(
    customersList: List<CustomerEntity>,
    selectedMenu: String,
    sortCol: SortColumn,
    sortDir: SortDirection,
    onHeaderClick: (SortColumn) -> Unit,
    onCustomerClick: (CustomerEntity) -> Unit,
    formatter: NumberFormat,
    kunjunganKeys: Set<String> = emptySet(),
    onToggleKunjungan: (CustomerEntity) -> Unit = {},
    onShowTindakLanjutPopup: (CustomerEntity) -> Unit = {},
    tindakLanjutMap: Map<String, String> = emptyMap(),
    dueTodayCount: Int = 0,
    dueHCounts: Map<Int, Int> = emptyMap(),
    selectedJatuhTempoH: Int = 0,
    filterJatuhTempoOnly: Boolean = false,
    onSelectJatuhTempoH: (Int) -> Unit = {},
    onToggleJatuhTempoOnly: () -> Unit = {},
    loggedInPN: String = "",
    showJatuhTempoFilter: Boolean = true,
    onResetBayarFull: () -> Unit = {},
    dailyDataMap: Map<String, CustomerEntity> = emptyMap(),
    showTunggakanColumn: Boolean = (selectedMenu == "KELOLAAN" || selectedMenu == "KUNJUNGAN")
) {
    val context = LocalContext.current
    val tableScrollState = rememberScrollState()

    var filterBayarFullOnly by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMenu) {
        if (selectedMenu != "KELOLAAN") {
            filterBayarFullOnly = false
        }
    }

    val bayarFullCustomers = remember(customersList, tindakLanjutMap) {
        customersList.filter { customer ->
            val key = if (customer.noRekening.isNotBlank()) customer.noRekening else if (customer.cif.isNotBlank()) customer.cif else customer.namaNasabah
            val tl = tindakLanjutMap[key] ?: tindakLanjutMap["kunjungan_$key"] ?: ""
            tl.equals("Bayar", ignoreCase = true) || tl.equals("Bayar Full", ignoreCase = true) || tl.equals("Bayar Lunas", ignoreCase = true)
        }
    }

    val displayCustomers = remember(customersList, filterBayarFullOnly, bayarFullCustomers, selectedMenu) {
        if (selectedMenu == "KELOLAAN" && filterBayarFullOnly) {
            bayarFullCustomers
        } else {
            customersList
        }
    }

    val extraHeaders = remember(customersList) {
        val firstWithExtra = customersList.firstOrNull { it.extraColumns.isNotBlank() }
        if (firstWithExtra != null) {
            firstWithExtra.extraColumns.split("||").mapNotNull {
                val parts = it.split("::")
                if (parts.isNotEmpty()) parts[0].trim() else null
            }.filter { headerName ->
                if (headerName.isBlank()) return@filter false
                val lowercaseName = headerName.trim().lowercase()
                val shouldExclude = lowercaseName == "at" ||
                        lowercaseName == "jw" ||
                        lowercaseName.contains("produk") ||
                        lowercaseName.contains("product") ||
                        lowercaseName.contains("tgl ph") ||
                        lowercaseName.contains("tanggal ph") ||
                        lowercaseName.contains("tgl_ph") ||
                        lowercaseName.contains("tgl realisasi") ||
                        lowercaseName.contains("tgl_realisasi") ||
                        lowercaseName.contains("tanggal realisasi") ||
                        lowercaseName.contains("tgl real") ||
                        lowercaseName.contains("currency") ||
                        lowercaseName.contains("valuta") ||
                        lowercaseName.contains("mata uang") ||
                        lowercaseName.contains("sisa awal pokok") ||
                        lowercaseName.contains("sisa_awal_pokok") ||
                        lowercaseName.contains("awal pokok") ||
                        lowercaseName.contains("jangka waktu") ||
                        lowercaseName.contains("tenor") ||
                        lowercaseName.contains("alih tagih") ||
                        lowercaseName.contains("alih_tagih") ||
                        lowercaseName.contains("besar realisasi") ||
                        lowercaseName.contains("besar_realisasi") ||
                        lowercaseName.contains("sisa akhir ph pokok") ||
                        lowercaseName.contains("sisa_akhir_ph_pokok") ||
                        lowercaseName.contains("sisa akhir ph") ||
                        lowercaseName.contains("sisa awal ph bunga") ||
                        lowercaseName.contains("sisa_awal_ph_bunga") ||
                        lowercaseName.contains("sisa awal ph")
                !shouldExclude
            }
        } else {
            emptyList()
        }
    }

    val firstColWidth = if (selectedMenu == "KELOLAAN" || selectedMenu == "KUNJUNGAN") 130.dp else 110.dp

    val (totalData, sumOs, totalSetorPokok, countSetorPokok) = remember(customersList, selectedMenu) {
        val total = customersList.size
        val os = if (selectedMenu == "EKSTRAKOMP") {
            customersList.sumOf { it.osPinjaman + it.pinalty }
        } else {
            customersList.sumOf { it.osPinjaman }
        }
        val sp = customersList.sumOf { it.setorPokok }
        val cnt = customersList.count { it.setorPokok > 0.0 }
        data class FooterCalc(val total: Int, val os: Double, val sp: Double, val cnt: Int)
        FooterCalc(total, os, sp, cnt)
    }

    val (lcrOs, lcrCount, smlOs, smlCount, nplOs, nplCount) = remember(customersList) {
        var lOs = 0.0
        var lCnt = 0
        var sOs = 0.0
        var sCnt = 0
        var nOs = 0.0
        var nCnt = 0
        for (c in customersList) {
            val k = c.kolek.trim().uppercase()
            val isNpl = k == "KL" || k == "KURANG LANCAR" || k.contains("KURANG") || k == "3" ||
                    k == "DIRAGUKAN" || k == "D" || k.contains("DIRAGUKAN") || k == "4" ||
                    k == "MACET" || k == "M" || k.contains("MACET") || k == "5" || k.contains("NPL")
            val isSml = !isNpl && (k.startsWith("SML") || k.contains("DPK") || k == "2" || k.startsWith("2."))
            if (isNpl) {
                nOs += c.osPinjaman
                nCnt++
            } else if (isSml) {
                sOs += c.osPinjaman
                sCnt++
            } else {
                lOs += c.osPinjaman
                lCnt++
            }
        }
        data class KolekStatsData(val lcrOs: Double, val lcrCount: Int, val smlOs: Double, val smlCount: Int, val nplOs: Double, val nplCount: Int)
        KolekStatsData(lOs, lCnt, sOs, sCnt, nOs, nCnt)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, bottom = if (selectedMenu == "KELOLAAN") 6.dp else 16.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // lovely soft blue surface
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCCB0)), // orange outline nuance
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
            
            // Fixed header with synchronized ScrollState
            TableHeader(
                selectedMenu = selectedMenu,
                sortCol = sortCol,
                sortDir = sortDir,
                scrollState = tableScrollState,
                firstColWidth = firstColWidth,
                extraHeaders = extraHeaders,
                onHeaderClick = onHeaderClick,
                loggedInPN = loggedInPN,
                showTunggakanColumn = showTunggakanColumn
            )

            HorizontalDivider(color = Color(0xFFFFCCB0))

            // Scrollable Rows
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (displayCustomers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedMenu == "KELOLAAN" && filterBayarFullOnly) "Tidak ada nasabah dengan TL Bayar" else "Tidak ada data nasabah",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = displayCustomers,
                        key = { idx, customer ->
                            if (customer.id != 0) customer.id else "${customer.noRekening}_${customer.cif}_$idx"
                        }
                    ) { idx, customer ->
                        TableRow(
                            idx = idx,
                            selectedMenu = selectedMenu,
                            customer = customer,
                            scrollState = tableScrollState,
                            firstColWidth = firstColWidth,
                            extraHeaders = extraHeaders,
                            onRowClick = { onCustomerClick(customer) },
                            formatter = formatter,
                            kunjunganKeys = kunjunganKeys,
                            onToggleKunjungan = onToggleKunjungan,
                            onShowTindakLanjutPopup = onShowTindakLanjutPopup,
                            tindakLanjutMap = tindakLanjutMap,
                            loggedInPN = loggedInPN,
                            dailyDataMap = dailyDataMap,
                            showTunggakanColumn = showTunggakanColumn
                        )
                        HorizontalDivider(color = Color(0xFFFFCCB0))
                    }
                }
            }
            
            // Premium Bento table footer indicator matching HTML spec
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF7F2)) // beautiful warm soft orange footer accent
                    .padding(vertical = 6.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left portion: Checked info with Deb count and total OS (e.g. 7 | 398.333.000)
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    val (checkedCount, checkedOs) = remember(customersList, kunjunganKeys, selectedMenu) {
                        val checked = customersList.filter { customer ->
                            val key = if (customer.noRekening.isNotBlank()) customer.noRekening else if (customer.cif.isNotBlank()) customer.cif else customer.namaNasabah
                            kunjunganKeys.contains(key)
                        }
                        val c = checked.size
                        val os = if (selectedMenu == "EKSTRAKOMP") {
                            checked.sumOf { it.osPinjaman + it.pinalty }
                        } else {
                            checked.sumOf { it.osPinjaman }
                        }
                        c to os
                    }
                    if (selectedMenu == "KELOLAAN" || selectedMenu == "KUNJUNGAN" || checkedCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selesai dikunjungi",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$checkedCount | ${formatDoubleClean(checkedOs, formatter)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }
                
                // Right portion
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (selectedMenu != "KELOLAAN") {
                        Text(
                            text = "OS ${formatDoubleClean(sumOs, formatter)} Deb $totalData",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF43474E)
                        )
                    }

                    if (selectedMenu == "KELOLAAN") {
                        val bayarFullOs = bayarFullCustomers.sumOf { it.osPinjaman }
                        val bayarFullCount = bayarFullCustomers.size
                        var showResetBayarFullDialog by remember { mutableStateOf(false) }

                        // Info Bayar OS dan Deb sekaligus tombol untuk menampilkan nasabah TL Bayar
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (filterBayarFullOnly) Color(0xFF1B5E20) else Color(0xFFE8F5E9),
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                if (filterBayarFullOnly) Color(0xFF1B5E20) else Color(0xFF81C784)
                            ),
                            modifier = Modifier.clickable {
                                filterBayarFullOnly = !filterBayarFullOnly
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (filterBayarFullOnly) Icons.Default.Check else Icons.Default.FilterAlt,
                                    contentDescription = "Tampilkan nasabah TL Bayar",
                                    tint = if (filterBayarFullOnly) Color.White else Color(0xFF1B5E20),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "Bayar: OS ${formatDoubleClean(bayarFullOs, formatter)} Deb $bayarFullCount",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (filterBayarFullOnly) Color.White else Color(0xFF1B5E20)
                                )
                            }
                        }

                        // Icon Reset untuk mereset TL Bayar nasabah
                        IconButton(
                            onClick = {
                                if (bayarFullCount > 0) {
                                    showResetBayarFullDialog = true
                                } else {
                                    android.widget.Toast.makeText(context, "Tidak ada nasabah dengan TL Bayar", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset TL Bayar",
                                tint = if (bayarFullCount > 0) Color(0xFFC62828) else Color.Gray,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        if (showResetBayarFullDialog) {
                            AlertDialog(
                                onDismissRequest = { showResetBayarFullDialog = false },
                                title = {
                                    Text(
                                        text = "Reset TL Bayar",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                },
                                text = {
                                    Text(
                                        text = "Apakah Anda yakin ingin mereset/menghapus tindak lanjut 'Bayar' untuk $bayarFullCount nasabah?",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            onResetBayarFull()
                                            filterBayarFullOnly = false
                                            showResetBayarFullDialog = false
                                            android.widget.Toast.makeText(context, "TL Bayar berhasil di-reset", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                                    ) {
                                        Text("Reset", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showResetBayarFullDialog = false }) {
                                        Text("Batal", fontSize = 11.sp)
                                    }
                                }
                            )
                        }
                    }

                    if (selectedMenu == "EKSTRAKOMP") {
                        // User request: dipojok kanan bawah tabel ekstrakomp hilangkan tombol Jatuh tempo ganti menjadi info total setoran pokok misal 23.900.000 I 34
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(12.dp)
                                .background(Color.Gray.copy(alpha = 0.5f))
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${formatDoubleClean(totalSetorPokok, formatter)} | $countSetorPokok",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    } else if (showJatuhTempoFilter) {
                        // Separator vertical line
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(12.dp)
                                .background(Color.Gray.copy(alpha = 0.5f))
                        )
                        
                        // Jatuh tempo / H-0 ... H-7 clickable badge with droplist
                        var showJatuhTempoDropdown by remember { mutableStateOf(false) }
                        val activeHLabel = "H-$selectedJatuhTempoH"
                        val activeHCount = dueHCounts[selectedJatuhTempoH] ?: (if (selectedJatuhTempoH == 0) dueTodayCount else 0)
                        
                        val badgeBgColor = if (filterJatuhTempoOnly) Color(0xFFD32F2F) else Color(0xFFE0E0E0)
                        val badgeTextColor = if (filterJatuhTempoOnly) Color.White else Color(0xFF424242)
                        
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(badgeBgColor)
                                    .clickable {
                                        showJatuhTempoDropdown = true
                                    }
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "$activeHLabel: $activeHCount Deb",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = badgeTextColor
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih H-0 s/d H-7",
                                    tint = badgeTextColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                if (filterJatuhTempoOnly) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                onToggleJatuhTempoOnly()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hapus filter",
                                            tint = badgeTextColor,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = showJatuhTempoDropdown,
                                onDismissRequest = { showJatuhTempoDropdown = false }
                            ) {
                                if (filterJatuhTempoOnly) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Tampilkan Semua (Hapus Filter)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD32F2F)
                                            )
                                        },
                                        onClick = {
                                            showJatuhTempoDropdown = false
                                            onToggleJatuhTempoOnly()
                                        }
                                    )
                                    HorizontalDivider()
                                }
                                for (h in 0..7) {
                                    val count = dueHCounts[h] ?: (if (h == 0) dueTodayCount else 0)
                                    val label = if (h == 0) "H-0 (Hari ini)" else "H-$h"
                                    val isSelected = filterJatuhTempoOnly && selectedJatuhTempoH == h
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color(0xFFD32F2F) else Color(0xFF212121)
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    text = "$count Deb",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color(0xFFD32F2F) else Color.Gray
                                                )
                                            }
                                        },
                                        onClick = {
                                            showJatuhTempoDropdown = false
                                            onSelectJatuhTempoH(h)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        if (selectedMenu == "KELOLAAN") {
            Spacer(modifier = Modifier.height(4.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                val baseFontSize = when {
                    maxWidth >= 440.dp -> 10.5.sp
                    maxWidth >= 390.dp -> 9.6.sp
                    maxWidth >= 360.dp -> 8.8.sp
                    else -> 8.0.sp
                }
                val debFontSize = when {
                    maxWidth >= 440.dp -> 9.5.sp
                    maxWidth >= 390.dp -> 8.6.sp
                    maxWidth >= 360.dp -> 7.9.sp
                    else -> 7.2.sp
                }

                val statsItems = listOf(
                    Triple("LCR", lcrOs to lcrCount, Color(0xFF2E7D32)),
                    Triple("SML", smlOs to smlCount, Color(0xFFE65100)),
                    Triple("NPL", nplOs to nplCount, Color(0xFFC62828))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    statsItems.forEach { (tag, data, color) ->
                        val (osVal, debCount) = data
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = androidx.compose.ui.text.buildAnnotatedString {
                                    withStyle(
                                        androidx.compose.ui.text.SpanStyle(
                                            fontSize = baseFontSize,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = color,
                                            letterSpacing = (-0.2).sp
                                        )
                                    ) {
                                        append("$tag ${formatDoubleClean(osVal, formatter)} ")
                                    }
                                    withStyle(
                                        androidx.compose.ui.text.SpanStyle(
                                            fontSize = debFontSize,
                                            fontWeight = FontWeight.Bold,
                                            color = color.copy(alpha = 0.85f),
                                            letterSpacing = (-0.2).sp
                                        )
                                    ) {
                                        append("Deb $debCount")
                                    }
                                },
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberTableShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "table_skeleton_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    val shimmerColors = listOf(
        Color(0xFFE6ECF4),
        Color(0xFFF6F9FD),
        Color(0xFFE6ECF4)
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 320f, translateAnim - 160f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
fun CustomerTableSkeleton(
    selectedMenu: String,
    loggedInPN: String = "",
    rowCount: Int = 12
) {
    val shimmerBrush = rememberTableShimmerBrush()
    val headerBgColor = if (selectedMenu == "EKSTRAKOMP") Color(0xFFD32F2F) else Color(0xFF005FAF)
    val firstColWidth = if (selectedMenu == "KELOLAAN" || selectedMenu == "KUNJUNGAN") 130.dp else 110.dp
    val skeletonScrollState = rememberScrollState()

    val columns: List<Pair<String, Dp>> = when (selectedMenu) {
        "EKSTRAKOMP" -> listOf(
            "Realisasi" to 62.dp,
            "Sisa Pokok" to 62.dp,
            "Alih Tagih" to 56.dp,
            "Sisa Bunga" to 66.dp,
            "Total DH" to 62.dp,
            "Segmen" to 62.dp
        )
        "KELOLAAN" -> buildList {
            add("Plafond" to 58.dp)
            add("OS Pinjaman" to 62.dp)
            add("Tunggakan" to 62.dp)
            add("Segmen" to 55.dp)
            add("Kolek" to 46.dp)
            add("NPD" to 45.dp)
            if (loggedInPN.trim() == "207803") {
                add("Desa" to 80.dp)
            }
            add("Alamat" to 150.dp)
        }
        "SIMPANAN" -> listOf(
            "CIF" to 72.dp,
            "Produk" to 70.dp,
            "Saldo" to 78.dp,
            "Status" to 58.dp
        )
        else -> listOf(
            "Plafond" to 58.dp,
            "OS Pinjaman" to 62.dp,
            "Segmen" to 55.dp,
            "Kolek" to 46.dp,
            "NPD" to 45.dp,
            "Nama Mantri" to 110.dp
        )
    }

    val nameWidthFractions = remember { listOf(0.84f, 0.68f, 0.92f, 0.74f, 0.62f, 0.88f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, bottom = if (selectedMenu == "KELOLAAN") 6.dp else 16.dp)
            .testTag("customer_table_skeleton")
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCCB0)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header mirroring TableHeader
                Row(
                    modifier = Modifier
                        .background(headerBgColor)
                        .padding(vertical = 2.dp, horizontal = 4.dp)
                        .height(26.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(firstColWidth)
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if (selectedMenu == "SIMPANAN") "NAMA NASABAH" else "NAMA NASABAH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.3f))
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(skeletonScrollState),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        columns.forEach { (colTitle, colWidth) ->
                            Box(
                                modifier = Modifier
                                    .width(colWidth)
                                    .fillMaxHeight()
                                    .padding(horizontal = 3.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = colTitle.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = if (colWidth < 65.dp) 7.5.sp else 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = if (colWidth < 65.dp) 0.sp else 0.5.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFFFCCB0))

                // Shimmer skeleton rows
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    userScrollEnabled = false
                ) {
                    items(rowCount) { idx ->
                        val rowBg = if (idx % 2 == 0) Color.White else Color(0xFFFBFDFF)
                        val nameFrac = nameWidthFractions[idx % nameWidthFractions.size]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg)
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Locked Name skeleton column
                            Box(
                                modifier = Modifier
                                    .width(firstColWidth)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(nameFrac)
                                        .height(11.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(shimmerBrush)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(18.dp)
                                    .background(Color(0xFFFFCCB0).copy(alpha = 0.45f))
                            )

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(skeletonScrollState),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                columns.forEachIndexed { cIdx, (colTitle, colWidth) ->
                                    val cellFrac = when {
                                        colTitle == "Kolek" -> 0.72f
                                        colTitle == "NPD" -> 0.76f
                                        (idx + cIdx) % 3 == 0 -> 0.82f
                                        (idx + cIdx) % 3 == 1 -> 0.66f
                                        else -> 0.74f
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(colWidth)
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = if (colTitle == "Kolek" || colTitle == "NPD") {
                                            Alignment.Center
                                        } else if (colTitle == "Alamat" || colTitle == "Desa" || colTitle == "Nama Mantri") {
                                            Alignment.CenterStart
                                        } else {
                                            Alignment.CenterEnd
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(cellFrac)
                                                .height(if (colTitle == "Kolek") 14.dp else 10.dp)
                                                .clip(RoundedCornerShape(if (colTitle == "Kolek") 6.dp else 4.dp))
                                                .background(shimmerBrush)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFFFCCB0).copy(alpha = 0.55f))
                    }
                }

                // Footer matching CustomerTable footer with live syncing label
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF7F2))
                        .padding(vertical = 7.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(11.dp),
                            strokeWidth = 1.6.dp,
                            color = if (selectedMenu == "EKSTRAKOMP") Color(0xFFD32F2F) else Color(0xFF005FAF)
                        )
                        Text(
                            text = "Menyinkronkan data Google Sheets...",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (selectedMenu == "EKSTRAKOMP") Color(0xFFD32F2F) else Color(0xFF005FAF)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }

        if (selectedMenu == "KELOLAAN") {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}

@Composable
fun TableHeader(
    selectedMenu: String,
    sortCol: SortColumn,
    sortDir: SortDirection,
    scrollState: ScrollState,
    firstColWidth: Dp,
    extraHeaders: List<String> = emptyList(),
    onHeaderClick: (SortColumn) -> Unit,
    loggedInPN: String = "",
    showTunggakanColumn: Boolean = (selectedMenu == "KELOLAAN" || selectedMenu == "KUNJUNGAN")
) {
    val headerBgColor = if (selectedMenu == "EKSTRAKOMP") Color(0xFFD32F2F) else Color(0xFF005FAF)
    Row(
        modifier = Modifier
            .background(headerBgColor) // Dynamic corporate background color
            .padding(vertical = 2.dp, horizontal = 4.dp)
            .height(26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nama Nasabah is fully LOCKED (does not scroll)
        HeaderCell(
            title = "Nama Nasabah",
            width = firstColWidth,
            align = Alignment.CenterStart,
            sorted = sortCol == SortColumn.NAMA_NASABAH,
            sortDir = sortDir,
            onClick = { onHeaderClick(SortColumn.NAMA_NASABAH) }
        )

        // Custom fine white vertical divider showing the locked split
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.3f))
        )

        // Other headers scroll horizontally in synchronization with the rows
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedMenu != "EKSTRAKOMP") {
                HeaderCell(
                    title = "Plafond",
                    width = 58.dp,
                    align = Alignment.CenterEnd,
                    sorted = sortCol == SortColumn.PLAFOND,
                    sortDir = sortDir,
                    onClick = { onHeaderClick(SortColumn.PLAFOND) }
                )
            }
            
            if (selectedMenu == "EKSTRAKOMP") {
                HeaderCell(
                    title = "Realisasi",
                    width = 62.dp,
                    align = Alignment.CenterEnd,
                    sorted = sortCol == SortColumn.PLAFOND,
                    sortDir = sortDir,
                    onClick = { onHeaderClick(SortColumn.PLAFOND) }
                )
                HeaderCell(
                    title = "Sisa Pokok",
                    width = 62.dp,
                    align = Alignment.CenterEnd,
                    sorted = sortCol == SortColumn.OS_PINJAMAN,
                    sortDir = sortDir,
                    onClick = { onHeaderClick(SortColumn.OS_PINJAMAN) }
                )
                HeaderCell(
                    title = "Alih Tagih",
                    width = 56.dp,
                    align = Alignment.CenterEnd,
                    sorted = sortCol == SortColumn.ALIH_TAGIH,
                    sortDir = sortDir,
                    onClick = { onHeaderClick(SortColumn.ALIH_TAGIH) }
                )
                HeaderCell(
                    title = "Sisa Bunga",
                    width = 66.dp,
                    align = Alignment.CenterEnd,
                    sorted = sortCol == SortColumn.SISA_BUNGA,
                    sortDir = sortDir,
                    onClick = { onHeaderClick(SortColumn.SISA_BUNGA) }
                )
                HeaderCell(
                    title = "Total DH",
                    width = 62.dp,
                    align = Alignment.CenterEnd,
                    sorted = sortCol == SortColumn.TOTAL_DH,
                    sortDir = sortDir,
                    onClick = { onHeaderClick(SortColumn.TOTAL_DH) }
                )
                HeaderCell(
                    title = "Segmen",
                    width = 62.dp,
                    align = Alignment.CenterEnd,
                    sorted = sortCol == SortColumn.SEGMEN,
                    sortDir = sortDir,
                    onClick = { onHeaderClick(SortColumn.SEGMEN) }
                )
                HeaderCell(
                    title = "Update Pokok",
                    width = 75.dp,
                    align = Alignment.CenterEnd,
                    sorted = sortCol == SortColumn.UPDATE_POKOK,
                    sortDir = sortDir,
                    onClick = { onHeaderClick(SortColumn.UPDATE_POKOK) }
                )
                HeaderCell(
                    title = "Setor Pokok",
                    width = 75.dp,
                    align = Alignment.CenterEnd,
                    sorted = sortCol == SortColumn.SETOR_POKOK,
                    sortDir = sortDir,
                    onClick = { onHeaderClick(SortColumn.SETOR_POKOK) }
                )
            } else {
                HeaderCell(
                    title = "OS Pinjaman",
                    width = 62.dp,
                    align = Alignment.CenterEnd,
                    sorted = sortCol == SortColumn.OS_PINJAMAN,
                    sortDir = sortDir,
                    onClick = { onHeaderClick(SortColumn.OS_PINJAMAN) }
                )
                if (showTunggakanColumn) {
                    HeaderCell(
                        title = "Tunggakan",
                        width = 62.dp,
                        align = Alignment.CenterEnd,
                        sorted = sortCol == SortColumn.TOTAL_DH,
                        sortDir = sortDir,
                        onClick = { onHeaderClick(SortColumn.TOTAL_DH) }
                    )
                }
                HeaderCell(
                    title = "Segmen",
                    width = 55.dp,
                    align = Alignment.CenterEnd,
                    sorted = sortCol == SortColumn.SEGMEN,
                    sortDir = sortDir,
                    onClick = { onHeaderClick(SortColumn.SEGMEN) }
                )
                if (selectedMenu == "KELOLAAN" || selectedMenu == "KUNJUNGAN") {
                    HeaderCell(
                        title = "Kolek",
                        width = 46.dp,
                        align = Alignment.Center,
                        sorted = sortCol == SortColumn.KOLEK,
                        sortDir = sortDir,
                        onClick = { onHeaderClick(SortColumn.KOLEK) }
                    )
                    HeaderCell(
                        title = "NPD",
                        width = 45.dp,
                        align = Alignment.Center,
                        sorted = sortCol == SortColumn.NPD,
                        sortDir = sortDir,
                        onClick = { onHeaderClick(SortColumn.NPD) }
                    )
                    if (selectedMenu == "KELOLAAN" && loggedInPN.trim() == "207803") {
                        HeaderCell(
                            title = "Desa",
                            width = 80.dp,
                            align = Alignment.CenterStart,
                            sorted = sortCol == SortColumn.DESA,
                            sortDir = sortDir,
                            onClick = { onHeaderClick(SortColumn.DESA) }
                        )
                    }
                    HeaderCell(
                        title = "Alamat",
                        width = 150.dp,
                        align = Alignment.CenterStart,
                        sorted = sortCol == SortColumn.ALAMAT,
                        sortDir = sortDir,
                        onClick = { onHeaderClick(SortColumn.ALAMAT) }
                    )
                }
                if (selectedMenu != "KELOLAAN" && selectedMenu != "KUNJUNGAN") {
                    HeaderCell(
                        title = "Kolek",
                        width = 46.dp,
                        align = Alignment.Center,
                        sorted = sortCol == SortColumn.KOLEK,
                        sortDir = sortDir,
                        onClick = { onHeaderClick(SortColumn.KOLEK) }
                    )
                    if (selectedMenu == "INTRAKOMP") {
                        HeaderCell(
                            title = "NPD",
                            width = 45.dp,
                            align = Alignment.Center,
                            sorted = sortCol == SortColumn.NPD,
                            sortDir = sortDir,
                            onClick = { onHeaderClick(SortColumn.NPD) }
                        )
                    }
                    HeaderCell(
                        title = "Nama Mantri",
                        width = 110.dp,
                        align = Alignment.CenterStart,
                        sorted = sortCol == SortColumn.NAMA_MANTRI,
                        sortDir = sortDir,
                        onClick = { onHeaderClick(SortColumn.NAMA_MANTRI) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(32.dp)) // padding matching action button
        }
    }
}

@Composable
fun HeaderCell(
    title: String,
    width: Dp = 100.dp,
    align: Alignment = Alignment.CenterStart,
    sorted: Boolean,
    sortDir: SortDirection,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .clickable { onClick() }
            .padding(horizontal = 2.dp),
        contentAlignment = align
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (width < 65.dp) 7.5.sp else 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = if (width < 65.dp) 0.sp else 0.5.sp
                ),
                color = if (sorted) Color.White else Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (sorted) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = if (sortDir == SortDirection.ASC) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (sortDir == SortDirection.ASC) "Sorted A-Z / Naik" else "Sorted Z-A / Turun",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun TableRow(
    idx: Int,
    selectedMenu: String,
    customer: CustomerEntity,
    scrollState: ScrollState,
    firstColWidth: Dp,
    extraHeaders: List<String> = emptyList(),
    onRowClick: () -> Unit,
    formatter: NumberFormat,
    kunjunganKeys: Set<String> = emptySet(),
    onToggleKunjungan: (CustomerEntity) -> Unit = {},
    onShowTindakLanjutPopup: (CustomerEntity) -> Unit = {},
    tindakLanjutMap: Map<String, String> = emptyMap(),
    loggedInPN: String = "",
    dailyDataMap: Map<String, CustomerEntity> = emptyMap(),
    showTunggakanColumn: Boolean = (selectedMenu == "KELOLAAN" || selectedMenu == "KUNJUNGAN")
) {
    val uniqueKey = if (customer.noRekening.isNotBlank()) customer.noRekening else if (customer.cif.isNotBlank()) customer.cif else customer.namaNasabah
    // Beautiful distinct blue-themed zebra striping for rows
    val backgroundCol = if (idx % 2 == 0) Color(0xFFEEF4FC) else Color(0xFFD5E3F5)
    
    Row(
        modifier = Modifier
            .background(backgroundCol)
            .clickable { onRowClick() }
            .padding(vertical = 1.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nama Nasabah is fully LOCKED (does not scroll)
        Row(
            modifier = Modifier.width(firstColWidth),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedMenu == "KELOLAAN" || selectedMenu == "KUNJUNGAN") {
                val isChecked = kunjunganKeys.contains(uniqueKey)
                
                if (selectedMenu == "KUNJUNGAN") {
                    val hasTindakLanjut = !tindakLanjutMap["kunjungan_$uniqueKey"].isNullOrBlank()
                    Box(
                        modifier = Modifier
                            .padding(start = 2.dp, end = 6.dp)
                            .size(15.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (hasTindakLanjut) Color(0xFF4CAF50) else Color(0xFF005FAF))
                            .clickable { onShowTindakLanjutPopup(customer) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tindak Lanjut",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .padding(start = 2.dp, end = 6.dp)
                            .size(15.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isChecked) Color(0xFF005FAF) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isChecked) Color(0xFF005FAF) else Color.Gray.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(3.dp)
                            )
                            .clickable { onToggleKunjungan(customer) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Terpilih",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
            val rowGeneralTLVal = tindakLanjutMap[uniqueKey] ?: "-"
            val rowKunjunganTLVal = tindakLanjutMap["kunjungan_$uniqueKey"] ?: "-"
            val isRowBergeserNpl = rowGeneralTLVal == "Bergeser NPL" || rowKunjunganTLVal == "Bergeser NPL"

            val isEkstrakomp = selectedMenu == "EKSTRAKOMP" || customer.menuType == "EKSTRAKOMP"
            val isDebiturLunasOrHilang = remember(customer, dailyDataMap, isEkstrakomp) {
                if (isEkstrakomp) {
                    false
                } else if (customer.osPinjaman <= 0.0) {
                    true
                } else if (dailyDataMap.isNotEmpty()) {
                    val rawRek = customer.noRekening.trim()
                    val cleanRek = rawRek.filter { it.isDigit() }
                    val cleanCif = customer.cif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }

                    val dInfo = dailyDataMap[rawRek]
                        ?: (if (cleanRek.isNotBlank()) dailyDataMap[cleanRek] else null)
                        ?: (if (cleanRek.length >= 15) dailyDataMap[cleanRek.takeLast(15)] else null)
                        ?: (if (cleanRek.isNotBlank()) dailyDataMap[cleanRek.trimStart('0')] else null)
                        ?: (if (customer.cif.isNotBlank()) dailyDataMap[customer.cif] else null)
                        ?: (if (cleanCif.isNotBlank()) dailyDataMap[cleanCif] ?: dailyDataMap["CIF-$cleanCif"] else null)
                        ?: (if (cleanRek.isNotBlank()) dailyDataMap.entries.firstOrNull { it.key.filter { c -> c.isDigit() } == cleanRek }?.value else null)

                    dInfo == null || dInfo.osPinjaman <= 0.0
                } else {
                    false
                }
            }

            val nameColor = when {
                isDebiturLunasOrHilang -> Color(0xFF757575)
                isRowBergeserNpl -> Color(0xFFD32F2F)
                else -> Color(0xFF001C3B)
            }
            val nameDecoration = if (isDebiturLunasOrHilang) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None

            Text(
                text = customer.namaNasabah.uppercase(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 9.sp,
                    textDecoration = nameDecoration
                ),
                fontWeight = if (isDebiturLunasOrHilang) FontWeight.Normal else FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                color = nameColor
            )
        }

        // Custom fine orange vertical divider showing the locked split
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(18.dp)
                .background(Color(0xFFFFCCB0))
        )

        // Other cells scroll horizontally in synchronization with the headers
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Plafond
            if (selectedMenu != "EKSTRAKOMP") {
                Text(
                    text = formatDoubleClean(customer.plafond, formatter),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(58.dp).padding(horizontal = 2.dp),
                    color = Color(0xFF001C3B)
                )
            }

            if (selectedMenu == "EKSTRAKOMP") {
                // Realisasi (plafond)
                Text(
                    text = formatDoubleClean(customer.plafond, formatter),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(62.dp).padding(horizontal = 2.dp),
                    color = Color(0xFF001C3B)
                )
                // Sisa Pokok (osPinjaman)
                Text(
                    text = formatDoubleClean(customer.osPinjaman, formatter),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(62.dp).padding(horizontal = 2.dp),
                    color = Color(0xFF001C3B)
                )
                // Alih Tagih (pinalty)
                Text(
                    text = formatDoubleClean(customer.pinalty, formatter),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(56.dp).padding(horizontal = 2.dp),
                    color = Color(0xFF001C3B)
                )
                // Sisa Bunga (tunggakanBunga)
                Text(
                    text = if (customer.menuType == "EKSTRAKOMP") formatDoubleWith2Decimals(customer.tunggakanBunga) else formatDoubleClean(customer.tunggakanBunga, formatter),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(66.dp).padding(horizontal = 2.dp),
                    color = Color(0xFF001C3B)
                )
                // Total DH (totalTunggakan)
                Text(
                    text = formatDoubleClean(customer.totalTunggakan, formatter),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(62.dp).padding(horizontal = 2.dp),
                    color = Color(0xFF001C3B)
                )
                // Segmen
                Text(
                    text = if (customer.menuType == "EKSTRAKOMP") mapEkstrakompSegmen(customer.segmen) else customer.segmen,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(62.dp).padding(horizontal = 2.dp),
                    color = Color(0xFF001C3B)
                )
                // Update Pokok (Warna Biru)
                Text(
                    text = formatDoubleClean(customer.updatePokok, formatter),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(75.dp).padding(horizontal = 4.dp),
                    color = Color(0xFF005FAF)
                )
                // Setor Pokok
                Text(
                    text = formatDoubleClean(customer.setorPokok, formatter),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(75.dp).padding(horizontal = 4.dp),
                    color = if (customer.setorPokok > 0) Color(0xFF1B5E20) else Color(0xFF001C3B)
                )
            } else {
                // OS Pinjaman
                Text(
                    text = formatDoubleClean(customer.osPinjaman, formatter),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(62.dp).padding(horizontal = 2.dp),
                    color = Color(0xFF001C3B)
                )
                if (showTunggakanColumn) {
                    val displayTunggakan = if (selectedMenu == "KELOLAAN" || selectedMenu == "KUNJUNGAN") {
                        customer.totalTunggakan
                    } else {
                        val dCust = if (dailyDataMap.isNotEmpty()) {
                            dailyDataMap[customer.noRekening.trim()]
                                ?: (if (customer.noRekening.isNotBlank()) dailyDataMap[customer.noRekening.filter { it.isDigit() }] else null)
                        } else null
                        when {
                            dCust != null && dCust.totalTunggakan > 0.0 -> dCust.totalTunggakan
                            customer.totalTunggakan > 0.0 -> customer.totalTunggakan
                            dCust != null && (dCust.tunggakanPokok + dCust.tunggakanBunga) > 0.0 -> dCust.tunggakanPokok + dCust.tunggakanBunga
                            (customer.tunggakanPokok + customer.tunggakanBunga) > 0.0 -> customer.tunggakanPokok + customer.tunggakanBunga
                            else -> 0.0
                        }
                    }
                    // Tunggakan
                    Text(
                        text = formatDoubleClean(displayTunggakan, formatter),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.width(62.dp).padding(horizontal = 2.dp),
                        color = Color(0xFF001C3B)
                    )
                }
                // Segmen
                Text(
                    text = customer.segmen,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(55.dp).padding(horizontal = 4.dp),
                    color = Color(0xFF001C3B)
                )
                if (selectedMenu == "KELOLAAN" || selectedMenu == "KUNJUNGAN") {
                    // Kolek (between segmen and NPD)
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        KolekBadge(kolek = customer.kolek, compact = true)
                    }
                    // NPD
                    Text(
                        text = customer.npd,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.width(45.dp).padding(horizontal = 4.dp),
                        color = Color(0xFF001C3B)
                    )
                    if (selectedMenu == "KELOLAAN" && loggedInPN.trim() == "207803") {
                        val desaVal = customer.extraColumns.split("||")
                            .firstOrNull { it.startsWith("Desa::") }
                            ?.substringAfter("Desa::")?.trim() ?: ""
                        Text(
                            text = desaVal,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(80.dp).padding(horizontal = 4.dp),
                            color = Color(0xFF001C3B)
                        )
                    }
                    // Alamat
                    val displayRowAlamat = if (selectedMenu == "EKSTRAKOMP" || customer.menuType == "EKSTRAKOMP") simplifyAddress(customer.alamat) else customer.alamat
                    Text(
                        text = displayRowAlamat,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(150.dp).padding(horizontal = 4.dp),
                        color = Color(0xFF001C3B)
                    )
                }
                if (selectedMenu != "KELOLAAN" && selectedMenu != "KUNJUNGAN") {
                    // Kolek
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        KolekBadge(kolek = customer.kolek, compact = true)
                    }
                    if (selectedMenu == "INTRAKOMP") {
                        // NPD
                        Text(
                            text = customer.npd,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.width(45.dp).padding(horizontal = 4.dp),
                            color = Color(0xFF001C3B)
                        )
                    }
                    // Nama Mantri
                    Text(
                        text = if (customer.namaMantri.isBlank() || customer.namaMantri == "TIDAK TERIDENTIFIKASI") "Blank PN" else customer.namaMantri,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(110.dp).padding(horizontal = 4.dp),
                        color = Color(0xFF43474E)
                    )
                }
            }

            // Compact vertical padding click area
            if (selectedMenu == "KELOLAAN") {
                val hasTindakLanjut = !tindakLanjutMap[uniqueKey].isNullOrBlank()
                IconButton(
                    onClick = { onShowTindakLanjutPopup(customer) },
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (hasTindakLanjut) Color(0xFFF57C00) else Color(0xFF005FAF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Isi Tindak Lanjut",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = { onRowClick() },
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Buka detail nasabah",
                        tint = Color(0xFF005FAF),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun RencanaCustomerCardList(
    customersList: List<CustomerEntity>,
    onCustomerClick: (CustomerEntity) -> Unit,
    onSaveTindakLanjut: (CustomerEntity, String) -> Unit,
    onDeleteTindakLanjut: (CustomerEntity) -> Unit,
    sortCol: SortColumn,
    sortDir: SortDirection,
    onSelectSort: (SortColumn) -> Unit,
    formatter: NumberFormat,
    tindakLanjutMap: Map<String, String> = emptyMap(),
    dailyDataMap: Map<String, CustomerEntity> = emptyMap()
) {
    val totalData = customersList.size
    val sumOs = remember(customersList) { customersList.sumOf { it.osPinjaman } }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val gapPx = with(density) { 6.dp.roundToPx() }
    val marginPx = with(density) { 8.dp.roundToPx() }

    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var activeTlCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var activeTlIdx by remember { mutableStateOf(0) }
    var activeTlAnchorRect by remember { mutableStateOf<Rect?>(null) }
    var manualTlCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var manualTlText by remember { mutableStateOf("") }
    var showSortDropdown by remember { mutableStateOf(false) }
    var sortButtonCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val sortOptions = remember {
        listOf(
            SortColumn.NAMA_NASABAH to "Nama",
            SortColumn.DESA to "Desa",
            SortColumn.KOLEK to "Kolek",
            SortColumn.NPD to "NPD",
            SortColumn.SEGMEN to "Segmen",
            SortColumn.ALAMAT to "Alamat"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { containerCoords = it }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (customersList.isEmpty()) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EventNote,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Belum ada daftar rencana kunjungan",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF00386B),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Cari nasabah di kolom pencarian atas untuk menambahkan ke daftar rencana.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    itemsIndexed(
                        items = customersList,
                        key = { idx, customer ->
                            if (customer.id != 0) "rencana_${customer.id}"
                            else "rencana_${customer.noRekening}_${customer.cif}_${customer.namaNasabah}_$idx"
                        }
                    ) { idx, customer ->
                        RencanaCustomerCard(
                            idx = idx,
                            customer = customer,
                            tindakLanjutMap = tindakLanjutMap,
                            dailyDataMap = dailyDataMap,
                            onDetailClick = { onCustomerClick(customer) },
                            onOpenTlMenu = { iconCoords ->
                                val cc = containerCoords
                                val rect = if (cc != null && cc.isAttached && iconCoords != null && iconCoords.isAttached) {
                                    cc.localBoundingBoxOf(iconCoords, clipBounds = false)
                                } else {
                                    null
                                }
                                showSortDropdown = false
                                activeTlCustomer = customer
                                activeTlIdx = idx
                                activeTlAnchorRect = rect
                            }
                        )
                    }
                }
            }

            // Compact summary & Sort By footer bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OS ${formatDoubleClean(sumOs, formatter)} • $totalData Deb",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF00386B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    val activeSortLabel = sortOptions.firstOrNull { it.first == sortCol }?.second ?: "Nama"
                    val isSortMatch = sortOptions.any { it.first == sortCol }

                    Row(
                        modifier = Modifier
                            .onGloballyPositioned { sortButtonCoords = it }
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSortMatch) Color(0xFF005FAF) else Color(0xFFE2E8F0))
                            .clickable {
                                activeTlCustomer = null
                                activeTlAnchorRect = null
                                showSortDropdown = !showSortDropdown
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("rencana_sort_by_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort by",
                            tint = if (isSortMatch) Color.White else Color(0xFF334155),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isSortMatch) "Sort: $activeSortLabel" else "Sort by",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSortMatch) Color.White else Color(0xFF334155)
                        )
                        if (isSortMatch) {
                            Icon(
                                imageVector = if (sortDir == SortDirection.ASC) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = if (sortDir == SortDirection.ASC) "Ascending" else "Descending",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih urutan",
                            tint = if (isSortMatch) Color.White else Color(0xFF334155),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // In-window overlay for Tindak Lanjut menu (avoids separate Pop-Up Window surface teardown issues)
        val currentTlCust = activeTlCustomer
        if (currentTlCust != null) {
            BackHandler {
                activeTlCustomer = null
                activeTlAnchorRect = null
            }

            val tlUniqueKey = if (currentTlCust.noRekening.isNotBlank()) currentTlCust.noRekening
            else if (currentTlCust.cif.isNotBlank()) currentTlCust.cif
            else currentTlCust.namaNasabah
            val currentTlVal = tindakLanjutMap["kunjungan_$tlUniqueKey"]?.trim().orEmpty()
            val tlOptions = remember {
                listOf(
                    "Bayar Lunas" to Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32)),
                    "Bayar 1x angsuran" to Pair(Color(0xFFE0F2F1), Color(0xFF00695C)),
                    "Bayar Sebagian" to Pair(Color(0xFFE3F2FD), Color(0xFF1565C0)),
                    "Janji Bayar" to Pair(Color(0xFFFFF3E0), Color(0xFFE65100)),
                    "Tidak Ditempat" to Pair(Color(0xFFF3E5F5), Color(0xFF6A1B9A)),
                    "Pindah Alamat" to Pair(Color(0xFFE8EAF6), Color(0xFF283593)),
                    "Meninggal Dunia" to Pair(Color(0xFFECEFF1), Color(0xFF37474F))
                )
            }
            val anchor = activeTlAnchorRect

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        activeTlCustomer = null
                        activeTlAnchorRect = null
                    }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                        val maxX = (constraints.maxWidth - placeable.width - marginPx).coerceAtLeast(marginPx)
                        val maxY = (constraints.maxHeight - placeable.height - marginPx).coerceAtLeast(marginPx)
                        val targetX = if (anchor != null) anchor.right.roundToInt() + gapPx else marginPx
                        val targetY = if (anchor != null) anchor.top.roundToInt() else marginPx
                        val x = targetX.coerceIn(marginPx, maxX)
                        val y = targetY.coerceIn(marginPx, maxY)
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.placeRelative(x, y)
                        }
                    }
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .width(155.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                        .testTag("rencana_tl_popup_$activeTlIdx")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF005FAF))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = currentTlCust.namaNasabah.trim().uppercase().ifBlank { "-" },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(
                            modifier = Modifier.padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Tindak Lanjut",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color(0xFF005FAF),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )

                        tlOptions.forEach { (opt, colorsPair) ->
                            val (bgCol, txtCol) = colorsPair
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(25.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(bgCol)
                                    .clickable {
                                        activeTlCustomer = null
                                        activeTlAnchorRect = null
                                        onSaveTindakLanjut(currentTlCust, opt)
                                    }
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = opt,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = txtCol,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.6.dp)

                        // Input Manual option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(25.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFFEBF5FF))
                                .clickable {
                                    manualTlText = currentTlVal
                                    manualTlCustomer = currentTlCust
                                    activeTlCustomer = null
                                    activeTlAnchorRect = null
                                }
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFF005FAF),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Input Manual",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF005FAF),
                                maxLines = 1
                            )
                        }

                        // Bersihkan option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(25.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFFFFEBEE))
                                .clickable {
                                    activeTlCustomer = null
                                    activeTlAnchorRect = null
                                    onDeleteTindakLanjut(currentTlCust)
                                }
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Bersihkan",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFFC62828),
                                maxLines = 1
                            )
                        }
                        }
                    }
                }
            }
        }

        // In-window overlay for Sort By dropdown (avoids separate Pop-Up Window surface teardown issues)
        if (showSortDropdown) {
            BackHandler { showSortDropdown = false }
            val cc = containerCoords
            val sbc = sortButtonCoords
            val sortAnchor = if (cc != null && cc.isAttached && sbc != null && sbc.isAttached) {
                cc.localBoundingBoxOf(sbc, clipBounds = false)
            } else {
                null
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showSortDropdown = false }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                        val maxX = (constraints.maxWidth - placeable.width - marginPx).coerceAtLeast(marginPx)
                        val maxY = (constraints.maxHeight - placeable.height - marginPx).coerceAtLeast(marginPx)
                        val targetX = if (sortAnchor != null) sortAnchor.right.roundToInt() - placeable.width else maxX
                        val targetY = if (sortAnchor != null) sortAnchor.top.roundToInt() - placeable.height - gapPx else maxY
                        val x = targetX.coerceIn(marginPx, maxX)
                        val y = targetY.coerceIn(marginPx, maxY)
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.placeRelative(x, y)
                        }
                    }
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .width(148.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                        .testTag("rencana_sort_by_menu")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        sortOptions.forEach { (col, label) ->
                            val isSelected = sortCol == col
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showSortDropdown = false
                                        onSelectSort(col)
                                    }
                                    .background(if (isSelected) Color(0xFFEBF5FF) else Color.Transparent)
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF005FAF) else Color(0xFF1E293B)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = if (sortDir == SortDirection.ASC) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = Color(0xFF005FAF),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val currentManualCust = manualTlCustomer
    if (currentManualCust != null) {
        AlertDialog(
            onDismissRequest = { manualTlCustomer = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(14.dp),
            title = {
                Text(
                    text = "Input Manual Tindak Lanjut",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF005FAF)
                    )
                )
            },
            text = {
                OutlinedTextField(
                    value = manualTlText,
                    onValueChange = { manualTlText = it },
                    placeholder = { Text("Tulis keterangan tindak lanjut...", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = manualTlText.trim()
                        if (trimmed.isNotEmpty()) {
                            onSaveTindakLanjut(currentManualCust, trimmed)
                            manualTlCustomer = null
                        }
                    },
                    enabled = manualTlText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF))
                ) {
                    Text("Simpan", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { manualTlCustomer = null }) {
                    Text("Batal", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun RencanaCustomerCard(
    idx: Int,
    customer: CustomerEntity,
    tindakLanjutMap: Map<String, String>,
    dailyDataMap: Map<String, CustomerEntity>,
    onDetailClick: () -> Unit,
    onOpenTlMenu: (LayoutCoordinates?) -> Unit
) {
    val uniqueKey = if (customer.noRekening.isNotBlank()) customer.noRekening else if (customer.cif.isNotBlank()) customer.cif else customer.namaNasabah
    val kunjunganTLVal = tindakLanjutMap["kunjungan_$uniqueKey"]?.trim().orEmpty()
    val generalTLVal = tindakLanjutMap[uniqueKey]?.trim().orEmpty()
    val hasTindakLanjut = kunjunganTLVal.isNotBlank()
    val isRowBergeserNpl = generalTLVal == "Bergeser NPL" || kunjunganTLVal == "Bergeser NPL"
    val isEkstrakomp = customer.menuType == "EKSTRAKOMP"

    val isDebiturLunasOrHilang = remember(customer, dailyDataMap, isEkstrakomp) {
        if (isEkstrakomp) {
            false
        } else if (customer.osPinjaman <= 0.0) {
            true
        } else if (dailyDataMap.isNotEmpty()) {
            val rawRek = customer.noRekening.trim()
            val cleanRek = rawRek.filter { it.isDigit() }
            val cleanCif = customer.cif.filter { it.isLetterOrDigit() }.dropWhile { it == '0' }
            val dInfo = dailyDataMap[rawRek]
                ?: (if (cleanRek.isNotBlank()) dailyDataMap[cleanRek] else null)
                ?: (if (cleanRek.length >= 15) dailyDataMap[cleanRek.takeLast(15)] else null)
                ?: (if (cleanRek.isNotBlank()) dailyDataMap[cleanRek.trimStart('0')] else null)
                ?: (if (customer.cif.isNotBlank()) dailyDataMap[customer.cif] else null)
                ?: (if (cleanCif.isNotBlank()) dailyDataMap[cleanCif] ?: dailyDataMap["CIF-$cleanCif"] else null)
                ?: (if (cleanRek.isNotBlank()) dailyDataMap.entries.firstOrNull { it.key.filter { c -> c.isDigit() } == cleanRek }?.value else null)
            dInfo == null || dInfo.osPinjaman <= 0.0
        } else {
            false
        }
    }

    val nameColor = when {
        isDebiturLunasOrHilang -> Color(0xFF757575)
        isRowBergeserNpl -> Color(0xFFD32F2F)
        else -> Color(0xFF00386B)
    }
    val nameDecoration = if (isDebiturLunasOrHilang) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None

    val rawAlamat = if (isEkstrakomp) simplifyAddress(customer.alamat) else customer.alamat
    val displayAlamat = rawAlamat.ifBlank {
        customer.desa.takeIf { it.isNotBlank() }?.let { "desa $it" } ?: "-"
    }.lowercase()
    val displaySegmen = if (isEkstrakomp) mapEkstrakompSegmen(customer.segmen) else customer.segmen

    val coroutineScope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val revealWidthDp = 82.dp
    val revealWidthPx = with(density) { revealWidthDp.toPx() }
    val offsetX = remember(uniqueKey) { Animatable(0f) }
    var personIconCoords by remember(uniqueKey) { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(14.dp),
                clip = false
            )
            .clip(RoundedCornerShape(14.dp))
    ) {
        // Hidden Detail button revealed when swiped left
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFF005FAF)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(
                modifier = Modifier
                    .width(revealWidthDp)
                    .fillMaxHeight()
                    .clickable {
                        coroutineScope.launch { offsetX.animateTo(0f, spring()) }
                        onDetailClick()
                    }
                    .testTag("rencana_detail_button_$idx"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Detail Debitur",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Detail",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }

        // Foreground Card styled after MantriPositionCard (without Plafond/OS/Tgk row)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            val target = (offsetX.value + delta).coerceIn(-revealWidthPx, 0f)
                            offsetX.snapTo(target)
                        }
                    },
                    onDragStopped = {
                        coroutineScope.launch {
                            if (offsetX.value < -revealWidthPx * 0.4f) {
                                offsetX.animateTo(-revealWidthPx, spring())
                            } else {
                                offsetX.animateTo(0f, spring())
                            }
                        }
                    }
                )
                .clickable(enabled = offsetX.value < -10f) {
                    coroutineScope.launch { offsetX.animateTo(0f, spring()) }
                }
                .testTag("rencana_customer_card_$idx"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = if (hasTindakLanjut) 1.2.dp else 1.dp,
                color = if (hasTindakLanjut) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Person icon in top-left corner: ONLY this triggers Tindak Lanjut menu (to its right)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasTindakLanjut) Color(0xFFE8F5E9) else Color(0xFFEBF5FF),
                    modifier = Modifier
                        .size(28.dp)
                        .onGloballyPositioned { personIconCoords = it }
                        .clickable {
                            if (offsetX.value < -10f) {
                                coroutineScope.launch { offsetX.animateTo(0f, spring()) }
                            }
                            onOpenTlMenu(personIconCoords)
                        }
                        .testTag("rencana_tl_icon_$idx")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (hasTindakLanjut) Icons.Default.Check else Icons.Default.Person,
                            contentDescription = if (hasTindakLanjut) "Tindak lanjut terisi" else "Pilih tindak lanjut",
                            tint = if (hasTindakLanjut) Color(0xFF2E7D32) else Color(0xFF005FAF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = customer.namaNasabah.uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textDecoration = nameDecoration
                            ),
                            color = nameColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (hasTindakLanjut) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isRowBergeserNpl) Color(0xFFFEE2E2) else Color(0xFFE8F5E9),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        if (isRowBergeserNpl) Color(0xFFEF9A9A) else Color(0xFFA5D6A7)
                                    )
                                ) {
                                    Text(
                                        text = kunjunganTLVal,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isRowBergeserNpl) Color(0xFFDC2626) else Color(0xFF2E7D32),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .widthIn(max = 76.dp)
                                            .padding(horizontal = 4.dp, vertical = 1.5.dp)
                                    )
                                }
                            }

                            val displayDesa = customer.desa.trim()
                            if (displayDesa.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFE0F2F1),
                                    border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF4DB6AC))
                                ) {
                                    Text(
                                        text = displayDesa,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFF00695C),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .widthIn(max = 76.dp)
                                            .padding(horizontal = 4.dp, vertical = 1.5.dp)
                                    )
                                }
                            }

                            KolekBadge(kolek = customer.kolek, compact = true)

                            if (displaySegmen.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFEBF5FF),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF90CAF9))
                                ) {
                                    Text(
                                        text = displaySegmen,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFF005FAF),
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Alamat dibawah nama dengan huruf kecil dan warna lebih tebal agar terbaca jelas
                    Text(
                        text = displayAlamat,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 13.sp
                        ),
                        color = Color(0xFF1E293B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


@Composable
fun KolekBadge(kolek: String, compact: Boolean = false) {
    val clean = kolek.trim().uppercase()
    val bgCol: Color
    val textCol: Color
    
    when {
        clean.contains("KL") || clean.contains("KURANG") || clean.contains("K.LANCAR") || clean.contains("K. LANCAR") -> {
            bgCol = Color(0xFFFCE7F3)  // pink-100 (merah muda)
            textCol = Color(0xFFBE185D) // pink-700
        }
        clean.contains("LANCAR") || clean == "L" -> {
            bgCol = Color(0xFFDCFCE7)  // green-100 (hijau)
            textCol = Color(0xFF15803D) // green-700
        }
        clean.contains("SML1") || clean.contains("SML2") || clean.contains("SML3") || 
        clean.contains("SML 1") || clean.contains("SML 2") || clean.contains("SML 3") || 
        clean.contains("DPK") || clean == "SML" -> {
            bgCol = Color(0xFFFEF3C7)  // amber-100 (Kuning/oranye)
            textCol = Color(0xFFD97706) // amber-700 (Kuning/oranye)
        }
        clean.contains("DIRAGUKAN") || clean == "D" -> {
            bgCol = Color(0xFFFEE2E2)  // red-100 (merah)
            textCol = Color(0xFFDC2626) // red-600
        }
        clean.contains("MACET") || clean == "M" || clean == "NPL" -> {
            bgCol = Color(0xFFEF4444)  // red-500 (merah biasa, tidak terlalu tua)
            textCol = Color(0xFFFFFFFF) // white for high contrast
        }
        else -> {
            bgCol = Color(0xFFF3EDF7)
            textCol = Color(0xFF49454F)
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(if (compact) 4.dp else 6.dp))
            .background(bgCol)
            .padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 3.dp else 4.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        val shortLabel = getShortKolekLabel(kolek)
        Text(
            text = shortLabel,
            style = if (compact) MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp) else MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textCol,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


fun getShortKolekLabel(kolek: String): String {
    val clean = kolek.trim().uppercase()
    return when {
        clean.contains("KL") || clean.contains("KURANG") || clean.contains("K.LANCAR") || clean.contains("K. LANCAR") -> "KL"
        clean.contains("LANCAR") || clean == "L" -> "L"
        clean.contains("DIRAGUKAN") || clean == "D" -> "D"
        clean.contains("MACET") || clean == "M" -> "M"
        else -> kolek
    }
}

private fun rencanaCustomerKey(customer: CustomerEntity): String =
    if (customer.noRekening.isNotBlank()) customer.noRekening
    else if (customer.cif.isNotBlank()) customer.cif
    else customer.namaNasabah

private fun resolveRencanaSearchKolekColors(kolekLabel: String): Pair<Color, Color> {
    val clean = kolekLabel.trim().uppercase()
    return when {
        clean == "DH" -> Color(0xFFFFDAD6) to Color(0xFF93000A)
        clean.contains("KL") || clean.contains("KURANG") || clean.contains("K.LANCAR") || clean.contains("K. LANCAR") ->
            Color(0xFFFCE7F3) to Color(0xFFBE185D)
        clean.contains("LANCAR") || clean == "L" ->
            Color(0xFFDCFCE7) to Color(0xFF15803D)
        clean.contains("SML1") || clean.contains("SML2") || clean.contains("SML3") ||
            clean.contains("SML 1") || clean.contains("SML 2") || clean.contains("SML 3") ||
            clean.contains("DPK") || clean == "SML" ->
            Color(0xFFFEF3C7) to Color(0xFFD97706)
        clean.contains("DIRAGUKAN") || clean == "D" ->
            Color(0xFFFEE2E2) to Color(0xFFDC2626)
        clean.contains("MACET") || clean == "M" || clean == "NPL" ->
            Color(0xFFEF4444) to Color.White
        else -> Color(0xFFF3EDF7) to Color(0xFF49454F)
    }
}

@Composable
private fun RencanaSearchBadge(
    text: String,
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xFF005FAF),
    textColor: Color = Color.White
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(3.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            fontSize = 8.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

fun getUmurTgkStatus(days: Int): String {
    return when {
        days <= 0 -> "Lancar"
        days <= 30 -> "SML1"
        days <= 60 -> "SML2"
        days <= 90 -> "SML3"
        days <= 120 -> "KL"
        days <= 179 -> "D"
        else -> "M"
    }
}
@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Data Tidak Ditemukan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Silakan bersihkan filter pencarian atau sinkronisasikan ulang spreadsheet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.neutralSecondary(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.neutralSecondary()
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DetailItemCol(
    label: String,
    value: String = "",
    annotatedValue: AnnotatedString? = null,
    valueColor: Color = Color(0xFF1D1B20),
    showWarningIcon: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.5.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
            color = Color(0xFF005FAF),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(1.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (showWarningIcon) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(11.dp)
                )
            }
            if (annotatedValue != null) {
                Text(
                    text = annotatedValue,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = valueColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = valueColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

@Composable
fun DetailItemColWithChange(
    label: String,
    oldValue: String,
    newValue: String?,
    showChange: Boolean = true,
    onCopyClick: (() -> Unit)? = null
) {
    val hasChange = showChange && newValue != null && newValue != oldValue
    val annotatedValue = androidx.compose.ui.text.buildAnnotatedString {
        if (hasChange) {
            // Angka sebelumnya di-coret dan warnanya agak pudar tapi masih bisa terbaca
            withStyle(
                style = androidx.compose.ui.text.SpanStyle(
                    color = Color(0xFF8E8E93),
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                    fontWeight = FontWeight.Medium
                )
            ) {
                append(oldValue)
            }
            withStyle(style = androidx.compose.ui.text.SpanStyle(color = Color(0xFF6B7280))) { // Gray arrow
                append(" -> ")
            }
            withStyle(style = androidx.compose.ui.text.SpanStyle(color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)) { // Red new value
                append(newValue)
            }
        } else {
            append(oldValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
                color = Color(0xFF005FAF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (onCopyClick != null) {
                IconButton(
                    onClick = onCopyClick,
                    modifier = Modifier.size(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy $label",
                        modifier = Modifier.size(10.dp),
                        tint = Color(0xFF005FAF)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = annotatedValue,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = Color(0xFF1D1B20),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SleekFilterDropdown(
    selectedValue: String,
    options: List<String>,
    displayOption: (String) -> String = { it },
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isSelected = selectedValue != "SEMUA"
    val bgColor = if (isSelected) Color(0xFFFF7043) else Color(0xFF005FAF)

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(bgColor, shape = RoundedCornerShape(16.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayOption(selectedValue),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .heightIn(max = 200.dp)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = displayOption(opt),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        )
                    },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Custom MaterialTheme styles extension to follow frontend-design guidelines strictly
@Composable
fun MaterialTheme.styleOfDisplayTitle() = typography.titleLarge.copy(
    fontWeight = FontWeight.ExtraBold,
    letterSpacing = (-0.5).sp
)

@Composable
fun ColorScheme.neutralSecondary() = onSurfaceVariant.copy(alpha = 0.7f)

@Composable
fun InfoBox(
    title: String,
    osVal: Double,
    debtorCount: Int,
    percentage: Double? = null,
    formatter: NumberFormat,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isSelected) Color(0xFFC3E8FF) else Color(0xFFEBF5FF), shape = RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF00386B) else Color(0xFF005FAF),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        // Debtor count top-right corner with person icon, colored red, small letters
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFFC62828),
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = "$debtorCount deb",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFC62828)
            )
        }

        // Title and Value
        Column(
            modifier = Modifier.fillMaxWidth().padding(end = 45.dp), // prevent overlapping top-right text
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF00386B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (percentage != null) {
                    Text(
                        text = "(${String.format(Locale.US, "%.1f", percentage)}%)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color(0xFF005FAF)
                    )
                }
            }
            Text(
                text = "Rp ${formatDoubleClean(osVal, formatter)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color(0xFF052C54)
            )
        }
    }
}

@Composable
fun MiniInfoBox(
    title: String,
    osVal: Double,
    debtorCount: Int,
    formatter: NumberFormat,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(if (isSelected) Color(0xFFC3E8FF) else Color(0xFFEBF5FF), shape = RoundedCornerShape(6.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFF00386B) else Color(0xFF005FAF),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        // Debtor Count top-right
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFFC62828),
                modifier = Modifier.size(8.dp)
            )
            Text(
                text = "$debtorCount",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFC62828)
            )
        }

        // Title and Value
        Column(
            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF00386B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatDoubleClean(osVal, formatter),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF052C54),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ToggleFilterSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (checked) Color(0xFFFF7043) else Color(0xFF005FAF)
    Box(
        modifier = modifier
            .height(24.dp)
            .background(bgColor, shape = RoundedCornerShape(16.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SimpananTable(
    simpananList: List<com.example.data.model.SimpananRow>,
    formatter: java.text.NumberFormat,
    sortCol: String,
    sortDir: com.example.ui.viewmodel.SortDirection,
    onSortClick: (String) -> Unit
) {
    val horizontalScrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCCB0)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Row: Frozen (Nama Nasabah) + Scrollable (No Rek Simpanan, CIF, Saldo, Kode1)
            Row(
                modifier = Modifier
                    .background(Color(0xFF005FAF))
                    .padding(vertical = 2.dp, horizontal = 4.dp)
                    .height(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // FIXED/FROZEN COLUMN: Nama Nasabah
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "NAMA NASABAH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White
                    )
                }

                // Vertical Divider to separate frozen column
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.3f))
                )

                // SCROLLABLE PART (No Rek Simpanan, CIF, Saldo, Kode1)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // No Rek Simpanan Column
                    Box(
                        modifier = Modifier
                            .width(85.dp)
                            .padding(horizontal = 3.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "NO REK SIMPANAN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp
                            ),
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.3f))
                    )

                    // CIF Column
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .padding(horizontal = 3.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "CIF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp
                            ),
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.3f))
                    )

                    // Saldo Column (Clickable Header)
                    HeaderCell(
                        title = "SALDO",
                        width = 80.dp,
                        align = Alignment.CenterEnd,
                        sorted = sortCol == "SALDO",
                        sortDir = sortDir,
                        onClick = { onSortClick("SALDO") }
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.3f))
                    )

                    // Kode1 Column (Clickable Header)
                    HeaderCell(
                        title = "KODE1",
                        width = 50.dp,
                        align = Alignment.Center,
                        sorted = sortCol == "KODE1",
                        sortDir = sortDir,
                        onClick = { onSortClick("KODE1") }
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFFFCCB0))

            // LazyColumn for Rows
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                itemsIndexed(
                    items = simpananList,
                    key = { idx, row -> "${row.cif}_${row.noTabungan}_$idx" }
                ) { idx, row ->
                    val rowBgColor = if (idx % 2 == 0) Color(0xFFEEF4FC) else Color(0xFFD5E3F5)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBgColor)
                            .padding(vertical = 1.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // FIXED/FROZEN: Nama Nasabah Cell
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = row.namaNasabah.uppercase(),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001C3B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Vertical Divider to separate frozen column
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(18.dp)
                                .background(Color(0xFFFFCCB0))
                        )

                        // SCROLLABLE PART (No Rek Simpanan, CIF, Saldo, Kode1)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(horizontalScrollState),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // No Rek Simpanan Cell
                            Box(
                                modifier = Modifier
                                    .width(85.dp)
                                    .padding(horizontal = 3.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = row.noTabungan,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                    color = Color(0xFF001C3B),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Border line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(18.dp)
                                    .background(Color(0xFFFFCCB0).copy(alpha = 0.5f))
                            )

                            // CIF Cell
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .padding(horizontal = 3.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = row.cif,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                    color = Color(0xFF001C3B),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Border line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(18.dp)
                                    .background(Color(0xFFFFCCB0).copy(alpha = 0.5f))
                            )

                            // Saldo Cell
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = formatDoubleClean(row.balance, formatter),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF001C3B),
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Border line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(18.dp)
                                    .background(Color(0xFFFFCCB0).copy(alpha = 0.5f))
                            )

                            // Kode1 Cell
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = row.kode1,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFFFCCB0).copy(alpha = 0.3f))
                }
            }

            // Summary Footer for Simpanan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF7F2))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalData = simpananList.size
                val sumBalance = simpananList.sumOf { it.balance }
                Text(
                    text = "Jumlah data: $totalData Rekening | Total Saldo: ${formatDoubleClean(sumBalance, formatter)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF43474E)
                )
            }
        }
    }
}

@Composable
fun DetailUnitDashboard(
    customersList: List<CustomerEntity>,
    dailyDataMap: Map<String, CustomerEntity>,
    onDismiss: () -> Unit,
    formatter: NumberFormat
) {
    val totalOs = customersList.sumOf { it.osPinjaman }
    val totalOsDaily = customersList.sumOf { dailyDataMap[it.noRekening]?.osPinjaman ?: 0.0 }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize().padding(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Dashboard Unit", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total OS Pinjaman", style = MaterialTheme.typography.titleSmall)
                        Text("Bulan Lalu: ${formatDoubleClean(totalOs, formatter)}", style = MaterialTheme.typography.headlineMedium)
                        Text("Posisi Kini: ${formatDoubleClean(totalOsDaily, formatter)}", style = MaterialTheme.typography.headlineMedium)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Tutup")
                }
            }
        }
    }
}

data class TusimPopupItem(
    val namaNasabah: String,
    val noPinjaman: String,
    val noSimpanan: String,
    val saldo: Double
)
