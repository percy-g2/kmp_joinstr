package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class VpnGateway(
    val host: String = "",
    val location: String = "",
    val ipAddress: String = "",
    val port: String = "",
    val protocol: String = ""
)