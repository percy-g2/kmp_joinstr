package invincible.privacy.joinstr.network

import invincible.privacy.joinstr.model.BlockChainInfo
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.model.Transaction
import invincible.privacy.joinstr.model.TransactionsResponse
import invincible.privacy.joinstr.theme.NodeConfig
import invincible.privacy.joinstr.theme.SettingsManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
}

class HttpClient {
    private suspend fun getNodeConfig(): NodeConfig? {
        return SettingsManager.store.get()?.nodeConfig
    }

    private suspend fun createHttpClient(): HttpClient {
        val nodeConfig = getNodeConfig()

        return HttpClient {
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
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(
                            username = "${nodeConfig?.userName}",
                            password = "${nodeConfig?.password}"
                        )
                    }
                }
            }
            defaultRequest {
                url("${nodeConfig?.url}:${nodeConfig?.port}/")
                contentType(ContentType.Application.Json)
            }
        }
    }

    suspend fun fetchBlockChainInfo(body: RpcRequestBody): BlockChainInfo? = try {
        val response: HttpResponse = createHttpClient().post {
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
        val response: HttpResponse = createHttpClient().post {
            setBody(body)
        }
        if (response.status == HttpStatusCode.OK) {
            json.decodeFromString<TransactionsResponse>(response.bodyAsText()).result
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
