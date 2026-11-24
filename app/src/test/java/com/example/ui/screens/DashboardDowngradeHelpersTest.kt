package com.example.ui.screens

import com.example.data.network.SpreadsheetService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class DashboardDowngradeHelpersTest {

    @Test
    fun isNplKolek_matchesAllNplCategoriesOnly() {
        assertTrue(isNplKolek("KL"))
        assertTrue(isNplKolek("Kurang Lancar"))
        assertTrue(isNplKolek("3"))
        assertTrue(isNplKolek("D"))
        assertTrue(isNplKolek("Diragukan"))
        assertTrue(isNplKolek("4"))
        assertTrue(isNplKolek("M"))
        assertTrue(isNplKolek("Macet"))
        assertTrue(isNplKolek("5"))
        assertTrue(isNplKolek("NPL"))

        assertFalse(isNplKolek("L"))
        assertFalse(isNplKolek("Lancar"))
        assertFalse(isNplKolek("1"))
        assertFalse(isNplKolek("SML1"))
        assertFalse(isNplKolek("SML2"))
        assertFalse(isNplKolek("SML3"))
        assertFalse(isNplKolek("DPK"))
        assertFalse(isNplKolek(""))
    }

    @Test
    fun isDateInYearMonth_handlesMultipleDateFormats() {
        val target = YearMonth.of(2026, 11)

        assertTrue(isDateInYearMonth("24/11/2026", target))
        assertTrue(isDateInYearMonth("2026-11-05", target))
        assertTrue(isDateInYearMonth("11/24/2026", target))
        assertTrue(isDateInYearMonth("05-Nov-2026", target))
        assertTrue(isDateInYearMonth("12 Nopember 2026", target))
        assertTrue(isDateInYearMonth("15/11/26", target))
        assertTrue(isDateInYearMonth("11/2026", target))
        assertTrue(isDateInYearMonth("2026-11", target))

        assertFalse(isDateInYearMonth("24/10/2026", target))
        assertFalse(isDateInYearMonth("24/11/2025", target))
        assertFalse(isDateInYearMonth("", target))
        assertFalse(isDateInYearMonth("-", target))
    }

    @Test
    fun fetchLw325DailyCustomers_extractsTglPhAndFiltersByCurrentMonth() {
        val headers = MutableList(39) { "COL_$it" }
        headers[2] = "CIF"
        headers[3] = "NO REKENING"
        headers[7] = "NAMA DEBITUR"
        headers[11] = "DESKRIPSI SEGMEN"
        headers[13] = "TGL PH"
        headers[17] = "SISA AWAL PH BUNGA"
        headers[18] = "BESAR REALISASI"
        headers[27] = "SISA AKHIR PH POKOK"
        headers[29] = "ALIH TAGIH"
        headers[38] = "MANTRI"

        val rowCurrentMonth = MutableList(39) { "" }.apply {
            this[2] = "CIF001"
            this[3] = "123401000001501"
            this[7] = "BUDI SANTOSO"
            this[11] = "KUR"
            this[13] = "15/11/2026"
            this[17] = "500000"
            this[18] = "25000000"
            this[27] = "12000000"
            this[29] = "0"
            this[38] = "207803 - Nurul Mubin"
        }

        val rowPreviousMonth = MutableList(39) { "" }.apply {
            this[2] = "CIF002"
            this[3] = "123401000002501"
            this[7] = "SITI AMINAH"
            this[11] = "KUPEDES"
            this[13] = "15/10/2026"
            this[17] = "300000"
            this[18] = "15000000"
            this[27] = "8000000"
            this[29] = "0"
            this[38] = "207803 - Nurul Mubin"
        }

        val csv = listOf(headers, rowCurrentMonth, rowPreviousMonth)
            .joinToString("\n") { it.joinToString(",") }

        val customers = SpreadsheetService.fetchLw325DailyCustomers(
            url = null,
            offlineContent = csv
        )
        assertEquals(2, customers.size)

        val targetMonth = YearMonth.of(2026, 11)
        val dhNew = customers.filter {
            isDateInYearMonth(extractTglPhFromExtraColumns(it.extraColumns), targetMonth)
        }
        assertEquals(1, dhNew.size)
        assertEquals("BUDI SANTOSO", dhNew.first().namaNasabah)
        assertEquals(12000000.0, dhNew.first().osPinjaman, 0.01)
        assertEquals("15/11/2026", extractTglPhFromExtraColumns(dhNew.first().extraColumns))
    }
}
