package invincible.privacy.joinstr

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.RemoteException
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.blinkt.openvpn.api.IOpenVPNAPIService
import de.blinkt.openvpn.api.IOpenVPNStatusCallback
import invincible.privacy.joinstr.model.CoinJoinHistory
import invincible.privacy.joinstr.theme.DarkColorScheme
import invincible.privacy.joinstr.theme.LightColorScheme
import invincible.privacy.joinstr.ui.pools.HistoryItem
import invincible.privacy.joinstr.utils.SettingsManager
import invincible.privacy.joinstr.utils.Theme
import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader


class MainActivity : ComponentActivity(), Handler.Callback {

    protected var mService: IOpenVPNAPIService? = null

    private var mHandler: Handler? = null

    private var auth_failed = false

    private val MSG_UPDATE_STATE: Int = 0

    private val ICS_OPENVPN_PERMISSION: Int = 7

    private val NOTIFICATIONS_PERMISSION_REQUEST_CODE: Int = 11

    /**
     * Taking permission for network access
     */
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ICS_OPENVPN_PERMISSION) {
            try {
                mService!!.registerStatusCallback(mCallback)
            } catch (e: RemoteException) {
                Napier.e("openvpn status callback failed: " + e.message)
                e.printStackTrace()
            }
        }
    }

    private val mCallback: IOpenVPNStatusCallback = object : IOpenVPNStatusCallback.Stub() {
        /**
         * This is called by the remote service regularly to tell us about
         * new values.  Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */
        @Throws(RemoteException::class)
        override fun newStatus(uuid: String, state: String, message: String, level: String) {
            val msg: Message = Message.obtain(mHandler, MSG_UPDATE_STATE, "$state|$message")

            if (state == "AUTH_FAILED" || state == "CONNECTRETRY") {
                auth_failed = true
            }
            if (!auth_failed) {
                try {
                    //  setStatus(state)
                    // updateConnectionStatus(state)
                } catch (e: Exception) {
                    Napier.e("openvpn status callback failed: " + e.message)
                    e.printStackTrace()
                }
                msg.sendToTarget()
            }

            if (auth_failed) {
                Napier.i("AUTHORIZATION FAILED!!")
                Napier.i("CONNECTRETRY")
            }
            if (state == "CONNECTED") {
                auth_failed = false
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf<String>(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATIONS_PERMISSION_REQUEST_CODE
                    )
                }
                //    bindTimerService()
            } else {
                //    unbindTimerService()
            }
        }
    }

    private fun bindService() {
        val icsopenvpnService = Intent(IOpenVPNAPIService::class.java.name)
        icsopenvpnService.setPackage("invincible.privacy.joinstr")

        this.bindService(icsopenvpnService, mConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Start the VPN
     */
    private fun startVpn() {


        try {

            /* Try opening test.local.conf first */
            var conf: InputStream = this.getAssets().open("test.conf")

            val br = BufferedReader(InputStreamReader(conf))
            val config = StringBuilder()
            var line: String?
            while (true) {
                line = br.readLine()
                if (line == null) break
                config.append(line).append("\n")
            }
            br.close()
            conf.close()

            val profile = mService?.addNewVPNProfile("test", false, config.toString())
            if (profile != null) {
                Napier.i("profile.mUUID")

                // Update log
                Napier.i("Connecting...")

                mService?.startProfile(profile.mUUID)
                mService?.startVPN(config.toString())

                auth_failed = false
            } else {
                Napier.e("profile.mUUID null")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Napier.e("openvpn server connection failed: " + e.message)
        }
    }

    public override fun onStart() {
        super.onStart()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel("openvpn_newstat", "VPN foreground service", NotificationManager.IMPORTANCE_NONE)
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val chanBgVPN = NotificationChannel("openvpn_bg", "VPN background service", NotificationManager.IMPORTANCE_NONE)
            chanBgVPN.lightColor = Color.BLUE
            chanBgVPN.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)
            service.createNotificationChannel(chanBgVPN)
        }

        mHandler = Handler(this)
        bindService()


        // Checking permission for network monitor
        val intent = mService?.prepareVPNService()

        if (intent != null) {
            startActivityForResult(intent, 1)
        } else {
            startVpn() //Already have permission
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder,
        ) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.

            mService = IOpenVPNAPIService.Stub.asInterface(service)

            try {
                // Request permission to use the API
                val i = mService?.prepare(this@MainActivity.getPackageName())
                if (i != null) {
                    startActivityForResult(i, ICS_OPENVPN_PERMISSION)
                } else {
                    onActivityResult(ICS_OPENVPN_PERMISSION, RESULT_OK, null)
                }
            } catch (e: RemoteException) {
                Napier.e("openvpn service connection failed: " + e.message)
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null
        }
    }

    override fun handleMessage(msg: Message): Boolean {
        if (msg.what == MSG_UPDATE_STATE) {
            val messageText = msg.obj as String
            val stateDetails = messageText.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val currState = stateDetails[0]

            Napier.i(currState)
        }
        return true
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RequestNotificationPermission()

            val themeState by SettingsManager.themeState.collectAsState()
            val view = LocalView.current

            if (!view.isInEditMode) {
                // Determine color scheme based on themeState and system theme
                val colorScheme = when (themeState) {
                    Theme.SYSTEM.id -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
                    Theme.LIGHT.id -> LightColorScheme
                    Theme.DARK.id -> DarkColorScheme
                    else -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
                }

                // Set the system UI bar colors based on the app and system theme
                val barColor = colorScheme.background.toArgb()
                val navBarColor = colorScheme.surfaceColorAtElevation(3.dp).toArgb()

                val isSystemInDarkTheme = isSystemInDarkTheme()
                LaunchedEffect(themeState) {
                    if (themeState == Theme.LIGHT.id || (!isSystemInDarkTheme && themeState == Theme.SYSTEM.id)) {
                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.light(barColor, barColor),
                            navigationBarStyle = SystemBarStyle.light(navBarColor, navBarColor)
                        )
                    } else {
                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.dark(barColor),
                            navigationBarStyle = SystemBarStyle.dark(navBarColor)
                        )
                    }
                }
            }
            App()
        }
    }

    @Composable
    fun RequestNotificationPermission() {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Napier.i("permission granted")
            } else {
                Napier.v("permission not granted")
            }
        }

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    // set context for App preview
    ContextProvider.setContext(LocalContext.current)
    App()
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun CoinjoinHistoryItemPreview() {
    val coinJoinHistory = CoinJoinHistory(
        relay = "wss://nostr.fmt.wiz.biz",
        publicKey = "57ae43228fa2f2a6f83cb327757ef0d077bfcaffa4b917ee4e7d5642e054e8f8",
        privateKey = "56778fd1c67641deb10f14450b683b15674e62056164cbccb11a03faf2ced119",
        amount = 0.99956,
        psbt = "cHNidP8BAHECAAAAAeX8EYwAAAAAAAAAA/////w8AAAAAAAAAIgAgihVeUpqt5eSA7gWtv7UNObRTkNISXiAVyXNkFsKPf7pAAAAAAABAP0BAAAAAQAAAAABAAAAAAAAgAEAAAABAAAAAQEfAqACAAAAAQAAAAAAAQERKhYAAAAAABYAFKlUwqDXAsFWKvKdl3wtrki1pS8BAgAAAAEAAAAAAAAAIgAg+2M/VTeckRCOuy0y0mK6zCz/CT8J0FQjjDleqUV1zYEAAAAA\n",
        tx = "a40ae97da65ed66f279cc04c54ed5040e94cde39d11b6f2d1ea151855b49c931",
        timestamp = Clock.System.now().toEpochMilliseconds()
    )
    HistoryItem(coinJoinHistory)
}