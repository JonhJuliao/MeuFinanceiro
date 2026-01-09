package com.meufinanceiro.ui.screens

import android.app.DatePickerDialog
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoricoScreen(navController: NavController) {

    val context = LocalContext.current
    val view = LocalView.current
    val colors = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
    }

    val repository = remember { TransacaoRepository(db.transacaoDao()) }
    val viewModel: HistoricoViewModel = viewModel(factory = HistoricoFactory(repository))

    val lista by viewModel.transacoes.collectAsState()

    var dataInicio by remember { mutableStateOf<Long?>(null) }
    var dataFim by remember { mutableStateOf<Long?>(null) }
    var isFiltroExpandido by remember { mutableStateOf(false) }

    fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day, 0, 0, 0)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = colors.inverseSurface,
                    contentColor = colors.inverseOnSurface,
                    actionColor = colors.primary,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Histórico", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {

            // --- CARD DE FILTROS ---
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var textoBusca by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = textoBusca,
                        onValueChange = { textoBusca = it; viewModel.filtrarPorTexto(it) },
                        placeholder = { Text("Buscar...", color = colors.onSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = colors.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isFiltroExpandido = !isFiltroExpandido }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.FilterList, null, tint = colors.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Filtros de Data", fontWeight = FontWeight.Bold, color = colors.primary)
                        }
                        Icon(if (isFiltroExpandido) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, tint = colors.onSurfaceVariant)
                    }

                    AnimatedVisibility(visible = isFiltroExpandido) {
                        Column {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = { showDatePicker { dataInicio = it } },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text(dataInicio?.toDateFormat() ?: "Início", fontSize = 12.sp) }

                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = { showDatePicker { dataFim = it } },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text(dataFim?.toDateFormat() ?: "Fim", fontSize = 12.sp) }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row {
                                Button(
                                    onClick = { viewModel.filtrarPorPeriodo(dataInicio!!, dataFim!!) },
                                    enabled = dataInicio != null && dataFim != null,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("Aplicar") }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { dataInicio = null; dataFim = null; viewModel.limparFiltro() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("Limpar") }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- LISTA DE TRANSAÇÕES ---
            if (lista.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma transação encontrada", color = colors.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = lista, key = { it.transacao.id }) { item ->

                        val currentItem by rememberUpdatedState(item)

                        // Estado local para controlar a animação DESTE item
                        var isVisible by remember { mutableStateOf(true) }

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    isVisible = false // Inicia animação de saída
                                    true
                                } else false
                            }
                        )

                        // --- CORREÇÃO DO BUG VERMELHO ---
                        // Esse código roda sempre que o item "nasce" na lista (inclusive ao restaurar).
                        // Ele força o estado a voltar para "Settled" (Normal), removendo o vermelho.
                        LaunchedEffect(Unit) {
                            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                        }

                        // Lógica de deleção
                        LaunchedEffect(isVisible) {
                            if (!isVisible) {
                                delay(300) // Espera a animação
                                val itemBackup = currentItem
                                viewModel.deletar(currentItem.transacao)

                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Transação removida",
                                        actionLabel = "DESFAZER",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restaurar(itemBackup)
                                    }
                                }
                            }
                        }

                        // Animação de "Encolher"
                        AnimatedVisibility(
                            visible = isVisible,
                            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)),
                            modifier = Modifier.animateItem()
                        ) {
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    // A cor é revelada pelo movimento do Swipe
                                    val color = colors.error

                                    val scale by animateFloatAsState(
                                        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1.2f else 0.8f,
                                        label = "iconScale"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(color)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            null,
                                            tint = colors.onError,
                                            modifier = Modifier.scale(scale)
                                        )
                                    }
                                }
                            ) {
                                TransacaoCardHistorico(
                                    transacao = item,
                                    onClick = {
                                        navController.navigate("registrar?id=${item.transacao.id}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransacaoCardHistorico(transacao: TransacaoComCategoria, onClick: () -> Unit) {
    val isReceita = transacao.transacao.tipo == TipoTransacao.RECEITA
    val colors = MaterialTheme.colorScheme

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val icon = if (isReceita) Icons.Rounded.ArrowUpward else getIconePagamento(transacao.transacao.metodoPagamento)
                Icon(icon, null, tint = colors.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transacao.categoriaNome,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val info = if(transacao.transacao.totalParcelas > 1)
                        "Parcela ${transacao.transacao.parcelaAtual}/${transacao.transacao.totalParcelas}"
                    else
                        transacao.transacao.descricao?.ifBlank { null } ?: transacao.transacao.metodoPagamento

                    Text(
                        text = info.toString().lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                    Text("•", fontSize = 12.sp, color = colors.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = transacao.transacao.dataMillis.toDateFormat(),
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = transacao.transacao.valor.toCurrency(),
                fontWeight = FontWeight.Bold,
                color = if (isReceita) colors.primary else colors.error,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun getIconePagamento(metodo: String): ImageVector {
    // Converte para maiúsculo para garantir que "Pix", "pix" e "PIX" funcionem
    return when (metodo.uppercase()) {
        "CREDITO", "CRÉDITO" -> Icons.Rounded.CreditCard
        "DEBITO", "DÉBITO" -> Icons.Rounded.CreditCard
        // Pix: Troquei para 'AccountBalance' (Banco) ou 'ElectricBolt' (Raio/Instantâneo).
        // Escolha o que preferir. Eu deixei AccountBalance que é mais elegante.
        "PIX" -> Icons.Rounded.AccountBalance
        "DINHEIRO" -> Icons.Rounded.AttachMoney
        else -> Icons.Rounded.Payment
    }
}