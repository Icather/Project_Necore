package icather.pages.dev.api.plugin

import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
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
        val extraPayload: JsonObject? = null
    )

    // B3: 零开销请求拼接引擎 (Zero-overhead parameter injection)
    private val gson = GsonBuilder()
        .registerTypeAdapter(DynamicApiRequest::class.java, JsonSerializer<DynamicApiRequest> { src, _, context ->
            val jsonObj = JsonObject()
            jsonObj.addProperty("model", src.model)
            jsonObj.add("messages", context.serialize(src.messages))
            jsonObj.addProperty("stream", src.stream)
            src.temperature?.let { jsonObj.addProperty("temperature", it) }
            
            // Inject extra payload directly into root without heavy tree merges
            src.extraPayload?.entrySet()?.forEach { entry ->
                jsonObj.add(entry.key, entry.value)
            }
            jsonObj
        })
        .create()

    private data class StreamResponse(val choices: List<StreamChoice>?, val usage: StreamUsage?)
    private data class StreamChoice(val delta: StreamDelta?)
    private data class StreamDelta(val content: String?, val reasoning_content: String?)
    private data class StreamUsage(val prompt_tokens: Int?, val completion_tokens: Int?, val prompt_cache_hit_tokens: Int?)

    override fun getCompletion(messages: List<ApiService.ApiMessage>, apiKey: String, options: Map<String, Any>): Flow<ApiService.ApiResponseChunk> = flow {
        val providerInfo = config.providerInfo ?: throw IllegalStateException("Provider info is missing")
        val modelName = providerInfo.displayName // Or actual model ID if we add it to schema
        
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
        }

        val requestBody = DynamicApiRequest(
            model = providerInfo.id,
            messages = mappedMessages,
            stream = true,
            temperature = temperature,
            extraPayload = extraPayload
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

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val reader = response.body?.source()?.inputStream()?.bufferedReader() ?: return@flow
            reader.useLines { lines ->
                lines.forEach { line ->
                    // B4: SSE 保活碎片免疫解析器 (SSE Keep-Alive Immunity)
                    if (line.isBlank() || line.startsWith(":")) {
                        // Ignore empty lines and keep-alive comments like ": keep-alive"
                        return@forEach
                    }
                    
                    if (line.startsWith("data:")) {
                        val json = line.substring(5).trim()
                        if (json != "[DONE]") {
                            try {
                                val chunk = gson.fromJson(json, StreamResponse::class.java)
                                val delta = chunk.choices?.firstOrNull()?.delta
                                val usage = chunk.usage
                                
                                // Map custom response field for reasoning if needed
                                val reasoningContent = if (config.featureReasoning?.responseField != null && config.featureReasoning.responseField != "reasoning_content") {
                                    // This requires parsing as JsonObject to get dynamic field, skipping for performance unless needed
                                    delta?.reasoning_content 
                                } else {
                                    delta?.reasoning_content
                                }

                                emit(ApiService.ApiResponseChunk(
                                    content = delta?.content,
                                    reasoning = reasoningContent,
                                    inputTokens = usage?.prompt_tokens,
                                    outputTokens = usage?.completion_tokens,
                                    cacheHitTokens = usage?.prompt_cache_hit_tokens
                                ))
                            } catch (e: Exception) {
                                // Fault tolerance for bad chunks
                                println("Dynamic API parsing error: ${e.message} for chunk: $json")
                            }
                        }
                    }
                }
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
