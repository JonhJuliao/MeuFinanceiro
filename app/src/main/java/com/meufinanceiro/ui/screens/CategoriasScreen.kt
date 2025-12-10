package com.meufinanceiro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
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

    // 1. Configuração do Banco e ViewModel
    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db").build()
    }
    val repository = remember { CategoriaRepository(db.categoriaDao()) }
    val viewModel: CategoriasViewModel = viewModel(
        factory = CategoriasViewModelFactory(repository)
    )

    // 2. Observa a lista real do banco de dados
    val listaCategorias by viewModel.categorias.collectAsState()

    var novaCategoria by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorias") },
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
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Campo + botão de adicionar
            OutlinedTextField(
                value = novaCategoria,
                onValueChange = { novaCategoria = it },
                label = { Text("Nova categoria") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val nome = novaCategoria.trim()
                    if (nome.isNotEmpty()) {
                        // Chama o ViewModel para salvar no banco
                        viewModel.adicionarCategoria(nome)
                        novaCategoria = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Adicionar", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de categorias vinda do Banco
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(listaCategorias) { categoria ->
                    CategoriaCard(
                        categoria = categoria,
                        onDelete = {
                            viewModel.deletarCategoria(categoria)
                        }
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
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = categoria.nome,
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir categoria",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}