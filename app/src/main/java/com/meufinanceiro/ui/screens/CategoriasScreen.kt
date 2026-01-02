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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.meufinanceiro.backend.model.Categoria
import com.meufinanceiro.backend.repository.CategoriaRepository
import com.meufinanceiro.ui.extensions.toCurrency
import com.meufinanceiro.ui.viewmodel.CategoriasViewModel
import com.meufinanceiro.ui.viewmodel.CategoriasViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val db = remember { Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db").build() }
    val repository = remember { CategoriaRepository(db.categoriaDao()) }
    val viewModel: CategoriasViewModel = viewModel(factory = CategoriasViewModelFactory(repository))

    val listaCategorias by viewModel.categorias.collectAsState()

    // Estados do Formulário
    var novaCategoria by remember { mutableStateOf("") }
    var novaMeta by remember { mutableStateOf("") } // String para input

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
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- CARTÃO DE CRIAÇÃO ---
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Nova Categoria",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Campo Nome
                    OutlinedTextField(
                        value = novaCategoria,
                        onValueChange = { novaCategoria = it },
                        label = { Text("Nome (Ex: Mercado)") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Tag, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Campo Meta (Novo)
                    OutlinedTextField(
                        value = novaMeta,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) novaMeta = it },
                        label = { Text("Meta Mensal (Opcional)") },
                        placeholder = { Text("Ex: 500.00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Rounded.AttachMoney, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            val nome = novaCategoria.trim()
                            if (nome.isNotEmpty()) {
                                val metaValor = novaMeta.toDoubleOrNull() ?: 0.0
                                viewModel.adicionarCategoria(nome, metaValor) // <--- Agora passa a meta
                                novaCategoria = ""
                                novaMeta = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Criar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- LISTA ---
            Text(
                text = "Metas Definidas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(listaCategorias) { categoria ->
                    CategoriaComMetaCard(
                        categoria = categoria,
                        onDelete = { viewModel.deletarCategoria(categoria) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoriaComMetaCard(
    categoria: Categoria,
    onDelete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

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
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Ícone com Inicial
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = categoria.nome.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = categoria.nome,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Mostra a meta se for maior que zero
                    if (categoria.metaMensal > 0) {
                        Text(
                            text = "Meta: ${categoria.metaMensal.toCurrency()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Text(
                            text = "Sem meta definida",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}