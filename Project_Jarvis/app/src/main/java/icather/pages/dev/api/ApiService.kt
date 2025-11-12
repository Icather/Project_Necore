package icather.pages.dev.api

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * A generic interface for different API providers.
 */
interface ApiService {

    /**
     * A simple data class to represent a message in the conversation history for the API request.
     */
    data class ApiMessage(val role: String, val content: String)

    /**
     * A simple data class to represent a chunk of the API response.
     */
    data class ApiResponseChunk(val content: String?, val reasoning: String?)

    /**
     * Sends a list of messages to the API and returns a flow of response chunks.
     * @param messages The list of messages to send, in the format required by the API.
     * @param apiKey The API key to use for the request.
     * @return A Flow that emits [ApiResponseChunk]s.
     */
    fun getCompletion(messages: List<ApiMessage>, apiKey: String): Flow<ApiResponseChunk>

    /**
     * Performs OCR on an image and returns the recognized text.
     * @param imageUri The URI of the image to perform OCR on.
     * @param apiKey The API key to use for the request.
     * @return The recognized text.
     */
    suspend fun performOcr(imageUri: Uri, apiKey: String): String
}
