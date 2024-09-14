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
import invincible.privacy.joinstr.theme.DarkColorScheme
import invincible.privacy.joinstr.theme.LightColorScheme
import invincible.privacy.joinstr.utils.SettingsManager
import invincible.privacy.joinstr.utils.Theme

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
    App()
}