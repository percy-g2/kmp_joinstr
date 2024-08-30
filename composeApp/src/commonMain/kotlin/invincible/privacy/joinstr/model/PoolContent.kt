package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class PoolContent(
    val type: String,
    val id: String,
    val publicKey: String,
    val privateKey: String? = null,
    val denomination: Float,
    val peers: Int,
    val timeout: Long,
    val relay: String,
    val feeRate: Int
)
