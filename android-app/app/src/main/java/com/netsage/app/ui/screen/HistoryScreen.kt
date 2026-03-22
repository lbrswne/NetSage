package com.netsage.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netsage.app.model.DiagnoseHistoryRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    records: List<DiagnoseHistoryRecord>,
    onReuse: (DiagnoseHistoryRecord) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    var keyword by remember { mutableStateOf("") }
    val shown = records.filter {
        keyword.isBlank() ||
            it.inputSummary.contains(keyword, ignoreCase = true) ||
            it.resultSummary.contains(keyword, ignoreCase = true) ||
            it.inputText.contains(keyword, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("诊断历史", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("搜索输入摘要 / 结果摘要") },
            singleLine = true,
        )

        if (records.isEmpty()) {
            Text("暂无历史记录")
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shown) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(formatter.format(Date(item.timestamp)), style = MaterialTheme.typography.labelMedium)
                        Text("输入摘要：${item.inputSummary}")
                        Text("结果摘要：${item.resultSummary}", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { onReuse(item) }, modifier = Modifier.fillMaxWidth()) {
                            Text("回填到输入框")
                        }
                    }
                }
            }
        }

        OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth(), enabled = records.isNotEmpty()) {
            Text("清空历史")
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("返回首页")
        }
    }
}
