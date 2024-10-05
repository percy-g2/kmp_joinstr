package invincible.privacy.joinstr.ui.pools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import invincible.privacy.joinstr.getHistoryStore
import invincible.privacy.joinstr.getPoolsStore
import invincible.privacy.joinstr.model.LocalPoolContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

class MyPoolsViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _localPools = MutableStateFlow<List<LocalPoolContent>?>(null)
    val localPools: StateFlow<List<LocalPoolContent>?> = _localPools.asStateFlow()

    fun fetchLocalPools() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(2.seconds)
            _localPools.value = getPoolsStore().get()
                ?.sortedByDescending { it.timeout }
                ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }
                ?.filter { getHistoryStore().get()?.map { it.privateKey }?.contains(it.privateKey)?.not() == true }
            _isLoading.value = false
        }
    }

    fun removeLocalPool(id: String) {
        viewModelScope.launch {
            _localPools.value = _localPools.value?.filter { it.id != id }
        }
    }
}