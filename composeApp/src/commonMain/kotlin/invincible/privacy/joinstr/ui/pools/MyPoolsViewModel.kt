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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class MyPoolsViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _localPools = MutableStateFlow<List<LocalPoolContent>?>(null)
    val localPools: StateFlow<List<LocalPoolContent>?> = _localPools.asStateFlow()

    @OptIn(ExperimentalTime::class)
    fun fetchLocalPools() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(2.seconds)
            _localPools.value = getPoolsStore().get()
                ?.sortedByDescending { it.timeout }
                ?.filter { it.timeout > (kotlin.time.Clock.System.now().toEpochMilliseconds() / 1000) }
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