package invincible.privacy.joinstr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun ListUnspentCloudsScreen() {
    var isLoading by remember { mutableStateOf(true) }
    val httpClient = remember { HttpClient() }
    var listUnspent by remember { mutableStateOf<List<ListUnspentResponseItem>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var selectedTxId by remember { mutableStateOf("") }
    var autoRotation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Input Registration".uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val unspent = listUnspent.find { it.txid == selectedTxId }

                Text(
                    text = if (selectedTxId.isNotEmpty()) "${unspent?.txid} | ${unspent?.vout}" else "",
                    fontSize = 16.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(all = 8.dp)
                )

                if (listUnspent.isNotEmpty()) {
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
                        modifier = Modifier
                            .width((listUnspent.size * 70).dp)
                            .height((listUnspent.size * 30).dp)
                            .padding(all = 64.dp),
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
                                    isSelected = item.txid == selectedTxId,
                                    onClick = {
                                        selectedTxId = if (selectedTxId == item.txid) {
                                            autoRotation = true
                                            ""
                                        } else {
                                            item.txid
                                        }
                                    }
                                )
                            }
                        }
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
            Button(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
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
                    text = "Register",
                    fontSize = 16.sp
                )
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
            modifier = Modifier.padding(8.dp)
        )
    }
}