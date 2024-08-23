package invincible.privacy.joinstr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import invincible.privacy.joinstr.model.ListUnspentResponseItem
import invincible.privacy.joinstr.model.Methods
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.network.HttpClient
import invincible.privacy.joinstr.theme.red
import invincible.privacy.joinstr.ui.components.ProgressDialog
import invincible.privacy.joinstr.ui.components.tagcloud.TagCloud
import invincible.privacy.joinstr.ui.components.tagcloud.math.Vector3
import invincible.privacy.joinstr.ui.components.tagcloud.rememberTagCloudState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
fun ListUnspentCloudsScreen() {
    var isLoading by remember { mutableStateOf(true) }
    var usdPerBtc by remember { mutableStateOf(0.0) }
    val httpClient = remember { HttpClient() }
    var listUnspent by remember { mutableStateOf<List<ListUnspentResponseItem>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var longPressDialog by remember { mutableStateOf(false) }
    var selectedTxId by remember { mutableStateOf("") }
    var autoRotation by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            usdPerBtc = httpClient.fetchUsdtPrice()
            val rpcRequestBody = RpcRequestBody(
                method = Methods.LIST_UNSPENT.value
            )
            listUnspent = httpClient.fetchUnspentList(rpcRequestBody)
            isLoading = false
        }
    }

    Surface {

        if (isLoading) {
            ProgressDialog()
        }

        if (longPressDialog) {
            autoRotation = false
            AlertDialog(
                properties = DialogProperties(),
                onDismissRequest = {
                    autoRotation = true
                    longPressDialog = false
                },
                text = {
                    val unspent = listUnspent.find { it.txid == selectedTxId }
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = "${unspent?.vout} | ${unspent?.txid}",
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {

                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SelectSpendableOutputsScreen(
                totalSats = listUnspent.sumOf { it.amount.times(100000000) }.toLong(),
                totalUsdt = listUnspent.sumOf { it.amount * usdPerBtc },
                selectedSats = (listUnspent.find { it.txid == selectedTxId }?.amount?.times(100000000))?.toLong() ?: 0L,
                selectedUsdt = (listUnspent.find { it.txid == selectedTxId }?.amount?.times(usdPerBtc)) ?: 0.0
            )

            if (listUnspent.isNotEmpty()) {
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
                        .height((listUnspent.size * 30).dp)
                        .padding(all = 32.dp),
                    state = state
                ) {
                    items(listUnspent) { item ->
                        Box(
                            modifier = Modifier.tagCloudItemFade(toAlpha = .5f)
                        ) {
                            val color = if (item.txid == selectedTxId) {
                                red
                            } else Color.Transparent

                            CustomOutlinedButton(
                                text = item.amount.toString(),
                                color = color,
                                onClick = {
                                    selectedTxId = if (selectedTxId == item.txid) {
                                        autoRotation = true
                                        ""
                                    } else {
                                        autoRotation = false
                                        item.txid
                                    }
                                },
                                onLongClick = {
                                    longPressDialog = !longPressDialog
                                    selectedTxId = item.txid
                                }
                            )
                        }
                    }
                }

                Button(
                    modifier = Modifier.padding(4.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = selectedTxId.isNotEmpty(),
                    onClick = {
                        // TODO
                    }
                ) {
                    Text(
                        modifier = Modifier
                            .padding(4.dp)
                            .align(Alignment.Bottom),
                        text = "Confirm",
                        fontSize = 16.sp
                    )
                }
            } else {
                if (isLoading.not()) {
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
    }
}

@Composable
fun SelectSpendableOutputsScreen(
    totalSats: Long,
    totalUsdt: Double,
    selectedSats: Long,
    selectedUsdt: Double
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
                text = "Total: $totalSats sats = $totalUsdt USDT",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 18.dp)
            )

            // Selected sats
            Text(
                text = "$selectedSats SATS",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Selected USDT
            Text(
                text = "$selectedUsdt USDT",
                fontSize = 20.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CustomOutlinedButton(
    modifier: Modifier = Modifier,
    text: String,
    color: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    longPressDuration: Long = 500L
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val fontSize = 16.sp
    var textWidth by remember { mutableStateOf(0.dp) }
    val buttonSize = textWidth + 32.dp

    Box(
        modifier = modifier
            .clip(CircleShape)
            .size(buttonSize)
            .background(color, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val pressStartTime = Clock.System.now().toEpochMilliseconds()
                        val job = coroutineScope.launch {
                            delay(longPressDuration)
                            onLongClick()
                        }
                        tryAwaitRelease()
                        job.cancel()
                        if (Clock.System.now().toEpochMilliseconds() - pressStartTime < longPressDuration) {
                            onClick()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
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