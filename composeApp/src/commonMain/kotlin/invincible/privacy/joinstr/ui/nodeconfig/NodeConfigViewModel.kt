package invincible.privacy.joinstr.ui.nodeconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import invincible.privacy.joinstr.Platform
import invincible.privacy.joinstr.getPlatform
import invincible.privacy.joinstr.model.Methods
import invincible.privacy.joinstr.model.NetworkInfo
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.model.RpcResponse
import invincible.privacy.joinstr.network.HttpClient
import invincible.privacy.joinstr.network.json
import invincible.privacy.joinstr.utils.NodeConfig
import invincible.privacy.joinstr.utils.SettingsManager
import invincible.privacy.joinstr.utils.SettingsStore
import io.github.aakira.napier.Napier
import io.ktor.client.request.basicAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Intent-based API for Node Configuration
 */
sealed interface NodeConfigIntent {
    data class RelayChanged(val value: String) : NodeConfigIntent
    data class NodeUrlChanged(val value: String) : NodeConfigIntent
    data class PortChanged(val value: String) : NodeConfigIntent
    data class UsernameChanged(val value: String) : NodeConfigIntent
    data class PasswordChanged(val value: String) : NodeConfigIntent
    object TestConnection : NodeConfigIntent
    object Save : NodeConfigIntent
}

/**
 * Status enum for state machine
 */
sealed interface Status {
    object Idle : Status
    object Editing : Status
    object Testing : Status
    object Success : Status
    object Saving : Status
    object Saved : Status
}

/**
 * Error types for proper error handling
 */
sealed interface ErrorType {
    data class ValidationError(val field: String, val message: String) : ErrorType
    object AuthError : ErrorType
    object NetworkError : ErrorType
    data class UnknownError(val message: String) : ErrorType
}

/**
 * Immutable UiState - Single Source of Truth
 */
data class NodeConfigUiState(
    val relayUrl: String = "",
    val nodeUrl: String = "",
    val port: String = "",
    val username: String = "",
    val password: String = "",
    val status: Status = Status.Idle,
    val error: ErrorType? = null,
    val testSuccess: Boolean = false
) {
    val canSave: Boolean
        get() = testSuccess && status !is Status.Saving && status !is Status.Testing
    
    val canTest: Boolean
        get() = status !is Status.Testing && status !is Status.Saving && 
                isValidForTest()
    
    private fun isValidForTest(): Boolean {
        return nodeUrl.isNotBlank() && 
               port.isNotBlank() &&
               username.isNotBlank() &&
               password.isNotBlank()
    }
}

class NodeConfigViewModel : ViewModel() {
    private val httpClient = HttpClient()
    
    private val _uiState = MutableStateFlow(NodeConfigUiState())
    val uiState: StateFlow<NodeConfigUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            // Load existing settings
            val settings = SettingsManager.store.get() ?: SettingsStore()
            _uiState.update { currentState ->
                currentState.copy(
                    relayUrl = settings.nostrRelay,
                    nodeUrl = settings.nodeConfig.url,
                    port = settings.nodeConfig.port.toString().takeIf { it != "0" } ?: "",
                    username = settings.nodeConfig.userName,
                    password = settings.nodeConfig.password,
                    status = Status.Editing
                )
            }
        }
    }
    
    fun handleIntent(intent: NodeConfigIntent) {
        when (intent) {
            is NodeConfigIntent.RelayChanged -> handleRelayChanged(intent.value)
            is NodeConfigIntent.NodeUrlChanged -> handleNodeUrlChanged(intent.value)
            is NodeConfigIntent.PortChanged -> handlePortChanged(intent.value)
            is NodeConfigIntent.UsernameChanged -> handleUsernameChanged(intent.value)
            is NodeConfigIntent.PasswordChanged -> handlePasswordChanged(intent.value)
            is NodeConfigIntent.TestConnection -> handleTestConnection()
            is NodeConfigIntent.Save -> handleSave()
        }
    }
    
    private fun handleRelayChanged(value: String) {
        _uiState.update { state ->
            val isValid = value.isBlank() || isValidWebSocketUrl(value)
            state.copy(
                relayUrl = value,
                status = Status.Editing,
                error = if (isValid) null else ErrorType.ValidationError(
                    field = "relayUrl",
                    message = "Invalid WebSocket URL (must start with ws:// or wss://)"
                )
            )
        }
    }

    private fun handleNodeUrlChanged(value: String) {
        _uiState.update { state ->
            val isValid = value.isBlank() || isValidHostOrIp(value)
            state.copy(
                nodeUrl = value,
                status = Status.Editing,
                testSuccess = false, // Reset test success when URL changes
                error = if (isValid) null else ErrorType.ValidationError(
                    field = "nodeUrl",
                    message = "Invalid host or IP address"
                )
            )
        }
    }
    
    private fun handlePortChanged(value: String) {
        _uiState.update { state ->
            val isValid = value.isBlank() || isValidPort(value)
            state.copy(
                port = value,
                status = Status.Editing,
                testSuccess = false, // Reset test success when port changes
                error = if (isValid) null else ErrorType.ValidationError(
                    field = "port",
                    message = "Invalid port (must be 1-65535)"
                )
            )
        }
    }
    
    private fun handleUsernameChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                username = value,
                status = Status.Editing,
                testSuccess = false // Reset test success when credentials change
            )
        }
    }
    
    private fun handlePasswordChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                password = value,
                status = Status.Editing,
                testSuccess = false // Reset test success when credentials change
            )
        }
    }
    
    private fun handleTestConnection() {
        val state = _uiState.value
        
        // Validate all fields before testing
        val validationErrors = validateFields(state)
        if (validationErrors.isNotEmpty()) {
            _uiState.update { 
                it.copy(
                    status = Status.Editing,
                    error = validationErrors.first()
                )
            }
            return
        }
        
        _uiState.update { it.copy(status = Status.Testing, error = null) }
        
        viewModelScope.launch {
            try {
                val portValue = state.port.toIntOrNull() ?: return@launch
                
                // Create temporary HTTP client for testing
                val testClient = httpClient.createHttpClient(timeout = 10)
                
                // Convert localhost/127.0.0.1 to 10.0.2.2 on Android (emulator host alias)
                val normalizedUrl = normalizeUrlForPlatform(state.nodeUrl)
                
                val testUrl = if (normalizedUrl.startsWith("http://") || normalizedUrl.startsWith("https://")) {
                    "${normalizedUrl}:${portValue}/"
                } else {
                    "http://${normalizedUrl}:${portValue}/"
                }
                
                val response = testClient.post {
                    url(testUrl)
                    basicAuth(
                        username = state.username,
                        password = state.password
                    )
                    setBody(RpcRequestBody(method = Methods.NETWORK_INFO.value))
                }
                
                when {
                    response.status == HttpStatusCode.OK -> {
                        // Manually deserialize to avoid star projection issues with kotlinx.serialization
                        val bodyText = response.bodyAsText()
                        val body: RpcResponse<NetworkInfo> = json.decodeFromString(bodyText)
                        if (body.error != null) {
                            when (body.error.code) {
                                401, 403 -> {
                                    _uiState.update {
                                        it.copy(
                                            status = Status.Editing,
                                            error = ErrorType.AuthError
                                        )
                                    }
                                }
                                else -> {
                                    _uiState.update {
                                        it.copy(
                                            status = Status.Editing,
                                            error = ErrorType.UnknownError(body.error.message)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Success!
                            _uiState.update {
                                it.copy(
                                    status = Status.Success,
                                    error = null,
                                    testSuccess = true
                                )
                            }
                        }
                    }
                    response.status == HttpStatusCode.Unauthorized || 
                    response.status == HttpStatusCode.Forbidden -> {
                        _uiState.update {
                            it.copy(
                                status = Status.Editing,
                                error = ErrorType.AuthError
                            )
                        }
                    }
                    else -> {
                        _uiState.update {
                            it.copy(
                                status = Status.Editing,
                                error = ErrorType.NetworkError
                            )
                        }
                    }
                }
            } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
                _uiState.update {
                    it.copy(
                        status = Status.Editing,
                        error = ErrorType.NetworkError
                    )
                }
            }catch (e: Exception) {
                // Never log password - redact it
                Napier.e("Test connection failed", e)
                _uiState.update {
                    it.copy(
                        status = Status.Editing,
                        error = ErrorType.UnknownError(e.message ?: "Unknown error occurred")
                    )
                }
            }
        }
    }
    
    private fun handleSave() {
        val state = _uiState.value
        
        // Must have successful test before saving
        if (!state.testSuccess) {
            _uiState.update {
                it.copy(
                    error = ErrorType.ValidationError(
                        field = "test",
                        message = "Please test connection before saving"
                    )
                )
            }
            return
        }
        
        // Validate all fields again
        val validationErrors = validateFields(state)
        if (validationErrors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    status = Status.Editing,
                    error = validationErrors.first()
                )
            }
            return
        }
        
        _uiState.update { it.copy(status = Status.Saving, error = null) }
        
        viewModelScope.launch {
            try {
                val portValue = state.port.toIntOrNull() ?: return@launch
                
                val nodeConfig = NodeConfig(
                    url = state.nodeUrl,
                    userName = state.username,
                    password = state.password,
                    port = portValue,
                    selectedWallet = SettingsManager.store.get()?.nodeConfig?.selectedWallet ?: ""
                )
                
                SettingsManager.updateSettings(
                    vpnGateway = SettingsManager.store.get()?.vpnGateway,
                    nodeConfig = nodeConfig,
                    nostrRelay = state.relayUrl
                )
                
                _uiState.update { it.copy(status = Status.Saved) }
            } catch (e: Exception) {
                Napier.e("Save failed", e)
                _uiState.update {
                    it.copy(
                        status = Status.Editing,
                        error = ErrorType.UnknownError(e.message ?: "Failed to save configuration")
                    )
                }
            }
        }
    }
    
    private fun validateFields(state: NodeConfigUiState): List<ErrorType> {
        val errors = mutableListOf<ErrorType>()
        
        // Relay URL is managed in Settings screen, so no validation needed here

        // Node URL validation (required)
        if (state.nodeUrl.isBlank()) {
            errors.add(ErrorType.ValidationError("nodeUrl", "Node URL is required"))
        } else if (!isValidHostOrIp(state.nodeUrl)) {
            errors.add(ErrorType.ValidationError("nodeUrl", "Invalid host or IP address"))
        }
        
        // Port validation (required)
        if (state.port.isBlank()) {
            errors.add(ErrorType.ValidationError("port", "Port is required"))
        } else if (!isValidPort(state.port)) {
            errors.add(ErrorType.ValidationError("port", "Invalid port (must be 1-65535)"))
        }
        
        // Username validation (optional per spec)
        // Password validation (optional per spec)
        
        return errors
    }
    
    private fun isValidWebSocketUrl(url: String): Boolean {
        val regex = "^(wss?://)[\\w.-]+(:\\d+)?(/.*)?$".toRegex()
        return url.isNotBlank() && regex.matches(url)
    }
    
    private fun isValidHostOrIp(host: String): Boolean {
        // Remove protocol if present
        val cleaned = host.removePrefix("http://").removePrefix("https://")
        
        // Check if it's a valid IP address
        val ipRegex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$".toRegex()
        if (ipRegex.matches(cleaned)) return true
        
        // Check if it's a valid hostname
        val hostnameRegex = "^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$|^localhost$|^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?$".toRegex()
        return hostnameRegex.matches(cleaned)
    }
    
    private fun isValidPort(port: String): Boolean {
        return port.toIntOrNull() in 1..65535
    }
    
    /**
     * Normalizes URLs for platform-specific localhost handling.
     * On Android, converts 127.0.0.1/localhost to 10.0.2.2 (emulator host alias).
     * On other platforms, returns the URL as-is.
     * Note: Port is handled separately, so this only normalizes the host part.
     */
    private fun normalizeUrlForPlatform(url: String): String {
        if (getPlatform() != Platform.ANDROID) {
            return url
        }
        
        // Extract protocol if present
        val hasHttps = url.startsWith("https://")
        val hasHttp = url.startsWith("http://")
        val protocolPrefix = when {
            hasHttps -> "https://"
            hasHttp -> "http://"
            else -> ""
        }
        
        // Remove protocol prefix to get the host part
        val host = url.removePrefix(protocolPrefix).split("/").first()
        
        // Check if it's exactly localhost or 127.0.0.1 (not a substring)
        val isLocalhost = host == "localhost" || host == "127.0.0.1"
        
        if (isLocalhost) {
            // Replace with Android emulator host alias
            val replacement = "10.0.2.2"
            return "$protocolPrefix$replacement"
        }
        
        return url
    }
}
