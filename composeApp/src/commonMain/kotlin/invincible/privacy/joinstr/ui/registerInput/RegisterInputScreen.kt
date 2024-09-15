package invincible.privacy.joinstr.ui.registerInput

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import invincible.privacy.joinstr.convertFloatExponentialToString
import invincible.privacy.joinstr.getPoolsStore
import invincible.privacy.joinstr.theme.greenDark
import invincible.privacy.joinstr.theme.redDark
import invincible.privacy.joinstr.ui.components.CenterColumnText
import invincible.privacy.joinstr.ui.components.tagcloud.TagCloud
import invincible.privacy.joinstr.ui.components.tagcloud.math.Vector3
import invincible.privacy.joinstr.ui.components.tagcloud.rememberTagCloudState
import invincible.privacy.joinstr.ui.components.timeline.EventPointType
import invincible.privacy.joinstr.ui.components.timeline.ItemsList
import invincible.privacy.joinstr.ui.components.timeline.JetLimeColumn
import invincible.privacy.joinstr.ui.components.timeline.JetLimeDefaults
import invincible.privacy.joinstr.ui.components.timeline.JetLimeEvent
import invincible.privacy.joinstr.ui.components.timeline.JetLimeEventDefaults
import invincible.privacy.joinstr.ui.components.timeline.VerticalEventContent
import invincible.privacy.joinstr.ui.components.timeline.data.Item
import joinstr.composeapp.generated.resources.Res
import joinstr.composeapp.generated.resources.input_registration
import joinstr.composeapp.generated.resources.no_amount_found_to_spend
import joinstr.composeapp.generated.resources.register
import joinstr.composeapp.generated.resources.something_went_wrong
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalAnimationApi::class, ExperimentalStdlibApi::class)
@Composable
fun RegisterInputScreen(
    poolId: String,
    viewModel: RegisterInputViewModel = viewModel { RegisterInputViewModel() },
) {
    val isLoading by viewModel.isLoading
    val listUnspent by viewModel.listUnspent
    val selectedTxId by viewModel.selectedTx
    var autoRotation by remember { mutableStateOf(false) }
    val showRegisterInputTimeLine = remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val items = remember { mutableStateListOf<Item>() }

    LaunchedEffect(Unit) {
        val activePools = getPoolsStore().get()
            ?.sortedByDescending { it.timeout }
            ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }
        showRegisterInputTimeLine.value = activePools?.find { it.peersData.filter { it.type == "input" }.size == it.peers }?.id?.let { true } ?: false

        if (showRegisterInputTimeLine.value) {
            val selectedPool = activePools?.find { it.id == poolId } ?: throw IllegalStateException("Selected pool not found")
            viewModel.checkRegisteredInputs(
                selectedPool= selectedPool,
                onSuccess = {

                }
            )
        }

       // items.addAll(getCharacters())
    }

    if (showRegisterInputTimeLine.value) {
        VerticalDynamicTimeLine(items = items)
    } else {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp))
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(Res.string.input_registration).uppercase(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .clickable {
                                        scope.launch {
                                            getPoolsStore().reset()
                                        }
                                    },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            SelectionContainer {
                                Text(
                                    text = viewModel.getSelectedTxInfo()?.let { "${it.first}:${it.second}" } ?: "",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }

                        // TagCloud
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(64.dp)
                        ) {
                            listUnspent?.let { list ->
                                if (list.isNotEmpty()) {
                                    val state = rememberTagCloudState(
                                        onStartGesture = { autoRotation = false },
                                        onEndGesture = { autoRotation = true },
                                    )

                                    LaunchedEffect(state, autoRotation) {
                                        while (isActive && autoRotation && selectedTxId == null) {
                                            delay(10)
                                            state.rotateBy(0.001f, Vector3(1f, 1f, 1f))
                                        }
                                    }

                                    TagCloud(
                                        modifier = Modifier.fillMaxSize(),
                                        state = state
                                    ) {
                                        items(list) { item ->
                                            Box(
                                                modifier = Modifier.tagCloudItemFade(toAlpha = .5f)
                                            ) {
                                                val color = if (item.txid == selectedTxId?.txid && item.vout == selectedTxId?.vout) {
                                                    redDark
                                                } else Color.Transparent

                                                CustomOutlinedButton(
                                                    text = item.amount.convertFloatExponentialToString(),
                                                    color = color,
                                                    isSelected = item.txid == selectedTxId?.txid && item.vout == selectedTxId?.vout,
                                                    onClick = {
                                                        viewModel.setSelectedTxId(item.txid, item.vout)
                                                        autoRotation = selectedTxId == null
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else CenterColumnText(Res.string.no_amount_found_to_spend)
                            } ?: run {
                                CenterColumnText(Res.string.something_went_wrong)
                            }
                        }

                        // Button
                        Button(
                            modifier = Modifier.padding(16.dp),
                            shape = RoundedCornerShape(8.dp),
                            enabled = selectedTxId != null,
                            onClick = {
                                viewModel.registerInput(
                                    poolId = poolId,
                                    onError = {

                                    },
                                    onSuccess = { item ->
                                        showRegisterInputTimeLine.value = true
                                        items.add(item)
                                    }
                                )
                            }
                        ) {
                            Text(
                                text = stringResource(Res.string.register),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomOutlinedButton(
    modifier: Modifier = Modifier,
    text: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    val fontSize = 16.sp
    var textWidth by remember { mutableStateOf(0.dp) }
    val buttonSize = textWidth + 32.dp

    Box(
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (isSelected) Modifier.shadow(8.dp, CircleShape) else Modifier
            )
            .size(buttonSize)
            .background(color, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Unspecified,
            fontSize = fontSize,
            onTextLayout = { textLayoutResult: TextLayoutResult ->
                textWidth = with(density) { textLayoutResult.size.width.toDp() }
            },
            modifier = Modifier.padding(8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@ExperimentalAnimationApi
@Composable
fun VerticalDynamicTimeLine(
    modifier: Modifier = Modifier,
    items: SnapshotStateList<Item>,
) {
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            JetLimeColumn(
                modifier = Modifier.padding(32.dp),
                listState = listState,
                style = JetLimeDefaults.columnStyle(
                    lineBrush = JetLimeDefaults.lineGradientBrush(),
                ),
                itemsList = ItemsList(items),
                key = { _, item -> item.id },
            ) { index, item, position ->
                JetLimeEvent(
                    style = JetLimeEventDefaults.eventStyle(
                        position = position,
                        pointAnimation = if (index == items.lastIndex && index != 3) JetLimeEventDefaults.pointAnimation() else null,
                        pointType = if (index == items.lastIndex && index != 3) {
                            EventPointType.filled(0.5f)
                        } else {
                            EventPointType.custom(
                                icon = rememberVectorPainter(Icons.Default.CheckCircle),
                                tint = greenDark
                            )
                        }
                    )
                ) {
                    VerticalEventContent(item = item)
                }
            }
        }
    }
}

fun getCharacters(): MutableList<Item> {
    val eventId = "2e4b14f5d54a4190c0101b87382db1ce5ef9ec5db39dc2265bac5bd9d91cded2"
    val txId = "a40ae97da65ed66f279cc04c54ed5040e94cde39d11b6f2d1ea151855b49c931"
    val psbt = "cHNidP8BAHEBAAAAAfLM0vZk4Pb/TKeH8PErLIODeXv+M28W/Nu2ezmWyhzCAAAAAAD/////ApgrDwAAAAAAFgAU/+UQ7zfiNo+wVH7RjPFGJkuisOmYKw8AAAAAABYAFJwmTGPsh+qhvUc2XVbHffqMS1ctAAAAAAABAHECAAAAAUocGE+a3XytR2BZDuDs32zXLfQB9IHwf1vqxerhgWsXAAAAAAD9////AkBCDwAAAAAAFgAUXZScFsN1uQJ27p+5u8mQJ47EdQ79DZTllAAAABYAFPz4t4CcMYUtphL4IHbVyFNgu4e1AAAAAAEBH0BCDwAAAAAAFgAUXZScFsN1uQJ27p+5u8mQJ47EdQ4iAgKMROawXPu/6jlDHYDehoDm1AYXKZf/4ocoYOSnRJOSxkcwRAIgewYrIko+RjhNYt5ebkD7y2UmoS0gDoUblEP6So7NgMgCIHxv4zmjKB6o8Yq1y+MpHOKQCP/DigWF1o/uRii0ps2ZgQEDBIEAAAAiBgKMROawXPu/6jlDHYDehoDm1AYXKZf/4ocoYOSnRJOSxhhzPZ7eVAAAgAEAAIAAAACAAAAAABUAAAAAIgIC7ZxyiroDMszdByYUCe6RgU7wplrLFXME5eXqeR2vv8sYcz2e3lQAAIABAACAAAAAgAAAAACXAAAAACICA+AeP0Bd7Vs8RjhidixHZUOGofm+sWLOOOp7M/YQrUBDGHM9nt5UAACAAQAAgAAAAIAAAAAAlgAAAAA="
    return mutableListOf(
        Item(id = 0, title = "Register Input", description = "Input registered with event id: $eventId"),
        Item(id = 1, title = "Wait", description = "Waiting for other users to register input..."),
        Item(id = 2, title = "Finalize Coinjoin Tx", description = "PSBT: $psbt"),
        Item(id = 3, title = "Broadcast Tx", description = "TX: $txId", info = "https://mempool.space/signet/tx/a40ae97da65ed66f279cc04c54ed5040e94cde39d11b6f2d1ea151855b49c931"),
    )
}
