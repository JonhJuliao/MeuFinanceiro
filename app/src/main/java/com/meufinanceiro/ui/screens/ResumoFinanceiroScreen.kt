package com.meufinanceiro.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.room.Room
import com.meufinanceiro.backend.db.AppDatabase
import com.meufinanceiro.backend.repository.CategoriaRepository
import com.meufinanceiro.backend.repository.TransacaoRepository
import com.meufinanceiro.ui.extensions.toCurrency
import com.meufinanceiro.ui.theme.AcessibilidadeApp
import com.meufinanceiro.ui.viewmodel.ResumoFinanceiroViewModel
import com.meufinanceiro.ui.viewmodel.ResumoFinanceiroViewModelFactory

// Mantido aqui conforme seu código original
data class GastoCategoriaUi(
    val nome: String,
    val valor: Double,
    val cor: Color,
    val porcentagem: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumoFinanceiroScreen(navController: NavController) {
    val context = LocalContext.current

    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db")
            .addMigrations(AppDatabase.MIGRATION_1_2) // <--- OBRIGATÓRIO TER ISSO
            .build()
    }
    val transacaoRepo = remember { TransacaoRepository(db.transacaoDao()) }
    val categoriaRepo = remember { CategoriaRepository(db.categoriaDao()) }

    val viewModel: ResumoFinanceiroViewModel = viewModel(
        factory = ResumoFinanceiroViewModelFactory(transacaoRepo, categoriaRepo)
    )

    val state by viewModel.uiState.collectAsState()

    // --- NOVA LÓGICA DE ABAS ---
    var abaSelecionada by remember { mutableIntStateOf(0) }

    // Define qual lista mostrar baseada na aba
    val dadosGrafico = if (abaSelecionada == 0) state.listaPorCategoria else state.listaPorPagamento
    val despesaTotal = state.despesaTotal

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resumo Mensal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. AS ABAS (TABS)
            TabRow(
                selectedTabIndex = abaSelecionada,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = abaSelecionada == 0,
                    onClick = { abaSelecionada = 0 },
                    text = { Text("Por Categoria") }
                )
                Tab(
                    selected = abaSelecionada == 1,
                    onClick = { abaSelecionada = 1 },
                    text = { Text("Por Pagamento") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. CONTEÚDO (GRÁFICO E LISTA)
            if (dadosGrafico.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.DonutLarge,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nenhuma despesa registrada",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                // GRÁFICO
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(240.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Gráfico de rosca mostrando despesa total."
                        }
                ) {
                    // key(abaSelecionada) força a animação reiniciar quando troca a aba
                    key(abaSelecionada) {
                        DonutChartAnimado(dados = dadosGrafico)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Gasto",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = despesaTotal.toCurrency(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AcessibilidadeApp.corDespesa
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // TÍTULO DA LISTA
                Text(
                    text = if(abaSelecionada == 0) "Detalhamento por Categoria" else "Detalhamento por Pagamento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp, start = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(dadosGrafico) { item ->
                        ItemLegendaGrafico(item)
                    }
                }
            }
        }
    }
}

// ... (Mantenha DonutChartAnimado e ItemLegendaGrafico iguais, não mudaram)
@Composable
fun DonutChartAnimado(dados: List<GastoCategoriaUi>, espessura: Dp = 30.dp) {
    val animacaoProgresso = remember { Animatable(0f) }
    LaunchedEffect(dados) {
        animacaoProgresso.snapTo(0f)
        animacaoProgresso.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 1000))
    }
    Canvas(modifier = Modifier.size(220.dp)) {
        var anguloInicio = -90f
        val diametro = size.minDimension
        dados.forEach { fatia ->
            val anguloVarredura = (fatia.porcentagem * 360f) * animacaoProgresso.value
            drawArc(
                color = fatia.cor,
                startAngle = anguloInicio,
                sweepAngle = anguloVarredura,
                useCenter = false,
                topLeft = Offset(espessura.toPx() / 2, espessura.toPx() / 2),
                size = Size(diametro - espessura.toPx(), diametro - espessura.toPx()),
                style = Stroke(width = espessura.toPx(), cap = StrokeCap.Round)
            )
            anguloInicio += anguloVarredura
        }
    }
}

@Composable
fun ItemLegendaGrafico(item: GastoCategoriaUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).background(item.cor, CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = item.nome, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "${(item.porcentagem * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        Text(text = item.valor.toCurrency(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}