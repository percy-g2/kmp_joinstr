package invincible.privacy.joinstr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import invincible.privacy.joinstr.model.ListUnspentResponseItem
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.network.HttpClient
import invincible.privacy.joinstr.theme.redForLoss
import invincible.privacy.joinstr.ui.components.ProgressDialog
import invincible.privacy.joinstr.ui.components.tagcloud.TagCloud
import invincible.privacy.joinstr.ui.components.tagcloud.math.Vector3
import invincible.privacy.joinstr.ui.components.tagcloud.rememberTagCloudState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun SelectSpendableOutputsScreen(
    totalSats: Long,
    totalUsd: Double,
    selectedSats: Long,
    selectedUsd: Double
) {
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Select spendable outputs",
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Total sats and USD
            Text(
                text = "Total: $totalSats sats = $totalUsd USD",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Selected sats
            Text(
                text = "$selectedSats SATS",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Selected USD
            Text(
                text = "$selectedUsd USD",
                fontSize = 20.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
fun ListUnspentCloudsScreen() {
    var isLoading by remember { mutableStateOf(true) }
    var usdPerBtc by remember { mutableStateOf(0.0) }
    val httpClient = remember { HttpClient() }
    var listUnspent by remember { mutableStateOf<List<ListUnspentResponseItem>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            usdPerBtc = httpClient.fetchUsdtPrice()
            val rpcRequestBody = RpcRequestBody(
                method = "listunspent"
            )
            listUnspent = httpClient.fetchUnspentList(rpcRequestBody)
            isLoading = false
        }
    }

    if (isLoading) {
        ProgressDialog()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var selectedTxId by remember { mutableStateOf("") }
        SelectSpendableOutputsScreen(
            totalSats = listUnspent.sumOf { it.amount.times(100000000) }.toLong(),
            totalUsd = listUnspent.sumOf { it.amount * usdPerBtc },
            selectedSats = (listUnspent.find { it.txid == selectedTxId }?.amount?.times(100000000))?.toLong() ?: 0L,
            selectedUsd = (listUnspent.find { it.txid == selectedTxId }?.amount?.times(usdPerBtc)) ?: 0.0
        )

        if (listUnspent.isNotEmpty()) {
            var autoRotation by remember { mutableStateOf(true) }

            val state = rememberTagCloudState(
                onStartGesture = { autoRotation = false },
                onEndGesture = { autoRotation = true },
            )

            LaunchedEffect(state, autoRotation) {
                while (isActive && autoRotation) {
                    delay(10)
                    state.rotateBy(0.001f, Vector3(1f, 1f, 1f))
                }
            }
            TagCloud(
                modifier = Modifier
                    .width((listUnspent.size * 70).dp)
                    .height((listUnspent.size * 80).dp)
                    .padding(all = 32.dp),
                state = state
            ) {
                items(listUnspent) {
                    Box {
                        val density = LocalDensity.current
                        val text = it.amount.toString()
                        val fontSize = 16.sp
                        var textWidth by remember { mutableStateOf(0.dp) }
                        val buttonSize = textWidth + 32.dp

                        val color = if (it.txid == selectedTxId) {
                            redForLoss
                        } else Color.Transparent

                        OutlinedButton(
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(buttonSize),
                            onClick = {
                                selectedTxId = if (it.txid == selectedTxId) {
                                    ""
                                } else it.txid
                            },
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = color)
                        ) {
                            Text(
                                text = text,
                                fontSize = fontSize,
                                onTextLayout = { textLayoutResult: TextLayoutResult ->
                                    textWidth = with(density) { textLayoutResult.size.width.toDp() }
                                },
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No amount found to spend",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Normalizes the BTC amount to a circle radius.
 * Assumes a reasonable max radius for display purposes.
 */
fun normalizeAmountToRadius(amount: Double): Float {
    val minRadius = 100f    // Increased minimum circle radius for better visibility
    val maxRadius = 300f   // Increased maximum circle radius for larger amounts

    // Calculate logarithmic scaling
    val logAmount = log10(amount + 1) // Adding 1 to avoid issues with log10(0)
    val maxLogAmount = log10(21_000_000.0 + 1) // Log value for 21 million BTC
    val minLogAmount = log10(0.00000001 + 1) // Log value for smallest unit (1 Satoshi)

    // Normalize the log amount to a 0..1 range
    val normalizedLog = (logAmount - minLogAmount) / (maxLogAmount - minLogAmount)

    // Apply an additional scaling factor to make the radii more noticeable
    val scalingFactor = 50f  // Adjust this factor to amplify the differences

    // Scale to the radius range
    val scaledRadius = normalizedLog.pow(scalingFactor.toDouble()) * (maxRadius - minRadius) + minRadius

    return scaledRadius.toFloat().coerceIn(minRadius, maxRadius)
}