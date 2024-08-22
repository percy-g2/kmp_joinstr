package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class RpcRequestBody(
    val id: String = "curltest",
    val jsonrpc: String = "1.0",
    val method: String,
    val params: List<String> = emptyList()
)

enum class Methods(val value: String) {
    LIST_UNSPENT("listunspent"),
    BLOCK_CHAIN_INFO("getblockchaininfo");
}