package icather.pages.dev.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import icather.pages.dev.db.ApiConfig
import icather.pages.dev.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ApiConfigUiState(
    val configs: List<ApiConfig> = emptyList(),
    val activeConfigId: Long = -1L,
    val showAddDialog: Boolean = false,
    val configToEdit: ApiConfig? = null
)

class ApiConfigViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiConfigUiState())
    val uiState: StateFlow<ApiConfigUiState> = _uiState.asStateFlow()

    init {
        loadConfigs()
    }

    private fun loadConfigs() {
        viewModelScope.launch {
            repository.getAllApiConfigs().collect { configs ->
                _uiState.update { it.copy(configs = configs) }
            }
        }
        viewModelScope.launch {
            repository.activeApiConfigId.collect { activeId ->
                _uiState.update { it.copy(activeConfigId = activeId) }
            }
        }
    }

    fun setActiveConfig(id: Long) {
        viewModelScope.launch {
            repository.setActiveApiConfigId(id)
        }
    }

    fun deleteConfig(config: ApiConfig) {
        viewModelScope.launch {
            repository.deleteApiConfig(config)
        }
    }

    fun setShowAddDialog(show: Boolean) {
        _uiState.update { it.copy(showAddDialog = show) }
        if (!show) {
            _uiState.update { it.copy(configToEdit = null) }
        }
    }

    fun setConfigToEdit(config: ApiConfig?) {
        _uiState.update { it.copy(configToEdit = config, showAddDialog = true) }
    }

    fun addConfig(provider: String, modelName: String, displayName: String, apiKey: String) {
        viewModelScope.launch {
            repository.insertApiConfig(
                ApiConfig(
                    provider = provider,
                    modelName = modelName,
                    name = displayName,
                    apiKey = apiKey
                )
            )
            setShowAddDialog(false)
        }
    }

    fun updateConfig(config: ApiConfig) {
        viewModelScope.launch {
            repository.updateApiConfig(config)
            setShowAddDialog(false)
        }
    }

    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ApiConfigViewModel::class.java)) {
                return ApiConfigViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
