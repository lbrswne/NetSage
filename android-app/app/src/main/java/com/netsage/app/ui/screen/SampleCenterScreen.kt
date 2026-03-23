package com.netsage.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netsage.app.model.SampleLogItem
import com.netsage.app.ui.theme.NetSageHeroGradient

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SampleCenterScreen(
    samples: List<SampleLogItem>,
    onUseSample: (String) -> Unit,
    onBack: () -> Unit,
) {
    var currentCategory by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.netsage.app.model.FaultCategory?>(null) }
    val shown = currentCategory?.let { c -> samples.filter { it.category == c } } ?: samples

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack) {
                Text("返回")
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .background(NetSageHeroGradient)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("NetSage", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
                Text("样例中心", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("快速体验典型网络故障日志，适合演示、试跑与验证推荐链路。", color = Color.White.copy(alpha = 0.84f), style = MaterialTheme.typography.bodyMedium)
                Text("当前共 ${samples.size} 条样例，点击即可直接带入输入页继续诊断。", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("使用说明", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("• 样例用于离线演示与回归验收，不依赖联网链路。", style = MaterialTheme.typography.bodySmall)
                Text("• 点击“使用这个样例”后，会直接回填到输入页继续诊断。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { currentCategory = null }, label = { Text("全部") })
                    com.netsage.app.model.FaultCategory.entries.forEach { category ->
                        AssistChip(onClick = { currentCategory = category }, label = { Text(category.label) })
                    }
                }
                Text("当前显示 ${shown.size} 条样例", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { shown.randomOrNull()?.let { onUseSample(it.content) } },
                        modifier = Modifier.weight(1f),
                        enabled = shown.isNotEmpty()
                    ) {
                        Text("抽一条样例")
                    }
                    OutlinedButton(
                        onClick = { samples.randomOrNull()?.let { onUseSample(it.content) } },
                        modifier = Modifier.weight(1f),
                        enabled = samples.isNotEmpty()
                    ) {
                        Text("全库随机")
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(shown) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("分类：${item.category.label}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(item.hint, style = MaterialTheme.typography.bodyMedium)
                        Text(item.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { onUseSample(item.content) }, modifier = Modifier.fillMaxWidth()) {
                            Text("使用这个样例")
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
