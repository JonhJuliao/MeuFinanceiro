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

// ViewModel responsável pela tela de Registro.
// Ele precisa de DOIS repositórios:
// 1. TransacaoRepository: Para salvar ou atualizar a transação.
// 2. CategoriaRepository: Para listar as categorias no menu (dropdown).
class RegistrarViewModel(
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    // --- ESTADO DAS CATEGORIAS ---
    // Mantemos uma lista atualizada de categorias para o usuário escolher.
    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias.asStateFlow()

    // --- CONTROLE DE EDIÇÃO ---
    // Essa variável é o segredo do Update.
    // Se for 0L = Estamos criando uma transação NOVA.
    // Se for > 0L = Estamos editando uma transação EXISTENTE (e esse é o ID dela).
    var transacaoIdAtual: Long = 0L

    // Ao iniciar, já carrega as categorias para o dropdown não ficar vazio.
    init {
        carregarCategorias()
    }

    private fun carregarCategorias() {
        viewModelScope.launch {
            _categorias.value = categoriaRepository.listarTodas()
        }
    }

    // --- FUNÇÃO DE CARREGAR DADOS (USADA APENAS NA EDIÇÃO) ---
    // Se a tela receber um ID, ela chama essa função.
    // O ViewModel busca no banco e devolve os dados (Transacao + Categoria)
    // através do callback 'onResult', para a tela preencher os campos automaticamente.
    fun carregarDadosParaEdicao(id: Long, onResult: (Transacao, Categoria?) -> Unit) {
        if (id == 0L) return // Se for 0, não faz nada (é cadastro novo)

        transacaoIdAtual = id // Marca que estamos editando este ID

        viewModelScope.launch {
            // Busca a transação completa (com o objeto categoria associado)
            val transacaoCompleta = transacaoRepository.buscarComCategoriaPorId(id)

            if (transacaoCompleta != null) {
                // Devolve para a UI preencher os campos de texto
                onResult(transacaoCompleta.transacao, transacaoCompleta.categoria)
            }
        }
    }

    // --- FUNÇÃO DE SALVAR (SERVE PARA INSERT E UPDATE) ---
    fun salvarTransacao(
        tipoTela: TipoTela,      // Enum da UI (Visual)
        valor: Double,
        dataMillis: Long,
        categoriaId: Long,
        descricao: String?,
        onSuccess: () -> Unit,   // Callback de sucesso (fecha a tela)
        onError: (String) -> Unit // Callback de erro (mostra Toast)
    ) {
        viewModelScope.launch {
            // 1. Converte o tipo visual (Tela) para o tipo do Banco (Backend)
            val tipoBackend = if (tipoTela == TipoTela.RECEITA)
                TipoTransacao.RECEITA
            else
                TipoTransacao.DESPESA

            // 2. Cria o objeto Transação
            val novaTransacao = Transacao(
                // AQUI ESTÁ A MÁGICA:
                // Se transacaoIdAtual for 0, o Room entende como INSERT (cria novo ID).
                // Se for > 0, o Room entende como UPDATE (atualiza o registro existente).
                id = transacaoIdAtual,
                tipo = tipoBackend,
                valor = valor,
                categoriaId = categoriaId,
                descricao = descricao,
                dataMillis = dataMillis
            )

            try {
                // Chama o repositório para salvar no banco
                transacaoRepository.salvar(novaTransacao)
                onSuccess() // Avisa a tela que deu tudo certo
            } catch (e: Exception) {
                e.printStackTrace()
                onError("Erro ao salvar: ${e.message}")
            }
        }
    }
}

// --- FACTORY ---
// Necessária porque nosso ViewModel tem 2 argumentos no construtor.
class RegistrarViewModelFactory(
    private val transacaoRepo: TransacaoRepository,
    private val categoriaRepo: CategoriaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistrarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegistrarViewModel(transacaoRepo, categoriaRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}