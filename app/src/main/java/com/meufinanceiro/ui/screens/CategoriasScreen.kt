package com.meufinanceiro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
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
fun CategoriasScreen(navController: NavController) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
    }
    val catRepo = remember { CategoriaRepository(db.categoriaDao()) }
    val transacaoRepo = remember { TransacaoRepository(db.transacaoDao()) }

    val viewModel: CategoriasViewModel = viewModel(factory = CategoriasViewModelFactory(catRepo, transacaoRepo))
    val listaComProgresso by viewModel.uiState.collectAsState()

    var novaCategoria by remember { mutableStateOf("") }
    var novaMeta by remember { mutableStateOf("") }
    var temMeta by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorias e Metas", fontWeight = FontWeight.Bold, color = colors.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar", tint = colors.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = CircleShape
            ) { Icon(Icons.Rounded.Add, null) }
        },
        containerColor = colors.background
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {

            if (listaComProgresso.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma categoria cadastrada", color = colors.onSurfaceVariant)
                }
            } else {
                Text(
                    "Suas Categorias",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = colors.onBackground
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(listaComProgresso) { item ->
                        CategoriaProgressoCard(item = item, onDelete = { viewModel.deletarCategoria(item.categoria) })
                    }
                }
            }
        }

        // DIALOG DE CRIAÇÃO
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                containerColor = colors.surface,
                title = { Text("Nova Categoria", color = colors.onSurface) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = novaCategoria,
                            onValueChange = { novaCategoria = it },
                            label = { Text("Nome (Ex: Lazer, Mercado)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.onSurface,
                                unfocusedTextColor = colors.onSurface,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline
                            )
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { temMeta = !temMeta }
                        ) {
                            Checkbox(
                                checked = temMeta,
                                onCheckedChange = { temMeta = it },
                                colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                            )
                            Text("Definir meta de gastos?", fontSize = 14.sp, color = colors.onSurface)
                        }

                        AnimatedVisibility(visible = temMeta) {
                            OutlinedTextField(
                                value = novaMeta,
                                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) novaMeta = it },
                                label = { Text("Valor da Meta (R$)") },
                                placeholder = { Text("0.00") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.onSurface,
                                    unfocusedTextColor = colors.onSurface,
                                    focusedBorderColor = colors.primary
                                )
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val nome = novaCategoria.trim()
                            if (nome.isNotEmpty()) {
                                val metaValor = if (temMeta) novaMeta.toDoubleOrNull() ?: 0.0 else 0.0
                                viewModel.adicionarCategoria(nome, metaValor)
                                novaCategoria = ""
                                novaMeta = ""
                                temMeta = false
                                showDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
                    ) { Text("Salvar") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancelar", color = colors.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
fun CategoriaProgressoCard(item: CategoriaComProgresso, onDelete: () -> Unit) {
    val categoria = item.categoria
    val temMeta = categoria.metaMensal > 0
    val colors = MaterialTheme.colorScheme

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        // USA A NOVA FUNÇÃO CORRIGIDA
                        Icon(
                            imageVector = getIconeCategoriaFinal(categoria.nome),
                            contentDescription = null,
                            tint = colors.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(text = categoria.nome, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        if (!temMeta) Text("Sem meta definida", fontSize = 11.sp, color = colors.onSurfaceVariant)
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = colors.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (temMeta) {
                LinearProgressIndicator(
                    progress = { item.progresso },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if(item.corStatus == Color.Red) colors.error else colors.primary,
                    trackColor = colors.surfaceVariant.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.totalGasto.toCurrency(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if(item.corStatus == Color.Red) colors.error else colors.primary)
                    Text("de ${categoria.metaMensal.toCurrency()}", fontSize = 12.sp, color = colors.onSurfaceVariant)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.TrendingUp, null, modifier = Modifier.size(14.dp), tint = colors.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Esta categoria não tem limite", fontSize = 12.sp, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

// --- FUNÇÃO FINAL E CORRIGIDA (CHECA OS NOMES GENÉRICOS) ---
private fun getIconeCategoriaFinal(nome: String): ImageVector {
    val n = nome.trim().lowercase()

    return when {
        // 1. ACADEMIA / ESPORTE (Prioridade para não confundir com Saúde)
        n.contains("academia") || n.contains("treino") || n.contains("fit") || n.contains("gym") || n.contains("esporte") || n.contains("cross") -> Icons.Rounded.FitnessCenter

        // 2. ASSINATURAS / STREAMING
        n.contains("assinatura") || n.contains("stream") || n.contains("netflix") || n.contains("spotify") || n.contains("prime") || n.contains("disney") -> Icons.Rounded.PlayCircle

        // 3. EDUCAÇÃO
        n.contains("educ") || n.contains("escola") || n.contains("faculdade") || n.contains("curso") || n.contains("estudo") || n.contains("livro") -> Icons.Rounded.School

        // 4. ALIMENTAÇÃO
        n.contains("aliment") || n.contains("refeic") || n.contains("restaurante") || n.contains("jantar") || n.contains("almoco") || n.contains("lanche") || n.contains("ifood") || n.contains("comida") || n.contains("mercado") || n.contains("compra") -> Icons.Rounded.Restaurant

        // 5. LAZER
        n.contains("lazer") || n.contains("diversao") || n.contains("entretenimento") || n.contains("cinema") || n.contains("filme") || n.contains("jogo") || n.contains("passeio") -> Icons.Rounded.LocalActivity

        // 6. MORADIA
        n.contains("moradia") || n.contains("habitacao") || n.contains("casa") || n.contains("aluguel") || n.contains("condominio") -> Icons.Rounded.Home

        // 7. TRANSPORTE
        n.contains("transporte") || n.contains("locomocao") || n.contains("veiculo") || n.contains("uber") || n.contains("taxi") || n.contains("onibus") || n.contains("combustivel") || n.contains("gasolina") || n.contains("carro") || n.contains("moto") -> Icons.Rounded.DirectionsCar

        // 8. CONTAS GERAIS
        n.contains("conta") || n.contains("boleto") || n.contains("pagamento") || n.contains("fatura") || n.contains("internet") || n.contains("luz") || n.contains("agua") || n.contains("celular") -> Icons.Rounded.ReceiptLong

        // 9. SAÚDE
        n.contains("saude") || n.contains("saúde") || n.contains("medico") || n.contains("hospital") || n.contains("farmacia") || n.contains("remedio") -> Icons.Rounded.LocalHospital

        // 10. TRABALHO / RENDA
        n.contains("free") || n.contains("salario") || n.contains("salário") || n.contains("renda") || n.contains("trabalho") || n.contains("provento") || n.contains("bico") -> Icons.Rounded.Work

        // 11. INVESTIMENTO
        n.contains("invest") || n.contains("poupanca") || n.contains("aplicacao") || n.contains("cdb") -> Icons.Rounded.TrendingUp

        // OUTROS
        n.contains("viagem") || n.contains("ferias") -> Icons.Rounded.Flight
        n.contains("roupa") || n.contains("vest") || n.contains("loja") -> Icons.Rounded.Checkroom
        n.contains("pet") || n.contains("animal") -> Icons.Rounded.Pets

        else -> Icons.Rounded.Category
    }
}