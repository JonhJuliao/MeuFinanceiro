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
import com.meufinanceiro.ui.theme.AcessibilidadeApp
import com.meufinanceiro.ui.viewmodel.HomeViewModel
import com.meufinanceiro.ui.viewmodel.HomeViewModelFactory
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme // Puxa do seu Theme.kt (Cores Cyber/Soft Dark)

    // Configuração do Banco
    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
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

    // ESTADO PARA O POP-UP DE PARCELAS
    var transacaoParaDetalhes by remember { mutableStateOf<TransacaoComCategoria?>(null) }

    val nomeExibicao = if (nomeUsuarioRaw.isBlank()) "Usuário" else nomeUsuarioRaw

    // Cálculos para o Card
    val despesasTotais = ultimasTransacoes
        .filter { it.transacao.tipo == TipoTransacao.DESPESA }
        .sumOf { it.transacao.valor }

    val maiorGasto = ultimasTransacoes
        .filter { it.transacao.tipo == TipoTransacao.DESPESA }
        .maxByOrNull { it.transacao.valor }

    // --- DIALOG DE PERFIL ---
    if (showProfileSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showProfileSettingsDialog = false },
            title = { Text("Configurar Perfil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Seu Nome") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.onSurface,
                            unfocusedTextColor = colors.onSurface
                        )
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (tempName.isNotBlank()) viewModel.atualizarNome(tempName)
                    showProfileSettingsDialog = false
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showProfileSettingsDialog = false }) { Text("Cancelar") } },
            containerColor = colors.surface,
            titleContentColor = colors.onSurface,
            textContentColor = colors.onSurfaceVariant
        )
    }

    // --- DIALOG DE PARCELAS ---
    if (transacaoParaDetalhes != null) {
        ParcelamentoDialog(
            transacao = transacaoParaDetalhes!!,
            onDismiss = { transacaoParaDetalhes = null }
        )
    }

    // --- TELA PRINCIPAL ---
    Scaffold(
        containerColor = colors.background, // Fundo ajustado (CyberBlack)
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.Registrar.route) },
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Nova Transação")
            }
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

            // 1. CARD DE SALDO TOTAL
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.clickable {
                            tempName = nomeUsuarioRaw
                            showProfileSettingsDialog = true
                        }) {
                            Text("Olá, $nomeExibicao", color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Saldo atual", color = colors.onSurfaceVariant, fontSize = 12.sp)
                        }

                        Icon(
                            imageVector = if (showBalance) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp).clickable { showBalance = !showBalance }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val corValor = if (saldo < 0) colors.error else colors.primary

                    Text(
                        text = if (showBalance) saldo.toCurrency() else "R$ •••••",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showBalance) corValor else colors.onSurface
                    )
                }
            }

            // 2. CARD DE RESUMO DO MÊS
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Resumo rápido", color = colors.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("Gastos", fontSize = 12.sp, color = colors.onSurfaceVariant)
                            Text(despesasTotais.toCurrency(), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = colors.primary)

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { 0.58f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = colors.primary,
                                trackColor = colors.onSurface.copy(alpha = 0.1f),
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(0.8f)) {
                            Text("Maior gasto", fontSize = 12.sp, color = colors.onSurfaceVariant)
                            if (maiorGasto != null) {
                                Text(maiorGasto.transacao.valor.toCurrency(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.onSurface)
                                Text(maiorGasto.categoriaNome, fontSize = 12.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            } else {
                                Text("-", fontWeight = FontWeight.Bold, color = colors.onSurface)
                            }
                        }
                    }
                }
            }

            // 3. AÇÕES RÁPIDAS
            Text(
                "Ações Rápidas",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colors.onBackground
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    BigActionButton(
                        icon = Icons.Rounded.History,
                        label = "Extrato",
                        modifier = Modifier.weight(1f),
                        colors = colors,
                        onClick = { navController.navigate(Screen.Historico.route) }
                    )
                }
                item {
                    BigActionButton(
                        icon = Icons.Rounded.PieChart,
                        label = "Resumo",
                        modifier = Modifier.weight(1f),
                        colors = colors,
                        onClick = { navController.navigate(Screen.Resumo.route) }
                    )
                }
                item {
                    BigActionButton(
                        icon = Icons.Rounded.Settings,
                        label = "Metas",
                        modifier = Modifier.weight(1f),
                        colors = colors,
                        onClick = { navController.navigate(Screen.Categorias.route) }
                    )
                }
            }

            // 4. LISTA DE MOVIMENTAÇÕES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Últimas Movimentações", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.onBackground)

                Text(
                    "Ver extrato >",
                    fontSize = 12.sp,
                    color = colors.primary,
                    modifier = Modifier.clickable { navController.navigate(Screen.Historico.route) }
                )
            }

            if (ultimasTransacoes.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhuma movimentação", color = colors.onSurfaceVariant)
                }
            } else {
                // Mostra apenas as 5 últimas na Home
                ultimasTransacoes.take(5).forEach { item ->
                    TransacaoItemStyleDark(
                        item = item,
                        colors = colors,
                        onClick = {
                            if (item.transacao.totalParcelas > 1) {
                                transacaoParaDetalhes = item
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// --- COMPONENTES VISUAIS ---

@Composable
fun BigActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    colors: ColorScheme,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(80.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = colors.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
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

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone Círculo
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

            Column(modifier = Modifier.weight(1f)) {
                Text(item.categoriaNome, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.onSurface)
                val desc = item.transacao.descricao
                if (!desc.isNullOrBlank()) {
                    Text(desc, fontSize = 12.sp, color = colors.onSurfaceVariant)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (item.transacao.totalParcelas > 1) {
                    Box(
                        modifier = Modifier
                            .background(colors.primaryContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Parcelado ${item.transacao.parcelaAtual}/${item.transacao.totalParcelas}", fontSize = 10.sp, color = colors.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = (if(isReceita) "+ " else "- ") + item.transacao.valor.toCurrency(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isReceita) colors.primary else colors.error
                )
            }
        }
    }
}

// --- POPUP DE PARCELAS ---
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
                Text(
                    text = transacao.categoriaNome,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = "Cronograma de Pagamento",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                listaParcelas.forEach { (numero, data) ->
                    val isPassada = numero < transacao.transacao.parcelaAtual
                    val isAtual = numero == transacao.transacao.parcelaAtual

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isAtual) colors.primary
                                        else if (isPassada) colors.primary.copy(alpha = 0.3f)
                                        else colors.onSurfaceVariant.copy(alpha = 0.2f)
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${numero}ª Parcela",
                                color = if (isAtual) colors.primary else colors.onSurface,
                                fontWeight = if (isAtual) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        Text(
                            text = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(data),
                            color = if (isAtual) colors.onSurface else colors.onSurfaceVariant,
                            fontWeight = if (isAtual) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (numero < listaParcelas.size) {
                        HorizontalDivider(color = colors.onSurface.copy(alpha = 0.1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = colors.primary)
            }
        }
    )
}

// Função de ícones (Expandida)
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