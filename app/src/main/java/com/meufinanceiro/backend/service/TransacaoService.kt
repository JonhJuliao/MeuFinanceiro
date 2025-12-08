package com.meufinanceiro.backend.service

import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.model.Transacao
import com.meufinanceiro.backend.model.TransacaoComCategoria
import com.meufinanceiro.backend.repository.CategoriaRepository
import com.meufinanceiro.backend.repository.TransacaoRepository

class TransacaoService(
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository
) {

    suspend fun registrarTransacao(
        tipo: TipoTransacao,
        valor: Double,
        dataMillis: Long,
        categoriaId: Long,
        descricao: String?
    ): Long {
        require(valor > 0) { "Valor da transação deve ser positivo" }

        val categoria = categoriaRepository.buscarPorId(categoriaId)
            ?: throw IllegalArgumentException("Categoria inexistente")

        val transacao = Transacao(
            tipo = tipo,
            valor = valor,
            dataMillis = dataMillis,
            categoriaId = categoria.id,
            descricao = descricao
        )

        return transacaoRepository.salvar(transacao)
    }

    suspend fun listarTransacoes(): List<Transacao> =
        transacaoRepository.listarTodas()

    suspend fun listarTransacoesComCategoria(): List<TransacaoComCategoria> =
        transacaoRepository.listarComCategoria()

    suspend fun excluirTransacao(id: Long) {
        val existente = transacaoRepository.buscarPorId(id)
            ?: return
        transacaoRepository.deletar(existente)
    }

    // depois você pode criar um método editarTransacao(...) aproveitando o mesmo fluxo
}