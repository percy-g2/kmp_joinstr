package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class Output(
    val address: String? = null, // Address to send Bitcoin to
    val amount: Double? = null,  // Amount in BTC
)