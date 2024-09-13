package invincible.privacy.joinstr

import fr.acinq.bitcoin.Bitcoin
import fr.acinq.bitcoin.Block
import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.OutPoint
import fr.acinq.bitcoin.Satoshi
import fr.acinq.bitcoin.Script
import fr.acinq.bitcoin.SigHash.SIGHASH_ALL
import fr.acinq.bitcoin.SigHash.SIGHASH_ANYONECANPAY
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.TxId
import fr.acinq.bitcoin.TxIn
import fr.acinq.bitcoin.TxOut
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.psbt.UpdateFailure
import fr.acinq.bitcoin.utils.Either
import fr.acinq.secp256k1.Hex
import fr.acinq.secp256k1.Secp256k1
import fr.acinq.secp256k1.Secp256k1.Companion.pubKeyTweakMul
import invincible.privacy.joinstr.model.ListUnspentResponseItem
import invincible.privacy.joinstr.model.LocalPoolContent
import invincible.privacy.joinstr.ui.components.SnackbarController
import invincible.privacy.joinstr.utils.NodeConfig
import invincible.privacy.joinstr.utils.SettingsStore
import invincible.privacy.joinstr.utils.Theme
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Clock
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDecimalNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dateByAddingTimeInterval
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.seconds

actual fun getSettingsStore(): KStore<SettingsStore> {
    val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    return storeOf<SettingsStore>(
        file = "${paths.firstOrNull() as? String}/settings.json".toPath(),
        default = SettingsStore(
            selectedTheme = Theme.SYSTEM.id,
            nodeConfig = NodeConfig()
        )
    )
}

actual fun getPoolsStore(): KStore<List<LocalPoolContent>> {
    val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    return storeOf<List<LocalPoolContent>>(
        file = "${paths.firstOrNull() as? String}/pools.json".toPath(),
        default = emptyList()
    )
}

actual fun Float.convertFloatExponentialToString(): String {
    val decimalNumber = NSDecimalNumber(string = toString())
    return decimalNumber.stringValue
}

actual fun getWebSocketClient(): HttpClient {
    return HttpClient(Darwin) {
        install(WebSockets)

        install(HttpTimeout) {
            requestTimeoutMillis = 5.seconds.inWholeMilliseconds
            connectTimeoutMillis = 5.seconds.inWholeMilliseconds
            socketTimeoutMillis = 5.seconds.inWholeMilliseconds
        }

        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
    }
}

class NotificationDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
    ) {
        withCompletionHandler(UNNotificationPresentationOptionBanner or
            UNNotificationPresentationOptionSound or
            UNNotificationPresentationOptionList or
            UNAuthorizationOptionBadge
        )
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit
    ) {
        println("Received notification response: ${didReceiveNotificationResponse.notification.request.identifier}")
        withCompletionHandler()
    }
}

actual object LocalNotification {
    private val delegate = NotificationDelegate()

    init {
        UNUserNotificationCenter.currentNotificationCenter().delegate = delegate
    }

    actual fun showNotification(title: String, message: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(message)
            setSound(UNNotificationSound.defaultSound())
            setBadge(badge = null)
        }

        val uuid = NSUUID.UUID().UUIDString
        val date = NSDate().dateByAddingTimeInterval(1.0)
        val components = NSCalendar.currentCalendar.components(
            NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
                NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond,
            date
        )
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, false)

        val request = UNNotificationRequest.requestWithIdentifier(
            uuid,
            content,
            trigger
        )

        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { requestError ->
            if (requestError != null) {
                println("Error showing notification: ${requestError.localizedDescription}")
            } else {
                println("Notification scheduled successfully with ID: $uuid")
            }
        }
    }

    actual suspend fun requestPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { granted, error ->
            if (error != null) {
                println("Error requesting notification permission: ${error.localizedDescription}")
            }
            continuation.resume(granted)
        }
    }
}

actual fun pubkeyCreate(privateKey: ByteArray): ByteArray {
    if (!Secp256k1.secKeyVerify(privateKey)) throw Exception("Invalid private key!")
    val pubKey = Secp256k1.pubkeyCreate(privateKey).drop(1).take(32).toByteArray()
    //context.cleanup()
    return pubKey
}

actual suspend fun signSchnorr(content: ByteArray, privateKey: ByteArray, freshRandomBytes: ByteArray): ByteArray {
    val contentSignature = Secp256k1.signSchnorr(content, privateKey, freshRandomBytes)
    return contentSignature
}

actual fun getSharedSecret(privateKey: ByteArray, pubKey: ByteArray): ByteArray =
    pubKeyTweakMul(Hex.decode("02") + pubKey, privateKey).copyOfRange(1, 33)

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun createPsbt(
    poolId: String,
    unspentItem: ListUnspentResponseItem
): String? {
    return runCatching {
        val activePools = getPoolsStore().get()
            ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }
            ?.sortedByDescending { it.timeout }

        val selectedPool = activePools?.find { it.id == poolId }
            ?: throw IllegalStateException("Selected pool not found")

        val poolAmount = selectedPool.denomination
        val selectedTxAmount = unspentItem.amount
        val estimatedVByteSize = 100 * selectedPool.peers
        val estimatedBtcFee = (selectedPool.feeRate.toFloat() * estimatedVByteSize.toFloat()) / 100000000

        val outputAmount = poolAmount - estimatedBtcFee
        val sighashType = SIGHASH_ALL or SIGHASH_ANYONECANPAY

        val input = TxIn(
            outPoint = OutPoint(TxId(unspentItem.txid), unspentItem.vout.toLong()),
            sequence = 0xFFFFFFFFL,
            signatureScript = listOf()
        )

        val outputs = selectedPool.peersData
            .filter { it.type == "output" }
            .sortedWith(compareBy { it.address })
            .mapNotNull { peerData ->
                Bitcoin.addressToPublicKeyScript(Block.SignetGenesisBlock.hash, peerData.address ?: "").fold(
                    { error ->
                        println("Error creating output script for address ${peerData.address}: ${error.message}")
                        null
                    },
                    { scriptElts ->
                        TxOut(
                            amount = Satoshi((outputAmount * 100_000_000).toLong()),
                            publicKeyScript = ByteVector(Script.write(scriptElts))
                        )
                    }
                )
            }

        if (!((poolAmount * 100_000_000) + 500 <= selectedTxAmount * 100_000_000 &&
                selectedTxAmount * 100_000_000 <= (poolAmount * 100_000_000) + 5000)
        ) {
            SnackbarController.showMessage(
                "Error: Selected input value is not within the specified range for this pool " +
                    "(denomination: $poolAmount BTC)"
            )
        }

        val transaction = Transaction(
            version = 1L,
            txIn = listOf(input),
            txOut = outputs,
            lockTime = 0
        )

        val unsignedPsbt = Psbt(transaction)

        val updatedPsbt = unsignedPsbt.updateWitnessInput(
            outPoint = input.outPoint,
            txOut = TxOut(
                Satoshi((selectedTxAmount * 100_000_000).toLong()),
                ByteVector(unspentItem.scriptPubKey)
            ),
            sighashType = sighashType
        ).right ?: throw IllegalStateException("Failed to update PSBT")

        // Encode PSBT to Base64
        val psbtBytes = Psbt.write(updatedPsbt)
        val psbtBase64 = Base64.encode(psbtBytes.toByteArray())

        return psbtBase64
    }.getOrElse {
        println("createPsbt")
        it.printStackTrace()
        return null
    }
}

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun joinPsbts(
    listOfPsbts: List<String>
): String? {
    return runCatching {
        val psbts = listOfPsbts.mapNotNull { Psbt.read(Base64.decode(it.toByteArray())).right }
        val joinedPsbt = joinUniqueOutputs(*psbts.toTypedArray())
        val psbtBytes = Psbt.write(joinedPsbt.right!!)
        val psbtBase64 = Base64.encode(psbtBytes.toByteArray())
        println("joined psbt>> $psbtBase64")
        return psbtBase64
    }.getOrElse {
        println("joinPsbts")
        it.printStackTrace()
        return null
    }
}

fun joinUniqueOutputs(vararg psbts: Psbt): Either<UpdateFailure, Psbt> {
    return when {
        psbts.isEmpty() -> Either.Left(UpdateFailure.CannotJoin("no psbt provided"))
        psbts.map { it.global.version }.toSet().size != 1 -> Either.Left(UpdateFailure.CannotJoin("cannot join psbts with different versions"))
        psbts.map { it.global.tx.version }.toSet().size != 1 -> Either.Left(UpdateFailure.CannotJoin("cannot join psbts with different tx versions"))
        psbts.map { it.global.tx.lockTime }.toSet().size != 1 -> Either.Left(UpdateFailure.CannotJoin("cannot join psbts with different tx lockTime"))
        psbts.any { it.global.tx.txIn.size != it.inputs.size || it.global.tx.txOut.size != it.outputs.size } -> Either.Left(UpdateFailure.CannotJoin("some psbts have an invalid number of inputs/outputs"))
        psbts.flatMap { it.global.tx.txIn.map { txIn -> txIn.outPoint } }.toSet().size != psbts.sumOf { it.global.tx.txIn.size } -> Either.Left(
            UpdateFailure.CannotJoin("cannot join psbts that spend the same input"))
        else -> {
            val uniqueOutputs = psbts.flatMap { it.global.tx.txOut }.distinctBy { it.toString() }
            val uniqueOutputData = psbts.flatMap { it.outputs }.distinctBy { it.toString() }

            if (uniqueOutputs.size != uniqueOutputData.size) {
                Either.Left(UpdateFailure.CannotJoin("mismatch between unique outputs and output data"))
            } else {
                val global = psbts[0].global.copy(
                    tx = psbts[0].global.tx.copy(
                        txIn = psbts.flatMap { it.global.tx.txIn },
                        txOut = uniqueOutputs
                    ),
                    extendedPublicKeys = psbts.flatMap { it.global.extendedPublicKeys }.distinct(),
                    unknown = psbts.flatMap { it.global.unknown }.distinct()
                )
                Either.Right(psbts[0].copy(
                    global = global,
                    inputs = psbts.flatMap { it.inputs },
                    outputs = uniqueOutputData
                ))
            }
        }
    }
}