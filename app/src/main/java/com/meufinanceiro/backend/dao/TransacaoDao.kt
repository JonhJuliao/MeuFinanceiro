package com.meufinanceiro.backend.dao

import androidx.room.*
import com.meufinanceiro.backend.model.Transacao
import com.meufinanceiro.backend.model.TransacaoComCategoria

@Dao
interface TransacaoDao {

    @Insert
    suspend fun inserir(transacao: Transacao): Long

    @Update
    suspend fun atualizar(transacao: Transacao)

    @Delete
    suspend fun deletar(transacao: Transacao)

    @Query("SELECT * FROM transacoes ORDER BY dataMillis DESC")
    suspend fun listarTodas(): List<Transacao>

    @Query("SELECT * FROM transacoes WHERE id = :id")
    suspend fun buscarPorId(id: Long): Transacao?

    // Relação com Categoria

    @Transaction
    @Query("SELECT * FROM transacoes ORDER BY dataMillis DESC")
    suspend fun listarComCategoria(): List<TransacaoComCategoria>

    @Transaction
    @Query("SELECT * FROM transacoes WHERE id = :id")
    suspend fun buscarComCategoriaPorId(id: Long): TransacaoComCategoria?

    @Transaction
    @Query(
        """
        SELECT * FROM transacoes
        WHERE dataMillis BETWEEN :inicio AND :fim
        ORDER BY dataMillis DESC
        """
    )
    suspend fun listarPorPeriodo(
        inicio: Long,
        fim: Long
    ): List<TransacaoComCategoria>

}