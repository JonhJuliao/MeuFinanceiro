package com.meufinanceiro.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.room.Room
import com.meufinanceiro.backend.db.AppDatabase
import com.meufinanceiro.backend.repository.CategoriaRepository
import com.meufinanceiro.backend.repository.TransacaoRepository
import com.meufinanceiro.ui.extensions.toCurrency
import com.meufinanceiro.ui.viewmodel.CategoriaComProgresso
import com.meufinanceiro.ui.viewmodel.CategoriasViewModel
import com.meufinanceiro.ui.viewmodel.CategoriasViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db")
            .addMigrations(AppDatabase.MIGRATION_1_2) // <--- OBRIGATÓRIO TER ISSO
            .build()
    }
    // Precisamos dos dois repos agora
    val catRepo = remember { CategoriaRepository(db.categoriaDao()) }
    val transacaoRepo = remember { TransacaoRepository(db.transacaoDao()) }

    val viewModel: CategoriasViewModel = viewModel(
        factory = CategoriasViewModelFactory(catRepo, transacaoRepo)
    )

    val listaComProgresso by viewModel.uiState.collectAsState()

    var novaCategoria by remember { mutableStateOf("") }
    var novaMeta by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorias e Metas", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
                .fillMaxSize()
        ) {

            // --- CARTÃO DE CRIAÇÃO (Mais compacto e elegante) ---
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Nova Categoria",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Nome (Peso maior)
                        OutlinedTextField(
                            value = novaCategoria,
                            onValueChange = { novaCategoria = it },
                            label = { Text("Nome") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Meta (Peso menor)
                        OutlinedTextField(
                            value = novaMeta,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) novaMeta = it },
                            label = { Text("Meta R$") },
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val nome = novaCategoria.trim()
                            if (nome.isNotEmpty()) {
                                val metaValor = novaMeta.toDoubleOrNull() ?: 0.0
                                viewModel.adicionarCategoria(nome, metaValor)
                                novaCategoria = ""
                                novaMeta = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adicionar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- LISTA ---
            Text(
                text = "Metas Definidas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(listaComProgresso) { item ->
                    CategoriaProgressoCard(
                        item = item,
                        onDelete = { viewModel.deletarCategoria(item.categoria) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoriaProgressoCard(
    item: CategoriaComProgresso,
    onDelete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val categoria = item.categoria

    // Se não tiver meta definida, mostra layout simples
    val temMeta = categoria.metaMensal > 0

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.1f) else Color(0xFFEEEEEE)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // LINHA 1: Ícone, Nome e Lixeira
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Ícone com Inicial
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                // Se estourou a meta, o fundo do ícone fica da cor de alerta
                                if (temMeta && item.totalGasto > categoria.metaMensal) item.corStatus.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = categoria.nome.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = if (temMeta && item.totalGasto > categoria.metaMensal) item.corStatus else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = categoria.nome,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }
            }

            // Se tiver meta, mostra a barra de progresso
            if (temMeta) {
                Spacer(modifier = Modifier.height(12.dp))

                // BARRA DE PROGRESSO
                LinearProgressIndicator(
                    progress = { item.progresso }, // Passa o valor aqui dentro da lambda
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = item.corStatus,
                    trackColor = if (isDark) Color.DarkGray else Color(0xFFE0E0E0),
                )

                Spacer(modifier = Modifier.height(6.dp))

                // TEXTOS ABAIXO DA BARRA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.totalGasto.toCurrency(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = item.corStatus // Cor do valor gasto acompanha o status
                    )

                    Text(
                        text = "Meta: ${categoria.metaMensal.toCurrency()}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            } else {
                // Sem meta, apenas mostra "Sem meta definida" discreto
                Text(
                    text = "Sem meta mensal",
                    fontSize = 11.sp,
                    color = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 48.dp, top = 4.dp)
                )
            }
        }
    }
}