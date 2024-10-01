package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class Wallet(
    val name: String
)

@Serializable
data class WalletResult(
    val wallets: List<Wallet>
)
