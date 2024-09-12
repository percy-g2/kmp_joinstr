package invincible.privacy.joinstr.ui.registerInput

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import invincible.privacy.joinstr.getPoolsStore
import invincible.privacy.joinstr.ktx.toExactString
import invincible.privacy.joinstr.theme.redDark
import invincible.privacy.joinstr.ui.components.CenterColumnText
import invincible.privacy.joinstr.ui.components.tagcloud.TagCloud
import invincible.privacy.joinstr.ui.components.tagcloud.math.Vector3
import invincible.privacy.joinstr.ui.components.tagcloud.rememberTagCloudState
import joinstr.composeapp.generated.resources.Res
import joinstr.composeapp.generated.resources.input_registration
import joinstr.composeapp.generated.resources.no_amount_found_to_spend
import joinstr.composeapp.generated.resources.register
import joinstr.composeapp.generated.resources.something_went_wrong
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterInputScreen(
    poolId: String,
    viewModel: RegisterInputViewModel = viewModel { RegisterInputViewModel() }
) {
    val isLoading by viewModel.isLoading
    val listUnspent by viewModel.listUnspent
    val selectedTxId by viewModel.selectedTxId
    var autoRotation by remember { mutableStateOf(false) }
    val showWaitingDialog = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showWaitingDialog.value) {
        BasicAlertDialog(
            onDismissRequest = {
                showWaitingDialog.value = false
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            content = {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.wrapContentSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Waiting for other users to register inputs...",
                            style = MaterialTheme.typography.labelLarge
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CircularProgressIndicator()
                    }
                }
            }
        )
    }

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
                                    while (isActive && autoRotation && selectedTxId.isEmpty()) {
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
                                            val color = if (item.txid == selectedTxId) {
                                                redDark
                                            } else Color.Transparent

                                            CustomOutlinedButton(
                                                text = item.amount.toExactString(),
                                                color = color,
                                                isSelected = item.txid == selectedTxId,
                                                onClick = {
                                                    viewModel.setSelectedTxId(item.txid)
                                                    autoRotation = selectedTxId.isEmpty()
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
                        enabled = selectedTxId.isNotEmpty(),
                        onClick = {
                            viewModel.registerInput(poolId) {
                                showWaitingDialog.value = true
                            }
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

@Composable
fun CustomOutlinedButton(
    modifier: Modifier = Modifier,
    text: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
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