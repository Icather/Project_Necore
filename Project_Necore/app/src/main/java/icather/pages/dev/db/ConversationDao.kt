package icather.pages.dev.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ConversationDao {
    @Insert
    suspend fun insert(conversation: Conversation): Long

    @Update
    suspend fun update(conversation: Conversation)

    @Query("SELECT * FROM conversations ORDER BY startTime DESC")
    suspend fun getAllConversations(): List<Conversation>

    @Query("SELECT * FROM conversations WHERE id IN (:conversationIds)")
    suspend fun getConversationsByIds(conversationIds: List<Long>): List<Conversation>

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversation(conversationId: Long): Conversation?

    @Query("DELETE FROM conversations")
    suspend fun clearAll()

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteById(conversationId: Long)

    // E5: 搜索 — 标题匹配 + 消息内容全文搜索
    @Query("""
        SELECT DISTINCT c.* FROM conversations c 
        LEFT JOIN messages m ON c.id = m.conversationId 
        WHERE c.title LIKE '%' || :query || '%' 
           OR m.text LIKE '%' || :query || '%'
        ORDER BY c.startTime DESC
    """)
    suspend fun searchConversations(query: String): List<Conversation>
}
