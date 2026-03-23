package com.netsage.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netsage.app.ui.theme.NetSageHeroGradient
import com.netsage.app.ui.theme.NetSageSoftHighlight
import com.netsage.app.ui.theme.NetSageSoftHighlightBorder

private data class InputTemplate(
    val label: String,
    val content: String,
)

private fun buildInputQualityHint(text: String): String {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return "建议至少提供报错原文、故障现象或日志片段中的一种。"
    if (trimmed.length < 24) return "输入偏短，建议补充时间点、错误码、现象描述或关键日志。"

    val lower = trimmed.lowercase()
    val signalCount = listOf(
        "dns", "nxdomain", "tls", "certificate", "handshake", "http", "502", "504",
        "timeout", "unreachable", "route", "gateway", "连接", "超时", "证书", "解析"
    ).count { lower.contains(it) }

    return when {
        signalCount >= 3 -> "输入信息较完整，已包含多项诊断信号，可直接开始诊断。"
        signalCount >= 1 -> "已有部分诊断信号，若能补充环境、时间点或更多原始日志，结果会更稳。"
        else -> "建议补充更明确的错误码、异常关键词或原始日志，避免只写笼统现象。"
    }
}

private fun buildInputQualityScore(text: String): Int {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return 0

    var score = 0
    if (trimmed.length >= 24) score += 20
    if (trimmed.length >= 80) score += 15
    if (Regex("\\d{2}:\\d{2}|\\d{4}-\\d{2}-\\d{2}").containsMatchIn(trimmed)) score += 15

    val lower = trimmed.lowercase()
    if (listOf("502", "504", "timeout", "nxdomain", "certificate", "handshake", "unreachable", "refused").any { lower.contains(it) }) score += 25
    if (listOf("校园网", "宿舍", "公司", "家庭", "热点", "wifi", "内网").any { lower.contains(it) }) score += 15
    if (listOf("影响范围", "复现", "日志", "错误码", "时间").any { lower.contains(it) }) score += 10

    return score.coerceIn(0, 100)
}

private fun qualityLevel(score: Int): String = when {
    score >= 75 -> "高"
    score >= 45 -> "中"
    else -> "低"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InputScreen(
    onDiagnose: (String) -> Unit,
    initialText: String = "",
    isLoading: Boolean = false,
    onFillSample: () -> String = { "" },
    onOpenUserAgreement: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onBackHome: () -> Unit = {},
) {
    val logText = remember(initialText) { mutableStateOf(initialText) }
    val templates = listOf(
        InputTemplate(
            "故障现象模板",
            "故障现象：\n发生时间：\n影响范围：\n复现频率：\n我已观察到："
        ),
        InputTemplate(
            "日志片段模板",
            "日志时间：\n错误码/关键词：\n原始日志：\n前后文补充："
        ),
        InputTemplate(
            "网络环境模板",
            "网络环境：\n客户端位置/网络类型：\n目标服务/域名：\n已尝试操作："
        )
    )
    val qualityHint = buildInputQualityHint(logText.value)
    val qualityScore = buildInputQualityScore(logText.value)
    val qualityLevel = qualityLevel(qualityScore)
    val completionChecklist = listOf(
        "是否包含明确报错原文/错误码",
        "是否给出发生时间与复现频率",
        "是否描述了影响范围（单设备/多设备）",
        "是否说明网络环境（校园网/宿舍/企业/家宽/热点）"
    )
    val faultTypeGuides = listOf(
        "DNS解析" to "可补充 nslookup/dig 结果、DNS 服务器地址、是否全员受影响",
        "TLS证书" to "可补充证书报错原文、系统时间、客户端/服务端 TLS 版本",
        "网关502/504" to "可补充网关层日志、上游服务状态、是否与发布变更同时间",
        "连接超时" to "可补充目标 IP:Port、ping/mtr/traceroute 结果、网络环境"
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
            OutlinedButton(onClick = onBackHome) {
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("NetSage", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
                Text(
                    "日志诊断工作台",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "先在下方输入区粘贴日志或故障描述，再按需使用模板补充信息。",
                    color = Color.White.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.bodyMedium
                )
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
                Text("诊断输入区", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = logText.value,
                    onValueChange = { logText.value = it },
                    label = { Text("粘贴日志 / 报错文本 / 故障描述") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 10,
                    shape = RoundedCornerShape(18.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("输入质量提示", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("质量评分：$qualityScore/100（$qualityLevel）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text(qualityHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val sample = onFillSample()
                            if (sample.isNotBlank()) {
                                logText.value = sample
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("随机样例")
                    }
                    OutlinedButton(
                        onClick = { logText.value = "" },
                        enabled = logText.value.isNotBlank() && !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("一键清空")
                    }
                }
                Button(
                    onClick = { onDiagnose(logText.value) },
                    enabled = logText.value.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                        Text("诊断中，请稍候…")
                    } else {
                        Text("开始诊断")
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = NetSageSoftHighlight),
            border = BorderStroke(1.dp, NetSageSoftHighlightBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("快捷模板", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("点击后会直接填入上方输入区。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.forEach { template ->
                        AssistChip(
                            onClick = {
                                logText.value = template.content
                            },
                            label = { Text(template.label) }
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("输入补全清单", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                completionChecklist.forEach { item ->
                    Text("• $item", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("故障类型引导", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                faultTypeGuides.forEach { (title, guide) ->
                    Text("• $title：$guide", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("录入说明", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("• 支持粘贴日志片段、错误信息、网关报错、DNS / TLS / HTTP 异常描述", style = MaterialTheme.typography.bodySmall)
                Text("• 当前为单机版，本页输入内容默认仅用于本地诊断与本地记录", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = NetSageSoftHighlight),
            border = BorderStroke(1.dp, NetSageSoftHighlightBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("协议", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
    }
}
