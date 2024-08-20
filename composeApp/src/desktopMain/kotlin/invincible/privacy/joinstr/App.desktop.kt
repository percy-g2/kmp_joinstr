package invincible.privacy.joinstr

import invincible.privacy.joinstr.theme.NodeConfig
import invincible.privacy.joinstr.theme.Settings
import invincible.privacy.joinstr.theme.Theme
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import net.harawata.appdirs.AppDirsFactory
import okio.Path.Companion.toPath
import java.io.File

actual fun getKStore(): KStore<Settings> {
    val directory = AppDirsFactory.getInstance()
        .getUserDataDir("invincible.privacy.joinstr", "1.0.0", "invincible")
    if (File(directory).exists().not()) {
        File(directory).mkdirs()
    }
    return storeOf<Settings>(
        file = "${directory}/settings.json".toPath(),
        default = Settings(
            selectedTheme = Theme.SYSTEM.id,
            nodeConfig = NodeConfig()
        )
    )
}