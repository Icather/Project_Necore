package icather.pages.dev.db

/**
 * F5: 按模型聚合的用量统计
 *
 * 用于在用量看板中展示每个模型的 Token 消耗与缓存命中率。
 */
data class ModelUsageStat(
    val modelName: String?,
    val totalInput: Long,
    val totalOutput: Long,
    val totalCacheHit: Long,
    val messageCount: Long
)