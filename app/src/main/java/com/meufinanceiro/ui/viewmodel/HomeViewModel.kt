package com.meufinanceiro.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.model.TransacaoComCategoria
import com.meufinanceiro.backend.preferences.UserPreferences
import com.meufinanceiro.backend.repository.TransacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs // Importante para corrigir o sinal matemático

class HomeViewModel(
    private val repository: TransacaoRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    // 1. ESTADO DO SALDO
    private val _saldoTotal = MutableStateFlow(0.0)
    val saldoTotal = _saldoTotal.asStateFlow()

    // 2. ESTADO DO NOME DO USUÁRIO
    private val _nomeUsuario = MutableStateFlow("Usuário")
    val nomeUsuario = _nomeUsuario.asStateFlow()

    // 3. ESTADO DA LISTA DE TRANSAÇÕES RECENTES
    private val _ultimasTransacoes = MutableStateFlow<List<TransacaoComCategoria>>(emptyList())
    val ultimasTransacoes = _ultimasTransacoes.asStateFlow()

    init {
        carregarDados()
    }

    fun carregarDados() {
        carregarNome()
        carregarFinanceiro()
    }

    private fun carregarFinanceiro() {
        viewModelScope.launch {
            // Pega TODAS as transações do banco
            val lista = repository.listarComCategoria()

            // --- CORREÇÃO MATEMÁTICA DEFINITIVA ---
            // Percorre item por item e decide o sinal com segurança
            val saldoCalculado = lista.sumOf { item ->
                val valor = item.transacao.valor

                if (item.transacao.tipo == TipoTransacao.RECEITA) {
                    // Se é Receita, pega o valor absoluto (sempre positivo) e SOMA
                    abs(valor)
                } else {
                    // Se é Despesa, pega o valor absoluto e inverte o sinal para SUBTRAIR
                    -abs(valor)
                }
            }

            // Atualiza o saldo
            _saldoTotal.value = saldoCalculado

            // --- FILTRO DAS ÚLTIMAS 5 ---
            _ultimasTransacoes.value = lista.take(5)
        }
    }

    private fun carregarNome() {
        _nomeUsuario.value = userPreferences.recuperarNome()
    }

    fun atualizarNome(novoNome: String) {
        userPreferences.salvarNome(novoNome)
        _nomeUsuario.value = novoNome
    }
}

class HomeViewModelFactory(
    private val repository: TransacaoRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                repository,
                UserPreferences(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}