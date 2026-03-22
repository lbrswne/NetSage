package com.netsage.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netsage.app.model.FaultCategory
import com.netsage.app.model.FaultScenario

@Composable
fun ScenarioLibraryScreen(
    scenarios: List<FaultScenario>,
    onBack: () -> Unit
) {
    var current by remember { mutableStateOf<FaultCategory?>(null) }
    var keyword by remember { mutableStateOf("") }
    val expandedIds = remember { mutableStateListOf<String>() }
    val filteredByCategory = current?.let { c -> scenarios.filter { it.category == c } } ?: scenarios
    val shown = filteredByCategory.filter {
        keyword.isBlank() ||
            it.title.contains(keyword, ignoreCase = true) ||
            it.symptoms.contains(keyword, ignoreCase = true) ||
            it.fixHints.any { hint -> hint.contains(keyword, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("常见故障场景库", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("搜索场景 / 现象 / 修复建议") },
            singleLine = true,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { current = null }, label = { Text("全部") })
            FaultCategory.entries.forEach { category ->
                AssistChip(onClick = { current = category }, label = { Text(category.label) })
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shown) { item ->
                val expanded = item.id in expandedIds
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("[${item.category.label}] ${item.title}", style = MaterialTheme.typography.titleMedium)
                        Text("现象：${item.symptoms}")
                        if (expanded) {
                            Text("排查：${item.checks.joinToString("；")}", style = MaterialTheme.typography.bodySmall)
                            Text("修复：${item.fixHints.joinToString("；")}", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(
                            onClick = {
                                if (expanded) {
                                    expandedIds.remove(item.id)
                                } else {
                                    expandedIds.add(item.id)
                                }
                            }
                        ) {
                            Text(if (expanded) "收起详情" else "展开详情")
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
