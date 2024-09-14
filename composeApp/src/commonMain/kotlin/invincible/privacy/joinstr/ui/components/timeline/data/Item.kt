package invincible.privacy.joinstr.ui.components.timeline.data

import org.jetbrains.compose.resources.DrawableResource

data class Item(
    val id: Int = 0,
    var title: String,
    var info: String = "",
    var images: List<DrawableResource> = listOf(),
    var showActions: Boolean = false,
    val description: String? = null,
)