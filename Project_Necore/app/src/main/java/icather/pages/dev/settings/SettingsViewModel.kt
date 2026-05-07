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
    val isImageCompressionEnabled: Boolean = true
)

sealed class SettingsEvent {
    data class ShowToast(val message: String) : SettingsEvent()
    data class TriggerExportApiConfigs(val fileName: String) : SettingsEvent()
    data class TriggerExportChatHistory(val fileName: String) : SettingsEvent()
}

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            isImageCompressionEnabled = repository.isImageCompressionEnabled()
        )
    }

    fun setImageCompressionEnabled(enabled: Boolean) {
        repository.setImageCompressionEnabled(enabled)
        _uiState.value = _uiState.value.copy(isImageCompressionEnabled = enabled)
    }

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    private var jsonToExport: String? = null

    fun prepareExportApiConfigs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val json = repository.getApiConfigsJson()
            if (json == null) {
                _events.emit(SettingsEvent.ShowToast("没有找到可导出的配置"))
            } else {
                jsonToExport = json
                val hash = repository.calculateSha256Hash(json).substring(0, 8)
                _events.emit(SettingsEvent.TriggerExportApiConfigs("配置API_${hash}.json"))
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun executeExportApiConfigs(uri: Uri) {
        val json = jsonToExport ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.exportApiConfigsToUri(uri, json)
            if (result.isSuccess) {
                _events.emit(SettingsEvent.ShowToast("导出成功"))
            } else {
                _events.emit(SettingsEvent.ShowToast("导出失败: ${result.exceptionOrNull()?.message}"))
            }
            jsonToExport = null
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun importApiConfigs(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.importApiConfigsFromUri(uri)
            if (result.isSuccess) {
                _events.emit(SettingsEvent.ShowToast("导入成功. 共导入 ${result.getOrNull()} 个配置."))
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
                _events.emit(SettingsEvent.ShowToast("导入成功: $convs 个会话, $msgs 条消息"))
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
