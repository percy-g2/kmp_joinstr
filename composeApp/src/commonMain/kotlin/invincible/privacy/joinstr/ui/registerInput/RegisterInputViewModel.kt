package invincible.privacy.joinstr.ui.registerInput

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import invincible.privacy.joinstr.createPsbt
import invincible.privacy.joinstr.getPoolsStore
import invincible.privacy.joinstr.getSharedSecret
import invincible.privacy.joinstr.joinPsbts
import invincible.privacy.joinstr.ktx.hexToByteArray
import invincible.privacy.joinstr.model.ListUnspentResponseItem
import invincible.privacy.joinstr.model.Methods
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.model.RpcResponse
import invincible.privacy.joinstr.network.HttpClient
import invincible.privacy.joinstr.network.NostrClient
import invincible.privacy.joinstr.network.json
import invincible.privacy.joinstr.network.test
import invincible.privacy.joinstr.ui.components.SnackbarController
import invincible.privacy.joinstr.utils.Event
import invincible.privacy.joinstr.utils.NostrCryptoUtils.createEvent
import invincible.privacy.joinstr.utils.NostrCryptoUtils.encrypt
import io.ktor.util.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class RegisterInputViewModel : ViewModel() {
    private val httpClient = HttpClient()
    private val nostrClient = NostrClient()

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
        poolId: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val activePools = getPoolsStore().get()
                ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }
                ?.sortedByDescending { it.timeout }
            val selectedPool = activePools?.find { it.id == poolId } ?: throw IllegalStateException("Selected pool not found")
            _listUnspent.value?.find { it.txid == selectedTxId.value }?.let {
                viewModelScope.launch {
                    val psbtBase64 = createPsbt(
                        poolId = poolId,
                        unspentItem = it
                    )

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

                    println(processPsbt?.psbt)

                    val jsonObject = buildJsonObject {
                        put("hex", JsonPrimitive(processPsbt?.psbt))
                        put("type", JsonPrimitive("input"))
                    }
                    val data = json.encodeToString(JsonObject.serializer(), jsonObject)
                    val sharedSecret = getSharedSecret(
                        selectedPool.privateKey.hexToByteArray(),
                        selectedPool.publicKey.hexToByteArray()
                    )
                    val encryptedMessage = encrypt(data, sharedSecret)
                    val nostrEvent = createEvent(
                        content = encryptedMessage,
                        event = Event.ENCRYPTED_DIRECT_MESSAGE,
                        privateKey = selectedPool.privateKey.hexToByteArray(),
                        publicKey = selectedPool.publicKey.hexToByteArray(),
                        tagPubKey = selectedPool.publicKey
                    )

                    nostrClient.sendEvent(
                        event = nostrEvent,
                        onSuccess = {
                            SnackbarController.showMessage("Signed input registered for coinjoin.\nEvent ID: ${nostrEvent.id}")
                            onSuccess.invoke()
                            checkRegisteredInputs(
                                poolId = selectedPool.id,
                                privateKey = selectedPool.privateKey.hexToByteArray(),
                                publicKey = selectedPool.publicKey.hexToByteArray()
                            )
                        },
                        onError = { error ->
                            val msg = error ?: "Something went wrong while communicating with the relay.\nPlease try again."
                            SnackbarController.showMessage(msg)
                        }
                    )
                }
            } ?: run {
                if (PlatformUtils.IS_WASM_JS) {
                    SnackbarController.showMessage("PSBT creation is not yet supported for the web!")
                } else {
                    SnackbarController.showMessage("An error occurred during PSBT creation!")
                }
                return@launch
            }
        }
    }

    fun checkRegisteredInputs(
        poolId: String,
        publicKey: ByteArray,
        privateKey: ByteArray
    ) {
        viewModelScope.launch {
            nostrClient.checkRegisteredInputs(
                publicKey = publicKey,
                privateKey = privateKey,
                onSuccess = { registeredAddressList ->
                    runCatching {
                        viewModelScope.launch {
                            getPoolsStore().update { existingPools ->
                                existingPools?.map {
                                    if (it.id == poolId) {
                                        it.copy(peersData = registeredAddressList)
                                    } else it
                                } ?: emptyList()
                            }
                        }
                        val listOfPsbts = registeredAddressList.mapNotNull { it.hex }
                        viewModelScope.launch {
                            val rawTx = joinPsbts(listOfPsbts)
                            if (rawTx != null) {
                                httpClient.broadcastRawTx(rawTx)
                            } else {
                                SnackbarController.showMessage("Something went wrong.\nPlease try again.")
                            }
                        }
                    }
                },
                onError = { error ->
                    val msg = error ?: "Something went wrong while communicating with the relay.\nPlease try again."
                    SnackbarController.showMessage(msg)
                }
            )
        }
    }
}

@Serializable
data class PsbtResponse(
    val psbt: String,           // The base64-encoded partially signed transaction
    val complete: Boolean,      // Indicates if the transaction has a complete set of signatures
    val hex: String? = null     // Optional: The hex-encoded network transaction if complete
)