package com.meufinanceiro.backend.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.meufinanceiro.backend.dao.CategoriaDao
import com.meufinanceiro.backend.dao.TransacaoDao
import com.meufinanceiro.backend.model.Categoria
import com.meufinanceiro.backend.model.Transacao

@Database(
    entities = [Categoria::class, Transacao::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoriaDao(): CategoriaDao
    abstract fun transacaoDao(): TransacaoDao
}