package invincible.privacy.joinstr.ui.registerInput

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.acinq.bitcoin.Base58Check
import fr.acinq.bitcoin.ByteVector32
import fr.acinq.bitcoin.PrivateKey
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
            val activePools = getPoolsStore().get()
                ?.sortedByDescending { it.timeout }
                ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }

            val selectedPool = activePools?.find { it.id == poolId }
            val poolAmount = selectedPool?.denomination ?: 0f
            val selectedTxAmount = _listUnspent.value?.find { it.txid == _selectedTxId.value }?.amount ?: 0.0
            val estimatedVByteSize = 100 * (selectedPool?.peers ?: 0)
            val estimatedBtcFee = ((selectedPool?.feeRate?.toFloat() ?: 0f) * estimatedVByteSize.toFloat()) / 100000000
            println("estimatedBtcFee: $estimatedBtcFee")
            if (!((poolAmount * 100_000_000) + 500 <= selectedTxAmount &&
                    selectedTxAmount <= (poolAmount * 100_000_000) + 5000)
            ) {

                SnackbarController.showMessage("Error: Selected input value is not within the specified range for this pool " +
                    "(denomination: $poolAmount BTC)")
            }
           /* val activePools = getPoolsStore().get()
                ?.sortedByDescending { it.timeout }
                ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }

            val selectedPool = activePools?.find { it.id == poolId }
            val poolAmount = selectedPool?.denomination ?: 0f
            val selectedTxAmount = _listUnspent.value?.find { it.txid == _selectedTxId.value }?.amount ?: 0.0
            val estimatedVByteSize = 100 * (selectedPool?.peers ?: 0)
            val estimatedBtcFee = ((selectedPool?.feeRate?.toFloat() ?: 0f) * estimatedVByteSize.toFloat()) / 100000000

            println("estimatedBtcFee: $estimatedBtcFee")


            val outputAmount = poolAmount - estimatedBtcFee
            val sighashType = SIGHASH_ALL or SIGHASH_ANYONECANPAY
            val (txid, outputIndex) = getSelectedTxInfo() ?: throw IllegalStateException("No transaction selected")

// Define inputs with proper error checking
            val inputs = try {
                listOf(
                    TxIn(
                        outPoint = OutPoint(
                            txid = TxId(ByteVector32.fromValidHex(txid)),
                            index = outputIndex.toLong()
                        ),
                        signatureScript = ByteVector.empty,
                        sequence = 0xffffffffL,
                        witness = ScriptWitness.empty
                    )
                )
            } catch (e: Exception) {
                throw IllegalStateException("Failed to create input: ${e.message}")
            }


            val listOfOutputAddresses = selectedPool?.peersData
                ?.filter { it.type == "output" }
                ?.map { it.address }

            val outputs = listOfOutputAddresses?.mapNotNull { address ->
                Bitcoin.addressToPublicKeyScript(Block.SignetGenesisBlock.hash, address).fold(
                    { error ->
                        println("Error creating output script for address $address: ${error.message}")
                        null  // Skip this output if there's an error
                    },
                    { scriptElts ->
                        val outputScript = Script.write(scriptElts)
                        TxOut(
                            amount = Satoshi((outputAmount * 100_000_000).toLong()),
                            publicKeyScript = outputScript
                        )
                    }
                )
            } ?: emptyList()


// Create PSBT
            val transaction = Transaction(
                version = 2,
                txIn = inputs,
                txOut = outputs,
                lockTime = 0
            )



            val psbt = Psbt(transaction)


            val signed = psbt.sign(getPrivateKeyFromString(selectedPool?.privateKey ?: ""),getSelectedTxInfo()?.second ?: 0)

            println("Input TXID: ${inputs[0].outPoint.txid}")
            println("Input Index: ${inputs[0].outPoint.index}")
            println("Input Amount: $selectedTxAmount")
            println("Output Amount: $outputAmount")

            when (signed) {
                is Either.Right -> println("Signed PSBT: ${signed.value.psbt}")
                is Either.Left -> println("Signing failed: ${signed.left}")
            }*/
// Print PSBT
         /*   println("PSBT (Base64): ${psbtBase64.sign(PrivateKey(ByteVector.fromHex(selectedPool?.privateKey ?: "")), getSelectedTxInfo()
                ?.second ?: 0).left}")*/
           /* val activePools = getPoolsStore().get()
                ?.sortedByDescending { it.timeout }
                ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }

            val selectedPool = activePools?.find { it.id == poolId }
            val poolAmount = selectedPool?.denomination ?: 0f
            val selectedTxAmount = _listUnspent.value?.find { it.txid == _selectedTxId.value }?.amount ?: 0.0
            val estimatedVByteSize = 100 * (selectedPool?.peers ?: 0)
            val estimatedBtcFee = ((selectedPool?.feeRate?.toFloat() ?: 0f) * estimatedVByteSize.toFloat()) / 100000000

            println("estimatedBtcFee: $estimatedBtcFee")

            val outputAmount = poolAmount - estimatedBtcFee

            val input = Input(
                txid = getSelectedTxInfo()?.first ?: "",
                vout = getSelectedTxInfo()?.second ?: 0
            )

            val listOfOutputAddresses = selectedPool?.peersData
                ?.filter { it.type == "output" }
                ?.map { it.address }

            val outputMap = listOfOutputAddresses?.associateWith { outputAmount } ?: emptyMap()

            println(listOf(input, outputMap))

// Constructing the PSBT request with raw inputs and outputs, not encoded JSON strings
            val psbtRequest = PsbtRequest(
                inputs = listOf(input),  // Passing raw object, not encoded string
                outputs = outputMap      // Passing raw output map
            )

            val inputsJson = json.encodeToJsonElement(psbtRequest.inputs)
            val outputsJson = json.encodeToJsonElement(psbtRequest.outputs)

            // Construct params as a JsonArray containing the raw data
            val params = JsonArray(listOf(inputsJson, outputsJson))

            val rpcRequestBody = RpcRequestBody(
                method = Methods.CREATE_PSBT.value,
                params = params  // Raw data, no JSON encoding here
            )

            val rawTx = httpClient.fetchNodeData<RpcResponse<String>>(rpcRequestBody)?.result

            val walletProcessPsbtParams = JsonArray(listOf(
                JsonPrimitive(rawTx),
                JsonPrimitive(true),
                JsonPrimitive("ALL|ANYONECANPAY")
            ))

            val rpcWalletProcessPsbtParamsRequestBody = RpcRequestBody(
                method = Methods.WALLET_PROCESS_PSBT.value,
                params = walletProcessPsbtParams  // Raw data, no JSON encoding here
            )

            val processPsbt = httpClient.fetchNodeData<RpcResponse<PsbtResponse>>(rpcWalletProcessPsbtParamsRequestBody)?.result


            println(rawTx)
            println(json.encodeToString(processPsbt))*/
           /* if (!((poolAmount * 100_000_000) + 500 <= selectedTxAmount &&
                    selectedTxAmount <= (poolAmount * 100_000_000) + 5000)
            ) {

                SnackbarController.showMessage("Error: Selected input value is not within the specified range for this pool " +
                    "(denomination: $poolAmount BTC)")
                println(
                    "Error: Selected input value is not within the specified range for this pool (denomination: ${poolAmount * 100_000_000}), selected input: ${_selectedTxId.value}"
                )
            }*/
        }
    }
}

fun getPrivateKeyFromString(privateKeyString: String): PrivateKey {
    return try {
        // First, try to parse as WIF
        val (_, privKey) = Base58Check.decode(privateKeyString)
        PrivateKey(privKey)
    } catch (e: Exception) {
        try {
            // If WIF fails, try to parse as hex
            PrivateKey(ByteVector32.fromValidHex(privateKeyString))
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid private key format")
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