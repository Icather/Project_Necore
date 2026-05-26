package icather.pages.dev.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import icather.pages.dev.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isImageCompressionEnabled: Boolean = true,
    val isIdentityEnabled: Boolean = true,
    val isMemoryEnabled: Boolean = true,
    val isEmotionEnabled: Boolean = true,
    val isFallbackEnabled: Boolean = false
)

sealed class SettingsEvent {
    data class ShowToast(val message: String) : SettingsEvent()
    data class TriggerExportSettings(val fileName: String) : SettingsEvent()
    data class TriggerExportChatHistory(val fileName: String) : SettingsEvent()
}

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            isImageCompressionEnabled = repository.isImageCompressionEnabled(),
            isIdentityEnabled = repository.isIdentityEnabled(),
            isMemoryEnabled = repository.isMemoryEnabled(),
            isEmotionEnabled = repository.isEmotionEnabled(),
            isFallbackEnabled = repository.isFallbackEnabled()
        )
    }

    fun setImageCompressionEnabled(enabled: Boolean) {
        repository.setImageCompressionEnabled(enabled)
        _uiState.value = _uiState.value.copy(isImageCompressionEnabled = enabled)
    }

    // D3: AI 人设系统
    fun setIdentityEnabled(enabled: Boolean) {
        repository.setIdentityEnabled(enabled)
        _uiState.value = _uiState.value.copy(isIdentityEnabled = enabled)
    }

    // D3: 长期记忆
    fun setMemoryEnabled(enabled: Boolean) {
        repository.setMemoryEnabled(enabled)
        _uiState.value = _uiState.value.copy(isMemoryEnabled = enabled)
    }

    // D4: 情绪感知 — 联动 HeartbeatWorker 调度
    fun setEmotionEnabled(enabled: Boolean) {
        repository.setEmotionEnabled(enabled)
        _uiState.value = _uiState.value.copy(isEmotionEnabled = enabled)
        val context = repository.getContext()
        if (enabled) {
            icather.pages.dev.soul.HeartbeatWorker.schedule(context)
        } else {
            icather.pages.dev.soul.HeartbeatWorker.cancel(context)
        }
    }

    // G2: 模型 Fallback 链
    fun setFallbackEnabled(enabled: Boolean) {
        repository.setFallbackEnabled(enabled)
        _uiState.value = _uiState.value.copy(isFallbackEnabled = enabled)
    }

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    private var jsonToExport: String? = null

    fun prepareExportSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val json = repository.getSettingsBackupJson()
            if (json == null) {
                _events.emit(SettingsEvent.ShowToast("没有找到可导出的设置"))
            } else {
                jsonToExport = json
                val hash = repository.calculateSha256Hash(json).substring(0, 8)
                _events.emit(SettingsEvent.TriggerExportSettings("设置备份_${hash}.json"))
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun executeExportSettings(uri: Uri) {
        val json = jsonToExport ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.exportSettingsToUri(uri, json)
            if (result.isSuccess) {
                _events.emit(SettingsEvent.ShowToast("导出成功"))
            } else {
                _events.emit(SettingsEvent.ShowToast("导出失败: ${result.exceptionOrNull()?.message}"))
            }
            jsonToExport = null
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun importSettings(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.importSettingsFromUri(uri)
            if (result.isSuccess) {
                // 导入成功后刷新 UI 状态以反映还原的开关
                _uiState.value = _uiState.value.copy(
                    isImageCompressionEnabled = repository.isImageCompressionEnabled(),
                    isIdentityEnabled = repository.isIdentityEnabled(),
                    isMemoryEnabled = repository.isMemoryEnabled(),
                    isEmotionEnabled = repository.isEmotionEnabled(),
                    isFallbackEnabled = repository.isFallbackEnabled()
                )
                _events.emit(SettingsEvent.ShowToast("导入成功. 共还原 ${result.getOrNull()} 个 API 配置及所有设置."))
            } else {
                _events.emit(SettingsEvent.ShowToast("导入失败: ${result.exceptionOrNull()?.message}"))
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun prepareExportAllChatHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val json = repository.getAllChatHistoryJson()
            if (json == null) {
                _events.emit(SettingsEvent.ShowToast("没有找到可导出的聊天记录"))
            } else {
                jsonToExport = json
                val hash = repository.calculateSha256Hash(json).substring(0, 8)
                _events.emit(SettingsEvent.TriggerExportChatHistory("聊天记录_${hash}.json"))
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun prepareExportSelectedChatHistory(selectedIds: List<Long>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val json = repository.getSelectedChatHistoryJson(selectedIds)
            if (json == null) {
                _events.emit(SettingsEvent.ShowToast("选择的记录为空或导出失败"))
            } else {
                jsonToExport = json
                val hash = repository.calculateSha256Hash(json).substring(0, 8)
                _events.emit(SettingsEvent.TriggerExportChatHistory("聊天记录_${hash}.json"))
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun executeExportChatHistory(uri: Uri) {
        val json = jsonToExport ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.exportChatHistoryToUri(uri, json)
            if (result.isSuccess) {
                _events.emit(SettingsEvent.ShowToast("导出成功"))
            } else {
                _events.emit(SettingsEvent.ShowToast("导出失败: ${result.exceptionOrNull()?.message}"))
            }
            jsonToExport = null
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun importChatHistory(uri: Uri, overwrite: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.importChatHistoryFromUri(uri, overwrite)
            if (result.isSuccess) {
                val (convs, msgs) = result.getOrNull()!!
                val message = if (convs == 0 && !overwrite) {
                    "所有会话已存在，无需重复导入"
                } else {
                    "导入成功: $convs 个会话, $msgs 条消息"
                }
                _events.emit(SettingsEvent.ShowToast(message))
            } else {
                _events.emit(SettingsEvent.ShowToast("导入失败: ${result.exceptionOrNull()?.message}"))
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
