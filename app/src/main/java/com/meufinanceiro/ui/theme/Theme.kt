package com.meufinanceiro.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ---------- SHAPES ----------
val FinanceiroShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// ---------- DARK ----------
private val DarkColorScheme = darkColorScheme(
    primary = ElectricGreen,
    onPrimary = CyberBlack,
    background = CyberBlack,
    onBackground = Color.White,
    surface = CyberSurface,
    onSurface = Color.White,
    error = NeonError,
    onError = CyberBlack,
    surfaceTint = Color.Transparent
)

// ---------- LIGHT ----------
private val LightColorScheme = lightColorScheme(
    primary = ModernGreen,
    onPrimary = Color.White,
    primaryContainer = SoftMintGray,
    onPrimaryContainer = ModernGreen,
    background = TechWhite,
    onBackground = TechDarkText,
    surface = TechSurface,
    onSurface = TechDarkText,
    surfaceVariant = SoftMintGray,
    onSurfaceVariant = TechDarkText,
    error = LightError,
    onError = Color.White,
    surfaceTint = Color.Transparent
)

@Composable
fun MeuFinanceiroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
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
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FinanceiroTypography,
        shapes = FinanceiroShapes,
        content = content
    )
}
