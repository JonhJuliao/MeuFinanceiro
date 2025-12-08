package com.meufinanceiro.backend.db

import androidx.room.TypeConverter
import com.meufinanceiro.backend.model.TipoTransacao

class Converters {

    @TypeConverter
    fun fromTipoTransacao(tipo: TipoTransacao): String = tipo.name

    @TypeConverter
    fun toTipoTransacao(valor: String): TipoTransacao =
        TipoTransacao.valueOf(valor)
}