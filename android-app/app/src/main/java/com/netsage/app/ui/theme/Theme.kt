package com.netsage.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.netsage.app.util.AppearanceSettings
import com.netsage.app.util.ThemeStyleOption

val NetSagePageBackground = Color(0xFFF3F7FB)
val NetSageCardBackground = Color(0xFFFFFFFF)
val NetSageSoftHighlight = Color(0xFFEEF7FF)
val NetSageSoftHighlightBorder = Color(0xFFB4D8F4)

val NetSageHeroGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF101827), Color(0xFF183A5A), Color(0xFF2A7C98))
)

private fun buildLightColors(themeStyle: ThemeStyleOption) = when (themeStyle) {
    ThemeStyleOption.DEFAULT -> lightColorScheme(
        primary = Color(0xFF16608A),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF2A7C98),
        background = NetSagePageBackground,
        surface = NetSageCardBackground,
        surfaceVariant = Color(0xFFE8F0F7),
        outlineVariant = Color(0xFFD5E0EA),
        onSurface = Color(0xFF16202A),
        onSurfaceVariant = Color(0xFF5C6B78)
    )
    ThemeStyleOption.FOREST -> lightColorScheme(
        primary = Color(0xFF2F6E4F),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF4B8D6B),
        background = Color(0xFFF4F8F4),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE7F0E8),
        outlineVariant = Color(0xFFD3E1D5),
        onSurface = Color(0xFF1B271F),
        onSurfaceVariant = Color(0xFF607164)
    )
    ThemeStyleOption.SUNSET -> lightColorScheme(
        primary = Color(0xFFB45B2A),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFFD48845),
        background = Color(0xFFFFF6F0),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFFCEBDD),
        outlineVariant = Color(0xFFF1D8C4),
        onSurface = Color(0xFF2F2119),
        onSurfaceVariant = Color(0xFF7A6356)
    )
}

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7BC2E8),
    secondary = Color(0xFF8FD4EE)
)

private fun scaleText(style: TextStyle, scale: Float, lineHeightScale: Float): TextStyle {
    val fontSize = if (style.fontSize != TextUnit.Unspecified) style.fontSize * scale else style.fontSize
    val lineHeight = if (style.lineHeight != TextUnit.Unspecified) style.lineHeight * scale * lineHeightScale else style.lineHeight
    return style.copy(fontSize = fontSize, lineHeight = lineHeight)
}

private fun buildTypography(scale: Float, lineHeightScale: Float): Typography {
    val base = Typography(
        displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
        displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
        displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 44.sp),
        headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
        headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.01.em),
        titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.em),
        bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.01.em),
        bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.01.em),
        bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.02.em),
        labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.em),
        labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.03.em),
        labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.03.em)
    )

    return Typography(
        displayLarge = scaleText(base.displayLarge, scale, lineHeightScale),
        displayMedium = scaleText(base.displayMedium, scale, lineHeightScale),
        displaySmall = scaleText(base.displaySmall, scale, lineHeightScale),
        headlineLarge = scaleText(base.headlineLarge, scale, lineHeightScale),
        headlineMedium = scaleText(base.headlineMedium, scale, lineHeightScale),
        headlineSmall = scaleText(base.headlineSmall, scale, lineHeightScale),
        titleLarge = scaleText(base.titleLarge, scale, lineHeightScale),
        titleMedium = scaleText(base.titleMedium, scale, lineHeightScale),
        titleSmall = scaleText(base.titleSmall, scale, lineHeightScale),
        bodyLarge = scaleText(base.bodyLarge, scale, lineHeightScale),
        bodyMedium = scaleText(base.bodyMedium, scale, lineHeightScale),
        bodySmall = scaleText(base.bodySmall, scale, lineHeightScale),
        labelLarge = scaleText(base.labelLarge, scale, lineHeightScale),
        labelMedium = scaleText(base.labelMedium, scale, lineHeightScale),
        labelSmall = scaleText(base.labelSmall, scale, lineHeightScale)
    )
}

@Composable
fun NetSageTheme(
    appearanceSettings: AppearanceSettings = AppearanceSettings(),
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else buildLightColors(appearanceSettings.themeStyle),
        typography = buildTypography(
            scale = appearanceSettings.fontScale.scale,
            lineHeightScale = appearanceSettings.layoutDensity.lineHeightScale
        ),
        content = content
    )
}
