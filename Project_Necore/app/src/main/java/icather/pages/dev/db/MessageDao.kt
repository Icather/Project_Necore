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
}

