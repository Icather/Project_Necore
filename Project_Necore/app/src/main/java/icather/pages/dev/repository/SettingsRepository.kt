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

    fun getContext(): Context = context

    companion object {
        const val DEFAULT_API_ID = 1L
        private const val KEY_IMAGE_COMPRESSION = "image_compression_enabled"
        private const val KEY_IDENTITY_ENABLED = "identity_enabled"
        private const val KEY_MEMORY_ENABLED = "memory_enabled"
        private const val KEY_EMOTION_ENABLED = "emotion_enabled"
        private const val KEY_FALLBACK_ENABLED = "fallback_enabled"
    }

    fun getAllApiConfigs() = db.apiConfigDao().getAll()

    fun isImageCompressionEnabled(): Boolean {
        return prefs.getBoolean(KEY_IMAGE_COMPRESSION, true)
    }

    fun setImageCompressionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IMAGE_COMPRESSION, enabled).apply()
    }

    // D3: AI 人设系统开关（默认开启）
    fun isIdentityEnabled(): Boolean = prefs.getBoolean(KEY_IDENTITY_ENABLED, true)
    fun setIdentityEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_IDENTITY_ENABLED, enabled).apply() }

    // D3: 长期记忆开关（默认开启）
    fun isMemoryEnabled(): Boolean = prefs.getBoolean(KEY_MEMORY_ENABLED, true)
    fun setMemoryEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_MEMORY_ENABLED, enabled).apply() }

    // D4: 情绪感知开关（默认开启）
    fun isEmotionEnabled(): Boolean = prefs.getBoolean(KEY_EMOTION_ENABLED, true)
    fun setEmotionEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_EMOTION_ENABLED, enabled).apply() }

    // G2: 模型 Fallback 链开关（默认关闭）
    fun isFallbackEnabled(): Boolean = prefs.getBoolean(KEY_FALLBACK_ENABLED, false)
    fun setFallbackEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_FALLBACK_ENABLED, enabled).apply() }

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
            importApiConfigsFromJson(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 从 JSON 字符串导入 API 配置（局域网同步入口） */
    suspend fun importApiConfigsFromJson(json: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
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
            importChatHistoryFromJson(json, overwrite)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 从 JSON 字符串导入聊天记录（局域网同步入口） */
    suspend fun importChatHistoryFromJson(json: String, overwrite: Boolean): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val type = object : TypeToken<ChatHistoryBundle>() {}.type
            val bundle: ChatHistoryBundle = gson.fromJson(json, type)

            if (overwrite) {
                db.conversationDao().clearAll()
                db.messageDao().clearAll()
            }

            val idMap = mutableMapOf<Long, Long>()
            var importedConvCount = 0
            var skippedConvCount = 0

            bundle.conversations.forEach { conversation ->
                val oldId = conversation.id

                if (!overwrite) {
                    // 增量导入时，按 title + startTime 检测是否已存在
                    val existing = db.conversationDao().findByTitleAndStartTime(
                        conversation.title, conversation.startTime
                    )
                    if (existing != null) {
                        // 已存在，跳过此会话（不记录 idMap，其消息也会被自动跳过）
                        skippedConvCount++
                        return@forEach
                    }
                }

                val newId = db.conversationDao().insert(conversation.copy(id = 0))
                idMap[oldId] = newId
                importedConvCount++
            }

            var importedMsgCount = 0
            bundle.messages.forEach { message ->
                val newConversationId = idMap[message.conversationId]
                if (newConversationId != null) {
                    // 仅导入有对应新会话的消息（跳过的会话的消息自动忽略）
                    db.messageDao().insert(message.copy(id = 0, conversationId = newConversationId))
                    importedMsgCount++
                }
            }

            Result.success(Pair(importedConvCount, importedMsgCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取同步数据统计（供 SyncManifest 使用） */
    suspend fun getSyncDataCounts(): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        val convCount = db.conversationDao().getAllConversations().size
        val msgCount = db.messageDao().getAllMessages().size
        val apiCount = db.apiConfigDao().getAllOnce().size
        Triple(convCount, msgCount, apiCount)
    }

    suspend fun calculateSha256Hash(input: String): String = withContext(Dispatchers.Default) {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        bytes.fold("") { str, it -> str + "%02x".format(it) }
    }
}
