package com.meufinanceiro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meufinanceiro.backend.model.Transacao
import com.meufinanceiro.backend.model.TransacaoComCategoria
import com.meufinanceiro.backend.repository.TransacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoricoViewModel(
    private val repository: TransacaoRepository
) : ViewModel() {

    private val _transacoes = MutableStateFlow<List<TransacaoComCategoria>>(emptyList())
    val transacoes = _transacoes.asStateFlow()

    // Cache para quando limparmos a busca
    private var listaCompletaCache = listOf<TransacaoComCategoria>()

    init {
        atualizarLista()
    }

    fun atualizarLista() {
        viewModelScope.launch {
            val lista = repository.listarComCategoria()
            listaCompletaCache = lista // Salva o cache
            _transacoes.value = lista
        }
    }

    // --- FUNÇÃO CORRIGIDA PARA O SWIPE ---
    // Agora aceita a Transacao inteira, combinando com a Tela
    fun deletar(transacao: Transacao) {
        viewModelScope.launch {
            repository.deletar(transacao)
            atualizarLista() // Atualiza a lista visualmente
        }
    }

    // --- FUNÇÃO PARA O BOTÃO DESFAZER ---
    fun restaurar(item: TransacaoComCategoria) {
        viewModelScope.launch {
            // O Room respeita o ID antigo, então ela volta pro mesmo lugar
            repository.inserir(item.transacao)
            atualizarLista()
        }
    }

    // --- FILTROS ---

    fun filtrarPorPeriodo(inicio: Long, fim: Long) {
        viewModelScope.launch {
            // Ajuste para pegar o dia inteiro (até 23:59:59) se inicio == fim
            val fimAjustado = if (inicio == fim) fim + 86400000 else fim
            _transacoes.value = repository.listarPorPeriodo(inicio, fimAjustado)
        }
    }

    fun filtrarPorTexto(texto: String) {
        viewModelScope.launch {
            if (texto.isBlank()) {
                _transacoes.value = listaCompletaCache
            } else {
                _transacoes.value = repository.buscarPorDescricao(texto)
            }
        }
    }

    fun limparFiltro() {
        _transacoes.value = listaCompletaCache
    }
}

class HistoricoFactory(private val repository: TransacaoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoricoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoricoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}