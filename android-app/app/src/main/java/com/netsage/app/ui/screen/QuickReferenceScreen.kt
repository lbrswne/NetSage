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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netsage.app.model.QuickRefCategory
import com.netsage.app.model.QuickRefItem

@Composable
fun QuickReferenceScreen(
    items: List<QuickRefItem>,
    onBack: () -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    var current by remember { mutableStateOf<QuickRefCategory?>(null) }
    val filteredByCategory = current?.let { c -> items.filter { it.category == c } } ?: items
    val shown = filteredByCategory.filter {
        keyword.isBlank() ||
            it.term.contains(keyword, ignoreCase = true) ||
            it.explanation.contains(keyword, ignoreCase = true) ||
            it.tips.contains(keyword, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("错误码 / 术语速查", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("搜索错误码 / 术语 / 建议") },
            singleLine = true,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { current = null }, label = { Text("全部") })
            QuickRefCategory.entries.forEach { category ->
                AssistChip(onClick = { current = category }, label = { Text(category.label) })
            }
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
                        Text(item.term, style = MaterialTheme.typography.titleMedium)
                        Text(item.explanation)
                        Text("建议：${item.tips}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("返回首页")
        }
    }
}
