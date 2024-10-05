package invincible.privacy.joinstr.ui.pools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import invincible.privacy.joinstr.getHistoryStore
import invincible.privacy.joinstr.getPoolsStore
import invincible.privacy.joinstr.model.PoolContent
import invincible.privacy.joinstr.network.NostrClient
import invincible.privacy.joinstr.ui.components.SnackbarController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class OtherPoolViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _otherPoolEvents = MutableStateFlow<List<PoolContent>?>(null)
    val otherPoolEvents: StateFlow<List<PoolContent>?> = _otherPoolEvents.asStateFlow()

    fun fetchOtherPools() {
        viewModelScope.launch {
            _isLoading.value = true
            _otherPoolEvents.value = null
            NostrClient().fetchOtherPools(
                onSuccess = { nostrEvents ->
                    viewModelScope.launch {
                        val currentTime = (Clock.System.now().toEpochMilliseconds() / 1000)
                        val pools = getPoolsStore().get()
                        _otherPoolEvents.value = nostrEvents
                            .sortedByDescending { it.timeout }
                            .filter { it.timeout > currentTime && it.id !in pools?.map { pool -> pool.id }.orEmpty() }
                            .filter { getHistoryStore().get()?.map { it.privateKey }?.contains(it.privateKey)?.not() == true }
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

    fun removeOtherPool(id: String) {
        viewModelScope.launch {
            _otherPoolEvents.value = _otherPoolEvents.value?.filter { it.id != id }
        }
    }
}