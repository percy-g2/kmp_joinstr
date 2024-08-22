package invincible.privacy.joinstr.network

import invincible.privacy.joinstr.model.BlockChainInfo
import invincible.privacy.joinstr.model.ListUnspentResponse
import invincible.privacy.joinstr.model.ListUnspentResponseItem
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.model.TickerResponse
import invincible.privacy.joinstr.model.Transaction
import invincible.privacy.joinstr.model.TransactionsResponse
import invincible.privacy.joinstr.theme.NodeConfig
import invincible.privacy.joinstr.theme.SettingsManager
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

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private suspend fun getNodeConfig(): NodeConfig {
        return SettingsManager.store.get()?.nodeConfig ?: NodeConfig()
    }

    private val createHttpClient: HttpClient =  HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 5.seconds.inWholeMilliseconds
                connectTimeoutMillis = 5.seconds.inWholeMilliseconds
                socketTimeoutMillis = 5.seconds.inWholeMilliseconds
            }
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.ALL
            }
            install(ContentNegotiation) {
                json(json)
            }
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
    }

    suspend fun fetchUsdtPrice(): Double = try {
        val response: HttpResponse = createHttpClient.get {
            url("https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT")
        }
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<TickerResponse>(response.bodyAsText()).price.toDouble()
        } else 0.0
    } catch (e: Exception) {
        e.printStackTrace()
        0.0
    }

    suspend fun fetchBlockChainInfo(body: RpcRequestBody): BlockChainInfo? = try {
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
            json.decodeFromString<BlockChainInfo>(response.bodyAsText())
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    suspend fun fetchTransactions(body: RpcRequestBody): List<Transaction>? = try {
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
            json.decodeFromString<TransactionsResponse>(response.bodyAsText()).result
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    suspend fun fetchUnspentList(body: RpcRequestBody): List<ListUnspentResponseItem> = try {
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
            json.decodeFromString<ListUnspentResponse>(response.bodyAsText()).result
        } else emptyList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}
