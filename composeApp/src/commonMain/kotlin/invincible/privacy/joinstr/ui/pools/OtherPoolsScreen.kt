package invincible.privacy.joinstr.ui.pools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import invincible.privacy.joinstr.ktx.hexToByteArray
import invincible.privacy.joinstr.ktx.toHexString
import invincible.privacy.joinstr.model.PoolContent
import invincible.privacy.joinstr.model.copyToLocalPoolContent
import invincible.privacy.joinstr.ui.components.CenterColumnText
import invincible.privacy.joinstr.utils.NostrCryptoUtils.generatePrivateKey
import invincible.privacy.joinstr.utils.NostrCryptoUtils.getPublicKey
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrColors
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import io.ktor.util.*
import joinstr.composeapp.generated.resources.Res
import joinstr.composeapp.generated.resources.no_active_pools
import joinstr.composeapp.generated.resources.pool_request
import joinstr.composeapp.generated.resources.something_went_wrong
import joinstr.composeapp.generated.resources.waiting_for_pool_credentials
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherPoolsScreen(
    poolsViewModel: PoolsViewModel
) {
    val poolContents by poolsViewModel.otherPoolEvents.collectAsState(initial = null)
    val isLoading by poolsViewModel.isLoading.collectAsState()
    val showJoinDialog = remember { mutableStateOf(false) }
    val showWaitingDialog = remember { mutableStateOf(false) }
    val showQrCodeDialog = remember { mutableStateOf(Pair<PoolContent?, Boolean>(null, false)) }
    val activePoolReady by poolsViewModel.activePoolReady.collectAsState()

    LaunchedEffect(Unit) {
        poolsViewModel.fetchOtherPools()
    }

    if (showWaitingDialog.value && activePoolReady) {
        showWaitingDialog.value = false
    }

    if (showQrCodeDialog.value.second) {
        BasicAlertDialog(
            onDismissRequest = {
                showQrCodeDialog.value = Pair(null, false)
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            content = {

                showQrCodeDialog.value.first?.let { poolContent ->

                    val privateKey = generatePrivateKey()
                    val publicKey = if (PlatformUtils.IS_BROWSER) {
                        getPublicKey(privateKey).drop(1).take(32).toByteArray()
                    } else getPublicKey(privateKey)

                    val qrCodeColor = MaterialTheme.colorScheme.onBackground
                    val painter = rememberQrCodePainter(
                        data = poolContent.publicKey,
                        colors = QrColors(
                            dark = QrBrush.solid(
                                color = qrCodeColor
                            )
                        )
                    )

                    Box(
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .wrapContentSize()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painter,
                                contentDescription = "QR Code",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                contentScale = ContentScale.Fit
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            SelectionContainer {
                                Text(
                                    text = "Public Key: ${publicKey.toHexString()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                shape = RoundedCornerShape(8.dp),
                                onClick = {
                                    showQrCodeDialog.value = Pair(null, false)
                                    poolsViewModel.joinRequest(
                                        publicKey = publicKey,
                                        privateKey = privateKey,
                                        poolPublicKey = poolContent.publicKey,
                                        showJoinDialog = showJoinDialog
                                    ) { pool ->
                                        showWaitingDialog.value = true
                                        poolsViewModel.checkRegisteredOutputs(
                                            poolId = pool.id,
                                            privateKey = pool.privateKey.hexToByteArray(),
                                            publicKey = pool.publicKey.hexToByteArray(),
                                            showWaitingDialog = showWaitingDialog
                                        )
                                    }
                                }
                            ) {
                                Text(
                                    text = "Continue",
                                    fontSize = 16.sp,
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                showQrCodeDialog.value = Pair(null, false)
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 12.dp, y = (-12).dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }
            }
        )
    }

    if (showJoinDialog.value) {
        BasicAlertDialog(
            onDismissRequest = {
                showJoinDialog.value = false
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
                            text = stringResource(Res.string.pool_request),
                            style = MaterialTheme.typography.labelMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(Res.string.waiting_for_pool_credentials),
                            style = MaterialTheme.typography.labelSmall
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CircularProgressIndicator()
                    }
                }
            }
        )
    }

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
                            text = "Waiting for other users to register outputs...",
                            style = MaterialTheme.typography.labelLarge
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CircularProgressIndicator()
                    }
                }
            }
        )
    }

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter
    ) {
        if (isLoading) {
            ShimmerEventList()
        } else {
            poolContents?.let { list ->
                if (list.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.wrapContentSize(),
                    ) {
                        items(list) { poolContent ->
                            var isVisible by remember { mutableStateOf(true) }
                            AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                PoolItem(
                                    poolContent = copyToLocalPoolContent(poolContent),
                                    onJoinRequest = {
                                        showQrCodeDialog.value = Pair(poolContent, true)
                                    },
                                    onTimeout = {
                                        isVisible = false
                                        poolsViewModel.removeOtherPool(poolContent.id)
                                    }
                                )
                            }
                        }
                    }
                } else CenterColumnText(Res.string.no_active_pools)
            } ?: run {
                CenterColumnText(Res.string.something_went_wrong)
            }
        }
    }
}

@Composable
fun ShimmerEventList() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(all = 8.dp)
    ) {
        items(10) {
            ShimmerEventItem()
        }
    }
}

@Composable
fun ShimmerEventItem() {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition()
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            )
        )
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Relay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // PubKey
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Denomination
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Peers
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Timeout
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Countdown Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(brush)
            )
        }
    }
}