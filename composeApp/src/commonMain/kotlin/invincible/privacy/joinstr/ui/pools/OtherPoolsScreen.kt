package invincible.privacy.joinstr.ui.pools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import invincible.privacy.joinstr.convertFloatExponentialToString
import invincible.privacy.joinstr.ktx.displayDateTime
import invincible.privacy.joinstr.model.PoolCreationContent
import invincible.privacy.joinstr.utils.SettingsManager
import invincible.privacy.joinstr.utils.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherPoolsScreen(
    poolsViewModel: PoolsViewModel
) {
    val poolContents by poolsViewModel.otherPoolEvents.collectAsState(initial = null)
    val isLoading by poolsViewModel.isLoading.collectAsState()
    var showJoinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        poolsViewModel.fetchOtherPools()
    }

    val selectedTheme = SettingsManager.themeState.value
    val isDarkTheme = selectedTheme == Theme.DARK.id || (selectedTheme == Theme.SYSTEM.id && isSystemInDarkTheme())

    if (showJoinDialog) {
        BasicAlertDialog(
            onDismissRequest = {
                showJoinDialog = false
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            content = {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.background,
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
                            text = "Pool Request",
                            style = MaterialTheme.typography.labelMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Waiting for pool credentials...",
                            style = MaterialTheme.typography.labelSmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        CircularProgressIndicator()
                    }
                }
            }
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(4.dp),
                ambientColor = if (isDarkTheme) Color.White else Color.Black,
                spotColor = if (isDarkTheme) Color.White else Color.Black
            )
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(4.dp)),
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
                                EventItem(
                                    poolContent = poolContent,
                                    onJoinRequest = {
                                        poolsViewModel.joinRequest(
                                            replay = poolContent.relay,
                                            poolPublicKey = poolContent.publicKey,
                                            denomination = poolContent.denomination,
                                            peers = poolContent.peers
                                        ) {
                                            showJoinDialog = true
                                        }
                                    },
                                    onTimeout = {
                                        isVisible = false
                                        poolsViewModel.removeOtherPool(poolContent.id)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No pools available",
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } ?: run {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Something went wrong!\nCheck your settings",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun EventItem(
    poolContent: PoolCreationContent,
    onJoinRequest: () -> Unit,
    onTimeout: () -> Unit
) {
    var isTimedOut by remember { mutableStateOf(false) }
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(isTimedOut) {
        if (isTimedOut) {
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
            onTimeout()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Relay: ${poolContent.relay}",
                style = MaterialTheme.typography.labelSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "PubKey: ${poolContent.publicKey}", style = MaterialTheme.typography.labelSmall)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Denomination: ${poolContent.denomination.convertFloatExponentialToString()}",
                style = MaterialTheme.typography.labelSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Peers: ${poolContent.peers}", style = MaterialTheme.typography.labelSmall)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Timeout at: ${poolContent.timeout.displayDateTime()}",
                style = MaterialTheme.typography.labelSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            CountdownTimer(
                targetTime = poolContent.timeout,
                onTimeout = { isTimedOut = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    shape = RoundedCornerShape(8.dp),
                    onClick = onJoinRequest
                ) {
                    Text(
                        text = "Join",
                        fontSize = 16.sp,
                    )
                }
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
    }
}