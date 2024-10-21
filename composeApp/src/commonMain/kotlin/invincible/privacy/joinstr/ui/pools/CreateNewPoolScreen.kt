package invincible.privacy.joinstr.ui.pools

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import invincible.privacy.joinstr.ui.PoolsViewModel
import invincible.privacy.joinstr.ui.components.SnackbarController
import joinstr.composeapp.generated.resources.Res
import joinstr.composeapp.generated.resources.denomination
import joinstr.composeapp.generated.resources.denomination_exceeded
import joinstr.composeapp.generated.resources.denomination_support_txt
import joinstr.composeapp.generated.resources.invalid_denomination
import joinstr.composeapp.generated.resources.pool_details
import joinstr.composeapp.generated.resources.waiting_for_outputs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNewPoolScreen(
    poolsViewModel: PoolsViewModel
) {
    val activePoolReady by poolsViewModel.activePoolReady.collectAsState()
    val showWaitingDialog = remember { mutableStateOf(false) }

    if (showWaitingDialog.value && activePoolReady.first) {
        showWaitingDialog.value = false
    }

    if (showWaitingDialog.value) {
        BasicAlertDialog(
            onDismissRequest = {
                showWaitingDialog.value = false
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            content = {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainer,
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
                            text = stringResource(Res.string.waiting_for_outputs),
                            style = MaterialTheme.typography.labelLarge
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CircularProgressIndicator()
                    }
                }
            }
        )
    }

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter
    ) {
        val (focusRequester) = FocusRequester.createRefs()
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        var denomination by remember { mutableStateOf(TextFieldValue("")) }
        var peers by remember { mutableStateOf("") }

        val invalidDenomination = stringResource(Res.string.invalid_denomination)
        val denominationExceeded = stringResource(Res.string.denomination_exceeded)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 16.dp)
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Text(
                text = stringResource(Res.string.pool_details),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            OutlinedTextField(
                value = denomination,
                onValueChange = { inputText ->
                    val maxBtcValue = 21_000_000.00000000
                    var input = inputText.text.trim()
                    var selectionPosition = inputText.selection.start

                    if (input.startsWith(".")) {
                        input = "0$input"
                        selectionPosition += 1
                    }

                    val regex = Regex("^\\d{0,8}(\\.\\d{0,8})?\$")

                    if (regex.matches(input)) {
                        val value = input.toDoubleOrNull()

                        if (value != null && value <= maxBtcValue) {
                            denomination = TextFieldValue(text = input, selection = TextRange(selectionPosition))
                        } else {
                            if (input.isNotEmpty()) {
                                SnackbarController.showMessage(message = denominationExceeded)
                            }
                            denomination = TextFieldValue("")
                        }
                    } else {
                        SnackbarController.showMessage(message = invalidDenomination)
                        denomination = TextFieldValue("")
                    }
                },
                label = { Text(text = stringResource(Res.string.denomination)) },
                supportingText = {
                    Text(
                        text = stringResource(Res.string.denomination_support_txt),
                        color = Color.Gray
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusRequester.requestFocus() }
                ),
                modifier = Modifier
                    .wrapContentSize()
                    .focusRequester(focusRequester),
                trailingIcon = {
                    if (denomination.text.isNotEmpty()) {
                        IconButton(onClick = { denomination = TextFieldValue("") }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear text"
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = peers,
                onValueChange = {
                    val input = it.trim()
                    if (input.all { char -> char.isDigit() }) {
                        peers = input
                    } else {
                        runCatching {
                            SnackbarController.showMessage("Invalid input: '$input' is not a valid peer.")
                            peers = ""
                        }.onFailure { exception ->
                            SnackbarController.showMessage("Error occurred while processing input '$input': ${exception.message}")
                            exception.printStackTrace()
                            peers = ""
                        }
                    }
                },
                label = { Text("Number of peers") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                modifier = Modifier
                    .wrapContentSize()
                    .focusRequester(focusRequester),
                trailingIcon = {
                    if (peers.isNotEmpty()) {
                        IconButton(onClick = { peers = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear text"
                            )
                        }
                    }
                }
            )
        }

        Button(
            modifier = Modifier
                .padding(all = 12.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(8.dp),
            enabled = denomination.text.isNotEmpty() && peers.isNotEmpty(),
            onClick = {
                poolsViewModel.createPool(denomination.text, peers) {
                    denomination = TextFieldValue("")
                    peers = ""
                    showWaitingDialog.value = true
                    focusManager.clearFocus()

                    CoroutineScope(Dispatchers.Default).launch {
                        delay(10.minutes.inWholeMilliseconds)
                        if (showWaitingDialog.value) {
                            showWaitingDialog.value = false
                        }
                    }
                }
            }
        ) {
            Text(
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.Bottom),
                text = "Create",
                fontSize = 16.sp
            )
        }
    }
}