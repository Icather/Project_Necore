package icather.pages.dev.api

/**
 * A data class representing a single chunk of a streamed API response.
 * It's designed to be generic to support different providers.
 */
data class ApiResponseChunk(
    val content: String?,
    val reasoning: String?
)
