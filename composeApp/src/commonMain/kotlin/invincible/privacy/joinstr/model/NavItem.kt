package invincible.privacy.joinstr.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import joinstr.composeapp.generated.resources.Res
import joinstr.composeapp.generated.resources.ostrich_logo
import org.jetbrains.compose.resources.DrawableResource

open class Item(
    val path: String,
    val title: String,
    val icon: ImageVector? = null,
    val drawableResource: DrawableResource? = null
)

sealed class NavItem {
    object Home :
        Item(
            path = NavPath.HOME.toString(),
            title = NavTitle.HOME,
            icon = Icons.Default.Home
        )

    object Pools :
        Item(
            path = NavPath.POOLS.toString(),
            title = NavTitle.POOLS,
            drawableResource = Res.drawable.ostrich_logo
        )

    object Settings :
        Item(
            path = NavPath.SETTINGS.toString(),
            title = NavTitle.SETTINGS,
            icon = Icons.Default.Settings
        )
}

enum class NavPath {
    HOME, POOLS, SETTINGS
}

object NavTitle {
    const val HOME = "Home"
    const val POOLS = "Pools"
    const val SETTINGS = "Settings"
}