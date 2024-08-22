package invincible.privacy.joinstr.network

import invincible.privacy.joinstr.model.NostrEvent
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.seconds

class NostrClient {
    private val _events = MutableStateFlow<List<NostrEvent>>(emptyList())
    val events: StateFlow<List<NostrEvent>> = _events

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client =
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 5.seconds.inWholeMilliseconds
                connectTimeoutMillis = 5.seconds.inWholeMilliseconds
                socketTimeoutMillis = 5.seconds.inWholeMilliseconds
            }
            install(WebSockets)
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.ALL
            }
        }

    suspend fun connectAndListen(onReceived: () -> Unit) {
        client.wss("wss://nos.lol") {
            val subscribeMessage = """["REQ", "my-sub", {"kinds": [2022], "limit": 1000}]"""
            send(Frame.Text(subscribeMessage))
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    runCatching {
                        val elem = json.parseToJsonElement(frame.readText()).jsonArray
                        if (elem[0].jsonPrimitive.content == "EVENT") {
                            _events.update {
                                listOf(json.decodeFromJsonElement<NostrEvent>(elem[2])) + it
                            }
                        }
                        if (elem[0].jsonPrimitive.content == "EOSE") {
                            onReceived.invoke()
                        }
                    }.getOrElse {
                        it.printStackTrace()
                    }
                }
            }
        }
    }
}