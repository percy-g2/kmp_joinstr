package invincible.privacy.joinstr.ui.registerInput

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import invincible.privacy.joinstr.LocalNotification
import invincible.privacy.joinstr.createPsbt
import invincible.privacy.joinstr.getHistoryStore
import invincible.privacy.joinstr.getPoolsStore
import invincible.privacy.joinstr.getSharedSecret
import invincible.privacy.joinstr.joinPsbts
import invincible.privacy.joinstr.ktx.hexToByteArray
import invincible.privacy.joinstr.model.CoinJoinHistory
import invincible.privacy.joinstr.model.ListUnspentResponseItem
import invincible.privacy.joinstr.model.LocalPoolContent
import invincible.privacy.joinstr.model.Methods
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.model.RpcResponse
import invincible.privacy.joinstr.network.HttpClient
import invincible.privacy.joinstr.network.NostrClient
import invincible.privacy.joinstr.network.json
import invincible.privacy.joinstr.ui.components.SnackbarController
import invincible.privacy.joinstr.ui.components.timeline.data.Item
import invincible.privacy.joinstr.utils.Event
import invincible.privacy.joinstr.utils.NostrCryptoUtils.createEvent
import invincible.privacy.joinstr.utils.NostrCryptoUtils.encrypt
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val historyStore
        get() = getHistoryStore()

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _listUnspent = mutableStateOf<List<ListUnspentResponseItem>?>(null)
    val listUnspent: State<List<ListUnspentResponseItem>?> = _listUnspent

    private val _selectedTx = mutableStateOf<ListUnspentResponseItem?>(null)
    val selectedTx: State<ListUnspentResponseItem?> = _selectedTx

    init {
        fetchListUnspent()
    }

    private fun fetchListUnspent() {
        viewModelScope.launch {
            _isLoading.value = true
            val rpcRequestBody = RpcRequestBody(
                method = Methods.LIST_UNSPENT.value
            )
            _listUnspent.value = httpClient.fetchNodeData<RpcResponse<List<ListUnspentResponseItem>>>(rpcRequestBody)?.result
            _isLoading.value = false
        }
    }

    fun setSelectedTxId(txId: String, vOut:Int) {
        val selectedTx = _listUnspent.value?.find { it.txid == txId && it.vout == vOut }
        _selectedTx.value = if (_selectedTx.value == selectedTx) null else selectedTx
    }

    fun getSelectedTxInfo(): Pair<String, Int>? {
        return _selectedTx.value?.let {
            Pair(it.txid, it.vout)
        }
    }

    fun registerInput(
        poolId: String,
        onSuccess: (Item) -> Unit,
        onError: () -> Unit,
    ) {
        viewModelScope.launch {
            val activePools = getPoolsStore().get()
                ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }
                ?.sortedByDescending { it.timeout }
            val selectedPool = activePools?.find { it.id == poolId } ?: throw IllegalStateException("Selected pool not found")

            val poolAmount = selectedPool.denomination.toBigDecimal(decimalMode = DecimalMode(decimalPrecision = 8, scale = 8, roundingMode = RoundingMode.CEILING))
            val selectedTxAmount = _selectedTx.value?.amount?.toBigDecimal(decimalMode = DecimalMode(decimalPrecision = 8, scale = 8, roundingMode = RoundingMode.CEILING)) ?: BigDecimal.ZERO

            val min = 0.00_000_500
            val max = 0.00_005_000
            val minOffset = min.toBigDecimal(decimalMode = DecimalMode(decimalPrecision = 8, scale = 8, roundingMode = RoundingMode.CEILING))
            val maxOffset = max.toBigDecimal(decimalMode = DecimalMode(decimalPrecision = 8, scale = 8, roundingMode = RoundingMode.CEILING))

            val isSelectedTxOutsideRange = !isWithinRange(poolAmount, selectedTxAmount, minOffset, maxOffset)

            if (isSelectedTxOutsideRange && poolAmount > selectedTxAmount) {
                SnackbarController.showMessage(
                    "Error: Selected input value is not within the specified range for this pool " +
                        "(denomination: ${poolAmount.toPlainString()} BTC) \n (selected: ${selectedTxAmount.toPlainString()} BTC)"
                )
                return@launch
            } else {
                _selectedTx.value?.let {
                    viewModelScope.launch {

                        val psbtBase64 = createPsbt(
                            poolId = poolId,
                            unspentItem = it
                        )

                        val walletProcessPsbtParams = JsonArray(
                            listOf(
                                JsonPrimitive(psbtBase64),
                                JsonPrimitive(true),
                                JsonPrimitive("ALL|ANYONECANPAY"),
                                JsonPrimitive(true),
                                JsonPrimitive(false)
                            )
                        )

                        val rpcWalletProcessPsbtParamsRequestBody = RpcRequestBody(
                            method = Methods.WALLET_PROCESS_PSBT.value,
                            params = walletProcessPsbtParams
                        )

                        val processPsbt = httpClient.fetchNodeData<RpcResponse<PsbtResponse>>(rpcWalletProcessPsbtParamsRequestBody)?.result

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

                        val item = Item(
                            id = 0,
                            title = "Register Input",
                            description = "Input registered with event id: ${nostrEvent.id}"
                        )
                        onSuccess.invoke(item)

                        nostrClient.sendEvent(
                            event = nostrEvent,
                            onSuccess = {
                                val waitItem = Item(
                                    id = 1,
                                    title = "Wait",
                                    description = "Waiting for other users to register input..."
                                )
                                onSuccess.invoke(waitItem)
                                SnackbarController.showMessage("Signed input registered for coinjoin.\nEvent ID: ${nostrEvent.id}")
                                checkRegisteredInputs(
                                    selectedPool = selectedPool,
                                    onSuccess = onSuccess
                                )
                            },
                            onError = { error ->
                                val msg = error ?: "Something went wrong while communicating with the relay.\nPlease try again."
                                SnackbarController.showMessage(msg)
                                onError.invoke()
                            }
                        )
                    }
                } ?: run {
                    if (PlatformUtils.IS_WASM_JS) {
                        SnackbarController.showMessage("PSBT creation is not yet supported for the web!")
                    } else {
                        SnackbarController.showMessage("An error occurred during PSBT creation!")
                    }
                    onError.invoke()
                    return@launch
                }
            }
        }
    }

    fun checkRegisteredInputs(
        selectedPool: LocalPoolContent,
        onSuccess: (Item) -> Unit
    ) {
        viewModelScope.launch {
            nostrClient.checkRegisteredInputs(
                publicKey = selectedPool.publicKey.hexToByteArray(),
                privateKey = selectedPool.privateKey.hexToByteArray(),
                onSuccess = { registeredAddressList ->
                    runCatching {
                        viewModelScope.launch {
                            getPoolsStore().update { existingPools ->
                                existingPools?.map {
                                    if (it.id == selectedPool.id) {
                                        it.copy(peersData = registeredAddressList)
                                    } else it
                                } ?: emptyList()
                            }
                        }
                        val listOfPsbts = registeredAddressList.mapNotNull { it.hex }
                        viewModelScope.launch {
                            val (psbt, rawTx) = joinPsbts(listOfPsbts.sortedWith(compareBy { it }))
                            if (rawTx != null && psbt != null) {
                                val waitItem = Item(
                                    id = 2,
                                    title = "Finalize Coinjoin Tx",
                                    description = "PSBT: $psbt"
                                )
                                onSuccess.invoke(waitItem)
                                val txId = httpClient.broadcastRawTx(rawTx)
                                if (txId != null) {
                                    val broadcastTxItem = Item(
                                        id = 3,
                                        title = "Broadcast Tx",
                                        info = "Tx: $txId"
                                    )

                                    CoroutineScope(Dispatchers.Default).launch {
                                        historyStore.update {
                                            val transaction = CoinJoinHistory(
                                                relay = selectedPool.relay,
                                                publicKey = selectedPool.publicKey,
                                                privateKey = selectedPool.privateKey,
                                                amount = selectedPool.denomination,
                                                psbt = psbt,
                                                tx = txId,
                                                timestamp = Clock.System.now().toEpochMilliseconds()
                                            )
                                            it?.plus(transaction) ?: listOf(transaction)
                                        }
                                        val result = LocalNotification.requestPermission()
                                        if (result) {
                                            LocalNotification.showNotification(
                                                title = "Coinjoin tx broadcast successful",
                                                message = "Check pool history for more details."
                                            )
                                        }
                                    }
                                    onSuccess.invoke(broadcastTxItem)
                                }
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

    fun isWithinRange(poolAmount: BigDecimal, selectedTxAmount: BigDecimal, minOffset: BigDecimal, maxOffset: BigDecimal): Boolean {
        val minAllowedAmount = poolAmount.plus(minOffset)
        val maxAllowedAmount = poolAmount.plus(maxOffset)
        return selectedTxAmount in minAllowedAmount..maxAllowedAmount
    }
}

@Serializable
data class PsbtResponse(
    val psbt: String,           // The base64-encoded partially signed transaction
    val complete: Boolean,      // Indicates if the transaction has a complete set of signatures
    val hex: String? = null     // Optional: The hex-encoded network transaction if complete
)