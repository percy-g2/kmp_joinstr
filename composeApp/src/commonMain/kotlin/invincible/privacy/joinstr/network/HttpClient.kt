package invincible.privacy.joinstr.network

import invincible.privacy.joinstr.model.BlockChainInfo
import invincible.privacy.joinstr.model.RpcRequestBody
import io.ktor.client.*
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

val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
}

class HttpClient {
    private val client = HttpClient {
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
                    BasicAuthCredentials(username = "user", password = "pass")
                }
            }
        }
        defaultRequest {
            url("http://127.0.0.1:38332/")
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun fetchBlockChainInfo(body: RpcRequestBody): BlockChainInfo? {
        return try {
            val response: HttpResponse = client.post {
                setBody(body)
            }
            if (response.status == HttpStatusCode.OK) {
                return json.decodeFromString<BlockChainInfo>(response.bodyAsText())
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
