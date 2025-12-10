package com.meufinanceiro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meufinanceiro.backend.model.Transacao
import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.repository.TransacaoRepository
import com.meufinanceiro.ui.screens.TipoTela
import kotlinx.coroutines.launch

class RegistrarViewModel(
    private val repository: TransacaoRepository
) : ViewModel() {

    fun salvarTransacao(
        tipoTela: TipoTela,
        valor: Double,
        dataMillis: Long,
        categoria: String, // Recebe o NOME da tela
        descricao: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            // 1. Converte o TipoTela (Visual) para TipoTransacao (Banco)
            val tipoBackend = if (tipoTela == TipoTela.RECEITA)
                TipoTransacao.RECEITA
            else
                TipoTransacao.DESPESA

            // 2. Tenta descobrir o ID baseado no nome (GAMBIARRA PROVISÓRIA)
            // IMPORTANTE: Para isso funcionar sem crashar, a tabela 'Categoria'
            // PRECISA ter esses IDs criados no banco.
            // Se o ID 1 não existir na tabela 'categorias', o app vai fechar com erro de Foreign Key.
            val idDaCategoria: Long = when (categoria) {
                "Alimentação" -> 1L
                "Transporte" -> 2L
                "Salário" -> 3L
                "Lazer" -> 4L
                "Contas" -> 5L
                "Outros" -> 6L
                else -> 6L // Default para Outros
            }

            // 3. Cria o objeto Transacao correto
            val novaTransacao = Transacao(
                id = 0,
                tipo = tipoBackend,
                valor = valor,
                categoriaId = idDaCategoria, // <--- AGORA ESTÁ CORRETO (Usa o ID, não o nome)
                descricao = descricao,
                dataMillis = dataMillis
            )

            try {
                // 4. Salva no banco
                repository.salvar(novaTransacao)
                onSuccess()
            } catch (e: Exception) {
                // Se der erro de chave estrangeira (ID não existe), vai cair aqui
                e.printStackTrace()
            }
        }
    }
}

class RegistrarViewModelFactory(private val repository: TransacaoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistrarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegistrarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}