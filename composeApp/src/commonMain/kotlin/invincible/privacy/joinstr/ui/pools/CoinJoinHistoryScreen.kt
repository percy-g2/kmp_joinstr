package invincible.privacy.joinstr.ui.pools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import invincible.privacy.joinstr.model.CoinJoinHistory
import invincible.privacy.joinstr.openLink
import invincible.privacy.joinstr.theme.lightBlue
import invincible.privacy.joinstr.ui.components.CenterColumnText
import joinstr.composeapp.generated.resources.Res
import joinstr.composeapp.generated.resources.no_history_found
import joinstr.composeapp.generated.resources.something_went_wrong
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinJoinHistoryScreen(poolsViewModel: PoolsViewModel) {
    val events by poolsViewModel.coinJoinHistory.collectAsState()
    val isLoading by poolsViewModel.isLoading.collectAsState()
    val pullState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        poolsViewModel.fetchCoinJoinHistory()
    }

    HistoryList(
        isLoading = isLoading,
        events = events,
        pullRefreshState = pullState,
        poolsViewModel = poolsViewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryList(
    isLoading: Boolean,
    events: List<CoinJoinHistory>?,
    pullRefreshState: PullToRefreshState,
    poolsViewModel: PoolsViewModel,
) {
    BoxWithConstraints(
        contentAlignment = TopCenter
    ) {
        when {
            isLoading -> ShimmerHistoryList()
            else -> HistoryListContent(
                isLoading = isLoading,
                events = events,
                pullRefreshState = pullRefreshState,
                onRefresh = {
                    poolsViewModel.fetchCoinJoinHistory()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.HistoryListContent(
    isLoading: Boolean,
    events: List<CoinJoinHistory>?,
    pullRefreshState: PullToRefreshState,
    onRefresh: () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isLoading,
        state = pullRefreshState,
        modifier = Modifier.align(TopCenter),
        onRefresh = onRefresh
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = if (events.isNullOrEmpty()) Arrangement.Center else Arrangement.Top
        ) {
            when {
                !events.isNullOrEmpty() -> {
                    items(events) { coinJoinHistory ->
                        HistoryItem(coinJoinHistory = coinJoinHistory)
                    }
                }

                events == null -> {
                    item { CenterColumnText(Res.string.something_went_wrong) }
                }

                else -> {
                    item { CenterColumnText(Res.string.no_history_found) }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    coinJoinHistory: CoinJoinHistory
) {
    var isPrivateKeyVisible by remember { mutableStateOf(false) }
    var isDetailExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            InfoRow("Tx", coinJoinHistory.tx, isClickable = true) {
                openLink("https://mempool.space/signet/tx/${coinJoinHistory.tx}")
            }
            InfoRow("Relay", coinJoinHistory.relay)
            InfoRow("Amount", "${coinJoinHistory.amount} BTC")

            AnimatedVisibility(visible = isDetailExpanded) {
                Column {
                    InfoRow("Pool Public Key", coinJoinHistory.publicKey, isSelectable = true)
                    InfoRow(
                        label = "Pool Private Key",
                        value = if (isPrivateKeyVisible) coinJoinHistory.privateKey else "••••••••••••••••",
                        isSelectable = isPrivateKeyVisible,
                        trailingIcon = {
                            IconButton(onClick = { isPrivateKeyVisible = !isPrivateKeyVisible }) {
                                Icon(
                                    imageVector = if (isPrivateKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPrivateKeyVisible) "Hide private key" else "Show private key"
                                )
                            }
                        }
                    )
                    InfoRow("PSBT", coinJoinHistory.psbt, isSelectable = true)
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { isDetailExpanded = !isDetailExpanded },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = if (isDetailExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isDetailExpanded) "Collapse details" else "Expand details"
                    )
                }

                Text(
                    text = formatTimestamp(coinJoinHistory.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    isSelectable: Boolean = false,
    isClickable: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(120.dp)
        )
        if (isSelectable) {
            SelectionContainer(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            val textModifier = if (isClickable) {
                Modifier
                    .weight(1f)
                    .clickable(onClick = onClick ?: {})
            } else {
                Modifier.weight(1f)
            }
            Text(
                modifier = textModifier,
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isClickable) lightBlue else Color.Unspecified
            )
        }
        trailingIcon?.invoke()
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return buildString {
        append(dateTime.year)
        append('-')
        append(dateTime.monthNumber.toString().padStart(2, '0'))
        append('-')
        append(dateTime.dayOfMonth.toString().padStart(2, '0'))
        append(' ')
        append(dateTime.hour.toString().padStart(2, '0'))
        append(':')
        append(dateTime.minute.toString().padStart(2, '0'))
    }
}

@Composable
fun ShimmerHistoryItem() {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    ShimmerHistoryItemContent(brush)
}

@Composable
fun ShimmerHistoryItemContent(brush: Brush) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            repeat(3) {
                ShimmerInfoRow(brush)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(
                    modifier = Modifier
                        .width(100.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Spacer(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
            }
        }
    }
}

@Composable
fun ShimmerInfoRow(brush: Brush) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Spacer(
            modifier = Modifier
                .width(100.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
    }
}

@Composable
fun ShimmerHistoryList() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(5) {
            ShimmerHistoryItem()
        }
    }
}