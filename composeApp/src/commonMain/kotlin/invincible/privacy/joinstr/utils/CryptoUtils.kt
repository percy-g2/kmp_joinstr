package invincible.privacy.joinstr.utils

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.digest.SHA256
import dev.whyoleg.cryptography.algorithms.symmetric.AES
import dev.whyoleg.cryptography.random.CryptographyRandom
import fr.acinq.secp256k1.Hex
import fr.acinq.secp256k1.Secp256k1
import fr.acinq.secp256k1.Secp256k1.Companion.pubKeyTweakMul
import fr.acinq.secp256k1.Secp256k1Exception
import invincible.privacy.joinstr.getCryptoProvider
import io.ktor.utils.io.core.*
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
        if (!Secp256k1.secKeyVerify(privateKey)) throw Exception("Invalid private key!")
        val pubKey = Secp256k1.pubkeyCreate(privateKey).drop(1).take(32).toByteArray()
        //context.cleanup()
        return pubKey
    }

    /**
     * Function that returns the hash of content
     * @param content the content to be hashed
     * @return the content hash, as a byte array.
     */
    //TODO: Should the function return a string or a byte array?
    fun contentHash(content: String): ByteArray {
        return getCryptoProvider().get(SHA256)
            .hasher()
            .hashBlocking(content.toByteArray())

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
    @Throws(Error::class)
    fun signContent(privateKey: ByteArray, content: ByteArray): ByteArray {
        val freshRandomBytes = ByteArray(32)
        CryptographyRandom.nextBytes(freshRandomBytes)
        val contentSignature = Secp256k1.signSchnorr(content, privateKey, freshRandomBytes)
        return contentSignature
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


    /**
     * The function verifies the provided 64-byte signature.
     * @param signature the signature to provide, as a byte array.
     * @param publicKey the 32-byte public key to provide, as a byte array.
     * @param content the signed content to provide, as a byte array.
     *
     * @return the validity of the signature, as a boolean.
     */
    @Throws(Secp256k1Exception::class)
    fun verifyContentSignature(
        signature: ByteArray,
        publicKey: ByteArray,
        content: ByteArray
    ): Boolean {
        val verificationStatus = Secp256k1.verifySchnorr(signature, content, publicKey)

        return verificationStatus
    }

    fun getSharedSecret(privateKey: ByteArray, pubKey: ByteArray): ByteArray =
        pubKeyTweakMul(Hex.decode("02") + pubKey, privateKey).copyOfRange(1, 33)
}

fun ByteArray.toHexString() = Hex.encode(this)
fun String.toBytes() = Hex.decode(this)
