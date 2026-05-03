package icather.pages.dev.api.plugin

import android.net.Uri
import icather.pages.dev.api.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AnthropicApiService : ApiService {
    override fun getCompletion(messages: List<ApiService.ApiMessage>, apiKey: String): Flow<ApiService.ApiResponseChunk> = flow {
        // TODO: Implement actual Anthropic network call here
        emit(ApiService.ApiResponseChunk(content = "This is a stub Anthropic response. Please configure network call.", reasoning = null))
    }

    override suspend fun performOcr(imageUri: Uri, apiKey: String): String {
        return "Anthropic OCR not implemented yet."
    }
}
