package com.example.data.model

data class SavingsAccount(
    val noRekening: String,
    val saldo: Double
)

data class Di319Row(
    val cif: String,
    val noRekening: String,
    val namaNasabah: String,
    val saldo: Double
)

data class SimpananRow(
    val cif: String,
    val namaNasabah: String,
    val noTabungan: String,
    val balance: Double,
    val pinjaman: Double,
    val kode1: String,
    val shortBalance: String,
    val shortCif: String,
    val shortNama: String,
    val shortNoTabungan: String,
    val kode2: String,
    val rumus1: String,
    val rumus2: Double,
    val rumus3: String,
    val rumus4: String
)
