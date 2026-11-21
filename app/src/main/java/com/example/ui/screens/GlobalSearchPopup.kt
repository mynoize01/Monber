package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.data.model.desa
import java.text.NumberFormat

@Composable
fun GlobalSearchPopup(
    query: String,
    onQueryChange: (String) -> Unit,
    allCustomers: List<CustomerEntity>,
    formatter: NumberFormat,
    onCustomerClick: (CustomerEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showInputField: Boolean = true
) {
    val trimmedQuery = query.trim()
    val searchResults = remember(trimmedQuery, allCustomers) {
        if (trimmedQuery.isBlank()) {
            emptyList()
        } else {
            val terms = trimmedQuery.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
            allCustomers.filter { c ->
                val name = c.namaNasabah.lowercase()
                val rek = c.noRekening.lowercase()
                val cif = c.cif.lowercase()
                val mantri = c.namaMantri.lowercase()
                terms.all { t ->
                    name.contains(t) || rek.contains(t) || cif.contains(t) || mantri.contains(t)
                }
            }.take(30)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        tonalElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            if (showInputField) {
                // Search Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = Color(0xFF001C3B),
                            fontWeight = FontWeight.SemiBold
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
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
                                    contentDescription = "Icon Pencarian",
                                    tint = Color(0xFF005FAF),
                                    modifier = Modifier
                                        .size(15.dp)
                                        .padding(end = 4.dp)
                                )
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = "Cari nama, no. rek, CIF, atau mantri...",
                                            color = Color(0xFF64748B),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    innerTextField()
                                }
                                if (query.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onQueryChange("") },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hapus Pencarian",
                                            tint = Color(0xFF43474E),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Information / Result Header
            if (trimmedQuery.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchResults.isEmpty()) "Tidak ditemukan nasabah" else "Ditemukan ${searchResults.size} nasabah",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                }
            }

            // Results List
            if (trimmedQuery.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ketik nama nasabah, no rekening, CIF, atau nama mantri...",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada nasabah yang cocok dengan \"$trimmedQuery\".",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(searchResults, key = { "${it.menuType}_${it.id}_${it.noRekening}" }) { customer ->
                        GlobalSearchResultCard(
                            customer = customer,
                            formatter = formatter,
                            onClick = {
                                // Detail debitur dibuka, popup TIDAK menghilang
                                onCustomerClick(customer)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalSearchResultCard(
    customer: CustomerEntity,
    formatter: NumberFormat,
    onClick: () -> Unit
) {
    val isIntrakomp = customer.menuType == "INTRAKOMP"
    val containerBg = if (isIntrakomp) Color(0xFFF8FAFC) else Color(0xFFFFFBFB)
    val borderColor = Color(0xFFE2E8F0)
    val nameColor = if (isIntrakomp) Color(0xFF00386B) else Color(0xFF93000A)

    val rekStr = if (customer.noRekening.isNotBlank()) customer.noRekening else customer.cif.ifBlank { "-" }
    val osVal = if (isIntrakomp) customer.osPinjaman else customer.totalTunggakan
    val osLabel = if (isIntrakomp) "OS" else "Total"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .background(containerBg)
            .border(0.7.dp, borderColor, RoundedCornerShape(5.dp))
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 2.5.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            // Row 1: Nama Nasabah (Kiri) + [No Rek & OS • Info Kolek] (Kanan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = customer.namaNasabah.ifBlank { "Tanpa Nama" },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = nameColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))

                // Info No Rek & OS di sebelah kiri badge Kolek
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Rek: $rekStr • $osLabel: Rp ${formatDoubleClean(osVal, formatter)}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isIntrakomp) Color(0xFF00386B) else Color(0xFF93000A)
                    )

                    // Info Kolek: Badge kolektabilitas
                    if (isIntrakomp) {
                        val rawKolek = customer.kolek.trim()
                        val cleanUpper = rawKolek.uppercase()

                        val isNPL = cleanUpper == "3" || cleanUpper == "4" || cleanUpper == "5" ||
                                cleanUpper.contains("KURANG LANCAR") ||
                                cleanUpper.contains("DIRAGUKAN") ||
                                cleanUpper.contains("MACET") ||
                                cleanUpper.contains("NPL") ||
                                cleanUpper.startsWith("KL")

                        val isDPK = !isNPL && (cleanUpper == "2" || cleanUpper.contains("DPK") || cleanUpper.contains("PERHATIAN") || cleanUpper.startsWith("SML"))
                        val isLancar = !isNPL && !isDPK && (cleanUpper == "1" || cleanUpper == "LANCAR")

                        val kolekColor = when {
                            isNPL -> Color(0xFFC62828)
                            isDPK -> Color(0xFFE65100)
                            isLancar -> Color(0xFF2E7D32)
                            else -> Color(0xFFC62828)
                        }
                        val kolekBg = when {
                            isNPL -> Color(0xFFFFEBEE)
                            isDPK -> Color(0xFFFFF3E0)
                            isLancar -> Color(0xFFE8F5E9)
                            else -> Color(0xFFFFEBEE)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(kolekBg)
                                .padding(horizontal = 4.dp, vertical = 0.5.dp)
                        ) {
                            Text(
                                text = if (rawKolek.isNotBlank()) rawKolek else "-",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = kolekColor
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFFFDAD6))
                                .padding(horizontal = 4.dp, vertical = 0.5.dp)
                        ) {
                            Text(
                                text = "DH",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF93000A)
                            )
                        }
                    }
                }
            }

            // Row 2: Info Alamat (Kiri, di bawah nama) + Nama Mantri (Kanan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info Alamat di bawah nama
                val alamatDisplay = when {
                    customer.alamat.isNotBlank() && !customer.alamat.equals("JL. RAYA KATAPANG NO. 12", ignoreCase = true) -> customer.alamat.trim()
                    customer.desa.isNotBlank() -> "Desa ${customer.desa.trim()}"
                    customer.alamat.isNotBlank() -> customer.alamat.trim()
                    else -> "-"
                }
                Text(
                    text = alamatDisplay,
                    fontSize = 9.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Nama Mantri saja
                val mantriName = extractMantriNameOnly(customer.namaMantri)
                Text(
                    text = mantriName,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Ekstraksi nama mantri saja tanpa menampilkan nomor PN.
 * Menangani berbagai format seperti:
 * - "80327110 - HENDRA SAPUTRA" -> "HENDRA SAPUTRA"
 * - "207803 - Nurul Mubin" -> "Nurul Mubin"
 * - "HENDRA SAPUTRA (80327110)" -> "HENDRA SAPUTRA"
 * - "80327110 HENDRA" -> "HENDRA"
 */
private fun extractMantriNameOnly(raw: String): String {
    if (raw.isBlank()) return "-"
    var name = raw.trim()

    // Jika ada tanda pemisah hyphen "-" seperti "PN - NAMA" atau "NAMA - PN"
    if (name.contains("-")) {
        val parts = name.split("-").map { it.trim() }
        val textPart = parts.firstOrNull { part ->
            part.any { it.isLetter() } && !part.all { it.isDigit() || it.isWhitespace() }
        } ?: parts.lastOrNull { part -> part.any { it.isLetter() } }
        if (!textPart.isNullOrBlank()) {
            name = textPart
        }
    }

    // Hilangkan PN dalam tanda kurung misalnya (123456) atau [123456]
    name = name.replace(Regex("\\([0-9\\s-]+\\)"), "")
    name = name.replace(Regex("\\[[0-9\\s-]+\\]"), "")

    // Hilangkan angka PN yang tersisa di dalam string
    name = name.replace(Regex("[0-9]"), "")

    // Bersihkan tanda baca awalan/akhiran
    name = name.replace(Regex("^[- .:/]+"), "").replace(Regex("[- .:/]+$"), "").trim()

    if (name.isBlank() ||
        name.equals("TIDAK TERIDENTIFIKASI", ignoreCase = true) ||
        name.equals("MIKRO", ignoreCase = true) ||
        name.equals("UNIT", ignoreCase = true)
    ) {
        return "-"
    }
    return name
}

