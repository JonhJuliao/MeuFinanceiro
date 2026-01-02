package com.meufinanceiro.backend.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MetodoPagamento {
    DINHEIRO, CREDITO, DEBITO, PIX
}

@Entity(tableName = "transacoes")
data class Transacao(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val valor: Double,
    val descricao: String?,
    val dataMillis: Long,
    val tipo: TipoTransacao,
    val categoriaId: Long,

    // NOVO CAMPO
    val metodoPagamento: String = MetodoPagamento.DINHEIRO.name
)