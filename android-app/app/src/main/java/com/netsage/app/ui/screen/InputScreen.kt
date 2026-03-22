package com.netsage.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.netsage.app.ui.theme.NetSagePageBackground
import com.netsage.app.ui.theme.NetSageSoftHighlight
import com.netsage.app.ui.theme.NetSageSoftHighlightBorder

@Composable
fun InputScreen(
    onDiagnose: (String) -> Unit,
    initialText: String = "",
    onFillSample: (String) -> Unit = {},
    onOpenUserAgreement: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onBackHome: () -> Unit = {},
) {
    val logText = remember(initialText) { mutableStateOf(initialText) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetSagePageBackground)
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
                    .background(NetSageHeroGradient)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("NetSage", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
                Text(
                    "日志诊断工作台",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "将日志、报错文本或故障描述粘贴到下方，本地规则会给出 Top3 根因与优先修复建议。",
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("录入说明", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("• 支持粘贴日志片段、错误信息、网关报错、DNS / TLS / HTTP 异常描述", style = MaterialTheme.typography.bodyMedium)
                Text("• 当前为单机版，本页输入内容默认仅用于本地诊断与本地记录", style = MaterialTheme.typography.bodyMedium)
                Text("• 如暂时没有真实日志，可先使用样例快速体验", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("诊断输入区", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = logText.value,
                    onValueChange = { logText.value = it },
                    label = { Text("粘贴日志 / 报错文本 / 故障描述") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    minLines = 12,
                    shape = RoundedCornerShape(18.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val sample = "nslookup failed: NXDOMAIN; destination host unreachable"
                            logText.value = sample
                            onFillSample(sample)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("填充样例")
                    }
                    Button(
                        onClick = { onDiagnose(logText.value) },
                        enabled = logText.value.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
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
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("协议与返回", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("继续使用即表示您已阅读《用户协议》与《隐私政策》", style = MaterialTheme.typography.bodySmall)
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
                OutlinedButton(onClick = onBackHome, modifier = Modifier.fillMaxWidth()) {
                    Text("返回首页")
                }
            }
        }
    }
}
