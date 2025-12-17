package com.meufinanceiro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.meufinanceiro.navigation.Screen
import com.meufinanceiro.ui.extensions.categoriaNome
import com.meufinanceiro.ui.extensions.toCurrency
import com.meufinanceiro.ui.viewmodel.HomeViewModel
import com.meufinanceiro.ui.viewmodel.HomeViewModelFactory

// Imports essenciais para usar o StateFlow com 'by'
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current

    // 1. CONFIGURAÇÃO DO BANCO E VIEWMODEL
    // O 'remember' evita recriar o banco a cada recomposição da tela.
    val db = remember { Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db").build() }
    val repository = remember { TransacaoRepository(db.transacaoDao()) }

    // Cria o ViewModel injetando as dependências necessárias via Factory
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(repository, context)
    )

    // 2. OBSERVANDO OS DADOS (Estado Reativo)
    // A tela observa o ViewModel. Se o saldo ou a lista mudar lá, a tela atualiza aqui.
    val saldo by viewModel.saldoTotal.collectAsState()
    val nomeUsuario by viewModel.nomeUsuario.collectAsState()

    // Lista das 5 últimas transações (Começa vazia para evitar erros de tipo)
    val ultimasTransacoes by viewModel.ultimasTransacoes.collectAsState(initial = emptyList())

    // Efeito Colateral: Recarrega os dados (saldo + lista) toda vez que entramos na tela
    LaunchedEffect(Unit) {
        viewModel.carregarDados()
    }

    // Estados locais para controle visual (mostrar saldo, pop-up de nome)
    var showBalance by remember { mutableStateOf(true) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }

    // --- POP-UP PARA EDITAR O NOME ---
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Como você quer ser chamado?") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Seu nome") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempName.isNotBlank()) {
                        viewModel.atualizarNome(tempName) // Salva
                        showEditNameDialog = false
                    }
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showEditNameDialog = false }) { Text("Cancelar") } }
        )
    }

    // --- ESTRUTURA DA TELA ---
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                // Permite rolar a tela se o conteúdo for maior que o display
                .verticalScroll(rememberScrollState())
        ) {

            // ============================================
            // 1. HERO CARD (Topo com Saldo e Nome)
            // ============================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp) // Altura ajustada para ficar mais compacto
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .fillMaxSize(),
                    // Centraliza o conteúdo verticalmente para agrupar Nome e Saldo
                    verticalArrangement = Arrangement.Center
                ) {

                    // LINHA 1: Nome e Avatar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Clique no topo abre edição de nome
                                tempName = nomeUsuario
                                showEditNameDialog = true
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Coluna do Texto (Nome)
                        // weight(1f) faz ela ocupar todo o espaço possível antes do ícone
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Olá,", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)

                            // Ajuste para nomes longos:
                            // 1. maxLines = 1 (Não quebra linha)
                            // 2. overflow = Ellipsis (Coloca "..." se não couber)
                            // 3. fontSize reduzido para 26.sp (Melhor ajuste)
                            Text(
                                text = nomeUsuario,
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Espaçamento entre o texto e o ícone
                        Spacer(modifier = Modifier.width(16.dp))

                        // Ícone de Perfil com fundo translúcido
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Perfil",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Espaço fixo entre o Nome e o Saldo (para não ficarem colados demais)
                    Spacer(modifier = Modifier.height(30.dp))

                    // LINHA 2: Saldo e Botão "Olho"
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Seu saldo total", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))

                            // Botão para esconder/mostrar saldo
                            IconButton(onClick = { showBalance = !showBalance }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = if (showBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Esconder saldo",
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Exibe o saldo formatado (R$) ou bolinhas de proteção
                        Text(
                            text = if (showBalance) saldo.toCurrency() else "R$ •••••",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ============================================
            // 2. AÇÕES RÁPIDAS (Grid de Botões)
            // ============================================
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Ações Rápidas",
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Linha com 4 botões de navegação
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActionButton(Icons.Default.Add, "Nova\nTransação") { navController.navigate(Screen.Registrar.route) }
                ActionButton(Icons.Default.History, "Ver\nHistórico") { navController.navigate(Screen.Historico.route) }
                ActionButton(Icons.Default.PieChart, "Resumo\nMensal") { navController.navigate(Screen.Resumo.route) }
                ActionButton(Icons.Default.Settings, "Categorias") { navController.navigate(Screen.Categorias.route) }
            }

            // ============================================
            // 3. ÚLTIMAS MOVIMENTAÇÕES (Lista Simplificada)
            // ============================================
            Spacer(modifier = Modifier.height(24.dp))

            // Cabeçalho da Lista + Botão "Ver todas"
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Últimas Movimentações", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                TextButton(onClick = { navController.navigate(Screen.Historico.route) }) {
                    Text("Ver todas")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Verifica se a lista está vazia para mostrar mensagem ou os itens
            if (ultimasTransacoes.isEmpty()) {
                Text(
                    text = "Nenhuma atividade recente.",
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                // Renderiza os cartões pequenos das últimas transações
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ultimasTransacoes.forEach { item ->
                        MiniTransacaoCard(transacao = item)
                    }
                }
            }

            // Espaço extra no rodapé para não colar na borda da tela
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ============================================
// COMPONENTES REUTILIZÁVEIS DA HOME
// ============================================

// Botão redondo das Ações Rápidas
@Composable
fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// Card pequeno e compacto para a lista da Home
@Composable
fun MiniTransacaoCard(transacao: TransacaoComCategoria) {
    val isReceita = transacao.transacao.tipo == TipoTransacao.RECEITA
    val icon = if (isReceita) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
    val color = if (isReceita) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Lado Esquerdo: Ícone + Textos
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = color.copy(alpha = 0.1f), modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = transacao.categoriaNome, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    // Descrição (com tratamento para texto vazio)
                    Text(
                        text = transacao.transacao.descricao?.takeIf { it.isNotBlank() } ?: "Sem descrição",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            // Lado Direito: Valor
            Text(
                text = transacao.transacao.valor.toCurrency(),
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 14.sp
            )
        }
    }
}