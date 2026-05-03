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
            // "plugins" folder might not exist in the "pure" flavor, so we catch exceptions.
            // Wait, in build.gradle.kts we mapped `protocol_plugins` to the root of assets, 
            // so the files are in `assets/` directly or `assets/plugins/`?
            // `assets.srcDir("../../protocol_plugins")` maps the *contents* of protocol_plugins directly into the root of `assets/`.
            // So deepseek_test.json will be at `assets/deepseek_test.json`.
            // We should list all files in `assets/` that end with `.json`.
            val assetManager = context.assets
            val files = assetManager.list("") ?: emptyArray()
            
            for (fileName in files) {
                if (fileName.endsWith(".json")) {
                    try {
                        val inputStream = assetManager.open(fileName)
                        val reader = InputStreamReader(inputStream)
                        val config = gson.fromJson(reader, ProtocolPluginJson::class.java)
                        reader.close()
                        
                        if (config != null && config.providerId.isNotBlank()) {
                            protocols[config.providerId] = { DynamicApiService(config) }
                            pluginConfigs[config.providerId] = config
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
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

    fun getCapabilities(providerName: String): List<String> {
        return pluginConfigs[providerName]?.capabilities ?: emptyList()
    }
}
