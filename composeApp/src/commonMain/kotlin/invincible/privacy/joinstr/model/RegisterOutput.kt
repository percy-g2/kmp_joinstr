package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterAddress(
    val address: String,
    val type: String
)
