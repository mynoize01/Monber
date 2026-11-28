package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.GroupOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.data.model.desa
import com.example.ui.components.LazyLoadingFooterRow
import com.example.ui.components.rememberLazyPagedItems
import java.text.NumberFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class BerandaListFilterType {
    KUALITAS,
    SEGMEN
}

data class BerandaCustomerListPageRequest(
    val filterType: BerandaListFilterType,
    val filterValue: String,
    val scopeLabel: String,
    val customers: List<CustomerEntity>
)

private enum class BerandaListSortOption(val key: String, val label: String) {
    OS_DESC("os_desc", "OS Terbesar"),
    TUNGGAKAN_DESC("tgk_desc", "Tunggakan"),
    UMUR_TGK_DESC("umur_desc", "Umur Tgk"),
    NAMA_ASC("nama_asc", "Nama A-Z")
}

private data class ProcessedBerandaCustomerList(
    val categoryCount: Int = 0,
    val filteredCustomers: List<CustomerEntity> = emptyList(),
    val totalOs: Double = 0.0,
    val totalTunggakan: Double = 0.0
)

@Composable
fun BerandaFilteredCustomerListPage(
    request: BerandaCustomerListPageRequest,
    formatter: NumberFormat,
    onBack: () -> Unit,
    onCustomerClick: (CustomerEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrgentKolek = request.filterType == BerandaListFilterType.KUALITAS &&
        (request.filterValue.equals("SML", ignoreCase = true) || request.filterValue.equals("NPL", ignoreCase = true))

    var searchQuery by remember(request.filterType, request.filterValue, request.scopeLabel) { mutableStateOf("") }
    var sortOption by remember(request.filterType, request.filterValue, request.scopeLabel) {
        mutableStateOf(if (isUrgentKolek) BerandaListSortOption.TUNGGAKAN_DESC else BerandaListSortOption.OS_DESC)
    }

    val (accentColor, badgeBgColor, badgeBorderColor) = remember(request.filterType, request.filterValue) {
        when {
            request.filterType == BerandaListFilterType.KUALITAS && request.filterValue.equals("Lancar", ignoreCase = true) ->
                Triple(Color(0xFF16A34A), Color(0xFFF0FDF4), Color(0xFFBBF7D0))
            request.filterType == BerandaListFilterType.KUALITAS && request.filterValue.equals("SML", ignoreCase = true) ->
                Triple(Color(0xFFD97706), Color(0xFFFFFBEB), Color(0xFFFDE68A))
            request.filterType == BerandaListFilterType.KUALITAS && request.filterValue.equals("NPL", ignoreCase = true) ->
                Triple(Color(0xFFDC2626), Color(0xFFFEF2F2), Color(0xFFFECACA))
            else ->
                Triple(Color(0xFF005FAF), Color(0xFFEFF6FF), Color(0xFFBFDBFE))
        }
    }

    val pageTitle = remember(request.filterType, request.filterValue) {
        if (request.filterType == BerandaListFilterType.KUALITAS) {
            "Nasabah ${request.filterValue}"
        } else {
            "Segmen ${request.filterValue}"
        }
    }

    val categoryTagLabel = remember(request.filterType, request.filterValue) {
        if (request.filterType == BerandaListFilterType.KUALITAS) {
            when (request.filterValue.uppercase()) {
                "LANCAR" -> "Kolektibilitas 1 (Lancar)"
                "SML" -> "Dalam Perhatian Khusus"
                "NPL" -> "Kurang Lancar, Diragukan & Macet"
                else -> "Kualitas Kredit"
            }
        } else {
            "Daftar Nasabah Segmen"
        }
    }

    // Filter kategori, pencarian, dan pengurutan dijalankan di Dispatchers.Default
    // agar pembukaan halaman tetap instan dan responsif walaupun memuat ratusan data nasabah.
    var processedState by remember(request.filterType, request.filterValue, request.scopeLabel) {
        mutableStateOf(ProcessedBerandaCustomerList())
    }
    var isProcessing by remember(request.filterType, request.filterValue, request.scopeLabel) {
        mutableStateOf(true)
    }

    LaunchedEffect(request, searchQuery, sortOption) {
        isProcessing = processedState.filteredCustomers.isEmpty() && processedState.categoryCount == 0
        val result = withContext(Dispatchers.Default) {
            val targetValueUpper = request.filterValue.trim().uppercase()
            val categoryMatched = when (request.filterType) {
                BerandaListFilterType.KUALITAS -> request.customers.filter { cust ->
                    val k = cust.kolek.trim().uppercase()
                    when (targetValueUpper) {
                        "LANCAR" -> k == "L" || k == "LANCAR" || k == "1" || (k.contains("LANCAR") && !k.contains("KURANG"))
                        "SML" -> k.startsWith("SML") || k.contains("DPK") || k == "2"
                        "NPL" -> k == "KL" || k.contains("KURANG") || k == "D" || k.contains("DIRAGUKAN") ||
                            k == "M" || k.contains("MACET") || k == "3" || k == "4" || k == "5" || k == "NPL"
                        else -> false
                    }
                }
                BerandaListFilterType.SEGMEN -> request.customers.filter { cust ->
                    cust.segmen.trim().uppercase().ifBlank { "LAINNYA" } == targetValueUpper
                }
            }

            val q = searchQuery.trim()
            val filtered = if (q.isEmpty()) {
                categoryMatched
            } else {
                categoryMatched.filter { cust ->
                    cust.namaNasabah.contains(q, ignoreCase = true) ||
                        cust.noRekening.contains(q, ignoreCase = true) ||
                        cust.cif.contains(q, ignoreCase = true) ||
                        cust.namaMantri.contains(q, ignoreCase = true) ||
                        cust.segmen.contains(q, ignoreCase = true) ||
                        cust.desa.contains(q, ignoreCase = true) ||
                        cust.alamat.contains(q, ignoreCase = true)
                }
            }

            val sorted = when (sortOption) {
                BerandaListSortOption.OS_DESC -> filtered.sortedWith(
                    compareByDescending<CustomerEntity> { it.osPinjaman }
                        .thenByDescending { it.totalTunggakan }
                        .thenBy { it.namaNasabah.lowercase() }
                )
                BerandaListSortOption.TUNGGAKAN_DESC -> filtered.sortedWith(
                    compareByDescending<CustomerEntity> { it.totalTunggakan }
                        .thenByDescending { it.osPinjaman }
                        .thenBy { it.namaNasabah.lowercase() }
                )
                BerandaListSortOption.UMUR_TGK_DESC -> filtered.sortedWith(
                    compareByDescending<CustomerEntity> { it.umurTgk }
                        .thenByDescending { it.totalTunggakan }
                        .thenByDescending { it.osPinjaman }
                )
                BerandaListSortOption.NAMA_ASC -> filtered.sortedBy { it.namaNasabah.trim().lowercase() }
            }

            ProcessedBerandaCustomerList(
                categoryCount = categoryMatched.size,
                filteredCustomers = sorted,
                totalOs = sorted.sumOf { it.osPinjaman },
                totalTunggakan = sorted.sumOf { it.totalTunggakan }
            )
        }
        processedState = result
        isProcessing = false
    }

    val listState = rememberLazyListState()
    val paginationResetKey = Triple(
        "${request.filterType}_${request.filterValue}_${request.scopeLabel}",
        searchQuery.trim(),
        sortOption
    )
    LaunchedEffect(paginationResetKey) {
        if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
            listState.scrollToItem(0)
        }
    }

    val pagedResult = rememberLazyPagedItems(
        items = processedState.filteredCustomers,
        listState = listState,
        pageSize = 30,
        prefetchDistance = 6,
        resetKey = paginationResetKey
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("beranda_filtered_customer_list_page"),
        color = Color(0xFFF3F6FB)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Top Gradient Header with Back Navigation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF003B73), Color(0xFF005FAF))
                        ),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .statusBarsPadding()
                    .padding(top = 6.dp, bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .align(Alignment.Center)
                        .padding(horizontal = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("beranda_list_page_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke Beranda",
                                tint = Color.White
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = pageTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = categoryTagLabel,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                        ) {
                            Text(
                                text = request.scopeLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Summary Metrics Card inside header
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total OS Pinjaman",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Rp ${formatDoubleClean(processedState.totalOs, formatter)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (processedState.totalTunggakan > 0.0) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Text(
                                        text = "Total Tunggakan",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFDE68A)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Rp ${formatDoubleClean(processedState.totalTunggakan, formatter)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFEF08A),
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Jumlah Debitur",
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = formatter.format(processedState.filteredCustomers.size),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search & Sort Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .align(Alignment.CenterHorizontally)
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("beranda_list_page_search"),
                    placeholder = {
                        Text(
                            text = "Cari nama, no. rekening, CIF, desa, mantri...",
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = Color(0xFF64748B)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("beranda_list_page_clear_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus pencarian",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sort Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BerandaListSortOption.entries.forEach { option ->
                        val selected = sortOption == option
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { sortOption = option }
                                .testTag("beranda_list_page_sort_${option.key}"),
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) accentColor else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (selected) accentColor else Color(0xFFE2E8F0)
                            )
                        ) {
                            Text(
                                text = option.label,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (selected) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isProcessing) {
                            "Menyiapkan daftar nasabah..."
                        } else if (searchQuery.isBlank()) {
                            if (pagedResult.hasMore) {
                                "Menampilkan ${pagedResult.visibleItems.size} dari ${formatter.format(pagedResult.totalCount)} nasabah"
                            } else {
                                "Menampilkan ${formatter.format(pagedResult.totalCount)} nasabah"
                            }
                        } else {
                            "Ditemukan ${formatter.format(pagedResult.totalCount)} dari ${formatter.format(processedState.categoryCount)} nasabah"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "Ketuk kartu untuk rincian",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Customer List, Loading, or Empty State
                val listStateKey = Triple(
                    isProcessing,
                    (processedState.categoryCount == 0) to processedState.filteredCustomers.isEmpty(),
                    sortOption
                )
                AnimatedContent(
                    targetState = listStateKey,
                    transitionSpec = {
                        (fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                            slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it / 16 }) togetherWith
                            (fadeOut(tween(150, easing = FastOutSlowInEasing)) +
                                slideOutVertically(tween(150, easing = FastOutSlowInEasing)) { -it / 20 })
                    },
                    modifier = Modifier.weight(1f),
                    label = "beranda_filtered_list_content"
                ) { (loadingNow, emptyFlags, _) ->
                    val (isSourceEmpty, isSearchEmpty) = emptyFlags
                    when {
                        loadingNow -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = accentColor,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Text(
                                        text = "Memuat data nasabah...",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                        isSourceEmpty || isSearchEmpty -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .background(badgeBgColor, CircleShape)
                                                .border(1.dp, badgeBorderColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.GroupOff,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Text(
                                            text = if (isSourceEmpty) {
                                                "Belum Ada Nasabah di Kategori Ini"
                                            } else {
                                                "Nasabah Tidak Ditemukan"
                                            },
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = if (isSourceEmpty) {
                                                "Tidak terdapat data nasabah untuk ${request.filterValue} pada cakupan ${request.scopeLabel}."
                                            } else {
                                                "Tidak ada nasabah yang cocok dengan kata kunci \"${searchQuery.trim()}\"."
                                            },
                                            fontSize = 12.5.sp,
                                            color = Color(0xFF64748B),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("beranda_list_page_recycler"),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                itemsIndexed(
                                    items = pagedResult.visibleItems,
                                    key = { idx, cust -> "${cust.noRekening}_${cust.cif}_${cust.namaNasabah}_$idx" }
                                ) { idx, customer ->
                                    BerandaFilteredCustomerCard(
                                        index = idx + 1,
                                        customer = customer,
                                        showMantri = request.scopeLabel.equals("Unit", ignoreCase = true),
                                        accentColor = accentColor,
                                        badgeBgColor = badgeBgColor,
                                        badgeBorderColor = badgeBorderColor,
                                        formatter = formatter,
                                        onClick = { onCustomerClick(customer) },
                                        modifier = Modifier
                                            .animateItem()
                                            .testTag("beranda_list_page_item_$idx")
                                    )
                                }

                                if (pagedResult.hasMore) {
                                    item(key = "beranda_list_lazy_footer") {
                                        LazyLoadingFooterRow(
                                            visibleCount = pagedResult.visibleItems.size,
                                            totalCount = pagedResult.totalCount,
                                            accentColor = accentColor
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

@Composable
private fun BerandaFilteredCustomerCard(
    index: Int,
    customer: CustomerEntity,
    showMantri: Boolean,
    accentColor: Color,
    badgeBgColor: Color,
    badgeBorderColor: Color,
    formatter: NumberFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kolekText = customer.kolek.trim().ifBlank { "-" }
    val (kolekAccent, kolekBg, kolekBorder) = remember(kolekText) {
        val k = kolekText.uppercase()
        when {
            k == "KL" || k.contains("KURANG") || k == "D" || k.contains("DIRAGUKAN") ||
                k == "M" || k.contains("MACET") || k == "3" || k == "4" || k == "5" || k == "NPL" ->
                Triple(Color(0xFFDC2626), Color(0xFFFEF2F2), Color(0xFFFECACA))
            k.startsWith("SML") || k.contains("DPK") || k == "2" ->
                Triple(Color(0xFFD97706), Color(0xFFFFFBEB), Color(0xFFFDE68A))
            else ->
                Triple(Color(0xFF16A34A), Color(0xFFF0FDF4), Color(0xFFBBF7D0))
        }
    }

    val desaLabel = customer.desa.trim().ifBlank { customer.alamat.trim() }
    val mantriLabel = cleanMantriNameOnly(customer.namaMantri).ifBlank { customer.namaMantri.trim() }
    val metaLine = buildList {
        if (customer.noRekening.isNotBlank()) add(customer.noRekening.trim())
        if (customer.segmen.isNotBlank()) add(customer.segmen.trim())
        if (showMantri && mantriLabel.isNotBlank() && !mantriLabel.equals("Blank PN", ignoreCase = true)) {
            add(mantriLabel)
        }
    }.joinToString(" • ")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(badgeBgColor, CircleShape)
                        .border(1.dp, badgeBorderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index",
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
                        text = customer.namaNasabah.trim().ifBlank { "Tanpa Nama" },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (metaLine.isNotBlank()) {
                        Text(
                            text = metaLine,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (desaLabel.isNotBlank()) {
                        Text(
                            text = desaLabel,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Lihat detail nasabah",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = kolekBg,
                        border = BorderStroke(0.8.dp, kolekBorder)
                    ) {
                        Text(
                            text = "Kolek $kolekText",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = kolekAccent,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    if (customer.umurTgk > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF1F2),
                            border = BorderStroke(0.8.dp, Color(0xFFFECDD3))
                        ) {
                            Text(
                                text = "Tgk ${customer.umurTgk} hr",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBE123C),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Rp ${formatDoubleClean(customer.osPinjaman, formatter)}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1
                    )
                    if (customer.totalTunggakan > 0.0) {
                        Text(
                            text = "Tgk Rp ${formatDoubleClean(customer.totalTunggakan, formatter)}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBE123C),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
