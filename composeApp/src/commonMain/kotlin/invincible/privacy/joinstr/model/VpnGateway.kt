package invincible.privacy.joinstr.model

data class VpnGateway(
    val ipAddress: String,
    val ports: List<String>,
    val protocols: List<String>
)
