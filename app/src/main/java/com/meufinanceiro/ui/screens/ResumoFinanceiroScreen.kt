package com.meufinanceiro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.room.Room
import com.meufinanceiro.backend.db.AppDatabase
import com.meufinanceiro.backend.repository.TransacaoRepository
import com.meufinanceiro.backend.service.ResumoFinanceiroService
import com.meufinanceiro.ui.extensions.toCurrency
import com.meufinanceiro.ui.viewmodel.ResumoFinanceiroFactory
import com.meufinanceiro.ui.viewmodel.ResumoFinanceiroViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumoFinanceiroScreen(navController: NavController) {

    val context = LocalContext.current

    // 1. CONSTRUÇÃO DA ARQUITETURA (Injeção de Dependências)
    // Aqui montamos a estrutura completa necessária para o ViewModel funcionar.

    // Passo A: Banco de Dados
    val db = remember {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "meu_financeiro.db"
        ).build()
    }

    // Passo B: Service (Lógica de Negócio)
    // O Service precisa do Repository, que precisa do DAO do banco.
    val service = remember {
        ResumoFinanceiroService(
            transacaoRepository = TransacaoRepository(
                dao = db.transacaoDao()
            )
        )
    }

    // Passo C: ViewModel
    // Usamos a Factory para entregar o Service pronto para o ViewModel.
    val viewModel: ResumoFinanceiroViewModel = viewModel(
        factory = ResumoFinanceiroFactory(service)
    )

    // 2. OBSERVANDO OS ESTADOS (Reatividade)
    // A tela fica "escutando" as mudanças nos valores calculados pelo ViewModel.
    // Se você adicionar uma despesa nova, o 'despesas' muda e a tela atualiza sozinha.
    val receitas by viewModel.totalReceitas.collectAsState()
    val despesas by viewModel.totalDespesas.collectAsState()
    val saldo by viewModel.saldo.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resumo Financeiro") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { padding ->

        // 3. ORGANIZAÇÃO VISUAL (Coluna Vertical)
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp) // Espaço entre os cartões
        ) {

            // Cartão de Receitas (Verde/Azul)
            CardResumo(
                titulo = "Receitas",
                valor = receitas,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                textColor = MaterialTheme.colorScheme.primary
            )

            // Cartão de Despesas (Vermelho)
            CardResumo(
                titulo = "Despesas",
                valor = despesas,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                textColor = MaterialTheme.colorScheme.error
            )

            // Cartão de Saldo (Dinâmico)
            // Lógica Visual: Se o saldo for positivo, usa a cor Primária.
            // Se for negativo (devedor), usa a cor de Erro (Vermelho).
            CardResumo(
                titulo = "Saldo",
                valor = saldo,
                color = if (saldo >= 0)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else
                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                textColor = if (saldo >= 0)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

// 4. COMPONENTE REUTILIZÁVEL
// Em vez de repetir o código do Card 3 vezes lá em cima, criamos
// uma função que desenha o card baseada nos parâmetros.
// Isso deixa o código mais limpo e profissional (Princípio DRY - Don't Repeat Yourself).
@Composable
fun CardResumo(
    titulo: String,
    valor: Double,
    color: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {

            Text(
                text = titulo,
                fontSize = 20.sp,
                color = textColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Usa a extensão .toCurrency() que criamos para formatar (R$ 1.000,00)
            Text(
                text = valor.toCurrency(),
                fontSize = 26.sp,
                color = textColor
            )
        }
    }
}