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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netsage.app.model.SampleLogItem

@Composable
fun SampleCenterScreen(
    samples: List<SampleLogItem>,
    onUseSample: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("样例中心", style = MaterialTheme.typography.headlineSmall)
        Text("快速体验常见网络故障日志诊断")

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(samples) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text("分类：${item.category.label}", style = MaterialTheme.typography.bodySmall)
                        Text(item.hint, style = MaterialTheme.typography.bodySmall)
                        Text(item.content, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { onUseSample(item.content) }, modifier = Modifier.fillMaxWidth()) {
                            Text("使用这个样例")
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
