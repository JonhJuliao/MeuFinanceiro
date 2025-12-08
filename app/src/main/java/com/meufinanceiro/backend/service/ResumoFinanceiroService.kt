package com.meufinanceiro.backend.service

import com.meufinanceiro.backend.model.ResumoFinanceiro
import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.repository.TransacaoRepository

class ResumoFinanceiroService(
    private val transacaoRepository: TransacaoRepository
) {

    suspend fun calcularResumo(): ResumoFinanceiro {
        val transacoes = transacaoRepository.listarTodas()

        val totalReceitas = transacoes
            .filter { it.tipo == TipoTransacao.RECEITA }
            .sumOf { it.valor }

        val totalDespesas = transacoes
            .filter { it.tipo == TipoTransacao.DESPESA }
            .sumOf { it.valor }

        val saldo = totalReceitas - totalDespesas

        return ResumoFinanceiro(
            totalReceitas = totalReceitas,
            totalDespesas = totalDespesas,
            saldo = saldo
        )
    }
}