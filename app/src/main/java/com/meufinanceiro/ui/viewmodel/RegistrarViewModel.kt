package com.meufinanceiro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meufinanceiro.backend.model.Categoria
import com.meufinanceiro.backend.model.Transacao
import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.repository.CategoriaRepository
import com.meufinanceiro.backend.repository.TransacaoRepository
import com.meufinanceiro.ui.screens.TipoTela
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistrarViewModel(
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository // <--- NOVO: Precisamos disso
) : ViewModel() {

    // Lista de categorias REAIS do banco
    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias.asStateFlow()

    init {
        carregarCategorias()
    }

    private fun carregarCategorias() {
        viewModelScope.launch {
            _categorias.value = categoriaRepository.listarTodas()
        }
    }

    fun salvarTransacao(
        tipoTela: TipoTela,
        valor: Double,
        dataMillis: Long,
        categoriaId: Long, // <--- MUDANÇA: Agora recebemos o ID direto, sem adivinhação
        descricao: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val tipoBackend = if (tipoTela == TipoTela.RECEITA)
                TipoTransacao.RECEITA
            else
                TipoTransacao.DESPESA

            val novaTransacao = Transacao(
                id = 0,
                tipo = tipoBackend,
                valor = valor,
                categoriaId = categoriaId, // <--- ID correto vindo da tela
                descricao = descricao,
                dataMillis = dataMillis
            )

            try {
                transacaoRepository.salvar(novaTransacao)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError("Erro ao salvar: ${e.message}")
            }
        }
    }
}

// Factory atualizada para receber os DOIS repositórios
class RegistrarViewModelFactory(
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistrarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegistrarViewModel(transacaoRepository, categoriaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}