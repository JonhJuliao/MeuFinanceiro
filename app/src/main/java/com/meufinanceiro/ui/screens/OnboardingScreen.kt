package com.meufinanceiro.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            "Controle Total",
            "Saiba exatamente para onde seu dinheiro está indo com gráficos inteligentes.",
            Icons.Rounded.DonutLarge
        ),
        OnboardingPage(
            "Organize Gastos",
            "Categorize suas despesas e receitas em segundos.",
            Icons.Rounded.TrendingUp
        ),
        OnboardingPage(
            "Economize Mais",
            "Defina metas e alcance sua liberdade financeira.",
            Icons.Rounded.Savings
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    // CORREÇÃO DE DESIGN: Fundo Gradiente (Verde Escuro -> Verde Claro)
    // Isso tira a cara de "tela vazia" e dá um ar de app de banco
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF00695C), // Verde Petróleo Escuro
            Color(0xFF4DB6AC)  // Verde Água
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.weight(0.2f))

            // SLIDES
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(0.6f)
            ) { pageIndex ->
                OnboardingPageContent(page = pages[pageIndex])
            }

            // INDICADORES (Bolinhas)
            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration)
                        Color.White
                    else
                        Color.White.copy(alpha = 0.4f)

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(if (pagerState.currentPage == iteration) 12.dp else 10.dp) // A ativa fica maior
                    )
                }
            }

            // BOTÃO BRANCO (Destaque)
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp), // Botão bem arredondado
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White, // Botão Branco
                    contentColor = Color(0xFF00695C) // Texto Verde Escuro
                ),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (pagerState.currentPage < pages.size - 1) "Próximo" else "Começar Agora",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (pagerState.currentPage < pages.size - 1) Icons.Rounded.ArrowForward else Icons.Rounded.Check,
                        contentDescription = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Círculo com ícone translúcido
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = page.title,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )
    }
}