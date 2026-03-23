package com.netsage.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netsage.app.ui.theme.NetSageHeroGradient
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
    onQuickOpenSamples: () -> Unit,
    onQuickOpenReviewDemo: () -> Unit,
    onQuickOpenShotsGuide: () -> Unit,
    onQuickOpenScenarios: () -> Unit,
    onQuickOpenFeatureShowcase: () -> Unit,
    onReuseLatestRecord: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenUserAgreement: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenAppearanceSettings: () -> Unit,
) {
    val allModules = modules
    val overviewStats = listOf(
        OverviewStat("收藏诊断", savedReportCount.toString(), if (savedReportCount > 0) "可回看重点结果" else "尚未收藏结果"),
        OverviewStat("历史记录", historyCount.toString(), if (historyCount > 0) "支持继续回填分析" else "等待首次诊断沉淀"),
    )
    val capabilityStats = listOf(
        "诊断深度" to 5,
        "场景覆盖" to 5,
        "工具能力" to 4,
        "审核可见" to 5,
    )
    val releaseProgress = listOf(
        "输入质量评分与补全清单" to true,
        "结果页处置剧本与应急指挥卡" to true,
        "场景库层级筛选与热门场景" to true,
        "审核演示/截图向导/提审文案复制" to true,
        "工具箱命令+判读提示复制" to true,
    )
    val enhancementDone = releaseProgress.count { it.second }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
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
                    Text(
                        "审核可见声明：无账号系统，输入内容默认仅本地处理。",
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
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
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                overviewStats.forEach { stat ->
                    Box(modifier = Modifier.weight(1f)) {
                        OverviewCard(stat)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NetSageSoftHighlight),
                border = BorderStroke(1.dp, NetSageSoftHighlightBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("本版本功能厚度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("新增可见功能模块：7 项（首页总览、十宫格入口、结果页专业报告+行动单、场景筛选、输入辅助、工具箱、版本新增清单）", style = MaterialTheme.typography.bodySmall)
                    Text("模块总数：${allModules.size}｜增强项完成：$enhancementDone/${releaseProgress.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    capabilityStats.forEach { (name, score) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name, style = MaterialTheme.typography.bodySmall)
                                Text("$score/5", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                repeat(5) { idx ->
                                    val active = idx < score
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                        ),
                                        shape = RoundedCornerShape(999.dp)
                                    ) { Spacer(modifier = Modifier.padding(vertical = 4.dp)) }
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("版本增强进度", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            releaseProgress.forEach { (item, done) ->
                                Text("${if (done) "✅" else "⬜"} $item", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = onQuickOpenFeatureShowcase, label = { Text("查看新增功能") })
                        AssistChip(onClick = onQuickOpenSamples, label = { Text("审核演示样例") })
                        AssistChip(onClick = onQuickOpenReviewDemo, label = { Text("一键审核演示") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = onQuickOpenShotsGuide, label = { Text("截图向导") })
                        AssistChip(onClick = onQuickOpenScenarios, label = { Text("场景直达") })
                    }
                }
            }
        }

        item {
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
                        AssistChip(onClick = onQuickOpenSamples, label = { Text("跑样例") })
                        AssistChip(onClick = onQuickOpenFeatureShowcase, label = { Text("看新增") })
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
                        Text(
                            "显示与风格",
                            modifier = Modifier.clickable { onOpenAppearanceSettings() },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("新手引导", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("推荐路径：先用样例体验 → 再贴真实日志 → 查看结果页“下一步行动”执行排查。", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onQuickOpenSamples) { Text("先看样例") }
                        OutlinedButton(onClick = onQuickOpenReviewDemo) { Text("一键演示") }
                        OutlinedButton(onClick = onQuickOpenShotsGuide) { Text("截图向导") }
                        OutlinedButton(onClick = onQuickOpenInput) { Text("直接诊断") }
                    }
                }
            }
        }

        latestRecordSummary?.let { summary ->
            item {
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
        }

        item {
            Text("功能入口", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        items(allModules.chunked(2).size) { index ->
            val rowModules = allModules.chunked(2)[index]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowModules.forEach { module ->
                    Box(modifier = Modifier.weight(1f)) {
                        UtilityModuleCard(module)
                    }
                }
                if (rowModules.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
