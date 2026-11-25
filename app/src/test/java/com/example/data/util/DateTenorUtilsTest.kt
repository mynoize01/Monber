package com.example.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateTenorUtilsTest {

    @Test
    fun `extractMonthFromDate handles all Indonesian and ISO date formats`() {
        assertEquals("01", DateTenorUtils.extractMonthFromDate("15/01/2026"))
        assertEquals("08", DateTenorUtils.extractMonthFromDate("5/8/2026"))
        assertEquals("03", DateTenorUtils.extractMonthFromDate("18-03-2027"))
        assertEquals("11", DateTenorUtils.extractMonthFromDate("2026-11-25"))
        assertEquals("07", DateTenorUtils.extractMonthFromDate("01.07.2028"))
        assertEquals("05", DateTenorUtils.extractMonthFromDate("15/05/26"))
        assertEquals("08", DateTenorUtils.extractMonthFromDate("15 Agu 2026"))
        assertEquals("03", DateTenorUtils.extractMonthFromDate("15-Maret-2027"))
        assertEquals("12", DateTenorUtils.extractMonthFromDate("20-Desember-2026"))
        assertEquals("01", DateTenorUtils.extractMonthFromDate("15/01/2026 00:00:00"))
        assertEquals("03", DateTenorUtils.extractMonthFromDate("46093")) // 2026-03-12
        assertNull(DateTenorUtils.extractMonthFromDate(""))
        assertNull(DateTenorUtils.extractMonthFromDate("-"))
    }

    @Test
    fun `extractYearFromDate extracts valid 4-digit years only`() {
        assertEquals("2026", DateTenorUtils.extractYearFromDate("15/01/2026"))
        assertEquals("2027", DateTenorUtils.extractYearFromDate("2027-08-05"))
        assertEquals("2028", DateTenorUtils.extractYearFromDate("15-05-28"))
        assertEquals("2026", DateTenorUtils.extractYearFromDate("15/01/2026 00:00:00"))
        assertEquals("2029", DateTenorUtils.extractYearFromDate("05-Mei-2029"))
        assertNull(DateTenorUtils.extractYearFromDate("1234"))
        assertNull(DateTenorUtils.extractYearFromDate("-"))
        assertNull(DateTenorUtils.extractYearFromDate(""))
    }

    @Test
    fun `parseTenorMonths and effectiveTenor repair float strings and missing columns`() {
        assertEquals(24, DateTenorUtils.parseTenorMonths("24.0", "01/01/2024", "01/01/2026"))
        assertEquals(36, DateTenorUtils.parseTenorMonths("36 Bulan", "", ""))
        assertEquals(60, DateTenorUtils.parseTenorMonths("", "05/02/2024", "05/02/2029"))

        // Legacy Room cache had tenor=24 or tenor=360 ("36.0"), effectiveTenor repairs via tglReal..tglLunas
        assertEquals(60, DateTenorUtils.effectiveTenor(24, "05/02/2024", "05/02/2029"))
        assertEquals(36, DateTenorUtils.effectiveTenor(360, "18/03/2023", "18/03/2026"))
        assertEquals(12, DateTenorUtils.effectiveTenor(24, "20/11/2023", "20/11/2024"))
    }

    @Test
    fun `elapsed and remaining months work across date formats`() {
        val now = LocalDate.of(2026, 11, 25)
        assertEquals(20, DateTenorUtils.monthsElapsedSince("18-03-2025", now))
        assertEquals(20, DateTenorUtils.monthsElapsedSince("2025-03-18", now))
        assertEquals(20, DateTenorUtils.monthsElapsedSince("18/03/2025 00:00:00", now))

        assertEquals(4, DateTenorUtils.monthsRemainingUntil("18/03/2027", now))
        assertEquals(4, DateTenorUtils.monthsRemainingUntil("2027-03-18", now))
        assertEquals(0, DateTenorUtils.monthsRemainingUntil("18/03/2024", now))
        assertNull(DateTenorUtils.monthsElapsedSince("", now))
        assertNull(DateTenorUtils.monthsRemainingUntil("", now))
    }
}
