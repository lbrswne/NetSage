package com.netsage.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netsage.app.ui.theme.NetSageHeroGradient
import com.netsage.app.util.AppearanceSettings
import com.netsage.app.util.FontScaleOption
import com.netsage.app.util.LayoutDensityOption
import com.netsage.app.util.ThemeModeOption
import com.netsage.app.util.ThemeStyleOption

private fun paletteFor(option: ThemeStyleOption): List<Color> = when (option) {
    ThemeStyleOption.DEFAULT -> listOf(Color(0xFF16608A), Color(0xFF2A7C98), Color(0xFFB4D8F4))
    ThemeStyleOption.FOREST -> listOf(Color(0xFF2F6E4F), Color(0xFF4B8D6B), Color(0xFFA9CBB4))
    ThemeStyleOption.SUNSET -> listOf(Color(0xFFB45B2A), Color(0xFFD48845), Color(0xFFF1D8C4))
}

@Composable
fun AppearanceSettingsScreen(
    settings: AppearanceSettings,
    onSelectFontScale: (FontScaleOption) -> Unit,
    onSelectThemeStyle: (ThemeStyleOption) -> Unit,
    onSelectThemeMode: (ThemeModeOption) -> Unit,
    onSelectLayoutDensity: (LayoutDensityOption) -> Unit,
    onResetDefaults: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack) { Text("返回") }
            OutlinedButton(onClick = onResetDefaults) { Text("恢复默认") }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .background(NetSageHeroGradient)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("NetSage", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
                Text("显示与风格", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("支持深色模式、字体、配色与界面紧凑度；修改后会立即生效并自动保存。", color = Color.White.copy(alpha = 0.84f), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("外观模式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ThemeModeOption.entries.forEach { option ->
                        AssistChip(
                            onClick = { onSelectThemeMode(option) },
                            label = { Text(option.label) },
                            border = if (settings.themeMode == option) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("字体大小", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("适合不同距离和阅读习惯。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FontScaleOption.entries.forEach { option ->
                        AssistChip(
                            onClick = { onSelectFontScale(option) },
                            label = { Text(option.label) },
                            border = if (settings.fontScale == option) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("整体配色", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("可切换主色调，色块可快速预览风格。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ThemeStyleOption.entries.forEach { option ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        AssistChip(
                            onClick = { onSelectThemeStyle(option) },
                            label = { Text(option.label) },
                            border = if (settings.themeStyle == option) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        )
                        paletteFor(option).forEach { color ->
                            Surface(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clip(CircleShape),
                                color = color,
                                shape = CircleShape
                            ) {
                                Text("  ", modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("界面密度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("紧凑模式更密集，舒展模式更易读。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    LayoutDensityOption.entries.forEach { option ->
                        AssistChip(
                            onClick = { onSelectLayoutDensity(option) },
                            label = { Text(option.label) },
                            border = if (settings.layoutDensity == option) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        )
                    }
                }
            }
        }
    }
}
