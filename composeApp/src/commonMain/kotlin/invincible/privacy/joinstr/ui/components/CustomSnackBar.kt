package invincible.privacy.joinstr.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext


private val LocalSnackbarController = staticCompositionLocalOf {
    SnackbarController(
        host = SnackbarHostState(),
        scope = CoroutineScope(EmptyCoroutineContext)
    )
}
private val channel = Channel<SnackbarChannelMessage>(capacity = Int.MAX_VALUE)

@Composable
fun SnackbarControllerProvider(content: @Composable (snackbarHost: SnackbarHostState) -> Unit) {
    val snackHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackController = remember(scope) { SnackbarController(snackHostState, scope) }

    DisposableEffect(snackController, scope) {
        val job = scope.launch {
            for (payload in channel) {
                snackController.showMessage(
                    message = payload.message,
                    duration = payload.duration,
                    action = payload.action
                )
            }
        }

        onDispose {
            job.cancel()
        }
    }

    CompositionLocalProvider(LocalSnackbarController provides snackController) {
        content(
            snackHostState
        )
    }
}

@Immutable
class SnackbarController(
    private val host: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    companion object {
        val current
            @Composable
            @ReadOnlyComposable
            get() = LocalSnackbarController.current

        fun showMessage(
            message: String,
            action: SnackbarAction? = null,
            onDismissAction: SnackbarAction? = null,
            duration: SnackbarDuration = SnackbarDuration.Short,
        ) {
            channel.trySend(
                SnackbarChannelMessage(
                    message = message,
                    duration = duration,
                    action = action,
                    onDismissAction = onDismissAction
                )
            )
        }
    }


    fun showMessage(
        message: String,
        action: SnackbarAction? = null,
        onDismissAction: SnackbarAction? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
    ) {
        scope.launch {
            /**
             * note: uncomment this line if you want snackbar to be displayed immediately,
             * rather than being enqueued and waiting [duration] * current_queue_size
             */
            // host.currentSnackbarData?.dismiss()
            val result =
                host.showSnackbar(
                    message = message,
                    actionLabel = action?.title,
                    duration = duration
                )

            if (result == SnackbarResult.ActionPerformed) {
                action?.onActionPress?.invoke()
            }
            if (result == SnackbarResult.Dismissed) {
                onDismissAction?.onActionPress?.invoke()
            }
        }
    }
}

data class SnackbarChannelMessage(
    val message: String,
    val action: SnackbarAction?,
    val onDismissAction: SnackbarAction?,
    val duration: SnackbarDuration = SnackbarDuration.Short,
)


data class SnackbarAction(val title: String, val onActionPress: () -> Unit)

@Composable
fun CustomStackedSnackbar(
    snackbarData: SnackbarData,
    onActionClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardSnackbarContainer(
        modifier = modifier.clickable {
            onActionClicked.invoke()
        },
        content = {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {

                Column {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = snackbarData.visuals.message,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
    )
}

@Composable
private fun CardSnackbarContainer(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .wrapContentHeight()
            .then(modifier),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
    ) {
        content.invoke()
    }
}