package com.meufinanceiro.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.meufinanceiro.ui.extensions.toCurrency

// Modelo de dados simples apenas para essa tela (Visual)
data class GastoCategoriaUi(
    val nome: String,
    val valor: Double,
    val cor: Color,
    val porcentagem: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumoFinanceiroScreen(navController: NavController) {

    // --- DADOS MOCKADOS (SIMULAÇÃO) ---
    // Aqui você pode colocar cores que combinem com o seu tema
    val dadosGrafico = remember {
        listOf(
            GastoCategoriaUi("Alimentação", 650.00, Color(0xFFEF5350), 0.45f), // Vermelho
            GastoCategoriaUi("Transporte", 320.00, Color(0xFF42A5F5), 0.22f),  // Azul
            GastoCategoriaUi("Lazer", 210.00, Color(0xFFFFA726), 0.15f),       // Laranja
            GastoCategoriaUi("Saúde", 150.00, Color(0xFF66BB6A), 0.10f),       // Verde
            GastoCategoriaUi("Outros", 110.00, Color(0xFFAB47BC), 0.08f)       // Roxo
        )
    }

    val despesaTotal = dadosGrafico.sumOf { it.valor }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resumo Mensal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Botão decorativo de filtro de data
                    IconButton(onClick = { /* Nada por enquanto */ }) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = "Mudar Mês")
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
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // Título do Mês
            Text(
                text = "Dezembro, 2025",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- O GRÁFICO DE ROSCA (DONUT CHART) ---
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                // O Desenho do Gráfico
                DonutChartAnimado(dados = dadosGrafico)

                // O Texto no meio do buraco da rosca
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Total Gasto",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = despesaTotal.toCurrency(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- A LISTA DE LEGENDAS ---
            Text(
                text = "Detalhamento",
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

// COMPONENTE DO GRÁFICO (CANVAS)
@Composable
fun DonutChartAnimado(
    dados: List<GastoCategoriaUi>,
    espessura: Dp = 25.dp
) {
    // Animação de entrada (0% -> 100%)
    val animacaoProgresso = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animacaoProgresso.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000) // 1 segundo de animação
        )
    }

    Canvas(modifier = Modifier.size(200.dp)) {
        var anguloInicio = -90f // Começa do topo (12 horas)
        val diametro = size.minDimension
        val raio = diametro / 2

        // Desenha cada fatia
        dados.forEach { fatia ->
            val anguloVarredura = (fatia.porcentagem * 360f) * animacaoProgresso.value

            drawArc(
                color = fatia.cor,
                startAngle = anguloInicio,
                sweepAngle = anguloVarredura,
                useCenter = false, // false faz ser uma rosca (borda), true faria uma pizza cheia
                topLeft = Offset(espessura.toPx() / 2, espessura.toPx() / 2),
                size = Size(diametro - espessura.toPx(), diametro - espessura.toPx()),
                style = Stroke(width = espessura.toPx(), cap = StrokeCap.Round)
            )

            // Atualiza o ângulo de início para a próxima fatia começar onde essa terminou
            anguloInicio += anguloVarredura
        }
    }
}

// COMPONENTE DA LISTA (LEGENDA)
@Composable
fun ItemLegendaGrafico(item: GastoCategoriaUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Bolinha da cor
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(item.cor, CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = item.nome,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(item.porcentagem * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        Text(
            text = item.valor.toCurrency(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}