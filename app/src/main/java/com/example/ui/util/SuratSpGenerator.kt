package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.data.model.CustomerEntity
import com.example.data.model.desa
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SpType(val code: String, val title: String, val perihal: String) {
    SP1("SP1", "Surat Peringatan Pertama", "Surat Peringatan Pertama"),
    SP2("SP2", "Surat Peringatan Kedua", "Surat Peringatan Kedua"),
    SP3("SP3", "Surat Peringatan Ketiga", "Surat Peringatan Ketiga (Terakhir)"),
    PANGGILAN("SPg", "Surat Panggilan", "Surat Panggilan");

    companion object {
        fun fromLabel(label: String): SpType {
            return when (label.uppercase()) {
                "SP1" -> SP1
                "SP2" -> SP2
                "SP3" -> SP3
                else -> PANGGILAN
            }
        }
    }
}

object SuratSpGenerator {

    private val idLocale = Locale("in", "ID")
    private val currencyFormat = NumberFormat.getNumberInstance(idLocale).apply {
        maximumFractionDigits = 0
    }

    private fun formatRupiah(value: Double): String {
        return currencyFormat.format(value.toLong())
    }

    private fun getMonthRoman(monthIndex: Int): String {
        return when (monthIndex + 1) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            5 -> "V"
            6 -> "VI"
            7 -> "VII"
            8 -> "VIII"
            9 -> "IX"
            10 -> "X"
            11 -> "XI"
            12 -> "XII"
            else -> "Okt"
        }
    }

    private fun getMonthShort(monthIndex: Int): String {
        return when (monthIndex + 1) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "Mei"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Agu"
            9 -> "Sep"
            10 -> "Okt"
            11 -> "Nov"
            12 -> "Des"
            else -> "Okt"
        }
    }

    fun getLogoBase64(context: Context): String {
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, com.example.R.drawable.monber8)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    fun generateHtml(
        context: Context,
        customer: CustomerEntity,
        spType: SpType,
        customDate: Date = Date(),
        unitName: String = "KATAPANG",
        branchName: String = "SOREANG",
        city: String = "Kab Bandung",
        kaunitName: String = "Atep Dani Nugraha"
    ): String {
        val cal = Calendar.getInstance().apply { time = customDate }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthStr = SimpleDateFormat("MMMM", idLocale).format(customDate)
        val monthShort = getMonthShort(cal.get(Calendar.MONTH))
        val year = cal.get(Calendar.YEAR)
        val fullDateStr = "$day $monthStr $year"

        val deadlineCal = Calendar.getInstance().apply {
            time = customDate
            val daysToAdd = when (spType) {
                SpType.SP1 -> 13
                SpType.SP2 -> 7
                SpType.SP3 -> 3
                SpType.PANGGILAN -> 3
            }
            add(Calendar.DAY_OF_MONTH, daysToAdd)
        }
        val deadlineDateStr = SimpleDateFormat("d MMMM yyyy", idLocale).format(deadlineCal.time)
        val deadlineDayName = SimpleDateFormat("EEEE", idLocale).format(deadlineCal.time)

        val rek = if (customer.noRekening.isNotBlank()) customer.noRekening else customer.cif
        val rekPrefix = if (rek.length >= 4) rek.take(4) else "3289"
        val noSurat = "316/${spType.code}/$rekPrefix/$monthShort/$year"
        val sphNomor = "$rek/$rekPrefix/${year.toString().takeLast(2)}"

        val plafondFormatted = formatRupiah(customer.plafond)
        val osFormatted = formatRupiah(customer.osPinjaman)
        val tgkPokokFormatted = formatRupiah(if (customer.tunggakanPokok > 0) customer.tunggakanPokok else customer.osPinjaman)
        val tgkBungaFormatted = formatRupiah(customer.tunggakanBunga)
        val penaltyFormatted = formatRupiah(customer.pinalty)
        
        val calcTotal = if (customer.totalTunggakan > 0) {
            customer.totalTunggakan
        } else {
            (if (customer.tunggakanPokok > 0) customer.tunggakanPokok else customer.osPinjaman) + customer.tunggakanBunga + customer.pinalty
        }
        val totalFormatted = formatRupiah(calcTotal)

        val logoBase64 = getLogoBase64(context)
        val logoTag = if (logoBase64.isNotBlank()) {
            "<img src=\"data:image/png;base64,$logoBase64\" style=\"height: 52px; vertical-align: middle;\" alt=\"BRI Logo\" />"
        } else {
            "<div style=\"font-size: 26px; font-weight: bold; color: #005FAF;\">BRI</div>"
        }

        val desaStr = customer.desa.ifBlank { "Sukamukti" }
        val alamatStr = customer.alamat.ifBlank { "Kp Pasung Rt 2 Rw 5" }

        val bodyParagraph2 = when (spType) {
            SpType.SP1 -> {
                "Sehubungan dengan hal tersebut diatas, apabila Saudara tidak memenuhi panggilan tunggakan tersebut selambat - lambatnya tanggal <b>$deadlineDateStr</b> maka untuk selanjutnya BRI akan melakukan langkah hukum berupa penyelesaian melalui saluran hukum sesuai dengan ketentuan yang berlaku."
            }
            SpType.SP2 -> {
                "Sehubungan dengan Surat Peringatan Pertama yang telah kami sampaikan sebelumnya dan Saudara belum menyelesaikan kewajiban tersebut, maka kami sampaikan Surat Peringatan Kedua. Apabila Saudara tidak menyelesaikan tunggakan tersebut selambat - lambatnya tanggal <b>$deadlineDateStr</b>, maka BRI akan melanjutkan proses penyelesaian sesuai ketentuan hukum yang berlaku."
            }
            SpType.SP3 -> {
                "Sehubungan dengan Surat Peringatan sebelumnya dan belum adanya penyelesaian kewajiban kredit Saudara, maka dengan ini kami sampaikan Surat Peringatan Ketiga (Terakhir). Apabila Saudara tidak menyelesaikan kewajiban tersebut selambat - lambatnya tanggal <b>$deadlineDateStr</b>, maka BRI akan segera melakukan tindakan hukum berupa penyerahan penagihan dan eksekusi agunan sesuai ketentuan perundang-undangan yang berlaku."
            }
            SpType.PANGGILAN -> {
                "Sehubungan dengan penyelesaian kewajiban kredit Saudara pada BRI, dengan ini kami mengharapkan kehadiran Saudara di Kantor BRI Unit $unitName pada hari <b>$deadlineDayName</b>, tanggal <b>$deadlineDateStr</b> pukul 09.00 WIB untuk bermusyawarah mencari solusi penyelesaian kewajiban kredit Saudara."
            }
        }

        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>${spType.title} - ${customer.namaNasabah}</title>
    <style>
        @page {
            size: A4;
            margin: 20mm 15mm 20mm 15mm;
        }
        body {
            font-family: Arial, Helvetica, sans-serif;
            font-size: 11pt;
            line-height: 1.35;
            color: #000;
            margin: 0;
            padding: 10px 25px;
        }
        .header-table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 25px;
        }
        .header-table td {
            vertical-align: middle;
        }
        .kop-text {
            text-align: center;
        }
        .kop-title {
            font-size: 14pt;
            font-weight: bold;
            letter-spacing: 0.5px;
            margin: 0;
        }
        .kop-branch {
            font-size: 12pt;
            font-weight: bold;
            margin: 2px 0 0 0;
        }
        .kop-unit {
            font-size: 12pt;
            font-weight: bold;
            margin: 2px 0 0 0;
        }
        .meta-table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 25px;
        }
        .meta-table td {
            vertical-align: top;
            font-size: 11pt;
        }
        .sph-title {
            font-size: 10.5pt;
            margin-top: 15px;
            margin-bottom: 4px;
        }
        .divider {
            border-top: 1.5px solid #000;
            margin-bottom: 15px;
        }
        .content-p {
            font-size: 11pt;
            text-align: justify;
            margin-bottom: 12px;
        }
        .data-table {
            width: 100%;
            border-collapse: collapse;
            margin: 15px 0 18px 0;
            font-size: 10pt;
        }
        .data-table th, .data-table td {
            border: 1px solid #000;
            padding: 6px 4px;
            text-align: center;
            vertical-align: middle;
        }
        .data-table th {
            font-weight: normal;
        }
        .data-table .col-sub {
            font-size: 8.5pt;
        }
        .data-table td.bold {
            font-weight: bold;
        }
        .signature-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 25px;
        }
        .signature-table td {
            vertical-align: top;
            text-align: center;
        }
        .tindasan {
            font-size: 9pt;
            margin-top: 35px;
        }
        .qr-placeholder {
            width: 80px;
            height: 80px;
            margin: 10px auto;
            border: 1px dashed #666;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 8pt;
            color: #444;
        }
    </style>
</head>
<body>
    <!-- KOP SURAT -->
    <table class="header-table">
        <tr>
            <td style="width: 18%; text-align: left;">
                $logoTag
            </td>
            <td class="kop-text" style="width: 82%; padding-right: 18%;">
                <div class="kop-title">PT. BANK RAKYAT INDONESIA (PERSERO) TBK.</div>
                <div class="kop-branch">KC $branchName</div>
                <div class="kop-unit">UNIT $unitName</div>
            </td>
        </tr>
    </table>

    <!-- NOMOR, PERIHAL & TUJUAN -->
    <table class="meta-table">
        <tr>
            <td style="width: 12%;">Nomor</td>
            <td style="width: 3%;">:</td>
            <td style="width: 45%;">$noSurat</td>
            <td style="width: 40%;">$city, &nbsp;$fullDateStr</td>
        </tr>
        <tr>
            <td>Perihal</td>
            <td>:</td>
            <td>${spType.perihal}</td>
            <td>
                Kepada Yth,<br/>
                <b>${customer.namaNasabah.uppercase()}</b><br/>
                $alamatStr<br/>
                $desaStr<br/>
                Kec. $unitName
            </td>
        </tr>
    </table>

    <div class="sph-title">Surat Pengakuan Hutang Nomor $sphNomor</div>
    <div class="divider"></div>

    <div class="content-p">
        Menunjuk Surat Pengakuan Hutang tersebut diatas dengan ini kami sampaikan bahwa berdasarkan catatan administrasi yang ada pada BRI, berikut kami sampaikan kewajiban kredit Saudara posisi tanggal $fullDateStr untuk segera diselesaikan dengan rincian sebagai berikut :
    </div>

    <!-- TABEL RINCIAN -->
    <table class="data-table">
        <thead>
            <tr>
                <th rowspan="2" style="width: 20%;">Nomor Rekening</th>
                <th rowspan="2" style="width: 15%;">Plafond</th>
                <th rowspan="2" style="width: 15%;">Sisa Pokok</th>
                <th colspan="2" style="width: 28%;">Tunggakan</th>
                <th rowspan="2" style="width: 10%;">Penalty</th>
                <th rowspan="2" style="width: 12%;">Total Kewajiban Tunggakan</th>
            </tr>
            <tr>
                <th>Pokok</th>
                <th>Bunga</th>
            </tr>
            <tr class="col-sub">
                <td>(1)</td>
                <td>(2)</td>
                <td>(3)</td>
                <td>(4)</td>
                <td>(5)</td>
                <td>(6)</td>
                <td></td>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td>$rek</td>
                <td>$plafondFormatted</td>
                <td>$osFormatted</td>
                <td>$tgkPokokFormatted</td>
                <td>$tgkBungaFormatted</td>
                <td>$penaltyFormatted</td>
                <td class="bold">$totalFormatted</td>
            </tr>
        </tbody>
    </table>

    <div class="content-p">
        $bodyParagraph2
    </div>

    <div class="content-p">
        Demikian, atas perhatian dan kerjasamanya Saudara kami sampaikan terima kasih.
    </div>

    <!-- TANDA TANGAN -->
    <table class="signature-table">
        <tr>
            <td style="width: 50%;"></td>
            <td style="width: 50%;">
                <b>PT. BANK RAKYAT INDONESIA (Persero) Tbk.</b><br/>
                <b>BRI UNIT $unitName</b><br/>
                
                <!-- QR Code Validasi Resmi -->
                <div style="margin: 10px 0;">
                    <svg width="76" height="76" viewBox="0 0 76 76" style="margin: 0 auto; display: block;">
                        <rect width="76" height="76" fill="white" />
                        <!-- Outer border and corner squares for authentic QR appearance -->
                        <rect x="4" y="4" width="22" height="22" fill="none" stroke="black" stroke-width="3"/>
                        <rect x="9" y="9" width="12" height="12" fill="black"/>
                        <rect x="50" y="4" width="22" height="22" fill="none" stroke="black" stroke-width="3"/>
                        <rect x="55" y="9" width="12" height="12" fill="black"/>
                        <rect x="4" y="50" width="22" height="22" fill="none" stroke="black" stroke-width="3"/>
                        <rect x="9" y="55" width="12" height="12" fill="black"/>
                        <!-- QR Matrix modules -->
                        <rect x="32" y="6" width="4" height="4" fill="black"/>
                        <rect x="40" y="6" width="4" height="4" fill="black"/>
                        <rect x="36" y="14" width="4" height="4" fill="black"/>
                        <rect x="32" y="22" width="8" height="4" fill="black"/>
                        <rect x="8" y="32" width="4" height="8" fill="black"/>
                        <rect x="18" y="36" width="6" height="4" fill="black"/>
                        <rect x="28" y="30" width="6" height="6" fill="black"/>
                        <rect x="40" y="32" width="6" height="6" fill="black"/>
                        <rect x="52" y="30" width="8" height="4" fill="black"/>
                        <rect x="64" y="34" width="6" height="6" fill="black"/>
                        <rect x="32" y="42" width="6" height="6" fill="black"/>
                        <rect x="42" y="44" width="8" height="4" fill="black"/>
                        <rect x="54" y="42" width="4" height="8" fill="black"/>
                        <rect x="62" y="50" width="6" height="4" fill="black"/>
                        <rect x="32" y="54" width="4" height="8" fill="black"/>
                        <rect x="40" y="56" width="6" height="6" fill="black"/>
                        <rect x="50" y="56" width="6" height="4" fill="black"/>
                        <rect x="60" y="62" width="8" height="6" fill="black"/>
                        <rect x="34" y="66" width="8" height="4" fill="black"/>
                        <rect x="46" y="64" width="6" height="6" fill="black"/>
                    </svg>
                </div>

                <u><b>$kaunitName</b></u><br/>
                Kepala Unit
            </td>
        </tr>
    </table>

    <div class="tindasan">
        Tindasan :<br/>
        - Arsip
    </div>
</body>
</html>
        """.trimIndent()
    }

    fun printDocument(context: Context, htmlContent: String, jobTitle: String) {
        try {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    val printAdapter = webView.createPrintDocumentAdapter(jobTitle)
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    printManager?.print(jobTitle, printAdapter, attributes)
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal memulai cetak: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateWhatsAppText(
        customer: CustomerEntity,
        spType: SpType,
        customDate: Date = Date(),
        unitName: String = "KATAPANG",
        branchName: String = "SOREANG",
        kaunitName: String = "Atep Dani Nugraha"
    ): String {
        val cal = Calendar.getInstance().apply { time = customDate }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthStr = SimpleDateFormat("MMMM", idLocale).format(customDate)
        val monthShort = getMonthShort(cal.get(Calendar.MONTH))
        val year = cal.get(Calendar.YEAR)
        val fullDateStr = "$day $monthStr $year"

        val deadlineCal = Calendar.getInstance().apply {
            time = customDate
            val daysToAdd = when (spType) {
                SpType.SP1 -> 13
                SpType.SP2 -> 7
                SpType.SP3 -> 3
                SpType.PANGGILAN -> 3
            }
            add(Calendar.DAY_OF_MONTH, daysToAdd)
        }
        val deadlineDateStr = SimpleDateFormat("d MMMM yyyy", idLocale).format(deadlineCal.time)
        val deadlineDayName = SimpleDateFormat("EEEE", idLocale).format(deadlineCal.time)

        val rek = if (customer.noRekening.isNotBlank()) customer.noRekening else customer.cif
        val rekPrefix = if (rek.length >= 4) rek.take(4) else "3289"
        val noSurat = "316/${spType.code}/$rekPrefix/$monthShort/$year"
        val sphNomor = "$rek/$rekPrefix/${year.toString().takeLast(2)}"

        val plafondFormatted = formatRupiah(customer.plafond)
        val osFormatted = formatRupiah(customer.osPinjaman)
        val tgkPokokFormatted = formatRupiah(if (customer.tunggakanPokok > 0) customer.tunggakanPokok else customer.osPinjaman)
        val tgkBungaFormatted = formatRupiah(customer.tunggakanBunga)
        val penaltyFormatted = formatRupiah(customer.pinalty)

        val calcTotal = if (customer.totalTunggakan > 0) {
            customer.totalTunggakan
        } else {
            (if (customer.tunggakanPokok > 0) customer.tunggakanPokok else customer.osPinjaman) + customer.tunggakanBunga + customer.pinalty
        }
        val totalFormatted = formatRupiah(calcTotal)

        val bodyWarning = when (spType) {
            SpType.SP1 -> "Sehubungan dengan hal tersebut diatas, apabila Saudara tidak menyelesaikan tunggakan tersebut selambat-lambatnya tanggal *$deadlineDateStr*, maka untuk selanjutnya BRI akan melakukan langkah hukum berupa penyelesaian melalui saluran hukum sesuai ketentuan yang berlaku."
            SpType.SP2 -> "Sehubungan dengan Surat Peringatan Pertama yang telah disampaikan dan belum diselesaikan, maka kami sampaikan Surat Peringatan Kedua. Apabila Saudara tidak menyelesaikan tunggakan selambat-lambatnya tanggal *$deadlineDateStr*, maka BRI akan melanjutkan proses penyelesaian hukum."
            SpType.SP3 -> "Sehubungan dengan Surat Peringatan sebelumnya dan belum adanya penyelesaian, maka kami sampaikan Surat Peringatan Ketiga (Terakhir). Apabila belum selesai selambat-lambatnya tanggal *$deadlineDateStr*, BRI segera melakukan penagihan dan eksekusi agunan sesuai ketentuan perundang-undangan."
            SpType.PANGGILAN -> "Kami mengharapkan kehadiran Saudara di Kantor BRI Unit $unitName pada hari *$deadlineDayName*, tanggal *$deadlineDateStr* pukul 09.00 WIB untuk bermusyawarah mencari solusi penyelesaian kewajiban kredit Saudara."
        }

        return """
*PT. BANK RAKYAT INDONESIA (PERSERO) TBK.*
*KC $branchName - UNIT $unitName*
━━━━━━━━━━━━━━━━━━━━━━━━━━
Nomor   : $noSurat
Perihal : *${spType.perihal}*

Kepada Yth.
Bapak/Ibu *${customer.namaNasabah.uppercase()}*
Di Tempat

Menunjuk Surat Pengakuan Hutang Nomor $sphNomor, dengan ini kami sampaikan catatan kewajiban kredit Saudara posisi tanggal $fullDateStr untuk segera diselesaikan dengan rincian sebagai berikut:

• *No. Rekening* : $rek
• *Plafond* : Rp $plafondFormatted
• *Sisa Pokok* : Rp $osFormatted
• *Tunggakan Pokok* : Rp $tgkPokokFormatted
• *Tunggakan Bunga* : Rp $tgkBungaFormatted
• *Penalty* : Rp $penaltyFormatted
──────────────────────────
*TOTAL KEWAJIBAN : Rp $totalFormatted*
──────────────────────────

$bodyWarning

Demikian, atas perhatian dan kerjasamanya Saudara kami sampaikan terima kasih.

*PT. BANK RAKYAT INDONESIA (Persero) Tbk.*
*BRI UNIT $unitName*

*$kaunitName*
Kepala Unit
        """.trimIndent()
    }

    fun sendToWhatsApp(context: Context, targetPhone: String, message: String) {
        try {
            var cleanPhone = targetPhone.replace(Regex("[^0-9]"), "")
            if (cleanPhone.startsWith("8")) {
                cleanPhone = "0" + cleanPhone
            }
            val formattedPhone = if (cleanPhone.startsWith("0")) {
                "62" + cleanPhone.substring(1)
            } else {
                cleanPhone
            }

            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val uri = if (formattedPhone.length >= 7) {
                Uri.parse("whatsapp://send?phone=$formattedPhone&text=$encodedMsg")
            } else {
                Uri.parse("whatsapp://send?text=$encodedMsg")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback to general share intent if direct whatsapp schema fails
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(shareIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Aplikasi WhatsApp tidak ditemukan!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
