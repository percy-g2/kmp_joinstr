package invincible.privacy.joinstr.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import joinstr.composeapp.generated.resources.Res
import joinstr.composeapp.generated.resources.ostrich_logo
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource

sealed class NavItem {
    open class Item<T>(
        val path: T,
        val title: String,
        val icon: ImageVector? = null,
        val drawableResource: DrawableResource? = null,
    ) : NavItem()

    object HomeScreen : Item<Home>(
        path = Home,
        title = "Home",
        icon = Icons.Default.Home
    )

    object PoolsScreen : Item<Pools>(
        path = Pools,
        title = "Pools",
        drawableResource = Res.drawable.ostrich_logo
    )

    object SettingsScreen : Item<Settings>(
        path = Settings,
        title = "Settings",
        icon = Icons.Default.Settings
    )

    object InputRegistrationScreen : Item<InputRegistration>(
        path = InputRegistration(),
        title = "Input Registration"
    )
}

@Serializable
object Home

@Serializable
object Pools

@Serializable
object Settings

@Serializable
data class InputRegistration(val id: String = "")