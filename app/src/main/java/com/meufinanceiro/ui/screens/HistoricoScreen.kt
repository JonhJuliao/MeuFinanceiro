package com.meufinanceiro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    // Define cores baseadas no tipo
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
            // Ícone indicativo (opcional, mas fica bonito)
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

            // Textos (Categoria e Descrição)
            Column(modifier = Modifier.weight(1f)) {
                // 1. NOME DA CATEGORIA (Destaque)
                Text(
                    text = transacao.categoriaNome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 2. DESCRIÇÃO (Se houver)
                if (!transacao.transacao.descricao.isNullOrBlank()) {
                    Text(
                        text = transacao.transacao.descricao,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3. DATA
                Text(
                    text = formatDate(transacao.transacao.dataMillis),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Valor e Botão Delete
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "R$ %.2f".format(transacao.transacao.valor),
                    color = valorColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
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