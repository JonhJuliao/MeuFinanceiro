package com.meufinanceiro.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.repository.CategoriaRepository
import com.meufinanceiro.backend.repository.TransacaoRepository
import com.meufinanceiro.ui.screens.GastoCategoriaUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResumoFinanceiroViewModel(
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    // Estado da tela (Lista + Total)
    private val _uiState = MutableStateFlow(ResumoUiState(emptyList(), 0.0))
    val uiState: StateFlow<ResumoUiState> = _uiState.asStateFlow()

    // Paleta de cores
    private val coresGrafico = listOf(
        Color(0xFFEF5350), // Vermelho
        Color(0xFF42A5F5), // Azul
        Color(0xFFFFA726), // Laranja
        Color(0xFF66BB6A), // Verde
        Color(0xFFAB47BC), // Roxo
        Color(0xFF26C6DA), // Ciano
        Color(0xFFFF7043)  // Coral
    )

    // Bloco que roda assim que o ViewModel nasce
    init {
        carregarDados()
    }

    // Função que vai lá no banco buscar os dados
    fun carregarDados() {
        viewModelScope.launch {
            // Busca os dados usando os métodos que JÁ EXISTEM no seu Repo
            val transacoes = transacaoRepository.listarTodas()
            val categorias = categoriaRepository.listarTodas()

            // Filtra só despesas
            val despesas = transacoes.filter { it.tipo == TipoTransacao.DESPESA }

            // Calcula total
            val totalGeral = despesas.sumOf { it.valor }

            if (totalGeral == 0.0) {
                _uiState.value = ResumoUiState(emptyList(), 0.0)
                return@launch
            }

            // Agrupa e calcula
            val gastosPorCategoria = despesas.groupBy { it.categoriaId }

            val dadosGrafico = gastosPorCategoria.mapNotNull { (catId, listaTransacoes) ->
                val nomeCategoria = categorias.find { it.id == catId }?.nome ?: "Outros"
                val totalCategoria = listaTransacoes.sumOf { it.valor }
                val porcentagem = (totalCategoria / totalGeral).toFloat()

                if (totalCategoria > 0) {
                    GastoCategoriaUi(
                        nome = nomeCategoria,
                        valor = totalCategoria,
                        porcentagem = porcentagem,
                        cor = Color.Gray // Será preenchida abaixo
                    )
                } else null
            }.sortedByDescending { it.porcentagem }

            // Aplica cores
            val listaColorida = dadosGrafico.mapIndexed { index, item ->
                item.copy(cor = coresGrafico[index % coresGrafico.size])
            }

            // Atualiza a tela
            _uiState.value = ResumoUiState(listaColorida, totalGeral)
        }
    }
}

data class ResumoUiState(
    val listaGastos: List<GastoCategoriaUi>,
    val despesaTotal: Double
)

// --- A CORREÇÃO ESTÁ AQUI EMBAIXO ---
// Agora a Factory recebe os Repositories e passa para o ViewModel
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