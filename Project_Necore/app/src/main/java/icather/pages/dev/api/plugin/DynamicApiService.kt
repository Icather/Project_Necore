package icather.pages.dev.api.plugin

import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import icather.pages.dev.api.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class DynamicApiService(private val config: ProtocolPluginJson) : ApiService {

    private val client = OkHttpClient.Builder()
        .readTimeout(120, TimeUnit.SECONDS) // Long timeout for reasoning models
        .build()

    private data class DynamicApiRequest(
        val model: String,
        val messages: List<ApiService.ApiMessage>,
        val stream: Boolean = true,
        val temperature: Double? = null,
        val extraPayload: JsonObject? = null,
        val toolsJson: JsonArray? = null  // D3: Tool Calls 工具定义注入
    )

    // ===== MessageContent 多态序列化器 =====
    // 遵循第零法则：密封类 when 穷举，编译期保证不遗漏。
    private fun serializeMessageContent(content: ApiService.MessageContent): com.google.gson.JsonElement {
        return when (content) {
            is ApiService.MessageContent.Text -> JsonPrimitive(content.text)
            is ApiService.MessageContent.Multimodal -> JsonArray().apply {
                content.parts.forEach { part ->
                    add(when (part) {
                        is ApiService.ContentPart.TextPart -> JsonObject().apply {
                            addProperty("type", "text")
                            addProperty("text", part.text)
                        }
                        is ApiService.ContentPart.ImagePart -> JsonObject().apply {
                            addProperty("type", "image_url")
                            add("image_url", JsonObject().apply {
                                addProperty("url", part.base64DataUrl)
                            })
                        }
                    })
                }
            }
        }
    }

    // B3: 零开销请求拼接引擎 (Zero-overhead parameter injection)
    private val gson = GsonBuilder()
        .registerTypeAdapter(DynamicApiRequest::class.java, JsonSerializer<DynamicApiRequest> { src, _, _ ->
            val jsonObj = JsonObject()
            jsonObj.addProperty("model", src.model)

            // 手动序列化 messages 数组，支持 MessageContent 多态
            val messagesArray = JsonArray()
            src.messages.forEach { msg ->
                val msgObj = JsonObject()
                msgObj.addProperty("role", msg.role)
                msgObj.add("content", serializeMessageContent(msg.content))
                messagesArray.add(msgObj)
            }
            jsonObj.add("messages", messagesArray)

            jsonObj.addProperty("stream", src.stream)
            src.temperature?.let { jsonObj.addProperty("temperature", it) }

            // D3: Tool Calls 工具定义注入
            src.toolsJson?.let { jsonObj.add("tools", it) }
            
            // Inject extra payload directly into root without heavy tree merges
            src.extraPayload?.entrySet()?.forEach { entry ->
                jsonObj.add(entry.key, entry.value)
            }
            jsonObj
        })
        .create()

    private data class StreamResponse(val choices: List<StreamChoice>?, val usage: StreamUsage?)
    private data class StreamChoice(val delta: StreamDelta?, val finish_reason: String?)
    private data class StreamDelta(val content: String?, val reasoning_content: String?, val tool_calls: JsonArray?)
    private data class StreamUsage(val prompt_tokens: Int?, val completion_tokens: Int?, val prompt_cache_hit_tokens: Int?)

    // H3: 当前活跃的 HTTP 请求引用，用于取消
    @Volatile
    private var currentCall: okhttp3.Call? = null

    override fun cancelCurrentRequest() {
        currentCall?.cancel()
        currentCall = null
    }

    override fun getCompletion(messages: List<ApiService.ApiMessage>, apiKey: String, options: Map<String, Any>): Flow<ApiService.ApiResponseChunk> = flow {
        val providerInfo = config.providerInfo ?: throw IllegalStateException("Provider info is missing")
        
        // B3: 角色映射 (Role Mapping)
        val systemRole = config.featureRoles?.systemRoleName ?: "system"
        val mappedMessages = messages.map { msg ->
            val finalRole = if (msg.role == "system") systemRole else msg.role
            ApiService.ApiMessage(finalRole, msg.content)
        }

        // Handle Reasoning Features
        val isThinking = options["thinking_mode"] == true
        var extraPayload: JsonObject? = null
        var temperature: Double? = 0.7

        if (isThinking && config.featureReasoning?.supported == true) {
            extraPayload = config.featureReasoning.triggerPayload
            if (config.featureReasoning.allowsTemperature == false) {
                temperature = null // Must strip temperature
            }
        } else if (!isThinking && config.featureReasoning?.supported == true && config.featureReasoning.disablePayload != null) {
            // 显式关闭思考模式 — DeepSeek V4 等模型默认开启思考，
            // 不发送 disable_payload 会导致非思考模式无法激活
            extraPayload = config.featureReasoning.disablePayload
        }

        // D3: Tool Calls — 从 options 中提取工具定义
        val toolsJson = options["tools_json"] as? JsonArray

        // 模型名称：优先使用用户在 ApiConfig 中配置的 modelName，
        // 否则回退到协议插件的 provider_info.id（兼容旧配置）
        val modelName = (options["model_name"] as? String)?.takeIf { it.isNotBlank() }
            ?: providerInfo.id

        val requestBody = DynamicApiRequest(
            model = modelName,
            messages = mappedMessages,
            stream = true,
            temperature = temperature,
            extraPayload = extraPayload,
            toolsJson = toolsJson
        )

        val requestJson = gson.toJson(requestBody)
        val authHeader = "Bearer $apiKey" // Assuming OpenAI compatible

        val request = Request.Builder()
            .url(providerInfo.baseUrl + "/chat/completions")
            .addHeader("accept", "application/json, text/event-stream")
            .addHeader("authorization", authHeader)
            .addHeader("content-type", "application/json")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        val call = client.newCall(request)
        currentCall = call
        val response = call.execute()

        if (response.isSuccessful) {
            // 流式修复：使用 Okio 原生 BufferedSource 逐行读取 SSE。
            // 避免 Java BufferedReader 的 8KB 预读缓冲导致 SSE 小数据块被积压。
            // Okio 的 readUtf8Line() 在遇到换行符时立即返回，确保逐 chunk 实时推送。
            val source = response.body?.source() ?: return@flow
            try {
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break

                    // B4: SSE 保活碎片免疫解析器 (SSE Keep-Alive Immunity)
                    if (line.isBlank() || line.startsWith(":")) {
                        // Ignore empty lines and keep-alive comments like ": keep-alive"
                        continue
                    }

                    if (line.startsWith("data:")) {
                        val json = line.substring(5).trim()
                        if (json != "[DONE]") {
                            try {
                                // H2 Debug: 打印前3个 SSE chunk 的原始 JSON
                                android.util.Log.d("NecoreSSE", "RAW: ${json.take(300)}")
                                val chunk = gson.fromJson(json, StreamResponse::class.java)
                                val firstChoice = chunk.choices?.firstOrNull()
                                val delta = firstChoice?.delta
                                val usage = chunk.usage

                                // Map custom response field for reasoning if needed
                                val reasoningContent = if (config.featureReasoning?.responseField != null && config.featureReasoning.responseField != "reasoning_content") {
                                    delta?.reasoning_content
                                } else {
                                    delta?.reasoning_content
                                }

                                // D3: Tool Calls 透传
                                val toolCallsStr = delta?.tool_calls?.toString()

                                // H2 Debug: 追踪原始 reasoning 数据
                                if (reasoningContent != null) {
                                    android.util.Log.d("NecoreDebug", "SSE reasoning_content: len=${reasoningContent.length}")
                                }

                                emit(ApiService.ApiResponseChunk(
                                    content = delta?.content,
                                    reasoning = reasoningContent,
                                    inputTokens = usage?.prompt_tokens,
                                    outputTokens = usage?.completion_tokens,
                                    cacheHitTokens = usage?.prompt_cache_hit_tokens,
                                    toolCalls = toolCallsStr,
                                    finishReason = firstChoice?.finish_reason
                                ))
                            } catch (e: Exception) {
                                // Fault tolerance for bad chunks
                                println("Dynamic API parsing error: ${e.message} for chunk: $json")
                            }
                        }
                    }
                }
            } finally {
                source.close()
            }
        } else {
            // Check for B2: Token Backoff signals (HTTP 400 Context Length Exceeded)
            val errorBody = response.body?.string()
            if (response.code == 400 && errorBody?.contains("context_length_exceeded") == true) {
                throw icather.pages.dev.api.ContextLengthExceededException("Context length exceeded: $errorBody")
            }
            throw Exception("API Error: ${response.code} ${response.message}. Body: $errorBody")
        }
    }

    override suspend fun performOcr(imageUri: Uri, apiKey: String): String {
        return "OCR not implemented for dynamic plugins yet."
    }
}
