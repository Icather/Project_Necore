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
        cacheHitTokens: Int? = null,
        modelName: String? = null
    ): Long = withContext(Dispatchers.IO) {
        db.messageDao().insert(Message(
            conversationId = conversationId, 
            text = text, 
            isUser = isUser, 
            isHtml = isHtml,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            cacheHitTokens = cacheHitTokens,
            modelName = modelName
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

    // 消息版本分支 — 带分支信息的消息保存
    suspend fun saveMessageWithBranch(
        conversationId: Long,
        text: String,
        isUser: Boolean,
        isHtml: Boolean = false,
        inputTokens: Int? = null,
        outputTokens: Int? = null,
        cacheHitTokens: Int? = null,
        modelName: String? = null,
        parentId: Long? = null,
        branchIndex: Int = 0
    ): Long = withContext(Dispatchers.IO) {
        db.messageDao().insertAndGetId(Message(
            conversationId = conversationId,
            text = text,
            isUser = isUser,
            isHtml = isHtml,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            cacheHitTokens = cacheHitTokens,
            modelName = modelName,
            parentId = parentId,
            branchIndex = branchIndex
        ))
    }

    // 更新对话最后使用的模型
    suspend fun setConversationLastModel(conversationId: Long, modelName: String) = withContext(Dispatchers.IO) {
        db.conversationDao().setLastModelName(conversationId, modelName)
    }

    // 消息版本分支 — 查询同一根消息下的所有分支
    suspend fun getSiblingBranches(rootId: Long): List<Message> = withContext(Dispatchers.IO) {
        db.messageDao().getSiblingBranches(rootId)
    }

    /**
     * 首次安装时自动生成示例对话。
     * 仅在对话列表为空时触发，展示 Necore 的功能介绍。
     */
    suspend fun ensureSampleConversation() = withContext(Dispatchers.IO) {
        if (db.conversationDao().getConversationCount() > 0) return@withContext

        val convoId = db.conversationDao().insert(Conversation(
            title = "欢迎使用 Necore",
            startTime = System.currentTimeMillis() - 60000
        ))

        db.messageDao().insert(Message(
            conversationId = convoId,
            text = "你好！我是 Necore —— 你的智能 AI 对话助手。\n\n" +
                   "✨ 我可以做什么？\n" +
                   "• 让你的 AI 拥有「人设」和「长期记忆」\n" +
                   "• 支持多模态：图片识别、文件分析\n" +
                   "• 强大的协议引擎：兼容 OpenAI / DeepSeek / 通义千问 等主流 LLM\n" +
                   "• 思考链 + 联网搜索 + 自动模型降级\n" +
                   "• 消息版本分支：编辑已发送的内容，探索不同回复方向\n\n" +
                   "从「设置」配置你的 API Key，就可以开始对话了 🚀",
            isUser = false,
            isHtml = false,
            timestamp = System.currentTimeMillis() - 30000,
            modelName = null
        ))
    }
}