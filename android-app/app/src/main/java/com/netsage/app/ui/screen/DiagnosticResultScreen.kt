package com.netsage.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.netsage.app.ui.theme.NetSageHeroGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ProbeResultUi(
    val title: String,
    val role: String = "主目标",
    val target: String,
    val status: String,
    val durationMs: Long,
    val evidence: List<String>,
    val error: String? = null,
)

data class HypothesisUi(
    val title: String,
    val priority: String,
    val score: Int,
    val matchedEvidence: List<String>,
    val conflictingEvidence: List<String>,
    val actions: List<String>,
)

data class DiagnosticSessionUi(
    val id: String,
    val createdAt: Long,
    val mode: String,
    val target: String,
    val networkLines: List<String>,
    val probes: List<ProbeResultUi>,
    val hypotheses: List<HypothesisUi>,
    val comparisonLines: List<String> = emptyList(),
    val referenceTarget: String? = null,
    val referenceSummary: String? = null,
    val tcpLines: List<String> = emptyList(),
)

private fun statusColor(status: String): Color = when (status.uppercase()) {
    "SUCCESS", "PASSED", "正常" -> Color(0xFF16815D)
    "SKIPPED", "CANCELLED", "跳过" -> Color(0xFF6B7280)
    else -> Color(0xFFB45309)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagnosticResultScreen(
    session: DiagnosticSessionUi,
    isRetesting: Boolean,
    retestProgress: String,
    onBack: () -> Unit,
    onRetest: () -> Unit,
    onExportMarkdown: () -> Unit,
    onExportJson: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(session.createdAt))
    var showTechnicalDetails by remember(session.id) { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OutlinedButton(onClick = onBack, enabled = !isRetesting) { Text("返回") }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.background(NetSageHeroGradient).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("NetSage · Diagnostic session", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
                    Text("本地诊断结果", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("${session.mode} · ${session.target}", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
                    Text(time, color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    "结果由本机规则和本次探测证据生成。规则分数用于排序，不代表统计概率；日志与结果未上传给 NetSage。",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("本次结论", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(session.hypotheses.firstOrNull()?.title ?: "本次尚无诊断假设", style = MaterialTheme.typography.titleMedium)
                    session.hypotheses.firstOrNull()?.matchedEvidence?.firstOrNull()?.let {
                        Text(
                            "关键证据：$it",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text("下一步", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(session.hypotheses.firstOrNull()?.actions?.firstOrNull() ?: "查看下方技术详情并补充故障信息。")
                }
            }
        }
        session.referenceSummary?.let { summary ->
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("主目标与对照目标", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("对照：${session.referenceTarget.orEmpty()}")
                        Text(summary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (session.tcpLines.isNotEmpty()) {
            item {
                EvidenceSection("IPv4 / IPv6 TCP 建连", session.tcpLines + "仅代表本次少量建连采样，不是丢包率或持续可用性。")
            }
        }
        item {
            OutlinedButton(onClick = { showTechnicalDetails = !showTechnicalDetails }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showTechnicalDetails) "收起技术详情" else "查看检测证据与其他假设")
            }
        }
        if (showTechnicalDetails && session.networkLines.isNotEmpty()) {
            item {
                EvidenceSection("当前网络快照", session.networkLines)
            }
        }
        if (showTechnicalDetails) item {
            Text("检测时间线", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (showTechnicalDetails && session.probes.isEmpty()) {
            item { Text("本次为纯日志诊断，没有执行主动探测。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else if (showTechnicalDetails) {
            session.probes.forEachIndexed { index, probe ->
                item(key = "probe-${session.id}-$index") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${index + 1}. ${probe.role} · ${probe.title}", fontWeight = FontWeight.SemiBold)
                                Text(probe.status, color = statusColor(probe.status), style = MaterialTheme.typography.labelLarge)
                            }
                            Text(probe.target, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("耗时 ${probe.durationMs} ms", style = MaterialTheme.typography.bodySmall)
                            probe.evidence.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                            probe.error?.takeIf { it.isNotBlank() }?.let {
                                Text("异常：$it", style = MaterialTheme.typography.bodySmall, color = statusColor("FAILED"))
                            }
                        }
                    }
                }
            }
        }
        if (showTechnicalDetails) item {
            Text("Top 3 诊断假设", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (showTechnicalDetails) session.hypotheses.forEachIndexed { index, item ->
            item(key = "hypothesis-${session.id}-$index") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("${index + 1}. ${item.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = {}, label = { Text(item.priority) })
                            AssistChip(onClick = {}, label = { Text("规则分数 ${item.score}") })
                        }
                        if (item.matchedEvidence.isNotEmpty()) {
                            Text("匹配证据", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            item.matchedEvidence.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                        }
                        if (item.conflictingEvidence.isNotEmpty()) {
                            Text("冲突或反向证据", style = MaterialTheme.typography.labelLarge, color = Color(0xFFB45309))
                            item.conflictingEvidence.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                        }
                        Text("修复步骤", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        item.actions.forEachIndexed { actionIndex, action ->
                            Text(
                                "${actionIndex + 1}. $action",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
        if (session.comparisonLines.isNotEmpty()) {
            item { EvidenceSection("修复后复测对比", session.comparisonLines) }
        }
        item {
            if (isRetesting) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                        Column {
                            Text("正在复测", fontWeight = FontWeight.SemiBold)
                            Text(retestProgress, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                Button(onClick = onRetest, modifier = Modifier.fillMaxWidth(), enabled = session.probes.isNotEmpty()) {
                    Text("修复后复测")
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(onClick = onExportMarkdown, modifier = Modifier.weight(1f)) { Text("导出 Markdown") }
                    OutlinedButton(onClick = onExportJson, modifier = Modifier.weight(1f)) { Text("导出 JSON") }
                }
                OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) { Text("通过系统分享") }
                OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("删除本地会话") }
            }
        }
    }
}

@Composable
private fun EvidenceSection(title: String, lines: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            lines.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
