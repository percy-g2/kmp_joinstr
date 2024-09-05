package invincible.privacy.joinstr

import fr.acinq.secp256k1.Hex
import fr.acinq.secp256k1.Secp256k1
import fr.acinq.secp256k1.Secp256k1.Companion.pubKeyTweakMul
import invincible.privacy.joinstr.model.LocalPoolContent
import invincible.privacy.joinstr.utils.NodeConfig
import invincible.privacy.joinstr.utils.Settings
import invincible.privacy.joinstr.utils.Theme
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
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
import platform.Foundation.addTimeInterval
import platform.Foundation.dateByAddingTimeInterval
import platform.Foundation.timeIntervalSinceNow
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationBackgroundFetchIntervalNever
import platform.UIKit.UIBackgroundFetchResult
import platform.UserNotifications.UNAuthorizationOptionAlert
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
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.time.Duration.Companion.seconds

class NotificationDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
    ) {
        withCompletionHandler(UNNotificationPresentationOptionBanner or
            UNNotificationPresentationOptionSound or
            UNNotificationPresentationOptionList)
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

    actual fun show(title: String, message: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()

        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or
                UNAuthorizationOptionSound
        ) { granted, error ->
            if (granted) {
                val content = UNMutableNotificationContent().apply {
                    setTitle(title)
                    setBody(message)
                    setSound(UNNotificationSound.defaultSound())
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

                center.addNotificationRequest(request) { requestError ->
                    if (requestError != null) {
                        println("Error showing notification: ${requestError.localizedDescription}")
                    } else {
                        println("Notification scheduled successfully with ID: $uuid")
                    }
                }
                if (error != null) {
                    println("Error notification: ${error.localizedDescription}")
                }
            } else {
                println("Notification permission denied")
            }
        }
    }
}

actual class KmpMainThread {
    actual companion object {
        actual fun runViaMainThread(action: DefaultAction) {
            dispatch_async(dispatch_get_main_queue()) {
                action()
            }
        }
    }
}
actual object KmpBackgrounding {
    private const val appleDefaultId = "invincible.privacy.joinstr.backgrounding"
    lateinit var backgroundTask: DefaultAction

    fun registerBackgroundService(){
        KmpMainThread.runViaMainThread {
            BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(appleDefaultId, null){
                backgroundTask()
            }
        }
    }

    fun cancel() {
        KmpMainThread.runViaMainThread {
            BGTaskScheduler.sharedScheduler.cancelAllTaskRequests()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun createAndStartWorker(action: DefaultAction) {
        action.invoke()
        KmpMainThread.runViaMainThread {
            // Schedule the background task
            val processor = BGProcessingTaskRequest(appleDefaultId)

            BGTaskScheduler.sharedScheduler.submitTaskRequest(processor, null)
        }
    }
}

actual fun getSettingsStore(): KStore<Settings> {
    val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    return storeOf<Settings>(
        file = "${paths.firstOrNull() as? String}/settings.json".toPath(),
        default = Settings(
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