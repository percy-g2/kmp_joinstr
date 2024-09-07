package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

@Serializable
data class RpcRequestBody(
    val id: String = "curltest",
    val jsonrpc: String = "1.0",
    val method: String,
    val params: JsonElement = JsonArray(emptyList())
)

enum class Methods(val value: String) {
    LIST_UNSPENT("listunspent"),
    NEW_ADDRESS("getnewaddress"),
    WALLET_PROCESS_PSBT("walletprocesspsbt"),
    NETWORK_INFO("getnetworkinfo"),
    CREATE_PSBT("createpsbt"),
    BLOCK_CHAIN_INFO("getblockchaininfo");
}

@Serializable
data class RpcResponse<T>(
    val result: T,
    val error: String? = null,
    val id: String
)