package com.meufinanceiro.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.meufinanceiro.ui.screens.HomeScreen
import com.meufinanceiro.ui.screens.HistoricoScreen
import com.meufinanceiro.ui.screens.RegistrarScreen
import com.meufinanceiro.ui.screens.CategoriasScreen
import com.meufinanceiro.ui.screens.ResumoFinanceiroScreen
import com.meufinanceiro.ui.screens.OnboardingScreen // <--- Importei a tela nova

// Este Composable é o "Gerente de Tráfego" do aplicativo.
@Composable
fun AppNavHost(navController: NavHostController) {

    // NavHost: É um container vazio que vai sendo preenchido pelas telas.
    NavHost(
        navController = navController,
        // startDestination = Screen.Home.route  <-- ANTIGO
        startDestination = "onboarding"       // <-- NOVO: Começa pela apresentação
    ) {

        // --- NOVA ROTA: ONBOARDING (BOAS-VINDAS) ---
        composable("onboarding") {
            OnboardingScreen(
                onFinish = {
                    // Navega para a Home
                    navController.navigate(Screen.Home.route) {
                        // TRUQUE PRO: Remove a tela de Onboarding da pilha de voltar.
                        // Assim, se o usuário clicar "Voltar" na Home, ele sai do app
                        // e não volta para a tela de tutorial.
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // --- ROTA DA HOME ---
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }

        // --- ROTA DO HISTÓRICO ---
        composable(Screen.Historico.route) {
            HistoricoScreen(navController)
        }

        // --- ROTA DE REGISTRAR ---
        composable(
            route = "registrar?id={id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            RegistrarScreen(navController, transacaoId = id)
        }

        // --- ROTA DE CATEGORIAS ---
        composable(Screen.Categorias.route) {
            CategoriasScreen(navController)
        }

        // --- ROTA DE RESUMO ---
        composable(Screen.Resumo.route) {
            ResumoFinanceiroScreen(navController)
        }
    }
}