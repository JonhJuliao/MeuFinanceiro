package com.meufinanceiro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meufinanceiro.backend.model.Categoria
import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.model.Transacao
import com.meufinanceiro.backend.repository.CategoriaRepository
import com.meufinanceiro.backend.repository.TransacaoRepository
import com.meufinanceiro.ui.screens.TipoTela
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistrarViewModel(
    private val repository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private var transacaoIdEdicao: Long? = null

    // --- CORREÇÃO AQUI (Carregamento Manual igual ao CategoriasViewModel) ---
    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias.asStateFlow()

    init {
        carregarCategorias()
    }

    private fun carregarCategorias() {
        viewModelScope.launch {
            // Usa a função 'listarTodas()' que já existe no seu repositório
            _categorias.value = categoriaRepository.listarTodas()
        }
    }
    // -----------------------------------------------------------------------

    fun carregarDadosParaEdicao(id: Long, onDadosCarregados: (Transacao, Categoria?) -> Unit) {
        transacaoIdEdicao = id
        viewModelScope.launch {
            val transacaoComCategoria = repository.buscarComCategoriaPorId(id)
            if (transacaoComCategoria != null) {
                onDadosCarregados(transacaoComCategoria.transacao, transacaoComCategoria.categoria)
            }
        }
    }

    fun salvarTransacao(
        tipoTela: TipoTela,
        valor: Double,
        dataMillis: Long,
        categoriaId: Long,
        descricao: String,
        metodoPagamento: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val tipo = if (tipoTela == TipoTela.RECEITA) TipoTransacao.RECEITA else TipoTransacao.DESPESA

                val transacao = Transacao(
                    id = transacaoIdEdicao ?: 0L,
                    valor = valor,
                    descricao = descricao,
                    dataMillis = dataMillis,
                    tipo = tipo,
                    categoriaId = categoriaId,
                    metodoPagamento = metodoPagamento
                )

                // Agora as funções 'atualizar' e 'inserir' existem no repository!
                if (transacaoIdEdicao != null && transacaoIdEdicao!! > 0) {
                    repository.atualizar(transacao)
                } else {
                    repository.inserir(transacao)
                }
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError()
            }
        }
    }
}

class RegistrarViewModelFactory(
    private val repository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistrarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegistrarViewModel(repository, categoriaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}