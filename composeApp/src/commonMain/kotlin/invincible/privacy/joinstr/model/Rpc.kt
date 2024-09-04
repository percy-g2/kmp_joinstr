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
    NEW_ADDRESS("getnewaddress"),
    NETWORK_INFO("getnetworkinfo"),
    BLOCK_CHAIN_INFO("getblockchaininfo");
}

@Serializable
data class RpcResponse<T>(
    val result: T,
    val error: String? = null,
    val id: String
)