package com.example.data.util

import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object DateTenorUtils {

    private val monthNameRules: List<Pair<List<String>, Int>> = listOf(
        listOf("januari", "january", "jan") to 1,
        listOf("februari", "february", "pebruari", "feb", "peb") to 2,
        listOf("maret", "march", "mar") to 3,
        listOf("april", "apr") to 4,
        listOf("mei", "may") to 5,
        listOf("juni", "june", "jun") to 6,
        listOf("juli", "july", "jul") to 7,
        listOf("agustus", "august", "agu", "ags", "aug") to 8,
        listOf("september", "sept", "sep") to 9,
        listOf("oktober", "october", "okt", "oct") to 10,
        listOf("november", "nov", "nop") to 11,
        listOf("desember", "december", "des", "dec") to 12
    )

    private fun normalizeTwoDigitYear(rawYear: Int): Int {
        return if (rawYear <= 79) 2000 + rawYear else 1900 + rawYear
    }

    /**
     * Parses common spreadsheet date formats into (year, month 1..12, day 1..31).
     * Supports dd/MM/yyyy, d/M/yyyy, dd-MM-yyyy, yyyy-MM-dd, dd.MM.yyyy,
     * dd/MM/yy, text months (e.g. "15 Jan 2026", "15-Mei-26"), timestamps,
     * and numeric Excel serial dates.
     */
    fun parseYearMonthDay(dateStr: String): Triple<Int, Int, Int>? {
        val clean = dateStr.trim()
        if (clean.isBlank() || clean == "-" || clean.equals("N/A", ignoreCase = true) || clean.equals("NULL", ignoreCase = true)) {
            return null
        }

        // 1. Numeric Excel serial date (days since 1899-12-30)
        if (clean.all { it.isDigit() || it == '.' || it == ',' } && !clean.contains('/') && !clean.contains('-')) {
            val serial = clean.replace(',', '.').toDoubleOrNull()
            if (serial != null && serial in 20000.0..75000.0) {
                return try {
                    val d = LocalDate.of(1899, 12, 30).plusDays(serial.toLong())
                    Triple(d.year, d.monthValue, d.dayOfMonth)
                } catch (_: Exception) {
                    null
                }
            }
        }

        // 2. Named month tokens (e.g. "15 Jan 2026", "15-Agu-26", "2026 Mei 15")
        val wordTokens = clean.lowercase(Locale.US)
            .split('/', '-', '.', ',', ' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        var matchedNamedMonth: Int? = null
        var matchedMonthTokenIdx = -1
        for (i in wordTokens.indices) {
            val tok = wordTokens[i]
            if (tok.any { it.isLetter() }) {
                val found = monthNameRules.firstOrNull { (aliases, _) ->
                    aliases.any { alias -> tok == alias }
                }?.second
                if (found != null) {
                    matchedNamedMonth = found
                    matchedMonthTokenIdx = i
                    break
                }
            }
        }

        if (matchedNamedMonth != null) {
            val numericTokens = wordTokens.mapIndexedNotNull { idx, tok ->
                if (idx == matchedMonthTokenIdx) null else tok.toIntOrNull()?.let { idx to tok to it }
            }
            val yearCandidate = numericTokens.firstOrNull { (_, num) ->
                num in 1900..2099
            } ?: numericTokens.lastOrNull { (pair, num) ->
                pair.first > matchedMonthTokenIdx && pair.second.length == 2 && num in 0..99
            }
            if (yearCandidate != null) {
                val rawY = yearCandidate.second
                val year = if (yearCandidate.first.second.length == 2) normalizeTwoDigitYear(rawY) else rawY
                val day = numericTokens.firstOrNull { (pair, num) ->
                    pair.first != yearCandidate.first.first && num in 1..31
                }?.second ?: 1
                if (year in 1900..2099) {
                    return Triple(year, matchedNamedMonth, day.coerceIn(1, 31))
                }
            }
            return null
        }

        // 3. Numeric date strings (strip optional time part after space or 'T')
        val dateOnly = clean.split(' ', 'T').firstOrNull()?.trim().orEmpty()
        if (dateOnly.isBlank()) return null

        val parts = dateOnly.split('/', '-', '.')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (parts.size >= 3) {
            val p0 = parts[0]
            val p1 = parts[1]
            val p2 = parts[2]
            if (p0.length == 4 && p0.all { it.isDigit() }) {
                // yyyy-MM-dd
                val y = p0.toIntOrNull() ?: return null
                val m = p1.toIntOrNull() ?: return null
                val d = p2.toIntOrNull() ?: 1
                if (y in 1900..2099 && m in 1..12 && d in 1..31) {
                    return Triple(y, m, d)
                }
            } else if (p2.all { it.isDigit() } && (p2.length == 4 || p2.length == 2)) {
                // dd/MM/yyyy, d/M/yyyy, dd/MM/yy (or MM/dd/yyyy when middle > 12)
                val rawY = p2.toIntOrNull() ?: return null
                val y = if (p2.length == 2) normalizeTwoDigitYear(rawY) else rawY
                val n0 = p0.toIntOrNull() ?: return null
                val n1 = p1.toIntOrNull() ?: return null
                val (day, month) = if (n1 > 12 && n0 in 1..12) n1 to n0 else n0 to n1
                if (y in 1900..2099 && month in 1..12 && day in 1..31) {
                    return Triple(y, month, day)
                }
            }
        } else if (parts.size == 2) {
            val p0 = parts[0]
            val p1 = parts[1]
            if (p0.length == 4 && p0.all { it.isDigit() }) {
                val y = p0.toIntOrNull()
                val m = p1.toIntOrNull()
                if (y != null && y in 1900..2099 && m != null && m in 1..12) {
                    return Triple(y, m, 1)
                }
            } else if (p1.length == 4 && p1.all { it.isDigit() }) {
                val m = p0.toIntOrNull()
                val y = p1.toIntOrNull()
                if (y != null && y in 1900..2099 && m != null && m in 1..12) {
                    return Triple(y, m, 1)
                }
            }
        }

        return null
    }

    fun extractMonthFromDate(dateStr: String): String? {
        val parsed = parseYearMonthDay(dateStr) ?: return null
        return String.format(Locale.US, "%02d", parsed.second)
    }

    fun extractYearFromDate(dateStr: String): String? {
        val parsed = parseYearMonthDay(dateStr) ?: return null
        return parsed.first.toString()
    }

    fun monthsElapsedSince(tglReal: String, now: LocalDate = LocalDate.now()): Int? {
        val (y, m, _) = parseYearMonthDay(tglReal) ?: return null
        val elapsed = (now.year - y) * 12 + (now.monthValue - m)
        return elapsed.coerceAtLeast(0)
    }

    fun monthsRemainingUntil(tglLunas: String, now: LocalDate = LocalDate.now()): Int? {
        val (y, m, _) = parseYearMonthDay(tglLunas) ?: return null
        val remaining = (y - now.year) * 12 + (m - now.monthValue)
        return remaining.coerceAtLeast(0)
    }

    fun monthsBetween(startDateStr: String, endDateStr: String): Int? {
        val (y1, m1, _) = parseYearMonthDay(startDateStr) ?: return null
        val (y2, m2, _) = parseYearMonthDay(endDateStr) ?: return null
        val diff = (y2 - y1) * 12 + (m2 - m1)
        return if (diff >= 0) diff else null
    }

    /**
     * Parses a raw tenor cell (e.g., "36", "36.0", "24 Bulan") cleanly without turning "24.0" into 240.
     * Falls back to monthsBetween(tglReal, tglLunas) if the cell is missing or invalid, then 24.
     */
    fun parseTenorMonths(tenorStr: String, tglReal: String, tglLunas: String): Int {
        val clean = tenorStr.trim()
        if (clean.isNotBlank()) {
            val numberToken = Regex("""\d+(?:[.,]\d+)?""").find(clean)?.value
            if (numberToken != null) {
                val parsed = numberToken.replace(',', '.').toDoubleOrNull()?.roundToInt()
                if (parsed != null && parsed in 1..360) {
                    return parsed
                }
            }
        }
        val span = monthsBetween(tglReal, tglLunas)
        if (span != null && span in 1..360) {
            return span
        }
        return 24
    }

    /**
     * Resolves effective loan tenor in months, automatically correcting legacy cached rows
     * where tenor defaulted to 24 or "24.0" became 240 when tglReal & tglLunas show the true span.
     */
    fun effectiveTenor(rawTenor: Int, tglReal: String, tglLunas: String): Int? {
        val span = monthsBetween(tglReal, tglLunas)?.takeIf { it in 1..360 }
        return when {
            span != null && (rawTenor !in 1..240 || abs(span - rawTenor) >= 3) -> span
            rawTenor in 1..240 -> rawTenor
            span != null -> span
            else -> null
        }
    }
}
