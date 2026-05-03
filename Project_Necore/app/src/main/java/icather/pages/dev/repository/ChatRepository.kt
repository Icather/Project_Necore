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

    suspend fun saveMessage(conversationId: Long, text: String, isUser: Boolean, isHtml: Boolean = false): Long = withContext(Dispatchers.IO) {
        db.messageDao().insert(Message(conversationId = conversationId, text = text, isUser = isUser, isHtml = isHtml))
    }

    suspend fun performOcr(apiService: ApiService, imageUri: Uri, apiKey: String): String = withContext(Dispatchers.IO) {
        apiService.performOcr(imageUri, apiKey)
    }

    fun getCompletion(apiService: ApiService, messages: List<ApiService.ApiMessage>, apiKey: String): Flow<ApiService.ApiResponseChunk> {
        return apiService.getCompletion(messages, apiKey).flowOn(Dispatchers.IO)
    }
}
