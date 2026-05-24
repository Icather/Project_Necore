package icather.pages.dev.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConversationReferenceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ref: ConversationReference)

    @Query("DELETE FROM conversation_references WHERE conversationId = :conversationId AND referencedConversationId = :referencedConversationId")
    suspend fun delete(conversationId: Long, referencedConversationId: Long)

    @Query("SELECT * FROM conversation_references WHERE conversationId = :conversationId ORDER BY addedTime ASC")
    suspend fun getReferencesForConversation(conversationId: Long): List<ConversationReference>

    // 联表查询：直接返回被引用的 Conversation 列表
    @Query("""
        SELECT c.* FROM conversations c
        INNER JOIN conversation_references cr ON c.id = cr.referencedConversationId
        WHERE cr.conversationId = :conversationId
        ORDER BY cr.addedTime ASC
    """)
    suspend fun getReferencedConversations(conversationId: Long): List<Conversation>
}
