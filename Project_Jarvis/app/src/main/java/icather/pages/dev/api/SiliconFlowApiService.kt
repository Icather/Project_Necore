package icather.pages.dev.api

import android.net.Uri
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class SiliconFlowApiService : ApiService {

    private val client = OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build()
    private val gson = Gson()

    // Data classes specific to SiliconFlow's API
    private data class SiliconFlowApiRequest(val model: String, val messages: List<ApiService.ApiMessage>, val stream: Boolean)
    private data class SiliconFlowApiStreamResponse(val choices: List<SiliconFlowApiStreamChoice>)
    private data class SiliconFlowApiStreamChoice(val delta: SiliconFlowStreamDelta)
    private data class SiliconFlowStreamDelta(val content: String?, val reasoning_content: String?)

    override fun getCompletion(messages: List<ApiService.ApiMessage>, apiKey: String): Flow<ApiService.ApiResponseChunk> = flow {
        val requestBody = SiliconFlowApiRequest("deepseek-ai/DeepSeek-R1-0528-Qwen3-8B", messages, true)
        val requestJson = gson.toJson(requestBody)

        val request = Request.Builder()
            .url("https://api.siliconflow.cn/v1/chat/completions")
            .addHeader("accept", "application/json, text/event-stream")
            .addHeader("authorization", "Bearer $apiKey")
            .addHeader("content-type", "application/json")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val reader = response.body?.source()?.inputStream()?.bufferedReader() ?: return@flow
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.startsWith("data:")) {
                        val json = line.substring(5).trim()
                        if (json != "[DONE]") {
                            try {
                                val chunk = gson.fromJson(json, SiliconFlowApiStreamResponse::class.java)
                                val delta = chunk.choices.firstOrNull()?.delta
                                emit(ApiService.ApiResponseChunk(content = delta?.content, reasoning = delta?.reasoning_content))
                            } catch (e: Exception) {
                                println("Error parsing stream chunk: $json, error: ${e.message}")
                            }
                        }
                    }
                }
            }
        } else {
            val errorBody = response.body?.string()
            throw Exception("API Error: ${response.code} ${response.message}. Body: $errorBody")
        }
    }

    override suspend fun performOcr(imageUri: Uri, apiKey: String): String {
        throw UnsupportedOperationException("OCR is not supported by SiliconFlowApiService")
    }
}
