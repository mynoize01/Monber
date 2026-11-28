package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomerEntity
import com.example.data.util.DateTenorUtils
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class CustomerDateField(
    val key: String,
    val label: String,
    val shortLabel: String
) {
    TGL_REAL("TGL_REAL", "Tgl Realisasi", "Real"),
    TGL_LUNAS("TGL_LUNAS", "Tgl Lunas / JT", "Lunas");

    companion object {
        fun fromKey(raw: String): CustomerDateField {
            return entries.firstOrNull { it.key.equals(raw.trim(), ignoreCase = true) } ?: TGL_REAL
        }
    }
}

private val SHORT_MONTHS_ID = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agt", "Sep", "Okt", "Nov", "Des"
)

fun formatShortLocalDate(date: LocalDate): String {
    val mo = SHORT_MONTHS_ID.getOrElse(date.monthValue - 1) { date.monthValue.toString() }
    val yr = String.format(Locale.US, "%02d", date.year % 100)
    return "${date.dayOfMonth} $mo $yr"
}

fun formatDateRangeSummary(start: LocalDate?, end: LocalDate?): String {
    return when {
        start != null && end != null -> {
            if (start == end) formatShortLocalDate(start)
            else "${formatShortLocalDate(start)} - ${formatShortLocalDate(end)}"
        }
        start != null -> "Mulai ${formatShortLocalDate(start)}"
        end != null -> "s.d. ${formatShortLocalDate(end)}"
        else -> "Semua Tanggal"
    }
}

fun resolveCustomerFilterDate(
    customer: CustomerEntity,
    field: CustomerDateField
): LocalDate? {
    val rawDate = when (field) {
        CustomerDateField.TGL_REAL -> {
            customer.tglReal.ifBlank {
                customer.extraColumns.split("||")
                    .firstOrNull { part ->
                        part.startsWith("Tgl Real::", ignoreCase = true) ||
                            part.startsWith("Tgl Realisasi::", ignoreCase = true) ||
                            part.startsWith("TGL_REAL::", ignoreCase = true)
                    }
                    ?.substringAfter("::")
                    ?.trim()
                    ?: ""
            }
        }
        CustomerDateField.TGL_LUNAS -> {
            customer.tglLunas.ifBlank {
                if (customer.menuType.equals("EKSTRAKOMP", ignoreCase = true)) {
                    customer.extraColumns.split("||")
                        .firstOrNull { part -> part.startsWith("Tgl PH::", ignoreCase = true) }
                        ?.substringAfter("::")
                        ?.trim()
                        ?: ""
                } else {
                    ""
                }
            }
        }
    }
    if (rawDate.isBlank()) return null
    return DateTenorUtils.parseLocalDate(rawDate)
}

fun filterCustomersByDateRange(
    customers: List<CustomerEntity>,
    field: CustomerDateField,
    startDate: LocalDate?,
    endDate: LocalDate?
): List<CustomerEntity> {
    if (startDate == null && endDate == null) return customers
    val (normalizedStart, normalizedEnd) = if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
        endDate to startDate
    } else {
        startDate to endDate
    }
    return customers.filter { cust ->
        val custDate = resolveCustomerFilterDate(cust, field) ?: return@filter false
        val afterStart = normalizedStart == null || !custDate.isBefore(normalizedStart)
        val beforeEnd = normalizedEnd == null || !custDate.isAfter(normalizedEnd)
        afterStart && beforeEnd
    }
}

enum class DateRangeQuickPreset(val label: String) {
    THIS_MONTH("Bulan Ini"),
    LAST_30_DAYS("30 Hari"),
    LAST_3_MONTHS("3 Bulan"),
    LAST_6_MONTHS("6 Bulan"),
    THIS_YEAR("Tahun Ini");

    fun resolveBounds(today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> {
        return when (this) {
            THIS_MONTH -> today.withDayOfMonth(1) to today
            LAST_30_DAYS -> today.minusDays(29) to today
            LAST_3_MONTHS -> today.minusMonths(3) to today
            LAST_6_MONTHS -> today.minusMonths(6) to today
            THIS_YEAR -> today.withDayOfYear(1) to today
        }
    }
}

@Composable
fun ActiveDateRangeBanner(
    field: CustomerDateField,
    startDate: LocalDate?,
    endDate: LocalDate?,
    resultCount: Int,
    accentColor: Color = Color(0xFF005FAF),
    onEditClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = startDate != null || endDate != null
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onEditClick)
                .testTag("active_date_range_banner"),
            shape = RoundedCornerShape(12.dp),
            color = accentColor.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Rentang Tanggal Aktif",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${field.label}: ${formatDateRangeSummary(startDate, endDate)}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$resultCount nasabah cocok • Ketuk untuk ubah",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onClearClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("active_date_range_clear")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hapus filter tanggal",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeFilterDialog(
    initialField: CustomerDateField,
    initialStartDate: LocalDate?,
    initialEndDate: LocalDate?,
    accentColor: Color = Color(0xFF005FAF),
    countProvider: (CustomerDateField, LocalDate?, LocalDate?) -> Int,
    onApply: (CustomerDateField, LocalDate?, LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedField by remember(initialField) { mutableStateOf(initialField) }
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = DateTenorUtils.localDateToUtcMillis(initialStartDate),
        initialSelectedEndDateMillis = DateTenorUtils.localDateToUtcMillis(initialEndDate)
    )

    val pickedStart = DateTenorUtils.utcMillisToLocalDate(dateRangePickerState.selectedStartDateMillis)
    val pickedEnd = DateTenorUtils.utcMillisToLocalDate(dateRangePickerState.selectedEndDateMillis)

    val liveMatchCount by produceState(
        initialValue = countProvider(selectedField, pickedStart, pickedEnd),
        selectedField,
        pickedStart,
        pickedEnd
    ) {
        value = withContext(Dispatchers.Default) {
            countProvider(selectedField, pickedStart, pickedEnd)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .testTag("date_range_filter_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Filter Rentang Tanggal",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = accentColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Cocok: $liveMatchCount nasabah",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            if (pickedStart != null || pickedEnd != null) {
                                Text(
                                    text = formatDateRangeSummary(pickedStart, pickedEnd),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("date_range_dialog_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup filter tanggal",
                            tint = Color(0xFF475569)
                        )
                    }
                }

                // Field Selector: Tgl Realisasi vs Tgl Lunas / JT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomerDateField.entries.forEach { field ->
                        val isSelected = selectedField == field
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedField = field }
                                .testTag("date_field_${field.key.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) accentColor else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) accentColor else Color(0xFFE2E8F0)
                            )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                            ) {
                                Text(
                                    text = field.label,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Quick Preset Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DateRangeQuickPreset.entries.forEach { preset ->
                        val (presetStart, presetEnd) = remember(preset) { preset.resolveBounds() }
                        val isPresetSelected = pickedStart == presetStart && pickedEnd == presetEnd
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable {
                                    dateRangePickerState.setSelection(
                                        DateTenorUtils.localDateToUtcMillis(presetStart),
                                        DateTenorUtils.localDateToUtcMillis(presetEnd)
                                    )
                                }
                                .testTag("date_preset_${preset.name.lowercase()}"),
                            shape = RoundedCornerShape(999.dp),
                            color = if (isPresetSelected) accentColor.copy(alpha = 0.14f) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (isPresetSelected) accentColor else Color(0xFFE2E8F0)
                            )
                        ) {
                            Text(
                                text = preset.label,
                                fontSize = 11.sp,
                                fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isPresetSelected) accentColor else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Material 3 DateRangePicker Calendar
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    title = null,
                    headline = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (pickedStart != null || pickedEnd != null) {
                                    formatDateRangeSummary(pickedStart, pickedEnd)
                                } else {
                                    "Pilih tanggal mulai & akhir"
                                },
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (pickedStart != null || pickedEnd != null) {
                                TextButton(
                                    onClick = { dateRangePickerState.setSelection(null, null) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Hapus",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    },
                    showModeToggle = true,
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.White,
                        selectedDayContainerColor = accentColor,
                        selectedDayContentColor = Color.White,
                        todayDateBorderColor = accentColor,
                        todayContentColor = accentColor,
                        dayInSelectionRangeContainerColor = accentColor.copy(alpha = 0.16f),
                        dayInSelectionRangeContentColor = Color(0xFF0F172A)
                    )
                )

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Footer Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onApply(selectedField, null, null)
                        },
                        modifier = Modifier.testTag("date_range_reset_button")
                    ) {
                        Text(
                            text = "Semua Tanggal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "Batal",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                        }
                        Button(
                            onClick = {
                                val normalized = if (pickedStart != null && pickedEnd != null && pickedStart.isAfter(pickedEnd)) {
                                    pickedEnd to pickedStart
                                } else {
                                    pickedStart to (pickedEnd ?: pickedStart)
                                }
                                onApply(selectedField, normalized.first, normalized.second)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("date_range_apply_button")
                        ) {
                            Text(
                                text = "Terapkan",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
