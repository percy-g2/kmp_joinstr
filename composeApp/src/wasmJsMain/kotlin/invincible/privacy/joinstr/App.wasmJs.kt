package invincible.privacy.joinstr

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.webcrypto.WebCrypto
import invincible.privacy.joinstr.theme.NodeConfig
import invincible.privacy.joinstr.theme.Settings
import invincible.privacy.joinstr.theme.Theme
import invincible.privacy.joinstr.utils.CryptoUtils
import io.eqoty.kryptools.Secp256k1
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

actual fun getCryptoProvider(): CryptographyProvider {
    return CryptographyProvider.WebCrypto
}

@OptIn(ExperimentalUnsignedTypes::class)
fun getPublicKey(): ByteArray {
    val privateKey = CryptoUtils.generatePrivateKey()
    return Secp256k1.makeKeypair(privateKey.toUByteArray()).pubkey.toByteArray()
}

@OptIn(ExperimentalUnsignedTypes::class)
fun signContent(privateKey: ByteArray, content: ByteArray): ByteArray {
    return Secp256k1.createSignature(content.toUByteArray(),privateKey.toUByteArray()).s.toByteArray()
}
