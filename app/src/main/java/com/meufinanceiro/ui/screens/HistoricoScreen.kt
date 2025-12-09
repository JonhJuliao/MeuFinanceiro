package com.meufinanceiro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.meufinanceiro.ui.viewmodel.HistoricoFactory
import com.meufinanceiro.ui.viewmodel.HistoricoViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoScreen(navController: NavController) {

    val context = LocalContext.current

    // DATABASE
    val db = remember {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "meu_financeiro.db"
        ).build()
    }

    // REPOSITORY REAL
    val repository = remember {
        TransacaoRepository(db.transacaoDao())
    }

    // VIEWMODEL
    val viewModel: HistoricoViewModel = viewModel(
        factory = HistoricoFactory(repository)
    )

    val lista by viewModel.transacoes.collectAsState()

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

            if (lista.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma transação registrada", fontSize = 18.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(lista) { item ->
                        TransacaoCard(
                            item,
                            // AQUI ESTAVA O ERRO: mudou de item.id para item.transacao.id
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
    onDelete: () -> Unit
) {
    val color = if (transacao.transacao.tipo == TipoTransacao.RECEITA)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else
        MaterialTheme.colorScheme.error.copy(alpha = 0.15f)

    val valorColor = if (transacao.transacao.tipo == TipoTransacao.RECEITA)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.error

    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .background(color)
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {
                Text(
                    text = transacao.transacao.descricao ?: transacao.categoriaNome,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = formatDate(transacao.transacao.dataMillis),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "R$ %.2f".format(transacao.transacao.valor),
                    color = valorColor,
                    fontSize = 18.sp
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

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}