package icather.pages.dev.api

object ApiServiceFactory {

    /**
     * Creates an [ApiService] instance based on the provider name.
     * @param provider The name of the API provider (e.g., "SiliconFlow", "DeepSeek").
     * @return An instance of [ApiService].
     * @throws IllegalArgumentException if the provider is not supported.
     */
    fun create(provider: String): ApiService {
        return when (provider) {
            "SiliconFlow" -> SiliconFlowApiService()
            "DeepSeek" -> DeepSeekOcrApiService()
            else -> throw IllegalArgumentException("Unsupported API provider: $provider")
        }
    }
}
