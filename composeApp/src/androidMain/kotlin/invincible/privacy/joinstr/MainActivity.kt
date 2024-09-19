package invincible.privacy.joinstr

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.core.content.ContextCompat
import invincible.privacy.joinstr.model.CoinJoinHistory
import invincible.privacy.joinstr.theme.DarkColorScheme
import invincible.privacy.joinstr.theme.LightColorScheme
import invincible.privacy.joinstr.ui.pools.HistoryItem
import invincible.privacy.joinstr.utils.SettingsManager
import invincible.privacy.joinstr.utils.Theme
import kotlinx.datetime.Clock

class MainActivity : ComponentActivity() {

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
                println("permission granted")
            } else {
                println("permission not granted")
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
        amount = 0.99956f,
        psbt = "cHNidP8BAHECAAAAAeX8EYwAAAAAAAAAA/////w8AAAAAAAAAIgAgihVeUpqt5eSA7gWtv7UNObRTkNISXiAVyXNkFsKPf7pAAAAAAABAP0BAAAAAQAAAAABAAAAAAAAgAEAAAABAAAAAQEfAqACAAAAAQAAAAAAAQERKhYAAAAAABYAFKlUwqDXAsFWKvKdl3wtrki1pS8BAgAAAAEAAAAAAAAAIgAg+2M/VTeckRCOuy0y0mK6zCz/CT8J0FQjjDleqUV1zYEAAAAA\n",
        tx = "a40ae97da65ed66f279cc04c54ed5040e94cde39d11b6f2d1ea151855b49c931",
        timestamp = Clock.System.now().toEpochMilliseconds()
    )
    HistoryItem(coinJoinHistory)
}