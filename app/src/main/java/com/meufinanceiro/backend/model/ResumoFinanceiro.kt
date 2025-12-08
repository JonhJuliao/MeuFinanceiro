package com.meufinanceiro.backend.model

data class ResumoFinanceiro(
    val totalReceitas: Double,
    val totalDespesas: Double,
    val saldo: Double
)