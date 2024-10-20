package invincible.privacy.joinstr.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RiseupVPN(
    val gateways: List<Gateway>,
    val locations: Map<String, Location>,
    val openvpn_configuration: OpenVPNConfiguration,
    val serial: Int,
    val version: Int
)

@Serializable
data class Gateway(
    val capabilities: Capabilities,
    val host: String,
    val ip_address: String,
    val location: String
)

@Serializable
data class Capabilities(
    val adblock: Boolean,
    val filter_dns: Boolean,
    val limited: Boolean,
    val transport: List<Transport>
)

@Serializable
data class Transport(
    val ports: List<String>,
    val protocols: List<String>,
    val type: String,
    val options: Options? = null
)

@Serializable
data class Options(
    val cert: String,
    val iatMode: String
)

@Serializable
data class Location(
    val country_code: String,
    val hemisphere: String,
    val name: String,
    val timezone: String
)

@Serializable
data class OpenVPNConfiguration(
    val auth: String,
    val cipher: String,
    @SerialName("data-ciphers")
    val dataCiphers: String,
    val dev: String,
    val float: String,
    val keepalive: String,
    @SerialName("key-direction")
    val keyDirection: String,
    val nobind: Boolean,
    @SerialName("persist-key")
    val persistKey: Boolean,
    val rcvbuf: String,
    val sndbuf: String,
    @SerialName("tls-cipher")
    val tlsCipher: String,
    @SerialName("tls-version-min")
    val tlsVersionMin: String,
    val verb: String
)