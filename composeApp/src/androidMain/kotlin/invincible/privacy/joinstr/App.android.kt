package invincible.privacy.joinstr

import invincible.privacy.joinstr.theme.NodeConfig
import invincible.privacy.joinstr.theme.Settings
import invincible.privacy.joinstr.theme.Theme
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import okio.Path.Companion.toPath

actual fun getKStore(): KStore<Settings> {
    val context = ContextProvider.getContext()
    return storeOf<Settings>(
        file = "${context.cacheDir?.absolutePath}/settings.json".toPath(),
        default = Settings(
            selectedTheme = Theme.SYSTEM.id,
            nodeConfig = NodeConfig()
        )
    )
}