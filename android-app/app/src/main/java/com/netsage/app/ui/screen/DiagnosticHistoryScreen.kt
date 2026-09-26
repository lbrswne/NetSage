package com.netsage.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import com.netsage.app.diagnostic.session.DiagnosticSessionMode
import com.netsage.app.ui.theme.NetSageHeroGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticHistoryItemUi(
    val id: String,
    val createdAt: Long,
    val mode: String,
    val sessionMode: DiagnosticSessionMode,
    val target: String,
    val summary: String,
    val probeSummary: String,
)

enum class HistoryModeFilter(val label: String) {
    ALL("全部"), CHECKUP("体检/组合"), LOG("日志"), RETEST("复测")
}

fun filterDiagnosticHistory(
    items: List<DiagnosticHistoryItemUi>,
    query: String,
    modeFilter: HistoryModeFilter,
): List<DiagnosticHistoryItemUi> {
    val search = query.trim()
    return items.filter { item ->
        val modeMatches = when (modeFilter) {
            HistoryModeFilter.ALL -> true
            HistoryModeFilter.CHECKUP -> item.sessionMode == DiagnosticSessionMode.QUICK_CHECKUP || item.sessionMode == DiagnosticSessionMode.COMBINED
            HistoryModeFilter.LOG -> item.sessionMode == DiagnosticSessionMode.LOG_ANALYSIS
            HistoryModeFilter.RETEST -> item.sessionMode == DiagnosticSessionMode.RETEST
        }
        modeMatches && (search.isEmpty() || item.target.contains(search, ignoreCase = true) || item.summary.contains(search, ignoreCase = true))
    }
}

@Composable
fun DiagnosticHistoryScreen(
    items: List<DiagnosticHistoryItemUi>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onShare: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClear: () -> Unit,
) {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    var showClearConfirm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DiagnosticHistoryItemUi?>(null) }
    var pendingShareId by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var modeFilter by remember { mutableStateOf(HistoryModeFilter.ALL) }
    val visibleItems = filterDiagnosticHistory(items, search, modeFilter)
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("确认清空全部本地会话？") },
            text = { Text("将删除全部 ${items.size} 条本地会话，删除后无法恢复。") },
            confirmButton = {
                Button(onClick = {
                    if (showClearConfirm) {
                        showClearConfirm = false
                        onClear()
                    }
                }) { Text("确认清空") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }
    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("确认删除本地会话？") },
            text = { Text("将删除会话「${item.target}」，删除后无法恢复。") },
            confirmButton = {
                Button(onClick = {
                    pendingDelete = null
                    onDelete(item.id)
                }) { Text("确认删除") }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
    pendingShareId?.let { id ->
        SharePrivacyDialog(
            onDismiss = { pendingShareId = null },
            onConfirm = {
                pendingShareId = null
                onShare(id)
            },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("返回") }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.background(NetSageHeroGradient).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("NetSage · On-device", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
                Text("诊断会话", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("本机保存诊断输入、网络快照、探测证据、Top 3 假设和复测差异。最多保留 20 条；每条原始日志最多 100,000 个字符。", color = Color.White.copy(alpha = 0.84f))
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("本地会话 ${items.size} 条", fontWeight = FontWeight.SemiBold)
                Text("删除后无法恢复；导出文件由你自行选择保存位置。", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (items.isNotEmpty()) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("搜索目标地址或摘要") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HistoryModeFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = modeFilter == filter,
                        onClick = { modeFilter = filter },
                        label = { Text(filter.label) },
                    )
                }
            }
            Text("筛选结果 ${visibleItems.size} 条", style = MaterialTheme.typography.bodySmall)
        }
        if (items.isEmpty()) {
            Text("还没有 v0.2 诊断会话。完成日志诊断或本地体检后会显示在这里。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (visibleItems.isEmpty()) {
            Text("没有符合条件的会话。请调整搜索词或筛选模式。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        visibleItems.forEach { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(formatter.format(Date(item.createdAt)), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("${item.mode} · ${item.target}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(item.summary, style = MaterialTheme.typography.bodyMedium)
                    Text(item.probeSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { onOpen(item.id) }, modifier = Modifier.fillMaxWidth()) { Text("查看会话详情") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pendingShareId = item.id }, modifier = Modifier.weight(1f)) { Text("分享报告") }
                        OutlinedButton(onClick = { pendingDelete = item }, modifier = Modifier.weight(1f)) { Text("删除") }
                    }
                }
            }
        }
        if (items.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showClearConfirm = true }, modifier = Modifier.fillMaxWidth()) { Text("清空全部本地会话") }
            }
        }
    }
}
