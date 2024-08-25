package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class PoolCreationContent(
    val type: String,
    val id: String,
    val publicKey: String,
    val denomination: Float,
    val peers: Int,
    val timeout: Long,
    val relay: String,
    val feeRate: Int
)
