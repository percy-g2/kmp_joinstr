package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class JoinedPoolContent(
    val address: String? = null,
    val hex: String? = null,
    val type: String
)
