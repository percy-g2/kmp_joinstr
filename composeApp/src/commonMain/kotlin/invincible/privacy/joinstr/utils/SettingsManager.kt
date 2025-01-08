package invincible.privacy.joinstr.utils

import invincible.privacy.joinstr.getSettingsStore
import invincible.privacy.joinstr.model.VpnGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable


object SettingsManager {
    val themeState = MutableStateFlow(Theme.SYSTEM.id)
    val store = getSettingsStore()

    suspend fun updateTheme(newTheme: Int) {
        themeState.value = newTheme
        store.update { it?.copy(selectedTheme = newTheme) ?: SettingsStore(selectedTheme = newTheme, nodeConfig = NodeConfig()) }
    }

    suspend fun updateSettings(
        vpnGateway: VpnGateway?,
        nodeConfig: NodeConfig,
        nostrRelay: String,
    ) {
        store.update {
            it?.copy(
                vpnGateway = vpnGateway,
                selectedTheme = themeState.value,
                nodeConfig = nodeConfig,
                nostrRelay = nostrRelay
            )
        }
    }
}

@Serializable
data class SettingsStore(
    val selectedTheme: Int = Theme.SYSTEM.id,
    val nodeConfig: NodeConfig = NodeConfig(),
    val nostrRelay: String? = null,
    val vpnGateway: VpnGateway? = null
)

@Serializable
data class NodeConfig(
    val url: String? = null,
    val userName: String? = null,
    val password: String? = null,
    val port: Int? = null,
    val selectedWallet: String? = null
)

enum class Theme(val id: Int, val title: String, val description: String? = null) {
    SYSTEM(0, "Use Device Settings", "When selected, the Day or Night mode will align with the device's settings."),
    LIGHT(1, "Light"),
    DARK(2, "Dark");
}