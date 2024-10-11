package invincible.privacy.joinstr

import invincible.privacy.joinstr.ktx.hexToByteArray
import invincible.privacy.joinstr.ktx.toHexString
import invincible.privacy.joinstr.network.NostrClient
import invincible.privacy.joinstr.utils.Event
import invincible.privacy.joinstr.utils.NostrCryptoUtils.createEvent
import invincible.privacy.joinstr.utils.NostrCryptoUtils.decrypt
import invincible.privacy.joinstr.utils.NostrCryptoUtils.encrypt
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NostrEventsTest {

    // TEST KEY: DO NOT USE THIS
    private val privateKey = "5be151f3b93b0ddacbdb634c0c1828e3b243073103418541b2c096b108d876f8".hexToByteArray()

    // TEST KEY: DO NOT USE THIS
    private val publicKey = "9d7dfdc4e5fff62bfd4f94a5c8f586021caa0fc503ca88d4457233b1a17ec11e".hexToByteArray()

    @Test
    fun testSuccessfulEncryptionAndDecryptionWithValidSharedSecret() = runTest {
        val sharedSecret = getSharedSecret(privateKey, publicKey)
        // Given
        val message = "This is a secret message"

        // When
        val encryptedMessage = encrypt(message, sharedSecret)
        println("Encrypted Message: $encryptedMessage")

        val decryptedMessage = decrypt(encryptedMessage, sharedSecret)
        println("Decrypted Message: $decryptedMessage")

        // Then
        assertEquals(message, decryptedMessage, "The decrypted message should match the original")
    }

    @Test
    fun testNostrNoteEventCreationAndSuccessfulSend() = runTest {
        // Given
        val content = "This is a test nostr note event"

        // When
        val nostrEvent = createEvent(
            content = content,
            event = Event.NOTE,
            privateKey = privateKey,
            publicKey = publicKey
        )

        assertNotNull(nostrEvent, "Event creation should not be null")

        NostrClient().sendEvent(
            event = nostrEvent,
            onError = { error ->
                // Then
                assertNull(error, "Error callback should not be triggered on successful send")
            },
            onSuccess = { eventId ->
                // Then
                assertNotNull(eventId, "Event ID should be returned on successful send")
            }
        )
    }

    @Test
    fun testNostrEncryptedEventCreationAndSuccessfulSend() = runTest {
        // Given
        val content = "This is a test nostr encrypted DM event"

        val sharedSecret = getSharedSecret(privateKey, publicKey)
        val encryptedMessage = encrypt(content, sharedSecret)

        // When
        val nostrEvent = createEvent(
            content = encryptedMessage,
            event = Event.ENCRYPTED_DIRECT_MESSAGE,
            privateKey = privateKey,
            publicKey = publicKey,
            tagPubKey = publicKey.toHexString()
        )
        assertNotNull(nostrEvent, "Event creation should not be null")

        NostrClient().sendEvent(
            event = nostrEvent,
            onError = { error ->
                // Then
                assertNull(error, "Error callback should not be triggered on successful send")
            },
            onSuccess = { eventId ->
                // Then
                assertNotNull(eventId, "Event ID should be returned on successful send")
            }
        )
    }
}