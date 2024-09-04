package invincible.privacy.joinstr.ui.home

import KottieAnimation
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import contentScale.ContentScale
import joinstr.composeapp.generated.resources.Res
import joinstr.composeapp.generated.resources.joinstr_logo
import kottieComposition.KottieCompositionSpec
import kottieComposition.animateKottieCompositionAsState
import kottieComposition.rememberKottieComposition
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import utils.KottieConstants

@OptIn(ExperimentalResourceApi::class)
@Composable
fun HomeScreen() {
    var animation by remember { mutableStateOf("") }

    LaunchedEffect(Unit){
        animation = Res.readBytes("files/wave.json").decodeToString()
    }

    val composition = rememberKottieComposition(
        spec = KottieCompositionSpec.File(animation)
    )

    val animationState by animateKottieCompositionAsState(
        composition = composition,
        iterations = KottieConstants.IterateForever
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        KottieAnimation(
            composition = composition,
            progress = { animationState.progress },
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f),
            backgroundColor = MaterialTheme.colorScheme.background,
            contentScale = ContentScale.FillBounds
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.zIndex(2f)
        ) {
            Image(
                modifier = Modifier.wrapContentSize(),
                painter = painterResource(Res.drawable.joinstr_logo),
                contentDescription = "logo"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Joinstr",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}