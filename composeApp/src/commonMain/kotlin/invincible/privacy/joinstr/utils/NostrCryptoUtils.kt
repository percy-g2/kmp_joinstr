package invincible.privacy.joinstr.utils

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.symmetric.AES
import dev.whyoleg.cryptography.random.CryptographyRandom
import invincible.privacy.joinstr.ktx.hexToByteArray
import invincible.privacy.joinstr.ktx.toHexString
import invincible.privacy.joinstr.model.NostrEvent
import invincible.privacy.joinstr.network.json
import invincible.privacy.joinstr.pubkeyCreate
import invincible.privacy.joinstr.signSchnorr
import io.ktor.utils.io.core.*
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import okio.ByteString.Companion.encodeUtf8
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object NostrCryptoUtils {
    // CryptoUtils functions
    fun generatePrivateKey(): ByteArray {
        val secretKey = ByteArray(32)
        CryptographyRandom.nextBytes(secretKey)
        return secretKey
    }

    fun getPublicKey(privateKey: ByteArray): ByteArray {
        return pubkeyCreate(privateKey)
    }

    private fun sha256Hash(content: String): String {
        val byteString = content.encodeUtf8()
        val hash = byteString.sha256()
        return hash.hex()
    }

    private suspend fun signContent(content: ByteArray, privateKey: ByteArray): ByteArray {
        val freshRandomBytes = ByteArray(32)
        CryptographyRandom.nextBytes(freshRandomBytes)
        return signSchnorr(content, privateKey, freshRandomBytes)
    }

    @OptIn(ExperimentalEncodingApi::class, DelicateCryptographyApi::class)
    suspend fun decrypt(message: String, sharedSecret: ByteArray): String {
        val parts = message.split("?iv=")
        if (parts.size != 2 || sharedSecret.isEmpty()) throw Exception("Invalid message format or empty shared secret")

        val encryptedMsg = Base64.decode(parts[0])
        val iv = Base64.decode(parts[1])

        val provider = CryptographyProvider.Default
        val aesCbc = provider.get(AES.CBC)

        val key = aesCbc.keyDecoder().decodeFrom(AES.Key.Format.RAW, sharedSecret)
        val cipher = key.cipher()

        val decryptedBytes = cipher.decrypt(iv, encryptedMsg)
        return decryptedBytes.decodeToString(0, 0 + decryptedBytes.size)
    }

    @OptIn(ExperimentalEncodingApi::class, DelicateCryptographyApi::class)
    suspend fun encrypt(message: String, sharedSecret: ByteArray): String {
        if (message.isEmpty() || sharedSecret.isEmpty()) throw Exception("Empty message or shared secret")

        val messageBytes = message.toByteArray()

        val provider = CryptographyProvider.Default
        val aesCbc = provider.get(AES.CBC)

        val key = aesCbc.keyDecoder().decodeFrom(AES.Key.Format.RAW, sharedSecret)
        val cipher = key.cipher()

        val iv = ByteArray(16)
        CryptographyRandom.nextBytes(iv)

        val encryptedBytes = cipher.encrypt(iv, messageBytes)

        val encryptedMsgBase64 = Base64.encode(encryptedBytes)
        val ivBase64 = Base64.encode(iv)

        return "$encryptedMsgBase64?iv=$ivBase64"
    }

    // NostrUtil functions
    suspend fun createEvent(
        content: String,
        event: Event,
        privateKey: ByteArray,
        publicKey: ByteArray,
        tagPubKey: String? = null
    ): NostrEvent {
        val createdAt = Clock.System.now().epochSeconds

        val eventData = buildJsonArray {
            add(0)
            add(publicKey.toHexString())
            add(createdAt)
            add(event.kind)

            // Determine tags based on the event type
            val tags = if (event == Event.ENCRYPTED_DIRECT_MESSAGE && tagPubKey != null) {
                buildJsonArray {
                    add(buildJsonArray {
                        add("p")
                        add(tagPubKey)
                    })
                }
            } else {
                JsonArray(emptyList())
            }

            add(tags)
            add(content)
        }

        val serializedEvent = json.encodeToString(eventData)
        val id = sha256Hash(serializedEvent)
        val signature = signEvent(id, privateKey)

        return NostrEvent(
            id = id,
            pubKey = publicKey.toHexString(),
            createdAt = createdAt,
            kind = event.kind,
            tags = if (event == Event.ENCRYPTED_DIRECT_MESSAGE && tagPubKey != null) {
                listOf(listOf("p", tagPubKey))
            } else {
                emptyList()
            },
            content = content,
            sig = signature
        )
    }


    private suspend fun signEvent(id: String, privateKey: ByteArray): String {
        return signContent(id.hexToByteArray(), privateKey).toHexString()
    }
}

enum class Event(val kind: Int) {
    NOTE(1),
    ENCRYPTED_DIRECT_MESSAGE(4),
    JOIN_STR(2022),
    TEST_JOIN_STR(2022566)
}