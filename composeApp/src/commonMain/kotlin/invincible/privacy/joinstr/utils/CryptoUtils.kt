package invincible.privacy.joinstr.utils


import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.symmetric.AES
import dev.whyoleg.cryptography.random.CryptographyRandom
import invincible.privacy.joinstr.pubkeyCreate
import invincible.privacy.joinstr.signSchnorr
import io.ktor.utils.io.core.*
import okio.ByteString.Companion.encodeUtf8
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

//Class containing all the cryptographic helpers for Nostr
object CryptoUtils {

    fun generatePrivateKey(): ByteArray {
        val secretKey = ByteArray(32)
        val pseudoRandomBytes = CryptographyRandom
        pseudoRandomBytes.nextBytes(secretKey)
        return secretKey
    }

    /**
     * Generates(creates) a 32-byte public key from the provided private key.
     * @param privateKey the 32-byte private key, provided as a byte
     * array.
     *
     * @return the public key, as a byte array.
     */
    fun getPublicKey(privateKey: ByteArray): ByteArray {
        return pubkeyCreate(privateKey)
    }

    /**
     * Function that returns the hash of content
     * @param content the content to be hashed
     * @return the content hash, as a byte array.
     */


    fun sha256Hash(content: String): String {
        // Convert input string to ByteString
        val byteString = content.encodeUtf8()

        // Generate SHA-256 hash
        val hash = byteString.sha256()

        // Return the hash as a hex string
        return hash.hex()
    }


    /**
     * The function signs the content provided to it, and
     * returns the 64-byte schnorr signature of the content.
     * @param privateKey the private key used for signing, provided
     * as a byte array.
     * @param content the content to be signed, provided as a
     * byte array.
     *
     * @return the 64-byte signature, as a byte array.
     */

    suspend fun signContent(content: ByteArray, privateKey: ByteArray): ByteArray {
        val freshRandomBytes = ByteArray(32)
        CryptographyRandom.nextBytes(freshRandomBytes)
        return signSchnorr(content, privateKey, freshRandomBytes)
    }

    /**
     * Function to decrypt the message with the provided shared secret.
     * @param message The encrypted message in base64 format followed by "?iv=" and the IV in base64 format.
     * @param sharedSecret The shared secret used to decrypt the message.
     * @return The decrypted message as a String.
     * @throws GeneralSecurityException In case of any decryption error.
     * @throws DecoderException In case of any error during base64 decoding.
     */
    @OptIn(ExperimentalEncodingApi::class, DelicateCryptographyApi::class)
    suspend fun decrypt(message: String, sharedSecret: ByteArray): String {
        val parts = message.split("?iv=")
        if (parts.size != 2 || sharedSecret.isEmpty()) throw Exception("Invalid message format or empty shared secret")

        val encryptedMsg = Base64.decode(parts[0])
        val iv = Base64.decode(parts[1])

        // Initialize the cryptography provider and AES-CBC algorithm
        val provider = CryptographyProvider.Default
        val aesCbc = provider.get(AES.CBC)

        // Decode the shared secret into a key and initialize the cipher with the IV
        val key = aesCbc.keyDecoder().decodeFrom(AES.Key.Format.RAW, sharedSecret)
        val cipher = key.cipher()

        // Decrypt the message and return as a String
        val decryptedBytes = cipher.decrypt(iv, encryptedMsg)
        return decryptedBytes.decodeToString(0, 0 + decryptedBytes.size)
    }

    @OptIn(ExperimentalEncodingApi::class, DelicateCryptographyApi::class)
    suspend fun encrypt(message: String, sharedSecret: ByteArray): String {
        if (message.isEmpty() || sharedSecret.isEmpty()) throw Exception("Empty message or shared secret")

        // Convert the message to a byte array
        val messageBytes = message.toByteArray()

        // Initialize the cryptography provider and AES-CBC algorithm
        val provider = CryptographyProvider.Default
        val aesCbc = provider.get(AES.CBC)

        // Decode the shared secret into a key and initialize the cipher
        val key = aesCbc.keyDecoder().decodeFrom(AES.Key.Format.RAW, sharedSecret)
        val cipher = key.cipher()

        // Generate a random IV for encryption
        val iv = ByteArray(16)
        CryptographyRandom.nextBytes(iv)

        // Encrypt the message bytes
        val encryptedBytes = cipher.encrypt(iv, messageBytes)

        // Encode the encrypted message and IV to Base64 and concatenate them with "?iv=" as a delimiter
        val encryptedMsgBase64 = Base64.encode(encryptedBytes)
        val ivBase64 = Base64.encode(iv)

        return "$encryptedMsgBase64?iv=$ivBase64"
    }

}
