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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netsage.app.model.SavedReportItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedReportsScreen(
    items: List<SavedReportItem>,
    onBack: () -> Unit,
) {
    val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    var keyword by remember { mutableStateOf("") }
    val shown = items.filter {
        keyword.isBlank() ||
            it.title.contains(keyword, ignoreCase = true) ||
            it.summary.contains(keyword, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("收藏诊断", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("搜索标题 / 摘要") },
            singleLine = true,
        )

        if (items.isEmpty()) {
            Text("暂无收藏内容")
        } else if (shown.isEmpty()) {
            Text("没有匹配的收藏结果")
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
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(item.summary, style = MaterialTheme.typography.bodySmall)
                        Text(formatter.format(Date(item.createdAt)), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("返回首页")
        }
    }
}
