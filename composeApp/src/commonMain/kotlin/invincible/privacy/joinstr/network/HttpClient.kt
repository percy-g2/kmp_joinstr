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

    suspend fun getNodeConfig(): NodeConfig {
        return SettingsManager.store.get()?.nodeConfig ?: NodeConfig()
    }

    val createHttpClient: HttpClient =  HttpClient {
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
}

val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

const val test = "{\n" +
    "    \"result\": [\n" +
    "        {\n" +
    "            \"txid\": \"a1f6d4b2c4328bc2d0281b518cc7ecae0b7e6b47ad6a578c1e61747c4b842d91\",\n" +
    "            \"vout\": 0,\n" +
    "            \"address\": \"tb1qyl5cy6fx93l3fgpp7l8hygtvwxwkdsy0zkgwl4\",\n" +
    "            \"label\": \"\",\n" +
    "            \"scriptPubKey\": \"0014d1bc4b768471b949ccf4e8f780dbe3c0e6f5cb9f\",\n" +
    "            \"amount\": 0.0012,\n" +
    "            \"confirmations\": 95108,\n" +
    "            \"spendable\": true,\n" +
    "            \"solvable\": true,\n" +
    "            \"desc\": \"wpkh([733d9ede/84h/1h/0h/0/0]02ef489e775ba44bc5abffdaae0a2ed1bcb7857bd7ae24b0679984d9f842f8ff8b)#t57ekv6e\",\n" +
    "            \"parent_descs\": [\n" +
    "                \"wpkh(tpubD6NzVbkrYhZ4WYjWihxXYLeRBs4qm7afvuzYARdnjmWjCksjyU1ScokTe9dgMiDXfDy2MD1G3d7yuiknw7bQ96P4yvMG2QpX2ERDenTuo1c/84'/1'/0'/0/*)#mdvjcv6a\"\n" +
    "            ],\n" +
    "            \"safe\": true\n" +
    "        },\n" +
    "        {\n" +
    "            \"txid\": \"d9c76eec25cda227b8b0d1a2347321ea7e8e76b99b21a78e5e19f8fc9e7a6d834a4\",\n" +
    "            \"vout\": 1,\n" +
    "            \"address\": \"tb1qmnx6x44nqjrflsmryc9knzdj9y0r0eh6kxnftw\",\n" +
    "            \"label\": \"\",\n" +
    "            \"scriptPubKey\": \"00148fda3b982d9d3b6c679dcedb0b7b9e8b0f9c4f0d\",\n" +
    "            \"amount\": 0.0023425,\n" +
    "            \"confirmations\": 95108,\n" +
    "            \"spendable\": true,\n" +
    "            \"solvable\": true,\n" +
    "            \"desc\": \"wpkh([733d9ede/84h/1h/0h/0/1]03cf1e9e59444d3bb2a7b8d9b497b4a6b3f3d7eb47a4a8b5b1e4a5d5b7a8d8e5b3)#a19jkl0a\",\n" +
    "            \"parent_descs\": [\n" +
    "                \"wpkh(tpubD6NzVbkrYhZ4WYjWihxXYLeRBs4qm7afvuzYARdnjmWjCksjyU1ScokTe9dgMiDXfDy2MD1G3d7yuiknw7bQ96P4yvMG2QpX2ERDenTuo1c/84'/1'/0'/0/*)#mdvjcv6a\"\n" +
    "            ],\n" +
    "            \"safe\": true\n" +
    "        },\n" +
    "        {\n" +
    "            \"txid\": \"d9c76eec25cda227b8b0234d1a7321ea7e8e76b99b21a78e5e19f8fc9e7a6d834a4\",\n" +
    "            \"vout\": 1,\n" +
    "            \"address\": \"tb1qmnx6x44nqjrflsmryc9knzdj9y0r0eh6kxnftw\",\n" +
    "            \"label\": \"\",\n" +
    "            \"scriptPubKey\": \"00148fda3b982d9d3b6c679dcedb0b7b9e8b0f9c4f0d\",\n" +
    "            \"amount\": 0.032025,\n" +
    "            \"confirmations\": 95108,\n" +
    "            \"spendable\": true,\n" +
    "            \"solvable\": true,\n" +
    "            \"desc\": \"wpkh([733d9ede/84h/1h/0h/0/1]03cf1e9e59444d3bb2a7b8d9b497b4a6b3f3d7eb47a4a8b5b1e4a5d5b7a8d8e5b3)#a19jkl0a\",\n" +
    "            \"parent_descs\": [\n" +
    "                \"wpkh(tpubD6NzVbkrYhZ4WYjWihxXYLeRBs4qm7afvuzYARdnjmWjCksjyU1ScokTe9dgMiDXfDy2MD1G3d7yuiknw7bQ96P4yvMG2QpX2ERDenTuo1c/84'/1'/0'/0/*)#mdvjcv6a\"\n" +
    "            ],\n" +
    "            \"safe\": true\n" +
    "        },\n" +
    "        {\n" +
    "            \"txid\": \"d9c76eec25cda227234b8b0d1a7321ea7e8e76b99b21a78e5e19f8fc9e7a6d834a4\",\n" +
    "            \"vout\": 1,\n" +
    "            \"address\": \"tb1qmnx6x44nqjrflsmryc9knzdj9y0r0eh6kxnftw\",\n" +
    "            \"label\": \"\",\n" +
    "            \"scriptPubKey\": \"00148fda3b982d9d3b6c679dcedb0b7b9e8b0f9c4f0d\",\n" +
    "            \"amount\": 0.10025,\n" +
    "            \"confirmations\": 95108,\n" +
    "            \"spendable\": true,\n" +
    "            \"solvable\": true,\n" +
    "            \"desc\": \"wpkh([733d9ede/84h/1h/0h/0/1]03cf1e9e59444d3bb2a7b8d9b497b4a6b3f3d7eb47a4a8b5b1e4a5d5b7a8d8e5b3)#a19jkl0a\",\n" +
    "            \"parent_descs\": [\n" +
    "                \"wpkh(tpubD6NzVbkrYhZ4WYjWihxXYLeRBs4qm7afvuzYARdnjmWjCksjyU1ScokTe9dgMiDXfDy2MD1G3d7yuiknw7bQ96P4yvMG2QpX2ERDenTuo1c/84'/1'/0'/0/*)#mdvjcv6a\"\n" +
    "            ],\n" +
    "            \"safe\": true\n" +
    "        },\n" +
    "        {\n" +
    "            \"txid\": \"d9c76eec25c23423da227b8b0d1a7321ea7e8e76b99b21a78e5e19f8fc9e7a6d834a4\",\n" +
    "            \"vout\": 1,\n" +
    "            \"address\": \"tb1qmnx6x44nqjrflsmryc9knzdj9y0r0eh6kxnftw\",\n" +
    "            \"label\": \"\",\n" +
    "            \"scriptPubKey\": \"00148fda3b982d9d3b6c679dcedb0b7b9e8b0f9c4f0d\",\n" +
    "            \"amount\": 0.00325,\n" +
    "            \"confirmations\": 95108,\n" +
    "            \"spendable\": true,\n" +
    "            \"solvable\": true,\n" +
    "            \"desc\": \"wpkh([733d9ede/84h/1h/0h/0/1]03cf1e9e59444d3bb2a7b8d9b497b4a6b3f3d7eb47a4a8b5b1e4a5d5b7a8d8e5b3)#a19jkl0a\",\n" +
    "            \"parent_descs\": [\n" +
    "                \"wpkh(tpubD6NzVbkrYhZ4WYjWihxXYLeRBs4qm7afvuzYARdnjmWjCksjyU1ScokTe9dgMiDXfDy2MD1G3d7yuiknw7bQ96P4yvMG2QpX2ERDenTuo1c/84'/1'/0'/0/*)#mdvjcv6a\"\n" +
    "            ],\n" +
    "            \"safe\": true\n" +
    "        },\n" +
    "        {\n" +
    "            \"txid\": \"d9c76eec25cda227b82234b0d1a7321ea7e8e76b99b21a78e5e19f8fc9e7a6d834a4\",\n" +
    "            \"vout\": 1,\n" +
    "            \"address\": \"tb1qmnx6x44nqjrflsmryc9knzdj9y0r0eh6kxnftw\",\n" +
    "            \"label\": \"\",\n" +
    "            \"scriptPubKey\": \"00148fda3b982d9d3b6c679dcedb0b7b9e8b0f9c4f0d\",\n" +
    "            \"amount\": 0.0025,\n" +
    "            \"confirmations\": 95108,\n" +
    "            \"spendable\": true,\n" +
    "            \"solvable\": true,\n" +
    "            \"desc\": \"wpkh([733d9ede/84h/1h/0h/0/1]03cf1e9e59444d3bb2a7b8d9b497b4a6b3f3d7eb47a4a8b5b1e4a5d5b7a8d8e5b3)#a19jkl0a\",\n" +
    "            \"parent_descs\": [\n" +
    "                \"wpkh(tpubD6NzVbkrYhZ4WYjWihxXYLeRBs4qm7afvuzYARdnjmWjCksjyU1ScokTe9dgMiDXfDy2MD1G3d7yuiknw7bQ96P4yvMG2QpX2ERDenTuo1c/84'/1'/0'/0/*)#mdvjcv6a\"\n" +
    "            ],\n" +
    "            \"safe\": true\n" +
    "        },\n" +
    "        {\n" +
    "            \"txid\": \"d9c76eec25cda227b8b0d1a73545621ea7e8e76b99b21a78e5e19f8fc9e7a6d834a4\",\n" +
    "            \"vout\": 1,\n" +
    "            \"address\": \"tb1qmnx6x44nqjrflsmryc9knzdj9y0r0eh6kxnftw\",\n" +
    "            \"label\": \"\",\n" +
    "            \"scriptPubKey\": \"00148fda3b982d9d3b6c679dcedb0b7b9e8b0f9c4f0d\",\n" +
    "            \"amount\": 0.00425,\n" +
    "            \"confirmations\": 95108,\n" +
    "            \"spendable\": true,\n" +
    "            \"solvable\": true,\n" +
    "            \"desc\": \"wpkh([733d9ede/84h/1h/0h/0/1]03cf1e9e59444d3bb2a7b8d9b497b4a6b3f3d7eb47a4a8b5b1e4a5d5b7a8d8e5b3)#a19jkl0a\",\n" +
    "            \"parent_descs\": [\n" +
    "                \"wpkh(tpubD6NzVbkrYhZ4WYjWihxXYLeRBs4qm7afvuzYARdnjmWjCksjyU1ScokTe9dgMiDXfDy2MD1G3d7yuiknw7bQ96P4yvMG2QpX2ERDenTuo1c/84'/1'/0'/0/*)#mdvjcv6a\"\n" +
    "            ],\n" +
    "            \"safe\": true\n" +
    "        },\n" +
    "        {\n" +
    "            \"txid\": \"d9c76eec25cda227b8b0d1a732321ea7e8e76b99b21a78e5e19f8fc9e7a6d834a4\",\n" +
    "            \"vout\": 1,\n" +
    "            \"address\": \"tb1qmnx6x44nqjrflsmryc9knzdj9y0r0eh6kxnftw\",\n" +
    "            \"label\": \"\",\n" +
    "            \"scriptPubKey\": \"00148fda3b982d9d3b6c679dcedb0b7b9e8b0f9c4f0d\",\n" +
    "            \"amount\": 0.0025,\n" +
    "            \"confirmations\": 95108,\n" +
    "            \"spendable\": true,\n" +
    "            \"solvable\": true,\n" +
    "            \"desc\": \"wpkh([733d9ede/84h/1h/0h/0/1]03cf1e9e59444d3bb2a7b8d9b497b4a6b3f3d7eb47a4a8b5b1e4a5d5b7a8d8e5b3)#a19jkl0a\",\n" +
    "            \"parent_descs\": [\n" +
    "                \"wpkh(tpubD6NzVbkrYhZ4WYjWihxXYLeRBs4qm7afvuzYARdnjmWjCksjyU1ScokTe9dgMiDXfDy2MD1G3d7yuiknw7bQ96P4yvMG2QpX2ERDenTuo1c/84'/1'/0'/0/*)#mdvjcv6a\"\n" +
    "            ],\n" +
    "            \"safe\": true\n" +
    "        },\n" +
    "        {\n" +
    "            \"txid\": \"d9c76eec25cda227b8b0d143a7321ea7e8e76b99b21a78e5e19f8fc9e7a6d834a4\",\n" +
    "            \"vout\": 1,\n" +
    "            \"address\": \"tb1qmnx6x44nqjrflsmryc9knzdj9y0r0eh6kxnftw\",\n" +
    "            \"label\": \"\",\n" +
    "            \"scriptPubKey\": \"00148fda3b982d9d3b6c679dcedb0b7b9e8b0f9c4f0d\",\n" +
    "            \"amount\": 0.00235,\n" +
    "            \"confirmations\": 95108,\n" +
    "            \"spendable\": true,\n" +
    "            \"solvable\": true,\n" +
    "            \"desc\": \"wpkh([733d9ede/84h/1h/0h/0/1]03cf1e9e59444d3bb2a7b8d9b497b4a6b3f3d7eb47a4a8b5b1e4a5d5b7a8d8e5b3)#a19jkl0a\",\n" +
    "            \"parent_descs\": [\n" +
    "                \"wpkh(tpubD6NzVbkrYhZ4WYjWihxXYLeRBs4qm7afvuzYARdnjmWjCksjyU1ScokTe9dgMiDXfDy2MD1G3d7yuiknw7bQ96P4yvMG2QpX2ERDenTuo1c/84'/1'/0'/0/*)#mdvjcv6a\"\n" +
    "            ],\n" +
    "            \"safe\": true\n" +
    "        },\n" +
    "        {\n" +
    "            \"txid\": \"d9c76eec25cda227b8234b0d1a7321ea7e8e76b99b21a78e5e19f8fc9e7a6d834a4\",\n" +
    "            \"vout\": 1,\n" +
    "            \"address\": \"tb1qmnx6x44nqjrflsmryc9knzdj9y0r0eh6kxnftw\",\n" +
    "            \"label\": \"\",\n" +
    "            \"scriptPubKey\": \"00148fda3b982d9d3b6c679dcedb0b7b9e8b0f9c4f0d\",\n" +
    "            \"amount\": 0.001225,\n" +
    "            \"confirmations\": 95108,\n" +
    "            \"spendable\": true,\n" +
    "            \"solvable\": true,\n" +
    "            \"desc\": \"wpkh([733d9ede/84h/1h/0h/0/1]03cf1e9e59444d3bb2a7b8d9b497b4a6b3f3d7eb47a4a8b5b1e4a5d5b7a8d8e5b3)#a19jkl0a\",\n" +
    "            \"parent_descs\": [\n" +
    "                \"wpkh(tpubD6NzVbkrYhZ4WYjWihxXYLeRBs4qm7afvuzYARdnjmWjCksjyU1ScokTe9dgMiDXfDy2MD1G3d7yuiknw7bQ96P4yvMG2QpX2ERDenTuo1c/84'/1'/0'/0/*)#mdvjcv6a\"\n" +
    "            ],\n" +
    "            \"safe\": true\n" +
    "        },\n" +
    "        {\n" +
    "            \"txid\": \"d9c76eec2521cda227b8b0d1a7321ea7e8e76b99b21a78e5e19f8fc9e7a6d834a4\",\n" +
    "            \"vout\": 1,\n" +
    "            \"address\": \"tb1qmnx6x44nqjrflsmryc9knzdj9y0r0eh6kxnftw\",\n" +
    "            \"label\": \"\",\n" +
    "            \"scriptPubKey\": \"00148fda3b982d9d3b6c679dcedb0b7b9e8b0f9c4f0d\",\n" +
    "            \"amount\": 0.20025,\n" +
    "            \"confirmations\": 95108,\n" +
    "            \"spendable\": true,\n" +
    "            \"solvable\": true,\n" +
    "            \"desc\": \"wpkh([733d9ede/84h/1h/0h/0/1]03cf1e9e59444d3bb2a7b8d9b497b4a6b3f3d7eb47a4a8b5b1e4a5d5b7a8d8e5b3)#a19jkl0a\",\n" +
    "            \"parent_descs\": [\n" +
    "                \"wpkh(tpubD6NzVbkrYhZ4WYjWihxXYLeRBs4qm7afvuzYARdnjmWjCksjyU1ScokTe9dgMiDXfDy2MD1G3d7yuiknw7bQ96P4yvMG2QpX2ERDenTuo1c/84'/1'/0'/0/*)#mdvjcv6a\"\n" +
    "            ],\n" +
    "            \"safe\": true\n" +
    "        },\n" +
    "        {\n" +
    "            \"txid\": \"d9c76eec25cda227b8b2340d1a7321ea7e8e76b99b21a78e5e19f8fc9e7a6d834a4\",\n" +
    "            \"vout\": 1,\n" +
    "            \"address\": \"tb1qmnx6x44nqjrflsmryc9knzdj9y0r0eh6kxnftw\",\n" +
    "            \"label\": \"\",\n" +
    "            \"scriptPubKey\": \"00148fda3b982d9d3b6c679dcedb0b7b9e8b0f9c4f0d\",\n" +
    "            \"amount\": 02.0025,\n" +
    "            \"confirmations\": 95108,\n" +
    "            \"spendable\": true,\n" +
    "            \"solvable\": true,\n" +
    "            \"desc\": \"wpkh([733d9ede/84h/1h/0h/0/1]03cf1e9e59444d3bb2a7b8d9b497b4a6b3f3d7eb47a4a8b5b1e4a5d5b7a8d8e5b3)#a19jkl0a\",\n" +
    "            \"parent_descs\": [\n" +
    "                \"wpkh(tpubD6NzVbkrYhZ4WYjWihxXYLeRBs4qm7afvuzYARdnjmWjCksjyU1ScokTe9dgMiDXfDy2MD1G3d7yuiknw7bQ96P4yvMG2QpX2ERDenTuo1c/84'/1'/0'/0/*)#mdvjcv6a\"\n" +
    "            ],\n" +
    "            \"safe\": true\n" +
    "        },\n" +
    "        {\n" +
    "            \"txid\": \"d9c76eec25cda234234227b8b0d1a7321ea7e8e76b99b21a78e5e19f8fc9e7a6d834a4\",\n" +
    "            \"vout\": 1,\n" +
    "            \"address\": \"tb1qmnx6x44nqjrflsmryc9knzdj9y0r0eh6kxnftw\",\n" +
    "            \"label\": \"\",\n" +
    "            \"scriptPubKey\": \"00148fda3b982d9d3b6c679dcedb0b7b9e8b0f9c4f0d\",\n" +
    "            \"amount\": 0.0025,\n" +
    "            \"confirmations\": 95108,\n" +
    "            \"spendable\": true,\n" +
    "            \"solvable\": true,\n" +
    "            \"desc\": \"wpkh([733d9ede/84h/1h/0h/0/1]03cf1e9e59444d3bb2a7b8d9b497b4a6b3f3d7eb47a4a8b5b1e4a5d5b7a8d8e5b3)#a19jkl0a\",\n" +
    "            \"parent_descs\": [\n" +
    "                \"wpkh(tpubD6NzVbkrYhZ4WYjWihxXYLeRBs4qm7afvuzYARdnjmWjCksjyU1ScokTe9dgMiDXfDy2MD1G3d7yuiknw7bQ96P4yvMG2QpX2ERDenTuo1c/84'/1'/0'/0/*)#mdvjcv6a\"\n" +
    "            ],\n" +
    "            \"safe\": true\n" +
    "        },\n" +
    "        {\n" +
    "            \"txid\": \"d9c76eec25cda2324527b8b0d1a7321ea7e8e76b99b21a78e5e19f8fc9e7a6d834a4\",\n" +
    "            \"vout\": 1,\n" +
    "            \"address\": \"tb1qmnx6x44nqjrflsmryc9knzdj9y0r0eh6kxnftw\",\n" +
    "            \"label\": \"\",\n" +
    "            \"scriptPubKey\": \"00148fda3b982d9d3b6c679dcedb0b7b9e8b0f9c4f0d\",\n" +
    "            \"amount\": 0.20025,\n" +
    "            \"confirmations\": 95108,\n" +
    "            \"spendable\": true,\n" +
    "            \"solvable\": true,\n" +
    "            \"desc\": \"wpkh([733d9ede/84h/1h/0h/0/1]03cf1e9e59444d3bb2a7b8d9b497b4a6b3f3d7eb47a4a8b5b1e4a5d5b7a8d8e5b3)#a19jkl0a\",\n" +
    "            \"parent_descs\": [\n" +
    "                \"wpkh(tpubD6NzVbkrYhZ4WYjWihxXYLeRBs4qm7afvuzYARdnjmWjCksjyU1ScokTe9dgMiDXfDy2MD1G3d7yuiknw7bQ96P4yvMG2QpX2ERDenTuo1c/84'/1'/0'/0/*)#mdvjcv6a\"\n" +
    "            ],\n" +
    "            \"safe\": true\n" +
    "        },\n" +
    "        {\n" +
    "            \"txid\": \"d9c76eec25cda22342327b8b0d1a7321ea7e8e76b99b21a78e5e19f8fc9e7a6d834a4\",\n" +
    "            \"vout\": 1,\n" +
    "            \"address\": \"tb1qmnx6x44nqjrflsmryc9knzdj9y0r0eh6kxnftw\",\n" +
    "            \"label\": \"\",\n" +
    "            \"scriptPubKey\": \"00148fda3b982d9d3b6c679dcedb0b7b9e8b0f9c4f0d\",\n" +
    "            \"amount\": 0.220025,\n" +
    "            \"confirmations\": 95108,\n" +
    "            \"spendable\": true,\n" +
    "            \"solvable\": true,\n" +
    "            \"desc\": \"wpkh([733d9ede/84h/1h/0h/0/1]03cf1e9e59444d3bb2a7b8d9b497b4a6b3f3d7eb47a4a8b5b1e4a5d5b7a8d8e5b3)#a19jkl0a\",\n" +
    "            \"parent_descs\": [\n" +
    "                \"wpkh(tpubD6NzVbkrYhZ4WYjWihxXYLeRBs4qm7afvuzYARdnjmWjCksjyU1ScokTe9dgMiDXfDy2MD1G3d7yuiknw7bQ96P4yvMG2QpX2ERDenTuo1c/84'/1'/0'/0/*)#mdvjcv6a\"\n" +
    "            ],\n" +
    "            \"safe\": true\n" +
    "        },\n" +
    "        {\n" +
    "            \"txid\": \"c7813e4c12b4b8b6b1234452f4e8b1cdb4b8f5c4a7b8b1f4d8a7b6b9e2f4b8d6c7e8f5\",\n" +
    "            \"vout\": 0,\n" +
    "            \"address\": \"tb1q2w5jl5c9x5zcl6xz7a4kxz6yx4l5k8z6f9x5gk\",\n" +
    "            \"label\": \"\",\n" +
    "            \"scriptPubKey\": \"0014a1bc4b7e8c471d8c9e7c7f8d9e8f7c4b8e9f6c4d\",\n" +
    "            \"amount\": 0.00336,\n" +
    "            \"confirmations\": 95108,\n" +
    "            \"spendable\": true,\n" +
    "            \"solvable\": true,\n" +
    "            \"desc\": \"wpkh([733d9ede/84h/1h/0h/0/2]02cf4e9e5948d7bb2a6b8d8b497f4a6b3d3d7eb47a7a8b5b1e4b5d7a8d8e6b5c3)#b2k3jl1c\",\n" +
    "            \"parent_descs\": [\n" +
    "                \"wpkh(tpubD6NzVbkrYhZ4WYjWihxXYLeRBs4qm7afvuzYARdnjmWjCksjyU1ScokTe9dgMiDXfDy2MD1G3d7yuiknw7bQ96P4yvMG2QpX2ERDenTuo1c/84'/1'/0'/0/*)#mdvjcv6a\"\n" +
    "            ],\n" +
    "            \"safe\": true\n" +
    "        }\n" +
    "    ],\n" +
    "    \"error\": null,\n" +
    "    \"id\": \"curltest\"\n" +
    "}"
