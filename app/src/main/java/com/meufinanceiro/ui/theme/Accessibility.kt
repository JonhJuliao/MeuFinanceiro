package com.meufinanceiro.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Singleton para gerenciar o estado globalmente no App
object AcessibilidadeApp {

    // Variável que guarda se o modo está ativo ou não
    // (Num app real, salvaríamos isso no DataStore/SharedPreferences)
    var isModoDaltonicoAtivo by mutableStateOf(false)

    // --- PALETA PADRÃO (Verde/Vermelho) ---
    private val VerdeReceita = Color(0xFF4CAF50) // Green 500
    private val VermelhoDespesa = Color(0xFFEF5350) // Red 400

    // --- PALETA DALTÔNICA (Azul/Laranja - Padrão Universal) ---
    // Azul é universalmente visível. Laranja oferece alto contraste contra o Azul.
    private val AzulReceita = Color(0xFF2979FF) // Blue A400 (Vibrante)
    private val LaranjaDespesa = Color(0xFFFF9100) // Orange A400 (Vibrante)

    // --- GETTERS INTELIGENTES ---
    // As telas vão chamar APENAS isso aqui. Elas não precisam saber qual modo está ativo.

    val corReceita: Color
        get() = if (isModoDaltonicoAtivo) AzulReceita else VerdeReceita

    val corDespesa: Color
        get() = if (isModoDaltonicoAtivo) LaranjaDespesa else VermelhoDespesa

    val corReceitaFundo: Color
        get() = if (isModoDaltonicoAtivo) AzulReceita.copy(alpha = 0.1f) else VerdeReceita.copy(alpha = 0.1f)

    val corDespesaFundo: Color
        get() = if (isModoDaltonicoAtivo) LaranjaDespesa.copy(alpha = 0.1f) else VermelhoDespesa.copy(alpha = 0.1f)
}