package com.meufinanceiro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.repository.TransacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: TransacaoRepository
) : ViewModel() {

    private val _saldoTotal = MutableStateFlow(0.0)
    val saldoTotal = _saldoTotal.asStateFlow()

    // O nome do usuário poderia vir de uma preferência ou banco também
    val nomeUsuario = "Guilherme"

    fun carregarSaldo() {
        viewModelScope.launch {
            val lista = repository.listarTodas()

            // Lógica simples de saldo: Receitas - Despesas
            val receitas = lista.filter { it.tipo == TipoTransacao.RECEITA }.sumOf { it.valor }
            val despesas = lista.filter { it.tipo == TipoTransacao.DESPESA }.sumOf { it.valor }

            _saldoTotal.value = receitas - despesas
        }
    }
}

class HomeViewModelFactory(private val repository: TransacaoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}