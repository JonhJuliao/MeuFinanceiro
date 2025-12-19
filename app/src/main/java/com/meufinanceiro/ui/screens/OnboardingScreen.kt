package com.meufinanceiro.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star


// 1. Modelo de dados para cada página do slide
data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit // Função que será chamada quando o usuário clicar em "Começar"
) {
    // 2. Definindo o conteúdo das 3 páginas
    val pages = listOf(
        OnboardingPage(
            "Controle Total",
            "Saiba exatamente para onde seu dinheiro está indo com gráficos simples.",
            Icons.Default.PieChart // Você pode trocar por outros ícones depois
        ),
        OnboardingPage(
            "Organize-se",
            "Categorize seus gastos e receitas para identificar onde economizar.",
            Icons.Default.List
        ),
        OnboardingPage(
            "Metas Financeiras",
            "Defina objetivos e acompanhe seu progresso mês a mês.",
            Icons.Default.Star
        )
    )

    // Estado do Pager (Controla qual página está visível)
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- ÁREA DO CARROSSEL (SLIDES) ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f) // Ocupa todo o espaço disponível
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        // --- INDICADORES DE PÁGINA (BOLINHAS) ---
        Row(
            modifier = Modifier.padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(10.dp)
                )
            }
        }

        // --- BOTÃO DE AÇÃO ---
        Button(
            onClick = {
                // Se não for a última página, avança. Se for, finaliza.
                if (pagerState.currentPage < pages.size - 1) {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    onFinish()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (pagerState.currentPage < pages.size - 1) {
                Text("Próximo")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            } else {
                Text("Começar Agora")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Check, contentDescription = null)
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        // Adicionando NavigationBarPadding para garantir que não fique atrás dos botões do Android
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ícone Gigante
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(150.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Título
        Text(
            text = page.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Descrição
        Text(
            text = page.description,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}