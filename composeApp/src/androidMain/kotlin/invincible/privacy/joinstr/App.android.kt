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
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import okio.Path.Companion.toPath
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.time.Duration.Companion.seconds

actual fun getSettingsStore(): KStore<Settings> {
    val context = ContextProvider.getContext()
    return storeOf<Settings>(
        file = "${context.cacheDir?.absolutePath}/settings.json".toPath(),
        default = Settings(
            selectedTheme = Theme.SYSTEM.id,
            nodeConfig = NodeConfig()
        )
    )
}

actual fun getPoolsStore(): KStore<List<PoolContent>> {
    val context = ContextProvider.getContext()
    return storeOf<List<PoolContent>>(
        file = "${context.cacheDir?.absolutePath}/pools.json".toPath(),
        default = emptyList()
    )
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

actual fun Float.convertFloatExponentialToString(): String {
    val mathContext = MathContext(8, RoundingMode.HALF_UP)
    return BigDecimal(toString()).round(mathContext).toPlainString()
}

actual fun getWebSocketClient(): HttpClient {
    return HttpClient(CIO) {
        install(WebSockets)

        install(HttpTimeout) {
            requestTimeoutMillis = 5.seconds.inWholeMilliseconds
            connectTimeoutMillis = 5.seconds.inWholeMilliseconds
            socketTimeoutMillis = 5.seconds.inWholeMilliseconds
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }
}


actual fun getSharedSecret(privateKey: ByteArray, pubKey: ByteArray): ByteArray =
    pubKeyTweakMul(Hex.decode("02") + pubKey, privateKey).copyOfRange(1, 33)
