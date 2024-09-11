package invincible.privacy.joinstr.ui.pools

import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import invincible.privacy.joinstr.getPoolsStore
import invincible.privacy.joinstr.getSharedSecret
import invincible.privacy.joinstr.ktx.hexToByteArray
import invincible.privacy.joinstr.ktx.toHexString
import invincible.privacy.joinstr.model.Credentials
import invincible.privacy.joinstr.model.LocalPoolContent
import invincible.privacy.joinstr.model.Methods
import invincible.privacy.joinstr.model.PoolContent
import invincible.privacy.joinstr.model.RegisterAddress
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.model.RpcResponse
import invincible.privacy.joinstr.network.HttpClient
import invincible.privacy.joinstr.network.NostrClient
import invincible.privacy.joinstr.network.json
import invincible.privacy.joinstr.ui.components.SnackbarController
import invincible.privacy.joinstr.utils.Event
import invincible.privacy.joinstr.utils.NostrCryptoUtils.createEvent
import invincible.privacy.joinstr.utils.NostrCryptoUtils.decrypt
import invincible.privacy.joinstr.utils.NostrCryptoUtils.encrypt
import invincible.privacy.joinstr.utils.NostrCryptoUtils.generatePrivateKey
import invincible.privacy.joinstr.utils.NostrCryptoUtils.getPublicKey
import invincible.privacy.joinstr.utils.SettingsManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.time.Duration.Companion.seconds

@OptIn(DelicateCoroutinesApi::class)
class PoolsViewModel : ViewModel() {
    private val nostrClient = NostrClient()
    private val httpClient = HttpClient()
    private val poolStore = getPoolsStore()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _otherPoolEvents = MutableStateFlow<List<PoolContent>?>(null)
    val otherPoolEvents: StateFlow<List<PoolContent>?> = _otherPoolEvents.asStateFlow()

    private val _localPools = MutableStateFlow<List<LocalPoolContent>?>(null)
    val localPools: StateFlow<List<LocalPoolContent>?> = _localPools.asStateFlow()

    private val _activePoolReady = MutableStateFlow(Pair(false, ""))
    val activePoolReady: StateFlow<Pair<Boolean,String>> = _activePoolReady.asStateFlow()

    fun startInitialChecks() {
        GlobalScope.launch {
            nostrClient.activePoolsCredentialsSender()
        }
        GlobalScope.launch {
            runCatching {
                checkForReadyActivePools()
            }.getOrElse {
                it.printStackTrace()
            }
        }
    }

    private suspend fun checkForReadyActivePools() = withContext(Dispatchers.Default){
        while (isActive) {
            val activePools = getPoolsStore().get()
                ?.sortedByDescending { it.timeout }
                ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }
            _activePoolReady.value = activePools?.find { it.peersData.size == it.peers }?.id?.let { Pair(true, it)} ?: Pair(false, "")
            // Delay for 30 seconds
            delay(30.seconds)
        }
    }


    private fun generatePoolId(): String {
        val letters = ('a'..'z').toList()
        val randomString = (1..10)
            .map { letters.random() }
            .joinToString("")
        val timestamp = Clock.System.now().toEpochMilliseconds() / 1000
        return randomString + timestamp.toString()
    }

    fun fetchLocalPools() {
        viewModelScope.launch {
            _isLoading.value = true
            _localPools.value = poolStore.get()
                ?.sortedByDescending { it.timeout }
                ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }
            _isLoading.value = false
        }
    }

    fun fetchOtherPools() {
        viewModelScope.launch {
            _isLoading.value = true
            _otherPoolEvents.value = null
            nostrClient.fetchOtherPools(
                onSuccess = { nostrEvents ->
                    viewModelScope.launch {
                        val currentTime = (Clock.System.now().toEpochMilliseconds() / 1000)
                        val pools = getPoolsStore().get()
                        val activePoolsIds = pools?.filter { it.timeout > currentTime }?.map { it.id }
                        _otherPoolEvents.value = nostrEvents
                            .sortedByDescending { it.timeout }
                            .filter { it.timeout > currentTime && it.id !in activePoolsIds.orEmpty() }
                        _isLoading.value = false
                    }
                },
                onError = { error ->
                    _isLoading.value = false
                    val msg = error ?: "Something went wrong while communicating with the relay.\nPlease try again."
                    SnackbarController.showMessage(msg)
                }
            )
        }
    }

    fun createPool(
        denomination: String,
        peers: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                _isLoading.value = true
                SettingsManager.store.get()?.nostrRelay?.let { nostrRelay ->
                    httpClient.fetchHourFee()?.let { hourFee ->
                        val params = JsonArray(listOf(
                            JsonPrimitive("coin_join"),
                            JsonPrimitive("bech32")
                        ))
                        val addressBody = RpcRequestBody(
                            method = Methods.NEW_ADDRESS.value,
                            params = params
                        )
                        httpClient.fetchNodeData<RpcResponse<String>>(addressBody)?.result?.let { address ->
                            val privateKey = generatePrivateKey()
                            val publicKey = getPublicKey(privateKey)
                            val poolId = generatePoolId()
                            val timeout = (Clock.System.now().toEpochMilliseconds() / 1000) + (600 * 6)
                            val poolContent = PoolContent(
                                id = poolId,
                                type = "new_pool",
                                peers = peers.toInt(),
                                denomination = denomination.toFloat(),
                                relay = nostrRelay,
                                feeRate = hourFee,
                                timeout = timeout,
                                publicKey = publicKey.toHexString()
                            )
                            val content = nostrClient.json.encodeToString(poolContent)
                            val nostrEvent = createEvent(
                                content = content,
                                event = Event.TEST_JOIN_STR,
                                privateKey = privateKey,
                                publicKey = publicKey
                            )
                            nostrClient.sendEvent(
                                event = nostrEvent,
                                onSuccess = {
                                    viewModelScope.launch {
                                        poolStore.update {
                                            val pool = LocalPoolContent(
                                                id = poolId,
                                                type = "new_pool",
                                                peers = peers.toInt(),
                                                denomination = denomination.toFloat(),
                                                relay = nostrRelay,
                                                feeRate = hourFee,
                                                timeout = timeout,
                                                publicKey = publicKey.toHexString(),
                                                privateKey = privateKey.toHexString()
                                            )
                                            it?.plus(pool) ?: listOf(pool)
                                        }
                                    }
                                    onSuccess.invoke()
                                    _isLoading.value = false
                                    SnackbarController.showMessage("New pool created!\nEvent ID: ${nostrEvent.id}")
                                    registerOutput(
                                        address = address,
                                        publicKey = publicKey,
                                        privateKey = privateKey
                                    )
                                },
                                onError = { error ->
                                    _isLoading.value = false
                                    val msg = error ?: "Something went wrong while communicating with the relay.\nPlease try again."
                                    SnackbarController.showMessage(msg)
                                }
                            )
                        } ?: run {
                            _isLoading.value = false
                            SnackbarController.showMessage("Unable to generate new address.\nPlease try again.")
                        }
                    } ?: run {
                        _isLoading.value = false
                        SnackbarController.showMessage("Failed to retrieve current hour fee rate.\nPlease check your connection.")
                    }
                } ?: run {
                    _isLoading.value = false
                    SnackbarController.showMessage("Nostr relay settings missing.\nPlease configure in Settings.")
                }
            }.getOrElse {
                _isLoading.value = false
                val msg = it.message ?: "Something went wrong!\nPlease try again."
                SnackbarController.showMessage(msg)
            }
        }
    }

    fun checkRegisteredOutputs(
        poolId: String,
        publicKey: ByteArray,
        privateKey: ByteArray,
        showWaitingDialog: MutableState<Boolean>
    ) {
        viewModelScope.launch {
            nostrClient.checkRegisteredOutputs(
                publicKey = publicKey,
                privateKey = privateKey,
                onSuccess = { registeredAddressList ->
                    viewModelScope.launch {
                        getPoolsStore().update { existingPools ->
                            existingPools?.map {
                                if (it.id == poolId) {
                                    it.copy(peersData = registeredAddressList)
                                } else it
                            } ?: emptyList()
                        }
                    }
                },
                onError = { error ->
                    val msg = error ?: "Something went wrong while communicating with the relay.\nPlease try again."
                    SnackbarController.showMessage(msg)
                    showWaitingDialog.value = false
                }
            )
        }
    }

    fun joinRequest(
        publicKey: ByteArray,
        privateKey: ByteArray,
        poolPublicKey: String,
        showJoinDialog: MutableState<Boolean>,
        onSuccess: (LocalPoolContent) -> Unit
    ) {
        viewModelScope.launch {
            showJoinDialog.value = true
            val jsonObject = buildJsonObject {
                put("type", JsonPrimitive("join_pool"))
            }
            val data = json.encodeToString(JsonObject.serializer(), jsonObject)
            val sharedSecret = getSharedSecret(privateKey, poolPublicKey.hexToByteArray())
            val encryptedMessage = encrypt(data, sharedSecret)
            val nostrEvent = createEvent(
                content = encryptedMessage,
                event = Event.ENCRYPTED_DIRECT_MESSAGE,
                privateKey = privateKey,
                publicKey = publicKey,
                tagPubKey = poolPublicKey
            )
            nostrClient.sendEvent(
                event = nostrEvent,
                onSuccess = {
                    SnackbarController.showMessage("Join request sent.\nEvent ID: ${nostrEvent.id}")
                    viewModelScope.launch {
                        nostrClient.requestPoolCredentials(
                            requestPublicKey = publicKey.toHexString(),
                            onSuccess = { eventWithCredentials ->
                                viewModelScope.launch {
                                    runCatching {
                                        val params = JsonArray(listOf(
                                            JsonPrimitive("coin_join"),
                                            JsonPrimitive("bech32")
                                        ))
                                        val addressBody = RpcRequestBody(
                                            method = Methods.NEW_ADDRESS.value,
                                            params = params
                                        )
                                        httpClient.fetchNodeData<RpcResponse<String>>(addressBody)?.result?.let { address ->
                                            val decryptedContent = decrypt(eventWithCredentials.content, sharedSecret)
                                            val credentials = json.decodeFromString<Credentials>(decryptedContent)
                                            val pool = LocalPoolContent(
                                                id = credentials.id,
                                                type = "new_pool",
                                                peers = credentials.peers,
                                                denomination = credentials.denomination,
                                                relay = credentials.relay,
                                                feeRate = credentials.feeRate,
                                                timeout = credentials.timeout,
                                                publicKey = credentials.publicKey,
                                                privateKey = credentials.privateKey
                                            )
                                            poolStore.update {
                                                it?.plus(pool) ?: listOf(pool)
                                            }
                                            showJoinDialog.value = false
                                            SnackbarController.showMessage("Credentials have been received and securely saved!")

                                            registerOutput(
                                                address = address,
                                                publicKey = credentials.publicKey.hexToByteArray(),
                                                privateKey = credentials.privateKey.hexToByteArray(),
                                                onSuccess = {
                                                    onSuccess.invoke(pool)
                                                }
                                            )
                                        } ?: run {
                                            showJoinDialog.value = false
                                            SnackbarController.showMessage("Unable to generate new address.\nPlease try again.")
                                        }
                                    }.getOrElse {
                                        showJoinDialog.value = false
                                        SnackbarController.showMessage("Something went wrong!\nPlease try again.")
                                    }
                                }
                            },
                            onError = { error ->
                                val msg = error ?: "Something went wrong while communicating with the relay.\nPlease try again."
                                SnackbarController.showMessage(msg)
                                showJoinDialog.value = false
                            }
                        )
                    }
                },
                onError = { error ->
                    val msg = error ?: "Something went wrong while communicating with the relay.\nPlease try again."
                    SnackbarController.showMessage(msg)
                }
            )
        }
    }

    private fun registerOutput(
        address: String,
        privateKey: ByteArray,
        publicKey: ByteArray,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            if (onSuccess == null) _isLoading.value = true
            val registerOutput = RegisterAddress(
                address = address,
                type = "output"
            )
            val data = json.encodeToString(registerOutput)
            val sharedSecret = getSharedSecret(privateKey, publicKey)
            val encryptedMessage = encrypt(data, sharedSecret)
            val nostrEvent = createEvent(
                content = encryptedMessage,
                event = Event.ENCRYPTED_DIRECT_MESSAGE,
                privateKey = privateKey,
                publicKey = publicKey,
                tagPubKey = publicKey.toHexString()
            )
            nostrClient.sendEvent(
                event = nostrEvent,
                onSuccess = {
                    if (onSuccess == null) _isLoading.value = false
                    onSuccess?.invoke()
                    SnackbarController.showMessage("Output registered for coinjoin.\nEvent ID: ${nostrEvent.id}")
                },
                onError = { error ->
                    if (onSuccess == null) _isLoading.value = false
                    val msg = error ?: "Something went wrong while communicating with the relay.\nPlease try again."
                    SnackbarController.showMessage(msg)
                }
            )
        }
    }

    fun removeLocalPool(id: String) {
        viewModelScope.launch {
            _localPools.value = _localPools.value?.filter { it.id != id }
        }
    }

    fun removeOtherPool(id: String) {
        viewModelScope.launch {
            _otherPoolEvents.value = _otherPoolEvents.value?.filter { it.id != id }
        }
    }


    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            nostrClient.close()
        }
    }
}