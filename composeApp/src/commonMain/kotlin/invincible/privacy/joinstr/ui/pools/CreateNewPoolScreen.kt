package invincible.privacy.joinstr.ui.pools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import invincible.privacy.joinstr.calculatePoolAmount
import invincible.privacy.joinstr.convertFloatExponentialToString
import invincible.privacy.joinstr.ui.components.SnackbarController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNewPoolScreen(
    poolsViewModel: PoolsViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val activePoolReady by poolsViewModel.activePoolReady.collectAsState()
    val listUnspentAmounts by poolsViewModel.listUnspentAmount.collectAsState()
    val showWaitingDialog = remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                poolsViewModel.fetchUnspentAmounts()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
                            text = "Waiting for other users to register outputs...",
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

        var selectedOption = remember { mutableStateOf<Float?>(null) }
        val denomination = remember { mutableStateOf("") }
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

            Text(
                text = "Pool Details",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            SelectInputDropdown(
                denomination = denomination,
                options = listUnspentAmounts ?: emptyList(),
                selectedOption = selectedOption,
                onOptionSelected = { selectedOption.value = it }
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
            enabled = denomination.value.isNotEmpty() && peers.isNotEmpty(),
            onClick = {
                poolsViewModel.createPool(denomination.value, peers) {
                    denomination.value = ""
                    peers = ""
                    showWaitingDialog.value = true
                    focusManager.clearFocus()
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

@Composable
private fun SelectInputDropdown(
    denomination: MutableState<String>,
    options: List<Float>,
    selectedOption: MutableState<Float?>,
    onOptionSelected: (Float?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val allOptions = listOf(null) + options

    Box(
        modifier = Modifier.wrapContentWidth()
    ) {
        OutlinedTextField(
            modifier = Modifier
                .padding(16.dp)
                .clickable(onClick = { expanded = true }),
            value = selectedOption.value?.convertFloatExponentialToString() ?: "Unselect",
            onValueChange = { /* no-op */ },
            textStyle = MaterialTheme.typography.bodyMedium,
            enabled = false,
            label = {
                Text(text = "Select input")
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onBackground,
                disabledLabelColor = MaterialTheme.colorScheme.onBackground,
                disabledBorderColor = MaterialTheme.colorScheme.onBackground
            ),
            trailingIcon = {
                if (denomination.value.isNotEmpty()) {
                    IconButton(onClick = {
                        denomination.value = ""
                        selectedOption.value = null
                    }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear text",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            },
            supportingText = {
                if (selectedOption.value != null) {
                    Text(text = "Pool amount ${denomination.value}")
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .wrapContentWidth()
                .heightIn(max = 300.dp)  // Limit the maximum height
        ) {
            allOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option?.let { "${it.convertFloatExponentialToString()} BTC" } ?: "Select input",
                            maxLines = 1,  // Ensure each item is single-line
                            overflow = TextOverflow.Ellipsis  // Add ellipsis for long text
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        val poolAmount = calculatePoolAmount(option?.toDouble() ?: 0.0)
                        denomination.value = poolAmount.toFloat().convertFloatExponentialToString()
                        expanded = false
                    },
                    modifier = Modifier.height(48.dp)  // Set a fixed height for each item
                )
            }
        }
    }
}