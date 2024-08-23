package invincible.privacy.joinstr.utils

import dev.whyoleg.cryptography.random.CryptographyRandom
import fr.acinq.secp256k1.Secp256k1
import invincible.privacy.joinstr.model.NostrEvent
import invincible.privacy.joinstr.utils.CryptoUtils.generatePrivateKey
import invincible.privacy.joinstr.utils.CryptoUtils.getPublicKey
import invincible.privacy.joinstr.utils.CryptoUtils.sha256Hash
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

class NostrUtil {
    private val secp256k1 = Secp256k1
    private val json = Json { encodeDefaults = true }

    fun createKindEvent(content: String, events: Events): NostrEvent {
        val privateKey = generatePrivateKey()
        val publicKey = getPublicKey(privateKey)
        val createdAt = Clock.System.now().epochSeconds

        // 1. Create the event object without the 'id' and 'sig' fields
        val eventData = buildJsonArray {
            add(0) // serialize_0
            add(publicKey.toHexString()) // serialize_1
            add(createdAt) // serialize_2
            add(events.value) // serialize_3 (kind)
            add(JsonArray(emptyList<JsonElement>())) // serialize_4 (tags)
            add(content) // serialize_5
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
            pubkey = publicKey.toHexString(),
            created_at = createdAt,
            kind = 1,
            tags = emptyList(),
            content = content,
            sig = signature
        )
    }

    private fun sha256(input: String): String {
        return sha256Hash(input).toHexString()
    }

    private fun signEvent(id: String, privateKey: ByteArray): String {
        val freshRandomBytes = ByteArray(32)
        CryptographyRandom.nextBytes(freshRandomBytes)
        val signature = secp256k1.signSchnorr(id.hexToByteArray(), privateKey, freshRandomBytes)
        return signature.toHexString()
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private fun String.hexToByteArray(): ByteArray =
        ByteArray(length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}

enum class Events(val value: Int) {
    KIND_1(1)
}