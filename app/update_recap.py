import re

with open('app/src/main/java/com/example/ui/screens/DashboardUnitView.kt', 'r') as f:
    text = f.read()

# 1. Update recapUpgradeItems calculation
old_recap = '''                                // Rekapan Total Perpindahan Kolek Upgrade (Dinamis: yg 0 tidak ditampilkan)
                                // - Bergeser ke Lancar: semua nasabah yg bergeser menjadi kolek lancar (berakhir dengan ➔ L)
                                // - Bergeser ke SML: dari NPL(KL,D,M) ke SML, dari SML3 ke 2 atau ke 1, dari sml2 ke 1
                                // - NPL Membaik: semua nasabah NPL (KL, D, M) yg bergeser jadi membaik
                                val recapUpgradeItems = remember(upgradeData) {
                                    val list = mutableListOf<Triple<String, Double, List<CustomerEntity>>>()

                                    // Bergeser ke Lancar (berakhir dengan ➔ L)
                                    val keL = upgradeData.filterKeys { it.trim().uppercase().endsWith("➔ L") }.values.flatten().distinctBy { it.id }
                                    if (keL.isNotEmpty()) {
                                        list.add(Triple("Bergeser ke Lancar", keL.sumOf { it.osPinjaman }, keL))
                                    }

                                    // Bergeser ke SML: 
                                    // 1. Dari NPL (KL, D, M) ke SML (SML1, SML2, SML3)
                                    // 2. Dari SML3 ke SML2 atau SML1
                                    // 3. Dari SML2 ke SML1
                                    val keSmlList = mutableListOf<CustomerEntity>()
                                    upgradeData.forEach { (k, custList) ->
                                        val up = k.trim().uppercase()
                                        val parts = up.split(Regex("\\\\s*➔\\\\s*"))
                                        if (parts.size == 2) {
                                            val fromK = parts[0]
                                            val toK = parts[1]
                                            val isFromNpl = fromK in listOf("KL", "D", "M")
                                            val isToSml = toK.startsWith("SML")
                                            val isSml3ToHigher = (fromK == "SML3" && (toK == "SML2" || toK == "SML1"))
                                            val isSml2To1 = (fromK == "SML2" && toK == "SML1")
                                            if ((isFromNpl && isToSml) || isSml3ToHigher || isSml2To1) {
                                                keSmlList.addAll(custList)
                                            }
                                        }
                                    }
                                    val distinctKeSml = keSmlList.distinctBy { it.id }
                                    if (distinctKeSml.isNotEmpty()) {
                                        list.add(Triple("Bergeser ke SML", distinctKeSml.sumOf { it.osPinjaman }, distinctKeSml))
                                    }

                                    // NPL Membaik: semua nasabah NPL (KL, D, M) yg bergeser jadi membaik
                                    val nplMembaikList = mutableListOf<CustomerEntity>()
                                    upgradeData.forEach { (k, custList) ->
                                        val up = k.trim().uppercase()
                                        val parts = up.split(Regex("\\\\s*➔\\\\s*"))
                                        if (parts.size == 2) {
                                            val fromK = parts[0]
                                            val toK = parts[1]
                                            val isFromNpl = fromK in listOf("KL", "D", "M")
                                            val isToBetterNplOrSmlOrL = toK in listOf("KL", "D", "SML1", "SML2", "SML3", "L") &&
                                                parseKolekCodeAndRank(toK).second < parseKolekCodeAndRank(fromK).second
                                            if (isFromNpl && isToBetterNplOrSmlOrL) {
                                                nplMembaikList.addAll(custList)
                                            }
                                        }
                                    }
                                    val distinctNplMembaik = nplMembaikList.distinctBy { it.id }
                                    if (distinctNplMembaik.isNotEmpty()) {
                                        list.add(Triple("NPL Membaik", distinctNplMembaik.sumOf { it.osPinjaman }, distinctNplMembaik))
                                    }

                                    list
                                }'''

new_recap = '''                                // Rekapan Total Perpindahan Kolek Upgrade: jumlahkan semua yg ada pada info upgrade sesuai kategorinya (Ke Lancar, Ke SML)
                                val recapUpgradeItems = remember(upgradeData) {
                                    val list = mutableListOf<Triple<String, Double, List<CustomerEntity>>>()

                                    // 1. Ke Lancar: semua transisi upgrade yang berujung pada Lancar (mengandung atau berakhiran ➔ L)
                                    val keLList = mutableListOf<CustomerEntity>()
                                    upgradeData.forEach { (k, custList) ->
                                        val up = k.trim().uppercase()
                                        if (up.endsWith("➔ L") || up.contains(" ➔ L")) {
                                            keLList.addAll(custList)
                                        }
                                    }
                                    val distinctKeL = keLList.distinctBy { it.id }
                                    if (distinctKeL.isNotEmpty()) {
                                        list.add(Triple("Ke Lancar", distinctKeL.sumOf { it.osPinjaman }, distinctKeL))
                                    }

                                    // 2. Ke SML: semua transisi upgrade yang berujung pada SML (SML1, SML2, SML3) dari manapun asalnya
                                    val keSmlList = mutableListOf<CustomerEntity>()
                                    upgradeData.forEach { (k, custList) ->
                                        val up = k.trim().uppercase()
                                        if (up.contains("➔ SML") || up.contains("➔ SML1") || up.contains("➔ SML2") || up.contains("➔ SML3")) {
                                            keSmlList.addAll(custList)
                                        }
                                    }
                                    val distinctKeSml = keSmlList.distinctBy { it.id }
                                    if (distinctKeSml.isNotEmpty()) {
                                        list.add(Triple("Ke SML", distinctKeSml.sumOf { it.osPinjaman }, distinctKeSml))
                                    }

                                    list
                                }'''

assert old_recap in text, 'old_recap not found'
text = text.replace(old_recap, new_recap, 1)

# 2. Update MantriTableRow to include debtor count info (e.g. 560.998.000 | 56) and tighter padding (1.5.dp)
old_row = '''@Composable
fun MantriTableRow(
    dateText: String,
    lancar: Double,
    sml: Double,
    npl: Double,
    formatter: NumberFormat,
    isHighlighted: Boolean,
    isEven: Boolean
) {
    val bg = if (isHighlighted) Color(0xFFE0F2FE) else if (isEven) Color(0xFFF8FAFC) else Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(vertical = 3.5.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateText,
            modifier = Modifier.weight(1f),
            fontSize = 9.5.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlighted) Color(0xFF005FAF) else Color(0xFF1E293B)
        )
        Text(
            text = formatDouble(lancar, formatter),
            modifier = Modifier.weight(1f),
            fontSize = 9.sp,
            textAlign = TextAlign.End,
            color = Color(0xFF2E7D32),
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = formatDouble(sml, formatter),
            modifier = Modifier.weight(1f),
            fontSize = 9.sp,
            textAlign = TextAlign.End,
            color = Color(0xFFD97706),
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = formatDouble(npl, formatter),
            modifier = Modifier.weight(1f),
            fontSize = 9.sp,
            textAlign = TextAlign.End,
            color = Color(0xFFC62828),
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
        )
    }
}'''

new_row = '''@Composable
fun MantriTableRow(
    dateText: String,
    lancar: Double,
    lancarDeb: Int = 0,
    sml: Double,
    smlDeb: Int = 0,
    npl: Double,
    nplDeb: Int = 0,
    formatter: NumberFormat,
    isHighlighted: Boolean,
    isEven: Boolean
) {
    val bg = if (isHighlighted) Color(0xFFE0F2FE) else if (isEven) Color(0xFFF8FAFC) else Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(vertical = 1.5.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateText,
            modifier = Modifier.weight(1f),
            fontSize = 9.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlighted) Color(0xFF005FAF) else Color(0xFF1E293B)
        )
        Text(
            text = "${formatDouble(lancar, formatter)} | $lancarDeb",
            modifier = Modifier.weight(1f),
            fontSize = 8.5.sp,
            textAlign = TextAlign.End,
            color = Color(0xFF2E7D32),
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "${formatDouble(sml, formatter)} | $smlDeb",
            modifier = Modifier.weight(1f),
            fontSize = 8.5.sp,
            textAlign = TextAlign.End,
            color = Color(0xFFD97706),
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "${formatDouble(npl, formatter)} | $nplDeb",
            modifier = Modifier.weight(1f),
            fontSize = 8.5.sp,
            textAlign = TextAlign.End,
            color = Color(0xFFC62828),
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
        )
    }
}'''

assert old_row in text, 'old_row not found'
text = text.replace(old_row, new_row, 1)

# 3. Update MantriTableRow calls to pass debtor counts
old_calls = '''                            // Row 1: Bulan Lalu
                            MantriTableRow(
                                dateText = dateBlnLalu,
                                lancar = bln.lancar.os,
                                sml = bln.sml.os,
                                npl = bln.npl.os,
                                formatter = formatter,
                                isHighlighted = activePeriodSource == "BULAN_LALU",
                                isEven = false
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                            // Row 2: Akhir Bulan
                            MantriTableRow(
                                dateText = dateAkhirBulan,
                                lancar = akhir.lancar.os,
                                sml = akhir.sml.os,
                                npl = akhir.npl.os,
                                formatter = formatter,
                                isHighlighted = activePeriodSource == "AKHIR_BULAN",
                                isEven = true
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                            // Row 3: Daily Info
                            MantriTableRow(
                                dateText = dateDaily,
                                lancar = daily.lancar.os,
                                sml = daily.sml.os,
                                npl = daily.npl.os,
                                formatter = formatter,
                                isHighlighted = activePeriodSource == "DAILY",
                                isEven = false
                            )'''

new_calls = '''                            // Row 1: Bulan Lalu
                            MantriTableRow(
                                dateText = dateBlnLalu,
                                lancar = bln.lancar.os,
                                lancarDeb = bln.lancar.debtors,
                                sml = bln.sml.os,
                                smlDeb = bln.sml.debtors,
                                npl = bln.npl.os,
                                nplDeb = bln.npl.debtors,
                                formatter = formatter,
                                isHighlighted = activePeriodSource == "BULAN_LALU",
                                isEven = false
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                            // Row 2: Akhir Bulan
                            MantriTableRow(
                                dateText = dateAkhirBulan,
                                lancar = akhir.lancar.os,
                                lancarDeb = akhir.lancar.debtors,
                                sml = akhir.sml.os,
                                smlDeb = akhir.sml.debtors,
                                npl = akhir.npl.os,
                                nplDeb = akhir.npl.debtors,
                                formatter = formatter,
                                isHighlighted = activePeriodSource == "AKHIR_BULAN",
                                isEven = true
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                            // Row 3: Daily Info
                            MantriTableRow(
                                dateText = dateDaily,
                                lancar = daily.lancar.os,
                                lancarDeb = daily.lancar.debtors,
                                sml = daily.sml.os,
                                smlDeb = daily.sml.debtors,
                                npl = daily.npl.os,
                                nplDeb = daily.npl.debtors,
                                formatter = formatter,
                                isHighlighted = activePeriodSource == "DAILY",
                                isEven = false
                            )'''

assert old_calls in text, 'old_calls not found'
text = text.replace(old_calls, new_calls, 1)

with open('app/src/main/java/com/example/ui/screens/DashboardUnitView.kt', 'w') as f:
    f.write(text)

print('DashboardUnitView.kt updated successfully with Ke Lancar / Ke SML recap and compact debtor counts!')
