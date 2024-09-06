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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import contentScale.ContentScale
import invincible.privacy.joinstr.LocalNotification
import invincible.privacy.joinstr.model.BlockchainInfo
import invincible.privacy.joinstr.model.NetworkInfo
import joinstr.composeapp.generated.resources.Res
import joinstr.composeapp.generated.resources.joinstr_logo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kottieComposition.KottieCompositionSpec
import kottieComposition.animateKottieCompositionAsState
import kottieComposition.rememberKottieComposition
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import utils.KottieConstants

@Composable
fun HomeScreen(
    homeScreenViewModel: HomeScreenViewModel = viewModel{ HomeScreenViewModel() }
) {
    val networkInfo by homeScreenViewModel.networkInfo.collectAsState()
    val blockchainInfo by homeScreenViewModel.blockchainInfo.collectAsState()
    val isLoading by homeScreenViewModel.isLoading.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoAnimation()

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                blockchainInfo?.let { blockchain ->
                    networkInfo?.let { network ->
                        BlockchainInfoDisplay(blockchain, network)
                    }
                } ?: run {
                    ErrorMessage()
                }
            }
        }

        FloatingActionButton(
            onClick = {
                CoroutineScope(Dispatchers.Default).launch {
                    val result = LocalNotification.requestPermission()
                    if (result) {
                        LocalNotification.showNotification("title", "msg")
                    }
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
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun LogoAnimation() {
    var animation by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
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
        modifier = Modifier.padding(top = 24.dp),
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

@Composable
fun BlockchainInfoDisplay(blockchain: BlockchainInfo, network: NetworkInfo) {
    val version = Regex("\\d+\\.\\d+\\.\\d+").find(network.subversion)?.value ?: ""
    val textInsideParentheses = Regex("\\((.*?)\\)").find(network.subversion)?.groupValues?.get(1) ?: ""

    InfoTextField(
        value = "Bitcoin Core v$version($textInsideParentheses)",
        label = "Version"
    )

    Spacer(modifier = Modifier.height(8.dp))

    InfoTextField(
        value = blockchain.chain.capitalize(Locale.current),
        label = "Network"
    )

    Spacer(modifier = Modifier.height(8.dp))

    InfoTextField(
        value = "${formatNumber(blockchain.blocks)} Blocks",
        label = "Block Height"
    )
}

@Composable
fun InfoTextField(value: String, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { /* no-op */ },
        textStyle = MaterialTheme.typography.bodyMedium,
        enabled = false,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onBackground,
            disabledLabelColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
fun ErrorMessage() {
    Text(
        text = "Verify Your Node Configuration!",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.errorContainer
    )
}

fun formatNumber(input: Int): String {
    return input.toString().reversed().chunked(3).joinToString(",").reversed()
}