package com.netsage.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyConsentDialog(
    onOpenUserAgreement: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onAgree: () -> Unit,
    onReject: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("用户协议与隐私政策") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "欢迎使用 NetSage。当前版本为单机版，主要在本地完成诊断与记录保存。为保障您的个人信息安全并满足应用市场审核要求，请您在使用前认真阅读并充分理解《用户协议》与《隐私政策》。点击“同意并继续”表示您已阅读、理解并同意上述内容。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "查看《用户协议》",
                    modifier = Modifier.clickable { onOpenUserAgreement() },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "查看《隐私政策》",
                    modifier = Modifier.clickable { onOpenPrivacyPolicy() },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAgree) {
                Text("同意并继续")
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) {
                Text("不同意并退出")
            }
        }
    )
}
