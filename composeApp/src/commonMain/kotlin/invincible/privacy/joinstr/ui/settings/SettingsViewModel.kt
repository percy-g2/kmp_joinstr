package invincible.privacy.joinstr.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import invincible.privacy.joinstr.utils.NodeConfig
import invincible.privacy.joinstr.utils.SettingsStore
import invincible.privacy.joinstr.utils.SettingsManager
import invincible.privacy.joinstr.utils.Theme
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
                        nostrRelay = settings?.nostrRelay ?: SettingsStore().nostrRelay,
                        nodeUrl = settings?.nodeConfig?.url ?: SettingsStore().nodeConfig.url,
                        username = settings?.nodeConfig?.userName ?: SettingsStore().nodeConfig.userName,
                        password = settings?.nodeConfig?.password ?: SettingsStore().nodeConfig.password,
                        port = settings?.nodeConfig?.port?.toString() ?: SettingsStore().nodeConfig.port.toString(),
                        selectedTheme = settings?.selectedTheme ?: Theme.SYSTEM.id
                    ).also { newState ->
                        validateAllFields(newState)
                    }
                }
            }
        }
    }

    fun updateNostrRelay(relay: String) {
        _uiState.update {
            it.copy(
                nostrRelay = relay,
                isNostrRelayValid = isValidWebSocketUrl(relay)
            )
        }
    }

    fun updateNodeUrl(nodeUrl: String) {
        _uiState.update {
            it.copy(
                nodeUrl = nodeUrl,
                isNodeUrlValid = isValidHttpUrl(nodeUrl)
            )
        }
    }

    fun updateUsername(username: String) {
        _uiState.update {
            it.copy(
                username = username,
                isUsernameValid = username.isNotBlank()
            )
        }
    }

    fun updatePassword(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                isPasswordValid = password.isNotBlank()
            )
        }
    }

    fun updatePort(port: String) {
        _uiState.update {
            it.copy(
                port = port,
                isPortValid = isValidPort(port)
            )
        }
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

    private fun validateAllFields(state: SettingsUiState): SettingsUiState {
        return state.copy(
            isNostrRelayValid = isValidWebSocketUrl(state.nostrRelay),
            isNodeUrlValid = isValidHttpUrl(state.nodeUrl),
            isUsernameValid = state.username.isNotBlank(),
            isPasswordValid = state.password.isNotBlank(),
            isPortValid = isValidPort(state.port)
        )
    }

    private fun isValidWebSocketUrl(url: String): Boolean {
        val regex = "^(wss?://)[\\w.-]+(:\\d+)?(/.*)?$".toRegex()
        return url.isNotBlank() && regex.matches(url)
    }

    private fun isValidHttpUrl(url: String): Boolean {
        val regex = "^(https?://)[\\w.-]+(:\\d+)?(/.*)?$".toRegex()
        return url.isNotBlank() && regex.matches(url)
    }

    private fun isValidPort(port: String): Boolean {
        return port.isNotBlank() && port.toIntOrNull() in 1..65535
    }
}

data class SettingsUiState(
    val nostrRelay: String = "",
    val nodeUrl: String = "",
    val username: String = "",
    val password: String = "",
    val port: String = "",
    val selectedTheme: Int = Theme.SYSTEM.id,
    val isNostrRelayValid: Boolean = true,
    val isNodeUrlValid: Boolean = true,
    val isUsernameValid: Boolean = true,
    val isPasswordValid: Boolean = true,
    val isPortValid: Boolean = true
)

sealed class SaveOperation {
    data object Idle : SaveOperation()
    data object InProgress : SaveOperation()
    data object Success : SaveOperation()
    data class Error(val message: String) : SaveOperation()
}