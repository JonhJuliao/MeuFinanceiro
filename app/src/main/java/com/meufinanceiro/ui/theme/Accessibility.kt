package com.meufinanceiro.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Centraliza decisões de acessibilidade visual do app
 */
object AcessibilidadeApp {

    var isModoDaltonicoAtivo by mutableStateOf(false)

    // ================================
    // CORES PADRÃO (IDENTIDADE VISUAL)
    // ================================
    private val ReceitaPadrao = ModernGreen
    private val DespesaPadrao = LightError

    // ================================
    // CORES PARA DALTÔNICOS
    // Azul x Laranja → alta distinção
    // ================================
    private val ReceitaDaltonico = Color(0xFF2979FF)
    private val DespesaDaltonico = Color(0xFFFF9100)

    // ================================
    // CORES FINAIS
    // ================================
    val corReceita: Color
        get() = if (isModoDaltonicoAtivo) ReceitaDaltonico else ReceitaPadrao

    val corDespesa: Color
        get() = if (isModoDaltonicoAtivo) DespesaDaltonico else DespesaPadrao

    /**
     * Fundo suave seguindo recomendação Material 3 (12%)
     */
    val corReceitaFundo: Color
        get() = corReceita.copy(alpha = 0.12f)

    val corDespesaFundo: Color
        get() = corDespesa.copy(alpha = 0.12f)
}
