package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.CustomerEntity
import com.example.data.model.desa
import com.example.ui.util.SpType
import com.example.ui.util.SuratSpGenerator
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SpLetterDialog(
    customer: CustomerEntity,
    spType: SpType,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val idLocale = remember { Locale("in", "ID") }
    val currencyFormat = remember {
        NumberFormat.getNumberInstance(idLocale).apply { maximumFractionDigits = 0 }
    }

    val currentDate = remember { Date() }
    val fullDateStr = remember(currentDate) {
        SimpleDateFormat("d MMMM yyyy", idLocale).format(currentDate)
    }

    val deadlineDateStr = remember(currentDate, spType) {
        val cal = Calendar.getInstance().apply {
            time = currentDate
            val days = when (spType) {
                SpType.SP1 -> 13
                SpType.SP2 -> 7
                SpType.SP3 -> 3
                SpType.PANGGILAN -> 3
            }
            add(Calendar.DAY_OF_MONTH, days)
        }
        SimpleDateFormat("d MMMM yyyy", idLocale).format(cal.time)
    }

    // Extract debtor phone number if available in extraColumns
    val phone = remember(customer.extraColumns) {
        val raw = customer.extraColumns.split("||")
            .firstOrNull { it.startsWith("HP::") || it.startsWith("TELP::") || it.startsWith("NO_HP::") }
            ?.substringAfter("::")?.trim() ?: ""
        if (raw.isNotBlank()) raw else ""
    }

    val rek = if (customer.noRekening.isNotBlank()) customer.noRekening else customer.cif
    val calcTotal = if (customer.totalTunggakan > 0) {
        customer.totalTunggakan
    } else {
        (if (customer.tunggakanPokok > 0) customer.tunggakanPokok else customer.osPinjaman) + customer.tunggakanBunga + customer.pinalty
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8FAFC),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Pop-up
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF005FAF).copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFF005FAF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "${spType.title} (${spType.code})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${customer.namaNasabah} • $rek",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))

                // Scrollable Paper Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFCBD5E1), shape = RoundedCornerShape(8.dp))
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Kop Surat Preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.logo2),
                                contentDescription = "BRI Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(40.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "PT. BANK RAKYAT INDONESIA (PERSERO) TBK.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "KC SOREANG",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "UNIT KATAPANG",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.size(40.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Nomor & Kepada Yth
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("Nomor   : 316/${spType.code}/${rek.take(4)}/Okt/2026", fontSize = 9.sp, color = Color.Black)
                                Text("Perihal : ${spType.perihal}", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("Kab Bandung, $fullDateStr", fontSize = 9.sp, color = Color.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Kepada Yth,", fontSize = 9.sp, color = Color.Black, textAlign = TextAlign.End)
                                Text(customer.namaNasabah.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End)
                                Text(customer.alamat.ifBlank { "Kp Pasung Rt 2 Rw 5" }, fontSize = 8.5.sp, color = Color.Black, textAlign = TextAlign.End)
                                Text(customer.desa.ifBlank { "Sukamukti" }, fontSize = 8.5.sp, color = Color.Black, textAlign = TextAlign.End)
                                Text("Kec. Katapang", fontSize = 8.5.sp, color = Color.Black, textAlign = TextAlign.End)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Surat Pengakuan Hutang Nomor $rek/${rek.take(4)}/21",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                        HorizontalDivider(thickness = 1.dp, color = Color.Black, modifier = Modifier.padding(vertical = 4.dp))

                        // Paragraf Pembuka
                        Text(
                            text = "Menunjuk Surat Pengakuan Hutang tersebut diatas dengan ini kami sampaikan bahwa berdasarkan catatan administrasi yang ada pada BRI, berikut kami sampaikan kewajiban kredit Saudara posisi tanggal $fullDateStr untuk segera diselesaikan dengan rincian sebagai berikut :",
                            fontSize = 8.5.sp,
                            lineHeight = 11.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Justify
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Ringkasan Tabel Angka (Card Ringkas)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF94A3B8))
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("No Rekening", fontSize = 8.5.sp, color = Color.DarkGray)
                                    Text(rek, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Plafond Pinjaman", fontSize = 8.5.sp, color = Color.DarkGray)
                                    Text("Rp ${currencyFormat.format(customer.plafond.toLong())}", fontSize = 8.5.sp, color = Color.Black)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Sisa Pokok (OS)", fontSize = 8.5.sp, color = Color.DarkGray)
                                    Text("Rp ${currencyFormat.format(customer.osPinjaman.toLong())}", fontSize = 8.5.sp, color = Color.Black)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tunggakan Pokok", fontSize = 8.5.sp, color = Color.DarkGray)
                                    val tgkPokok = if (customer.tunggakanPokok > 0) customer.tunggakanPokok else customer.osPinjaman
                                    Text("Rp ${currencyFormat.format(tgkPokok.toLong())}", fontSize = 8.5.sp, color = Color.Black)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tunggakan Bunga", fontSize = 8.5.sp, color = Color.DarkGray)
                                    Text("Rp ${currencyFormat.format(customer.tunggakanBunga.toLong())}", fontSize = 8.5.sp, color = Color.Black)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Penalty / Denda", fontSize = 8.5.sp, color = Color.DarkGray)
                                    Text("Rp ${currencyFormat.format(customer.pinalty.toLong())}", fontSize = 8.5.sp, color = Color.Black)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 3.dp), color = Color(0xFFCBD5E1))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Kewajiban", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                                    Text("Rp ${currencyFormat.format(calcTotal.toLong())}", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Paragraf Peringatan
                        val warningText = when (spType) {
                            SpType.SP1 -> "Sehubungan dengan hal tersebut diatas, apabila Saudara tidak memenuhi panggilan tunggakan tersebut selambat - lambatnya tanggal $deadlineDateStr maka untuk selanjutnya BRI akan melakukan langkah hukum berupa penyelesaian melalui saluran hukum sesuai dengan ketentuan yang berlaku."
                            SpType.SP2 -> "Sehubungan dengan Surat Peringatan Pertama yang telah kami sampaikan sebelumnya dan Saudara belum menyelesaikan kewajiban tersebut, maka kami sampaikan Surat Peringatan Kedua. Apabila Saudara tidak menyelesaikan tunggakan tersebut selambat - lambatnya tanggal $deadlineDateStr, maka BRI akan melanjutkan proses penyelesaian sesuai ketentuan hukum yang berlaku."
                            SpType.SP3 -> "Sehubungan dengan Surat Peringatan sebelumnya dan belum adanya penyelesaian kewajiban kredit Saudara, maka dengan ini kami sampaikan Surat Peringatan Ketiga (Terakhir). Apabila Saudara tidak menyelesaikan kewajiban tersebut selambat - lambatnya tanggal $deadlineDateStr, maka BRI akan segera melakukan tindakan hukum dan penyerahan penagihan/eksekusi agunan."
                            SpType.PANGGILAN -> "Sehubungan dengan penyelesaian kewajiban kredit Saudara pada BRI, dengan ini kami mengharapkan kehadiran Saudara di Kantor BRI Unit Katapang selambat - lambatnya tanggal $deadlineDateStr pukul 09.00 WIB untuk bermusyawarah mencari solusi penyelesaian kewajiban kredit Saudara."
                        }
                        Text(
                            text = warningText,
                            fontSize = 8.5.sp,
                            lineHeight = 11.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Justify
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tanda Tangan
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PT. BANK RAKYAT INDONESIA (Persero) Tbk.", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("BRI UNIT KATAPANG", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Spacer(modifier = Modifier.height(28.dp))
                                Text("Atep Dani Nugraha", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline, color = Color.Black)
                                Text("Kepala Unit", fontSize = 8.sp, color = Color.Black)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons: [Simpan PDF] & [Kirim ke WA]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tombol Simpan PDF (PrintManager ke PDF)
                    Button(
                        onClick = {
                            val html = SuratSpGenerator.generateHtml(
                                context = context,
                                customer = customer,
                                spType = spType,
                                customDate = currentDate
                            )
                            val jobName = "${spType.code}_${customer.namaNasabah.replace(' ', '_')}"
                            SuratSpGenerator.printDocument(context, html, jobName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Simpan PDF",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simpan PDF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Tombol Kirim ke WA
                    Button(
                        onClick = {
                            val message = SuratSpGenerator.generateWhatsAppText(
                                customer = customer,
                                spType = spType,
                                customDate = currentDate
                            )
                            SuratSpGenerator.sendToWhatsApp(context, phone, message)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_whatsapp),
                            contentDescription = "Kirim WA",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Kirim ke WA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
