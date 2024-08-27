package invincible.privacy.joinstr.utils

import dev.whyoleg.cryptography.random.CryptographyRandom
import invincible.privacy.joinstr.model.NostrEvent
import invincible.privacy.joinstr.signSchnorr
import invincible.privacy.joinstr.utils.CryptoUtils.sha256Hash
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

class NostrUtil {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun createEvent(
        content: String,
        event: Event,
        privateKey: ByteArray,
        publicKey: ByteArray
    ): NostrEvent {

        val createdAt = Clock.System.now().epochSeconds

        // 1. Create the event object without the 'id' and 'sig' fields
        val eventData = buildJsonArray {
            add(0)
            add(publicKey.toHexString())
            add(createdAt)
            add(event.kind)
            add(JsonArray(emptyList<JsonElement>()))
            add(content)
        }

        // 2. Serialize the event object
        val serializedEvent = json.encodeToString(eventData)

        // 3. Calculate the event id
        val id = sha256(serializedEvent)

        // 4. Create the signature
        val signature = signEvent(id, privateKey)

        // 5. Construct the final event object
        return NostrEvent(
            id = id,
            pubKey = publicKey.toHexString(),
            createdAt = createdAt,
            kind = event.kind,
            tags = emptyList(),
            content = content,
            sig = signature
        )
    }

    private fun sha256(input: String): String {
        return sha256Hash(input)
    }

    private suspend fun signEvent(id: String, privateKey: ByteArray): String {
        val freshRandomBytes = ByteArray(32)
        CryptographyRandom.nextBytes(freshRandomBytes)
        val signature = signSchnorr(id.hexToByteArray(), privateKey, freshRandomBytes)
        return signature.toHexString()
    }

    private fun String.hexToByteArray(): ByteArray =
        ByteArray(length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}

enum class Event(val kind: Int) {
    NOTE(1),
    ENCRYPTED_DIRECT_MESSAGE(4),
    JOIN_STR(2022),
    TEST_JOIN_STR(2022566)
}

fun ByteArray.toHexString(): String =
    joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
