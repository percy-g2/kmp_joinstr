package invincible.privacy.joinstr.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NostrEvent(
    val id: String,
    @SerialName("pubkey")
    val pubKey: String,
    @SerialName("created_at")
    val createdAt: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
    val sig: String,
)