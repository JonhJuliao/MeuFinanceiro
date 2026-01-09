package com.meufinanceiro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    // Configuração do Banco (Modo Seguro)
    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
    }
    val repository = remember { TransacaoRepository(db.transacaoDao()) }

    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(repository, context)
    )

    val saldo by viewModel.saldoTotal.collectAsState()
    val nomeUsuarioRaw by viewModel.nomeUsuario.collectAsState()
    val ultimasTransacoes by viewModel.ultimasTransacoes.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) { viewModel.carregarDados() }

    var showBalance by remember { mutableStateOf(true) }
    var showProfileSettingsDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var transacaoParaDetalhes by remember { mutableStateOf<TransacaoComCategoria?>(null) }

    val nomeExibicao = if (nomeUsuarioRaw.isBlank()) "Usuário" else nomeUsuarioRaw

    // --- CÁLCULOS INTELIGENTES ---
    val despesasTotais by remember(ultimasTransacoes) {
        derivedStateOf {
            ultimasTransacoes
                .filter { it.transacao.tipo == TipoTransacao.DESPESA }
                .sumOf { it.transacao.valor }
        }
    }

    val maiorGasto by remember(ultimasTransacoes) {
        derivedStateOf {
            ultimasTransacoes
                .filter { it.transacao.tipo == TipoTransacao.DESPESA }
                .maxByOrNull { it.transacao.valor }
        }
    }

    // Barra de progresso dinâmica
    val progressoBarra = remember(despesasTotais, saldo) {
        if (saldo <= 0) 1f else (despesasTotais / (despesasTotais + saldo)).toFloat().coerceIn(0f, 1f)
    }

    // --- DIALOGS ---
    if (showProfileSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showProfileSettingsDialog = false },
            title = { Text("Configurar Perfil") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Seu Nome") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (tempName.isNotBlank()) viewModel.atualizarNome(tempName)
                    showProfileSettingsDialog = false
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showProfileSettingsDialog = false }) { Text("Cancelar") } }
        )
    }

    if (transacaoParaDetalhes != null) {
        ParcelamentoDialog(transacao = transacaoParaDetalhes!!, onDismiss = { transacaoParaDetalhes = null })
    }

    // --- UI PRINCIPAL ---
    Scaffold(
        containerColor = colors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.Registrar.route) },
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = CircleShape
            ) { Icon(Icons.Rounded.Add, "Nova Transação") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // CARD DE SALDO
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.clickable { tempName = nomeUsuarioRaw; showProfileSettingsDialog = true }) {
                            Text("Olá, $nomeExibicao", color = colors.onSurface, fontWeight = FontWeight.Bold)
                            Text("Saldo atual", color = colors.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Icon(
                            imageVector = if (showBalance) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.clickable { showBalance = !showBalance },
                            tint = colors.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (showBalance) saldo.toCurrency() else "R$ •••••",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showBalance && saldo < 0) colors.error else colors.primary
                    )
                }
            }

            // CARD DE RESUMO
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Resumo rápido", color = colors.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("Gastos", fontSize = 12.sp, color = colors.onSurfaceVariant)
                            Text(despesasTotais.toCurrency(), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progressoBarra },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = colors.primary,
                                trackColor = colors.onSurface.copy(alpha = 0.1f)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(0.8f)) {
                            Text("Maior gasto", fontSize = 12.sp, color = colors.onSurfaceVariant)
                            if (maiorGasto != null) {
                                Text(maiorGasto!!.transacao.valor.toCurrency(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(maiorGasto!!.categoriaNome, fontSize = 11.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            } else { Text("-", color = colors.onSurfaceVariant) }
                        }
                    }
                }
            }

            // AÇÕES RÁPIDAS
            Text("Ações Rápidas", fontWeight = FontWeight.Bold, color = colors.onBackground)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item { BigActionButton(Icons.Rounded.History, "Extrato", colors) { navController.navigate(Screen.Historico.route) } }
                item { BigActionButton(Icons.Rounded.PieChart, "Resumo", colors) { navController.navigate(Screen.Resumo.route) } }
                item { BigActionButton(Icons.Rounded.Settings, "Metas", colors) { navController.navigate(Screen.Categorias.route) } }
            }

            // LISTA DE MOVIMENTAÇÕES
            Text("Últimas Movimentações", fontWeight = FontWeight.Bold, color = colors.onBackground)
            if (ultimasTransacoes.isEmpty()) {
                Text("Nenhuma movimentação", modifier = Modifier.padding(16.dp), color = colors.onSurfaceVariant)
            } else {
                ultimasTransacoes.take(5).forEach { item ->
                    TransacaoItemStyleDark(item, colors) {
                        if (item.transacao.totalParcelas > 1) transacaoParaDetalhes = item
                    }
                }
            }
        }
    }
}

// ==========================================
// FUNÇÕES AUXILIARES E COMPONENTES
// ==========================================

@Composable
fun BigActionButton(icon: ImageVector, label: String, colors: ColorScheme, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.size(100.dp, 80.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
            Icon(icon, null, tint = colors.primary)
            Text(label, fontSize = 12.sp, color = colors.onSurface)
        }
    }
}

@Composable
fun TransacaoItemStyleDark(
    item: TransacaoComCategoria,
    colors: ColorScheme,
    onClick: () -> Unit
) {
    val isReceita = item.transacao.tipo == TipoTransacao.RECEITA

    // Formatação de data (simples)
    val dataFormatada = remember(item.transacao.dataMillis) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = item.transacao.dataMillis
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(cal.time)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp) // Espaçamento entre itens
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ÍCONE
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.onSurface.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconePorCategoria(item.categoriaNome),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // TEXTOS (MEIO) - AQUI ESTAVA O BUG!
            // Adicionei maxLines = 1 e Ellipsis para cortar texto grande
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.categoriaNome,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val desc = item.transacao.descricao
                if (!desc.isNullOrBlank()) {
                    Text(
                        text = desc,
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 1, // <--- IMPORTANTE: Não deixa quebrar linha
                        overflow = TextOverflow.Ellipsis // <--- IMPORTANTE: Coloca "..."
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // VALOR E DATA (DIREITA)
            Column(horizontalAlignment = Alignment.End) {
                if (item.transacao.totalParcelas > 1) {
                    Box(
                        modifier = Modifier
                            .background(colors.primaryContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Parcelado ${item.transacao.parcelaAtual}/${item.transacao.totalParcelas}",
                            fontSize = 10.sp,
                            color = colors.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = (if(isReceita) "+ " else "- ") + item.transacao.valor.toCurrency(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isReceita) colors.primary else colors.error
                )

                Text(
                    text = dataFormatada,
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ParcelamentoDialog(
    transacao: TransacaoComCategoria,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val listaParcelas = remember(transacao) {
        val lista = mutableListOf<Pair<Int, Long>>()
        val cal = Calendar.getInstance()
        cal.timeInMillis = transacao.transacao.dataMillis

        val mesesParaVoltar = transacao.transacao.parcelaAtual - 1
        cal.add(Calendar.MONTH, -mesesParaVoltar)

        for (i in 1..transacao.transacao.totalParcelas) {
            lista.add(Pair(i, cal.timeInMillis))
            cal.add(Calendar.MONTH, 1)
        }
        lista
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Column {
                Text(transacao.categoriaNome, fontWeight = FontWeight.Bold, color = colors.onSurface)
                Text("Cronograma", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                listaParcelas.forEach { (numero, data) ->
                    val isAtual = numero == transacao.transacao.parcelaAtual
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${numero}ª Parcela", color = if (isAtual) colors.primary else colors.onSurface, fontWeight = if (isAtual) FontWeight.Bold else FontWeight.Normal)
                        Text(SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(data), color = colors.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

private fun getIconePorCategoria(nome: String): ImageVector {
    val nomeLimpo = nome.trim().lowercase()
    return when {
        nomeLimpo.contains("mercado") || nomeLimpo.contains("compras") -> Icons.Rounded.ShoppingCart
        nomeLimpo.contains("uber") || nomeLimpo.contains("transporte") || nomeLimpo.contains("carro") -> Icons.Rounded.DirectionsCar
        nomeLimpo.contains("ifood") || nomeLimpo.contains("restaurante") || nomeLimpo.contains("lanche") || nomeLimpo.contains("pizza") -> Icons.Rounded.Restaurant
        nomeLimpo.contains("internet") || nomeLimpo.contains("wifi") || nomeLimpo.contains("celular") -> Icons.Rounded.Wifi
        nomeLimpo.contains("casa") || nomeLimpo.contains("luz") || nomeLimpo.contains("aluguel") -> Icons.Rounded.Home
        nomeLimpo.contains("viagem") || nomeLimpo.contains("férias") || nomeLimpo.contains("hotel") -> Icons.Rounded.Flight
        nomeLimpo.contains("lazer") || nomeLimpo.contains("cinema") -> Icons.Rounded.Movie
        nomeLimpo.contains("saude") || nomeLimpo.contains("farmacia") -> Icons.Rounded.LocalPharmacy
        else -> Icons.Rounded.Category
    }
}