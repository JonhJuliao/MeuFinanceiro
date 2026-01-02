package com.meufinanceiro.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.repository.CategoriaRepository
import com.meufinanceiro.backend.repository.TransacaoRepository
import com.meufinanceiro.ui.screens.GastoCategoriaUi // Certifique-se que este import está correto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResumoFinanceiroViewModel(
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    // MUDANÇA 1: O estado inicial agora suporta duas listas
    private val _uiState = MutableStateFlow(ResumoUiState())
    val uiState: StateFlow<ResumoUiState> = _uiState.asStateFlow()

    private val coresGrafico = listOf(
        Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFFFFA726),
        Color(0xFF66BB6A), Color(0xFFAB47BC), Color(0xFF26C6DA), Color(0xFFFF7043)
    )

    init {
        carregarDados()
    }

    fun carregarDados() {
        viewModelScope.launch {
            // Mantendo a correção do listarComCategoria
            val transacoes = transacaoRepository.listarComCategoria().map { it.transacao }
            val categorias = categoriaRepository.listarTodas()

            val despesas = transacoes.filter { it.tipo == TipoTransacao.DESPESA }
            val totalGeral = despesas.sumOf { it.valor }

            if (totalGeral == 0.0) {
                _uiState.value = ResumoUiState()
                return@launch
            }

            // --- LÓGICA 1: POR CATEGORIA (JÁ EXISTIA) ---
            val porCategoria = despesas.groupBy { it.categoriaId }
                .mapNotNull { (catId, lista) ->
                    val nome = categorias.find { it.id == catId }?.nome ?: "Outros"
                    val total = lista.sumOf { it.valor }
                    if (total > 0) {
                        GastoCategoriaUi(nome, total, Color.Gray, (total / totalGeral).toFloat())
                    } else null
                }
                .sortedByDescending { it.porcentagem }
                .mapIndexed { i, item -> item.copy(cor = coresGrafico[i % coresGrafico.size]) }

            // --- LÓGICA 2: POR PAGAMENTO (NOVO!) ---
            val porPagamento = despesas.groupBy { it.metodoPagamento }
                .mapNotNull { (metodo, lista) ->
                    // Formata "CREDITO" para "Credito"
                    val nomeBonito = metodo.lowercase().replaceFirstChar { it.uppercase() }
                    val total = lista.sumOf { it.valor }

                    if (total > 0) {
                        GastoCategoriaUi(nomeBonito, total, Color.Gray, (total / totalGeral).toFloat())
                    } else null
                }
                .sortedByDescending { it.porcentagem }
                .mapIndexed { i, item -> item.copy(cor = coresGrafico[i % coresGrafico.size]) }

            // Atualiza a tela com as duas listas
            _uiState.value = ResumoUiState(
                listaPorCategoria = porCategoria,
                listaPorPagamento = porPagamento,
                despesaTotal = totalGeral
            )
        }
    }
}

// MUDANÇA 2: Atualizei o Data Class do Estado
data class ResumoUiState(
    val listaPorCategoria: List<GastoCategoriaUi> = emptyList(),
    val listaPorPagamento: List<GastoCategoriaUi> = emptyList(),
    val despesaTotal: Double = 0.0
)

class ResumoFinanceiroViewModelFactory(
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResumoFinanceiroViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ResumoFinanceiroViewModel(transacaoRepository, categoriaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}