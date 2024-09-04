package invincible.privacy.joinstr.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import invincible.privacy.joinstr.model.BlockchainInfo
import invincible.privacy.joinstr.model.Methods
import invincible.privacy.joinstr.model.NetworkInfo
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.model.RpcResponse
import invincible.privacy.joinstr.network.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeScreenViewModel : ViewModel() {
    private val httpClient = HttpClient()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _networkInfo = MutableStateFlow<NetworkInfo?>(null)
    val networkInfo: StateFlow<NetworkInfo?> = _networkInfo.asStateFlow()

    private val _blockchainInfo = MutableStateFlow<BlockchainInfo?>(null)
    val blockchainInfo: StateFlow<BlockchainInfo?> = _blockchainInfo.asStateFlow()

    init {
        fetchNetworkInfo()
    }

    fun fetchNetworkInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            _networkInfo.value = httpClient.fetchNodeData<RpcResponse<NetworkInfo>>(
                RpcRequestBody(
                    method = Methods.NETWORK_INFO.value
                )
            )?.result
            _blockchainInfo.value = httpClient.fetchNodeData<RpcResponse<BlockchainInfo>>(
                RpcRequestBody(
                    method = Methods.BLOCK_CHAIN_INFO.value
                )
            )?.result
            _isLoading.value = false
        }
    }
}