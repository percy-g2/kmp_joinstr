package invincible.privacy.joinstr.network

import invincible.privacy.joinstr.currentChain
import invincible.privacy.joinstr.ktx.isValidHttpUrl
import invincible.privacy.joinstr.model.Gateway
import invincible.privacy.joinstr.model.MempoolFee
import invincible.privacy.joinstr.model.RiseupVPN
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.model.VpnGateway
import invincible.privacy.joinstr.model.Wallet
import invincible.privacy.joinstr.utils.NodeConfig
import invincible.privacy.joinstr.utils.SettingsManager
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

class HttpClient {

    val getNodeConfig: suspend () -> NodeConfig
        get() = suspend { SettingsManager.store.get()?.nodeConfig ?: NodeConfig() }

    fun createHttpClient(timeout: Int = 5): HttpClient =  HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = timeout.seconds.inWholeMilliseconds
                connectTimeoutMillis = timeout.seconds.inWholeMilliseconds
                socketTimeoutMillis = timeout.seconds.inWholeMilliseconds
            }
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.NONE
            }
            install(ContentNegotiation) {
                json(json)
            }
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
    }

    suspend inline fun <reified T> fetchNodeData(
        body: RpcRequestBody,
        wallet: Wallet? = null
    ): T? = runCatching {
        val nodeConfig = getNodeConfig()
        if (nodeConfig.url.isValidHttpUrl() && nodeConfig.userName.isNotBlank()
            && nodeConfig.password.isNotBlank() && nodeConfig.port in 1..65535
        ) {
            val response: HttpResponse = createHttpClient().post {
                if (wallet != null && wallet.name.isEmpty().not()) {
                    url("${nodeConfig.url}:${nodeConfig.port}/wallet/${wallet.name}")
                } else url("${nodeConfig.url}:${nodeConfig.port}/")
                basicAuth(
                    username = nodeConfig.userName,
                    password = nodeConfig.password
                )
                setBody(body)
            }
            json.decodeFromString<T>(response.bodyAsText())
        } else null
    }.getOrElse {
        Napier.e("FetchNodeData ${body.method}", it)
        null
    }

    suspend fun fetchHourFee(): Int? = runCatching {
        val url = when(currentChain.value) {
            "Main" -> "https://mempool.space/api/v1/fees/recommended"
            "Testnet4" -> "https://mempool.space/testnet4/api/v1/fees/recommended"
            else -> "https://mempool.space/signet/api/v1/fees/recommended"
        }
        val response: HttpResponse = createHttpClient().get {
            url(url)
        }
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<MempoolFee>(response.bodyAsText()).hourFee
        } else null
    }.getOrElse {
        Napier.e("FetchHourFee", it)
        null
    }

    suspend fun broadcastRawTx(rawTx: String): String? = runCatching {
        val url = when(currentChain.value) {
            "Main" -> "https://mempool.space/api/tx"
            "Testnet4" -> "https://mempool.space/testnet4/api/tx"
            else -> "https://mempool.space/signet/api/tx"
        }
        val response: HttpResponse = createHttpClient().post {
            url(url)
            setBody(rawTx)
        }
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<String>(response.bodyAsText())
        } else response.bodyAsText()
    }.getOrElse {
        Napier.e("BroadcastRawTx", it)
        null
    }

    suspend fun fetchVpnGateways(): List<VpnGateway>? = runCatching {
        val configUrl = "https://api.black.riseup.net:443/3/config/eip-service.json"
        val response: HttpResponse = createHttpClient(timeout = 60).get(configUrl) {
            header("Accept", "application/json")
        }

        if (response.status == HttpStatusCode.OK) {
            val configData = json.decodeFromString<RiseupVPN>(response.bodyAsText())
            filterGateways(configData.gateways)
        } else {
            null
        }
    }.getOrElse {
        Napier.e("FetchVpnGateways", it)
        null
    }

    suspend fun fetchCaCertificate(): String? = runCatching {
        val configUrl = "https://black.riseup.net/ca.crt"
        val response: HttpResponse = createHttpClient().get(configUrl) {
            header("Accept", "application/json")
        }

        if (response.status == HttpStatusCode.OK) {
            response.bodyAsText()
        } else {
            null
        }
    }.getOrElse {
        Napier.e("fetchCaCertificate", it)
        null
    }

    suspend fun fetchClientsPublicCertificateAndKey(): String? = runCatching {
        val configUrl = "https://api.black.riseup.net/3/cert"
        val response: HttpResponse = createHttpClient().get(configUrl) {
            header("Accept", "application/json")
        }

        if (response.status == HttpStatusCode.OK) {
            response.bodyAsText()
        } else {
            null
        }
    }.getOrElse {
        Napier.e("fetchClientsPublicCertificateAndKey", it)
        null
    }

    private fun filterGateways(gateways: List<Gateway>): List<VpnGateway> {
        return gateways.filter { gateway ->
            gateway.capabilities.transport.any { transport ->
                transport.ports.contains("53") && transport.protocols.contains("udp")
            }
        }.map { gateway ->
            VpnGateway(
                host = gateway.host,
                ipAddress = gateway.ip_address,
                port = "53",
                protocol = "udp",
                location = gateway.location
            )
        }
    }
}

val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}