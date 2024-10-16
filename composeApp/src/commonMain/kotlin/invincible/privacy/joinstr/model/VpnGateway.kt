package invincible.privacy.joinstr.model

data class VpnGateway(
    val host: String,
    val location: String,
    val ipAddress: String,
    val ports: List<String>,
    val protocols: List<String>
)