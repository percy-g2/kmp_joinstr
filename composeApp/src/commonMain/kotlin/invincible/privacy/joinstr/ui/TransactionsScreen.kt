package invincible.privacy.joinstr.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.model.Transaction
import invincible.privacy.joinstr.network.HttpClient
import invincible.privacy.joinstr.theme.green
import invincible.privacy.joinstr.theme.red
import invincible.privacy.joinstr.ui.components.ProgressDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen() {
    var isLoading by remember { mutableStateOf(true) }
    val httpClient = remember { HttpClient() }
    var transactions by remember { mutableStateOf<List<Transaction>?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val rpcRequestBody = RpcRequestBody(
                method = "listtransactions"
            )
            transactions = httpClient.fetchTransactions(rpcRequestBody)
            isLoading = false
        }
    }

    data class TableHeader(
        val title: String,
        val cellWidth: Dp = Dp.Unspecified,
        val cellAlignment: TextAlign = TextAlign.Start
    )

    val headers = remember {
        listOf(
            TableHeader("Label", cellAlignment = TextAlign.Start),
            TableHeader("Amount (BTC)", cellAlignment = TextAlign.Center),
            TableHeader("Confirmations", cellAlignment = TextAlign.Center),
            TableHeader("Time", cellAlignment = TextAlign.Center)
        )
    }
    val tableWidth by derivedStateOf { headers.sumOf { it.cellWidth.value.toInt() }.dp }

    if (isLoading) {
        ProgressDialog()
    }
    transactions?.let { list ->
        BoxWithConstraints(
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(4.dp))
        ) {
            val isCompact = maxWidth <= tableWidth

            LazyColumn(
                modifier = if (isCompact) {
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .requiredWidth(tableWidth)
                } else Modifier.fillMaxWidth()
            ) {
                stickyHeader {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .widthIn(tableWidth)
                            .shadow(
                                elevation = 2.dp,
                                spotColor = MaterialTheme.colorScheme.onBackground,
                                ambientColor = MaterialTheme.colorScheme.onBackground
                            )
                    ) {
                        headers.forEach { header ->
                            Text(
                                text = header.title,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    textAlign = header.cellAlignment,
                                    fontWeight = FontWeight.Medium,
                                ),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .weight(1f)
                            )
                        }
                    }
                }

                itemsIndexed(list) { index, row ->
                    if (index != 0) {
                        HorizontalDivider(color = LightGray)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { /* TODO */ }
                    ) {
                        Text(
                            text = row.address,
                            style = MaterialTheme.typography.labelMedium.copy(
                                textAlign = TextAlign.Start,
                            ),
                            maxLines = 1,
                            modifier = Modifier
                                .widthIn(headers[0].cellWidth)
                                .padding(16.dp)
                                .weight(1f),
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = row.amount.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                textAlign = TextAlign.Center,
                                color = if (row.category == "receive") {
                                    green
                                } else red
                            ),
                            maxLines = 1,
                            modifier = Modifier
                                .widthIn(headers[1].cellWidth)
                                .padding(16.dp)
                                .weight(1f),
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = row.confirmations.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                textAlign = TextAlign.Center,
                            ),
                            maxLines = 1,
                            modifier = Modifier
                                .widthIn(headers[2].cellWidth)
                                .padding(16.dp)
                                .weight(1f),
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = row.timereceived,
                            style = MaterialTheme.typography.labelMedium.copy(
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 1,
                            modifier = Modifier
                                .widthIn(headers[3].cellWidth)
                                .padding(16.dp)
                                .weight(1f),
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    } ?: run {
        if (isLoading.not()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Failed to connect to the node!\n Please check your configuration and try again",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}