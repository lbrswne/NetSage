package com.netsage.app.ui.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
internal fun SharePrivacyDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分享完整诊断报告？") },
        text = { Text("报告可能包含原始日志、网络快照（如本地地址、网关、DNS）和探测证据。确认后将打开系统分享面板，由你选择分享对象。") },
        confirmButton = { Button(onClick = onConfirm) { Text("继续分享") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } },
    )
}
