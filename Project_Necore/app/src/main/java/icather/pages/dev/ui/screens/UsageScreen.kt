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
import icather.pages.dev.db.ModelUsageStat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import icather.pages.dev.R

/**
 * F4: API 用量统计数据模型
 */
data class UsageStats(
    val totalInputTokens: Long = 0,
    val totalOutputTokens: Long = 0,
    val totalCacheHitTokens: Long = 0,
    val totalConversations: Int = 0,
    val totalAiMessages: Int = 0,
    val dailyStats: List<DailyTokenStat> = emptyList(),
    val modelStats: List<ModelUsageStat> = emptyList()
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
                title = { Text(stringResource(R.string.usage_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                    stringResource(R.string.usage_overview),
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
                        label = stringResource(R.string.usage_total_tokens),
                        value = formatLargeNumber(totalTokens),
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Chat,
                        label = stringResource(R.string.usage_conversations),
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
                        label = stringResource(R.string.usage_input_tokens),
                        value = formatLargeNumber(stats.totalInputTokens),
                        color = Color(0xFF4CAF50)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Output,
                        label = stringResource(R.string.usage_output_tokens),
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
                        label = stringResource(R.string.usage_cache_hits),
                        value = formatLargeNumber(stats.totalCacheHitTokens),
                        color = Color(0xFF9C27B0)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.SmartToy,
                        label = stringResource(R.string.usage_ai_replies),
                        value = stats.totalAiMessages.toString(),
                        color = Color(0xFF2196F3)
                    )
                }
            }

            // 7 天趋势图
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.usage_7day_trend),
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
                            stringResource(R.string.usage_no_data),
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
                    stringResource(R.string.usage_efficiency),
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

                        MetricRow(stringResource(R.string.usage_avg_tokens_per_chat), formatLargeNumber(avgTokenPerMsg))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        MetricRow(stringResource(R.string.usage_avg_words_per_reply), formatLargeNumber(avgOutputPerMsg))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        MetricRow(stringResource(R.string.usage_cache_hit_rate), String.format("%.1f%%", cacheRate))
                    }
                }
            }

            // F5: 按模型用量明细
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.usage_by_model),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            if (stats.modelStats.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            stringResource(R.string.usage_no_model_data),
                            modifier = Modifier.padding(32.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                stats.modelStats.forEach { model ->
                    item {
                        ModelUsageCard(model)
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
                LegendDot(color = inputColor, label = stringResource(R.string.legend_input))
                LegendDot(color = outputColor, label = stringResource(R.string.legend_output))
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

/** F5: 单个模型的用量卡片 */
@Composable
private fun ModelUsageCard(stat: ModelUsageStat) {
    val modelName = stat.modelName ?: stringResource(R.string.usage_unknown_model)
    val totalModelTokens = stat.totalInput + stat.totalOutput
    val cacheRate = if (stat.totalInput > 0) (stat.totalCacheHit * 100.0 / stat.totalInput) else 0.0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 模型名标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = modelName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.usage_replies_count, stat.messageCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 两行指标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatLargeNumber(totalModelTokens),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.usage_total_token_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatLargeNumber(stat.totalInput),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = stringResource(R.string.usage_input_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatLargeNumber(stat.totalOutput),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                    Text(
                        text = stringResource(R.string.usage_output_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.1f%%", cacheRate),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9C27B0)
                    )
                    Text(
                        text = stringResource(R.string.usage_cache_hit_rate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
