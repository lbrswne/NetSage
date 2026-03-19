package com.netsage.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InputScreen(
    onDiagnose: (String) -> Unit,
    onFillSample: (String) -> Unit = {}
) {
    val logText = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("NetSage 网诊官", style = MaterialTheme.typography.headlineSmall)
        Text("粘贴日志后，获取 Top3 根因与修复建议")

        OutlinedTextField(
            value = logText.value,
            onValueChange = { logText.value = it },
            label = { Text("日志文本") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            minLines = 10
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val sample = "nslookup failed: NXDOMAIN; destination host unreachable"
                logText.value = sample
                onFillSample(sample)
            }) {
                Text("填充样例")
            }
            Button(onClick = { onDiagnose(logText.value) }, enabled = logText.value.isNotBlank()) {
                Text("开始诊断")
            }
        }
    }
}
