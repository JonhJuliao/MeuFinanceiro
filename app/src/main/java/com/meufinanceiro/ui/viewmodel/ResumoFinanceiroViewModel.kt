package com.meufinanceiro.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.repository.CategoriaRepository
import com.meufinanceiro.backend.repository.TransacaoRepository
// Importante: Certifique-se de que GastoCategoriaUi está neste pacote ou importe corretamente
import com.meufinanceiro.ui.screens.GastoCategoriaUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ResumoFinanceiroViewModel(
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResumoUiState())
    val uiState: StateFlow<ResumoUiState> = _uiState.asStateFlow()

    // Controla o mês atual da tela
    private var calendarioAtual = Calendar.getInstance()

    private val coresGrafico = listOf(
        Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFFFFA726),
        Color(0xFF66BB6A), Color(0xFFAB47BC), Color(0xFF26C6DA), Color(0xFFFF7043)
    )

    init {
        carregarDados()
    }

    // --- NAVEGAÇÃO ENTRE MESES ---
    fun mesAnterior() {
        calendarioAtual.add(Calendar.MONTH, -1)
        carregarDados()
    }

    fun proximoMes() {
        calendarioAtual.add(Calendar.MONTH, 1)
        carregarDados()
    }

    fun carregarDados() {
        viewModelScope.launch {
            // 1. Define o período do mês SELECIONADO
            val inicioMes = getInicioMes(calendarioAtual)
            val fimMes = getFimMes(calendarioAtual)

            // 2. Busca transações APENAS deste mês
            val listaTransacoesMes = transacaoRepository.listarPorPeriodo(inicioMes, fimMes)

            // Separa apenas os objetos de transação pura para cálculos simples
            val transacoesPuras = listaTransacoesMes.map { it.transacao }

            // 3. Cálculos do Cabeçalho (Resumo Geral)
            val totalReceitas = transacoesPuras.filter { it.tipo == TipoTransacao.RECEITA }.sumOf { it.valor }

            // Filtra as despesas (usaremos isso para os gráficos também)
            val despesasDoMes = listaTransacoesMes.filter { it.transacao.tipo == TipoTransacao.DESPESA }
            val totalDespesas = despesasDoMes.sumOf { it.transacao.valor }

            val saldo = totalReceitas - totalDespesas

            // 4. Comparação com Mês Anterior
            val comparacaoTexto = calcularComparacao(totalDespesas)

            // 5. Nome do Mês
            val nomeMes = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
                .format(calendarioAtual.time)
                .replaceFirstChar { it.uppercase() }

            // --- GRÁFICOS ---

            if (totalDespesas == 0.0) {
                _uiState.value = ResumoUiState(
                    mesExibicao = nomeMes,
                    totalReceitas = totalReceitas,
                    totalDespesas = 0.0,
                    saldo = saldo,
                    comparacao = "Sem dados anteriores"
                )
                return@launch
            }

            // Lógica 1: Por Categoria (CORRIGIDO O ERRO AQUI)
            // Usamos 'it.categoria.nome' diretamente para evitar erro de import da extensão
            val porCategoria = despesasDoMes
                .groupBy { it.categoria.nome }
                .mapNotNull { (nomeCat, lista) ->
                    val total = lista.sumOf { it.transacao.valor }
                    if (total > 0) {
                        GastoCategoriaUi(nomeCat, total, Color.Gray, (total / totalDespesas).toFloat())
                    } else null
                }
                .sortedByDescending { it.porcentagem }
                .mapIndexed { i, item -> item.copy(cor = coresGrafico[i % coresGrafico.size]) }

            // Lógica 2: Por Pagamento
            val porPagamento = despesasDoMes
                .groupBy { it.transacao.metodoPagamento }
                .mapNotNull { (metodo, lista) ->
                    val nomeBonito = metodo.lowercase().replaceFirstChar { it.uppercase() }
                    val total = lista.sumOf { it.transacao.valor }
                    if (total > 0) {
                        GastoCategoriaUi(nomeBonito, total, Color.Gray, (total / totalDespesas).toFloat())
                    } else null
                }
                .sortedByDescending { it.porcentagem }
                .mapIndexed { i, item -> item.copy(cor = coresGrafico[i % coresGrafico.size]) }

            // ATUALIZA O ESTADO
            _uiState.value = ResumoUiState(
                mesExibicao = nomeMes,
                totalReceitas = totalReceitas,
                totalDespesas = totalDespesas,
                saldo = saldo,
                comparacao = comparacaoTexto,
                listaPorCategoria = porCategoria,
                listaPorPagamento = porPagamento
            )
        }
    }

    private suspend fun calcularComparacao(despesasAtuais: Double): String {
        val calAnt = calendarioAtual.clone() as Calendar
        calAnt.add(Calendar.MONTH, -1)
        val inicioAnt = getInicioMes(calAnt)
        val fimAnt = getFimMes(calAnt)

        val transacoesAnt = transacaoRepository.listarPorPeriodo(inicioAnt, fimAnt)
        val despesasAnt = transacoesAnt.filter { it.transacao.tipo == TipoTransacao.DESPESA }.sumOf { it.transacao.valor }

        if (despesasAnt == 0.0) return "Sem histórico anterior"

        val diferenca = despesasAtuais - despesasAnt
        val porcentagem = (diferenca / despesasAnt) * 100

        return if (diferenca > 0) {
            "Gastou ${"%.0f".format(porcentagem)}% a mais que no mês anterior"
        } else {
            "Economizou ${"%.0f".format(Math.abs(porcentagem))}% comparado ao mês anterior"
        }
    }

    private fun getInicioMes(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        return c.timeInMillis
    }

    private fun getFimMes(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        return c.timeInMillis
    }
}

data class ResumoUiState(
    val mesExibicao: String = "",
    val totalReceitas: Double = 0.0,
    val totalDespesas: Double = 0.0,
    val saldo: Double = 0.0,
    val comparacao: String = "",
    val listaPorCategoria: List<GastoCategoriaUi> = emptyList(),
    val listaPorPagamento: List<GastoCategoriaUi> = emptyList()
)

class ResumoFinanceiroViewModelFactory(
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResumoFinanceiroViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ResumoFinanceiroViewModel(transacaoRepository, categoriaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}