package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val namaNasabah: String,
    val plafond: Double,
    val osPinjaman: Double,
    val kolek: String, // Ambil dari Dpk123 LW
    val namaMantri: String,
    val menuType: String, // "INTRAKOMP" atau "EKSTRAKOMP"
    val alamat: String = "",
    val tunggakanPokok: Double = 0.0,
    val tunggakanBunga: Double = 0.0,
    val pinalty: Double = 0.0,
    val totalTunggakan: Double = 0.0,
    val tglReal: String = "",
    val tglLunas: String = "", // dari tgl JT
    val cif: String = "",
    val kolekBlnLalu: String = "",
    val rate: Double = 12.0,
    val flag: String = "AKTIF",
    val tenor: Int = 24, // jangka waktu dalam bulan
    val segmen: String = "", // Jenis Pinjaman
    val noRekening: String = "",
    val umurTgk: Int = 0,
    val kolByLw: String = "",
    val extraColumns: String = "",
    val pnMantri: String = "",
    val npd: String = "",
    val namaMantriPrakarsa: String = "",
    val updatePokok: Double = 0.0,
    val setorPokok: Double = 0.0
)

val CustomerEntity.desa: String
    get() = extraColumns.split("||")
        .firstOrNull { it.startsWith("Desa::") }
        ?.substringAfter("Desa::")?.trim() ?: ""

fun mapEkstrakompSegmen(raw: String): String {
    val clean = raw.trim()
    val upper = clean.uppercase()
    return when {
        upper == "KUR MIKRO BARU" || upper == "KUR MIKRO" || upper.contains("KUR MIKRO") -> "KUR"
        upper == "KUPEDES RAKYAT" || upper.contains("KUPEDES RAKYAT") -> "Kupra"
        upper == "KREDIT MIKRO - GBT" || upper == "KREDIT MIKRO-GBT" || (upper.contains("KREDIT MIKRO") && upper.contains("GBT")) -> "Briguna"
        upper.startsWith("KUR") -> "KUR"
        upper.startsWith("KUPRA") -> "Kupra"
        upper.startsWith("BRIGUNA") -> "Briguna"
        clean.isNotBlank() -> clean
        else -> "LAINNYA"
    }
}

