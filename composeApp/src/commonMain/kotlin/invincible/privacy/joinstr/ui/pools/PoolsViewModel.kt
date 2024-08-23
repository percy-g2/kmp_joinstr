package invincible.privacy.joinstr.ui.pools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import invincible.privacy.joinstr.network.NostrClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PoolsViewModel : ViewModel() {
    private val nostrClient = NostrClient()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()


    fun createPool() {
        viewModelScope.launch {
            _isLoading.value = true
            // TODO
        }
    }
}