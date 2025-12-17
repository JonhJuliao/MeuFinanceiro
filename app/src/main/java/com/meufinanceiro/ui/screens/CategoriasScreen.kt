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
// Importante para usar a delegação "by" com States
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

    // 1. CONFIGURAÇÃO DO BANCO DE DADOS E VIEWMODEL
    // O 'remember' garante que o banco não seja recriado toda vez que a tela redesenha
    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db").build()
    }
    val repository = remember { CategoriaRepository(db.categoriaDao()) }

    // Cria o ViewModel usando a Factory (necessário para passar o repository como argumento)
    val viewModel: CategoriasViewModel = viewModel(
        factory = CategoriasViewModelFactory(repository)
    )

    // 2. OBSERVANDO OS DADOS
    // Converte o fluxo de dados do banco (Flow) em um Estado do Compose.
    // Assim, sempre que o banco mudar, a tela atualiza sozinha.
    val listaCategorias by viewModel.categorias.collectAsState()

    // Estado local para guardar o texto que o usuário digita no campo "Nova categoria"
    var novaCategoria by remember { mutableStateOf("") }

    // Scaffold fornece a estrutura padrão (Barra no topo + Conteúdo)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorias") },
                navigationIcon = {
                    // Botão de voltar para a Home
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding) // Respeita o espaço da TopBar
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp) // Espaço entre os itens da coluna
        ) {

            // --- CAMPO DE CADASTRO ---
            OutlinedTextField(
                value = novaCategoria,
                onValueChange = { novaCategoria = it }, // Atualiza o estado enquanto digita
                label = { Text("Nova categoria") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            // --- BOTÃO ADICIONAR ---
            Button(
                onClick = {
                    val nome = novaCategoria.trim() // Remove espaços em branco extras
                    if (nome.isNotEmpty()) {
                        // Chama o ViewModel para salvar no banco de dados
                        viewModel.adicionarCategoria(nome)
                        // Limpa o campo de texto após salvar
                        novaCategoria = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Adicionar", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- LISTAGEM (LazyColumn) ---
            // Usamos LazyColumn em vez de Column para listas que podem crescer,
            // pois ela é otimizada e tem rolagem automática.
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Itera sobre a lista que veio do banco
                items(listaCategorias) { categoria ->
                    CategoriaCard(
                        categoria = categoria,
                        onDelete = {
                            // Chama o ViewModel para deletar este item específico
                            viewModel.deletarCategoria(categoria)
                        }
                    )
                }
            }
        }
    }
}

// COMPONENTE VISUAL PARA CADA ITEM DA LISTA
@Composable
fun CategoriaCard(
    categoria: Categoria,
    onDelete: () -> Unit // Recebe uma função para executar quando clicar na lixeira
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
            horizontalArrangement = Arrangement.SpaceBetween, // Texto na esquerda, ícone na direita
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = categoria.nome,
                style = MaterialTheme.typography.titleMedium
            )

            // Botão de Deletar
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir categoria",
                    tint = MaterialTheme.colorScheme.error // Cor vermelha de erro
                )
            }
        }
    }
}