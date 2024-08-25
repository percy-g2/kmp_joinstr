package invincible.privacy.joinstr.ui.pools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import invincible.privacy.joinstr.convertFloatExponentialToString
import invincible.privacy.joinstr.model.PoolContent
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun MyPoolsScreens(
    poolsViewModel: PoolsViewModel
) {
    val events by poolsViewModel.localPools.collectAsState()
    val isLoading by poolsViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        poolsViewModel.fetchLocalPools()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .shadow(4.dp, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.TopCenter
    ) {
        events?.let { list ->
            if (list.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.wrapContentSize(),
                ) {
                    items(list) { event ->
                        PoolItem(event)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No pools available, please create one!",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } ?: run {
            if (isLoading.not()) {
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
fun PoolItem(poolContent: PoolContent) {
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
            Text(text = "Relay: ${poolContent.relay}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Author: ${poolContent.publicKey.take(8)}...", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Denomination: ${poolContent.denomination.convertFloatExponentialToString()}", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Peers: ${poolContent.peers}", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val instant = Instant.fromEpochMilliseconds(poolContent.timeout * 1000)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

            val month = localDateTime.month.name.take(3).lowercase().capitalize(Locale.current) // "Aug"
            val date = localDateTime.dayOfMonth.toString().padStart(2, '0') // "05"
            val year = localDateTime.year.toString() // "2006"

            val hour = if (localDateTime.hour % 12 == 0) 12 else localDateTime.hour % 12
            val minute = localDateTime.minute.toString().padStart(2, '0') // "05"
            val second = localDateTime.second.toString().padStart(2, '0') // "15"
            val period = if (localDateTime.hour < 12) "am" else "pm" // "PM"

            Text(
                text = "Created at: $hour:$minute:$second $period $month $date $year",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}