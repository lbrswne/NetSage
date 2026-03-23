package com.netsage.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.netsage.app.ui.theme.NetSageHeroGradient

data class ToolboxCommand(
    val title: String,
    val scene: String,
    val command: String,
    val hint: String,
    val passSignal: String,
    val failSignal: String,
)

@Composable
fun ToolboxScreen(
    onBack: () -> Unit,
    onCopyCommand: (String) -> Unit,
    onCopyInterpretation: (String) -> Unit,
) {
    val commands = listOf(
        ToolboxCommand(
            "DNS 解析检查",
            "域名解析异常",
            "nslookup api.example.com 8.8.8.8",
            "对比本地 DNS 与公共 DNS 的返回差异",
            "解析结果稳定且与权威记录一致",
            "返回 NXDOMAIN / SERVFAIL / 超时，或多次结果不一致"
        ),
        ToolboxCommand(
            "链路跳点定位",
            "连接超时/不稳定",
            "traceroute 8.8.8.8",
            "查看在哪一跳开始丢包或延迟抖动",
            "跳点延迟平稳，未出现持续性丢包",
            "某固定跳点后延迟陡增或持续超时"
        ),
        ToolboxCommand(
            "端口连通性",
            "服务不可达",
            "telnet 10.0.0.8 443",
            "快速判断目标端口是否可建立连接",
            "可快速建立连接并返回服务握手",
            "连接超时/拒绝，提示目标端口不可达"
        ),
        ToolboxCommand(
            "TLS 证书检查",
            "证书或握手异常",
            "openssl s_client -connect api.example.com:443 -servername api.example.com",
            "检查证书链与 SNI 是否匹配",
            "证书链完整，主机名匹配，verify return code=0",
            "证书链不完整、主机名不匹配或 verify 非0"
        ),
        ToolboxCommand(
            "持续丢包观察",
            "间歇性网络卡顿",
            "ping -c 20 192.168.1.1",
            "观察平均时延和丢包率变化",
            "平均时延稳定且丢包接近 0%",
            "丢包率持续 >3% 或时延抖动显著"
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                Text("现场工具箱", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("内置常见排障命令模板，现场可快速复制执行。", color = Color.White.copy(alpha = 0.84f), style = MaterialTheme.typography.bodyMedium)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(commands) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("适用场景：${item.scene}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.command, style = MaterialTheme.typography.bodyMedium)
                        Text(item.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("判读提示（正常）：${item.passSignal}", style = MaterialTheme.typography.bodySmall)
                        Text("判读提示（异常）：${item.failSignal}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { onCopyCommand(item.command) }, modifier = Modifier.fillMaxWidth()) {
                            Text("复制命令")
                        }
                        OutlinedButton(
                            onClick = {
                                val interpretation = """
                                    工具项：${item.title}
                                    适用场景：${item.scene}
                                    正常信号：${item.passSignal}
                                    异常信号：${item.failSignal}
                                """.trimIndent()
                                onCopyInterpretation(interpretation)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("复制判读提示")
                        }
                    }
                }
            }
        }
    }
}
