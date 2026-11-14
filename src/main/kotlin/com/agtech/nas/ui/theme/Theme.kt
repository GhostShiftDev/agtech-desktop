package com.agtech.nas.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Navy900 = Color(0xFF0F1A2C)
val Navy800 = Color(0xFF18304C)
val Navy700 = Color(0xFF294E67)
val Blue500 = Color(0xFF134A84)
val Cyan400 = Color(0xFF4FC4F6)
val Cyan300 = Color(0xFF70EEFD)
val Cyan200 = Color(0xFF80C7CE)
val Mint300 = Color(0xFF8CFBD0)
val TextPrimary = Color(0xFFE8F4FF)
val TextMuted = Color(0xFF9FBEDA)
val Danger = Color(0xFFFF8A80)
val GlassCard = Color(0xFF152C48)

val AppBackground = Brush.verticalGradient(
    colors = listOf(Navy900, Color(0xFF0B1A31), Color(0xFF091428), Navy900)
)
val HeaderGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF173A5C), Navy800, Color(0xFF123049), Color(0xFF1C4E6E))
)
val AccentGradient = Brush.horizontalGradient(listOf(Cyan400, Cyan300, Mint300))

private val AGTechScheme = darkColorScheme(
    primary = Cyan400,
    onPrimary = Navy900,
    primaryContainer = Navy700,
    onPrimaryContainer = Cyan300,
    secondary = Cyan200,
    onSecondary = Navy900,
    secondaryContainer = Navy800,
    onSecondaryContainer = Cyan200,
    tertiary = Mint300,
    onTertiary = Navy900,
    tertiaryContainer = Navy700,
    onTertiaryContainer = Mint300,
    background = Navy900,
    onBackground = TextPrimary,
    surface = Navy800,
    onSurface = TextPrimary,
    surfaceVariant = Navy700,
    onSurfaceVariant = TextMuted,
    outline = Blue500,
    error = Danger,
    onError = Navy900
)

private val AGTechShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

private val AGTechTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.3.sp)
)

@Composable
fun AGTechTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AGTechScheme,
        shapes = AGTechShapes,
        typography = AGTechTypography,
        content = content
    )
}
