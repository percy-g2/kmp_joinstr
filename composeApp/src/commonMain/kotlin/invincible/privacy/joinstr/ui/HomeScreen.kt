package invincible.privacy.joinstr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import invincible.privacy.joinstr.model.BlockChainInfo
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.network.HttpClient
import invincible.privacy.joinstr.ui.components.ProgressDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var isLoading by remember { mutableStateOf(true) }
    val httpClient = remember { HttpClient() }
    var blockChainInfo by remember { mutableStateOf<BlockChainInfo?>(null) }

    LaunchedEffect(Unit) {
        val rpcRequestBody = RpcRequestBody(
            id = "curltest",
            jsonrpc =  "1.0",
            method = "getblockchaininfo"
        )
        blockChainInfo = httpClient.fetchBlockChainInfo(rpcRequestBody)
        isLoading = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Text("Node Information")
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            if (isLoading) {
                ProgressDialog()
            } else {
                blockChainInfo?.let { data ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = "Chain: ${data.result.chain}",
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Blocks: ${data.result.blocks}",
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Best block hash: ${data.result.bestblockhash}",
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Difficulty: ${data.result.difficulty}",
                            fontSize = 18.sp
                        )
                    }
                } ?: run {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Incorrect node info!!",
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}