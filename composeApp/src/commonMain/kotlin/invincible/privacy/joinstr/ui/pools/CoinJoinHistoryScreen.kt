package invincible.privacy.joinstr.ui.pools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
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
            isLoading -> ShimmerEventList()
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
                modifier = Modifier.fillMaxWidth(),
                text = "CoinJoin Transaction",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            InfoRow("Relay", coinJoinHistory.relay)
            InfoRow("Amount", "${coinJoinHistory.amount} BTC")
            InfoRow("Address", coinJoinHistory.address)
            InfoRow("Timestamp", formatTimestamp(coinJoinHistory.timestamp))

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Advanced Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))

            InfoRow("Public Key", coinJoinHistory.publicKey, isSelectable = true)
            InfoRow("Private Key", coinJoinHistory.privateKey, isSelectable = true)
            InfoRow("PSBT", coinJoinHistory.psbt, isSelectable = true)
            InfoRow("TX", coinJoinHistory.tx, isSelectable = true) {
                openLink("https://mempool.space/signet/tx/${coinJoinHistory.tx}")
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    isSelectable: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )
        if (isSelectable) {
            SelectionContainer {
                val modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                Text(
                    modifier = modifier,
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (onClick != null) lightBlue else Color.Unspecified
                )
            }
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return buildString {
        append(dateTime.hour.toString().padStart(2, '0'))
        append(':')
        append(dateTime.minute.toString().padStart(2, '0'))
        append(' ')
        append(dateTime.year)
        append('-')
        append(dateTime.monthNumber.toString().padStart(2, '0'))
        append('-')
        append(dateTime.dayOfMonth.toString().padStart(2, '0'))
    }
}