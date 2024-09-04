package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class NetworkInfo(
    val version: Int,
    val subversion: String,
    val protocolversion: Int,
    val localservices: String,
    val localservicesnames: List<String>,
    val localrelay: Boolean,
    val timeoffset: Int,
    val networkactive: Boolean,
    val connections: Int,
    val connections_in: Int,
    val connections_out: Int,
    val networks: List<Network>,
    val relayfee: Double,
    val incrementalfee: Double,
    val localaddresses: List<LocalAddress>,
    val warnings: String
)

@Serializable
data class Network(
    val name: String,
    val limited: Boolean,
    val reachable: Boolean,
    val proxy: String,
    val proxy_randomize_credentials: Boolean
)

@Serializable
data class LocalAddress(
    val address: String,
    val port: Int,
    val score: Int
)