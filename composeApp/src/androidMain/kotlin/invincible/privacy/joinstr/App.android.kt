package invincible.privacy.joinstr

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import okio.Path.Companion.toPath
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.time.Duration.Companion.seconds

actual fun getSettingsStore(): KStore<Settings> {
    val context = ContextProvider.getContext()
    return storeOf<Settings>(
        file = "${context.cacheDir?.absolutePath}/settings.json".toPath(),
        default = Settings(
            selectedTheme = Theme.SYSTEM.id,
            nodeConfig = NodeConfig()
        )
    )
}

actual object LocalNotification {
    private val channelId = "joinstr"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Default Channel"
            val descriptionText = "Default notification channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                ContextProvider.getContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    actual fun showNotification(title: String, message: String) {
        val context = ContextProvider.getContext()
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                CoroutineScope(Dispatchers.Default).launch {
                    requestPermission()
                }
            }
            notify(Clock.System.now().toEpochMilliseconds().toInt(), builder.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    actual suspend fun requestPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            ContextProvider.getContext(),
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}

actual fun getPoolsStore(): KStore<List<LocalPoolContent>> {
    val context = ContextProvider.getContext()
    return storeOf<List<LocalPoolContent>>(
        file = "${context.cacheDir?.absolutePath}/pools.json".toPath(),
        default = emptyList()
    )
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

actual fun Float.convertFloatExponentialToString(): String {
    val mathContext = MathContext(8, RoundingMode.HALF_UP)
    return BigDecimal(toString()).round(mathContext).toPlainString()
}

actual fun getWebSocketClient(): HttpClient {
    return HttpClient(CIO) {
        install(WebSockets)

        install(HttpTimeout) {
            requestTimeoutMillis = 5.seconds.inWholeMilliseconds
            connectTimeoutMillis = 5.seconds.inWholeMilliseconds
            socketTimeoutMillis = 5.seconds.inWholeMilliseconds
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }
}


actual fun getSharedSecret(privateKey: ByteArray, pubKey: ByteArray): ByteArray =
    pubKeyTweakMul(Hex.decode("02") + pubKey, privateKey).copyOfRange(1, 33)
