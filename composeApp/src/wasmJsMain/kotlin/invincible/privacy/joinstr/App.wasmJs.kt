package invincible.privacy.joinstr

import invincible.privacy.joinstr.model.PoolContent
import invincible.privacy.joinstr.theme.NodeConfig
import invincible.privacy.joinstr.theme.Settings
import invincible.privacy.joinstr.theme.Theme
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.storage.storeOf
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.await
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.js.Promise
import kotlin.time.Duration.Companion.seconds

@JsModule("@noble/secp256k1")
external object Secp256k1 {
    val schnorr: Schnorr
    fun getPublicKey(privateKey: Uint8Array, compressed: Boolean = definedExternally): Uint8Array
    fun getSharedSecret(
        privateKeyA: Uint8Array,
        publicKeyB: Uint8Array,
        isCompressed: Boolean = definedExternally
    ): Uint8Array
}

external interface Schnorr {
    fun sign(message: Uint8Array, privateKey: Uint8Array, randomBytes: Uint8Array? = definedExternally): Promise<Uint8Array>
}

actual suspend fun signSchnorr(content: ByteArray, privateKey: ByteArray, freshRandomBytes: ByteArray): ByteArray {
    val uint8Content = content.toUint8Array()
    val uint8PrivateKey = privateKey.toUint8Array()
    val uint8RandomBytes = freshRandomBytes.toUint8Array()

    println("Content: ${uint8Content.toHexString()}")
    println("Private key: ${uint8PrivateKey.toHexString()}")
    println("Random bytes: ${uint8RandomBytes.toHexString()}")

    return try {
        println("Attempting to sign with schnorr...")
        val uint8Signature = Secp256k1.schnorr.sign(uint8Content, uint8PrivateKey, uint8RandomBytes)

        val byteArray = uint8Signature.await<Uint8Array>().toByteArray()
        println("Raw signature: ${byteArray.toHexString()}")

        if (byteArray.isEmpty()) {
            throw Error("Empty signature")
        }

        byteArray
    } catch (e: Throwable) {
        println("Error in signSchnorr: ${e.message}")
        println("Stack trace: ${e.stackTraceToString()}")
        ByteArray(0) // Return empty array on error
    }
}

actual fun pubkeyCreate(privateKey: ByteArray): ByteArray {
    val uint8PrivateKey = privateKey.toUint8Array()

    return try {
        println("Generating public key...")
        val uint8PublicKey = Secp256k1.getPublicKey(uint8PrivateKey)

        val byteArray = uint8PublicKey.toByteArray()
        println("Public key: ${byteArray.toHexString()}")

        byteArray
    } catch (e: Throwable) {
        println("Error in pubkeyCreate: ${e.message}")
        println("Stack trace: ${e.stackTraceToString()}")
        ByteArray(0) // Return empty array on error
    }
}

actual fun getSharedSecret(privateKey: ByteArray, pubKey: ByteArray): ByteArray {
    val uint8PrivateKey = privateKey.toUint8Array()
    val uint8PubKey = pubKey.toUint8Array()

    return try {
        println("Generating shared secret...")
        val uint8SharedSecret = Secp256k1.getSharedSecret(uint8PrivateKey, uint8PubKey)

        val byteArray = uint8SharedSecret.toByteArray()
        println("Shared secret: ${byteArray.toHexString()}")

        byteArray.copyOfRange(1, 33)
    } catch (e: Throwable) {
        println("Error in getSharedSecret: ${e.message}")
        println("Stack trace: ${e.stackTraceToString()}")
        ByteArray(0) // Return empty array on error
    }
}

// Utility function to convert ByteArray to Uint8Array
fun ByteArray.toUint8Array(): Uint8Array {
    return Uint8Array(this.size).apply {
        this@toUint8Array.forEachIndexed { index, byte ->
            this[index] = byte
        }
    }
}

// Utility function to convert Uint8Array to ByteArray
fun Uint8Array.toByteArray(): ByteArray {
    return ByteArray(this.length) { index -> this[index] }
}

private fun Uint8Array.toHexString(): String {
    return this.toByteArray().toHexString()
}

private fun ByteArray.toHexString(): String {
    return this.joinToString("") {
        val hexChars = "0123456789ABCDEF"
        val i = it.toInt() and 0xFF
        "${hexChars[i shr 4]}${hexChars[i and 0x0F]}"
    }
}

actual fun getSettingsStore(): KStore<Settings> {
    return storeOf<Settings>(
        key = "settings",
        default = Settings(
            selectedTheme = Theme.SYSTEM.id,
            nodeConfig = NodeConfig()
        )
    )
}

actual fun getPoolsStore(): KStore<List<PoolContent>> {
    return storeOf<List<PoolContent>>(
        key = "pools",
        default = emptyList()
    )
}

actual fun Float.convertFloatExponentialToString(): String {
    return this.toString()
}

actual fun getWebSocketClient(): HttpClient {
    return HttpClient(Js) {
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