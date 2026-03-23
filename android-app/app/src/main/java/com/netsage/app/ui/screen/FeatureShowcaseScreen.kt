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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netsage.app.ui.theme.NetSageHeroGradient
import com.netsage.app.ui.theme.NetSageSoftHighlight
import com.netsage.app.ui.theme.NetSageSoftHighlightBorder

@Composable
fun FeatureShowcaseScreen(
    onBack: () -> Unit,
    onOpenInput: () -> Unit,
    onOpenResultDemo: () -> Unit,
    onOpenScenarios: () -> Unit,
    onOpenSamples: () -> Unit,
    onCopySubmissionBrief: () -> Unit,
) {
    val features = listOf(
        "首页新增功能厚度总览（审核首屏可见）",
        "九宫格功能入口（10个模块）",
        "输入页：随机样例 + 一键清空 + 诊断进度",
        "结果页：风险等级 + 影响范围 + 处理顺序 + 行动单导出",
        "场景库：分类筛选 + 关键词检索 + 高亮推荐",
        "现场工具箱：排障命令模板 + 判读提示一键复制",
        "设置页：深色模式 + 密度模式 + 恢复默认"
    )

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
                Text("版本新增功能清单", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("该页面用于应用市场审核快速核验：当前版本新增了哪些可见能力。", color = Color.White.copy(alpha = 0.84f), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = NetSageSoftHighlight),
            border = BorderStroke(1.dp, NetSageSoftHighlightBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("审核核验建议", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("建议按：首页→输入页→结果页→场景库→设置页 的顺序截图，可一眼看出功能增量。", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onCopySubmissionBrief, modifier = Modifier.fillMaxWidth()) {
                    Text("复制提审功能说明")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("本版本新增（6项）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                features.forEach { item ->
                    Text("• $item", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onOpenInput, modifier = Modifier.weight(1f)) { Text("看输入页") }
            OutlinedButton(onClick = onOpenResultDemo, modifier = Modifier.weight(1f)) { Text("看结果演示") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onOpenScenarios, modifier = Modifier.weight(1f)) { Text("看场景库") }
            OutlinedButton(onClick = onOpenSamples, modifier = Modifier.weight(1f)) { Text("看样例中心") }
        }
    }
}
