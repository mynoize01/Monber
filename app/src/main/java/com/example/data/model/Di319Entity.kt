package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "di319_rows",
    indices = [Index(value = ["cif"])]
)
data class Di319Entity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cif: String,
    val noRekening: String,
    val namaNasabah: String,
    val saldo: Double
) {
    fun toDi319Row(): Di319Row = Di319Row(
        cif = cif,
        noRekening = noRekening,
        namaNasabah = namaNasabah,
        saldo = saldo
    )
}

fun Di319Row.toEntity(): Di319Entity = Di319Entity(
    cif = cif,
    noRekening = noRekening,
    namaNasabah = namaNasabah,
    saldo = saldo
)
