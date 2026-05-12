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
        // Register default protocols (legacy hardcoded entries).
        protocols["OpenAI"] = { OpenAiApiService() }
        // Anthropic 的硬编码空壳已移除，改由 JSON 插件 + AnthropicDynamicApiService 驱动
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
                            // 第零法则路由：根据协议兼容性分发到对应的 ApiService 实现
                            // 编译期保证 OpenAI 兼容协议和 Anthropic 协议走不同的类型通道
                            if (config.providerInfo.isOpenAiCompatible) {
                                protocols[config.providerInfo.id] = { DynamicApiService(config) }
                            } else {
                                // 非 OpenAI 兼容协议，目前仅支持 Anthropic
                                protocols[config.providerInfo.id] = { AnthropicDynamicApiService(config) }
                            }
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

    /**
     * 获取指定提供商的预设模型列表。
     * 合并所有使用同一 base_url 的插件中的 available_models + 各插件自身的 id。
     */
    fun getAvailableModels(providerName: String): List<String> {
        val config = pluginConfigs[providerName] ?: return emptyList()
        val baseUrl = config.providerInfo?.baseUrl ?: return emptyList()

        // 收集所有同 base_url 提供商的模型
        val models = mutableListOf<String>()
        pluginConfigs.values.forEach { plugin ->
            if (plugin.providerInfo?.baseUrl == baseUrl) {
                // 加入插件自身的 id（即默认模型名）
                plugin.providerInfo.id.let { models.add(it) }
                // 加入 available_models 列表
                plugin.providerInfo.availableModels?.let { models.addAll(it) }
            }
        }
        return models.distinct().sorted()
    }
}
