package icather.pages.dev.api.plugin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 插件管理器 ViewModel
 *
 * 管理远程插件列表获取、下载状态和本地插件删除。
 * 所有网络操作在 Dispatchers.IO 上执行。
 */
class PluginManagerViewModel(private val downloader: PluginDownloader) : ViewModel() {

    // ===== UI 状态 =====

    /** 单个插件的展示状态 */
    data class PluginItem(
        val fileName: String,
        val displayName: String,
        val modelId: String,
        val status: PluginStatus,
        val isDownloading: Boolean = false
    )

    sealed interface PluginStatus {
        /** APK 内置（不可删除） */
        data object BuiltIn : PluginStatus
        /** 已从网络下载到本地（可删除） */
        data object Downloaded : PluginStatus
        /** 仅存在于远程仓库（可下载） */
        data object Remote : PluginStatus
    }

    data class UiState(
        val plugins: List<PluginItem> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val toastMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** 消费 Toast 消息 */
    fun consumeToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    /** 消费错误消息 */
    fun consumeError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * 刷新：从 GitHub 拉取远程列表，合并本地状态。
     */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val remoteFiles = downloader.fetchRemotePluginList()
                val builtInNames = downloader.getBuiltInPluginFileNames()
                val localNames = downloader.getLocalPluginFileNames()

                val items = remoteFiles.map { entry ->
                    val status = when {
                        builtInNames.contains(entry.name) -> PluginStatus.BuiltIn
                        localNames.contains(entry.name) -> PluginStatus.Downloaded
                        else -> PluginStatus.Remote
                    }

                    // 解析 display_name：尝试从本地已有文件读取，否则从文件名推导
                    val displayName = resolveDisplayName(entry.name, status)
                    val modelId = entry.name.removeSuffix(".json")

                    PluginItem(
                        fileName = entry.name,
                        displayName = displayName,
                        modelId = modelId,
                        status = status
                    )
                }.sortedWith(compareBy<PluginItem> {
                    // 排序：已安装在前，远程在后
                    when (it.status) {
                        is PluginStatus.BuiltIn -> 0
                        is PluginStatus.Downloaded -> 1
                        is PluginStatus.Remote -> 2
                    }
                }.thenBy { it.displayName })

                _uiState.value = _uiState.value.copy(
                    plugins = items,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "获取插件列表失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 下载指定插件。
     */
    fun downloadPlugin(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // 标记该插件为下载中
            _uiState.value = _uiState.value.copy(
                plugins = _uiState.value.plugins.map {
                    if (it.fileName == fileName) it.copy(isDownloading = true) else it
                }
            )

            try {
                val remoteFiles = downloader.fetchRemotePluginList()
                val entry = remoteFiles.find { it.name == fileName }
                    ?: throw Exception("未找到远程文件: $fileName")

                val config = downloader.downloadPlugin(entry)

                // 热重载协议注册表
                ProtocolRegistry.reload()

                val newDisplayName = config.providerInfo?.displayName ?: fileName

                // 更新状态
                _uiState.value = _uiState.value.copy(
                    plugins = _uiState.value.plugins.map {
                        if (it.fileName == fileName) {
                            it.copy(
                                status = PluginStatus.Downloaded,
                                displayName = newDisplayName,
                                isDownloading = false
                            )
                        } else it
                    },
                    toastMessage = "已下载: $newDisplayName"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    plugins = _uiState.value.plugins.map {
                        if (it.fileName == fileName) it.copy(isDownloading = false) else it
                    },
                    toastMessage = "下载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 删除本地已下载的插件（不影响内置插件）。
     */
    fun deletePlugin(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = downloader.deletePlugin(fileName)
            if (success) {
                // 热重载协议注册表
                ProtocolRegistry.reload()

                _uiState.value = _uiState.value.copy(
                    plugins = _uiState.value.plugins.map {
                        if (it.fileName == fileName) {
                            it.copy(status = PluginStatus.Remote)
                        } else it
                    },
                    toastMessage = "已删除: $fileName"
                )
            }
        }
    }

    /**
     * 从文件名或本地配置解析展示名称。
     */
    private fun resolveDisplayName(fileName: String, status: PluginStatus): String {
        // 尝试从 ProtocolRegistry 已加载的配置中获取
        val baseName = fileName.removeSuffix(".json")
        val config = try {
            val registryConfig = ProtocolRegistry.getConfigSafe(baseName)
            if (registryConfig.providerInfo?.displayName?.startsWith("[协议丢失]") == false) {
                registryConfig
            } else null
        } catch (_: Exception) { null }

        return config?.providerInfo?.displayName
            ?: baseName.replace("-", " ").replace("_", " ")
    }

    // ===== ViewModelFactory =====

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PluginManagerViewModel(PluginDownloader(context.applicationContext)) as T
        }
    }
}
