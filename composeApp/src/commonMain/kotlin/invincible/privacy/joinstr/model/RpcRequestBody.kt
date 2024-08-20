package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class RpcRequestBody(
    val id: String,
    val jsonrpc: String,
    val method: String,
    val params: List<String> = emptyList()
)