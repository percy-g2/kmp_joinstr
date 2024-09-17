package invincible.privacy.joinstr.network

import invincible.privacy.joinstr.getPoolsStore
import invincible.privacy.joinstr.getSharedSecret
import invincible.privacy.joinstr.getWebSocketClient
import invincible.privacy.joinstr.ktx.hexToByteArray
import invincible.privacy.joinstr.ktx.toHexString
import invincible.privacy.joinstr.model.Credentials
import invincible.privacy.joinstr.model.JoinedPoolContent
import invincible.privacy.joinstr.model.NostrEvent
import invincible.privacy.joinstr.model.PoolContent
import invincible.privacy.joinstr.utils.Event
import invincible.privacy.joinstr.utils.NostrCryptoUtils.createEvent
import invincible.privacy.joinstr.utils.NostrCryptoUtils.decrypt
import invincible.privacy.joinstr.utils.NostrCryptoUtils.encrypt
import invincible.privacy.joinstr.utils.SettingsManager
import invincible.privacy.joinstr.utils.SettingsStore
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

open class NostrClient {
    private val nostrRelay
        get() = suspend {
            SettingsManager.store.get()?.nostrRelay ?: SettingsStore().nostrRelay
        }

    val json = Json {
        explicitNulls = false
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = lazy { getWebSocketClient() }
    private var wsSession: DefaultClientWebSocketSession? = null
    private var wsActivePoolsCredentialsSender: DefaultClientWebSocketSession? = null

    suspend fun fetchOtherPools(
        onSuccess: (List<PoolContent>) -> Unit,
        onError: (String?) -> Unit
    ) {
        runCatching {
            if (wsSession?.isActive != true) {
                wsSession = client.value.webSocketSession(nostrRelay.invoke())
            }

            wsSession?.let { session ->
                var events: List<PoolContent> = emptyList()
                val subscribeMessage = """["REQ", "my-events", {"kinds": [${Event.JOIN_STR.kind}]}]"""
                session.send(Frame.Text(subscribeMessage))

                for (frame in session.incoming) {
                    if (frame is Frame.Text) {
                        println(frame.readText())
                        val elem = json.parseToJsonElement(frame.readText()).jsonArray
                        if (elem[0].jsonPrimitive.content == "EVENT") {
                            runCatching {
                                val nostrEventContent = json.decodeFromJsonElement<NostrEvent>(elem[2]).content
                                val event = json.decodeFromString<PoolContent>(nostrEventContent)
                                events = listOf(event) + events
                            }.getOrElse {
                                it.printStackTrace()
                                closeSession()
                            }
                        }
                        if (elem[0].jsonPrimitive.content == "EOSE") {
                            onSuccess(events)
                            closeSession()
                            break
                        }
                    }
                }
            } ?: run {
                onError("Failed to establish WebSocket connection")
                closeSession()
                throw IllegalStateException("Failed to establish WebSocket connection")
            }
        }.getOrElse { error ->
            error.printStackTrace()
            onError(error.message)
            closeSession()
        }
    }


    suspend fun sendEvent(
        event: NostrEvent,
        onSuccess: (String) -> Unit,
        onError: (String?) -> Unit
    ) {
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
                                            onSuccess(responseArray[1].jsonPrimitive.content)
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
                                closeSession()
                            } catch (e: Exception) {
                                onError("Failed to parse response: ${e.message}")
                                e.printStackTrace()
                                closeSession()
                            }
                        }

                        else -> {
                            onError("Received non-text frame: $frame")
                            closeSession()
                        }
                    }
                }
            } ?: run {
                onError("Failed to establish WebSocket connection")
                closeSession()
                throw IllegalStateException("Failed to establish WebSocket connection")
            }
        }.getOrElse { error ->
            error.printStackTrace()
            onError(error.message)
            closeSession()
        }
    }

    suspend fun checkRegisteredInputs(
        publicKey: ByteArray,
        privateKey: ByteArray,
        intervalSeconds: Long = 30,
        onSuccess: (List<JoinedPoolContent>) -> Unit,
        onError: (String?) -> Unit
    ) {
        runCatching {
            if (wsSession?.isActive != true) {
                wsSession = client.value.webSocketSession(nostrRelay.invoke())
            }
            val sharedSecret = getSharedSecret(privateKey, publicKey)
            wsSession?.let { session ->
                withContext(Dispatchers.Default) {
                    while (isActive) {
                        var registeredAddressList: MutableList<JoinedPoolContent> = mutableListOf()
                        val poolPublicKey = publicKey.toHexString()
                        val subscribeMessage =
                            """["REQ", "Join-Channel", {"kinds": [${Event.ENCRYPTED_DIRECT_MESSAGE.kind}],"#p":["$poolPublicKey"]}]""".trimMargin()
                        launch {
                            registeredAddressList = mutableListOf()
                            session.send(Frame.Text(subscribeMessage))
                            println("Sent message: $subscribeMessage")
                        }

                        val responseJob = launch {
                            for (frame in session.incoming) {
                                if (frame is Frame.Text) {
                                    val response = frame.readText()
                                    println("Received response: $response")
                                    if (response.contains(poolPublicKey)) {
                                        runCatching {
                                            val elem = json.parseToJsonElement(frame.readText()).jsonArray
                                            if (elem[0].jsonPrimitive.content == "EVENT") {
                                                val activePools = getPoolsStore().get()
                                                    ?.sortedByDescending { it.timeout }
                                                    ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }
                                                val totalPeers = activePools?.find { it.publicKey == poolPublicKey }?.peers ?: 0
                                                val nostrEvent = json.decodeFromJsonElement<NostrEvent>(elem[2])
                                                val decryptedContent = decrypt(nostrEvent.content, sharedSecret)
                                                val registeredAddress = json.decodeFromString<JoinedPoolContent>(decryptedContent)
                                                if (registeredAddress.type == "input") {
                                                    registeredAddressList += registeredAddress
                                                    if (registeredAddressList.filter { it.type == "input" }.distinctBy { it.hex }.size ==
                                                        totalPeers) {
                                                        onSuccess(registeredAddressList.toList())
                                                        closeSession()
                                                    }
                                                }
                                                // this@withContext.cancel()
                                            }
                                        }.getOrElse {
                                            it.printStackTrace()
                                        }
                                    } else {
                                        println("waiting for response from server")
                                    }
                                }
                            }
                        }

                        delay(intervalSeconds * 1000)
                        responseJob.cancel()
                    }
                }
            } ?: run {
                onError("Failed to establish WebSocket connection")
                closeSession()
                throw IllegalStateException("Failed to establish WebSocket connection")
            }
        }.getOrElse { error ->
            error.printStackTrace()
            onError(error.message)
            closeSession()
        }
    }

    suspend fun checkRegisteredOutputs(
        publicKey: ByteArray,
        privateKey: ByteArray,
        intervalSeconds: Long = 30,
        onSuccess: (List<JoinedPoolContent>) -> Unit,
        onError: (String?) -> Unit
    ) {
        runCatching {
            if (wsSession?.isActive != true) {
                wsSession = client.value.webSocketSession(nostrRelay.invoke())
            }
            val sharedSecret = getSharedSecret(privateKey, publicKey)
            wsSession?.let { session ->
                withContext(Dispatchers.Default) {
                    while (isActive) {
                        var registeredAddressList: MutableList<JoinedPoolContent> = mutableListOf()
                        val poolPublicKey = publicKey.toHexString()
                        val subscribeMessage =
                            """["REQ", "Join-Channel", {"kinds": [${Event.ENCRYPTED_DIRECT_MESSAGE.kind}],"#p":["$poolPublicKey"]}]""".trimMargin()
                        launch {
                            registeredAddressList = mutableListOf()
                            session.send(Frame.Text(subscribeMessage))
                            println("Sent message: $subscribeMessage")
                        }

                        val responseJob = launch {
                            for (frame in session.incoming) {
                                if (frame is Frame.Text) {
                                    val response = frame.readText()
                                    println("Received response: $response")
                                    if (response.contains(poolPublicKey)) {
                                        runCatching {
                                            val elem = json.parseToJsonElement(frame.readText()).jsonArray
                                            if (elem[0].jsonPrimitive.content == "EVENT") {
                                                val activePools = getPoolsStore().get()
                                                    ?.sortedByDescending { it.timeout }
                                                    ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }
                                                val totalPeers = activePools?.find { it.publicKey == poolPublicKey }?.peers ?: 0
                                                val nostrEvent = json.decodeFromJsonElement<NostrEvent>(elem[2])
                                                val decryptedContent = decrypt(nostrEvent.content, sharedSecret)
                                                val registeredAddress = json.decodeFromString<JoinedPoolContent>(decryptedContent)
                                                if (registeredAddress.type == "output") {
                                                    registeredAddressList += registeredAddress
                                                    if (registeredAddressList.filter { it.type == "output" }.distinctBy { it.address }.size == totalPeers) {
                                                        onSuccess(registeredAddressList.toList())
                                                        closeSession()
                                                    }
                                                }
                                                // this@withContext.cancel()
                                            }
                                        }.getOrElse {
                                            it.printStackTrace()
                                        }
                                    } else {
                                        println("waiting for response from server")
                                    }
                                }
                            }
                        }

                        delay(intervalSeconds * 1000)
                        responseJob.cancel()
                    }
                }
            } ?: run {
                onError("Failed to establish WebSocket connection")
                closeSession()
                throw IllegalStateException("Failed to establish WebSocket connection")
            }
        }.getOrElse { error ->
            error.printStackTrace()
            onError(error.message)
            closeSession()
        }
    }

    suspend fun requestPoolCredentials(
        requestPublicKey: String,
        intervalSeconds: Long = 30,
        onSuccess: (NostrEvent) -> Unit,
        onError: (String?) -> Unit
    ) {
        runCatching {
            if (wsSession?.isActive != true) {
                wsSession = client.value.webSocketSession(nostrRelay.invoke())
            }

            wsSession?.let { session ->
                withContext(Dispatchers.Default) {
                    while (isActive) {
                        val subscribeMessage =
                            """["REQ", "Join-Channel", {"kinds": [${Event.ENCRYPTED_DIRECT_MESSAGE.kind}],"#p":["$requestPublicKey"]}]""".trimMargin()
                        launch {
                            session.send(Frame.Text(subscribeMessage))
                            println("Sent message: $subscribeMessage")
                        }

                        val responseJob = launch {
                            for (frame in session.incoming) {
                                if (frame is Frame.Text) {
                                    val response = frame.readText()
                                    println("Received response: $response")
                                    if (response.contains(requestPublicKey)) {
                                        runCatching {
                                            val elem = json.parseToJsonElement(frame.readText()).jsonArray
                                            if (elem[0].jsonPrimitive.content == "EVENT") {
                                                val nostrEvent = json.decodeFromJsonElement<NostrEvent>(elem[2])
                                                onSuccess(nostrEvent)
                                                closeSession()
                                                // this@withContext.cancel()
                                            }
                                        }.getOrElse {
                                            it.printStackTrace()
                                        }
                                    } else {
                                        println("waiting for response from server")
                                    }
                                }
                            }
                        }

                        delay(intervalSeconds * 1000)
                        responseJob.cancel()
                    }
                }
            } ?: run {
                onError("Failed to establish WebSocket connection")
                closeSession()
                throw IllegalStateException("Failed to establish WebSocket connection")
            }
        }.getOrElse { error ->
            error.printStackTrace()
            onError(error.message)
            closeSession()
        }
    }

    suspend fun activePoolsCredentialsSender() {
        runCatching {
            if (wsActivePoolsCredentialsSender?.isActive != true) {
                wsActivePoolsCredentialsSender = client.value.webSocketSession(nostrRelay.invoke())
            }

            wsActivePoolsCredentialsSender?.let { session ->
                withContext(Dispatchers.Default) {
                    while (isActive) {
                        var registeredAddressList: MutableList<JoinedPoolContent> = mutableListOf()
                        val activePools = getPoolsStore().get()?.sortedByDescending { it.timeout }
                            ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }
                        val activePoolsPublicKeys = activePools?.map { it.publicKey } ?: emptyList()
                        val formattedKeys = activePoolsPublicKeys.joinToString(
                            separator = ", ",
                            prefix = "[",
                            postfix = "]",
                        ){ "\"$it\"" }
                        val subscribeMessage = """["REQ", "my-events", {"kinds": [${Event.ENCRYPTED_DIRECT_MESSAGE.kind}],
                            |"#p":$formattedKeys}]""".trimMargin()
                        launch {
                            if (activePoolsPublicKeys.isNotEmpty()) {
                                registeredAddressList = mutableListOf()
                                session.send(Frame.Text(subscribeMessage))
                                println("Sent message: $subscribeMessage")
                            }
                        }

                        val responseJob = launch {
                            for (frame in session.incoming) {
                                if (frame is Frame.Text) {
                                    val response = frame.readText()
                                    println("Received response: $response")
                                    runCatching {
                                        val elem = json.parseToJsonElement(frame.readText()).jsonArray
                                        if (elem[0].jsonPrimitive.content == "EVENT") {
                                            val nostrEvent = json.decodeFromJsonElement<NostrEvent>(elem[2])
                                            val poolPublicKey = findMatchingPublicKey(activePoolsPublicKeys, nostrEvent.tags)
                                            if (poolPublicKey != null) {
                                                getPoolsStore().get().orEmpty().find { it.publicKey == poolPublicKey }?.let { pool ->
                                                    val privateKey = pool.privateKey.hexToByteArray()
                                                    val publicKey = pool.publicKey.hexToByteArray()
                                                    runCatching {
                                                        val sharedSecret = getSharedSecret(
                                                            privateKey = privateKey,
                                                            pubKey = publicKey
                                                        )
                                                        val totalPeers = activePools?.find { it.publicKey == poolPublicKey }?.peers ?: 0
                                                        val decryptedContent = decrypt(nostrEvent.content, sharedSecret)
                                                        val registeredAddress = json.decodeFromString<JoinedPoolContent>(decryptedContent)
                                                        registeredAddressList += registeredAddress
                                                        println("registeredAddressList >> " + registeredAddressList.joinToString(","))
                                                        if (registeredAddressList.size == totalPeers && pool.peersData.isEmpty()) {
                                                            getPoolsStore().update { existingPools ->
                                                                existingPools?.map {
                                                                    if (it.id == pool.id) {
                                                                        it.copy(peersData = registeredAddressList)
                                                                    } else it
                                                                } ?: emptyList()
                                                            }
                                                            println("registeredAddressList >> Updated")
                                                        }
                                                    }.getOrElse {
                                                        it.printStackTrace()
                                                    }

                                                    if (nostrEvent.pubKey != pool.publicKey &&
                                                        pool.peersPublicKeys.contains(nostrEvent.pubKey).not()
                                                    ) {
                                                        val credentials = Credentials(
                                                            id = pool.id,
                                                            publicKey = pool.publicKey,
                                                            denomination = pool.denomination,
                                                            peers = pool.peers,
                                                            timeout = pool.timeout,
                                                            relay = pool.relay,
                                                            feeRate = pool.feeRate,
                                                            privateKey = pool.privateKey
                                                        )
                                                        val data = json.encodeToString(credentials)
                                                        val sharedSecret = getSharedSecret(privateKey, nostrEvent.pubKey.hexToByteArray())
                                                        val encryptedMessage = encrypt(data, sharedSecret)
                                                        val credentialEvent = createEvent(
                                                            content = encryptedMessage,
                                                            event = Event.ENCRYPTED_DIRECT_MESSAGE,
                                                            privateKey = privateKey,
                                                            publicKey = publicKey,
                                                            tagPubKey = nostrEvent.pubKey
                                                        )
                                                        sendEvent(
                                                            event = credentialEvent,
                                                            onSuccess = {
                                                                println("Credentials sent successfully")
                                                                CoroutineScope(Dispatchers.Default).launch {
                                                                    getPoolsStore().update { existingPools ->
                                                                        existingPools?.map {
                                                                            if (it.id == pool.id) {
                                                                                it.copy(peersPublicKeys = pool.peersPublicKeys.plus(nostrEvent.pubKey))
                                                                            } else it
                                                                        } ?: emptyList()
                                                                    }
                                                                }
                                                            },
                                                            onError = { error ->
                                                                println("Error sending credentials: $error")
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }.getOrElse {
                                        it.printStackTrace()
                                    }
                                }
                            }
                        }

                        delay(30 * 1000) // 30 secs repeat logic
                        responseJob.cancel()
                    }
                }
            } ?: run {
                throw IllegalStateException("Failed to establish WebSocket connection")
            }
        }.getOrElse { error ->
            error.printStackTrace()
        }
    }

    private suspend fun closeSession() {
        wsSession?.close()
        wsSession = null
    }

    suspend fun close() {
        closeSession()
    }

    private fun findMatchingPublicKey(publicKeysList: List<String>, tags: List<List<String>>): String? {
        return publicKeysList.find { publicKey ->
            tags.any { tag ->
                tag.size == 2 && tag[0] == "p" && tag[1] == publicKey
            }
        }
    }
}