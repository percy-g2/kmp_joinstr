package invincible.privacy.joinstr

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.apple.Apple
import invincible.privacy.joinstr.theme.NodeConfig
import invincible.privacy.joinstr.theme.Settings
import invincible.privacy.joinstr.theme.Theme
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun getKStore(): KStore<Settings> {
    val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    return storeOf<Settings>(
        file = "${paths.firstOrNull() as? String}/settings.json".toPath(),
        default = Settings(
            selectedTheme = Theme.SYSTEM.id,
            nodeConfig = NodeConfig()
        )
    )
}

actual fun getCryptoProvider(): CryptographyProvider {
    return CryptographyProvider.Apple
}