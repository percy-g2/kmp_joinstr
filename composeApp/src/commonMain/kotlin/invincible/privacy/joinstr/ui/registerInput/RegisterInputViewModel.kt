package invincible.privacy.joinstr.ui.registerInput

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import invincible.privacy.joinstr.createPsbt
import invincible.privacy.joinstr.model.Input
import invincible.privacy.joinstr.model.ListUnspentResponseItem
import invincible.privacy.joinstr.model.Methods
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.model.RpcResponse
import invincible.privacy.joinstr.network.HttpClient
import invincible.privacy.joinstr.network.json
import invincible.privacy.joinstr.network.test
import invincible.privacy.joinstr.ui.components.SnackbarController
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

class RegisterInputViewModel : ViewModel() {
    private val httpClient = HttpClient()

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _listUnspent = mutableStateOf<List<ListUnspentResponseItem>?>(null)
    val listUnspent: State<List<ListUnspentResponseItem>?> = _listUnspent

    private val _selectedTxId = mutableStateOf("")
    val selectedTxId: State<String> = _selectedTxId

    init {
        fetchListUnspent()
    }

    private fun fetchListUnspent() {
        viewModelScope.launch {
            _isLoading.value = true
            val rpcRequestBody = RpcRequestBody(
                method = Methods.LIST_UNSPENT.value
            )
            _listUnspent.value = httpClient
                .fetchNodeData<RpcResponse<List<ListUnspentResponseItem>>>(rpcRequestBody)?.result
                ?: json.decodeFromString<RpcResponse<List<ListUnspentResponseItem>>>(test).result
            _isLoading.value = false
        }
    }

    fun setSelectedTxId(txId: String) {
        _selectedTxId.value = if (_selectedTxId.value == txId) "" else txId
    }

    fun getSelectedTxInfo(): Pair<String, Int>? {
        return _listUnspent.value?.find { it.txid == _selectedTxId.value }?.let {
            Pair(it.txid, it.vout)
        }
    }

    fun registerInput(
        poolId: String
    ) {
        viewModelScope.launch {
            val psbtBase64 = _listUnspent.value?.find { it.txid == selectedTxId.value }?.let {
                createPsbt(
                    poolId = poolId,
                    unspentItem = it
                )
            } ?: run {
                SnackbarController.showMessage("Something went wrong while creating psbt!")
                return@launch
            }
            val walletProcessPsbtParams = JsonArray(listOf(
                JsonPrimitive(psbtBase64),
                JsonPrimitive(true),
                JsonPrimitive("ALL|ANYONECANPAY"),
                JsonPrimitive(true),
                JsonPrimitive(false)
            ))

            val rpcWalletProcessPsbtParamsRequestBody = RpcRequestBody(
                method = Methods.WALLET_PROCESS_PSBT.value,
                params = walletProcessPsbtParams
            )

            val processPsbt = httpClient.fetchNodeData<RpcResponse<PsbtResponse>>(rpcWalletProcessPsbtParamsRequestBody)?.result

            println(processPsbt?.hex)
        }
    }
}

@Serializable
data class PsbtRequest(
    val inputs: List<Input>,
    val outputs: Map<String, Float>
)

@Serializable
data class PsbtResponse(
    val psbt: String,           // The base64-encoded partially signed transaction
    val complete: Boolean,      // Indicates if the transaction has a complete set of signatures
    val hex: String? = null     // Optional: The hex-encoded network transaction if complete
)