package invincible.privacy.joinstr

import fr.acinq.secp256k1.Hex
import fr.acinq.secp256k1.Secp256k1
import fr.acinq.secp256k1.Secp256k1.Companion.pubKeyTweakMul
import invincible.privacy.joinstr.model.PoolContent
import invincible.privacy.joinstr.theme.NodeConfig
import invincible.privacy.joinstr.theme.Settings
import invincible.privacy.joinstr.theme.Theme
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDecimalNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import kotlin.time.Duration.Companion.seconds

actual fun getSettingsStore(): KStore<Settings> {
    val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    return storeOf<Settings>(
        file = "${paths.firstOrNull() as? String}/settings.json".toPath(),
        default = Settings(
            selectedTheme = Theme.SYSTEM.id,
            nodeConfig = NodeConfig()
        )
    )
}

actual fun getPoolsStore(): KStore<List<PoolContent>> {
    val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    return storeOf<List<PoolContent>>(
        file = "${paths.firstOrNull() as? String}/pools.json".toPath(),
        default = emptyList()
    )
}

actual fun Float.convertFloatExponentialToString(): String {
    val decimalNumber = NSDecimalNumber(string = toString())
    return decimalNumber.stringValue
}

actual fun getWebSocketClient(): HttpClient {
    return HttpClient(Darwin) {
        install(WebSockets)

        install(HttpTimeout) {
            requestTimeoutMillis = 5.seconds.inWholeMilliseconds
            connectTimeoutMillis = 5.seconds.inWholeMilliseconds
            socketTimeoutMillis = 5.seconds.inWholeMilliseconds
        }

        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
    }
}

actual fun pubkeyCreate(privateKey: ByteArray): ByteArray {
    if (!Secp256k1.secKeyVerify(privateKey)) throw Exception("Invalid private key!")
    val pubKey = Secp256k1.pubkeyCreate(privateKey).drop(1).take(32).toByteArray()
    //context.cleanup()
    return pubKey
}

actual suspend fun signSchnorr(content: ByteArray, privateKey: ByteArray, freshRandomBytes: ByteArray): ByteArray {
    val contentSignature = Secp256k1.signSchnorr(content, privateKey, freshRandomBytes)
    return contentSignature
}

actual fun getSharedSecret(privateKey: ByteArray, pubKey: ByteArray): ByteArray =
    pubKeyTweakMul(Hex.decode("02") + pubKey, privateKey).copyOfRange(1, 33)