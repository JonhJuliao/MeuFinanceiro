package com.meufinanceiro.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meufinanceiro.backend.model.Categoria
import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.repository.CategoriaRepository
import com.meufinanceiro.backend.repository.TransacaoRepository
import com.meufinanceiro.ui.theme.AcessibilidadeApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Classe para facilitar a exibição na tela
data class CategoriaComProgresso(
    val categoria: Categoria,
    val totalGasto: Double,
    val progresso: Float, // De 0.0 a 1.0
    val corStatus: Color
)

class CategoriasViewModel(
    private val categoriaRepository: CategoriaRepository,
    private val transacaoRepository: TransacaoRepository // <--- Novo: Precisa ler gastos
) : ViewModel() {

    private val _uiState = MutableStateFlow<List<CategoriaComProgresso>>(emptyList())
    val uiState: StateFlow<List<CategoriaComProgresso>> = _uiState.asStateFlow()

    init {
        carregarDados()
    }

    fun carregarDados() {
        viewModelScope.launch {
            val categorias = categoriaRepository.listarTodas()

            // Usando a função nova que corrigimos (listarComCategoria) e mapeando
            val transacoes = transacaoRepository.listarComCategoria().map { it.transacao }

            val listaProcessada = categorias.map { cat ->
                // Soma apenas as DESPESAS dessa categoria
                val totalGasto = transacoes
                    .filter { it.categoriaId == cat.id && it.tipo == TipoTransacao.DESPESA }
                    .sumOf { it.valor }

                // Calcula % (Evita divisão por zero)
                val progresso = if (cat.metaMensal > 0) (totalGasto / cat.metaMensal).toFloat() else 0f

                // Define cor: Verde (ok), Amarelo (perto), Vermelho (estourou)
                val cor = when {
                    progresso >= 1f -> AcessibilidadeApp.corDespesa // Vermelho/Laranja (Estourou)
                    progresso >= 0.8f -> Color(0xFFFFC107) // Amarelo (Alerta)
                    else -> AcessibilidadeApp.corReceita // Verde/Azul (Tranquilo)
                }

                CategoriaComProgresso(
                    categoria = cat,
                    totalGasto = totalGasto,
                    progresso = progresso.coerceAtMost(1f), // Trava em 100% visualmente
                    corStatus = cor
                )
            }
            _uiState.value = listaProcessada
        }
    }

    fun adicionarCategoria(nome: String, meta: Double = 0.0) {
        viewModelScope.launch {
            val nova = Categoria(nome = nome, metaMensal = meta)
            categoriaRepository.salvar(nova)
            carregarDados()
        }
    }

    fun deletarCategoria(categoria: Categoria) {
        viewModelScope.launch {
            categoriaRepository.deletar(categoria)
            carregarDados()
        }
    }
}

// Factory atualizada para injetar os dois repositórios
class CategoriasViewModelFactory(
    private val categoriaRepository: CategoriaRepository,
    private val transacaoRepository: TransacaoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoriasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoriasViewModel(categoriaRepository, transacaoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}