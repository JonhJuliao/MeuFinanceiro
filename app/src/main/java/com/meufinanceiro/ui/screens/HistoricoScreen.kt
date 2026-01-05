package com.meufinanceiro.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.meufinanceiro.ui.extensions.categoriaNome
import com.meufinanceiro.ui.extensions.toCurrency
import com.meufinanceiro.ui.extensions.toDateFormat
import com.meufinanceiro.ui.theme.AcessibilidadeApp
import com.meufinanceiro.ui.viewmodel.HistoricoFactory
import com.meufinanceiro.ui.viewmodel.HistoricoViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoScreen(navController: NavController) {

    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db")
            .addMigrations(AppDatabase.MIGRATION_1_2) // <--- OBRIGATÓRIO TER ISSO
            .build()
    }
    val repository = remember { TransacaoRepository(db.transacaoDao()) }
    val viewModel: HistoricoViewModel = viewModel(factory = HistoricoFactory(repository))

    val lista by viewModel.transacoes.collectAsState()

    var dataInicio by remember { mutableStateOf<Long?>(null) }
    var dataFim by remember { mutableStateOf<Long?>(null) }

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
        topBar = {
            TopAppBar(
                title = { Text("Histórico", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFFAFAFA)
                )
            )
        },
        containerColor = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFFAFAFA)
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxSize()
        ) {

            // SEÇÃO 1: FILTRO E BUSCA
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // BARRA DE PESQUISA
                    var textoBusca by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = textoBusca,
                        onValueChange = {
                            textoBusca = it
                            viewModel.filtrarPorTexto(it)
                        },
                        placeholder = { Text("Buscar (Ex: Uber, Lanche)") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FilterList, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Filtrar por período",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { showDatePicker { dataInicio = it } },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if(dataInicio != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if(dataInicio != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.CalendarToday, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(dataInicio?.toDateFormat() ?: "Início", fontSize = 12.sp)
                        }

                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { showDatePicker { dataFim = it } },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if(dataFim != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if(dataFim != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.CalendarToday, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(dataFim?.toDateFormat() ?: "Fim", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = dataInicio != null && dataFim != null,
                            onClick = { viewModel.filtrarPorPeriodo(dataInicio!!, dataFim!!) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        ) {
                            Text("Filtrar")
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                dataInicio = null
                                dataFim = null
                                viewModel.limparFiltro()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("Limpar")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SEÇÃO 2: LISTA
            if (lista.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhuma movimentação",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(lista) { item ->
                        TransacaoCard(
                            transacao = item,
                            onClick = { navController.navigate("registrar?id=${item.transacao.id}") },
                            onDelete = { viewModel.deletar(item.transacao.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransacaoCard(
    transacao: TransacaoComCategoria,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isReceita = transacao.transacao.tipo == TipoTransacao.RECEITA
    val isDark = isSystemInDarkTheme()

    val color = if (isReceita) AcessibilidadeApp.corReceita else AcessibilidadeApp.corDespesa
    val icon = if (isReceita) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) color.copy(alpha = 0.3f) else Color(0xFFE0E0E0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Coluna Central (Nome + Descrição + Pagamento)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transacao.categoriaNome,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!transacao.transacao.descricao.isNullOrBlank()) {
                    Text(
                        text = transacao.transacao.descricao,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Data e Ícone do Pagamento
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // --- CORREÇÃO AQUI: Usando a função auxiliar ---
                    Icon(
                        imageVector = getIconePagamento(transacao.transacao.metodoPagamento),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp), // Um pouco maior
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = transacao.transacao.dataMillis.toDateFormat(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Coluna Direita (Valor + Delete)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = transacao.transacao.valor.toCurrency(),
                    color = color,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Excluir transação",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// --- NOVA FUNÇÃO AUXILIAR (No final do arquivo) ---
@Composable
fun getIconePagamento(metodo: String): ImageVector {
    // Mapeia a String do banco para um Ícone Visual
    return when (metodo) {
        "CREDITO", "DEBITO" -> Icons.Rounded.CreditCard
        "PIX" -> Icons.Rounded.QrCode // Use QrCode ou Smartphone se preferir
        "DINHEIRO" -> Icons.Rounded.AttachMoney
        else -> Icons.Rounded.Payment // Ícone genérico se não achar
    }
}