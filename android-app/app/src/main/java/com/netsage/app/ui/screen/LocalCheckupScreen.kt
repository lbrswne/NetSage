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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.netsage.app.ui.theme.NetSageHeroGradient
import com.netsage.app.ui.theme.NetSageSoftHighlight
import com.netsage.app.ui.theme.NetSageSoftHighlightBorder
import com.netsage.app.util.LocalDocumentIo

enum class LocalCheckupMode(val label: String) {
    QUICK("快速体检"),
    COMBINED("组合诊断")
}

enum class CheckProtocol(val label: String, val defaultPort: Int) {
    HTTPS("HTTPS", 443),
    HTTP("HTTP", 80)
}

data class LocalCheckupRequest(
    val mode: LocalCheckupMode,
    val host: String,
    val port: Int,
    val protocol: CheckProtocol,
    val logText: String,
)

private fun normalizeHostInput(raw: String): String {
    val withoutScheme = raw.trim().removePrefix("https://").removePrefix("http://")
    val authority = withoutScheme.substringBefore('/')
    if (authority.startsWith("[")) return authority.substringAfter('[').substringBefore(']')
    val colonCount = authority.count { it == ':' }
    return if (colonCount == 1 && authority.substringAfterLast(':').all(Char::isDigit)) {
        authority.substringBeforeLast(':')
    } else {
        authority
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocalCheckupScreen(
    initialMode: LocalCheckupMode,
    isRunning: Boolean,
    progressMessage: String,
    onBack: () -> Unit,
    onStart: (LocalCheckupRequest) -> Unit,
    onCancel: () -> Unit,
) {
    var mode by remember(initialMode) { mutableStateOf(initialMode) }
    var host by remember { mutableStateOf("example.com") }
    var protocol by remember { mutableStateOf(CheckProtocol.HTTPS) }
    var portText by remember { mutableStateOf(protocol.defaultPort.toString()) }
    var logText by remember { mutableStateOf("") }
    val normalizedHost = normalizeHostInput(host)
    val port = portText.toIntOrNull()
    val logLengthValid = logText.length <= LocalDocumentIo.MAX_LOG_CHARS
    val inputValid = normalizedHost.isNotBlank() && port != null && port in 1..65535 && logLengthValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedButton(onClick = onBack, enabled = !isRunning) { Text("返回") }

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
                Text("NetSage · Local diagnostics", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
                Text(mode.label, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "在本机依次检查网络状态、DNS、TCP、TLS 与 HTTP；没有 NetSage 服务器，也不会上传日志。",
                    color = Color.White.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = NetSageSoftHighlight),
            border = BorderStroke(1.dp, NetSageSoftHighlightBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("本地优先边界", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("• 连接只发往下方由你确认的目标地址", style = MaterialTheme.typography.bodySmall)
                Text("• 诊断会话默认仅保存在本机，关闭某项检测不会影响其他步骤", style = MaterialTheme.typography.bodySmall)
                Text("• 不使用账号、云数据库、遥测或广告 SDK", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("诊断方式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LocalCheckupMode.entries.forEach { item ->
                        AssistChip(
                            onClick = { if (!isRunning) mode = item },
                            label = { Text(item.label) },
                            border = if (mode == item) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        )
                    }
                }

                if (protocol == CheckProtocol.HTTP) {
                    Text(
                        "HTTP 不加密。仅在你确认目标确实使用明文 HTTP 时选择；NetSage 不会把 HTTPS 自动降级为 HTTP。",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("目标域名或 IP") },
                    supportingText = { Text("示例地址仅是默认探测目标，可替换为需要排查的服务") },
                    singleLine = true,
                    enabled = !isRunning
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CheckProtocol.entries.forEach { item ->
                        AssistChip(
                            onClick = {
                                if (!isRunning) {
                                    protocol = item
                                    portText = item.defaultPort.toString()
                                }
                            },
                            label = { Text(item.label) },
                            border = if (protocol == item) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        )
                    }
                }

                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("端口") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = portText.isNotBlank() && port == null,
                    singleLine = true,
                    enabled = !isRunning
                )

                if (mode == LocalCheckupMode.COMBINED) {
                    OutlinedTextField(
                        value = logText,
                        onValueChange = { logText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("补充日志或故障描述（可选）") },
                        supportingText = {
                            Text("${logText.length} / ${LocalDocumentIo.MAX_LOG_CHARS} 字符")
                        },
                        isError = !logLengthValid,
                        minLines = 6,
                        enabled = !isRunning
                    )
                }
            }
        }

        if (isRunning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("正在执行本地检测", fontWeight = FontWeight.SemiBold)
                        Text(progressMessage.ifBlank { "正在准备网络快照…" }, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("取消本次检测") }
        } else {
            Button(
                onClick = {
                    onStart(
                        LocalCheckupRequest(
                            mode = mode,
                            host = normalizedHost,
                            port = port ?: protocol.defaultPort,
                            protocol = protocol,
                            logText = logText.trim()
                        )
                    )
                },
                enabled = inputValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("开始本地检测")
            }
        }
    }
}
