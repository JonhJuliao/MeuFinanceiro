package com.meufinanceiro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.meufinanceiro.backend.service.ResumoFinanceiroService

// --- PADRÃO FACTORY (FÁBRICA) ---

// Esta classe serve como uma "receita de bolo" para ensinar o Android a fabricar
// o nosso ViewModel passando o ingrediente obrigatório (o Service).
class ResumoFinanceiroFactory(
    private val service: ResumoFinanceiroService // A dependência que o ViewModel precisa
) : ViewModelProvider.Factory {

    // Função padrão que o Android chama quando precisa de um ViewModel novo
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Aqui nós criamos manualmente a instância, injetando o Service
        return ResumoFinanceiroViewModel(service) as T
    }
}