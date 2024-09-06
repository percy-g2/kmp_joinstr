package invincible.privacy.joinstr.ui.home

import androidx.lifecycle.ViewModel
import invincible.privacy.joinstr.model.BlockchainInfo
import invincible.privacy.joinstr.model.Methods
import invincible.privacy.joinstr.model.NetworkInfo
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.model.RpcResponse
import invincible.privacy.joinstr.network.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeScreenViewModel(
    private val httpClient: HttpClient = HttpClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeScreenUiState())
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    suspend fun fetchData() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        val networkInfo = fetchRpcData<NetworkInfo>(Methods.NETWORK_INFO)
        val blockchainInfo = fetchRpcData<BlockchainInfo>(Methods.BLOCK_CHAIN_INFO)

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            networkInfo = networkInfo,
            blockchainInfo = blockchainInfo
        )
    }

    private suspend inline fun <reified T> fetchRpcData(method: Methods): T? {
        return httpClient.fetchNodeData<RpcResponse<T>>(
            RpcRequestBody(method = method.value)
        )?.result
    }
}

data class HomeScreenUiState(
    val isLoading: Boolean = false,
    val networkInfo: NetworkInfo? = null,
    val blockchainInfo: BlockchainInfo? = null
)