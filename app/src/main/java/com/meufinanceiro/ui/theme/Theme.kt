package com.meufinanceiro.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp

// 1. SHAPES
val FinanceiroShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// 2. TEMA ESCURO
private val DarkColorScheme = darkColorScheme(
    primary = ElectricGreen,
    onPrimary = CyberBlack,
    primaryContainer = CyberSurface,
    onPrimaryContainer = ElectricGreen,

    // Ajuste para não ficar roxo no dark mode também
    secondary = ElectricGreen,
    secondaryContainer = CyberSurface,
    onSecondaryContainer = ElectricGreen,

    background = CyberBlack,
    onBackground = Color.White,

    surface = CyberSurface,
    onSurface = Color.White,

    // Removemos o roxo daqui também
    surfaceVariant = CyberSurface,
    onSurfaceVariant = Color.White,

    error = NeonError,
    onError = CyberBlack
)

// 3. TEMA CLARO (A CORREÇÃO DO LILÁS ESTÁ AQUI)
private val LightColorScheme = lightColorScheme(
    primary = ModernGreen,
    onPrimary = Color.White,
    primaryContainer = SoftMintGray, // Fundo suave (ex: toggle buttons)
    onPrimaryContainer = ModernGreen,

    secondary = ModernGreen,
    onSecondary = Color.White,
    secondaryContainer = SoftMintGray, // Fundo suave
    onSecondaryContainer = ModernGreen,

    background = TechWhite,
    onBackground = TechDarkText,

    surface = TechSurface,
    onSurface = TechDarkText,

    // AQUI MATAMOS O LILÁS:
    // Antes estava vazio (padrão roxo). Agora é nosso Cinza-Menta.
    surfaceVariant = SoftMintGray,
    onSurfaceVariant = TechDarkText,

    error = LightError,
    onError = Color.White
)

@Composable
fun MeuFinanceiroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            WindowCompat.getInsetsController(window, view).apply {
                // CORREÇÃO: Força ícones brancos na barra de status (pois o fundo é sempre verde escuro no topo)
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FinanceiroTypography, // Certifique-se que FinanceiroTypography existe no seu Type.kt
        shapes = FinanceiroShapes,
        content = content
    )
}