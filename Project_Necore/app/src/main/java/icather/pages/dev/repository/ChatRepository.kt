package icather.pages.dev.repository

import android.content.Context
import android.net.Uri
import icather.pages.dev.api.ApiService
import icather.pages.dev.api.ApiServiceFactory
import icather.pages.dev.db.ApiConfig
import icather.pages.dev.db.AppDatabase
import icather.pages.dev.db.Conversation
import icather.pages.dev.db.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ChatRepository(private val context: Context, private val db: AppDatabase) {

    private val prefs = context.getSharedPreferences("api_prefs", Context.MODE_PRIVATE)

    fun isImageCompressionEnabled(): Boolean {
        return prefs.getBoolean("image_compression_enabled", true)
    }

    fun getContext(): Context = context

    fun getAllApiConfigs(): Flow<List<ApiConfig>> {
        return db.apiConfigDao().getAll().flowOn(Dispatchers.IO)
    }

    fun getActiveApiId(): Long {
        return prefs.getLong("active_api_id", -1L) // Using -1L to detect uninitialized
    }

    fun setActiveApiId(id: Long) {
        prefs.edit().putLong("active_api_id", id).apply()
    }

    fun createApiService(provider: String): ApiService {
        return ApiServiceFactory.create(provider)
    }

    suspend fun getConversation(conversationId: Long): Conversation? = withContext(Dispatchers.IO) {
        db.conversationDao().getConversation(conversationId)
    }

    suspend fun getMessagesForConversation(conversationId: Long): List<Message> = withContext(Dispatchers.IO) {
        db.messageDao().getMessagesForConversation(conversationId)
    }

    suspend fun createNewConversation(title: String): Long = withContext(Dispatchers.IO) {
        db.conversationDao().insert(Conversation(title = title))
    }

    suspend fun saveMessage(
        conversationId: Long, 
        text: String, 
        isUser: Boolean, 
        isHtml: Boolean = false,
        inputTokens: Int? = null,
        outputTokens: Int? = null,
        cacheHitTokens: Int? = null
    ): Long = withContext(Dispatchers.IO) {
        db.messageDao().insert(Message(
            conversationId = conversationId, 
            text = text, 
            isUser = isUser, 
            isHtml = isHtml,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            cacheHitTokens = cacheHitTokens
        ))
    }

    suspend fun performOcr(apiService: ApiService, imageUri: Uri, apiKey: String): String = withContext(Dispatchers.IO) {
        apiService.performOcr(imageUri, apiKey)
    }

    fun getCompletion(apiService: ApiService, messages: List<ApiService.ApiMessage>, apiKey: String, options: Map<String, Any> = emptyMap()): Flow<ApiService.ApiResponseChunk> {
        return apiService.getCompletion(messages, apiKey, options).flowOn(Dispatchers.IO)
    }

    // E3: 删除指定时间戳及之后的所有消息
    suspend fun deleteMessagesFrom(conversationId: Long, fromTimestamp: Long) = withContext(Dispatchers.IO) {
        db.messageDao().deleteMessagesFrom(conversationId, fromTimestamp)
    }

    // E1: 删除对话中最后一条消息
    suspend fun deleteLastMessage(conversationId: Long) = withContext(Dispatchers.IO) {
        db.messageDao().deleteLastMessage(conversationId)
    }

    // H1: 侧边栏 — 获取全部对话列表
    suspend fun getAllConversations(): List<Conversation> = withContext(Dispatchers.IO) {
        db.conversationDao().getAllConversations()
    }

    // H1: 侧边栏 — 搜索对话
    suspend fun searchConversations(query: String): List<Conversation> = withContext(Dispatchers.IO) {
        db.conversationDao().searchConversations(query)
    }

    // H1: 侧边栏 — 删除对话
    suspend fun deleteConversation(conversationId: Long) = withContext(Dispatchers.IO) {
        db.messageDao().deleteByConversationId(conversationId)
        db.conversationDao().deleteById(conversationId)
    }

    // H1: 侧边栏 — 置顶/取消置顶
    suspend fun setPinned(conversationId: Long, pinned: Boolean) = withContext(Dispatchers.IO) {
        db.conversationDao().setPinned(conversationId, pinned)
    }

    // H1: 侧边栏 — 重命名对话
    suspend fun renameConversation(conversationId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        db.conversationDao().rename(conversationId, newTitle)
    }
}

