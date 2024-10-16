package invincible.privacy.joinstr.network

import invincible.privacy.joinstr.ktx.isValidHttpUrl
import invincible.privacy.joinstr.model.MempoolFee
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.seconds

class HttpClient {

    val getNodeConfig: suspend () -> NodeConfig
        get() = suspend { SettingsManager.store.get()?.nodeConfig ?: NodeConfig() }

    val createHttpClient: HttpClient =  HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 5.seconds.inWholeMilliseconds
                connectTimeoutMillis = 5.seconds.inWholeMilliseconds
                socketTimeoutMillis = 5.seconds.inWholeMilliseconds
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
            val response: HttpResponse = createHttpClient.post {
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
        val response: HttpResponse = createHttpClient.get {
            url("https://mempool.space/signet/api/v1/fees/recommended")
        }
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<MempoolFee>(response.bodyAsText()).hourFee
        } else null
    }.getOrElse {
        Napier.e("FetchHourFee", it)
        null
    }

    suspend fun broadcastRawTx(rawTx: String): String? = runCatching {
        val response: HttpResponse = createHttpClient.post {
            url("https://mempool.space/signet/api/tx")
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
        val response: HttpResponse = createHttpClient.get(configUrl) {
            header("Accept", "application/json")
        }

        if (response.status == HttpStatusCode.OK) {
            val configData = json.parseToJsonElement(response.bodyAsText()).jsonObject
            val gateways = configData["gateways"]?.jsonArray ?: return emptyList()

            filterGateways(gateways)
        } else {
            null
        }
    }.getOrElse {
        Napier.e("FetchVpnGateways", it)
        null
    }

    private fun filterGateways(gateways: JsonArray): List<VpnGateway> {
        return gateways.mapNotNull { it as? JsonObject }
            .filter { gateway ->
                gateway["capabilities"]?.jsonObject?.get("transport")?.jsonArray?.any { transport ->
                    val ports = transport.jsonObject["ports"]?.jsonArray
                    val protocols = transport.jsonObject["protocols"]?.jsonArray
                    ports?.contains(JsonPrimitive("53")) == true && protocols?.contains(JsonPrimitive("udp")) == true
                } == true
            }
            .map { gateway ->
                val ip = gateway["ip_address"]?.jsonPrimitive?.content ?: ""
                val host = gateway["host"]?.jsonPrimitive?.content ?: ""
                val location = gateway["location"]?.jsonPrimitive?.content ?: ""
                val transport = gateway["capabilities"]?.jsonObject?.get("transport")?.jsonArray?.firstOrNull()?.jsonObject
                val ports = transport?.get("ports")?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                val protocols = transport?.get("protocols")?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                VpnGateway(
                    host = host,
                    ipAddress = ip,
                    ports = ports,
                    protocols = protocols,
                    location =  location
                )
            }
    }
}

val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}