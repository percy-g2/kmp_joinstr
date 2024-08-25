package invincible.privacy.joinstr.ui.pools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val nostrClient = NostrClient()
    private val httpClient = HttpClient()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()


    private fun generatePoolId(): String {
        val letters = ('a'..'z').toList()
        val randomString = (1..10)
            .map { letters.random() }
            .joinToString("")
        val timestamp = Clock.System.now().toEpochMilliseconds() / 1000
        return randomString + timestamp.toString()
    }

    fun createPool(
        denomination: String,
        peers: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            SettingsManager.store.get()?.nostrRelay?.let { nostrRelay ->
                val privateKey = generatePrivateKey()
                println(privateKey.toHexString())
                val publicKey = getPublicKey(privateKey)
                val poolCreationContent = PoolCreationContent(
                    id = generatePoolId(),
                    type = "new_pool",
                    peers = peers.toInt(),
                    denomination = denomination.toFloat(),
                    relay = nostrRelay,
                    feeRate = httpClient.fetchHourFee(),
                    timeout = (Clock.System.now().toEpochMilliseconds() / 1000) + 600,
                    publicKey = publicKey.toHexString()
                )
                val content = nostrClient.json.encodeToString(poolCreationContent).replace("\\\"", "\"")
                val nostrUtil = NostrUtil()
                val nostrEvent = nostrUtil.createEvent(content, Event.TEST_JOIN_STR)
                println("Event to be sent: $nostrEvent")
                NostrClient().sendEvent(
                    event = nostrEvent,
                    onSuccess = {
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