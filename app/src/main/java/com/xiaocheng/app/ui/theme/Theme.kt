package com.xiaocheng.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF18211F)
val Paper = Color(0xFFF7F4EE)
val CardWhite = Color(0xFFFFFFFF)
val Pine = Color(0xFF1F4D46)
val PineDeep = Color(0xFF14332E)
val Coral = Color(0xFFE07A5F)
val Amber = Color(0xFFE9B44C)
val Muted = Color(0xFF7B8A84)
val Line = Color(0xFFE7E1D5)
val SoftPine = Color(0xFFE8F0ED)
val SoftCoral = Color(0xFFFBEAE4)
val SoftAmber = Color(0xFFFBF1DE)
val Blue = Color(0xFF4A7C9B)
val Purple = Color(0xFF7B6CA8)
val SoftBlue = Color(0xFFE7EFF4)

fun categoryColor(category: String): Color = when (category) {
    "竞赛" -> Coral
    "讲座" -> Blue
    "活动" -> Amber
    "就业" -> Purple
    "志愿" -> Color(0xFF5B8C5A)
    "考试" -> Color(0xFFB44A3E)
    else -> Muted
}

fun categorySoft(category: String): Color = when (category) {
    "竞赛" -> SoftCoral
    "讲座" -> SoftBlue
    "活动" -> SoftAmber
    "就业" -> Color(0xFFEDEAF5)
    "志愿" -> Color(0xFFE8F1E7)
    "考试" -> Color(0xFFF8E6E3)
    else -> Color(0xFFEFEDE7)
}

private val colors = lightColorScheme(
    primary = Pine,
    onPrimary = CardWhite,
    primaryContainer = SoftPine,
    onPrimaryContainer = PineDeep,
    secondary = Coral,
    onSecondary = CardWhite,
    secondaryContainer = SoftCoral,
    onSecondaryContainer = Color(0xFF6B2D1F),
    tertiary = Amber,
    background = Paper,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = SoftPine,
    onSurfaceVariant = Muted,
    outline = Line,
    outlineVariant = Line,
    error = Color(0xFFB44A3E),
    onError = CardWhite,
)

private val typography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 34.sp, letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp, lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 23.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 25.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 15.sp,
    ),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun XiaoChengTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}