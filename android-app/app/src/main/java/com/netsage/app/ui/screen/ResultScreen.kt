package com.netsage.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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

private data class PlaybookBranch(
    val title: String,
    val actions: List<String>,
)

private enum class TaskStatus(val label: String) {
    TODO("待执行"),
    DONE("已执行"),
    INVALID("无效")
}

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

private fun buildPlaybookBranches(
    top: CauseItem?,
    recommendedChecklists: List<TroubleshootingChecklist>,
    recommendedScenarios: List<FaultScenario>
): List<PlaybookBranch> {
    if (top == null) return emptyList()

    val firstChecklist = recommendedChecklists.firstOrNull()?.title ?: "通用基础排查清单"
    val firstScenario = recommendedScenarios.firstOrNull()?.title ?: "相近故障场景"

    return listOf(
        PlaybookBranch(
            title = "A分支｜首轮处置后明显改善",
            actions = listOf(
                "按《$firstChecklist》完成前2项并记录结果",
                "对照“$firstScenario”复测关键指标，确认问题收敛",
                "将本次结论收藏并补充现场备注，防止复发"
            )
        ),
        PlaybookBranch(
            title = "B分支｜首轮处置后无明显改善",
            actions = listOf(
                "切换到 Top2 / Top3 原因继续验证，不要只盯单一结论",
                "补充更原始日志、时间点与环境变量后再次诊断",
                "优先排除链路层与配置变更带来的交叉影响"
            )
        ),
        PlaybookBranch(
            title = "C分支｜现场出现新异常或范围扩大",
            actions = listOf(
                "立刻评估影响范围（单终端/单网段/全网）",
                "优先执行保守绕行方案，先恢复可用性再做根因深挖",
                "导出“下一步行动单”同步团队并持续复盘"
            )
        )
    )
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
private fun ConfidenceBar(confidence: Double) {
    val pct = confidence.coerceIn(0.0, 1.0).toFloat()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(10) { i ->
            val active = i < (pct * 10).toInt().coerceAtLeast(1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(vertical = 4.dp)
            )
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
            Text("规则证据强度 ${(item.confidence * 100).toInt()}/100", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            ConfidenceBar(item.confidence)
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
    savedTaskStatuses: Map<Int, String> = emptyMap(),
    onTaskStatusChange: (Int, String) -> Unit = { _, _ -> },
    onOpenChecklists: (String?) -> Unit,
    onOpenScenarios: (String?) -> Unit,
    onSaveReport: () -> Unit,
    onBack: () -> Unit,
    onCopyReport: () -> Unit = {},
    onCopyIncidentBrief: () -> Unit = {},
    onExportActionPlan: () -> Unit = {}
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
    val confidenceText = top?.let { "当前主判断的规则证据强度为 ${(it.confidence * 100).toInt()}/100；这是排序分数而非统计概率，建议结合现场证据复核。" }
        ?: "当前暂无可用诊断结果。"
    val actionSteps = buildActionSteps(top, recommendedChecklists, recommendedScenarios)
    val playbookBranches = buildPlaybookBranches(top, recommendedChecklists, recommendedScenarios)
    val taskStatuses = remember(actionSteps, savedTaskStatuses) {
        mutableStateMapOf<Int, TaskStatus>().apply {
            actionSteps.indices.forEach { idx ->
                put(idx, savedTaskStatuses[idx]?.let { name -> TaskStatus.entries.firstOrNull { it.name == name } } ?: TaskStatus.TODO)
            }
        }
    }
    val (fitText, notFitText) = buildApplicabilityText(top)
    val impactScope = buildImpactScope(top)
    val emergencyPlan = listOf(
        "0-3 分钟：确认影响范围（单用户/单网段/全量）并锁定主判断",
        "3-8 分钟：执行主判断首轮处置，优先恢复可用性",
        "8-15 分钟：复测关键指标，若无改善立即切换 Top2/Top3"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onBack) {
                    Text("返回")
                }
            }
        }

        item {
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
        }

        top?.let {
            item {
                ReportBlock("问题概览") {
                    Text("最可能问题：${it.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("建议优先处理：${it.fix}", style = MaterialTheme.typography.bodyMedium)
                    Text("风险等级：$severity", style = MaterialTheme.typography.bodyMedium, color = severityColor)
                    Text("影响范围：$impactScope", style = MaterialTheme.typography.bodySmall)
                    Text("处理策略：先围绕主判断做首轮排查，再根据验证结果决定是否切换到 Top2 / Top3。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (top?.evidence?.isNotEmpty() == true) {
            item {
                ReportBlock("为什么这样判断") {
                    Text("当前主判断主要来自以下证据命中：", style = MaterialTheme.typography.bodyMedium)
                    top.evidence.forEach { evidence ->
                        Text("• $evidence", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (actionSteps.isNotEmpty()) {
            item {
                ReportBlock("交互式下一步任务流") {
                    val doneCount = taskStatuses.values.count { it == TaskStatus.DONE }
                    val invalidCount = taskStatuses.values.count { it == TaskStatus.INVALID }
                    val branchHint = when {
                        invalidCount >= 2 -> "建议切换到 B 分支：补充日志后重诊断。"
                        doneCount >= 2 -> "建议继续 A 分支：复测关键指标并收敛。"
                        else -> "先完成前两步，再判断是否进入 A/B/C 分支。"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("执行进度：$doneCount/${actionSteps.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        actionSteps.forEachIndexed { index, step ->
                            Text("${index + 1}. ${step.title}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(step.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                TaskStatus.entries.forEach { status ->
                                    AssistChip(
                                        onClick = {
                                            taskStatuses[index] = status
                                            onTaskStatusChange(index, status.name)
                                        },
                                        label = { Text(status.label) },
                                        border = if (taskStatuses[index] == status) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                    )
                                }
                            }
                        }
                        Text("分支建议：$branchHint", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            ReportBlock("应急指挥卡（黄金15分钟）") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    emergencyPlan.forEach { step ->
                        Text("• $step", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (playbookBranches.isNotEmpty()) {
            item {
                ReportBlock("处置剧本模式") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        playbookBranches.forEach { branch ->
                            Text(branch.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            branch.actions.forEach { action ->
                                Text("• $action", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        item {
            ReportBlock("适用与边界") {
                Text("适用场景", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(fitText, style = MaterialTheme.typography.bodyMedium)
                Text("不适用或需谨慎", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(notFitText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (recommendedChecklists.isNotEmpty() || recommendedScenarios.isNotEmpty()) {
            item {
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
        }

        item {
            Text("候选原因", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        items(causes.mapIndexed { index, item -> index to item }) { (index, item) ->
            CandidateCauseCard(item = item, index = index + 1)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onSaveReport, modifier = Modifier.weight(1f)) {
                    Text("收藏本次诊断")
                }
                OutlinedButton(onClick = onCopyReport, modifier = Modifier.weight(1f)) {
                    Text("复制报告")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCopyIncidentBrief, modifier = Modifier.weight(1f)) {
                    Text("复制故障简报")
                }
                OutlinedButton(onClick = onExportActionPlan, modifier = Modifier.weight(1f)) {
                    Text("导出行动单")
                }
            }
        }
    }
}
