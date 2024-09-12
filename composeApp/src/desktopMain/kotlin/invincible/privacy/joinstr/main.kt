package invincible.privacy.joinstr

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import joinstr.composeapp.generated.resources.Res
import joinstr.composeapp.generated.resources.joinstr_logo
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    var isVisible by remember { mutableStateOf(true) }
    val state = rememberWindowState(placement = WindowPlacement.Maximized)

    Window(
        onCloseRequest = { isVisible = false },
        visible = isVisible,
        state = state,
        title = "Joinstr"
    ) {
        App()
    }

    if (!isVisible) {
        Tray(
            icon = painterResource(Res.drawable.joinstr_logo),
            tooltip = "Joinstr",
            onAction = { isVisible = true },
            menu = {
                Item("Quit", onClick = ::exitApplication)
            }
        )
    }
}

