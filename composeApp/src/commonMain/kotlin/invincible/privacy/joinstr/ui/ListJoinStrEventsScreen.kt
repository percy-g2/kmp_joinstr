package invincible.privacy.joinstr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import invincible.privacy.joinstr.model.NostrEvent
import invincible.privacy.joinstr.network.NostrClient
import invincible.privacy.joinstr.ui.components.ProgressDialog
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

@Composable
fun ListJoinStrEventsScreen() {
    val nostrClient = remember { NostrClient() }
    val events by nostrClient.events.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()


    LaunchedEffect(Unit) {
        coroutineScope.launch {
            nostrClient.connectAndListen(
                onReceived = {
                    isLoading = false
                }
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .shadow(4.dp, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {

        if (isLoading) {
            ProgressDialog()
        }

        LazyColumn(
            modifier = Modifier.wrapContentSize(),
        ) {
            items(events) { event ->
                EventItem(event)
            }
        }
    }
}



@Composable
fun EventItem(event: NostrEvent) {
    Card(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(text = event.content, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Author: ${event.pubKey.take(8)}...", style = MaterialTheme.typography.labelSmall)
            Text(text = "Created at: ${Instant.fromEpochMilliseconds(event.createdAt * 1000)}", style = MaterialTheme.typography.labelSmall)
        }
    }
}