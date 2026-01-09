package com.meufinanceiro.ui.screens

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.meufinanceiro.ui.viewmodel.ResumoFinanceiroViewModel
import com.meufinanceiro.ui.viewmodel.ResumoFinanceiroViewModelFactory
import kotlin.math.atan2
import kotlin.math.sqrt

// Mantido para compatibilidade
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
    val colors = MaterialTheme.colorScheme

    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
    }

    val viewModel: ResumoFinanceiroViewModel = viewModel(
        factory = ResumoFinanceiroViewModelFactory(
            TransacaoRepository(db.transacaoDao()),
            CategoriaRepository(db.categoriaDao())
        )
    )

    val state by viewModel.uiState.collectAsState()

    // Controle das Abas (Categoria vs Pagamento)
    var abaSelecionada by remember { mutableIntStateOf(0) }
    var indiceSelecionado by remember { mutableStateOf<Int?>(null) }

    // Define qual lista mostrar no gráfico com base na aba
    val dadosGrafico = if (abaSelecionada == 0) state.listaPorCategoria else state.listaPorPagamento

    // Reseta seleção ao trocar de aba ou mês
    LaunchedEffect(abaSelecionada, state.mesExibicao) { indiceSelecionado = null }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Resumo Financeiro", fontWeight = FontWeight.Bold, color = colors.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar", tint = colors.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // Scroll na tela toda
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- 1. NAVEGAÇÃO DE MÊS (NOVO) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.mesAnterior() }) {
                    Icon(Icons.Rounded.ChevronLeft, null, tint = colors.primary)
                }

                Text(
                    text = state.mesExibicao,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colors.onSurface
                )

                IconButton(onClick = { viewModel.proximoMes() }) {
                    Icon(Icons.Rounded.ChevronRight, null, tint = colors.primary)
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- 2. SALDO GIGANTE (NOVO) ---
            Text("Saldo do Mês", fontSize = 14.sp, color = colors.onSurfaceVariant)
            Text(
                text = state.saldo.toCurrency(),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (state.saldo >= 0) colors.primary else colors.error
            )

            Spacer(Modifier.height(24.dp))

            // --- 3. CARDS DE ENTRADA E SAÍDA (NOVO) ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ResumoHeaderCard(
                    titulo = "Entradas",
                    valor = state.totalReceitas,
                    corIcone = colors.primary,
                    icone = Icons.Rounded.ArrowUpward,
                    modifier = Modifier.weight(1f)
                )
                ResumoHeaderCard(
                    titulo = "Saídas",
                    valor = state.totalDespesas,
                    corIcone = colors.error,
                    icone = Icons.Rounded.ArrowDownward,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // --- 4. CARD DE COMPARAÇÃO (NOVO) ---
            if (state.comparacao.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Insights, null, tint = colors.onSurface, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(state.comparacao, fontSize = 13.sp, color = colors.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(24.dp))

            // --- 5. GRÁFICOS E DETALHES (ANTIGO, MAS ADAPTADO) ---

            // Título da seção
            Text("Detalhamento de Gastos", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.onBackground, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            // Se não tiver despesa, mostra estado vazio
            if (state.totalDespesas == 0.0) {
                EmptyState()
            } else {
                // Abas
                TabRow(
                    selectedTabIndex = abaSelecionada,
                    containerColor = colors.surface,
                    contentColor = colors.primary,
                    indicator = { tabPositions ->
                        if (tabPositions.isNotEmpty()) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[abaSelecionada]),
                                color = colors.primary
                            )
                        }
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(selected = abaSelecionada == 0, onClick = { abaSelecionada = 0 }, text = { Text("Categorias") })
                    Tab(selected = abaSelecionada == 1, onClick = { abaSelecionada = 1 }, text = { Text("Pagamentos") })
                }

                Spacer(Modifier.height(24.dp))

                // Gráfico Donut
                DonutSection(
                    dados = dadosGrafico,
                    indiceSelecionado = indiceSelecionado,
                    onSelect = { indiceSelecionado = if (indiceSelecionado == it) null else it },
                    totalGeral = state.totalDespesas
                )

                Spacer(Modifier.height(24.dp))

                // Insight do Maior Gasto
                val maior = dadosGrafico.maxByOrNull { it.valor }
                if (maior != null && indiceSelecionado == null) {
                    InsightCard(maior)
                    Spacer(Modifier.height(16.dp))
                }

                // Lista de Itens (Legenda)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    dadosGrafico.forEachIndexed { index, item ->
                        ItemLegendaInterativo(
                            item = item,
                            isSelected = indiceSelecionado == index,
                            onClick = { indiceSelecionado = if (indiceSelecionado == index) null else index }
                        )
                    }
                }

                // Espaço extra no fim para não cortar com botões do sistema
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// --- COMPONENTES AUXILIARES (NOVOS) ---

@Composable
fun ResumoHeaderCard(
    titulo: String,
    valor: Double,
    corIcone: Color,
    icone: ImageVector,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(corIcone.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icone, null, tint = corIcone, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(titulo, fontSize = 12.sp, color = colors.onSurfaceVariant)
            Text(
                valor.toCurrency(),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colors.onSurface
            )
        }
    }
}

// --- COMPONENTES VISUAIS (MANTIDOS E ADAPTADOS) ---

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.DonutLarge,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text("Sem despesas neste mês", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DonutSection(
    dados: List<GastoCategoriaUi>,
    indiceSelecionado: Int?,
    onSelect: (Int) -> Unit,
    totalGeral: Double
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        DonutChartInterativo(dados, indiceSelecionado, onSelect)

        val item = indiceSelecionado?.let { dados[it] }

        Spacer(Modifier.height(16.dp))
        Text(
            text = item?.nome ?: "Total gasto",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = (item?.valor ?: totalGeral).toCurrency(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = item?.cor ?: MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DonutChartInterativo(
    dados: List<GastoCategoriaUi>,
    indiceSelecionado: Int?,
    onSelect: (Int) -> Unit,
    espessura: Dp = 40.dp
) {
    val animacao = remember { Animatable(0f) }
    val density = LocalDensity.current
    val sizePx = with(density) { 240.dp.toPx() }

    LaunchedEffect(dados) {
        animacao.animateTo(1f, tween(800))
    }

    Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    if (event.action != MotionEvent.ACTION_DOWN) return@pointerInteropFilter false

                    val center = sizePx / 2
                    val dx = event.x - center
                    val dy = event.y - center
                    val distance = sqrt(dx * dx + dy * dy)

                    val radius = center
                    val innerRadius = radius - with(density) { espessura.toPx() }

                    if (distance !in innerRadius..radius) return@pointerInteropFilter false

                    var angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 90f
                    if (angle < 0) angle += 360f

                    var current = 0f
                    dados.forEachIndexed { index, fatia ->
                        val sweep = fatia.porcentagem * 360f
                        if (angle in current..(current + sweep)) {
                            onSelect(index)
                            return@pointerInteropFilter true
                        }
                        current += sweep
                    }
                    false
                }
        ) {
            var startAngle = -90f

            dados.forEachIndexed { index, fatia ->
                val selected = indiceSelecionado == index
                val sweep = fatia.porcentagem * 360f * animacao.value

                val stroke = if (selected) espessura.toPx() + 15 else espessura.toPx()
                val alpha = if (indiceSelecionado != null && !selected) 0.3f else 1f

                drawArc(
                    color = fatia.cor.copy(alpha = alpha),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
    }
}

@Composable
fun ItemLegendaInterativo(
    item: GastoCategoriaUi,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (isSelected) colors.surfaceVariant else colors.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(item.cor, CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(item.nome, fontWeight = FontWeight.Bold, color = colors.onSurface)
                Spacer(Modifier.weight(1f))
                Text(item.valor.toCurrency(), fontWeight = FontWeight.Bold, color = if(isSelected) item.cor else colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { item.porcentagem },
                color = item.cor,
                trackColor = colors.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
fun InsightCard(maior: GastoCategoriaUi) {
    val colors = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(colors.primary.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Insights, null, tint = colors.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Maior Despesa", fontSize = 12.sp, color = colors.onPrimaryContainer.copy(alpha = 0.8f))
                Text("${maior.nome} representa ${(maior.porcentagem * 100).toInt()}% do total", fontWeight = FontWeight.Bold, color = colors.onPrimaryContainer)
            }
        }
    }
}