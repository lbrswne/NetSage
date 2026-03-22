package com.netsage.app.ui.screen

import androidx.compose.foundation.border
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
    selectedId: Long? = null,
) {
    val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    var keyword by remember { mutableStateOf("") }
    var expandedId by remember(selectedId) { mutableStateOf(selectedId) }
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
            items(shown, key = { it.id }) { item ->
                val expanded = expandedId == item.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (expanded) Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.medium,
                            ) else Modifier
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(formatter.format(Date(item.createdAt)), style = MaterialTheme.typography.labelMedium)
                        Text(item.summary, style = MaterialTheme.typography.bodySmall)
                        if (expanded) {
                            Text("收藏详情", style = MaterialTheme.typography.titleSmall)
                            Text("• 标题：${item.title}", style = MaterialTheme.typography.bodySmall)
                            Text("• 摘要：${item.summary}", style = MaterialTheme.typography.bodySmall)
                            Text("• 收藏时间：${formatter.format(Date(item.createdAt))}", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = { expandedId = if (expanded) null else item.id },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (expanded) "收起详情" else "查看详情")
                        }
                    }
                }
            }
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("返回首页")
        }
    }
}
