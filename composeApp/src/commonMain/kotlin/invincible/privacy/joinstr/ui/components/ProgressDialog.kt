package invincible.privacy.joinstr.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import invincible.privacy.joinstr.ktx.lottieAnimationImage

@Composable
fun ProgressDialog(onDialogDismiss: (() -> Unit)? = null) {
    val image = "files/loading-gray-circle.json".lottieAnimationImage()

    Dialog(
        onDismissRequest = { onDialogDismiss?.invoke() },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Image(
                modifier = Modifier.padding(12.dp),
                painter = image,
                contentDescription = "loading",
                contentScale = ContentScale.FillBounds
            )
        }
    }
}