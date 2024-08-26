package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class ListUnspentResponseItem(
    val address: String,
    val amount: Double,
    val confirmations: Int,
    val desc: String,
    val label: String,
    val parent_descs: List<String>,
    val safe: Boolean,
    val scriptPubKey: String,
    val solvable: Boolean,
    val spendable: Boolean,
    val txid: String,
    val vout: Int
)