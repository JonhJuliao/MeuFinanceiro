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

// Este Composable é o "Gerente de Tráfego" do aplicativo.
// Ele decide qual tela deve ser mostrada com base na rota atual.
@Composable
fun AppNavHost(navController: NavHostController) {

    // NavHost: É um container vazio que vai sendo preenchido pelas telas.
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route // Define que o app começa na Home
    ) {

        // --- ROTA DA HOME ---
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }

        // --- ROTA DO HISTÓRICO ---
        composable(Screen.Historico.route) {
            HistoricoScreen(navController)
        }

        // --- ROTA DE REGISTRAR (A MAIS COMPLEXA) ---
        // Aqui usamos uma sintaxe parecida com URL de site: "registrar?id={id}"
        // O "?" indica que o parâmetro 'id' é OPCIONAL.
        composable(
            route = "registrar?id={id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType // O ID é um número longo (Long)
                    defaultValue = 0L       // IMPORTANTE: Se não passarmos nada, vale 0 (Novo Cadastro)
                }
            )
        ) { backStackEntry ->
            // Recupera o ID que veio na navegação
            val id = backStackEntry.arguments?.getLong("id") ?: 0L

            // Passa o ID para a tela.
            // Se for 0, a tela abre vazia. Se for > 0, a tela carrega os dados para editar.
            RegistrarScreen(navController, transacaoId = id)
        }
        // -------------------------------------------------

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