package com.example.data.local

import android.content.Context
import com.example.ui.viewmodel.KolekSummaryData
import com.example.ui.viewmodel.MantriDetailData
import com.example.ui.viewmodel.UnitSummaryData
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object LocalSummaryCache {

    private const val SUMMARIES_FILE = "dashboard_summaries_cache.json"
    private const val MONTHLY_KOLEK_PREFIX = "lw321_kolek_m"
    private const val MONTHLY_TUNGGAKAN_PREFIX = "lw321_tunggakan_m"

    fun serializeSummaries(
        summaryBulanLalu: UnitSummaryData,
        summaryAkhirBulan: UnitSummaryData,
        summaryDaily: UnitSummaryData,
        kolekSummaryBulanLalu: KolekSummaryData,
        kolekSummaryAkhirBulan: KolekSummaryData,
        kolekSummaryDaily: KolekSummaryData,
        mantriSummaryBulanLalu: Map<String, MantriDetailData>,
        mantriSummaryAkhirBulan: Map<String, MantriDetailData>,
        mantriSummaryDaily: Map<String, MantriDetailData>,
        dashboardMonthlySummary: Map<Int, UnitSummaryData>,
        dashboardMonthlyKolekSummary: Map<Int, KolekSummaryData>,
        dashboardMonthlyMantriSummary: Map<Int, Map<String, MantriDetailData>>
    ): String {
        val root = JSONObject()

        root.put("summaryBulanLalu", unitSummaryToJson(summaryBulanLalu))
        root.put("summaryAkhirBulan", unitSummaryToJson(summaryAkhirBulan))
        root.put("summaryDaily", unitSummaryToJson(summaryDaily))

        root.put("kolekSummaryBulanLalu", kolekSummaryToJson(kolekSummaryBulanLalu))
        root.put("kolekSummaryAkhirBulan", kolekSummaryToJson(kolekSummaryAkhirBulan))
        root.put("kolekSummaryDaily", kolekSummaryToJson(kolekSummaryDaily))

        root.put("mantriSummaryBulanLalu", mantriMapToJson(mantriSummaryBulanLalu))
        root.put("mantriSummaryAkhirBulan", mantriMapToJson(mantriSummaryAkhirBulan))
        root.put("mantriSummaryDaily", mantriMapToJson(mantriSummaryDaily))

        val dMonthlyObj = JSONObject()
        dashboardMonthlySummary.forEach { (m, u) ->
            dMonthlyObj.put(m.toString(), unitSummaryToJson(u))
        }
        root.put("dashboardMonthlySummary", dMonthlyObj)

        val dKolekObj = JSONObject()
        dashboardMonthlyKolekSummary.forEach { (m, k) ->
            dKolekObj.put(m.toString(), kolekSummaryToJson(k))
        }
        root.put("dashboardMonthlyKolekSummary", dKolekObj)

        val dMantriObj = JSONObject()
        dashboardMonthlyMantriSummary.forEach { (m, map) ->
            dMantriObj.put(m.toString(), mantriMapToJson(map))
        }
        root.put("dashboardMonthlyMantriSummary", dMantriObj)

        return root.toString()
    }

    fun deserializeSummaries(jsonText: String): CachedDashboardData? {
        return try {
            if (jsonText.isBlank()) return null
            val root = JSONObject(jsonText)

            val sumBlnLalu = jsonToUnitSummary(root.optJSONObject("summaryBulanLalu"))
            val sumAkhirBln = jsonToUnitSummary(root.optJSONObject("summaryAkhirBulan"))
            val sumDaily = jsonToUnitSummary(root.optJSONObject("summaryDaily"))

            val kolBlnLalu = jsonToKolekSummary(root.optJSONObject("kolekSummaryBulanLalu"))
            val kolAkhirBln = jsonToKolekSummary(root.optJSONObject("kolekSummaryAkhirBulan"))
            val kolDaily = jsonToKolekSummary(root.optJSONObject("kolekSummaryDaily"))

            val mantriBlnLalu = jsonToMantriMap(root.optJSONObject("mantriSummaryBulanLalu"))
            val mantriAkhirBln = jsonToMantriMap(root.optJSONObject("mantriSummaryAkhirBulan"))
            val mantriDaily = jsonToMantriMap(root.optJSONObject("mantriSummaryDaily"))

            val dMonthly = mutableMapOf<Int, UnitSummaryData>()
            root.optJSONObject("dashboardMonthlySummary")?.let { obj ->
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val m = k.toIntOrNull() ?: continue
                    dMonthly[m] = jsonToUnitSummary(obj.optJSONObject(k))
                }
            }

            val dKolek = mutableMapOf<Int, KolekSummaryData>()
            root.optJSONObject("dashboardMonthlyKolekSummary")?.let { obj ->
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val m = k.toIntOrNull() ?: continue
                    dKolek[m] = jsonToKolekSummary(obj.optJSONObject(k))
                }
            }

            val dMantri = mutableMapOf<Int, Map<String, MantriDetailData>>()
            root.optJSONObject("dashboardMonthlyMantriSummary")?.let { obj ->
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val m = k.toIntOrNull() ?: continue
                    dMantri[m] = jsonToMantriMap(obj.optJSONObject(k))
                }
            }

            CachedDashboardData(
                summaryBulanLalu = sumBlnLalu,
                summaryAkhirBulan = sumAkhirBln,
                summaryDaily = sumDaily,
                kolekSummaryBulanLalu = kolBlnLalu,
                kolekSummaryAkhirBulan = kolAkhirBln,
                kolekSummaryDaily = kolDaily,
                mantriSummaryBulanLalu = mantriBlnLalu,
                mantriSummaryAkhirBulan = mantriAkhirBln,
                mantriSummaryDaily = mantriDaily,
                dashboardMonthlySummary = dMonthly,
                dashboardMonthlyKolekSummary = dKolek,
                dashboardMonthlyMantriSummary = dMantri
            )
        } catch (e: Exception) {
            android.util.Log.e("LocalSummaryCache", "Error deserializing summaries cache", e)
            null
        }
    }

    fun saveSummaries(
        context: Context,
        summaryBulanLalu: UnitSummaryData,
        summaryAkhirBulan: UnitSummaryData,
        summaryDaily: UnitSummaryData,
        kolekSummaryBulanLalu: KolekSummaryData,
        kolekSummaryAkhirBulan: KolekSummaryData,
        kolekSummaryDaily: KolekSummaryData,
        mantriSummaryBulanLalu: Map<String, MantriDetailData>,
        mantriSummaryAkhirBulan: Map<String, MantriDetailData>,
        mantriSummaryDaily: Map<String, MantriDetailData>,
        dashboardMonthlySummary: Map<Int, UnitSummaryData>,
        dashboardMonthlyKolekSummary: Map<Int, KolekSummaryData>,
        dashboardMonthlyMantriSummary: Map<Int, Map<String, MantriDetailData>>
    ) {
        try {
            val payload = serializeSummaries(
                summaryBulanLalu = summaryBulanLalu,
                summaryAkhirBulan = summaryAkhirBulan,
                summaryDaily = summaryDaily,
                kolekSummaryBulanLalu = kolekSummaryBulanLalu,
                kolekSummaryAkhirBulan = kolekSummaryAkhirBulan,
                kolekSummaryDaily = kolekSummaryDaily,
                mantriSummaryBulanLalu = mantriSummaryBulanLalu,
                mantriSummaryAkhirBulan = mantriSummaryAkhirBulan,
                mantriSummaryDaily = mantriSummaryDaily,
                dashboardMonthlySummary = dashboardMonthlySummary,
                dashboardMonthlyKolekSummary = dashboardMonthlyKolekSummary,
                dashboardMonthlyMantriSummary = dashboardMonthlyMantriSummary
            )
            val file = File(context.filesDir, SUMMARIES_FILE)
            file.writeText(payload)
        } catch (e: Exception) {
            android.util.Log.e("LocalSummaryCache", "Error saving summaries cache", e)
        }
    }

    data class CachedDashboardData(
        val summaryBulanLalu: UnitSummaryData,
        val summaryAkhirBulan: UnitSummaryData,
        val summaryDaily: UnitSummaryData,
        val kolekSummaryBulanLalu: KolekSummaryData,
        val kolekSummaryAkhirBulan: KolekSummaryData,
        val kolekSummaryDaily: KolekSummaryData,
        val mantriSummaryBulanLalu: Map<String, MantriDetailData>,
        val mantriSummaryAkhirBulan: Map<String, MantriDetailData>,
        val mantriSummaryDaily: Map<String, MantriDetailData>,
        val dashboardMonthlySummary: Map<Int, UnitSummaryData>,
        val dashboardMonthlyKolekSummary: Map<Int, KolekSummaryData>,
        val dashboardMonthlyMantriSummary: Map<Int, Map<String, MantriDetailData>>
    )

    fun loadSummaries(context: Context): CachedDashboardData? {
        try {
            val file = File(context.filesDir, SUMMARIES_FILE)
            if (!file.exists()) return null
            val text = file.readText()
            return deserializeSummaries(text)
        } catch (e: Exception) {
            android.util.Log.e("LocalSummaryCache", "Error loading summaries cache", e)
            return null
        }
    }

    // --- 12-Month Kolek History Cache ---

    fun saveMonthlyKolekMap(context: Context, month: Int, map: Map<String, String>) {
        if (month !in 1..12 || map.isEmpty()) return
        try {
            val file = File(context.filesDir, "${MONTHLY_KOLEK_PREFIX}_$month.tsv")
            val sb = java.lang.StringBuilder()
            map.forEach { (key, value) ->
                sb.append(key).append('\t').append(value).append('\n')
            }
            file.writeText(sb.toString())
        } catch (e: Exception) {
            android.util.Log.e("LocalSummaryCache", "Error saving monthly kolek for month $month", e)
        }
    }

    fun loadMonthlyKolekMap(context: Context, month: Int): Map<String, String>? {
        if (month !in 1..12) return null
        try {
            val file = File(context.filesDir, "${MONTHLY_KOLEK_PREFIX}_$month.tsv")
            if (!file.exists()) return null
            val map = HashMap<String, String>()
            file.forEachLine { line ->
                val tabIdx = line.indexOf('\t')
                if (tabIdx > 0 && tabIdx < line.length - 1) {
                    val k = line.substring(0, tabIdx)
                    val v = line.substring(tabIdx + 1)
                    map[k] = v
                }
            }
            return if (map.isNotEmpty()) map else null
        } catch (e: Exception) {
            android.util.Log.e("LocalSummaryCache", "Error loading monthly kolek for month $month", e)
            return null
        }
    }

    fun loadAllMonthlyKolekMaps(context: Context): Map<Int, Map<String, String>> {
        val result = HashMap<Int, Map<String, String>>()
        for (m in 1..12) {
            val map = loadMonthlyKolekMap(context, m)
            if (map != null && map.isNotEmpty()) {
                result[m] = map
            }
        }
        return result
    }

    // --- 12-Month Tunggakan History Cache ---

    fun saveMonthlyTunggakanMap(context: Context, month: Int, map: Map<String, Double>) {
        if (month !in 1..12 || map.isEmpty()) return
        try {
            val file = File(context.filesDir, "${MONTHLY_TUNGGAKAN_PREFIX}_$month.tsv")
            val sb = java.lang.StringBuilder()
            map.forEach { (key, value) ->
                sb.append(key).append('\t').append(value).append('\n')
            }
            file.writeText(sb.toString())
        } catch (e: Exception) {
            android.util.Log.e("LocalSummaryCache", "Error saving monthly tunggakan for month $month", e)
        }
    }

    fun loadMonthlyTunggakanMap(context: Context, month: Int): Map<String, Double>? {
        if (month !in 1..12) return null
        try {
            val file = File(context.filesDir, "${MONTHLY_TUNGGAKAN_PREFIX}_$month.tsv")
            if (!file.exists()) return null
            val map = HashMap<String, Double>()
            file.forEachLine { line ->
                val tabIdx = line.indexOf('\t')
                if (tabIdx > 0 && tabIdx < line.length - 1) {
                    val k = line.substring(0, tabIdx)
                    val v = line.substring(tabIdx + 1).toDoubleOrNull()
                    if (v != null) {
                        map[k] = v
                    }
                }
            }
            return if (map.isNotEmpty()) map else null
        } catch (e: Exception) {
            android.util.Log.e("LocalSummaryCache", "Error loading monthly tunggakan for month $month", e)
            return null
        }
    }

    fun loadAllMonthlyTunggakanMaps(context: Context): Map<Int, Map<String, Double>> {
        val result = HashMap<Int, Map<String, Double>>()
        for (m in 1..12) {
            val map = loadMonthlyTunggakanMap(context, m)
            if (map != null && map.isNotEmpty()) {
                result[m] = map
            }
        }
        return result
    }

    // --- JSON Helpers ---

    private fun unitSummaryToJson(u: UnitSummaryData): JSONObject {
        return JSONObject().apply {
            put("os", u.os)
            put("debtors", u.debtors)
        }
    }

    private fun jsonToUnitSummary(obj: JSONObject?): UnitSummaryData {
        if (obj == null) return UnitSummaryData()
        return UnitSummaryData(
            os = obj.optDouble("os", 0.0),
            debtors = obj.optInt("debtors", 0)
        )
    }

    private fun kolekSummaryToJson(k: KolekSummaryData): JSONObject {
        return JSONObject().apply {
            put("lancar", unitSummaryToJson(k.lancar))
            put("sml", unitSummaryToJson(k.sml))
            put("npl", unitSummaryToJson(k.npl))
            put("lainnya", unitSummaryToJson(k.lainnya))
        }
    }

    private fun jsonToKolekSummary(obj: JSONObject?): KolekSummaryData {
        if (obj == null) return KolekSummaryData()
        return KolekSummaryData(
            lancar = jsonToUnitSummary(obj.optJSONObject("lancar")),
            sml = jsonToUnitSummary(obj.optJSONObject("sml")),
            npl = jsonToUnitSummary(obj.optJSONObject("npl")),
            lainnya = jsonToUnitSummary(obj.optJSONObject("lainnya"))
        )
    }

    private fun mantriMapToJson(map: Map<String, MantriDetailData>): JSONObject {
        val obj = JSONObject()
        map.forEach { (name, detail) ->
            val dObj = JSONObject().apply {
                put("name", detail.name)
                put("total", unitSummaryToJson(detail.total))
                put("lancar", unitSummaryToJson(detail.lancar))
                put("sml", unitSummaryToJson(detail.sml))
                put("npl", unitSummaryToJson(detail.npl))
            }
            obj.put(name, dObj)
        }
        return obj
    }

    private fun jsonToMantriMap(obj: JSONObject?): Map<String, MantriDetailData> {
        if (obj == null) return emptyMap()
        val result = mutableMapOf<String, MantriDetailData>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val name = keys.next()
            val dObj = obj.optJSONObject(name) ?: continue
            result[name] = MantriDetailData(
                name = dObj.optString("name", name),
                total = jsonToUnitSummary(dObj.optJSONObject("total")),
                lancar = jsonToUnitSummary(dObj.optJSONObject("lancar")),
                sml = jsonToUnitSummary(dObj.optJSONObject("sml")),
                npl = jsonToUnitSummary(dObj.optJSONObject("npl"))
            )
        }
        return result
    }
}
