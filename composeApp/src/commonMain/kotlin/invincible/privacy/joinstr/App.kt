package invincible.privacy.joinstr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import invincible.privacy.joinstr.model.RpcRequestBody
import invincible.privacy.joinstr.network.HttpClient
import invincible.privacy.joinstr.network.json
import joinstr.composeapp.generated.resources.Res
import joinstr.composeapp.generated.resources.joinstr_logo
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        val httpClient = remember { HttpClient() }
        val coroutineScope = rememberCoroutineScope()

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.wrapContentSize())
                } else {

                    Button(onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            showContent = !showContent
                            val body = RpcRequestBody(
                                jsonrpc = "1.0",
                                id = "curltest",
                                method = "getblockchaininfo"
                            )
                            val response = httpClient.fetchBlockChainInfo(body)
                            println(json.encodeToString(response))
                            isLoading = false
                        }
                    }) {
                        Text("Click me!")
                    }
                    AnimatedVisibility(showContent) {
                        val greeting = remember { Greeting().greet() }
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(painterResource(Res.drawable.joinstr_logo), null)
                            Text("Compose: $greeting")
                        }
                    }
                }
            }
        }
    }
}