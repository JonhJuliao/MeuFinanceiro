package com.meufinanceiro.backend.dao

import androidx.room.*
import com.meufinanceiro.backend.model.Categoria

@Dao
interface CategoriaDao {

    @Insert
    suspend fun inserir(categoria: Categoria): Long

    @Update
    suspend fun atualizar(categoria: Categoria)

    @Delete
    suspend fun deletar(categoria: Categoria)

    @Query("SELECT * FROM categorias ORDER BY nome ASC")
    suspend fun listarTodas(): List<Categoria>

    @Query("SELECT * FROM categorias WHERE id = :id")
    suspend fun buscarPorId(id: Long): Categoria?
}