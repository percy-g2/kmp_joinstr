package invincible.privacy.joinstr.ui.pools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import invincible.privacy.joinstr.getPoolsStore
import invincible.privacy.joinstr.getSharedSecret
import invincible.privacy.joinstr.ktx.toHexString
import invincible.privacy.joinstr.model.Methods
import invincible.privacy.joinstr.model.NostrEvent
import invincible.privacy.joinstr.model.PoolContent
import invincible.privacy.joinstr.model.PoolCreationContent
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.model.RpcResponse
import invincible.privacy.joinstr.network.HttpClient
import invincible.privacy.joinstr.network.NostrClient
import invincible.privacy.joinstr.network.json
import invincible.privacy.joinstr.utils.SettingsManager
import invincible.privacy.joinstr.ui.components.SnackbarController
import invincible.privacy.joinstr.utils.Event
import invincible.privacy.joinstr.utils.NostrCryptoUtils.createEvent
import invincible.privacy.joinstr.utils.NostrCryptoUtils.encrypt
import invincible.privacy.joinstr.utils.NostrCryptoUtils.generatePrivateKey
import invincible.privacy.joinstr.utils.NostrCryptoUtils.getPublicKey
import io.ktor.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.random.Random

class PoolsViewModel : ViewModel() {
    private val nostrClient = NostrClient()
    private val httpClient = HttpClient()
    private val poolStore = getPoolsStore()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = MutableStateFlow<List<NostrEvent>?>(null)
    val events: StateFlow<List<NostrEvent>?> = _events

    private val _localPools = MutableStateFlow<List<PoolContent>?>(null)
    val localPools: StateFlow<List<PoolContent>?> = _localPools


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
            _localPools.value = poolStore.get()?.sortedByDescending { it.timeout }
            //    ?.map { it.copy(timeout = (Clock.System.now().toEpochMilliseconds() / 1000) + Random.nextInt(0, 601)) }
                ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }
            _isLoading.value = false
        }
    }

    fun fetchOtherPools() {
        viewModelScope.launch {
            _isLoading.value = true
            _events.value = null
            nostrClient.fetchOtherPools(
                onSuccess = { nostrEvents ->
                    _events.value = nostrEvents.sortedByDescending { it.createdAt }.filter {
                        json.decodeFromString<PoolCreationContent>(it.content).timeout > (Clock.System.now().toEpochMilliseconds() / 1000)
                    }
                    _isLoading.value = false
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
                        val addressBody = RpcRequestBody(
                            method = Methods.NEW_ADDRESS.value,
                            params = listOf("coin_join", "bech32")
                        )
                        httpClient.fetchNodeData<RpcResponse<String>>(addressBody)?.result?.let { address ->
                            val privateKey = generatePrivateKey()
                            val publicKey = if (PlatformUtils.IS_BROWSER) {
                                getPublicKey(privateKey).drop(1).take(32).toByteArray()
                            } else getPublicKey(privateKey)
                            val poolCreationContent = PoolCreationContent(
                                id = generatePoolId(),
                                type = "new_pool",
                                peers = peers.toInt(),
                                denomination = denomination.toFloat(),
                                relay = nostrRelay,
                                feeRate = hourFee,
                                timeout = (Clock.System.now().toEpochMilliseconds() / 1000) + 600,
                                publicKey = publicKey.toHexString()
                            )
                            val content = nostrClient.json.encodeToString(poolCreationContent)
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
                                            val pool = PoolContent(
                                                id = generatePoolId(),
                                                type = "new_pool",
                                                peers = peers.toInt(),
                                                denomination = denomination.toFloat(),
                                                relay = nostrRelay,
                                                feeRate = hourFee,
                                                timeout = (Clock.System.now().toEpochMilliseconds() / 1000) + 600,
                                                publicKey = publicKey.toHexString(),
                                                privateKey = privateKey.toHexString()
                                            )
                                            it?.plus(pool) ?: listOf(pool)
                                        }
                                    }
                                    onSuccess.invoke()
                                    _isLoading.value = false
                                    SnackbarController.showMessage("New pool created\nEvent ID: ${nostrEvent.id}")
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
                val msg = it.message ?: "Something went wrong!.\nPlease try again."
                SnackbarController.showMessage(msg)
            }
        }
    }

    private fun registerOutput(
        address: String,
        privateKey: ByteArray,
        publicKey: ByteArray
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val jsonObject = buildJsonObject {
                put("address", JsonPrimitive(address))
                put("type", JsonPrimitive("output"))
            }
            val data = json.encodeToString(JsonObject.serializer(), jsonObject)
            val sharedSecret = getSharedSecret(privateKey, publicKey)
            val encryptedMessage = encrypt(data, sharedSecret)
            val nostrEvent = createEvent(
                content = encryptedMessage,
                event = Event.ENCRYPTED_DIRECT_MESSAGE,
                privateKey = privateKey,
                publicKey = publicKey
            )
            nostrClient.sendEvent(
                event = nostrEvent,
                onSuccess = {
                    _isLoading.value = false
                    SnackbarController.showMessage("Output registered for coinjoin.\nEvent ID: ${nostrEvent.id}")
                },
                onError = { error ->
                    _isLoading.value = false
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

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            nostrClient.close()
        }
    }
}