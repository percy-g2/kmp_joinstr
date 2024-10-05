package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class BlockchainInfo(
    val chain: String = "",
    val blocks: Int = 0,
    val headers: Int = 0,
    val bestblockhash: String = "",
    val difficulty: Double = 0.0,
    val time: Long = 0L,
    val mediantime: Long = 0L,
    val verificationprogress: Double = 0.0,
    val initialblockdownload: Boolean = false,
    val chainwork: String = "",
    val size_on_disk: Long = 0L,
    val pruned: Boolean = false,
    val pruneheight: Int = 0,
    val automatic_pruning: Boolean = false,
    val prune_target_size: Long = 0L
)
