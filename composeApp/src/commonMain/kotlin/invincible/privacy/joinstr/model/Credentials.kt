package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class Credentials(
    val id: String,
    val publicKey: String,
    val denomination: Double,
    val peers: Int,
    val timeout: Long,
    val relay: String,
    val privateKey: String,
    val feeRate: Int,
    val transport: String? = null,
    val vpnGateway: String? = null
)
