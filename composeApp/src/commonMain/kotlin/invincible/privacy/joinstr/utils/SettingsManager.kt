package invincible.privacy.joinstr.utils

import invincible.privacy.joinstr.getSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable


object SettingsManager {
    val themeState = MutableStateFlow(Theme.SYSTEM.id)
    val store = getSettingsStore()

    suspend fun updateTheme(newTheme: Int) {
        themeState.value = newTheme
        store.update { it?.copy(selectedTheme = newTheme) ?: SettingsStore(selectedTheme = newTheme, nodeConfig = NodeConfig()) }
    }

    suspend fun updateNodeConfig(
        nodeConfig: NodeConfig,
        nostrRelay: String
    ) {
        store.set(
            SettingsStore(
                selectedTheme = themeState.value,
                nodeConfig = nodeConfig,
                nostrRelay = nostrRelay
            )
        )
    }
}

@Serializable
data class SettingsStore(
    val selectedTheme: Int = Theme.SYSTEM.id,
    val nodeConfig: NodeConfig = NodeConfig(),
    val nostrRelay: String = "wss://nostr.fmt.wiz.biz"
)

@Serializable
data class NodeConfig(
    val url: String = "http://192.168.1.2",
    val userName: String = "user",
    val password: String = "pass",
    val port: Int = 38332
)

enum class Theme(val id: Int, val title: String, val description: String? = null) {
    SYSTEM(0, "Use Device Settings", "When selected, the Day or Night mode will align with the device's settings."),
    LIGHT(1, "Light"),
    DARK(2, "Dark");
}