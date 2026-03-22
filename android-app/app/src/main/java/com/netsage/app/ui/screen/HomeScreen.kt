package com.netsage.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class HomeModule(
    val title: String,
    val desc: String,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    modules: List<HomeModule>,
    latestRecordSummary: String?,
    savedReportCount: Int,
    onQuickOpenInput: () -> Unit,
    onQuickOpenHistory: () -> Unit,
    onQuickOpenReference: () -> Unit,
    onReuseLatestRecord: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenUserAgreement: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    val featured = modules.take(2)
    val others = modules.drop(2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("NetSage 离线网络诊断工具箱", style = MaterialTheme.typography.headlineSmall)
        Text("单机版 · 无需登录 · 无需联网 · 诊断与记录默认本地处理")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("收藏数：$savedReportCount", style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (savedReportCount > 0) "已有可回看的诊断收藏" else "暂无收藏诊断",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text("常用入口", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = onQuickOpenInput, label = { Text("去诊断") })
            AssistChip(onClick = onQuickOpenHistory, label = { Text("看历史") })
            AssistChip(onClick = onQuickOpenReference, label = { Text("查术语") })
        }

        latestRecordSummary?.let { summary ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReuseLatestRecord() }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("最近一次诊断", style = MaterialTheme.typography.titleMedium)
                    Text(summary, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = onReuseLatestRecord, label = { Text("直接回填输入框") })
                        AssistChip(onClick = onOpenHistory, label = { Text("查看历史") })
                    }
                }
            }
        }

        Text("核心功能", style = MaterialTheme.typography.titleMedium)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(132.dp)
        ) {
            items(featured) { module ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clickable { module.onClick() }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(module.title, style = MaterialTheme.typography.titleMedium)
                            Text(module.desc, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Text("工具与记录", style = MaterialTheme.typography.titleMedium)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(others) { module ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clickable { module.onClick() }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(module.title, style = MaterialTheme.typography.titleMedium)
                            Text(module.desc, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Text(
            "点击上方协议文字可查看详细内容",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
