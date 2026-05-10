package icather.pages.dev.db

/**
 * F4: Room 查询返回的每日 Token 统计数据
 * dayEpoch = timestamp / 86400000 (按天分组的纪元天数)
 */
data class DailyTokenStat(
    val dayEpoch: Long,
    val totalInput: Long,
    val totalOutput: Long
)
