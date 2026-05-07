package icather.pages.dev.api

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * A generic interface for different API providers.
 */
interface ApiService {

    // ===== 消息内容的密封类型系统 (Sealed Message Content Types) =====
    // 遵循第零法则：使用密封接口保证编译期类型安全，禁止使用 Any。
    // 纯文本模型序列化为 JSON string，多模态模型序列化为 JSON array。

    /** 消息内容：纯文本或多模态 */
    sealed interface MessageContent {
        /** 纯文本消息 — 兼容所有模型 */
        data class Text(val text: String) : MessageContent
        /** 多模态消息 — 图片+文本混合 */
        data class Multimodal(val parts: List<ContentPart>) : MessageContent
    }

    /** 多模态消息中的单个内容块 */
    sealed interface ContentPart {
        data class TextPart(val text: String) : ContentPart
        data class ImagePart(val base64DataUrl: String) : ContentPart
        // 未来扩展：AudioPart, VideoPart...
    }

    /**
     * A data class to represent a message in the conversation history for the API request.
     */
    data class ApiMessage(val role: String, val content: MessageContent) {
        companion object {
            /** 便捷工厂方法：创建纯文本消息 */
            fun text(role: String, text: String) = ApiMessage(role, MessageContent.Text(text))
        }
    }

    /**
     * A simple data class to represent a chunk of the API response.
     */
    data class ApiResponseChunk(
        val content: String?, 
        val reasoning: String?,
        val inputTokens: Int? = null,
        val outputTokens: Int? = null,
        val cacheHitTokens: Int? = null
    )

    /**
     * Sends a list of messages to the API and returns a flow of response chunks.
     * @param messages The list of messages to send, in the format required by the API.
     * @param apiKey The API key to use for the request.
     * @param options A map of dynamic options to toggle model-specific features (e.g., "thinking_mode").
     * @return A Flow that emits [ApiResponseChunk]s.
     */
    fun getCompletion(messages: List<ApiMessage>, apiKey: String, options: Map<String, Any> = emptyMap()): Flow<ApiResponseChunk>

    /**
     * Performs OCR on an image and returns the recognized text.
     * @param imageUri The URI of the image to perform OCR on.
     * @param apiKey The API key to use for the request.
     * @return The recognized text.
     */
    suspend fun performOcr(imageUri: Uri, apiKey: String): String
}
