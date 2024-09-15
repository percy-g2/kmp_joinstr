package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class CoinJoinHistory(
    val relay: String,
    val publicKey: String,
    val privateKey: String,
    val amount: Float,
    val address: String,
    val psbt: String,
    val tx: String,
    val timestamp: Long
)