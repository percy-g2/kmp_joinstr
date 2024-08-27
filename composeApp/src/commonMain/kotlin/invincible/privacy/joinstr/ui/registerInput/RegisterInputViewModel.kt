package invincible.privacy.joinstr.ui.registerInput

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import invincible.privacy.joinstr.model.ListUnspentResponseItem
import invincible.privacy.joinstr.model.Methods
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.model.RpcResponse
import invincible.privacy.joinstr.network.HttpClient
import invincible.privacy.joinstr.network.json
import invincible.privacy.joinstr.network.test
import kotlinx.coroutines.launch

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
}