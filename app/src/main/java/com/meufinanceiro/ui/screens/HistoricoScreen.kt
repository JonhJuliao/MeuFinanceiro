package com.meufinanceiro.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.room.Room
import com.meufinanceiro.backend.db.AppDatabase
import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.model.TransacaoComCategoria
import com.meufinanceiro.backend.repository.TransacaoRepository
import com.meufinanceiro.ui.extensions.categoriaNome
import com.meufinanceiro.ui.extensions.toCurrency
import com.meufinanceiro.ui.extensions.toDateFormat
import com.meufinanceiro.ui.viewmodel.HistoricoFactory
import com.meufinanceiro.ui.viewmodel.HistoricoViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoScreen(navController: NavController) {

    val context = LocalContext.current

    // 1. CONFIGURAÇÃO DE DEPENDÊNCIAS (MVVM)
    // Inicializa o Banco de Dados e o Repositório.
    // O 'remember' garante que não recriamos o banco a cada recomposição da tela.
    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db").build()
    }
    val repository = remember { TransacaoRepository(db.transacaoDao()) }

    // Cria o ViewModel usando uma Factory customizada para injetar o repositório
    val viewModel: HistoricoViewModel = viewModel(factory = HistoricoFactory(repository))

    // 2. OBSERVANDO O ESTADO (Reatividade)
    // A tela "escuta" o StateFlow do ViewModel.
    // Sempre que a lista muda no ViewModel, a tela se redesenha automaticamente.
    val lista by viewModel.transacoes.collectAsState()

    // 3. ESTADOS LOCAIS PARA O FILTRO
    // Guardam as datas selecionadas pelo usuário (pode ser null se não selecionou ainda)
    var dataInicio by remember { mutableStateOf<Long?>(null) }
    var dataFim by remember { mutableStateOf<Long?>(null) }

    // Função auxiliar para abrir o Calendário Nativo do Android (DatePicker)
    fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                // Ajusta o calendário para a data escolhida e retorna os milissegundos
                calendar.set(year, month, day, 0, 0, 0)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de Transações") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxSize()
        ) {

            // ==========================================
            // SEÇÃO 1: CARTÃO DE FILTRO DE DATA
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {

                    Text(
                        text = "Filtrar por período",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botões para selecionar Data Início e Fim
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { showDatePicker { dataInicio = it } }
                        ) {
                            // Mostra a data formatada ou o texto padrão
                            Text(dataInicio?.toDateFormat() ?: "Data início")
                        }

                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { showDatePicker { dataFim = it } }
                        ) {
                            Text(dataFim?.toDateFormat() ?: "Data fim")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botões de Ação (Filtrar e Limpar)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            // Só habilita o botão se as duas datas foram escolhidas
                            enabled = dataInicio != null && dataFim != null,
                            onClick = {
                                viewModel.filtrarPorPeriodo(dataInicio!!, dataFim!!)
                            }
                        ) {
                            Text("Filtrar")
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // Limpa os estados locais e reseta a lista no ViewModel
                                dataInicio = null
                                dataFim = null
                                viewModel.limparFiltro()
                            }
                        ) {
                            Text("Limpar")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // SEÇÃO 2: LISTAGEM INTELIGENTE
            // ==========================================

            // Lógica de "Empty State" (Estado Vazio)
            // Se não houver itens, mostramos um aviso amigável em vez de uma tela em branco.
            if (lista.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Nenhuma movimentação",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Suas receitas e despesas aparecerão aqui.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Se houver itens, usamos LazyColumn para performance (carrega sob demanda)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(lista) { item ->
                        TransacaoCard(
                            transacao = item,
                            onClick = {
                                // Navega para a tela de Registro passando o ID para edição
                                navController.navigate("registrar?id=${item.transacao.id}")
                            },
                            onDelete = { viewModel.deletar(item.transacao.id) }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENTE: CARD DA TRANSAÇÃO
// ==========================================
@Composable
fun TransacaoCard(
    transacao: TransacaoComCategoria,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    // Define estilo visual baseado no tipo (Receita = Verde/Cima, Despesa = Vermelho/Baixo)
    val isReceita = transacao.transacao.tipo == TipoTransacao.RECEITA

    val containerColor = if (isReceita)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    else
        MaterialTheme.colorScheme.error.copy(alpha = 0.1f)

    val valorColor = if (isReceita)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.error

    val icone = if (isReceita) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Ícone circular colorido
            Surface(
                shape = CircleShape,
                color = valorColor.copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icone,
                        contentDescription = null,
                        tint = valorColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Informações Centrais (Categoria, Descrição, Data)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transacao.categoriaNome, // Extension property usada aqui
                    fontWeight = FontWeight.Bold
                )

                if (!transacao.transacao.descricao.isNullOrBlank()) {
                    Text(
                        text = transacao.transacao.descricao,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = transacao.transacao.dataMillis.toDateFormat(), // Extension function
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Lado Direito (Valor e Botão Excluir)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = transacao.transacao.valor.toCurrency(), // Extension function
                    color = valorColor,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}