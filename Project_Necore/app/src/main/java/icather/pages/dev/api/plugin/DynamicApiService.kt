package icather.pages.dev.api.plugin

import android.net.Uri
import icather.pages.dev.api.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DynamicApiService(private val config: ProtocolPluginJson) : ApiService {
    override fun getCompletion(messages: List<ApiService.ApiMessage>, apiKey: String): Flow<ApiService.ApiResponseChunk> = flow {
        // TODO: Use config.baseUrl, config.authHeaderFormat, etc., to make the network request.
        // For now, this is a stub demonstrating that it loaded correctly.
        val authHeader = config.authHeaderFormat.replace("%s", apiKey)
        emit(ApiService.ApiResponseChunk(
            content = "This is a dynamic response from ${config.providerId} using base URL: ${config.baseUrl}. Auth header would be: $authHeader",
            reasoning = null
        ))
    }

    override suspend fun performOcr(imageUri: Uri, apiKey: String): String {
        return "OCR not implemented for dynamic plugins yet."
    }
}
