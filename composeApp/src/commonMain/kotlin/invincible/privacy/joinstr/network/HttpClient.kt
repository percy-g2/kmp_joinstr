package invincible.privacy.joinstr.network

import invincible.privacy.joinstr.model.MempoolFee
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.utils.NodeConfig
import invincible.privacy.joinstr.utils.SettingsManager
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

    suspend inline fun <reified T> fetchNodeData(body: RpcRequestBody): T? = runCatching {
        val nodeConfig = getNodeConfig()
        val response: HttpResponse = createHttpClient.post {
            url("${nodeConfig.url}:${nodeConfig.port}/")
            basicAuth(
                username = nodeConfig.userName,
                password = nodeConfig.password
            )
            setBody(body)
        }
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<T>(response.bodyAsText())
        } else null
    }.getOrElse {
        it.printStackTrace()
        null
    }

    suspend fun fetchHourFee(): Int? = runCatching {
        val response: HttpResponse = createHttpClient.get {
            url("https://mempool.space/api/v1/fees/recommended")
        }
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<MempoolFee>(response.bodyAsText()).hourFee
        } else null
    }.getOrElse {
        it.printStackTrace()
        null
    }

    suspend fun broadcastRawTx(rawTx: String): String? = runCatching {
        val response: HttpResponse = createHttpClient.post {
            url("https://mempool.space/signet/api/tx")
            setBody(rawTx)
        }
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<String>(response.bodyAsText())
        } else null
    }.getOrElse {
        it.printStackTrace()
        null
    }
}

val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}