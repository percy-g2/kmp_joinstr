package invincible.privacy.joinstr.ui.registerInput

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.acinq.bitcoin.Bitcoin
import fr.acinq.bitcoin.Block
import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.OutPoint
import fr.acinq.bitcoin.Satoshi
import fr.acinq.bitcoin.Script
import fr.acinq.bitcoin.SigHash.SIGHASH_ALL
import fr.acinq.bitcoin.SigHash.SIGHASH_ANYONECANPAY
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.TxHash
import fr.acinq.bitcoin.TxId
import fr.acinq.bitcoin.TxIn
import fr.acinq.bitcoin.TxOut
import fr.acinq.bitcoin.psbt.Psbt
import invincible.privacy.joinstr.getPoolsStore
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
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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

    @OptIn(ExperimentalEncodingApi::class)
    fun registerInput(
        poolId: String
    ) {
        viewModelScope.launch {


            val activePools = getPoolsStore().get()
                ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }
                ?.sortedByDescending { it.timeout }

            val selectedPool = activePools?.find { it.id == poolId }
                ?: throw IllegalStateException("Selected pool not found")

            val poolAmount = selectedPool.denomination
            val selectedTx = _listUnspent.value?.find { it.txid == _selectedTxId.value }
                ?: throw IllegalStateException("Selected transaction not found")
            val selectedTxAmount = selectedTx.amount
            val estimatedVByteSize = 100 * selectedPool.peers
            val estimatedBtcFee = (selectedPool.feeRate.toFloat() * estimatedVByteSize.toFloat()) / 100000000
            println("estimatedBtcFee: $estimatedBtcFee")

            val (txid, outputIndex) = getSelectedTxInfo() ?: throw IllegalStateException("No transaction selected")
            val outputAmount = poolAmount - estimatedBtcFee
            val sighashType = SIGHASH_ALL or SIGHASH_ANYONECANPAY

            val input = TxIn(
                outPoint = OutPoint(TxId(TxHash(txid)), outputIndex.toLong()),
                sequence = 0xFFFFFFFFL,
                signatureScript = listOf()
            )

            val outputs = selectedPool.peersData
                .filter { it.type == "output" }
                .mapNotNull { peerData ->
                    Bitcoin.addressToPublicKeyScript(Block.SignetGenesisBlock.hash, peerData.address).fold(
                        { error ->
                            println("Error creating output script for address ${peerData.address}: ${error.message}")
                            null
                        },
                        { scriptElts ->
                            TxOut(
                                amount = Satoshi((outputAmount * 100_000_000).toLong()),
                                publicKeyScript = ByteVector(Script.write(scriptElts))
                            )
                        }
                    )
                }

            if (!((poolAmount * 100_000_000) + 500 <= selectedTxAmount * 100_000_000 &&
                    selectedTxAmount * 100_000_000 <= (poolAmount * 100_000_000) + 5000)
            ) {
                SnackbarController.showMessage("Error: Selected input value is not within the specified range for this pool " +
                    "(denomination: $poolAmount BTC)")
               // return@launch  // Exit the coroutine if the condition is not met
            }

            val transaction = Transaction(
                version = 1L,
                txIn = listOf(input),
                txOut = outputs,
                lockTime = 0
            )

            val unsignedPsbt = Psbt(transaction)

            val updatedPsbt = unsignedPsbt.updateWitnessInput(
                outPoint = input.outPoint,
                txOut = TxOut(
                    Satoshi((selectedTxAmount * 100_000_000).toLong()),
                    ByteVector(selectedTx.scriptPubKey)
                ),
                sighashType = sighashType
            ).right ?: throw IllegalStateException("Failed to update PSBT")

            // Encode PSBT to Base64
            val psbtBytes = Psbt.write(updatedPsbt)
            val psbtBase64 = Base64.encode(psbtBytes.toByteArray())

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