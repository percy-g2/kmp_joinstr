package invincible.privacy.joinstr.ktx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.painter.Painter
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.CompottieException
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import joinstr.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

fun String.hexToByteArray(): ByteArray =
    ByteArray(length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

fun String.isValidHttpUrl(): Boolean {
    val regex = "^(https?://)[\\w.-]+(:\\d+)?(/.*)?$".toRegex()
    return isNotBlank() && regex.matches(this)
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun String.lottieAnimationImage(): Painter {
    val composition = rememberLottieComposition {
        LottieCompositionSpec.JsonString(Res.readBytes(this).decodeToString())
    }

    LaunchedEffect(composition) {
        try {
            composition.await()
        } catch (t: CompottieException) {
            t.printStackTrace()
        }
    }

    val animationState by animateLottieCompositionAsState(
        composition = composition.value,
        iterations = Compottie.IterateForever
    )

   return rememberLottiePainter(
        composition = composition.value,
        progress = { animationState },
    )
}