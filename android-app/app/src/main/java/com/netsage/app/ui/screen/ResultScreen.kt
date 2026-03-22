package com.netsage.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netsage.app.model.CauseItem
import com.netsage.app.model.FaultScenario
import com.netsage.app.model.TroubleshootingChecklist

@Composable
private fun SeverityBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ReportBlock(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun CandidateCauseCard(item: CauseItem, index: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Top$index · ${item.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("置信度 ${(item.confidence * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("建议：${item.fix}", style = MaterialTheme.typography.bodyMedium)
            if (item.evidence.isNotEmpty()) {
                Text("证据：${item.evidence.joinToString("；")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ResultScreen(
    causes: List<CauseItem>,
    recommendedChecklists: List<TroubleshootingChecklist> = emptyList(),
    recommendedScenarios: List<FaultScenario> = emptyList(),
    onOpenChecklists: (String?) -> Unit,
    onOpenScenarios: (String?) -> Unit,
    onSaveReport: () -> Unit,
    onBack: () -> Unit,
    onCopyReport: () -> Unit = {}
) {
    val top = causes.firstOrNull()
    val severity = when {
        (top?.confidence ?: 0.0) >= 0.85 -> "高优先级"
        (top?.confidence ?: 0.0) >= 0.60 -> "中优先级"
        else -> "低优先级"
    }
    val severityColor = when (severity) {
        "高优先级" -> Color(0xFFCB3A31)
        "中优先级" -> Color(0xFFD78B13)
        else -> Color(0xFF2D7A46)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F7FB))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF101827), Color(0xFF183A5A), Color(0xFF276A8D))
                        )
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("NetSage 诊断报告", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
                Text(
                    top?.name ?: "暂无诊断结果",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                top?.let {
                    Text(
                        "已根据本地规则给出优先处理方向与关联排障建议。",
                        color = Color.White.copy(alpha = 0.84f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                SeverityBadge(severity, severityColor)
            }
        }

        top?.let {
            ReportBlock("问题概览") {
                Text("最可能问题：${it.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("建议优先处理：${it.fix}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (top?.evidence?.isNotEmpty() == true) {
            ReportBlock("证据摘要") {
                top.evidence.forEach { evidence ->
                    Text("• $evidence", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (recommendedChecklists.isNotEmpty() || recommendedScenarios.isNotEmpty()) {
            ReportBlock("推荐动作") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (recommendedChecklists.isNotEmpty()) {
                        Text("推荐排障清单：${recommendedChecklists.first().title}", style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = { onOpenChecklists(recommendedChecklists.firstOrNull()?.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("去看推荐排障清单")
                        }
                    }
                    if (recommendedScenarios.isNotEmpty()) {
                        Text("相关故障场景：${recommendedScenarios.first().title}", style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(
                            onClick = { onOpenScenarios(recommendedScenarios.firstOrNull()?.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("去看相关故障场景")
                        }
                    }
                }
            }
        }

        Text("候选原因", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(causes.mapIndexed { index, item -> index to item }) { (index, item) ->
                CandidateCauseCard(item = item, index = index + 1)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onSaveReport, modifier = Modifier.weight(1f)) {
                Text("收藏本次诊断")
            }
            OutlinedButton(onClick = onCopyReport, modifier = Modifier.weight(1f)) {
                Text("复制报告")
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("返回")
        }
    }
}
