package invincible.privacy.joinstr.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import invincible.privacy.joinstr.model.BlockchainInfo
import invincible.privacy.joinstr.model.NetworkInfo
import invincible.privacy.joinstr.theme.greenDark
import invincible.privacy.joinstr.theme.redDark
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.CompottieException
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import joinstr.composeapp.generated.resources.Res
import joinstr.composeapp.generated.resources.joinstr_logo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeScreen(
    homeScreenViewModel: HomeScreenViewModel = viewModel { HomeScreenViewModel() },
) {
    val uiState by homeScreenViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val job = scope.launch {
            homeScreenViewModel.fetchData()
        }
        onDispose {
            job.cancel()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoAnimation()

            if (uiState.isLoading.not()) {
                BlockchainInfoDisplay(uiState.blockchainInfo, uiState.networkInfo)
            }
        }

        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }

       /* FloatingActionButton(
            onClick = {
                scope.launch {
                    homeScreenViewModel.fetchData()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh"
            )
        }*/
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun LogoAnimation() {

    val composition = rememberLottieComposition {
        LottieCompositionSpec.JsonString(Res.readBytes("files/wave.json").decodeToString())
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

    Box(
        contentAlignment = Alignment.Center
    ) {
        /* Image(
             painter = rememberLottiePainter(
                 composition = composition.value,
                 progress = { animationState },
             ),
             contentDescription = "Lottie animation",
             modifier = Modifier
                 .fillMaxWidth()
                 .zIndex(1f),
             contentScale = ContentScale.FillBounds
         )*/

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,

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

@Composable
fun BlockchainInfoDisplay(blockchain: BlockchainInfo?, network: NetworkInfo?) {
    val version = network?.subversion?.let { Regex("\\d+\\.\\d+\\.\\d+").find(network.subversion)?.value } ?: ""
    val textInsideParentheses = network?.subversion?.let { Regex("\\((.*?)\\)").find(network.subversion)?.groupValues?.get(1) } ?: ""

    val chain = blockchain?.chain?.capitalize(Locale.current) ?: ""

    Column(
        modifier = Modifier.padding(top = 24.dp)
    ) {
        InfoTextField(
            value = if (version.isEmpty() && textInsideParentheses.isEmpty() ) "" else "Bitcoin Core v$version($textInsideParentheses)",
            label = "Node Version"
        )

        Spacer(modifier = Modifier.height(8.dp))

        InfoTextField(
            value = chain,
            label = "Network"
        )
    }
}

@Composable
fun InfoTextField(value: String, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { /* no-op */ },
        textStyle = MaterialTheme.typography.bodyMedium,
        enabled = false,
        label = {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                )

                Spacer(modifier = Modifier.width(4.dp))

                Badge(containerColor = if (value.isEmpty()) redDark else greenDark)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onBackground,
            disabledLabelColor = MaterialTheme.colorScheme.onBackground
        )
    )
}