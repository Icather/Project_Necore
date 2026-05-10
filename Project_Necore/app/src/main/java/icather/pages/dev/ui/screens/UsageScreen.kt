package icather.pages.dev.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import icather.pages.dev.db.DailyTokenStat
import java.text.SimpleDateFormat
import java.util.*

/**
 * F4: API 用量统计数据模型
 */
data class UsageStats(
    val totalInputTokens: Long = 0,
    val totalOutputTokens: Long = 0,
    val totalCacheHitTokens: Long = 0,
    val totalConversations: Int = 0,
    val totalAiMessages: Int = 0,
    val dailyStats: List<DailyTokenStat> = emptyList()
)

/**
 * F4: 用量统计看板界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageScreen(
    stats: UsageStats,
    onNavigateBack: () -> Unit
) {
    val totalTokens = stats.totalInputTokens + stats.totalOutputTokens

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用量统计") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 总览卡片
            item {
                Text(
                    "总览",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Token,
                        label = "总 Token",
                        value = formatLargeNumber(totalTokens),
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Chat,
                        label = "对话数",
                        value = stats.totalConversations.toString(),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Input,
                        label = "输入 Token",
                        value = formatLargeNumber(stats.totalInputTokens),
                        color = Color(0xFF4CAF50)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Output,
                        label = "输出 Token",
                        value = formatLargeNumber(stats.totalOutputTokens),
                        color = Color(0xFFFF9800)
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Cached,
                        label = "缓存命中",
                        value = formatLargeNumber(stats.totalCacheHitTokens),
                        color = Color(0xFF9C27B0)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.SmartToy,
                        label = "AI 回复数",
                        value = stats.totalAiMessages.toString(),
                        color = Color(0xFF2196F3)
                    )
                }
            }

            // 7 天趋势图
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "最近 7 天趋势",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            item {
                if (stats.dailyStats.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            "暂无数据",
                            modifier = Modifier.padding(32.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    TokenTrendChart(
                        dailyStats = stats.dailyStats,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }

            // 平均值
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "效率指标",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val avgTokenPerMsg = if (stats.totalAiMessages > 0) totalTokens / stats.totalAiMessages else 0L
                        val avgOutputPerMsg = if (stats.totalAiMessages > 0) stats.totalOutputTokens / stats.totalAiMessages else 0L
                        val cacheRate = if (stats.totalInputTokens > 0) (stats.totalCacheHitTokens * 100.0 / stats.totalInputTokens) else 0.0

                        MetricRow("平均每次对话 Token", formatLargeNumber(avgTokenPerMsg))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        MetricRow("平均每次回复字数", formatLargeNumber(avgOutputPerMsg))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        MetricRow("缓存命中率", String.format("%.1f%%", cacheRate))
                    }
                }
            }
        }
    }
}

/** 统计卡片组件 */
@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 效率指标行 */
@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** 7 天 Token 趋势折线图（Canvas 手绘） */
@Composable
private fun TokenTrendChart(
    dailyStats: List<DailyTokenStat>,
    modifier: Modifier = Modifier
) {
    val inputColor = Color(0xFF4CAF50)
    val outputColor = Color(0xFFFF9800)
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 图例
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                LegendDot(color = inputColor, label = "输入")
                LegendDot(color = outputColor, label = "输出")
            }

            // 折线图
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (dailyStats.isEmpty()) return@Canvas

                val maxVal = dailyStats.maxOf { maxOf(it.totalInput, it.totalOutput) }.coerceAtLeast(1)
                val stepX = size.width / (dailyStats.size - 1).coerceAtLeast(1)
                val paddingTop = 8f
                val chartHeight = size.height - paddingTop

                fun yFor(value: Long) = paddingTop + chartHeight * (1f - value.toFloat() / maxVal)

                // 输入线
                val inputPath = Path().apply {
                    dailyStats.forEachIndexed { i, stat ->
                        val x = i * stepX
                        val y = yFor(stat.totalInput)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(inputPath, inputColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

                // 输出线
                val outputPath = Path().apply {
                    dailyStats.forEachIndexed { i, stat ->
                        val x = i * stepX
                        val y = yFor(stat.totalOutput)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(outputPath, outputColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

                // 数据点
                dailyStats.forEachIndexed { i, stat ->
                    val x = i * stepX
                    drawCircle(inputColor, 4f, Offset(x, yFor(stat.totalInput)))
                    drawCircle(outputColor, 4f, Offset(x, yFor(stat.totalOutput)))
                }
            }

            // X 轴日期标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dailyStats.forEach { stat ->
                    val date = Date(stat.dayEpoch * 86400000)
                    Text(
                        text = dateFormat.format(date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

/** 图例小圆点 */
@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 大数字格式化 */
private fun formatLargeNumber(value: Long): String {
    return when {
        value >= 1_000_000_000 -> String.format("%.1fB", value / 1_000_000_000.0)
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
        else -> value.toString()
    }
}
