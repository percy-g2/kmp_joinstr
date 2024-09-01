package invincible.privacy.joinstr

import fr.acinq.secp256k1.Hex
import fr.acinq.secp256k1.Secp256k1
import fr.acinq.secp256k1.Secp256k1.Companion.pubKeyTweakMul
import invincible.privacy.joinstr.model.LocalPoolContent
import invincible.privacy.joinstr.utils.NodeConfig
import invincible.privacy.joinstr.utils.Settings
import invincible.privacy.joinstr.utils.Theme
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import net.harawata.appdirs.AppDirsFactory
import okio.Path.Companion.toPath
import java.io.File
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.time.Duration.Companion.seconds

actual fun getSettingsStore(): KStore<Settings> {
    val directory = AppDirsFactory.getInstance()
        .getUserDataDir("invincible.privacy.joinstr", "1.0.0", "invincible")
    if (File(directory).exists().not()) {
        File(directory).mkdirs()
    }
    return storeOf<Settings>(
        file = "${directory}/settings.json".toPath(),
        default = Settings(
            selectedTheme = Theme.SYSTEM.id,
            nodeConfig = NodeConfig(),
            nostrRelay = "wss://nostr.fmt.wiz.biz"
        )
    )
}

actual fun getPoolsStore(): KStore<List<LocalPoolContent>> {
    val directory = AppDirsFactory.getInstance()
        .getUserDataDir("invincible.privacy.joinstr", "1.0.0", "invincible")
    if (File(directory).exists().not()) {
        File(directory).mkdirs()
    }
    return storeOf<List<LocalPoolContent>>(
        file = "${directory}/pools.json".toPath(),
        default = emptyList()
    )
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
