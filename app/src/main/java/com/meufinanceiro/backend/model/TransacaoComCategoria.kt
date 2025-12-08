package com.meufinanceiro.backend.model

import androidx.room.Embedded
import androidx.room.Relation

data class TransacaoComCategoria(

    @Embedded
    val transacao: Transacao,

    @Relation(
        parentColumn = "categoria_id",
        entityColumn = "id"
    )
    val categoria: Categoria
)