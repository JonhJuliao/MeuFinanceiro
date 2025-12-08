package com.meufinanceiro.backend.model

import androidx.room.*

@Entity(
    tableName = "transacoes",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoria_id"],
            onDelete = ForeignKey.RESTRICT // ou CASCADE, se quiser
        )
    ],
    indices = [Index("categoria_id")]
)
data class Transacao(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val tipo: TipoTransacao,

    val valor: Double,

    // timestamp em millis (mais simples que lidar com LocalDate/Time aqui)
    val dataMillis: Long,

    @ColumnInfo(name = "categoria_id")
    val categoriaId: Long,

    val descricao: String? = null
)