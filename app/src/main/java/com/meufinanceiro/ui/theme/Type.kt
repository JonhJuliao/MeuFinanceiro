package com.meufinanceiro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
import com.meufinanceiro.R

// ---------- FONT ----------
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold)
)

// ---------- BASE STYLE ----------
private val BaseTextStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontFeatureSettings = "tnum"
)

// ---------- TYPOGRAPHY ----------
val FinanceiroTypography = Typography(
    displayLarge = BaseTextStyle.copy(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 48.sp
    ),
    displayMedium = BaseTextStyle.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    titleMedium = BaseTextStyle.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    bodyLarge = BaseTextStyle.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodySmall = BaseTextStyle.copy(
        fontSize = 12.sp,
        color = Color.Gray
    ),
    labelLarge = BaseTextStyle.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    )
)
