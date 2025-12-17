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

class HomeViewModel(
    private val repository: TransacaoRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    // 1. ESTADO DO SALDO
    // Começa com 0.0. É 'Mutable' pq o ViewModel pode mudar o valor.
    private val _saldoTotal = MutableStateFlow(0.0)
    // Expomos como 'StateFlow' (imutável) para a tela apenas LER, não mudar.
    val saldoTotal = _saldoTotal.asStateFlow()

    // 2. ESTADO DO NOME DO USUÁRIO
    private val _nomeUsuario = MutableStateFlow("Usuário")
    val nomeUsuario = _nomeUsuario.asStateFlow()

    // 3. ESTADO DA LISTA DE TRANSAÇÕES RECENTES (O ERRO ESTAVA AQUI)
    // Guarda uma lista de 'TransacaoComCategoria'. Começa vazia.
    private val _ultimasTransacoes = MutableStateFlow<List<TransacaoComCategoria>>(emptyList())
    val ultimasTransacoes = _ultimasTransacoes.asStateFlow()

    // Bloco 'init': Executa assim que o ViewModel nasce.
    init {
        carregarDados()
    }

    // Função pública que a tela chama para recarregar tudo
    fun carregarDados() {
        carregarNome()
        carregarFinanceiro()
    }

    // Busca as transações no banco e faz os cálculos
    private fun carregarFinanceiro() {
        // viewModelScope.launch: Abre uma "thread" secundária para não travar o app
        viewModelScope.launch {
            // Pega TODAS as transações do banco
            val lista = repository.listarComCategoria()

            // --- CÁLCULO DO SALDO ---
            // Soma tudo que é RECEITA
            val receitas = lista.filter { it.transacao.tipo == TipoTransacao.RECEITA }
                .sumOf { it.transacao.valor }

            // Soma tudo que é DESPESA
            val despesas = lista.filter { it.transacao.tipo == TipoTransacao.DESPESA }
                .sumOf { it.transacao.valor }

            // Atualiza o valor do saldo final
            _saldoTotal.value = receitas - despesas

            // --- FILTRO DAS ÚLTIMAS 5 ---
            // Pega apenas os primeiros 5 itens da lista para mostrar na Home
            // (Assumindo que o SQL já retorna ordenado por data)
            _ultimasTransacoes.value = lista.take(5)
        }
    }

    // Lê o nome salvo na memória do celular (Preferences)
    private fun carregarNome() {
        _nomeUsuario.value = userPreferences.recuperarNome()
    }

    // Salva um novo nome e atualiza a tela
    fun atualizarNome(novoNome: String) {
        userPreferences.salvarNome(novoNome)
        _nomeUsuario.value = novoNome
    }
}

// Factory: Necessário para criar o ViewModel passando argumentos (Repository e Context)
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