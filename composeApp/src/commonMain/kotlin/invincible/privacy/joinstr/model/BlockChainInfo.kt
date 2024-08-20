package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class BlockChainInfo(
    val error: String? = null,
    val id: String,
    val result: Result
)

@Serializable
data class Result(
    val automatic_pruning: Boolean,
    val bestblockhash: String,
    val blocks: Int,
    val chain: String,
    val chainwork: String,
    val difficulty: Double,
    val headers: Int,
    val initialblockdownload: Boolean,
    val mediantime: Int,
    val prune_target_size: Int,
    val pruned: Boolean,
    val pruneheight: Int,
    val size_on_disk: Int,
    val time: Int,
    val verificationprogress: Double,
    val warnings: String
)