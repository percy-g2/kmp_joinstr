package invincible.privacy.joinstr.network

import invincible.privacy.joinstr.getWebSocketClient
import invincible.privacy.joinstr.model.NostrEvent
import invincible.privacy.joinstr.theme.Settings
import invincible.privacy.joinstr.theme.SettingsManager
import invincible.privacy.joinstr.utils.Event
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class NostrClient {
    private val nostrRelay = suspend {
        SettingsManager.store.get()?.nostrRelay ?: Settings().nostrRelay
    }

    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = lazy { getWebSocketClient() }
    private var wsSession: DefaultClientWebSocketSession? = null
    private val mutex = Mutex()

    suspend fun fetchOtherPools(
        onReceived: (List<NostrEvent>?) -> Unit
    ) {
        mutex.withLock {
            runCatching {
                if (wsSession?.isActive != true) {
                    wsSession = client.value.webSocketSession(nostrRelay.invoke())
                }

                wsSession?.let { session ->
                    var events: List<NostrEvent> = emptyList()
                    val subscribeMessage = """["REQ", "my-events", {"kinds": [${Event.TEST_JOIN_STR.kind}]}]"""
                    session.send(Frame.Text(subscribeMessage))

                    for (frame in session.incoming) {
                        if (frame is Frame.Text) {
                            println(frame.readText())
                            val elem = json.parseToJsonElement(frame.readText()).jsonArray
                            if (elem[0].jsonPrimitive.content == "EVENT") {
                                events = listOf(json.decodeFromJsonElement<NostrEvent>(elem[2])) + events
                            }
                            if (elem[0].jsonPrimitive.content == "EOSE") {
                                onReceived(events)
                                break
                            }
                        }
                    }
                } ?: run {
                    throw IllegalStateException("Failed to establish WebSocket connection")
                }
            }.getOrElse { error ->
                error.printStackTrace()
                onReceived(emptyList())
                closeSession()
            }
        }
    }

    suspend fun sendEvent(
        event: NostrEvent,
        onSuccess: () -> Unit,
        onError: (String?) -> Unit
    ) {
        mutex.withLock {
            runCatching {
                if (wsSession?.isActive != true) {
                    wsSession = client.value.webSocketSession(nostrRelay.invoke())
                }

                wsSession?.let { session ->
                    val eventJson = json.encodeToString(event)
                    val sendMessage = """["EVENT", $eventJson]"""
                    session.send(Frame.Text(sendMessage))

                    println("Sent event: $sendMessage")

                    for (frame in session.incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val response = frame.readText()
                                println("Received raw response: $response")
                                try {
                                    val responseArray = json.decodeFromString<List<JsonElement>>(response)
                                    println("Parsed response: $responseArray")
                                    when (responseArray[0].jsonPrimitive.content) {
                                        "OK" -> {
                                            if (responseArray[2].jsonPrimitive.boolean) {
                                                onSuccess()
                                                println("Event accepted with ID: ${responseArray[1].jsonPrimitive.content}")
                                            } else {
                                                onError("Error: ${responseArray[3].jsonPrimitive.content}")
                                            }
                                            break
                                        }
                                        "NOTICE" -> {
                                            onError("Received notice: ${responseArray[1].jsonPrimitive.content}")
                                        }
                                        else -> {
                                            onError("Unexpected response type: ${responseArray[0].jsonPrimitive.content}")
                                        }
                                    }
                                    if (responseArray.size > 2) {
                                        onError("Additional information: ${responseArray.subList(2, responseArray.size)}")
                                    }
                                } catch (e: Exception) {
                                    onError("Failed to parse response: ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                            else -> {
                                onError("Received non-text frame: $frame")
                            }
                        }
                    }
                } ?: run {
                    onError("Failed to establish WebSocket connection")
                    throw IllegalStateException("Failed to establish WebSocket connection")
                }
            }.getOrElse { error ->
                error.printStackTrace()
                onError(error.message)
                closeSession()
            }
        }
    }

    private suspend fun closeSession() {
        wsSession?.close()
        wsSession = null
    }

    suspend fun close() {
        mutex.withLock {
            closeSession()
        }
    }
}