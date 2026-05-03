package icather.pages.dev.api.plugin

import android.net.Uri
import icather.pages.dev.api.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DynamicApiService(private val config: ProtocolPluginJson) : ApiService {
    override fun getCompletion(messages: List<ApiService.ApiMessage>, apiKey: String, options: Map<String, Any>): Flow<ApiService.ApiResponseChunk> = flow {
        // TODO: Use config.baseUrl, config.authHeaderFormat, etc., to make the network request.
        // For now, this is a stub demonstrating that it loaded correctly.
        val authHeader = config.authHeaderFormat.replace("%s", apiKey)
        val isThinking = options["thinking_mode"] == true
        val thinkingText = if (isThinking) " [THINKING MODE ENABLED]" else ""
        emit(ApiService.ApiResponseChunk(
            content = "This is a dynamic response from ${config.providerId} using base URL: ${config.baseUrl}.$thinkingText Auth header would be: $authHeader",
            reasoning = if (isThinking) "Dynamic plugin is simulating reasoning..." else null
        ))
    }

    override suspend fun performOcr(imageUri: Uri, apiKey: String): String {
        return "OCR not implemented for dynamic plugins yet."
    }
}
