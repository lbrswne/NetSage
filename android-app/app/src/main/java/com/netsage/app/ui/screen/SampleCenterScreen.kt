package com.netsage.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netsage.app.model.FaultCategory
import com.netsage.app.model.SampleLogItem
import com.netsage.app.ui.theme.NetSageHeroGradient

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SampleCenterScreen(
    samples: List<SampleLogItem>,
    onUseSample: (String) -> Unit,
    onBack: () -> Unit,
) {
    val currentCategory = remember { mutableStateOf<FaultCategory?>(null) }
    val shown = currentCategory.value?.let { category -> samples.filter { it.category == category } } ?: samples
    val listState = rememberLazyListState()
    val heroProgress by rememberHeroCollapseProgress(listState)
    val heroCorner = 28.dp - (12.dp * heroProgress)
    val heroScale = 1f - (0.065f * heroProgress)
    val heroAlpha = 1f - (0.1f * heroProgress)
    val heroOverlayAlpha = 0.08f + (0.16f * heroProgress)
    val heroTitleAlpha = 1f - (0.24f * heroProgress)
    val heroBodyAlpha = 0.88f - (0.3f * heroProgress)
    val heroMetaAlpha = 0.8f - (0.26f * heroProgress)
    val heroBadgeAlpha = 0.74f - (0.22f * heroProgress)
    val heroHorizontalPadding = 20.dp - (6.dp * heroProgress)
    val heroVerticalPadding = 20.dp - (8.dp * heroProgress)
    val heroSpacing = 12.dp - (5.dp * heroProgress)
    val stickyTopPadding = 6.dp + (6.dp * heroProgress)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(onClick = onBack) {
                        Text("返回")
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp - (34.dp * heroProgress))
                )
            }

            item {
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
                        Text(
                            "• 点击“使用这个样例”后，会直接回填到输入页继续诊断。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(onClick = { currentCategory.value = null }, label = { Text("全部") })
                            FaultCategory.entries.forEach { category ->
                                AssistChip(onClick = { currentCategory.value = category }, label = { Text(category.label) })
                            }
                        }
                        Text(
                            "当前显示 ${shown.size} 条样例",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
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
            }

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
                        Text(
                            "分类：${item.category.label}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(item.hint, style = MaterialTheme.typography.bodyMedium)
                        Text(item.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { onUseSample(item.content) }, modifier = Modifier.fillMaxWidth()) {
                            Text("使用这个样例")
                        }
                    }
                }
            }

            item {
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("返回首页")
                }
            }
        }

        SampleCenterHero(
            samplesCount = samples.size,
            progress = heroProgress,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 16.dp, end = 16.dp, top = 66.dp + stickyTopPadding)
                .graphicsLayer {
                    scaleX = heroScale
                    scaleY = heroScale
                    alpha = heroAlpha
                },
            corner = heroCorner,
            horizontalPadding = heroHorizontalPadding,
            verticalPadding = heroVerticalPadding,
            spacing = heroSpacing,
            titleAlpha = heroTitleAlpha,
            bodyAlpha = heroBodyAlpha,
            metaAlpha = heroMetaAlpha,
            badgeAlpha = heroBadgeAlpha,
            overlayAlpha = heroOverlayAlpha
        )
    }
}

@Composable
private fun SampleCenterHero(
    samplesCount: Int,
    progress: Float,
    modifier: Modifier = Modifier,
    corner: androidx.compose.ui.unit.Dp,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    verticalPadding: androidx.compose.ui.unit.Dp,
    spacing: androidx.compose.ui.unit.Dp,
    titleAlpha: Float,
    bodyAlpha: Float,
    metaAlpha: Float,
    badgeAlpha: Float,
    overlayAlpha: Float,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner)),
        shape = RoundedCornerShape(corner),
        color = Color.Transparent,
        shadowElevation = 3.dp + (5.dp * progress)
    ) {
        Box {
            Column(
                modifier = Modifier
                    .background(NetSageHeroGradient)
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Text(
                    "NetSage",
                    color = Color.White.copy(alpha = badgeAlpha),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    "样例中心",
                    color = Color.White.copy(alpha = titleAlpha),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "快速体验典型网络故障日志，适合演示、试跑与验证推荐链路。",
                    color = Color.White.copy(alpha = bodyAlpha),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "当前共 ${samplesCount} 条样例，点击即可直接带入输入页继续诊断。",
                    color = Color.White.copy(alpha = metaAlpha),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = overlayAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun rememberHeroCollapseProgress(listState: LazyListState): State<Float> = remember(listState) {
    derivedStateOf {
        val offset = when {
            listState.firstVisibleItemIndex == 0 -> 0f
            listState.firstVisibleItemIndex == 1 -> listState.firstVisibleItemScrollOffset.toFloat()
            else -> 220f
        }
        (offset / 220f).coerceIn(0f, 1f)
    }
}
