package com.meufinanceiro.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip // Importante para arredondar a barra
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.room.Room
import com.meufinanceiro.backend.db.AppDatabase
import com.meufinanceiro.backend.model.Categoria
import com.meufinanceiro.backend.repository.CategoriaRepository
import com.meufinanceiro.ui.viewmodel.CategoriasViewModel
import com.meufinanceiro.ui.viewmodel.CategoriasViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(
    navController: NavController
) {
    val context = LocalContext.current

    val db = remember { Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db").build() }
    val repository = remember { CategoriaRepository(db.categoriaDao()) }
    val viewModel: CategoriasViewModel = viewModel(factory = CategoriasViewModelFactory(repository))

    val listaCategorias by viewModel.categorias.collectAsState()
    var novaCategoria by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Metas por Categoria", fontWeight = FontWeight.Bold) }, // Mudei o título para valorizar a feature
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Campo de Texto
            OutlinedTextField(
                value = novaCategoria,
                onValueChange = { novaCategoria = it },
                label = { Text("Nova categoria") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.List, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            // Botão Adicionar
            Button(
                onClick = {
                    val nome = novaCategoria.trim()
                    if (nome.isNotEmpty()) {
                        viewModel.adicionarCategoria(nome)
                        novaCategoria = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adicionar Meta", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista com visual novo (Prototipado)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(listaCategorias) { categoria ->
                    CategoriaCard(
                        categoria = categoria,
                        onDelete = { viewModel.deletarCategoria(categoria) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoriaCard(
    categoria: Categoria,
    onDelete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary

    // --- SIMULAÇÃO DE DADOS (MOCK) ---
    // Gerando valores aleatórios para a demonstração visual
    val meta = 500.00
    // O 'remember' segura o número aleatório para ele não ficar mudando enquanto você rola a tela
    val gastoAtual = remember { (50..600).random().toDouble() }
    val progresso = (gastoAtual / meta).toFloat().coerceIn(0f, 1f)

    // Define a cor: Vermelho se estourou a meta, Primária se está ok
    val corBarra = if (gastoAtual > meta) MaterialTheme.colorScheme.error else primaryColor
    val corTextoGasto = if (gastoAtual > meta) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) primaryColor.copy(alpha = 0.5f) else Color(0xFFE0E0E0)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            // Linha Superior: Nome e Botão de Excluir
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoria.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Excluir categoria",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de Progresso Visual
            LinearProgressIndicator(
                progress = progresso,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)), // Barra arredondada
                color = corBarra,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Textos de valores (Pequenininho embaixo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Gasto: R$ ${String.format("%.2f", gastoAtual)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = corTextoGasto,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Meta: R$ ${String.format("%.2f", meta)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}