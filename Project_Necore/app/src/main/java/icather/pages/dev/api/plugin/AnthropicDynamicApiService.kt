package icather.pages.dev.api.plugin

import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import icather.pages.dev.api.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Anthropic Messages API (/v1/messages) 的完整适配实现。
 *
 * 与 DynamicApiService (走 OpenAI /chat/completions) 完全隔离，
 * 遵循第零法则：不同协议使用独立类型，编译期保证不混淆。
 *
 * Anthropic SSE 事件流格式：
 *   event: content_block_delta
 *   data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hello"}}
 *
 * 扩展思考(Extended Thinking)时会收到 thinking_delta 事件。
 */
class AnthropicDynamicApiService(private val config: ProtocolPluginJson) : ApiService {

    private val client = OkHttpClient.Builder()
        .readTimeout(180, TimeUnit.SECONDS) // Anthropic 扩展思考可能需要更长时间
        .build()

    // H3: 当前活跃的 HTTP 请求引用
    @Volatile
    private var currentCall: okhttp3.Call? = null

    override fun cancelCurrentRequest() {
        currentCall?.cancel()
        currentCall = null
    }

    private val gson = GsonBuilder().create()

    // ===== Anthropic Messages API 请求体构建 =====

    /**
     * 构建 Anthropic /v1/messages 的请求 JSON。
     * 与 OpenAI 格式的关键区别：
     * 1. system 消息不在 messages 数组中，而是 top-level 的 "system" 字段
     * 2. content 使用 Anthropic 的 content block 格式 (type: text / image)
     * 3. 扩展思考通过 top-level "thinking" 配置开启
     */
    private fun buildRequestJson(
        messages: List<ApiService.ApiMessage>,
        isThinking: Boolean,
        modelName: String
    ): String {
        val jsonObj = JsonObject()
        val providerInfo = config.providerInfo ?: throw IllegalStateException("Provider info is missing")

        // 设置模型：优先使用用户配置的 modelName
        jsonObj.addProperty("model", modelName)

        // 提取 system 消息（Anthropic 要求 system 在 top-level，不在 messages 数组内）
        val systemMessages = messages.filter { it.role == "system" }
        val nonSystemMessages = messages.filter { it.role != "system" }

        if (systemMessages.isNotEmpty()) {
            // 合并所有 system 消息为一个字符串
            val systemText = systemMessages.joinToString("\n") { msg ->
                when (val content = msg.content) {
                    is ApiService.MessageContent.Text -> content.text
                    is ApiService.MessageContent.Multimodal -> content.parts.filterIsInstance<ApiService.ContentPart.TextPart>().joinToString("\n") { it.text }
                }
            }
            jsonObj.addProperty("system", systemText)
        }

        // 构建 messages 数组（Anthropic 格式）
        val messagesArray = JsonArray()
        for (msg in nonSystemMessages) {
            val msgObj = JsonObject()
            msgObj.addProperty("role", msg.role)

            when (val content = msg.content) {
                is ApiService.MessageContent.Text -> {
                    // Anthropic 支持直接字符串 content
                    msgObj.addProperty("content", content.text)
                }
                is ApiService.MessageContent.Multimodal -> {
                    // Anthropic 多模态使用 content array
                    val contentArray = JsonArray()
                    for (part in content.parts) {
                        when (part) {
                            is ApiService.ContentPart.TextPart -> {
                                contentArray.add(JsonObject().apply {
                                    addProperty("type", "text")
                                    addProperty("text", part.text)
                                })
                            }
                            is ApiService.ContentPart.ImagePart -> {
                                // Anthropic 图片格式：从 data:image/xxx;base64,yyy 中解析
                                val dataUrl = part.base64DataUrl
                                val mediaType = dataUrl.substringAfter("data:").substringBefore(";base64,")
                                val base64Data = dataUrl.substringAfter(";base64,")
                                contentArray.add(JsonObject().apply {
                                    addProperty("type", "image")
                                    add("source", JsonObject().apply {
                                        addProperty("type", "base64")
                                        addProperty("media_type", mediaType)
                                        addProperty("data", base64Data)
                                    })
                                })
                            }
                        }
                    }
                    msgObj.add("content", contentArray)
                }
            }
            messagesArray.add(msgObj)
        }
        jsonObj.add("messages", messagesArray)

        // max_tokens 是 Anthropic 的必填项
        val maxTokens = config.constraints?.maxOutputTokens ?: 8192
        jsonObj.addProperty("max_tokens", maxTokens)

        // 流式
        jsonObj.addProperty("stream", true)

        // 扩展思考配置
        if (isThinking && config.featureReasoning?.supported == true) {
            val thinkingPayload = config.featureReasoning.triggerPayload
            if (thinkingPayload != null) {
                // 将 trigger_payload 中的字段注入到请求根部
                thinkingPayload.entrySet().forEach { entry ->
                    jsonObj.add(entry.key, entry.value)
                }
            }
        }

        return gson.toJson(jsonObj)
    }

    // ===== Anthropic SSE 流解析 =====

    override fun getCompletion(
        messages: List<ApiService.ApiMessage>,
        apiKey: String,
        options: Map<String, Any>
    ): Flow<ApiService.ApiResponseChunk> = flow {
        val providerInfo = config.providerInfo ?: throw IllegalStateException("Provider info is missing")
        val isThinking = options["thinking_mode"] == true
        val modelName = (options["model_name"] as? String)?.takeIf { it.isNotBlank() }
            ?: providerInfo.id

        val requestJson = buildRequestJson(messages, isThinking, modelName)

        // Anthropic 使用 x-api-key 鉴权（不是 Bearer token）
        // API 版本通过 anthropic-version 头指定
        val request = Request.Builder()
            .url(providerInfo.baseUrl + "/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .addHeader("accept", "text/event-stream")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        val call = client.newCall(request)
        currentCall = call
        val response = call.execute()

        if (response.isSuccessful) {
            // 流式修复：使用 Okio 原生 BufferedSource 逐行读取 SSE。
            // 避免 Java BufferedReader 的 8KB 预读缓冲导致 SSE 小数据块被积压。
            val source = response.body?.source() ?: return@flow

            // Anthropic SSE 使用 "event: xxx\ndata: {json}\n" 的双行格式
            var currentEventType = ""

            try {
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break

                    when {
                        line.isBlank() -> {
                            // SSE 事件分隔符，重置事件类型
                            currentEventType = ""
                        }
                        line.startsWith("event:") -> {
                            currentEventType = line.substring(6).trim()
                        }
                        line.startsWith("data:") -> {
                            val json = line.substring(5).trim()
                            if (json.isBlank()) continue

                            try {
                                val data = JsonParser.parseString(json).asJsonObject

                                when (currentEventType) {
                                    "content_block_delta" -> {
                                        val delta = data.getAsJsonObject("delta") ?: continue
                                        val deltaType = delta.get("type")?.asString ?: continue

                                        when (deltaType) {
                                            "text_delta" -> {
                                                val text = delta.get("text")?.asString
                                                emit(ApiService.ApiResponseChunk(
                                                    content = text,
                                                    reasoning = null
                                                ))
                                            }
                                            "thinking_delta" -> {
                                                val thinking = delta.get("thinking")?.asString
                                                emit(ApiService.ApiResponseChunk(
                                                    content = null,
                                                    reasoning = thinking
                                                ))
                                            }
                                            // signature_delta, input_json_delta 等暂时忽略
                                        }
                                    }
                                    "message_start" -> {
                                        // 提取 input_tokens
                                        val message = data.getAsJsonObject("message")
                                        val usage = message?.getAsJsonObject("usage")
                                        val inputTokens = usage?.get("input_tokens")?.asInt
                                        if (inputTokens != null) {
                                            emit(ApiService.ApiResponseChunk(
                                                content = null,
                                                reasoning = null,
                                                inputTokens = inputTokens
                                            ))
                                        }
                                    }
                                    "message_delta" -> {
                                        // 提取 output_tokens (累计值)
                                        val usage = data.getAsJsonObject("usage")
                                        val outputTokens = usage?.get("output_tokens")?.asInt
                                        if (outputTokens != null) {
                                            emit(ApiService.ApiResponseChunk(
                                                content = null,
                                                reasoning = null,
                                                outputTokens = outputTokens
                                            ))
                                        }
                                    }
                                    "ping", "content_block_start", "content_block_stop", "message_stop" -> {
                                        // 安全忽略
                                    }
                                    "error" -> {
                                        val error = data.getAsJsonObject("error")
                                        val errorMsg = error?.get("message")?.asString ?: "Unknown Anthropic error"
                                        throw Exception("Anthropic API Error: $errorMsg")
                                    }
                                }
                            } catch (e: com.google.gson.JsonSyntaxException) {
                                println("Anthropic SSE parsing error: ${e.message} for chunk: $json")
                            }
                        }
                        // 忽略以 : 开头的 SSE 注释
                    }
                }
            } finally {
                source.close()
            }
        } else {
            val errorBody = response.body?.string()
            throw Exception("Anthropic API Error: ${response.code} ${response.message}. Body: $errorBody")
        }
    }

    override suspend fun performOcr(imageUri: Uri, apiKey: String): String {
        return "OCR not implemented for Anthropic plugins yet."
    }
}
