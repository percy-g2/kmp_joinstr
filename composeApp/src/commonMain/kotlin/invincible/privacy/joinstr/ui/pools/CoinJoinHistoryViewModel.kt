package invincible.privacy.joinstr.ui.pools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import invincible.privacy.joinstr.getHistoryStore
import invincible.privacy.joinstr.model.CoinJoinHistory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class CoinJoinHistoryViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _coinJoinHistory = MutableStateFlow<List<CoinJoinHistory>?>(null)
    val coinJoinHistory: StateFlow<List<CoinJoinHistory>?> = _coinJoinHistory.asStateFlow()

    fun fetchCoinJoinHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(2.seconds)
            _coinJoinHistory.value = getHistoryStore().get()?.sortedByDescending { it.timestamp }
            _isLoading.value = false
        }
    }
}