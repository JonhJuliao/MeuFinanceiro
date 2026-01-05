package com.meufinanceiro.backend.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.meufinanceiro.backend.dao.CategoriaDao
import com.meufinanceiro.backend.dao.TransacaoDao
import com.meufinanceiro.backend.model.Categoria
import com.meufinanceiro.backend.model.Transacao

// 1. Mudamos a versão para 2
@Database(
    entities = [Categoria::class, Transacao::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoriaDao(): CategoriaDao
    abstract fun transacaoDao(): TransacaoDao

    companion object {
        // 2. Script de Migração
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ATENÇÃO: Aqui usamos "transacoes" (plural e minúsculo)
                // Isso é obrigatório porque seu Transacao.kt tem @Entity(tableName = "transacoes")
                db.execSQL("ALTER TABLE transacoes ADD COLUMN parcelaAtual INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE transacoes ADD COLUMN totalParcelas INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}