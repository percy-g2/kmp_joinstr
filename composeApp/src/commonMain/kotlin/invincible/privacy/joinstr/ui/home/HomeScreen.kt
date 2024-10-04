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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import invincible.privacy.joinstr.model.BlockchainInfo
import invincible.privacy.joinstr.model.NetworkInfo
import invincible.privacy.joinstr.theme.greenDark
import invincible.privacy.joinstr.theme.redDark
import invincible.privacy.joinstr.theme.redLight
import joinstr.composeapp.generated.resources.Res
import joinstr.composeapp.generated.resources.app_name
import joinstr.composeapp.generated.resources.joinstr_logo
import joinstr.composeapp.generated.resources.network
import joinstr.composeapp.generated.resources.node_version
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(
    homeScreenViewModel: HomeScreenViewModel = viewModel { HomeScreenViewModel() }
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by homeScreenViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    homeScreenViewModel.fetchData()
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
            Image(
                modifier = Modifier.wrapContentSize(),
                painter = painterResource(Res.drawable.joinstr_logo),
                contentDescription = "logo"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (uiState.isLoading.not()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                BlockchainInfoDisplay(uiState.blockchainInfo, uiState.networkInfo)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun BlockchainInfoDisplay(blockchain: BlockchainInfo?, network: NetworkInfo?) {
    val version = network?.subversion?.let { Regex("\\d+\\.\\d+\\.\\d+").find(network.subversion)?.value } ?: ""
    val textInsideParentheses = network?.subversion?.let { Regex("\\((.*?)\\)").find(network.subversion)?.groupValues?.get(1) } ?: ""

    val chain = blockchain?.chain?.capitalize(Locale.current) ?: ""

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        InfoTextField(
            value = when {
                version.isEmpty() && textInsideParentheses.isEmpty() -> ""
                textInsideParentheses.isEmpty() -> "Bitcoin Core v$version"
                else -> "Bitcoin Core v$version($textInsideParentheses)"
            },
            label = stringResource(Res.string.node_version)
        )

        Spacer(modifier = Modifier.height(8.dp))

        InfoTextField(
            value = chain,
            label = stringResource(Res.string.network)
        )
    }
}

@Composable
fun InfoTextField(value: String, label: String) {
    OutlinedTextField(
        value = value.ifEmpty { " " },
        onValueChange = { /* no-op */ },
        textStyle = MaterialTheme.typography.bodyMedium,
        enabled = false,
        isError = value.isEmpty(),
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
            disabledLabelColor = MaterialTheme.colorScheme.onBackground,
            disabledBorderColor = if (value.isEmpty()) redLight else MaterialTheme.colorScheme.onBackground
        )
    )
}