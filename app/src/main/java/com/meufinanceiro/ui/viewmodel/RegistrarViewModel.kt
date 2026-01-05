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
import java.util.Calendar // <--- Importante

class RegistrarViewModel(
    private val repository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private var transacaoIdEdicao: Long? = null

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
        totalParcelas: Int = 1, // <--- NOVO PARÂMETRO
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val tipo = if (tipoTela == TipoTela.RECEITA) TipoTransacao.RECEITA else TipoTransacao.DESPESA

                // MODO EDIÇÃO: Não parcelamos, apenas salvamos o que está na tela
                if (transacaoIdEdicao != null && transacaoIdEdicao!! > 0) {
                    val transacao = Transacao(
                        id = transacaoIdEdicao!!,
                        valor = valor,
                        descricao = descricao,
                        dataMillis = dataMillis,
                        tipo = tipo,
                        categoriaId = categoriaId,
                        metodoPagamento = metodoPagamento,
                        parcelaAtual = 1, // Edição simples
                        totalParcelas = 1
                    )
                    repository.atualizar(transacao)
                } else {
                    // MODO CRIAÇÃO: Aqui entra o LOOP DE PARCELAS

                    val valorParcela = valor / totalParcelas

                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = dataMillis

                    // Loop para criar transações futuras
                    for (i in 1..totalParcelas) {

                        // Formata a descrição: "TV Sala (1/10)"
                        val descFinal = if (totalParcelas > 1) {
                            val descBase = if (descricao.isBlank()) "Compra Parcelada" else descricao
                            "$descBase ($i/$totalParcelas)"
                        } else {
                            descricao
                        }

                        val novaTransacao = Transacao(
                            id = 0,
                            valor = valorParcela,
                            descricao = descFinal,
                            dataMillis = calendar.timeInMillis, // Data atual do loop
                            tipo = tipo,
                            categoriaId = categoriaId,
                            metodoPagamento = metodoPagamento,
                            parcelaAtual = i,
                            totalParcelas = totalParcelas
                        )

                        repository.inserir(novaTransacao)

                        // Avança 1 mês
                        calendar.add(Calendar.MONTH, 1)
                    }
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