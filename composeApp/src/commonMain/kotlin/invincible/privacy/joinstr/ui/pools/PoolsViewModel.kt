package invincible.privacy.joinstr.ui.pools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import invincible.privacy.joinstr.getPoolsStore
import invincible.privacy.joinstr.model.NostrEvent
import invincible.privacy.joinstr.model.PoolContent
import invincible.privacy.joinstr.model.PoolCreationContent
import invincible.privacy.joinstr.network.HttpClient
import invincible.privacy.joinstr.network.NostrClient
import invincible.privacy.joinstr.theme.SettingsManager
import invincible.privacy.joinstr.ui.components.SnackbarController
import invincible.privacy.joinstr.utils.CryptoUtils.generatePrivateKey
import invincible.privacy.joinstr.utils.CryptoUtils.getPublicKey
import invincible.privacy.joinstr.utils.Event
import invincible.privacy.joinstr.utils.NostrUtil
import invincible.privacy.joinstr.utils.toHexString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString

class PoolsViewModel : ViewModel() {
    private val nostrClient = lazy { NostrClient() }
    private val httpClient = lazy { HttpClient() }
    private val poolStore = lazy { getPoolsStore() }

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
            _localPools.value = poolStore.value.get()?.sortedByDescending { it.timeout }
            _isLoading.value = false
        }
    }

    fun fetchOtherPools() {
        viewModelScope.launch {
            _isLoading.value = true
            _events.value = null
            nostrClient.value.fetchOtherPools { nostrEvents ->
                _events.value = nostrEvents?.sortedByDescending { it.createdAt }
                _isLoading.value = false
            }
        }
    }

    fun createPool(
        denomination: String,
        peers: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            SettingsManager.store.get()?.nostrRelay?.let { nostrRelay ->
                val privateKey = generatePrivateKey()
                val publicKey = getPublicKey(privateKey)
                val hourFee = httpClient.value.fetchHourFee()
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
                val content = nostrClient.value.json.encodeToString(poolCreationContent).replace("\\\"", "\"")
                val nostrUtil = NostrUtil()
                val nostrEvent = nostrUtil.createEvent(content, Event.TEST_JOIN_STR)
                NostrClient().sendEvent(
                    event = nostrEvent,
                    onSuccess = {
                        viewModelScope.launch {
                            poolStore.value.update {
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
                    },
                    onError = {
                        _isLoading.value = false
                    }
                )
            }
        }
    }
}