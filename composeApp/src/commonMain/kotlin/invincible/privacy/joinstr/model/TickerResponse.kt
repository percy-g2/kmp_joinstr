package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class TickerResponse(
    val price: String,
    val symbol: String
)