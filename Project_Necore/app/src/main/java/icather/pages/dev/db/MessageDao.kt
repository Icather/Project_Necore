package icather.pages.dev.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: Message): Long

    @Query("SELECT * FROM messages")
    suspend fun getAllMessages(): List<Message>

    @Query("SELECT * FROM messages WHERE conversationId IN (:conversationIds)")
    suspend fun getMessagesForConversationIds(conversationIds: List<Long>): List<Message>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesForConversation(conversationId: Long): List<Message>

    @Query("DELETE FROM messages")
    suspend fun clearAll()

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversationId(conversationId: Long)

    // E3: 删除指定消息及其之后的所有消息（通过 timestamp 定位）
    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND timestamp >= :fromTimestamp")
    suspend fun deleteMessagesFrom(conversationId: Long, fromTimestamp: Long)

    // E1: 删除对话中最后一条消息
    @Query("DELETE FROM messages WHERE id = (SELECT id FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1)")
    suspend fun deleteLastMessage(conversationId: Long)

    // E3: 按 ID 查询消息
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Long): Message?

    // F4: 用量统计 — 全局 Token 汇总
    @Query("SELECT COALESCE(SUM(inputTokens), 0) FROM messages WHERE isUser = 0")
    suspend fun getTotalInputTokens(): Long

    @Query("SELECT COALESCE(SUM(outputTokens), 0) FROM messages WHERE isUser = 0")
    suspend fun getTotalOutputTokens(): Long

    @Query("SELECT COALESCE(SUM(cacheHitTokens), 0) FROM messages WHERE isUser = 0")
    suspend fun getTotalCacheHitTokens(): Long

    // F4: 对话总数
    @Query("SELECT COUNT(DISTINCT conversationId) FROM messages")
    suspend fun getTotalConversationCount(): Int

    // F4: AI 消息总数
    @Query("SELECT COUNT(*) FROM messages WHERE isUser = 0")
    suspend fun getTotalAiMessageCount(): Int

    // F4: 按天统计 Token (最近 7 天)
    @Query("""
        SELECT 
            (timestamp / 86400000) as dayEpoch,
            COALESCE(SUM(inputTokens), 0) as totalInput,
            COALESCE(SUM(outputTokens), 0) as totalOutput
        FROM messages 
        WHERE isUser = 0 AND timestamp > :since
        GROUP BY dayEpoch 
        ORDER BY dayEpoch ASC
    """)
    suspend fun getDailyTokenStats(since: Long): List<DailyTokenStat>

    // 消息版本分支 — 查询同一根消息下的所有分支消息
    @Query("SELECT * FROM messages WHERE parentId = :rootId ORDER BY branchIndex ASC")
    suspend fun getSiblingBranches(rootId: Long): List<Message>

    // 消息版本分支 — 带分支信息的插入（返回新消息 ID）
    @Insert
    suspend fun insertAndGetId(message: Message): Long

    // F5: 按模型统计用量（缓存命中率等）
    @Query("""
        SELECT 
            modelName,
            COALESCE(SUM(inputTokens), 0) as totalInput,
            COALESCE(SUM(outputTokens), 0) as totalOutput,
            COALESCE(SUM(cacheHitTokens), 0) as totalCacheHit,
            COUNT(*) as messageCount
        FROM messages 
        WHERE isUser = 0 AND modelName IS NOT NULL AND modelName != ''
        GROUP BY modelName 
        ORDER BY totalInput DESC
    """)
    suspend fun getModelUsageStats(): List<ModelUsageStat>
}