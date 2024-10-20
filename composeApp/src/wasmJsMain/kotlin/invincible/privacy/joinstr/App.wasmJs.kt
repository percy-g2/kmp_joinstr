package invincible.privacy.joinstr

import invincible.privacy.joinstr.model.CoinJoinHistory
import invincible.privacy.joinstr.model.ListUnspentResponseItem
import invincible.privacy.joinstr.model.LocalPoolContent
import invincible.privacy.joinstr.utils.NodeConfig
import invincible.privacy.joinstr.utils.SettingsStore
import invincible.privacy.joinstr.utils.Theme
import io.github.aakira.napier.Napier
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.storage.storeOf
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.datetime.Clock
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.events.Event
import org.w3c.notifications.DENIED
import org.w3c.notifications.GRANTED
import org.w3c.notifications.Notification
import org.w3c.notifications.NotificationOptions
import org.w3c.notifications.NotificationPermission
import kotlin.js.Promise
import kotlin.time.Duration.Companion.seconds

actual object LocalNotification {
    actual fun showNotification(title: String, message: String) {
        Napier.v("Attempting to show notification")
        try {
            val options = NotificationOptions(
                body = message,
                icon = "icon-192.png",
                badge = "icon-192.png",
                silent = false,

            )
            val notification = Notification(title, options)
            Napier.v("Notification created:$notification")

            notification.onclick = { event: Event ->
                event.preventDefault() // Prevent the default action
                Napier.v("Notification clicked")
                try {
                    window.focus()
                    notification.close()
                } catch (e: Throwable) {
                    Napier.e("Error handling notification click:", e)
                }
            }

            notification.onerror = { error ->
                Napier.e("Error showing notification:$error")
            }
        } catch (e: Throwable) {
            Napier.e("Error creating notification:", e)
            // Fallback to alert if notification creation fails
            window.alert("Notification: $title\n$message")
        }
    }

    actual suspend fun requestPermission(): Boolean {
        return when (Notification.permission) {
            NotificationPermission.Companion.GRANTED -> true
            NotificationPermission.Companion.DENIED -> false
            else -> {
                val result = Notification.requestPermission().await<NotificationPermission>()
                result == NotificationPermission.Companion.GRANTED
            }
        }
    }
}

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

        byteArray.drop(1).take(32).toByteArray()
    } catch (e: Throwable) {
        println("Error in pubkeyCreate: ${e.message}")
        println("Stack trace: ${e.stackTraceToString()}")
        ByteArray(0) // Return empty array on error
    }
}

actual fun getSharedSecret(privateKey: ByteArray, pubKey: ByteArray): ByteArray {
    return try {
        println("Generating shared secret...")

        if (pubKey.isEmpty() || privateKey.isEmpty()) {
            throw IllegalArgumentException("Public key or private key cannot be empty")
        }

        // Ensure the public key is in the correct format (33 or 65 bytes)
        val adjustedPubKey = when (pubKey.size) {
            32 -> byteArrayOf(0x02.toByte()) + pubKey // Assuming compressed key, prefix with 0x02
            33, 65 -> pubKey // Already in correct format
            else -> throw IllegalArgumentException("Invalid public key length: expected 32, 33, or 65 bytes, got ${pubKey.size}")
        }

        if (privateKey.size != 32) {
            throw IllegalArgumentException("Invalid private key length: expected 32 bytes, got ${privateKey.size}")
        }

        // Convert ByteArray to Uint8Array
        val privateKeyUint8 = privateKey.toUint8Array()
        val pubKeyUint8 = adjustedPubKey.toUint8Array()

        // Generate shared secret
        val sharedSecretUint8 = Secp256k1.getSharedSecret(privateKeyUint8, pubKeyUint8)

        // Convert Uint8Array to ByteArray
        val sharedSecret = sharedSecretUint8.toByteArray()

        println("Shared secret: ${sharedSecret.toHexString()}")

        sharedSecret.copyOfRange(1,33)
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

actual fun getSettingsStore(): KStore<SettingsStore> {
    return storeOf<SettingsStore>(
        key = "settings",
        default = SettingsStore(
            selectedTheme = Theme.SYSTEM.id,
            nodeConfig = NodeConfig()
        )
    )
}

actual fun getPoolsStore(): KStore<List<LocalPoolContent>> {
    return storeOf<List<LocalPoolContent>>(
        key = "pools",
        default = emptyList()
    )
}

actual fun getHistoryStore(): KStore<List<CoinJoinHistory>> {
    return storeOf<List<CoinJoinHistory>>(
        key = "coin_join_history",
        default = emptyList()
    )
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
            level = LogLevel.NONE
        }
    }
}

actual suspend fun createPsbt(
    poolId: String,
    unspentItem: ListUnspentResponseItem
): String? {
    val activePools = getPoolsStore().get()
        ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }
        ?.sortedByDescending { it.timeout }

    val selectedPool = activePools?.find { it.id == poolId }
        ?: throw IllegalStateException("Selected pool not found")

    val poolAmount = selectedPool.denomination
    val selectedTxAmount = unspentItem.amount
    val estimatedVByteSize = 100 * selectedPool.peers
    val estimatedBtcFee = (selectedPool.feeRate.toFloat() * estimatedVByteSize.toFloat()) / 100000000

    val outputAmount = poolAmount - estimatedBtcFee


    // TODO

    return null
}

actual suspend fun joinPsbts(
    listOfPsbts: List<String>,
): Pair<String?, String?> {
    // TODO

    return Pair(null, null)
}

actual fun openLink(link: String) {
    window.open(link)
}

actual fun getPlatform(): Platform = Platform.WASM_JS

actual suspend fun connectVpn() {
}