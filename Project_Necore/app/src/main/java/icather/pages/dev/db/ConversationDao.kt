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

    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, startTime DESC")
    suspend fun getAllConversations(): List<Conversation>

    @Query("SELECT * FROM conversations WHERE id IN (:conversationIds)")
    suspend fun getConversationsByIds(conversationIds: List<Long>): List<Conversation>

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversation(conversationId: Long): Conversation?

    @Query("DELETE FROM conversations")
    suspend fun clearAll()

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteById(conversationId: Long)

    // 置顶/取消置顶
    @Query("UPDATE conversations SET isPinned = :pinned WHERE id = :conversationId")
    suspend fun setPinned(conversationId: Long, pinned: Boolean)

    // 重命名对话
    @Query("UPDATE conversations SET title = :newTitle WHERE id = :conversationId")
    suspend fun rename(conversationId: Long, newTitle: String)

    // E5: 搜索 — 标题匹配 + 消息内容全文搜索
    @Query("""
        SELECT DISTINCT c.* FROM conversations c 
        LEFT JOIN messages m ON c.id = m.conversationId 
        WHERE c.title LIKE '%' || :query || '%' 
           OR m.text LIKE '%' || :query || '%'
        ORDER BY c.isPinned DESC, c.startTime DESC
    """)
    suspend fun searchConversations(query: String): List<Conversation>
}
