package com.netsage.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netsage.app.model.SavedReportItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun SavedSummaryCard(title: String, value: String, hint: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SavedReportsScreen(
    items: List<SavedReportItem>,
    onBack: () -> Unit,
    onReuse: (SavedReportItem) -> Unit,
    onDelete: (SavedReportItem) -> Unit,
    onCopySummary: (SavedReportItem) -> Unit,
    selectedId: Long? = null,
) {
    val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    var keyword by remember { mutableStateOf("") }
    var expandedId by remember(selectedId) { mutableStateOf(selectedId) }
    var pendingDeleteItem by remember { mutableStateOf<SavedReportItem?>(null) }
    val shown = items.filter {
        keyword.isBlank() ||
            it.title.contains(keyword, ignoreCase = true) ||
            it.summary.contains(keyword, ignoreCase = true)
    }

    pendingDeleteItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            title = { Text("确认删除收藏？") },
            text = { Text("将删除收藏“${item.title}”，删除后无法恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(item)
                        if (expandedId == item.id) expandedId = null
                        pendingDeleteItem = null
                    }
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDeleteItem = null }) {
                    Text("取消")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F7FB))
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
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF121725), Color(0xFF1B3757), Color(0xFF246B86))
                        )
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("NetSage", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
                Text("收藏诊断中心", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "沉淀重点诊断结果，支持继续回填、复制摘要与本地留存。",
                    color = Color.White.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SavedSummaryCard("收藏总数", items.size.toString(), if (items.isNotEmpty()) "已保存重点结果" else "尚无收藏")
            SavedSummaryCard("搜索结果", shown.size.toString(), if (keyword.isBlank()) "未启用筛选" else "当前筛选命中")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("搜索与浏览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索标题 / 摘要") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        if (items.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Text("暂无收藏内容", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        } else if (shown.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Text("没有匹配的收藏结果", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                shape = RoundedCornerShape(22.dp),
                            ) else Modifier
                        ),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(formatter.format(Date(item.createdAt)), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(item.summary, style = MaterialTheme.typography.bodyMedium)
                        if (expanded) {
                            Text("收藏详情", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("• 标题：${item.title}", style = MaterialTheme.typography.bodySmall)
                            Text("• 摘要：${item.summary}", style = MaterialTheme.typography.bodySmall)
                            Text("• 收藏时间：${formatter.format(Date(item.createdAt))}", style = MaterialTheme.typography.bodySmall)
                            if (item.inputText.isNotBlank()) {
                                Button(onClick = { onReuse(item) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("回填到输入框")
                                }
                            }
                            OutlinedButton(onClick = { onCopySummary(item) }, modifier = Modifier.fillMaxWidth()) {
                                Text("复制收藏摘要")
                            }
                            OutlinedButton(onClick = { pendingDeleteItem = item }, modifier = Modifier.fillMaxWidth()) {
                                Text("删除收藏")
                            }
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

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("返回首页")
        }
    }
}
