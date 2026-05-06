package icather.pages.dev.api.plugin

import android.content.Context
import com.google.gson.Gson
import icather.pages.dev.api.ApiService
import icather.pages.dev.api.DeepSeekOcrApiService
import icather.pages.dev.api.SiliconFlowApiService
import java.io.InputStreamReader

object ProtocolRegistry {

    private val protocols = mutableMapOf<String, () -> ApiService>()
    private val pluginConfigs = mutableMapOf<String, ProtocolPluginJson>()
    private val gson = Gson()
    private var isInitialized = false

    init {
        // Register default protocols.
        protocols["OpenAI"] = { OpenAiApiService() }
        protocols["Anthropic"] = { AnthropicApiService() }
    }

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        try {
            val assetManager = context.assets
            val files = assetManager.list("") ?: emptyArray()
            
            for (fileName in files) {
                if (fileName.endsWith(".json")) {
                    try {
                        val inputStream = assetManager.open(fileName)
                        val reader = InputStreamReader(inputStream)
                        val config = gson.fromJson(reader, ProtocolPluginJson::class.java)
                        reader.close()
                        
                        if (config?.providerInfo?.id?.isNotBlank() == true) {
                            protocols[config.providerInfo.id] = { DynamicApiService(config) }
                            pluginConfigs[config.providerInfo.id] = config
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Safe isolation: skip this malformed JSON and continue
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getProtocolNames(): List<String> {
        return protocols.keys.toList()
    }

    fun createService(providerName: String): ApiService {
        val factory = protocols[providerName] 
            ?: throw IllegalArgumentException("Unsupported API provider: $providerName. Please download the protocol plugin.")
        return factory()
    }

    /**
     * Safely retrieves the plugin config. If the config is missing (e.g. JSON deleted),
     * it generates a dummy "Orphan" config to prevent NPE crashes in the UI/ViewModel.
     */
    fun getConfigSafe(providerName: String): ProtocolPluginJson {
        return pluginConfigs[providerName] ?: ProtocolPluginJson(
            providerInfo = ProviderInfo(
                id = providerName,
                displayName = "[协议丢失] $providerName",
                baseUrl = "",
                isOpenAiCompatible = false
            ),
            featureReasoning = FeatureReasoning(supported = false),
            featureCache = FeatureCache(supported = false)
        )
    }

    // Deprecated: Migrating to getConfigSafe
    fun getCapabilities(providerName: String): List<String> {
        return emptyList()
    }
}
