package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class Input(
    val txid: String, // Transaction ID
    val vout: Int,    // Output index in the previous transaction
)