package invincible.privacy.joinstr.ui.pools

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import invincible.privacy.joinstr.ui.components.SnackbarController

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CreateNewPoolScreen(
    poolsViewModel: PoolsViewModel
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .shadow(4.dp, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.TopCenter
    ) {
        val (focusRequester) = FocusRequester.createRefs()
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        var denomination by remember { mutableStateOf(TextFieldValue("")) }
        var peers by remember { mutableStateOf("") }

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

            OutlinedTextField(
                value = denomination,
                onValueChange = { inputText ->
                    val maxBtcValue = 21_000_000.00000000
                    var input = inputText.text.trim()
                    var selectionPosition = inputText.selection.start

                    // Automatically add '0' if the input starts with '.'
                    if (input.startsWith(".")) {
                        input = "0$input"
                        selectionPosition += 1 // Move cursor after the dot
                    }

                    val regex = Regex("^\\d{0,8}(\\.\\d{0,8})?\$")

                    if (regex.matches(input)) {
                        val value = input.toDoubleOrNull()

                        if (value != null && value <= maxBtcValue) {
                            denomination = TextFieldValue(text = input, selection = TextRange(selectionPosition))
                        } else {
                            if (input.isNotEmpty()) {
                                SnackbarController.showMessage(
                                    message = "Input exceeds the maximum BTC value of 21 million or is invalid."
                                )
                            }
                            denomination = TextFieldValue("")
                        }
                    } else {
                        SnackbarController.showMessage(
                            message = "Input is not a valid BTC amount. Ensure it is up to 8 decimal places and contains only numbers."
                        )
                        denomination = TextFieldValue("")
                    }
                },
                label = { Text("Denomination") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusRequester.requestFocus()}
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
                    peers = it
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