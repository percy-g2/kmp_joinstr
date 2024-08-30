package invincible.privacy.joinstr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import invincible.privacy.joinstr.theme.DarkColorScheme
import invincible.privacy.joinstr.theme.LightColorScheme
import invincible.privacy.joinstr.utils.SettingsManager
import invincible.privacy.joinstr.utils.Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
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

                val isSystemInDarkTheme = isSystemInDarkTheme()
                LaunchedEffect(themeState) {
                    if (themeState == Theme.LIGHT.id || (!isSystemInDarkTheme && themeState == Theme.SYSTEM.id)) {
                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.light(barColor, barColor)
                        )
                    } else {
                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.dark(barColor)
                        )
                    }
                }
            }
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}