package invincible.privacy.joinstr

import invincible.privacy.joinstr.network.NostrClient
import invincible.privacy.joinstr.utils.Event
import invincible.privacy.joinstr.utils.NostrCryptoUtils.createEvent
import invincible.privacy.joinstr.utils.NostrCryptoUtils.decrypt
import invincible.privacy.joinstr.utils.NostrCryptoUtils.encrypt
import invincible.privacy.joinstr.utils.NostrCryptoUtils.generatePrivateKey
import invincible.privacy.joinstr.utils.NostrCryptoUtils.getPublicKey
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NostrEventsTest {

    @Test
    fun testSuccessfulEncryptionAndDecryptionWithValidSharedSecret() = runTest {
        val privateKey = generatePrivateKey()
        val publicKey = getPublicKey(privateKey)
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
    fun testNostrEventCreationAndSuccessfulSend() = runTest {
        val privateKey = generatePrivateKey()
        val publicKey = getPublicKey(privateKey)

        // Given
        val content = "This is a test Nostr event"

        // When
        val nostrEvent = createEvent(content, Event.NOTE, privateKey, publicKey)
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