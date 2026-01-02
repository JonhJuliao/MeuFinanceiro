package com.meufinanceiro.backend.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,

    // Verifique se tem essa vírgula acima e o valor padrão abaixo
    val metaMensal: Double = 0.0
)