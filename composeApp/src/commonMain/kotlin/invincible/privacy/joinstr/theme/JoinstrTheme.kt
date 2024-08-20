package invincible.privacy.joinstr.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import invincible.privacy.joinstr.getKStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = BlueLighter80,
    tertiary = BlueGrey80,
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = DarkBlue80,
    onBackground = Color(0xFFF9F7F7),
    onSurface = Color(0xFFF9F7F7),
    primaryContainer = DarkBlue80,
    onPrimaryContainer = Color.White
)

val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = BlueLighter40,
    tertiary = BlueGrey40,
    background = Color(0xFFF9F7F7),
    surface = Color(0xFFF9F7F7),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = DarkBlue40,
    onSurface = DarkBlue40,
    primaryContainer = BlueLighter40,
    onPrimaryContainer = Color.White
)


@Composable
fun JoinstrTheme(
    colorScheme: ColorScheme,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

object SettingsManager {
    val themeState = MutableStateFlow(Theme.SYSTEM.id)
    val store = getKStore()

    suspend fun updateTheme(newTheme: Int) {
        themeState.value = newTheme
        store.update { it?.copy(selectedTheme = newTheme) ?: Settings(selectedTheme = newTheme, nodeConfig = NodeConfig()) }
    }

    suspend fun updateNodeConfig(nodeConfig: NodeConfig) {
        store.update { it?.copy(nodeConfig = nodeConfig) ?: Settings(selectedTheme = themeState.value, nodeConfig = NodeConfig()) }
    }
}

@Serializable
data class Settings(
    val selectedTheme: Int = Theme.SYSTEM.id,
    val nodeConfig: NodeConfig = NodeConfig()
)

@Serializable
data class NodeConfig(
    val url: String = "http://127.0.0.1",
    val userName: String = "user",
    val password: String = "pass",
    val port: Int = 38332
)

enum class Theme(val id: Int, val title: String, val description: String? = null) {
    SYSTEM(0, "Use Device Settings", "Upon activation, Day or Night mode will be followed by device settings."),
    LIGHT(1, "Light"),
    DARK(2, "Dark")
}