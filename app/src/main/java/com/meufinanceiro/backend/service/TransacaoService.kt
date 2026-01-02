package com.meufinanceiro.backend.service

import com.meufinanceiro.backend.model.MetodoPagamento
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
        descricao: String?,
        // NOVO: Precisamos receber isso para salvar corretamente
        metodoPagamento: String = MetodoPagamento.DINHEIRO.name
    ): Long {
        require(valor > 0) { "Valor da transação deve ser positivo" }

        val categoria = categoriaRepository.buscarPorId(categoriaId)
            ?: throw IllegalArgumentException("Categoria inexistente")

        val transacao = Transacao(
            tipo = tipo,
            valor = valor,
            dataMillis = dataMillis,
            categoriaId = categoria.id,
            descricao = descricao,
            metodoPagamento = metodoPagamento // <--- Preenchendo o novo campo
        )

        // CORREÇÃO: No Repository a função de criar se chama 'inserir'
        return transacaoRepository.inserir(transacao)
    }

    suspend fun listarTransacoes(): List<Transacao> {
        // CORREÇÃO: O Repository não tem mais 'listarTodas' simples.
        // Usamos 'listarComCategoria' e extraímos apenas a parte da transação.
        return transacaoRepository.listarComCategoria().map { it.transacao }
    }

    suspend fun listarTransacoesComCategoria(): List<TransacaoComCategoria> =
        transacaoRepository.listarComCategoria()

    suspend fun excluirTransacao(id: Long) {
        // CORREÇÃO: O Repository usa 'buscarComCategoriaPorId'
        val itemCompleto = transacaoRepository.buscarComCategoriaPorId(id)
            ?: return

        transacaoRepository.deletar(itemCompleto.transacao)
    }
}