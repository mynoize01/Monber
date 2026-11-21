package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomerEntity
import java.text.NumberFormat

data class RealizedCustomerItem(
    val customer: CustomerEntity,
    val plafondBaru: Double,
    val osBlnLalu: Double,
    val netDisburs: Double,
    val isNasabahBaru: Boolean
)

@Composable
fun RealizedCustomersPopupDialog(
    mantriName: String,
    items: List<RealizedCustomerItem>,
    onDismiss: () -> Unit,
    formatter: NumberFormat,
    onCustomerClick: ((CustomerEntity) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("ALL") } // "ALL", "BARU", "SUPLESI"

    val newCount = remember(items) { items.count { it.isNasabahBaru } }
    val suplesiCount = remember(items) { items.size - newCount }

    val filteredList = remember(items, searchQuery, filterStatus) {
        val q = searchQuery.trim().lowercase()
        items.filter { item ->
            val matchStatus = when (filterStatus) {
                "BARU" -> item.isNasabahBaru
                "SUPLESI" -> !item.isNasabahBaru
                else -> true
            }
            val matchSearch = if (q.isBlank()) true else {
                item.customer.namaNasabah.lowercase().contains(q) ||
                item.customer.noRekening.lowercase().contains(q) ||
                item.customer.cif.lowercase().contains(q) ||
                item.customer.tglReal.lowercase().contains(q) ||
                (if (item.isNasabahBaru) "baru" else "suplesi").contains(q)
            }
            matchStatus && matchSearch
        }
    }

    val sumPlafond = remember(filteredList) { filteredList.sumOf { it.plafondBaru } }
    val sumOsLalu = remember(filteredList) { filteredList.sumOf { it.osBlnLalu } }
    val sumNetDisburs = remember(filteredList) { filteredList.sumOf { it.netDisburs } }

    // Column widths tightened and matched to Monitoring/Tucil table proportions
    val frozenColWidth = 108.dp // Nama Nasabah (frozen left)

    val colRekWidth = 72.dp
    val colCifWidth = 38.dp
    val colTglWidth = 48.dp
    val colPlafonWidth = 58.dp
    val colOsLaluWidth = 58.dp
    val colNetDisbursWidth = 60.dp
    val colStatusWidth = 40.dp
    val scrollableContentWidth = colRekWidth + colCifWidth + colTglWidth + colPlafonWidth + colOsLaluWidth + colNetDisbursWidth + colStatusWidth

    val horizontalScrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar Dialog (identical to Tucil / CustomerTablePopupDialog)
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
                            text = "Nasabah Realisasi: ${cleanMantriNameOnly(mantriName)}",
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
                            text = "Net Disburs: Rp ${formatDoubleClean(sumNetDisburs, formatter)} | ${filteredList.size} Debitur",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFBBF7D0),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
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

                // Search & Filter Status Chips (matching CustomerTablePopupDialog searchbar)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                            text = "Cari nama, rekening, CIF...",
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterStatusChip(
                            label = "Semua (${items.size})",
                            isSelected = filterStatus == "ALL",
                            color = Color(0xFF005FAF),
                            onClick = { filterStatus = "ALL" }
                        )
                        FilterStatusChip(
                            label = "Baru ($newCount)",
                            isSelected = filterStatus == "BARU",
                            color = Color(0xFF166534),
                            onClick = { filterStatus = "BARU" }
                        )
                        FilterStatusChip(
                            label = "Suplesi ($suplesiCount)",
                            isSelected = filterStatus == "SUPLESI",
                            color = Color(0xFF0284C7),
                            onClick = { filterStatus = "SUPLESI" }
                        )
                    }
                }

                // Inner Table Card identical to CustomerTable (Tucil table)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada nasabah realisasi",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCCB0)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Table Header Row matching TableHeader
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF005FAF))
                                        .padding(vertical = 2.dp, horizontal = 4.dp)
                                        .height(26.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Frozen left: Nama Nasabah
                                    Box(
                                        modifier = Modifier
                                            .width(frozenColWidth)
                                            .fillMaxHeight()
                                            .padding(horizontal = 2.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = "NAMA NASABAH",
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

                                    // Fine white divider matching TableHeader
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .fillMaxHeight()
                                            .background(Color.White.copy(alpha = 0.3f))
                                    )

                                    // Scrollable right headers
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .horizontalScroll(horizontalScrollState),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.width(scrollableContentWidth),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TableCellHeader("No Rek", colRekWidth, TextAlign.Start)
                                            TableCellHeader("CIF", colCifWidth, TextAlign.Center)
                                            TableCellHeader("Tgl Real", colTglWidth, TextAlign.Center)
                                            TableCellHeader("Plafond", colPlafonWidth, TextAlign.End)
                                            TableCellHeader("OS Lalu", colOsLaluWidth, TextAlign.End)
                                            TableCellHeader("Net Disb", colNetDisbursWidth, TextAlign.End)
                                            TableCellHeader("Status", colStatusWidth, TextAlign.Center)
                                        }
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFFFCCB0))

                                // Table Body Rows matching TableRow
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    itemsIndexed(filteredList) { index, item ->
                                        val rowBg = if (index % 2 == 0) Color(0xFFEEF4FC) else Color(0xFFD5E3F5)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(rowBg)
                                                .then(
                                                    if (onCustomerClick != null) {
                                                        Modifier.clickable { onCustomerClick(item.customer) }
                                                    } else Modifier
                                                )
                                                .padding(vertical = 2.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Frozen left: Nama Nasabah
                                            Text(
                                                text = item.customer.namaNasabah.uppercase(),
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .width(frozenColWidth)
                                                    .padding(horizontal = 3.dp),
                                                color = Color(0xFF001C3B)
                                            )

                                            // Fine orange vertical divider matching TableRow
                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .height(18.dp)
                                                    .background(Color(0xFFFFCCB0))
                                            )

                                            // Scrollable columns for this row
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .horizontalScroll(horizontalScrollState),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.width(scrollableContentWidth),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    TableCellText(
                                                        text = item.customer.noRekening,
                                                        width = colRekWidth,
                                                        textAlign = TextAlign.Start,
                                                        color = Color(0xFF001C3B)
                                                    )
                                                    TableCellText(
                                                        text = item.customer.cif,
                                                        width = colCifWidth,
                                                        textAlign = TextAlign.Center,
                                                        color = Color(0xFF001C3B)
                                                    )
                                                    TableCellText(
                                                        text = item.customer.tglReal,
                                                        width = colTglWidth,
                                                        textAlign = TextAlign.Center,
                                                        color = Color(0xFF001C3B)
                                                    )
                                                    TableCellText(
                                                        text = formatDoubleClean(item.plafondBaru, formatter),
                                                        width = colPlafonWidth,
                                                        textAlign = TextAlign.End,
                                                        color = Color(0xFF001C3B),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    TableCellText(
                                                        text = if (item.osBlnLalu > 0) formatDoubleClean(item.osBlnLalu, formatter) else "-",
                                                        width = colOsLaluWidth,
                                                        textAlign = TextAlign.End,
                                                        color = Color(0xFF475569)
                                                    )
                                                    TableCellText(
                                                        text = formatDoubleClean(item.netDisburs, formatter),
                                                        width = colNetDisbursWidth,
                                                        textAlign = TextAlign.End,
                                                        color = if (item.netDisburs >= 0) Color(0xFF166534) else Color(0xFFC62828),
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                    // Status badge
                                                    Box(
                                                        modifier = Modifier
                                                            .width(colStatusWidth)
                                                            .padding(horizontal = 1.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = if (item.isNasabahBaru) Color(0xFFDCFCE7) else Color(0xFFE0F2FE)
                                                        ) {
                                                            Text(
                                                                text = if (item.isNasabahBaru) "Baru" else "Suplesi",
                                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (item.isNasabahBaru) Color(0xFF166534) else Color(0xFF0369A1),
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = Color(0xFFFFCCB0))
                                    }
                                }

                                // Table Footer (Totals) matching CustomerTable
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFF7F2))
                                        .padding(vertical = 5.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TOTAL (${filteredList.size} Deb)",
                                        modifier = Modifier
                                            .width(frozenColWidth)
                                            .padding(horizontal = 3.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = Color(0xFF00386B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(16.dp)
                                            .background(Color(0xFFFFCCB0))
                                    )

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .horizontalScroll(horizontalScrollState),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.width(scrollableContentWidth),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Spacer(modifier = Modifier.width(colRekWidth + colCifWidth + colTglWidth))

                                            TableCellText(
                                                text = formatDoubleClean(sumPlafond, formatter),
                                                width = colPlafonWidth,
                                                textAlign = TextAlign.End,
                                                color = Color(0xFF00386B),
                                                fontWeight = FontWeight.Bold
                                            )
                                            TableCellText(
                                                text = formatDoubleClean(sumOsLalu, formatter),
                                                width = colOsLaluWidth,
                                                textAlign = TextAlign.End,
                                                color = Color(0xFF00386B),
                                                fontWeight = FontWeight.Bold
                                            )
                                            TableCellText(
                                                text = formatDoubleClean(sumNetDisburs, formatter),
                                                width = colNetDisbursWidth,
                                                textAlign = TextAlign.End,
                                                color = if (sumNetDisburs >= 0) Color(0xFF166534) else Color(0xFFC62828),
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Spacer(modifier = Modifier.width(colStatusWidth))
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

@Composable
private fun TableCellHeader(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    textAlign: TextAlign
) {
    Text(
        text = text.uppercase(),
        modifier = Modifier
            .width(width)
            .padding(horizontal = 1.dp),
        textAlign = textAlign,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 7.5.sp,
            letterSpacing = 0.sp,
            color = Color.White.copy(alpha = 0.9f)
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun TableCellText(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    textAlign: TextAlign,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 1.dp),
        textAlign = textAlign,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 8.8.sp,
            fontWeight = fontWeight,
            color = color
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun FilterStatusChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color else color.copy(alpha = 0.1f),
        modifier = Modifier.height(24.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 9.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else color
            )
        }
    }
}
