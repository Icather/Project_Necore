package icather.pages.dev.chat

import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import icather.pages.dev.ChatMessage
import icather.pages.dev.api.ApiService
import icather.pages.dev.db.ApiConfig
import icather.pages.dev.repository.ChatRepository
import icather.pages.dev.api.plugin.ProtocolRegistry
import icather.pages.dev.memory.UserMemoryManager
import icather.pages.dev.soul.EmotionParser
import icather.pages.dev.soul.EmotionState
import icather.pages.dev.util.ImageCompressor
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
    val isThinkingModeEnabled: Boolean = false,
    val currentEmotion: EmotionState = EmotionState.Neutral  // D4: AI 当前情绪
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
                    cacheHitTokens = it.cacheHitTokens,
                    messageId = it.id
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
        // 历史消息永远是纯文本（数据库只存文本）
        val historyMessages = dbMessages.map { 
            val role = if (it.isUser) "user" else "assistant"
            val content = it.text.replace(Regex("<font color='#999999'>.*?</font><br>", RegexOption.DOT_MATCHES_ALL), "")
            // D4: 清除历史消息中的情绪标签，避免污染上下文
            val cleanContent = content.replace(Regex("""\[emotion:\w+]"""), "").trim()
            ApiService.ApiMessage.text(role, cleanContent)
        }.toMutableList()

        // ===== D3: 灵魂组件库 — 上下文编排引擎 =====
        // 参考 SillyTavern 的 Prompt 构建顺序：
        // System Prompt = Identity人设 + User记忆 + 情绪指令 + 聊天历史
        val systemPromptParts = mutableListOf<String>()
        val prefs = repository.getContext().getSharedPreferences("api_prefs", android.content.Context.MODE_PRIVATE)

        // 1. Identity 人设注入
        if (prefs.getBoolean("identity_enabled", true)) {
            val db = icather.pages.dev.db.AppDatabase.getInstance(repository.getContext())
            val activeIdentity = db.identityDao().getActive()
            if (activeIdentity != null && activeIdentity.systemPrompt.isNotBlank()) {
                systemPromptParts.add(activeIdentity.systemPrompt)
            }
        }

        // 2. User.json 长期记忆注入
        if (prefs.getBoolean("memory_enabled", true)) {
            val memoryManager = UserMemoryManager(repository.getContext())
            val memoryText = memoryManager.getFormattedForPrompt()
            if (memoryText.isNotBlank()) {
                systemPromptParts.add("[用户档案（长期记忆）]\n$memoryText")
            }
        }

        // D4: 情绪感知指令注入已移除
        // 用户反馈：每条消息都加情绪标签不自然，不像真人。
        // EmotionState 基础设施保留给 HeartbeatWorker 使用。

        // 将编排好的 System Prompt 插入到消息列表开头
        if (systemPromptParts.isNotEmpty()) {
            val fullSystemPrompt = systemPromptParts.joinToString("\n\n")
            historyMessages.add(0, ApiService.ApiMessage.text("system", fullSystemPrompt))
        }

        // 多模态图片注入：如果有附件图片且模型支持视觉，替换最后一条用户消息为多模态格式
        val attachedImages = _uiState.value.attachedImages
        val supportsVision = _uiState.value.activeProtocol?.featureVision?.supported == true
        if (attachedImages.isNotEmpty() && supportsVision && historyMessages.isNotEmpty()) {
            val lastMsg = historyMessages.last()
            if (lastMsg.role == "user") {
                val lastText = when (val c = lastMsg.content) {
                    is ApiService.MessageContent.Text -> c.text
                    is ApiService.MessageContent.Multimodal -> c.parts
                        .filterIsInstance<ApiService.ContentPart.TextPart>()
                        .joinToString("\n") { it.text }
                }
                // 构建多模态内容：压缩图片 + Base64 编码 + 文本
                val multimodalContent = ApiService.MessageContent.Multimodal(buildList {
                    attachedImages.forEach { uri ->
                        try {
                            val compressed = ImageCompressor.compress(repository.getContext(), uri)
                            val b64 = Base64.encodeToString(compressed, Base64.NO_WRAP)
                            add(ApiService.ContentPart.ImagePart("data:image/jpeg;base64,$b64"))
                        } catch (e: Exception) {
                            println("Image compression failed for $uri: ${e.message}")
                        }
                    }
                    add(ApiService.ContentPart.TextPart(lastText))
                })
                historyMessages[historyMessages.lastIndex] = ApiService.ApiMessage("user", multimodalContent)
            }
        }
        val apiMessages = historyMessages

        val aiMessageIndex = _uiState.value.messages.size
        addMessageToView(ChatMessage("", false, isHtml = true, isStreaming = true))

        val finalContent = StringBuilder()
        val finalReasoning = StringBuilder()
        var finalInputTokens: Int? = null
        var finalOutputTokens: Int? = null
        var finalCacheHitTokens: Int? = null
        
        // D3: Tool Calls — 构建 options（含工具定义）
        val memoryEnabled = prefs.getBoolean("memory_enabled", true)
        val toolsSupported = _uiState.value.activeProtocol?.featureTools?.supported == true
        val toolCallHandler = if (memoryEnabled && toolsSupported) {
            val memoryManager = UserMemoryManager(repository.getContext())
            icather.pages.dev.api.tools.ToolCallHandler(memoryManager)
        } else null
        
        val options = mutableMapOf<String, Any>(
            "thinking_mode" to _uiState.value.isThinkingModeEnabled,
            "model_name" to config.modelName  // 用户配置的模型名称，透传给 DynamicApiService
        )
        if (toolCallHandler != null) {
            options["tools_json"] = toolCallHandler.getToolDefinitions()
        }

        // D1: 高频重绘节流阀 — 50ms 时间采样
        var lastUiUpdateTime = 0L
        val uiThrottleMs = 50L
        
        // D3: Tool Calls 累积器
        val accumulatedToolCalls = StringBuilder()
        var detectedToolCallFinish = false

        // G2: Fallback 状态追踪
        var fallbackTriggered = false
        var errorOccurred = false  // 错误标志：防止 catch 里的错误消息被后续空内容覆盖

        repository.getCompletion(service, apiMessages, apiKey, options)
            .catch { e ->
                // B2: 记忆压缩与 Token 退避 (Memory Compression & Token Backoff)
                if (e is icather.pages.dev.api.ContextLengthExceededException) {
                    val fallbackMsg = "<font color='#ff0000'>[⚠️ Context Limit Exceeded]</font><br>Initiating Memory Compression... (Intercepted HTTP 400. Middle 30% of history will be summarized to rebuild KV Cache Prefix)."
                    updateMessageAt(aiMessageIndex, fallbackMsg)
                    errorOccurred = true
                } else {
                    // G2: 模型自动降级 — 主模型失败时尝试备选
                    val prefs2 = repository.getContext().getSharedPreferences("api_prefs", android.content.Context.MODE_PRIVATE)
                    val fallbackEnabled = prefs2.getBoolean("fallback_enabled", false)
                    if (fallbackEnabled) {
                        fallbackTriggered = true
                        updateMessageAt(aiMessageIndex, "⚡ 主模型失败，正在尝试自动降级...", isStreaming = true)
                    } else {
                        val errorMsg = if (e is IOException) "Network error: ${e.message}" else "Error: ${e.message}"
                        updateMessageAt(aiMessageIndex, errorMsg)
                        errorOccurred = true
                    }
                }
            }
            .collect { chunk ->
                chunk.content?.let { finalContent.append(it) }
                chunk.reasoning?.let { finalReasoning.append(it) }
                chunk.toolCalls?.let { accumulatedToolCalls.append(it) }
                if (chunk.finishReason == "tool_calls") detectedToolCallFinish = true
                
                // Track usage if present in the chunk
                if (chunk.inputTokens != null) finalInputTokens = chunk.inputTokens
                if (chunk.outputTokens != null) finalOutputTokens = chunk.outputTokens
                if (chunk.cacheHitTokens != null) finalCacheHitTokens = chunk.cacheHitTokens

                // D1: 节流阀 — 只在超过间隔时才推送 UI
                // D5 修复: 流式阶段也清理情绪标签，防止闪现
                val now = System.currentTimeMillis()
                if (now - lastUiUpdateTime >= uiThrottleMs) {
                    lastUiUpdateTime = now
                    val reasoningText = if (finalReasoning.isNotEmpty()) "<font color='#999999'>${finalReasoning}</font><br>" else ""
                    val cleanedContent = finalContent.toString().replace(Regex("""\[emotion:\w+]"""), "")
                    val displayText = reasoningText + cleanedContent
                    updateMessageAt(aiMessageIndex, displayText, finalInputTokens, finalOutputTokens, finalCacheHitTokens, isStreaming = true)
                }
            }

        // ===== D3: Tool Calls 二次调用循环 =====
        // 如果模型请求了工具调用，执行工具并将结果回传
        if (detectedToolCallFinish && toolCallHandler != null && accumulatedToolCalls.isNotBlank()) {
            try {
                val toolCallsJson = com.google.gson.JsonParser.parseString(accumulatedToolCalls.toString()).asJsonArray
                
                // 更新 UI 显示工具调用中
                updateMessageAt(aiMessageIndex, "🔧 正在执行工具调用...", isStreaming = true)
                
                // 执行每个工具调用并收集结果
                val toolResults = mutableListOf<ApiService.ApiMessage>()
                // 先把助手的 tool_calls 消息加入（模型要求回传）
                toolResults.add(ApiService.ApiMessage.text("assistant", finalContent.toString()))
                
                for (i in 0 until toolCallsJson.size()) {
                    val tc = toolCallsJson[i].asJsonObject
                    val function = tc.getAsJsonObject("function")
                    val toolCallId = tc.get("id")?.asString ?: "call_$i"
                    val functionName = function.get("name")?.asString ?: ""
                    val arguments = function.get("arguments")?.asString ?: "{}"
                    
                    val result = toolCallHandler.executeToolCall(functionName, arguments)
                    toolResults.add(ApiService.ApiMessage.text("tool", result))
                }
                
                // 用原始消息 + 工具结果发起第二次请求（不带 tools，纯流式）
                val secondRoundMessages = apiMessages.toMutableList()
                secondRoundMessages.addAll(toolResults)
                
                val secondContent = StringBuilder()
                val secondOptions = mapOf<String, Any>("thinking_mode" to _uiState.value.isThinkingModeEnabled)
                
                repository.getCompletion(service, secondRoundMessages, apiKey, secondOptions)
                    .catch { /* 二次调用失败时静默降级，保留第一次的内容 */ }
                    .collect { chunk ->
                        chunk.content?.let { secondContent.append(it) }
                        if (chunk.inputTokens != null) finalInputTokens = chunk.inputTokens
                        if (chunk.outputTokens != null) finalOutputTokens = chunk.outputTokens
                        
                        val now = System.currentTimeMillis()
                        if (now - lastUiUpdateTime >= uiThrottleMs) {
                            lastUiUpdateTime = now
                            val cleanedContent = secondContent.toString().replace(Regex("""\[emotion:\w+]"""), "")
                            updateMessageAt(aiMessageIndex, cleanedContent, finalInputTokens, finalOutputTokens, finalCacheHitTokens, isStreaming = true)
                        }
                    }
                
                // 用第二轮结果覆盖最终显示
                if (secondContent.isNotEmpty()) {
                    finalContent.clear()
                    finalContent.append(secondContent)
                }
            } catch (e: Exception) {
                println("Tool call loop error: ${e.message}")
                // 降级：保留第一次的内容
            }
        }

        // D1: 流结束 — 最终全量刷新 + 关闭 isStreaming 标记
        // 关键守卫：如果 catch 中已经设置了错误消息，不要用空内容覆盖它
        if (!errorOccurred && !fallbackTriggered) {
            val reasoningText = if (finalReasoning.isNotEmpty()) "<font color='#999999'>${finalReasoning}</font><br>" else ""
            var finalDisplayText = reasoningText + finalContent.toString()

            // D4: 情绪解析 — 从回复中提取情绪标签并更新 UI 状态
            val emotionResult = EmotionParser.parse(finalContent.toString())
            _uiState.value = _uiState.value.copy(currentEmotion = emotionResult.emotion)
            // 使用清除了情绪标签的文本显示
            finalDisplayText = reasoningText + emotionResult.cleanText

            updateMessageAt(aiMessageIndex, finalDisplayText, finalInputTokens, finalOutputTokens, finalCacheHitTokens, isStreaming = false)

            val dbMessageText = if (finalReasoning.isNotEmpty()) {
                "<font color='#999999'>${finalReasoning}</font><br>${emotionResult.cleanText}"
            } else {
                emotionResult.cleanText
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

        // ===== G2: 模型 Fallback 链执行 =====
        if (fallbackTriggered) {
            val db = icather.pages.dev.db.AppDatabase.getInstance(repository.getContext())
            val allConfigs = withContext(Dispatchers.IO) { db.apiConfigDao().getAllOnce() }
            val otherConfigs = allConfigs.filter { it.id != config.id && it.apiKey.isNotBlank() }

            if (otherConfigs.isEmpty()) {
                updateMessageAt(aiMessageIndex, "⚠️ 主模型失败，且无可用的备选模型配置。", isStreaming = false)
                return
            }

            // 逐个尝试备选模型
            for (fallbackConfig in otherConfigs) {
                try {
                    updateMessageAt(aiMessageIndex, "⚡ 正在尝试备选模型: ${fallbackConfig.name}...", isStreaming = true)
                    val fallbackService = repository.createApiService(fallbackConfig.provider)
                    val fbContent = StringBuilder()
                    val fbReasoning = StringBuilder()
                    var fbInputTokens: Int? = null
                    var fbOutputTokens: Int? = null
                    var fbCacheHitTokens: Int? = null
                    val fbOptions = mapOf<String, Any>("thinking_mode" to _uiState.value.isThinkingModeEnabled)
                    var fbSuccess = false

                    repository.getCompletion(fallbackService, apiMessages, fallbackConfig.apiKey, fbOptions)
                        .catch { /* 此备选也失败，跳过 */ }
                        .collect { chunk ->
                            fbSuccess = true
                            chunk.content?.let { fbContent.append(it) }
                            chunk.reasoning?.let { fbReasoning.append(it) }
                            if (chunk.inputTokens != null) fbInputTokens = chunk.inputTokens
                            if (chunk.outputTokens != null) fbOutputTokens = chunk.outputTokens
                            if (chunk.cacheHitTokens != null) fbCacheHitTokens = chunk.cacheHitTokens

                            val now = System.currentTimeMillis()
                            if (now - lastUiUpdateTime >= uiThrottleMs) {
                                lastUiUpdateTime = now
                                val fbReasoningText = if (fbReasoning.isNotEmpty()) "<font color='#999999'>${fbReasoning}</font><br>" else ""
                                val fbClean = fbContent.toString().replace(Regex("""\[emotion:\w+]"""), "")
                                updateMessageAt(aiMessageIndex, fbReasoningText + fbClean, fbInputTokens, fbOutputTokens, fbCacheHitTokens, isStreaming = true)
                            }
                        }

                    if (fbSuccess && fbContent.isNotEmpty()) {
                        // Fallback 成功 — 最终显示
                        val fbReasoningText = if (fbReasoning.isNotEmpty()) "<font color='#999999'>${fbReasoning}</font><br>" else ""
                        val fbEmotionResult = EmotionParser.parse(fbContent.toString())
                        val fbFinalText = fbReasoningText + fbEmotionResult.cleanText
                        val fbNote = "<font color='#FF9800'>[⚡ 已自动降级至 ${fallbackConfig.name}]</font><br>"
                        updateMessageAt(aiMessageIndex, fbNote + fbFinalText, fbInputTokens, fbOutputTokens, fbCacheHitTokens, isStreaming = false)

                        // 更新 DB 中的消息（覆盖之前可能保存的空内容）
                        val fbDbText = if (fbReasoning.isNotEmpty()) {
                            "<font color='#999999'>${fbReasoning}</font><br>${fbEmotionResult.cleanText}"
                        } else {
                            fbEmotionResult.cleanText
                        }
                        // 删除旧的空 AI 消息，重新保存
                        repository.deleteLastMessage(conversationId)
                        repository.saveMessage(
                            conversationId = conversationId,
                            text = fbDbText,
                            isUser = false,
                            isHtml = true,
                            inputTokens = fbInputTokens,
                            outputTokens = fbOutputTokens,
                            cacheHitTokens = fbCacheHitTokens
                        )
                        return // 成功降级，退出
                    }
                } catch (_: Exception) {
                    // 此备选也失败，继续尝试下一个
                }
            }
            // 所有备选都失败
            updateMessageAt(aiMessageIndex, "⚠️ 所有模型均不可用。请检查 API 配置。", isStreaming = false)
        }
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

    /**
     * E3: 编辑消息并重新发送
     * 1. 截断 UI 列表到 index 位置
     * 2. DB 中删除该消息及之后的所有消息
     * 3. 用新文本发送
     */
    fun editAndResend(index: Int, newText: String) {
        val config = _uiState.value.activeApiConfig ?: return
        val conversationId = _uiState.value.currentConversationId ?: return
        
        viewModelScope.launch {
            // 获取被编辑消息的 DB 信息用于定位删除点
            val targetMessage = _uiState.value.messages.getOrNull(index) ?: return@launch
            
            // 截断 UI 消息列表（保留 index 之前的）
            val truncated = _uiState.value.messages.take(index).toMutableList()
            _uiState.value = _uiState.value.copy(messages = truncated)
            
            // DB: 删除该消息及之后的所有消息
            if (targetMessage.messageId > 0) {
                // 通过 messageId 获取 timestamp 来定位
                val dbMessages = repository.getMessagesForConversation(conversationId)
                val targetDb = dbMessages.find { it.id == targetMessage.messageId }
                if (targetDb != null) {
                    repository.deleteMessagesFrom(conversationId, targetDb.timestamp)
                }
            }
            
            // 用新文本重新发送
            addMessageToView(ChatMessage(newText, true))
            repository.saveMessage(conversationId, newText, true)
            
            getAIResponse(conversationId, config)
        }
    }

    /**
     * E1: 重新生成最后一条 AI 回复
     * 1. 删除最后一条 AI 消息（UI + DB）
     * 2. 用相同上下文重新请求
     */
    fun regenerateLastResponse() {
        val config = _uiState.value.activeApiConfig ?: return
        val conversationId = _uiState.value.currentConversationId ?: return
        val messages = _uiState.value.messages
        if (messages.isEmpty() || messages.last().isUser) return
        
        viewModelScope.launch {
            // 移除 UI 中最后一条 AI 消息
            val truncated = messages.dropLast(1)
            _uiState.value = _uiState.value.copy(messages = truncated)
            
            // DB: 删除最后一条消息
            repository.deleteLastMessage(conversationId)
            
            // 重新请求
            getAIResponse(conversationId, config)
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

