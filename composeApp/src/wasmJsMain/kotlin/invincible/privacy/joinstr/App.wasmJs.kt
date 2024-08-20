package invincible.privacy.joinstr

import invincible.privacy.joinstr.theme.NodeConfig
import invincible.privacy.joinstr.theme.Settings
import invincible.privacy.joinstr.theme.Theme
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.storage.storeOf

actual fun getKStore(): KStore<Settings> {
    return storeOf<Settings>(
        key = "settings",
        default = Settings(
            selectedTheme = Theme.SYSTEM.id,
            nodeConfig = NodeConfig()
        )
    )
}