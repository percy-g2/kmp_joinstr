package invincible.privacy.joinstr.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import invincible.privacy.joinstr.theme.NodeConfig
import invincible.privacy.joinstr.theme.Settings
import invincible.privacy.joinstr.theme.SettingsManager
import invincible.privacy.joinstr.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _saveOperation = MutableStateFlow<SaveOperation>(SaveOperation.Idle)
    val saveOperation: StateFlow<SaveOperation> = _saveOperation.asStateFlow()

    init {
        viewModelScope.launch {
            SettingsManager.store.updates.collect { settings ->
                _uiState.update { currentState ->
                    currentState.copy(
                        nostrRelay = settings?.nostrRelay ?: Settings().nostrRelay,
                        nodeUrl = settings?.nodeConfig?.url ?: Settings().nodeConfig.url,
                        username = settings?.nodeConfig?.userName ?: Settings().nodeConfig.userName,
                        password = settings?.nodeConfig?.password ?: Settings().nodeConfig.password,
                        port = settings?.nodeConfig?.port?.toString() ?: Settings().nodeConfig.port.toString(),
                        selectedTheme = settings?.selectedTheme ?: Theme.SYSTEM.id
                    )
                }
            }
        }
    }

    fun updateNostrRelay(relay: String) {
        _uiState.update { it.copy(nostrRelay = relay) }
    }

    fun updateNodeUrl(url: String) {
        _uiState.update { it.copy(nodeUrl = url) }
    }

    fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun updatePort(port: String) {
        _uiState.update { it.copy(port = port) }
    }

    fun updateTheme(themeId: Int) {
        viewModelScope.launch {
            SettingsManager.updateTheme(themeId)
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            _saveOperation.value = SaveOperation.InProgress
            delay(500)
            try {
                val nodeConfig = NodeConfig(
                    url = _uiState.value.nodeUrl,
                    userName = _uiState.value.username,
                    password = _uiState.value.password,
                    port = _uiState.value.port.toInt()
                )
                SettingsManager.updateNodeConfig(nodeConfig, _uiState.value.nostrRelay)
                _saveOperation.value = SaveOperation.Success
            } catch (e: Exception) {
                _saveOperation.value = SaveOperation.Error(e.message ?: "An error occurred")
            }
        }
    }
}

data class SettingsUiState(
    val nostrRelay: String = "",
    val nodeUrl: String = "",
    val username: String = "",
    val password: String = "",
    val port: String = "",
    val selectedTheme: Int = Theme.SYSTEM.id
)

sealed class SaveOperation {
    object Idle : SaveOperation()
    object InProgress : SaveOperation()
    object Success : SaveOperation()
    data class Error(val message: String) : SaveOperation()
}