package com.netsage.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val NetSagePageBackground = Color(0xFFF3F7FB)
val NetSageCardBackground = Color(0xFFFFFFFF)
val NetSageSoftHighlight = Color(0xFFEEF7FF)
val NetSageSoftHighlightBorder = Color(0xFFB4D8F4)

val NetSageHeroGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF101827), Color(0xFF183A5A), Color(0xFF2A7C98))
)

private val LightColors = lightColorScheme(
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

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7BC2E8),
    secondary = Color(0xFF8FD4EE)
)

@Composable
fun NetSageTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
