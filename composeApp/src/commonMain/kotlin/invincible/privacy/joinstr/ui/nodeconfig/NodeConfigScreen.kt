package invincible.privacy.joinstr.ui.nodeconfig

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeConfigScreen(
    viewModel: NodeConfigViewModel,
    onBackPress: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Handle errors via snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            val message = when (error) {
                is ErrorType.ValidationError -> error.message
                is ErrorType.AuthError -> "Authentication failed. Please check your username and password."
                is ErrorType.NetworkError -> "Network error. Please check your connection and try again."
                is ErrorType.UnknownError -> error.message
            }
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }
    
    // Handle success state
    LaunchedEffect(uiState.status) {
        if (uiState.status is Status.Saved) {
            scope.launch {
                snackbarHostState.showSnackbar("Configuration saved successfully")
            }
            // Navigate back after a short delay
            kotlinx.coroutines.delay(500)
            onBackPress()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Node Configuration") },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(
                    top = 0.dp,
                    bottom = 0.dp
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Relay URL Field
                    RelayUrlField(
                        value = uiState.relayUrl,
                        onValueChange = { viewModel.handleIntent(NodeConfigIntent.RelayChanged(it)) },
                        isValid = uiState.error !is ErrorType.ValidationError || 
                                  (uiState.error as? ErrorType.ValidationError)?.field != "relayUrl",
                        errorMessage = if (uiState.error is ErrorType.ValidationError && 
                                          (uiState.error as ErrorType.ValidationError).field == "relayUrl") {
                            (uiState.error as ErrorType.ValidationError).message
                        } else null,
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    )
                }
                
                item {
                    // Node URL Field
                    NodeUrlField(
                        value = uiState.nodeUrl,
                        onValueChange = { viewModel.handleIntent(NodeConfigIntent.NodeUrlChanged(it)) },
                        isValid = uiState.error !is ErrorType.ValidationError || 
                                 (uiState.error as? ErrorType.ValidationError)?.field != "nodeUrl",
                        errorMessage = if (uiState.error is ErrorType.ValidationError && 
                                          (uiState.error as ErrorType.ValidationError).field == "nodeUrl") {
                            (uiState.error as ErrorType.ValidationError).message
                        } else null,
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    )
                }
                
                item {
                    // Node Port Field
                    NodePortField(
                        value = uiState.port,
                        onValueChange = { viewModel.handleIntent(NodeConfigIntent.PortChanged(it)) },
                        isValid = uiState.error !is ErrorType.ValidationError || 
                                 (uiState.error as? ErrorType.ValidationError)?.field != "port",
                        errorMessage = if (uiState.error is ErrorType.ValidationError && 
                                          (uiState.error as ErrorType.ValidationError).field == "port") {
                            (uiState.error as ErrorType.ValidationError).message
                        } else null,
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    )
                }
                
                item {
                    // Username Field
                    UsernameField(
                        value = uiState.username,
                        onValueChange = { viewModel.handleIntent(NodeConfigIntent.UsernameChanged(it)) },
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    )
                }
                
                item {
                    // Password Field
                    PasswordField(
                        value = uiState.password,
                        onValueChange = { viewModel.handleIntent(NodeConfigIntent.PasswordChanged(it)) },
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                item {
                    // Bottom Action Bar
                    BottomActionBar(
                        canTest = uiState.canTest,
                        canSave = uiState.canSave,
                        isLoading = uiState.status is Status.Testing || uiState.status is Status.Saving,
                        onTestClick = { viewModel.handleIntent(NodeConfigIntent.TestConnection) },
                        onSaveClick = { viewModel.handleIntent(NodeConfigIntent.Save) }
                    )
                }
            }
            
            // Loading Overlay
            if (uiState.status is Status.Testing || uiState.status is Status.Saving) {
                LoadingOverlay(
                    message = if (uiState.status is Status.Testing) "Testing connection..." else "Saving..."
                )
            }
        }
    }
}

@Composable
private fun RelayUrlField(
    value: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
    errorMessage: String?,
    onNext: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Relay URL") },
        placeholder = { Text("wss://nostr.fmt.wiz.biz") },
        leadingIcon = {
            Icon(Icons.Filled.Wifi, contentDescription = null)
        },
        isError = !isValid && errorMessage != null,
        supportingText = {
            if (errorMessage != null && !isValid) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            } else {
                Text("WebSocket URL for Nostr relay connection (optional)")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext() }
        )
    )
}

@Composable
private fun NodeUrlField(
    value: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
    errorMessage: String?,
    onNext: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Node URL") },
        placeholder = { Text("http://localhost or 192.168.1.1") },
        leadingIcon = {
            Icon(Icons.Filled.Storage, contentDescription = null)
        },
        isError = !isValid && errorMessage != null,
        supportingText = {
            if (errorMessage != null && !isValid) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            } else {
                Text("HTTP URL or IP address of your node")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext() }
        )
    )
}

@Composable
private fun NodePortField(
    value: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
    errorMessage: String?,
    onNext: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Only allow numeric input
            if (newValue.all { it.isDigit() }) {  // Changed from newValue.isDigit() to it.isDigit()
                onValueChange(newValue)
            }
        },
        label = { Text("Node Port") },
        placeholder = { Text("8332") },
        isError = !isValid && errorMessage != null,
        supportingText = {
            if (errorMessage != null && !isValid) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            } else {
                Text("RPC port (1-65535)")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(  // Combined into keyboardOptions
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext() }
        )
    )
}

@Composable
private fun UsernameField(
    value: String,
    onValueChange: (String) -> Unit,
    onNext: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Username") },
        placeholder = { Text("RPC username") },
        leadingIcon = {
            Icon(Icons.Filled.Person, contentDescription = null)
        },
        supportingText = {
            Text("RPC authentication username (optional)")
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext() }
        )
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Password") },
        placeholder = { Text("RPC password") },
        leadingIcon = {
            Icon(Icons.Filled.Lock, contentDescription = null)
        },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
            }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        supportingText = {
            Text("RPC authentication password (optional)")
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(  // Combined into keyboardOptions
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDone() }
        )
    )
}

@Composable
private fun BottomActionBar(
    canTest: Boolean,
    canSave: Boolean,
    isLoading: Boolean,
    onTestClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Test Connection Button
            Button(
                onClick = onTestClick,
                enabled = canTest && !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Test Connection")
            }
            
            // Save Button
            Button(
                onClick = onSaveClick,
                enabled = canSave && !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun LoadingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
