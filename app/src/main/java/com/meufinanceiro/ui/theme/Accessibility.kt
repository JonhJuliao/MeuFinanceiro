package com.meufinanceiro.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object AcessibilidadeApp {

    var isModoDaltonicoAtivo by mutableStateOf(false)

    // --- MUDANÇA AQUI: Usando as cores do seu tema "Executive Emerald" ---

    // Agora a receita usa o mesmo verde "Emerald" dos seus botões
    private val VerdeReceita = ModernGreen // Era 0xFF4CAF50, agora é 0xFF15803D

    // Vamos usar o vermelho do seu tema Light também
    private val VermelhoDespesa = LightError // Era 0xFFEF5350, agora é 0xFFDC2626

    // --- MODO DALTÔNICO (MANTIDO) ---
    private val AzulReceita = Color(0xFF2979FF)
    private val LaranjaDespesa = Color(0xFFFF9100)

    val corReceita: Color
        get() = if (isModoDaltonicoAtivo) AzulReceita else VerdeReceita

    val corDespesa: Color
        get() = if (isModoDaltonicoAtivo) LaranjaDespesa else VermelhoDespesa

    val corReceitaFundo: Color
        get() = if (isModoDaltonicoAtivo) AzulReceita.copy(alpha = 0.1f) else VerdeReceita.copy(alpha = 0.1f)

    val corDespesaFundo: Color
        get() = if (isModoDaltonicoAtivo) LaranjaDespesa.copy(alpha = 0.1f) else VermelhoDespesa.copy(alpha = 0.1f)
}