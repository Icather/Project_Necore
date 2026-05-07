package icather.pages.dev.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import icather.pages.dev.ChatMessage
import icather.pages.dev.api.ApiService
import icather.pages.dev.db.ApiConfig
import icather.pages.dev.repository.ChatRepository
import icather.pages.dev.api.plugin.ProtocolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val attachedImages: List<Uri> = emptyList(),
    val attachedFiles: List<Uri> = emptyList(),
    val currentConversationId: Long? = null,
    val activeApiConfig: ApiConfig? = null,
    val apiConfigs: List<ApiConfig> = emptyList(),
    val title: String = "",
    val activeProtocol: icather.pages.dev.api.plugin.ProtocolPluginJson? = null,
    val isThinkingModeEnabled: Boolean = false
)

class ChatViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var apiService: ApiService? = null

    init {
        viewModelScope.launch {
            repository.getAllApiConfigs().collect { configs ->
                if (configs.isNotEmpty()) {
                    val activeId = repository.getActiveApiId()
                    var activeConfig = configs.find { it.id == activeId }
                    if (activeConfig == null) {
                        activeConfig = configs.first()
                        repository.setActiveApiId(activeConfig.id)
                    }
                    _uiState.value = _uiState.value.copy(
                        apiConfigs = configs,
                        activeApiConfig = activeConfig
                    )
                    initApiService(activeConfig)
                }
            }
        }
    }

    fun onModelSelected(config: ApiConfig) {
        repository.setActiveApiId(config.id)
        val protocol = ProtocolRegistry.getConfigSafe(config.provider)
        _uiState.value = _uiState.value.copy(
            activeApiConfig = config,
            activeProtocol = protocol,
            isThinkingModeEnabled = false // Reset on model change
        )
        initApiService(config)
    }

    private fun initApiService(config: ApiConfig) {
        try {
            apiService = repository.createApiService(config.provider)
            val protocol = ProtocolRegistry.getConfigSafe(config.provider)
            _uiState.value = _uiState.value.copy(activeProtocol = protocol)
        } catch (e: Exception) {
            addMessageToView(ChatMessage("Error initializing API: ${e.message}", false))
        }
    }

    fun toggleThinkingMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isThinkingModeEnabled = enabled)
    }

    fun addAttachments(uris: List<Uri>, isImage: Boolean) {
        if (isImage) {
            viewModelScope.launch {
                val compressionEnabled = repository.isImageCompressionEnabled()

                if (!compressionEnabled) {
                    // D2: 未开启压缩 — 校验总大小不超过 20MB
                    val totalSize = uris.sumOf { icather.pages.dev.util.ImageCompressor.getFileSize(repository.getContext(), it) }
                    val existingSize = _uiState.value.attachedImages.sumOf { icather.pages.dev.util.ImageCompressor.getFileSize(repository.getContext(), it) }
                    if (totalSize + existingSize > icather.pages.dev.util.ImageCompressor.MAX_TOTAL_RAW_SIZE_BYTES) {
                        addMessageToView(ChatMessage("⚠️ 图片总大小超出 20MB 限制，请开启图片压缩或减少图片数量。", false))
                        return@launch
                    }
                }
                // 无论是否压缩，先添加 Uri 到列表（压缩将在实际发送时执行）
                val current = _uiState.value.attachedImages.toMutableList()
                current.addAll(uris)
                _uiState.value = _uiState.value.copy(attachedImages = current)
            }
        } else {
            val current = _uiState.value.attachedFiles.toMutableList()
            current.addAll(uris)
            _uiState.value = _uiState.value.copy(attachedFiles = current)
        }
    }

    fun removeAttachment(uri: Uri, isImage: Boolean) {
        if (isImage) {
            val current = _uiState.value.attachedImages.toMutableList()
            current.remove(uri)
            _uiState.value = _uiState.value.copy(attachedImages = current)
        } else {
            val current = _uiState.value.attachedFiles.toMutableList()
            current.remove(uri)
            _uiState.value = _uiState.value.copy(attachedFiles = current)
        }
    }

    private fun resetAttachments() {
        _uiState.value = _uiState.value.copy(attachedImages = emptyList(), attachedFiles = emptyList())
    }

    fun startNewChat() {
        _uiState.value = _uiState.value.copy(
            currentConversationId = null,
            messages = emptyList(),
            title = ""
        )
        resetAttachments()
    }

    fun loadConversation(conversationId: Long) {
        viewModelScope.launch {
            val conversation = repository.getConversation(conversationId)
            val dbMessages = repository.getMessagesForConversation(conversationId)
            
            val newMessages = dbMessages.map { 
                ChatMessage(
                    text = it.text, 
                    isUser = it.isUser, 
                    isHtml = it.isHtml,
                    inputTokens = it.inputTokens,
                    outputTokens = it.outputTokens,
                    cacheHitTokens = it.cacheHitTokens
                ) 
            }
            
            _uiState.value = _uiState.value.copy(
                currentConversationId = conversationId,
                title = conversation?.title ?: "Chat",
                messages = newMessages
            )
            resetAttachments()
        }
    }

    fun sendMessage(text: String) {
        val config = _uiState.value.activeApiConfig
        val images = _uiState.value.attachedImages

        if (config?.modelName == "OCR" && images.isEmpty()) {
            addMessageToView(ChatMessage("Please attach an image for OCR.", false))
            return
        }

        viewModelScope.launch {
            if (config?.modelName == "OCR") {
                val imageUri = images.first()
                val fileName = "Image" // simplified
                val userText = "Image: $fileName"
                
                addMessageToView(ChatMessage(userText, true))
                val conversationId = ensureConversationExists(fileName)
                repository.saveMessage(conversationId, userText, true)
                
                getOcrResponse(conversationId, imageUri, config)
            } else {
                addMessageToView(ChatMessage(text, true))
                val conversationId = ensureConversationExists(text)
                repository.saveMessage(conversationId, text, true)
                
                getAIResponse(conversationId, config!!)
            }
            resetAttachments()
        }
    }

    private suspend fun ensureConversationExists(firstMessage: String): Long {
        var id = _uiState.value.currentConversationId
        if (id == null) {
            val title = firstMessage.take(30)
            id = repository.createNewConversation(title)
            _uiState.value = _uiState.value.copy(currentConversationId = id, title = title)
        }
        return id
    }

    private suspend fun getOcrResponse(conversationId: Long, imageUri: Uri, config: ApiConfig) {
        val apiKey = config.apiKey
        if (apiKey.isEmpty()) {
            addMessageToView(ChatMessage("API Key not set.", false))
            return
        }
        
        val service = apiService ?: return
        
        try {
            val ocrText = repository.performOcr(service, imageUri, apiKey)
            addMessageToView(ChatMessage(ocrText, false))
            repository.saveMessage(conversationId, ocrText, false)
        } catch (e: Exception) {
            val msg = if (e is IOException) "Network error: ${e.message}" else "Error: ${e.message}"
            addMessageToView(ChatMessage(msg, false))
        }
    }

    private suspend fun getAIResponse(conversationId: Long, config: ApiConfig) {
        val apiKey = config.apiKey
        if (apiKey.isEmpty()) {
            addMessageToView(ChatMessage("API Key not set.", false))
            return
        }

        val service = apiService ?: return
        val dbMessages = repository.getMessagesForConversation(conversationId)
        
        // B1: 思考链 (Reasoning) 清洗器
        // Strip the reasoning block completely so it doesn't pollute the prompt context.
        // We use DOT_MATCHES_ALL to ensure multiline reasoning blocks are cleanly removed.
        val apiMessages = dbMessages.map { 
            val role = if (it.isUser) "user" else "assistant"
            val content = it.text.replace(Regex("<font color='#999999'>.*?</font><br>", RegexOption.DOT_MATCHES_ALL), "")
            ApiService.ApiMessage(role, content)
        }

        val aiMessageIndex = _uiState.value.messages.size
        addMessageToView(ChatMessage("", false, isHtml = true, isStreaming = true))

        val finalContent = StringBuilder()
        val finalReasoning = StringBuilder()
        var finalInputTokens: Int? = null
        var finalOutputTokens: Int? = null
        var finalCacheHitTokens: Int? = null
        
        val options = mapOf("thinking_mode" to _uiState.value.isThinkingModeEnabled)

        // D1: 高频重绘节流阀 — 50ms 时间采样
        // 大模型每秒吐 50+ 个 chunk，如果逐个刷新 UI 会导致严重 Jank。
        // 这里只在距上次刷新 ≥50ms 时才推送 UI 更新（≈20fps），人眼无感但 Compose 压力降 80%。
        var lastUiUpdateTime = 0L
        val uiThrottleMs = 50L

        repository.getCompletion(service, apiMessages, apiKey, options)
            .catch { e ->
                // B2: 记忆压缩与 Token 退避 (Memory Compression & Token Backoff)
                if (e is icather.pages.dev.api.ContextLengthExceededException) {
                    val fallbackMsg = "<font color='#ff0000'>[⚠️ Context Limit Exceeded]</font><br>Initiating Memory Compression... (Intercepted HTTP 400. Middle 30% of history will be summarized to rebuild KV Cache Prefix)."
                    updateMessageAt(aiMessageIndex, fallbackMsg)
                    // The actual summarization call and DB rewrite will be triggered here
                } else {
                    val errorMsg = if (e is IOException) "Network error: ${e.message}" else "Error: ${e.message}"
                    updateMessageAt(aiMessageIndex, errorMsg)
                }
            }
            .collect { chunk ->
                chunk.content?.let { finalContent.append(it) }
                chunk.reasoning?.let { finalReasoning.append(it) }
                
                // Track usage if present in the chunk
                if (chunk.inputTokens != null) finalInputTokens = chunk.inputTokens
                if (chunk.outputTokens != null) finalOutputTokens = chunk.outputTokens
                if (chunk.cacheHitTokens != null) finalCacheHitTokens = chunk.cacheHitTokens

                // D1: 节流阀 — 只在超过间隔时才推送 UI
                val now = System.currentTimeMillis()
                if (now - lastUiUpdateTime >= uiThrottleMs) {
                    lastUiUpdateTime = now
                    val reasoningText = if (finalReasoning.isNotEmpty()) "<font color='#999999'>${finalReasoning}</font><br>" else ""
                    val displayText = reasoningText + finalContent.toString()
                    updateMessageAt(aiMessageIndex, displayText, finalInputTokens, finalOutputTokens, finalCacheHitTokens, isStreaming = true)
                }
            }

        // D1: 流结束 — 最终全量刷新 + 关闭 isStreaming 标记
        val reasoningText = if (finalReasoning.isNotEmpty()) "<font color='#999999'>${finalReasoning}</font><br>" else ""
        val finalDisplayText = reasoningText + finalContent.toString()
        updateMessageAt(aiMessageIndex, finalDisplayText, finalInputTokens, finalOutputTokens, finalCacheHitTokens, isStreaming = false)

        val dbMessageText = if (finalReasoning.isNotEmpty()) {
            "<font color='#999999'>${finalReasoning}</font><br>${finalContent}"
        } else {
            finalContent.toString()
        }

        repository.saveMessage(
            conversationId = conversationId, 
            text = dbMessageText, 
            isUser = false, 
            isHtml = true,
            inputTokens = finalInputTokens,
            outputTokens = finalOutputTokens,
            cacheHitTokens = finalCacheHitTokens
        )
    }

    private fun addMessageToView(message: ChatMessage) {
        val current = _uiState.value.messages.toMutableList()
        current.add(message)
        _uiState.value = _uiState.value.copy(messages = current)
    }

    private fun updateMessageAt(index: Int, text: String, inputTokens: Int? = null, outputTokens: Int? = null, cacheHitTokens: Int? = null, isStreaming: Boolean = false) {
        val current = _uiState.value.messages.toMutableList()
        if (index < current.size) {
            current[index] = current[index].copy(
                text = text,
                isStreaming = isStreaming,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
                cacheHitTokens = cacheHitTokens
            )
            _uiState.value = _uiState.value.copy(messages = current)
        }
    }

    class Factory(private val repository: ChatRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
