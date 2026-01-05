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
    val colors = MaterialTheme.colorScheme // TEMA ATIVO

    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }
    val catRepo = remember { CategoriaRepository(db.categoriaDao()) }
    val transacaoRepo = remember { TransacaoRepository(db.transacaoDao()) }

    val viewModel: CategoriasViewModel = viewModel(factory = CategoriasViewModelFactory(catRepo, transacaoRepo))
    val listaComProgresso by viewModel.uiState.collectAsState()

    var novaCategoria by remember { mutableStateOf("") }
    var novaMeta by remember { mutableStateOf("") }
    var temMeta by remember { mutableStateOf(false) }

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
        containerColor = colors.background
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            // CARD DE CRIAÇÃO
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Nova Categoria", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.onSurface)

                    OutlinedTextField(
                        value = novaCategoria,
                        onValueChange = { novaCategoria = it },
                        label = { Text("Nome (Ex: Mercado)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.background,
                            unfocusedContainerColor = colors.background,
                            focusedTextColor = colors.onSurface,
                            unfocusedTextColor = colors.onSurface,
                            cursorColor = colors.primary,
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { temMeta = !temMeta }
                    ) {
                        Checkbox(checked = temMeta, onCheckedChange = { temMeta = it }, colors = CheckboxDefaults.colors(checkedColor = colors.primary))
                        Text("Definir um limite de gastos (Meta)", fontSize = 14.sp, color = colors.onSurface)
                    }

                    AnimatedVisibility(visible = temMeta) {
                        OutlinedTextField(
                            value = novaMeta,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) novaMeta = it },
                            label = { Text("Valor da Meta (R$)") },
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.background,
                                unfocusedContainerColor = colors.background,
                                focusedTextColor = colors.onSurface,
                                unfocusedTextColor = colors.onSurface,
                                focusedBorderColor = colors.primary
                            )
                        )
                    }

                    Button(
                        onClick = {
                            val nome = novaCategoria.trim()
                            if (nome.isNotEmpty()) {
                                val metaValor = if (temMeta) novaMeta.toDoubleOrNull() ?: 0.0 else 0.0
                                viewModel.adicionarCategoria(nome, metaValor)
                                novaCategoria = ""
                                novaMeta = ""
                                temMeta = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Criar Categoria")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Suas Categorias", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp), color = colors.onBackground)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(listaComProgresso) { item ->
                    CategoriaProgressoCard(item = item, onDelete = { viewModel.deletarCategoria(item.categoria) })
                }
            }
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
                        Icon(imageVector = getIconeCategoriaLocal(categoria.nome), contentDescription = null, tint = colors.onSurfaceVariant)
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
                // Progresso com cor de fundo visível no dark mode
                LinearProgressIndicator(
                    progress = { item.progresso },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if(item.corStatus == Color.Red) colors.error else colors.primary, // Usa cores do tema
                    trackColor = colors.surfaceVariant,
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
                        .background(colors.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(14.dp), tint = colors.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Toque para definir uma meta", fontSize = 12.sp, color = colors.primary)
                }
            }
        }
    }
}

private fun getIconeCategoriaLocal(nome: String): ImageVector {
    val nomeLimpo = nome.trim().lowercase()
    return when {
        nomeLimpo.contains("mercado") || nomeLimpo.contains("compras") -> Icons.Rounded.ShoppingCart
        nomeLimpo.contains("uber") || nomeLimpo.contains("transporte") -> Icons.Rounded.DirectionsCar
        nomeLimpo.contains("ifood") || nomeLimpo.contains("restaurante") || nomeLimpo.contains("lanche") -> Icons.Rounded.Restaurant
        nomeLimpo.contains("internet") || nomeLimpo.contains("wifi") -> Icons.Rounded.Wifi
        nomeLimpo.contains("casa") || nomeLimpo.contains("luz") || nomeLimpo.contains("aluguel") -> Icons.Rounded.Home
        nomeLimpo.contains("saude") || nomeLimpo.contains("farmacia") -> Icons.Rounded.LocalPharmacy
        else -> Icons.Rounded.Category
    }
}