package invincible.privacy.joinstr.ui.pools

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import invincible.privacy.joinstr.ktx.displayDateTime
import invincible.privacy.joinstr.model.LocalPoolContent
import invincible.privacy.joinstr.theme.greenDark
import invincible.privacy.joinstr.theme.greenLight
import invincible.privacy.joinstr.theme.orangeLight
import invincible.privacy.joinstr.theme.redDark
import invincible.privacy.joinstr.theme.redLight
import invincible.privacy.joinstr.theme.yellowDark
import invincible.privacy.joinstr.ui.components.CenterColumnText
import invincible.privacy.joinstr.utils.SettingsManager
import invincible.privacy.joinstr.utils.Theme
import joinstr.composeapp.generated.resources.Res
import joinstr.composeapp.generated.resources.join
import joinstr.composeapp.generated.resources.no_active_pools
import joinstr.composeapp.generated.resources.something_went_wrong
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPoolsScreens(
    viewModel: MyPoolsViewModel = viewModel { MyPoolsViewModel() }
) {
    val events by viewModel.localPools.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pullState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        viewModel.fetchLocalPools()
    }

    PoolList(
        isLoading = isLoading,
        events = events,
        pullRefreshState = pullState,
        poolsViewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolList(
    isLoading: Boolean,
    events: List<LocalPoolContent>?,
    pullRefreshState: PullToRefreshState,
    poolsViewModel: MyPoolsViewModel
) {
    BoxWithConstraints(
        contentAlignment = TopCenter
    ) {
        when {
            isLoading -> ShimmerEventList()
            else -> PoolListContent(
                isLoading = isLoading,
                events = events,
                pullRefreshState = pullRefreshState,
                onRefresh = {
                    poolsViewModel.fetchLocalPools()
                },
                onRemovePool = { poolsViewModel.removeLocalPool(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.PoolListContent(
    isLoading: Boolean,
    events: List<LocalPoolContent>?,
    pullRefreshState: PullToRefreshState,
    onRefresh: () -> Unit,
    onRemovePool: (String) -> Unit,
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
                    items(events) { event ->
                        PoolItemWrapper(
                            poolContent = event,
                            onTimeout = { onRemovePool(event.id) }
                        )
                    }
                }

                events == null -> {
                    item { CenterColumnText(Res.string.something_went_wrong) }
                }

                else -> {
                    item { CenterColumnText(Res.string.no_active_pools) }
                }
            }
        }
    }
}

@Composable
private fun PoolItemWrapper(
    poolContent: LocalPoolContent,
    onTimeout: () -> Unit,
) {
    var isVisible by remember { mutableStateOf(true) }
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        PoolItem(
            isInternal = true,
            poolContent = poolContent,
            onTimeout = {
                isVisible = false
                onTimeout()
            }
        )
    }
}

@Composable
fun PoolItem(
    poolContent: LocalPoolContent,
    isInternal: Boolean = false,
    isJoining: Boolean = false,
    onJoinRequest: (() -> Unit)? = null,
    onTimeout: () -> Unit,
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
            .padding(8.dp)
            .graphicsLayer {
                rotationX = animatedProgress.value * 90f
                alpha = 1f - animatedProgress.value
            },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            SelectionContainer {
                Text(
                    text = "Relay: ${poolContent.relay}",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SelectionContainer {
                Text(
                    text = "PubKey: ${poolContent.publicKey}",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Denomination: ${poolContent.denomination.toBigDecimal().toStringExpanded()}",
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

            if (isInternal.not()) {

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (onJoinRequest != null) {
                        Button(
                            shape = RoundedCornerShape(8.dp),
                            onClick = onJoinRequest,
                            enabled = isJoining.not()
                        ) {
                            Text(
                                text = stringResource(Res.string.join),
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CountdownTimer(targetTime: Long, onTimeout: () -> Unit) {
    val currentTime = remember { mutableStateOf(Clock.System.now().epochSeconds) }
    val remainingTime = remember { mutableStateOf(targetTime - currentTime.value) }
    val progress = remember { Animatable(1f) }
    val totalDuration = 600f // 10 minutes in seconds

    LaunchedEffect(Unit) {
        while (remainingTime.value > 0) {
            currentTime.value = Clock.System.now().epochSeconds
            remainingTime.value = (targetTime - currentTime.value).coerceAtLeast(0)
            if (remainingTime.value == 0L) {
                onTimeout()
                break
            }
            delay(1.seconds)
        }
    }

    LaunchedEffect(remainingTime.value) {
        progress.animateTo(
            targetValue = (remainingTime.value / totalDuration).coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
        )
    }

    val selectedTheme = SettingsManager.themeState.value
    val isDark = selectedTheme == Theme.DARK.id || (selectedTheme == Theme.SYSTEM.id && isSystemInDarkTheme())

    val progressColors = object {
        val green: Color
            @Composable
            get() = if (isDark) greenDark else greenLight

        val middle: Color
            @Composable
            get() = if (isDark) yellowDark else orangeLight

        val red: Color
            @Composable
            get() = if (isDark) redDark else redLight
    }

    val timeColor = when {
        progress.value > 0.6f -> progressColors.green
        progress.value > 0.3f -> progressColors.middle
        else -> progressColors.red
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Time remaining: ",
                style = MaterialTheme.typography.labelSmall
            )
            AnimatedCountdownDisplay(remainingTime.value, timeColor, MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(4.dp))

        CustomProgressIndicator(
            progress = progress.value,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = timeColor,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun AnimatedCountdownDisplay(remainingTime: Long, color: Color, style: TextStyle) {
    val minutes = remainingTime / 60
    val seconds = remainingTime % 60

    Row {
        Text(
            text = "${minutes.toString().padStart(2, '0')}:",
            color = color,
            style = style
        )

        Text(
            text = (seconds / 10).toString(),
            color = color,
            style = style
        )

        AnimatedContent(
            targetState = seconds % 10,
            transitionSpec = {
                slideInVertically { height -> height } + fadeIn() togetherWith
                    slideOutVertically { height -> -height } + fadeOut()
            }
        ) { digit ->
            Text(
                text = digit.toString(),
                color = color,
                style = style
            )
        }
    }
}

@Composable
fun CustomProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color,
    backgroundColor: Color,
    height: Dp = 4.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        drawBackgroundTrack(backgroundColor, height)
        drawProgressBar(progress, color, height)
    }
}

private fun DrawScope.drawBackgroundTrack(
    backgroundColor: Color,
    height: Dp
) {
    drawLine(
        color = backgroundColor,
        start = Offset(0f, size.height / 2),
        end = Offset(size.width, size.height / 2),
        strokeWidth = height.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawProgressBar(
    progress: Float,
    color: Color,
    height: Dp
) {
    val progressWidth = progress * size.width
    drawLine(
        color = color,
        start = Offset(0f, size.height / 2),
        end = Offset(progressWidth, size.height / 2),
        strokeWidth = height.toPx(),
        cap = StrokeCap.Round
    )
}