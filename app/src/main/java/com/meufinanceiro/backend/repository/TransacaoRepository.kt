package com.meufinanceiro.backend.repository

import com.meufinanceiro.backend.dao.TransacaoDao
import com.meufinanceiro.backend.model.Transacao
import com.meufinanceiro.backend.model.TransacaoComCategoria

class TransacaoRepository(
    private val dao: TransacaoDao
) {

    suspend fun salvar(transacao: Transacao): Long {
        return if (transacao.id == 0L) {
            dao.inserir(transacao)
        } else {
            dao.atualizar(transacao)
            transacao.id
        }
    }

    suspend fun listarTodas(): List<Transacao> = dao.listarTodas()

    suspend fun buscarPorId(id: Long): Transacao? = dao.buscarPorId(id)

    suspend fun deletar(transacao: Transacao) {
        dao.deletar(transacao)
    }

    suspend fun listarComCategoria(): List<TransacaoComCategoria> =
        dao.listarComCategoria()

    suspend fun buscarComCategoriaPorId(id: Long): TransacaoComCategoria? =
        dao.buscarComCategoriaPorId(id)

    suspend fun listarPorPeriodo(
        inicio: Long,
        fim: Long
    ): List<TransacaoComCategoria> {
        return dao.listarPorPeriodo(inicio, fim)
    }

}