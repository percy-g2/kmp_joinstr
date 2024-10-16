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
                        selectedWallet = settings?.nodeConfig?.selectedWallet ?: ""
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
            it.copy(
                nostrRelay = relay,
                isNostrRelayValid = isValidWebSocketUrl(relay)
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
            it.copy(
                nodeUrl = nodeUrl,
                isNodeUrlValid = nodeUrl.isValidHttpUrl()
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
                    port = _uiState.value.port.toInt(),
                    selectedWallet = _uiState.value.selectedWallet
                )
                SettingsManager.updateNodeConfig(nodeConfig, _uiState.value.nostrRelay)

                if (_uiState.value.selectedWallet.isNotEmpty()) {
                    val loadWalletParams = JsonArray(listOf(JsonPrimitive(_uiState.value.selectedWallet)))
                    val loadWalletBody = RpcRequestBody(
                        method = Methods.LOAD_WALLET.value,
                        params = loadWalletParams
                    )
                    val loadWallet = httpClient.fetchNodeData<RpcResponse<Wallet>>(loadWalletBody)
                    if (loadWallet?.error != null) {
                        Napier.e(loadWallet.error.message)
                    } else Napier.i("Wallet ${_uiState.value.selectedWallet} loaded successfully")
                }
                _saveOperation.value = SaveOperation.Success
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
        return state.copy(
            isNostrRelayValid = isValidWebSocketUrl(state.nostrRelay),
            isNodeUrlValid = state.nodeUrl.isValidHttpUrl(),
            isUsernameValid = state.username.isNotBlank(),
            isPasswordValid = state.password.isNotBlank(),
            isPortValid = isValidPort(state.port)
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
    val isPortValid: Boolean = true
)

sealed class SaveOperation {
    data object Idle : SaveOperation()
    data object InProgress : SaveOperation()
    data object Success : SaveOperation()
    data class Error(val message: String) : SaveOperation()
}