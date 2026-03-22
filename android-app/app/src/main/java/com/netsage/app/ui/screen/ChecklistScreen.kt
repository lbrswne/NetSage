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
import com.netsage.app.model.TroubleshootingChecklist

@Composable
fun ChecklistScreen(
    items: List<TroubleshootingChecklist>,
    onBack: () -> Unit,
    highlightedId: String? = null,
) {
    var keyword by remember { mutableStateOf("") }
    val shown = items.filter {
        keyword.isBlank() ||
            it.title.contains(keyword, ignoreCase = true) ||
            it.notes.contains(keyword, ignoreCase = true) ||
            it.steps.any { step -> step.contains(keyword, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("排障清单", style = MaterialTheme.typography.headlineSmall)
        Text("按步骤完成常见网络问题排查")

        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("搜索清单 / 步骤 / 说明") },
            singleLine = true,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shown, key = { it.id }) { item ->
                val isHighlighted = highlightedId == item.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isHighlighted) Modifier.border(
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
                        Text("分类：${item.category.label}", style = MaterialTheme.typography.bodySmall)
                        item.steps.forEachIndexed { index, step ->
                            Text("${index + 1}. $step", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("说明：${item.notes}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("返回首页")
        }
    }
}
