package icather.pages.dev.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import icather.pages.dev.ChatHistoryBundle
import icather.pages.dev.db.ApiConfig
import icather.pages.dev.db.AppDatabase
import icather.pages.dev.db.Conversation
import icather.pages.dev.db.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.MessageDigest

class SettingsRepository(private val context: Context, private val db: AppDatabase) {

    private val gson = Gson()
    private val prefs = context.getSharedPreferences("api_prefs", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_API_ID = 1L
        private const val KEY_IMAGE_COMPRESSION = "image_compression_enabled"
    }

    fun getAllApiConfigs() = db.apiConfigDao().getAll()

    fun isImageCompressionEnabled(): Boolean {
        return prefs.getBoolean(KEY_IMAGE_COMPRESSION, true) // 默认开启
    }

    fun setImageCompressionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IMAGE_COMPRESSION, enabled).apply()
    }

    val activeApiConfigId: kotlinx.coroutines.flow.Flow<Long> = kotlinx.coroutines.flow.flow {
        while (true) {
            emit(prefs.getLong("active_api_id", DEFAULT_API_ID))
            kotlinx.coroutines.delay(500) // Simple polling for now
        }
    }

    suspend fun setActiveApiConfigId(id: Long) = withContext(Dispatchers.IO) {
        prefs.edit().putLong("active_api_id", id).apply()
    }

    suspend fun insertApiConfig(config: ApiConfig) = withContext(Dispatchers.IO) {
        db.apiConfigDao().insert(config)
    }

    suspend fun updateApiConfig(config: ApiConfig) = withContext(Dispatchers.IO) {
        db.apiConfigDao().update(config)
    }

    suspend fun deleteApiConfig(config: ApiConfig) = withContext(Dispatchers.IO) {
        db.apiConfigDao().deleteById(config.id)
    }

    suspend fun getApiConfigsJson(): String? = withContext(Dispatchers.IO) {
        val apiConfigs = db.apiConfigDao().getAllOnce()
        if (apiConfigs.isEmpty()) return@withContext null
        gson.toJson(apiConfigs)
    }

    suspend fun exportApiConfigsToUri(uri: Uri, json: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.writer(Charsets.UTF_8)?.use { it.write(json) }
                ?: throw Exception("Failed to open output stream")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importApiConfigsFromUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { 
                BufferedReader(InputStreamReader(it)).readText() 
            } ?: throw Exception("Failed to read file")
            
            val type = object : TypeToken<List<ApiConfig>>() {}.type
            val importedConfigs: List<ApiConfig> = gson.fromJson(json, type)
            
            db.apiConfigDao().insertAll(importedConfigs.map { it.copy(id = 0) })
            Result.success(importedConfigs.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllChatHistoryJson(): String? = withContext(Dispatchers.IO) {
        val conversations = db.conversationDao().getAllConversations()
        val messages = db.messageDao().getAllMessages()
        if (conversations.isEmpty() && messages.isEmpty()) return@withContext null

        val bundle = ChatHistoryBundle(conversations, messages)
        gson.toJson(bundle)
    }

    suspend fun getSelectedChatHistoryJson(selectedIds: List<Long>): String? = withContext(Dispatchers.IO) {
        if (selectedIds.isEmpty()) return@withContext null
        val selectedConversations = db.conversationDao().getConversationsByIds(selectedIds)
        val selectedMessages = db.messageDao().getMessagesForConversationIds(selectedIds)
        val bundle = ChatHistoryBundle(selectedConversations, selectedMessages)
        gson.toJson(bundle)
    }

    suspend fun exportChatHistoryToUri(uri: Uri, json: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.writer(Charsets.UTF_8)?.use { it.write(json) }
                ?: throw Exception("Failed to open output stream")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importChatHistoryFromUri(uri: Uri, overwrite: Boolean): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { 
                BufferedReader(InputStreamReader(it)).readText() 
            } ?: throw Exception("Failed to read file")
            
            val type = object : TypeToken<ChatHistoryBundle>() {}.type
            val bundle: ChatHistoryBundle = gson.fromJson(json, type)

            if (overwrite) {
                db.conversationDao().clearAll()
                db.messageDao().clearAll()
            }

            val idMap = mutableMapOf<Long, Long>()
            bundle.conversations.forEach { conversation ->
                val oldId = conversation.id
                val newId = db.conversationDao().insert(conversation.copy(id = 0))
                idMap[oldId] = newId
            }

            bundle.messages.forEach { message ->
                val newConversationId = idMap[message.conversationId] ?: message.conversationId
                db.messageDao().insert(message.copy(id = 0, conversationId = newConversationId))
            }

            Result.success(Pair(bundle.conversations.size, bundle.messages.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateSha256Hash(input: String): String = withContext(Dispatchers.Default) {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        bytes.fold("") { str, it -> str + "%02x".format(it) }
    }
}
