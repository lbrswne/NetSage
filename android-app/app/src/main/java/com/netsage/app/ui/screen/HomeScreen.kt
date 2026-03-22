package com.netsage.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netsage.app.ui.theme.NetSageHeroGradient
import com.netsage.app.ui.theme.NetSagePageBackground
import com.netsage.app.ui.theme.NetSageSoftHighlight
import com.netsage.app.ui.theme.NetSageSoftHighlightBorder

data class HomeModule(
    val title: String,
    val desc: String,
    val onClick: () -> Unit
)

private data class OverviewStat(
    val label: String,
    val value: String,
    val hint: String,
)

@Composable
private fun HomeBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun OverviewCard(stat: OverviewStat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(stat.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stat.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stat.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FeaturedModuleCard(module: HomeModule, accent: Brush) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .clickable { module.onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(accent)
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                Text("核心入口", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(module.title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(module.desc, color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun UtilityModuleCard(module: HomeModule) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .clickable { module.onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(module.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(module.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    modules: List<HomeModule>,
    latestRecordSummary: String?,
    savedReportCount: Int,
    historyCount: Int,
    onQuickOpenInput: () -> Unit,
    onQuickOpenHistory: () -> Unit,
    onQuickOpenReference: () -> Unit,
    onReuseLatestRecord: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenUserAgreement: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    val featured = modules.take(2)
    val others = modules.drop(2)
    val overviewStats = listOf(
        OverviewStat("收藏诊断", savedReportCount.toString(), if (savedReportCount > 0) "可回看重点结果" else "尚未收藏结果"),
        OverviewStat("历史记录", historyCount.toString(), if (historyCount > 0) "支持继续回填分析" else "等待首次诊断沉淀"),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetSagePageBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = Color.Transparent,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(brush = NetSageHeroGradient)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("NetSage", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
                Text(
                    "离线网络诊断控制台",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "单机版网络问题工作台：默认本地处理与本地保存，适合快速诊断、复盘与知识查阅。",
                    color = Color.White.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.bodyMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HomeBadge("单机版")
                    HomeBadge("无需登录")
                    HomeBadge("无需联网")
                    HomeBadge("本地处理")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onQuickOpenInput, modifier = Modifier.weight(1f)) {
                        Text("开始诊断")
                    }
                    OutlinedButton(
                        onClick = onQuickOpenHistory,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.38f))
                    ) {
                        Text("查看历史", color = Color.White)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            overviewStats.forEach { stat ->
                Box(modifier = Modifier.weight(1f)) {
                    OverviewCard(stat)
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("常用操作", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = onQuickOpenInput, label = { Text("去诊断") })
                    AssistChip(onClick = onQuickOpenHistory, label = { Text("看历史") })
                    AssistChip(onClick = onQuickOpenReference, label = { Text("查术语") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "查看《用户协议》",
                        modifier = Modifier.clickable { onOpenUserAgreement() },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "查看《隐私政策》",
                        modifier = Modifier.clickable { onOpenPrivacyPolicy() },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        latestRecordSummary?.let { summary ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReuseLatestRecord() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NetSageSoftHighlight),
                border = BorderStroke(1.dp, NetSageSoftHighlightBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("最近一次诊断", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onReuseLatestRecord) { Text("继续诊断") }
                        OutlinedButton(onClick = onOpenHistory) { Text("查看历史") }
                    }
                }
            }
        }

        Text("核心功能", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(168.dp)
        ) {
            items(featured) { module ->
                FeaturedModuleCard(
                    module = module,
                    accent = NetSageHeroGradient
                )
            }
        }

        Text("工具与记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(top = 2.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(others) { module ->
                UtilityModuleCard(module)
            }
        }

        Text(
            "当前首页 / 输入页 / 结果页已收口到统一主题令牌，后续可继续把其余页面接入同一视觉系统。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
