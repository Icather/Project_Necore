package icather.pages.dev.api

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DeepSeekOcrApiService : ApiService {
    override fun getCompletion(messages: List<ApiService.ApiMessage>, apiKey: String): Flow<ApiService.ApiResponseChunk> {
        // Not supported for this service
        return flow { throw UnsupportedOperationException("Completion is not supported for OCR service.") }
    }

    override suspend fun performOcr(imageUri: Uri, apiKey: String): String {
        // Here you would implement the actual API call to DeepSeek OCR.
        // This would involve creating a multipart request with the image.
        // For now, we'll just return a placeholder string.
        return "OCR result for $imageUri"
    }
}
