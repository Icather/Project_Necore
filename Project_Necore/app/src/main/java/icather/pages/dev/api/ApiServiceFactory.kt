package icather.pages.dev.api

import icather.pages.dev.api.plugin.ProtocolRegistry

object ApiServiceFactory {

    /**
     * Creates an [ApiService] instance based on the provider name.
     * @param provider The name of the API provider (e.g., "OpenAI", "Anthropic").
     * @return An instance of [ApiService].
     * @throws IllegalArgumentException if the provider is not supported.
     */
    fun create(provider: String): ApiService {
        return ProtocolRegistry.createService(provider)
    }
}
