package com.meufinanceiro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meufinanceiro.backend.service.ResumoFinanceiroService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// O ViewModel é a ponte entre a Lógica de Negócio (Service) e a Tela (UI).
// Ele recebe o Service no construtor para poder pedir os cálculos prontos.
class ResumoFinanceiroViewModel(
    private val service: ResumoFinanceiroService
) : ViewModel() {

    // --- ESTADOS OBSERVÁVEIS (StateFlow) ---
    // Usamos o padrão de encapsulamento:
    // 1. Variável Privada (_): Mutável, onde o ViewModel altera os valores.
    private val _totalReceitas = MutableStateFlow(0.0)
    // 2. Variável Pública: Imutável, onde a Tela apenas LÊ (observa) os valores.
    val totalReceitas: StateFlow<Double> = _totalReceitas

    private val _totalDespesas = MutableStateFlow(0.0)
    val totalDespesas: StateFlow<Double> = _totalDespesas

    private val _saldo = MutableStateFlow(0.0)
    val saldo: StateFlow<Double> = _saldo

    // Bloco init: Executa automaticamente quando o ViewModel é criado.
    // Assim, ao abrir a tela de Resumo, os dados já começam a carregar.
    init {
        carregarResumo()
    }

    // Função que aciona o Service para fazer os cálculos matemáticos
    fun carregarResumo() {
        // viewModelScope.launch: Cria uma Coroutine (thread secundária).
        // Isso é essencial para não travar a tela enquanto o banco de dados trabalha.
        viewModelScope.launch {
            // Chama o Service para processar os dados brutos do banco
            // e devolver um objeto bonitinho com os totais já somados.
            val resumo = service.calcularResumo()

            // Atualiza os estados com os valores retornados.
            // A Tela (Screen) perceberá essas mudanças e atualizará os números automaticamente.
            _totalReceitas.value = resumo.totalReceitas
            _totalDespesas.value = resumo.totalDespesas
            _saldo.value = resumo.saldo
        }
    }
}