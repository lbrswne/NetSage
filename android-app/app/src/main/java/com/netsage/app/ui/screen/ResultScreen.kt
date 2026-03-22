package com.netsage.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netsage.app.model.CauseItem
import com.netsage.app.model.FaultScenario
import com.netsage.app.model.TroubleshootingChecklist

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("诊断结果 Top3", style = MaterialTheme.typography.headlineSmall)

        top?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("问题概览", style = MaterialTheme.typography.titleMedium)
                    AssistChip(onClick = {}, label = { Text(severity) })
                    Text("最可能问题：${it.name}")
                    Text("建议优先处理：${it.fix}", style = MaterialTheme.typography.bodySmall)
                    if (it.evidence.isNotEmpty()) {
                        Text("证据摘要", style = MaterialTheme.typography.titleSmall)
                        Text(it.evidence.joinToString("；"), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (recommendedChecklists.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("推荐排障清单", style = MaterialTheme.typography.titleMedium)
                    recommendedChecklists.take(2).forEach { checklist ->
                        Text("• ${checklist.title}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (recommendedScenarios.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("相关故障场景", style = MaterialTheme.typography.titleMedium)
                    recommendedScenarios.take(2).forEach { scenario ->
                        Text("• ${scenario.title}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(causes) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text("置信度: ${(item.confidence * 100).toInt()}%")
                        Text("建议: ${item.fix}")
                    }
                }
            }
        }

        if (recommendedChecklists.isNotEmpty()) {
            Button(
                onClick = { onOpenChecklists(recommendedChecklists.firstOrNull()?.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("去看推荐排障清单")
            }
        }
        if (recommendedScenarios.isNotEmpty()) {
            Button(
                onClick = { onOpenScenarios(recommendedScenarios.firstOrNull()?.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("去看相关故障场景")
            }
        }
        Button(onClick = onSaveReport, modifier = Modifier.fillMaxWidth()) {
            Text("收藏本次诊断")
        }
        Button(onClick = onCopyReport, modifier = Modifier.fillMaxWidth()) {
            Text("复制报告")
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("返回")
        }
    }
}
