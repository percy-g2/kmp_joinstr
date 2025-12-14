package invincible.privacy.joinstr.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import invincible.privacy.joinstr.ktx.isValidHttpUrl
import invincible.privacy.joinstr.model.Methods
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.model.RpcResponse
import invincible.privacy.joinstr.model.VpnGateway
import invincible.privacy.joinstr.model.Wallet
import invincible.privacy.joinstr.model.WalletResult
import invincible.privacy.joinstr.network.HttpClient
import invincible.privacy.joinstr.utils.NodeConfig
import invincible.privacy.joinstr.utils.SettingsManager
import invincible.privacy.joinstr.utils.SettingsStore
import invincible.privacy.joinstr.utils.Theme
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

class SettingsViewModel : ViewModel() {
    private val httpClient = HttpClient()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _saveOperation = MutableStateFlow<SaveOperation>(SaveOperation.Idle)
    val saveOperation: StateFlow<SaveOperation> = _saveOperation.asStateFlow()

    private val _walletList = MutableStateFlow(emptyList<String>())
    val walletList: StateFlow<List<String>> = _walletList.asStateFlow()

    private val _vpnGatewayList = MutableStateFlow(emptyList<VpnGateway>())
    val vpnGatewayList: StateFlow<List<VpnGateway>> = _vpnGatewayList.asStateFlow()

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
                        selectedTheme = settings?.selectedTheme ?: Theme.SYSTEM.id,
                        selectedWallet = settings?.nodeConfig?.selectedWallet ?: "",
                        selectedVpnGateway = settings?.vpnGateway
                    ).also { newState ->
                        validateAllFields(newState)
                    }
                }
            }
        }
        fetchWalletList()
        fetchVpnGatewayList()
    }

    fun updateNostrRelay(relay: String) {
        _uiState.update {
            val isValid = relay.isBlank() || isValidWebSocketUrl(relay)
            it.copy(
                nostrRelay = relay,
                isNostrRelayValid = isValid
            )
        }
    }

    private fun fetchWalletList() {
        viewModelScope.launch {
            val walletListBody = RpcRequestBody(
                method = Methods.LIST_WALLETS.value
            )
            _walletList.value = httpClient.fetchNodeData<RpcResponse<WalletResult>>(walletListBody)
                ?.result?.wallets?.map { it.name }?.sorted() ?: emptyList()
        }
    }

    private fun fetchVpnGatewayList() {
        viewModelScope.launch {
            _vpnGatewayList.value = httpClient.fetchVpnGateways() ?: emptyList()
        }
    }

    fun updateNodeUrl(nodeUrl: String) {
        _uiState.update {
            // Allow empty, but validate format when not empty
            val isValid = nodeUrl.isBlank() || nodeUrl.isValidHttpUrl()
            val newState = it.copy(
                nodeUrl = nodeUrl,
                isNodeUrlValid = isValid
            )
            // Revalidate port when node URL changes (port becomes required if URL is provided)
            val portValid = if (nodeUrl.isNotBlank()) {
                newState.port.isNotBlank() && isValidPort(newState.port)
            } else {
                newState.port.isBlank() || isValidPort(newState.port)
            }
            newState.copy(isPortValid = portValid)
        }
    }

    fun updateUsername(username: String) {
        _uiState.update {
            // Show error only if save was attempted and field is empty
            val isValid = username.isNotBlank() || !it.hasAttemptedSave
            it.copy(
                username = username,
                isUsernameValid = isValid
            )
        }
    }

    fun updatePassword(password: String) {
        _uiState.update {
            // Show error only if save was attempted and field is empty
            val isValid = password.isNotBlank() || !it.hasAttemptedSave
            it.copy(
                password = password,
                isPasswordValid = isValid
            )
        }
    }

    fun updatePort(port: String) {
        _uiState.update {
            // Port is required if node URL is provided
            val isValid = if (it.nodeUrl.isNotBlank()) {
                port.isNotBlank() && isValidPort(port)
            } else {
                port.isBlank() || isValidPort(port)
            }
            it.copy(
                port = port,
                isPortValid = isValid
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
            // Mark that save has been attempted to show validation errors
            _uiState.update { it.copy(hasAttemptedSave = true) }
            
            // Validate all fields before saving
            val validatedState = validateAllFields(_uiState.value.copy(hasAttemptedSave = true))
            _uiState.value = validatedState
            
            // Check if all required fields are valid
            if (!validatedState.isNostrRelayValid || !validatedState.isNodeUrlValid ||
                !validatedState.isUsernameValid || !validatedState.isPasswordValid || !validatedState.isPortValid) {
                val errors = mutableListOf<String>()
                if (!validatedState.isNostrRelayValid && validatedState.nostrRelay.isNotBlank()) {
                    errors.add("Invalid Nostr Relay URL")
                }
                if (!validatedState.isNodeUrlValid && validatedState.nodeUrl.isNotBlank()) {
                    errors.add("Invalid Node URL")
                }
                if (!validatedState.isUsernameValid) {
                    errors.add("Username is required")
                }
                if (!validatedState.isPasswordValid) {
                    errors.add("Password is required")
                }
                if (!validatedState.isPortValid && validatedState.port.isNotBlank()) {
                    errors.add("Invalid port number")
                }
                if (validatedState.nodeUrl.isNotBlank() && validatedState.port.isBlank()) {
                    errors.add("Port is required when Node URL is provided")
                }
                
                val errorMessage = if (errors.isEmpty()) {
                    "Please fill in all required fields"
                } else {
                    errors.joinToString(", ")
                }
                _saveOperation.value = SaveOperation.Error(errorMessage)
                return@launch
            }
            
            _saveOperation.value = SaveOperation.InProgress
            delay(500)
            try {
                val portValue = validatedState.port.toIntOrNull()
                if (portValue == null || portValue !in 1..65535) {
                    _saveOperation.value = SaveOperation.Error("Invalid port number (must be 1-65535)")
                    return@launch
                }
                
                val nodeConfig = NodeConfig(
                    url = validatedState.nodeUrl,
                    userName = validatedState.username,
                    password = validatedState.password,
                    port = portValue,
                    selectedWallet = validatedState.selectedWallet
                )
                SettingsManager.updateSettings(validatedState.selectedVpnGateway, nodeConfig, validatedState.nostrRelay)

                if (validatedState.selectedWallet.isNotEmpty()) {
                    val loadWalletParams = JsonArray(listOf(JsonPrimitive(validatedState.selectedWallet)))
                    val loadWalletBody = RpcRequestBody(
                        method = Methods.LOAD_WALLET.value,
                        params = loadWalletParams
                    )
                    val loadWallet = httpClient.fetchNodeData<RpcResponse<Wallet>>(loadWalletBody)
                    if (loadWallet?.error != null) {
                        Napier.e(loadWallet.error.message)
                    } else Napier.i("Wallet ${validatedState.selectedWallet} loaded successfully")
                }
                _saveOperation.value = SaveOperation.Success
                // Reset save attempt flag on success
                _uiState.update { it.copy(hasAttemptedSave = false) }
            } catch (e: Exception) {
                _saveOperation.value = SaveOperation.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun updateSelectedWallet(wallet: String) {
        _uiState.update { it.copy(selectedWallet = wallet) }
    }

    fun updateSelectedVpnGateway(vpnGateway: VpnGateway) {
        _uiState.update { it.copy(selectedVpnGateway = vpnGateway) }
    }

    private fun validateAllFields(state: SettingsUiState): SettingsUiState {
        // Port is required if node URL is provided
        val portValid = if (state.nodeUrl.isNotBlank()) {
            state.port.isNotBlank() && isValidPort(state.port)
        } else {
            state.port.isBlank() || isValidPort(state.port)
        }
        
        return state.copy(
            isNostrRelayValid = state.nostrRelay.isBlank() || isValidWebSocketUrl(state.nostrRelay),
            isNodeUrlValid = state.nodeUrl.isBlank() || state.nodeUrl.isValidHttpUrl(),
            isUsernameValid = state.username.isNotBlank() || !state.hasAttemptedSave,
            isPasswordValid = state.password.isNotBlank() || !state.hasAttemptedSave,
            isPortValid = portValid
        )
    }

    private fun isValidWebSocketUrl(url: String): Boolean {
        val regex = "^(wss?://)[\\w.-]+(:\\d+)?(/.*)?$".toRegex()
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
    val selectedWallet: String = "",
    val selectedVpnGateway: VpnGateway? = null,
    val selectedTheme: Int = Theme.SYSTEM.id,
    val isNostrRelayValid: Boolean = true,
    val isNodeUrlValid: Boolean = true,
    val isUsernameValid: Boolean = true,
    val isPasswordValid: Boolean = true,
    val isPortValid: Boolean = true,
    val hasAttemptedSave: Boolean = false
)

sealed class SaveOperation {
    data object Idle : SaveOperation()
    data object InProgress : SaveOperation()
    data object Success : SaveOperation()
    data class Error(val message: String) : SaveOperation()
}