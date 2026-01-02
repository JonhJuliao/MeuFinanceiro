package com.meufinanceiro.backend.repository

import com.meufinanceiro.backend.dao.TransacaoDao
import com.meufinanceiro.backend.model.Transacao
import com.meufinanceiro.backend.model.TransacaoComCategoria

class TransacaoRepository(private val dao: TransacaoDao) {

    // --- LEITURAS ---
    suspend fun listarComCategoria(): List<TransacaoComCategoria> {
        return dao.listarComCategoria()
    }

    suspend fun buscarComCategoriaPorId(id: Long): TransacaoComCategoria? {
        return dao.buscarComCategoriaPorId(id)
    }

    suspend fun listarPorPeriodo(inicio: Long, fim: Long): List<TransacaoComCategoria> {
        return dao.listarPorPeriodo(inicio, fim)
    }

    suspend fun buscarPorDescricao(query: String): List<TransacaoComCategoria> {
        return dao.buscarPorDescricao(query)
    }

    // --- ESCRITAS (AS FUNÇÕES QUE FALTAVAM) ---
    suspend fun inserir(transacao: Transacao): Long {
        return dao.inserir(transacao)
    }

    suspend fun atualizar(transacao: Transacao) {
        dao.atualizar(transacao)
    }

    suspend fun deletar(transacao: Transacao) {
        dao.deletar(transacao)
    }
}