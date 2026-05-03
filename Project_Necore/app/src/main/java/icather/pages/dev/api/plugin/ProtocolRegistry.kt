package icather.pages.dev.api.plugin

import icather.pages.dev.api.ApiService
import icather.pages.dev.api.DeepSeekOcrApiService
import icather.pages.dev.api.SiliconFlowApiService

object ProtocolRegistry {

    // For now, we still map the old ones so the project compiles, 
    // but the UI will show OpenAI/Anthropic. We will stub OpenAI/Anthropic below.
    private val protocols = mutableMapOf<String, () -> ApiService>()

    init {
        // Register default protocols. Later these will be dynamically loaded from JSON.
        protocols["OpenAI"] = { OpenAiApiService() }
        protocols["Anthropic"] = { AnthropicApiService() }
        
        // Legacy fallback
        protocols["SiliconFlow"] = { SiliconFlowApiService() }
        protocols["DeepSeek"] = { DeepSeekOcrApiService() }
    }

    fun getProtocolNames(): List<String> {
        return protocols.keys.toList()
    }

    fun createService(providerName: String): ApiService {
        val factory = protocols[providerName] 
            ?: throw IllegalArgumentException("Unsupported API provider: $providerName. Please download the protocol plugin.")
        return factory()
    }
    
    // Future dynamic plugin loader
    fun registerDynamicProtocol(jsonConfig: String) {
        // TODO: Parse jsonConfig and register a DynamicApiService implementation
    }
}
