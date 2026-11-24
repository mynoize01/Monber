package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong

enum class LoanProductOption(
    val label: String,
    val defaultAnnualRate: String
) {
    KUPEDES("Kupedes", "15"),
    KUPRA("Kupra", "14"),
    KUR("KUR", "6")
}

data class AnnuityCalculationResult(
    val monthlyInstallment: Double,
    val totalPayment: Double,
    val totalInterest: Double,
    val isValid: Boolean
)

fun calculateAnnuityInstallment(
    plafon: Double,
    annualRatePercent: Double,
    tenorMonths: Int
): AnnuityCalculationResult {
    if (plafon <= 0.0 || tenorMonths <= 0 || annualRatePercent < 0.0) {
        return AnnuityCalculationResult(0.0, 0.0, 0.0, false)
    }

    val monthly = if (annualRatePercent == 0.0) {
        plafon / tenorMonths.toDouble()
    } else {
        val monthlyRate = (annualRatePercent / 100.0) / 12.0
        val factor = (1.0 + monthlyRate).pow(tenorMonths.toDouble())
        if (factor.isInfinite() || factor <= 1.0) {
            plafon / tenorMonths.toDouble()
        } else {
            plafon * (monthlyRate * factor) / (factor - 1.0)
        }
    }

    val totalPayment = monthly * tenorMonths.toDouble()
    val totalInterest = (totalPayment - plafon).coerceAtLeast(0.0)
    return AnnuityCalculationResult(
        monthlyInstallment = monthly,
        totalPayment = totalPayment,
        totalInterest = totalInterest,
        isValid = true
    )
}

private class ThousandSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        if (digits.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val sb = StringBuilder()
        val originalToTransformed = IntArray(digits.length + 1)
        var transformedIdx = 0

        for (i in digits.indices) {
            originalToTransformed[i] = transformedIdx
            sb.append(digits[i])
            transformedIdx++
            val remaining = digits.length - 1 - i
            if (remaining > 0 && remaining % 3 == 0) {
                sb.append('.')
                transformedIdx++
            }
        }
        originalToTransformed[digits.length] = transformedIdx

        val formatted = sb.toString()
        val transformedToOriginal = IntArray(formatted.length + 1)
        var origPos = 0
        for (j in formatted.indices) {
            transformedToOriginal[j] = origPos
            if (formatted[j] != '.') {
                origPos++
            }
        }
        transformedToOriginal[formatted.length] = digits.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return originalToTransformed[offset.coerceIn(0, digits.length)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transformedToOriginal[offset.coerceIn(0, formatted.length)]
            }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
fun LoanAnnuityCalculatorCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        LoanAnnuityCalculatorSection(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
fun LoanAnnuityCalculatorSection(
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf(LoanProductOption.KUPEDES) }
    var plafonInput by remember { mutableStateOf("50000000") }
    var bungaInput by remember { mutableStateOf(LoanProductOption.KUPEDES.defaultAnnualRate) }
    var tenorInput by remember { mutableStateOf("24") }

    val indonesiaLocale = remember { Locale.forLanguageTag("id-ID") }
    val rupiahFormatter = remember(indonesiaLocale) {
        NumberFormat.getNumberInstance(indonesiaLocale).apply {
            maximumFractionDigits = 0
        }
    }

    val plafonValue = plafonInput.toDoubleOrNull() ?: 0.0
    val bungaValue = bungaInput.replace(',', '.').toDoubleOrNull() ?: 0.0
    val tenorValue = tenorInput.toIntOrNull() ?: 0

    val result = remember(plafonValue, bungaValue, tenorValue) {
        calculateAnnuityInstallment(
            plafon = plafonValue,
            annualRatePercent = bungaValue,
            tenorMonths = tenorValue
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_calc_toggle")
                    .clickable { isExpanded = !isExpanded },
                shape = RoundedCornerShape(10.dp),
                color = if (isExpanded) Color(0xFF005FAF) else Color(0xFFF1F5F9),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isExpanded) Color(0xFF00386B) else Color(0xFFCBD5E1)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Kalkulator Pinjaman",
                            tint = if (isExpanded) Color.White else Color(0xFF005FAF),
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Kalkulator Pinjaman",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isExpanded) Color.White else Color(0xFF00386B)
                            )
                            Text(
                                text = if (isExpanded) {
                                    "Ketuk untuk menyembunyikan kalkulator"
                                } else {
                                    "Simulasi angsuran Kupedes, Kupra & KUR"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isExpanded) Color.White.copy(alpha = 0.85f) else Color(0xFF64748B)
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Tutup Kalkulator" else "Buka Kalkulator",
                        tint = if (isExpanded) Color.White else Color(0xFF64748B)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Pilihan Produk (Kupedes, Kupra, KUR)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Pilihan Produk",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LoanProductOption.entries.forEach { product ->
                                val isSelected = selectedProduct == product
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFF005FAF) else Color.White)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(0xFF005FAF) else Color(0xFFCBD5E1),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            selectedProduct = product
                                            bungaInput = product.defaultAnnualRate
                                        }
                                        .testTag("loan_product_${product.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = product.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF00386B),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // 2. Plafon Pengajuan
                    OutlinedTextField(
                        value = plafonInput,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(13)
                            plafonInput = digits
                        },
                        label = { Text("Plafon Pengajuan", fontSize = 12.sp) },
                        prefix = { Text("Rp ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                        singleLine = true,
                        visualTransformation = ThousandSeparatorVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF005FAF),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("loan_plafon_input")
                    )

                    // 3. Bunga & Tenor (bersebelahan)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bungaInput,
                            onValueChange = { raw ->
                                val filtered = buildString {
                                    var hasDot = false
                                    for (ch in raw) {
                                        if (ch.isDigit()) {
                                            append(ch)
                                        } else if ((ch == '.' || ch == ',') && !hasDot) {
                                            append('.')
                                            hasDot = true
                                        }
                                    }
                                }.take(6)
                                bungaInput = filtered
                            },
                            label = { Text("Bunga (% / thn)", fontSize = 11.5.sp) },
                            suffix = { Text("%", fontSize = 12.sp, color = Color(0xFF64748B)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF005FAF),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("loan_bunga_input")
                        )

                        OutlinedTextField(
                            value = tenorInput,
                            onValueChange = { raw ->
                                tenorInput = raw.filter { it.isDigit() }.take(3)
                            },
                            label = { Text("Tenor", fontSize = 11.5.sp) },
                            suffix = { Text("Bln", fontSize = 12.sp, color = Color(0xFF64748B)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF005FAF),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("loan_tenor_input")
                        )
                    }

                    // 4. Jumlah Angsuran
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("loan_installment_result"),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEBF5FF),
                        border = BorderStroke(1.dp, Color(0xFF93C5FD))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Jumlah Angsuran (${selectedProduct.label})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E40AF),
                                textAlign = TextAlign.Start
                            )
                            Text(
                                text = if (result.isValid) {
                                    "Rp ${rupiahFormatter.format(result.monthlyInstallment.roundToLong())} / bln"
                                } else {
                                    "Rp 0 / bln"
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF005FAF),
                                textAlign = TextAlign.Start
                            )
                            if (result.isValid) {
                                Text(
                                    text = "Total Bayar: Rp ${rupiahFormatter.format(result.totalPayment.roundToLong())} • Bunga: Rp ${rupiahFormatter.format(result.totalInterest.roundToLong())}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF475569),
                                    textAlign = TextAlign.Start
                                )
                            } else {
                                Text(
                                    text = "Masukkan plafon, bunga, dan tenor untuk menghitung",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }
            }
    }
}
