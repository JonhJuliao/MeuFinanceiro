package com.meufinanceiro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.meufinanceiro.backend.repository.CategoriaRepository
import com.meufinanceiro.backend.repository.TransacaoRepository

// --- PADRÃO FACTORY ATUALIZADO ---

class ResumoFinanceiroFactory(
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResumoFinanceiroViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // AQUI ESTAVA O ERRO: Agora passamos os repositórios, não o service
            return ResumoFinanceiroViewModel(transacaoRepository, categoriaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}