package com.example.ui.utils

import com.example.data.model.Di319Row
import com.example.data.model.SavingsAccount

object ExcelUtils {

    /**
     * Emulates Excel's XLOOKUP function:
     * Searches a lookupArray for a lookupValue, and returns the corresponding item in returnArray.
     *
     * @param lookupValue The value to search for.
     * @param lookupArray The array or list to search in.
     * @param returnArray The array or list containing the values to return.
     * @param ifNotFound The fallback value if no match is found.
     * @param matchMode 0 for exact match (default), 2 for partial/flexible match (e.g. wildcard or relaxed digits matching).
     * @return The matched item from returnArray, or ifNotFound.
     */
    /**
     * Emulates Excel's XLOOKUP function:
     * Searches a lookupArray for a lookupValue, and returns the corresponding item in returnArray.
     *
     * @param lookupValue The value to search for.
     * @param lookupArray The array or list to search in.
     * @param returnArray The array or list containing the values to return.
     * @param ifNotFound The fallback value if no match is found.
     * @param matchMode 0 for exact match (default), 2 for partial/flexible match (e.g. wildcard or relaxed digits matching/normalization).
     * @return The matched item from returnArray, or ifNotFound.
     */
    fun <T, R> xlookup(
        lookupValue: T,
        lookupArray: List<T>,
        returnArray: List<R>,
        ifNotFound: R? = null,
        matchMode: Int = 0
    ): R? {
        if (lookupArray.isEmpty() || returnArray.isEmpty()) return ifNotFound

        // Exact mode
        if (matchMode == 0) {
            val index = lookupArray.indexOf(lookupValue)
            return if (index != -1) returnArray.getOrNull(index) ?: ifNotFound else ifNotFound
        }

        // Partial/flexible Mode for strings
        if (matchMode == 2 && lookupValue is String) {
            val normValue = normalizeCif(lookupValue)
            if (normValue.isBlank()) return ifNotFound

            for (i in lookupArray.indices) {
                val candidateObj = lookupArray[i] ?: continue
                val candidateStr = candidateObj.toString()
                
                // Try exact match first for performance
                if (candidateStr == lookupValue) return returnArray.getOrNull(i) ?: ifNotFound

                // Then try normalized match
                if (normalizeCif(candidateStr) == normValue) {
                    return returnArray.getOrNull(i) ?: ifNotFound
                }
            }
        }

        return ifNotFound
    }

    /**
     * Emulates Excel's XLOOKUP but gathers *all* matches rather than just the first match.
     * This allows retrieval of multiple savings accounts for a single CIF.
     *
     * @param lookupValue The value to search for (e.g., debtor's CIF).
     * @param lookupArray The column/list to search inside (e.g., list of CIFs from DI319).
     * @param returnArray The column/list to retrieve results from (e.g., list of SavingsAccount).
     * @param matchMode 2 for flexible match (normalized match), 0 for exact match.
     * @return List of matching elements.
     */
    fun <T, R> xlookupAll(
        lookupValue: T,
        lookupArray: List<T>,
        returnArray: List<R>,
        matchMode: Int = 2
    ): List<R> {
        if (lookupArray.isEmpty() || returnArray.isEmpty()) return emptyList()
        
        if (matchMode == 0) {
            return lookupArray.indices.filter { lookupArray[it] == lookupValue }
                .mapNotNull { returnArray.getOrNull(it) }
        }

        val results = mutableListOf<R>()
        val normValue = if (lookupValue is String) normalizeCif(lookupValue) else lookupValue.toString()
        if (normValue.isBlank()) return emptyList()

        for (i in lookupArray.indices) {
            val candidateObj = lookupArray[i] ?: continue
            val candidateStr = candidateObj.toString()
            
            if (candidateStr == lookupValue.toString() || normalizeCif(candidateStr) == normValue) {
                returnArray.getOrNull(i)?.let { results.add(it) }
            }
        }

        return results
    }

    /**
     * Helper to normalize CIFs to a consistent format for comparison, removing prefixes, 
     * non-alphanumeric characters, and leading zeros.
     */
    fun normalizeCif(cif: String): String {
        var str = cif.trim().uppercase()
        // Remove common prefixes
        val prefixes = listOf("CIF", "-", "NO", "ID", ":")
        for (prefix in prefixes) {
            if (str.startsWith(prefix)) {
                str = str.substring(prefix.length).trim()
            }
        }
        
        // Keep only alphanumeric characters (A-Z, 0-9)
        str = str.replace(Regex("[^A-Z0-9]"), "")
        
        // Remove leading zeros
        str = str.dropWhile { it == '0' }
        return str
    }

    /**
     * Helper to lookup all matched records from Di319Row list based on a target CIF.
     */
    fun lookupSavingsForCif(
        targetCif: String,
        rows: List<Di319Row>
    ): List<SavingsAccount> {
        val normTarget = normalizeCif(targetCif)
        if (normTarget.isBlank()) return emptyList()
        return rows.filter { row ->
            val normRow = normalizeCif(row.cif)
            normRow.isNotBlank() && normRow == normTarget
        }.map { SavingsAccount(noRekening = it.noRekening, saldo = it.saldo) }
    }
}
