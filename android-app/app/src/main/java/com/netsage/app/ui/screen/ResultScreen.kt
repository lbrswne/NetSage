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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netsage.app.model.CauseItem
import com.netsage.app.model.FaultScenario
import com.netsage.app.model.TroubleshootingChecklist
import com.netsage.app.ui.theme.NetSageHeroGradient

private data class ActionStep(
    val title: String,
    val detail: String,
)

private fun buildActionSteps(
    top: CauseItem?,
    recommendedChecklists: List<TroubleshootingChecklist>,
    recommendedScenarios: List<FaultScenario>
): List<ActionStep> {
    if (top == null) return emptyList()

    val steps = mutableListOf<ActionStep>()
    steps += ActionStep(
        title = "先确认当前主判断是否贴近现场",
        detail = "先核对“${top.name}”是否与当前现象一致；若现象明显不符，应优先回看输入内容是否缺少关键日志。"
    )

    recommendedChecklists.firstOrNull()?.let { checklist ->
        steps += ActionStep(
            title = "按推荐清单先做基础排查",
            detail = "优先执行《${checklist.title}》中的前 1-2 步，先判断是配置类、链路类还是环境类问题。"
        )
    }

    top.evidence.firstOrNull()?.let { evidence ->
        steps += ActionStep(
            title = "围绕关键证据复测",
            detail = "当前最值得复核的证据是：${evidence}。建议结合原始日志、时间点和环境变量再次确认。"
        )
    }

    recommendedScenarios.firstOrNull()?.let { scenario ->
        steps += ActionStep(
            title = "参考相近故障场景做交叉判断",
            detail = "可对照“${scenario.title}”的症状与检查项，确认当前问题是否属于同类模式。"
        )
    }

    steps += ActionStep(
        title = "完成首轮处理后再回到结果页复盘",
        detail = "若首轮处理无效，优先查看候选原因 Top2 / Top3，避免只盯住单一结论。"
    )
    return steps
}

private fun buildImpactScope(top: CauseItem?): String {
    if (top == null) return "暂无评估"
    val text = (top.name + " " + top.evidence.joinToString(" ")).lowercase()
    return when {
        listOf("dns", "解析", "nxdomain").any { text.contains(it) } -> "影响范围通常可扩散到同一 DNS 出口下的多个终端，建议优先确认是否为全局解析问题。"
        listOf("tls", "certificate", "证书", "handshake").any { text.contains(it) } -> "影响范围通常集中在特定域名或特定客户端版本，需重点核对证书链与系统时间。"
        listOf("502", "504", "gateway", "upstream", "网关").any { text.contains(it) } -> "影响范围常覆盖同一入口流量，可能导致某业务接口整体不可用或高延迟。"
        listOf("timeout", "unreachable", "route", "连接").any { text.contains(it) } -> "影响范围依赖链路拓扑，可能为局部网段或跨网段通信受阻。"
        else -> "当前影响范围不明确，建议补充受影响用户比例、网络区域与复现时间窗口。"
    }
}

private fun buildApplicabilityText(top: CauseItem?): Pair<String, String> {
    if (top == null) return "适用场景" to "当前暂无可判断内容。"

    val evidenceText = top.evidence.joinToString(" ").lowercase()
    val fit = when {
        listOf("nxdomain", "解析", "dns").any { evidenceText.contains(it) || top.name.lowercase().contains(it) } ->
            "更适用于域名解析失败、server can't find、解析结果异常等场景。"
        listOf("tls", "certificate", "handshake", "证书").any { evidenceText.contains(it) || top.name.lowercase().contains(it) } ->
            "更适用于证书链异常、TLS 版本不兼容、握手失败等场景。"
        listOf("502", "504", "gateway", "upstream", "网关").any { evidenceText.contains(it) || top.name.lowercase().contains(it) } ->
            "更适用于网关报错、上游超时、代理层与服务层之间异常等场景。"
        listOf("丢包", "packet loss", "wireless", "无线").any { evidenceText.contains(it) || top.name.lowercase().contains(it) } ->
            "更适用于无线不稳定、时延波动、间歇性丢包等场景。"
        else -> "更适用于当前输入中已经出现明确报错、关键日志线索或稳定复现现象的场景。"
    }
    val notFit = "若现场现象与当前证据不一致，或输入内容过短、过旧、缺少关键上下文，则本结论仅适合作为首轮参考。"
    return fit to notFit
}

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
    val confidenceText = top?.let { "当前主判断置信度约 ${(it.confidence * 100).toInt()}%，建议优先按主判断推进首轮排查。" }
        ?: "当前暂无可用诊断结果。"
    val actionSteps = buildActionSteps(top, recommendedChecklists, recommendedScenarios)
    val (fitText, notFitText) = buildApplicabilityText(top)
    val impactScope = buildImpactScope(top)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack) {
                Text("返回")
            }
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
                Text("NetSage 诊断报告", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
                Text(
                    top?.name ?: "暂无诊断结果",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    confidenceText,
                    color = Color.White.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.bodyMedium
                )
                SeverityBadge(severity, severityColor)
            }
        }

        top?.let {
            ReportBlock("问题概览") {
                Text("最可能问题：${it.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("建议优先处理：${it.fix}", style = MaterialTheme.typography.bodyMedium)
                Text("风险等级：$severity", style = MaterialTheme.typography.bodyMedium, color = severityColor)
                Text("影响范围：$impactScope", style = MaterialTheme.typography.bodySmall)
                Text("处理策略：先围绕主判断做首轮排查，再根据验证结果决定是否切换到 Top2 / Top3。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (top?.evidence?.isNotEmpty() == true) {
            ReportBlock("为什么这样判断") {
                Text("当前主判断主要来自以下证据命中：", style = MaterialTheme.typography.bodyMedium)
                top.evidence.forEach { evidence ->
                    Text("• $evidence", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (actionSteps.isNotEmpty()) {
            ReportBlock("建议处理顺序") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    actionSteps.forEachIndexed { index, step ->
                        Text("${index + 1}. ${step.title}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(step.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        ReportBlock("适用与边界") {
            Text("适用场景", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(fitText, style = MaterialTheme.typography.bodyMedium)
            Text("不适用或需谨慎", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(notFitText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (recommendedChecklists.isNotEmpty() || recommendedScenarios.isNotEmpty()) {
            ReportBlock("关联排障资源") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (recommendedChecklists.isNotEmpty()) {
                        Text("推荐排障清单：${recommendedChecklists.first().title}", style = MaterialTheme.typography.bodyMedium)
                        Text("用途：适合把当前判断转成更具体的逐步排查动作。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = { onOpenChecklists(recommendedChecklists.firstOrNull()?.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("去看推荐排障清单")
                        }
                    }
                    if (recommendedScenarios.isNotEmpty()) {
                        Text("相关故障场景：${recommendedScenarios.first().title}", style = MaterialTheme.typography.bodyMedium)
                        Text("用途：适合核对当前症状是否属于同类问题模式。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    }
}
