package invincible.privacy.joinstr.model

import kotlinx.serialization.Serializable

@Serializable
data class PoolContent(
    val type: String,
    val id: String,
    val publicKey: String,
    val privateKey: String? = null,
    val denomination: Double,
    val peers: Int,
    val timeout: Long,
    val relay: String,
    val feeRate: Int,
    val transport: String? = null,
    val vpnGateway: String? = null
)

@Serializable
data class LocalPoolContent(
    val type: String,
    val id: String,
    val publicKey: String,
    val privateKey: String,
    val denomination: Double,
    val peers: Int,
    val timeout: Long,
    val relay: String,
    val feeRate: Int,
    val peersPublicKeys: List<String> = emptyList(),
    val peersData: List<JoinedPoolContent> = emptyList(),
    val transport: String? = null,
    val vpnGateway: String? = null
)

fun copyToLocalPoolContent(poolContent: PoolContent): LocalPoolContent {
    return LocalPoolContent(
        type = poolContent.type,
        id = poolContent.id,
        publicKey = poolContent.publicKey,
        privateKey = "",
        denomination = poolContent.denomination,
        peers = poolContent.peers,
        timeout = poolContent.timeout,
        relay = poolContent.relay,
        feeRate = poolContent.feeRate,
        peersPublicKeys = emptyList(),
        transport = poolContent.transport,
        vpnGateway = poolContent.vpnGateway
    )
}
