package com.meufinanceiro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meufinanceiro.backend.model.TransacaoComCategoria
import com.meufinanceiro.backend.repository.TransacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// O ViewModel atua como um intermediário entre a Tela (UI) e os Dados (Repository).
// Ele guarda o estado da tela para que os dados sobrevivam se a tela girar, por exemplo.
class HistoricoViewModel(
    private val repository: TransacaoRepository
) : ViewModel() {

    // --- GERENCIAMENTO DE ESTADO (StateFlow) ---
    // _transacoes (Privado): Lista mutável que só o ViewModel pode mexer.
    private val _transacoes = MutableStateFlow<List<TransacaoComCategoria>>(emptyList())

    // transacoes (Público): Lista imutável que a tela "observa".
    // Isso garante que a UI nunca altere os dados diretamente, evitando bugs.
    val transacoes = _transacoes.asStateFlow()

    // Bloco init: Executado assim que a tela de histórico é criada.
    init {
        atualizarLista()
    }

    // Busca TODOS os dados do banco e atualiza o StateFlow.
    // A tela perceberá a mudança automaticamente e se redesenhará.
    fun atualizarLista() {
        viewModelScope.launch { // Coroutine: executa em segundo plano
            _transacoes.value = repository.listarComCategoria()
        }
    }

    // Filtra a lista usando uma query específica de datas no banco (BETWEEN).
    fun filtrarPorPeriodo(inicio: Long, fim: Long) {
        viewModelScope.launch {
            // Aqui chamamos o método especial do Repository que criamos
            _transacoes.value = repository.listarPorPeriodo(inicio, fim)
        }
    }

    // Chamado quando o usuário clica em "Limpar" no filtro.
    // Simplesmente busca tudo do banco novamente.
    fun limparFiltro() {
        atualizarLista()
    }

    // Lógica para excluir um item
    fun deletar(transacaoId: Long) {
        viewModelScope.launch {
            // 1. Busca o item completo para ter certeza que existe
            val itemCompleto = repository.buscarComCategoriaPorId(transacaoId)

            if (itemCompleto != null) {
                // 2. O Room precisa do objeto 'Transacao' original para deletar
                val transacaoParaDeletar = itemCompleto.transacao
                repository.deletar(transacaoParaDeletar)

                // 3. Após deletar, recarregamos a lista para o item sumir da tela
                atualizarLista()
            }
        }
    }
}

// --- FACTORY (Injeção de Dependência Manual) ---
// O Android não sabe criar ViewModels com parâmetros (como o 'repository') nativamente.
// Esta classe ensina o Android a criar o HistoricoViewModel injetando o repository correto.
class HistoricoFactory(private val repository: TransacaoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoricoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoricoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}