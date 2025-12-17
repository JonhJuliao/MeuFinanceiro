package com.meufinanceiro.navigation

// A classe 'sealed' (selada) funciona como um "Super Enum".
// Ela serve para definir um conjunto restrito e fixo de opções.
// Neste caso, ela lista TODAS as telas que existem no aplicativo.
sealed class Screen(val route: String) {

    // Cada objeto abaixo representa uma tela e sua "URL" (rota) interna.
    // Usamos 'object' (Singleton) porque só precisamos de uma instância de cada definição.

    // Tela Principal
    object Home : Screen("home")

    // Tela de Cadastro/Edição
    // Nota: No AppNavHost, usamos "registrar?id={id}", mas a rota base é "registrar"
    object Registrar : Screen("registrar")

    // Tela de Listagem Completa
    object Historico : Screen("historico")

    // Tela de Gerenciamento de Categorias
    object Categorias : Screen("categorias")

    // Tela de Gráficos/Totais
    object Resumo : Screen("resumo")
}