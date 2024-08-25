package invincible.privacy.joinstr.network

import invincible.privacy.joinstr.getWebSocketClient
import invincible.privacy.joinstr.model.NostrEvent
import invincible.privacy.joinstr.theme.Settings
import invincible.privacy.joinstr.theme.SettingsManager
import invincible.privacy.joinstr.utils.Event
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class NostrClient {
    private val _events = MutableStateFlow<List<NostrEvent>?>(null)
    val events: StateFlow<List<NostrEvent>?> = _events
    private val nostrRelay = suspend {
        SettingsManager.store.get()?.nostrRelay ?: Settings().nostrRelay
    }

    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = lazy { getWebSocketClient() }

    suspend fun connectAndListen(
        onReceived: () -> Unit
    ) {
        runCatching {
            client.value.wss(nostrRelay.invoke()) {
                val subscribeMessage = """["REQ", "my-events", {"kinds": [${Event.TEST_JOIN_STR.kind}], "limit": 1000}]"""
                send(Frame.Text(subscribeMessage))
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        println(frame.readText())
                        val elem = json.parseToJsonElement(frame.readText()).jsonArray
                        if (elem[0].jsonPrimitive.content == "EVENT") {
                            _events.update { list ->
                                list?.let {
                                    listOf(json.decodeFromJsonElement<NostrEvent>(elem[2])) + it
                                } ?: listOf(json.decodeFromJsonElement<NostrEvent>(elem[2]))
                            }
                        }
                        if (elem[0].jsonPrimitive.content == "EOSE") {
                            if (_events.value == null) {
                                _events.value = emptyList()
                            }
                            onReceived.invoke()
                        }
                    }
                }
            }
        }.getOrElse {
            _events.value = emptyList()
            it.printStackTrace()
            onReceived.invoke()
        }
    }

    suspend fun sendEvent(
        event: NostrEvent,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        client.value.wss(nostrRelay.invoke()) {
            val eventJson = json.encodeToString(event)
            val sendMessage = """["EVENT", $eventJson]"""
            send(Frame.Text(sendMessage))

            println("Sent event: $sendMessage")

            for (frame in incoming) {
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
                                        onSuccess.invoke()
                                        println("Event accepted with ID: ${responseArray[1].jsonPrimitive.content}")
                                    } else {
                                        onError.invoke()
                                        println("Error: ${responseArray[3].jsonPrimitive.content}")
                                    }
                                    break
                                }
                                "NOTICE" -> {
                                    onError.invoke()
                                    println("Received notice: ${responseArray[1].jsonPrimitive.content}")
                                }
                                else -> {
                                    onError.invoke()
                                    println("Unexpected response type: ${responseArray[0].jsonPrimitive.content}")
                                }
                            }
                            if (responseArray.size > 2) {
                                onError.invoke()
                                println("Additional information: ${responseArray.subList(2, responseArray.size)}")
                            }
                        } catch (e: Exception) {
                            onError.invoke()
                            println("Failed to parse response: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                    else -> {
                        onError.invoke()
                        println("Received non-text frame: $frame")
                    }
                }
            }
        }
    }
}