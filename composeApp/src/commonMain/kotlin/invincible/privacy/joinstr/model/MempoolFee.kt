package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class MempoolFee(
    val fastestFee: Int,
    val halfHourFee: Int,
    val hourFee: Int,
    val economyFee: Int,
    val minimumFee: Int
)
