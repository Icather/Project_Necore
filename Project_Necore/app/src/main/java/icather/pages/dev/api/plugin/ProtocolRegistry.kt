package icather.pages.dev.api.plugin

import android.content.Context
import com.google.gson.Gson
import icather.pages.dev.api.ApiService
import icather.pages.dev.api.DeepSeekOcrApiService
import icather.pages.dev.api.SiliconFlowApiService
import java.io.File
import java.io.InputStreamReader

object ProtocolRegistry {

    private val protocols = mutableMapOf<String, () -> ApiService>()
    private val pluginConfigs = mutableMapOf<String, ProtocolPluginJson>()
    private val gson = Gson()
    private var isInitialized = false
    private var appContext: Context? = null

    init {
        // Register default protocols (legacy hardcoded entries).
        protocols["OpenAI"] = { OpenAiApiService() }
        // Anthropic 的硬编码空壳已移除，改由 JSON 插件 + AnthropicDynamicApiService 驱动
    }

    fun init(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        isInitialized = true
        loadAllPlugins(context)
    }

    /**
     * 热重载：下载/删除插件后调用，重新扫描所有来源。
     * 清空已有注册表后重新加载 assets + filesDir/plugins/。
     */
    fun reload() {
        val ctx = appContext ?: return
        protocols.clear()
        pluginConfigs.clear()
        // 重新注册硬编码默认协议
        protocols["OpenAI"] = { OpenAiApiService() }
        loadAllPlugins(ctx)
    }

    private fun loadAllPlugins(context: Context) {
        // 第一步：加载 APK 内置 assets 中的插件
        loadFromAssets(context)
        // 第二步：加载 filesDir/plugins/ 中的外部下载插件（可覆盖内置同名插件）
        loadFromExternalDir(context)
    }

    private fun loadFromAssets(context: Context) {
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
                        registerPlugin(config)
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

    private fun loadFromExternalDir(context: Context) {
        try {
            val pluginsDir = File(context.filesDir, "plugins")
            if (!pluginsDir.exists()) return

            val files = pluginsDir.listFiles() ?: return
            for (file in files) {
                if (file.extension == "json") {
                    try {
                        val config = gson.fromJson(file.readText(Charsets.UTF_8), ProtocolPluginJson::class.java)
                        registerPlugin(config)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerPlugin(config: ProtocolPluginJson?) {
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

    // ===== 提供商分组 API（UI 层使用） =====

    /**
     * 提供商分组：一个 base_url 代表一个提供商。
     * @param baseUrl 该提供商的 API 地址
     * @param displayName 用于 UI 展示的提供商名称
     * @param pluginIds 该组下所有插件的 id 列表（任选一个作为 DB 中 provider 字段的值）
     * @param availableModels 该提供商下所有可用的模型名称（去重）
     */
    data class ProviderGroup(
        val baseUrl: String,
        val displayName: String,
        val pluginIds: List<String>,
        val availableModels: List<String>
    )

    /**
     * 按 base_url 聚合所有插件，返回去重的提供商分组列表。
     * 每组使用该组中第一个插件的 display_name 作为提供商展示名。
     */
    fun getProviderGroups(): List<ProviderGroup> {
        // 按 base_url 分组
        val grouped = pluginConfigs.values
            .filter { it.providerInfo?.baseUrl?.isNotBlank() == true }
            .groupBy { it.providerInfo!!.baseUrl }

        return grouped.map { (baseUrl, plugins) ->
            // 提供商展示名：域名映射优先（最可靠），括号提取回退
            val providerDisplayName = deriveProviderName(baseUrl, plugins.first())

            // 收集所有模型
            val models = mutableListOf<String>()
            plugins.forEach { plugin ->
                plugin.providerInfo?.id?.let { models.add(it) }
                plugin.providerInfo?.availableModels?.let { models.addAll(it) }
            }

            ProviderGroup(
                baseUrl = baseUrl,
                displayName = providerDisplayName,
                pluginIds = plugins.mapNotNull { it.providerInfo?.id },
                availableModels = models.distinct().sorted()
            )
        }.sortedBy { it.displayName }
    }

    /**
     * 根据 base_url 和模型名称，反查到对应的插件 id。
     * 优先匹配 id 与 modelName 完全一致的插件，否则返回该组中的第一个插件 id。
     * 这保证了 ApiConfig.provider 始终是有效的 ProtocolRegistry 键。
     */
    fun findPluginIdForProvider(baseUrl: String, modelName: String): String? {
        val matchingPlugins = pluginConfigs.values.filter { it.providerInfo?.baseUrl == baseUrl }
        if (matchingPlugins.isEmpty()) return null

        // 优先精确匹配：如果有插件的 id 就是用户选择的模型名
        val exactMatch = matchingPlugins.find { it.providerInfo?.id == modelName }
        if (exactMatch != null) return exactMatch.providerInfo?.id

        // 否则返回该提供商组中第一个插件的 id（作为通用网关）
        return matchingPlugins.first().providerInfo?.id
    }

    /**
     * 从 base_url 域名推导提供商展示名称。
     * 域名映射最可靠，不依赖 display_name 中括号内容的一致性。
     */
    private fun deriveProviderName(baseUrl: String, fallbackPlugin: ProtocolPluginJson): String {
        // 域名 → 提供商名称映射（覆盖所有当前已适配提供商）
        val domainMap = mapOf(
            "deepseek.com" to "DeepSeek",
            "openai.com" to "OpenAI",
            "anthropic.com" to "Anthropic",
            "dashscope.aliyuncs.com" to "阿里云百炼",
            "siliconflow.cn" to "硅基流动",
            "volces.com" to "火山引擎",
            "bce.baidu.com" to "百度千帆",
            "googleapis.com" to "Google"
        )

        // 优先域名匹配
        domainMap.forEach { (domain, name) ->
            if (baseUrl.contains(domain)) return name
        }

        // 回退：从 display_name 的括号中提取
        val rawName = fallbackPlugin.providerInfo?.displayName ?: return baseUrl
        return Regex("\\((.+?)\\)").find(rawName)?.groupValues?.get(1)
            ?: rawName
    }
}

