package invincible.privacy.joinstr.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType.Companion.PrimaryEditable
import androidx.compose.material3.ExposedDropdownMenuAnchorType.Companion.PrimaryNotEditable
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import invincible.privacy.joinstr.Platform
import invincible.privacy.joinstr.getPlatform
import invincible.privacy.joinstr.model.VpnGateway
import invincible.privacy.joinstr.ui.components.SnackbarController
import invincible.privacy.joinstr.utils.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel { SettingsViewModel() },
    onNavigateToNodeConfig: () -> Unit = {},
    onBackPress: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveOperation by viewModel.saveOperation.collectAsState()
    val listState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val hasScrolled by remember {
        derivedStateOf {
            listState.value > 0
        }
    }

    val appBarElevation by animateDpAsState(targetValue = if (hasScrolled) 4.dp else 0.dp)

    LaunchedEffect(saveOperation) {
        when (saveOperation) {
            is SaveOperation.Success -> {
                SnackbarController.showMessage("Settings saved successfully")
            }

            is SaveOperation.Error -> {
                SnackbarController.showMessage("Error: ${(saveOperation as SaveOperation.Error).message}")
            }

            else -> { /*no op*/ }
        }
    }

    val isDarkTheme = (uiState.selectedTheme == Theme.DARK.id || (uiState.selectedTheme == Theme.SYSTEM.id && isSystemInDarkTheme()))

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkTheme) {
                        MaterialTheme.colorScheme.surface.copy(alpha = if (hasScrolled) 1f else 0f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.shadow(
                    elevation = appBarElevation,
                    spotColor = if (isDarkTheme) Color.White else Color.Black
                ),
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(
                    top = 0.dp,
                    bottom = 0.dp
                )
            )
        },
        floatingActionButton = {
            val allFieldsValid = uiState.isNostrRelayValid && uiState.isNodeUrlValid &&
                uiState.isUsernameValid && uiState.isPasswordValid && uiState.isPortValid
            val hasRequiredFields = uiState.username.isNotBlank() && 
                uiState.password.isNotBlank() && 
                uiState.nodeUrl.isNotBlank() && 
                uiState.port.isNotBlank()
            ExtendedFloatingActionButton(
                text = {  
                    Text(
                        when {
                            saveOperation is SaveOperation.InProgress -> "Saving..."
                            !hasRequiredFields -> "Fill Required Fields"
                            !allFieldsValid -> "Fix Errors"
                            else -> "Save"
                        }
                    ) 
                },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = "save-icon"
                    )
                },
                expanded = listState.isScrollingUp(),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                onClick = {
                    viewModel.saveSettings()
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .verticalScroll(listState)
                .padding(top = innerPadding.calculateTopPadding())
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                    }
                }
        ) {

            // Theme section
            SettingsSection(title = "Theme") {
                ThemeOptions(
                    selectedTheme = uiState.selectedTheme,
                    onThemeSelected = viewModel::updateTheme
                )
            }

            // Configuration section
            SettingsSection(title = "Configuration") {
                // Node Configuration Card - Clickable to navigate
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToNodeConfig() },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Node Configuration",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap to configure node settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit node configuration",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                WalletDropdown(
                    selectedWallet = uiState.selectedWallet,
                    viewModel = viewModel
                )

                if (getPlatform() == Platform.ANDROID) {
                    Spacer(modifier = Modifier.height(16.dp))

                    VpnGatewayDropDown(
                        selectedGateway = uiState.selectedVpnGateway,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnGatewayDropDown(
    selectedGateway: VpnGateway?,
    viewModel: SettingsViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    val vpnGateways by viewModel.vpnGatewayList.collectAsState()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedGateway?.let { "(${it.location}) ${it.host}" } ?: "",
            onValueChange = { },
            label = { Text("VPN Gateway") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            vpnGateways.forEach { vpnGateway ->
                DropdownMenuItem(
                    text = {
                        val displayText = "(${vpnGateway.location}) ${vpnGateway.host}"
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    },
                    onClick = {
                        viewModel.updateSelectedVpnGateway(vpnGateway)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDropdown(
    selectedWallet: String,
    viewModel: SettingsViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    val wallets by viewModel.walletList.collectAsState()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedWallet,
            onValueChange = { },
            label = { Text("Wallet") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(PrimaryEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            wallets.forEach { wallet ->
                DropdownMenuItem(
                    text = { Text(wallet) },
                    onClick = {
                        viewModel.updateSelectedWallet(wallet)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun ThemeOptions(selectedTheme: Int, onThemeSelected: (Int) -> Unit) {
    Theme.entries.forEachIndexed { index, theme ->
        ThemeOption(
            title = theme.title,
            description = theme.description,
            index = index,
            isSelected = selectedTheme == index,
            onOptionSelected = { onThemeSelected(index) }
        )
    }
}

@Composable
fun NodeConfigurationWizard(
    uiState: SettingsUiState,
    currentStep: Int,
    onStepChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onNostrRelayChange: (String) -> Unit,
    onNodeUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onSave: () -> Unit,
    saveOperation: SaveOperation
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val totalSteps = 5
    
    val stepTitles = listOf(
        "Nostr Relay",
        "Node URL",
        "Port",
        "Username",
        "Password"
    )
    
    val stepDescriptions = listOf(
        "WebSocket URL for Nostr relay connection",
        "HTTP URL of your node",
        "RPC port number",
        "RPC authentication username",
        "RPC authentication password"
    )
    
    LaunchedEffect(currentStep) {
        focusRequester.requestFocus()
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Dialog Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Node Configuration",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Step ${currentStep + 1} of $totalSteps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close dialog"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step Title
            Text(
                text = stepTitles[currentStep],
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Step Description
            Text(
                text = stepDescriptions[currentStep],
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Current Step Field
            when (currentStep) {
                0 -> {
                    ValidatedTextField(
                        value = uiState.nostrRelay,
                        onValueChange = onNostrRelayChange,
                        label = "Nostr Relay",
                        placeholder = "Enter WebSocket URL (ws:// or wss://)",
                        helperText = "",
                        leadingIcon = Icons.Filled.Wifi,
                        isValid = uiState.isNostrRelayValid,
                        errorMessage = "Invalid WebSocket URL (must start with ws:// or wss://)",
                        onNext = { if (uiState.isNostrRelayValid) onStepChange(1) },
                        focusRequester = focusRequester
                    )
                }
                1 -> {
                    ValidatedTextField(
                        value = uiState.nodeUrl,
                        onValueChange = onNodeUrlChange,
                        label = "Node URL",
                        placeholder = "Enter HTTP URL of your node (e.g., http://localhost)",
                        helperText = "",
                        leadingIcon = Icons.Filled.Storage,
                        isValid = uiState.isNodeUrlValid,
                        errorMessage = if (uiState.nodeUrl.isBlank()) "Node URL is required" else "Invalid HTTP URL",
                        onNext = { if (uiState.isNodeUrlValid) onStepChange(2) },
                        focusRequester = focusRequester
                    )
                }
                2 -> {
                    ValidatedTextField(
                        value = uiState.port,
                        onValueChange = onPortChange,
                        label = "Port",
                        placeholder = "Enter RPC port (1-65535)",
                        helperText = "",
                        isValid = uiState.isPortValid,
                        errorMessage = if (uiState.port.isBlank() && uiState.nodeUrl.isNotBlank()) "Port is required" else "Invalid port (1-65535)",
                        keyboardType = KeyboardType.Number,
                        onNext = { if (uiState.isPortValid) onStepChange(3) },
                        focusRequester = focusRequester
                    )
                }
                3 -> {
                    ValidatedTextField(
                        value = uiState.username,
                        onValueChange = onUsernameChange,
                        label = "RPC Username",
                        placeholder = "Enter RPC authentication username",
                        helperText = "",
                        leadingIcon = Icons.Filled.Person,
                        isValid = uiState.isUsernameValid,
                        errorMessage = if (uiState.username.isBlank()) "Username is required" else "Username cannot be empty",
                        onNext = { if (uiState.isUsernameValid) onStepChange(4) },
                        focusRequester = focusRequester
                    )
                }
                4 -> {
                    ValidatedTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        label = "RPC Password",
                        placeholder = "Enter RPC authentication password",
                        helperText = "",
                        leadingIcon = Icons.Filled.Lock,
                        isValid = uiState.isPasswordValid,
                        errorMessage = if (uiState.password.isBlank()) "Password is required" else "Password cannot be empty",
                        visualTransformation = PasswordVisualTransformation(),
                        onNext = { 
                            keyboardController?.hide()
                        },
                        focusRequester = focusRequester
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { 
                        if (currentStep > 0) {
                            onStepChange(currentStep - 1)
                        } else {
                            onDismiss()
                        }
                    },
                    enabled = currentStep > 0 || saveOperation !is SaveOperation.InProgress
                ) {
                    if (currentStep > 0) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Previous")
                    } else {
                        Text("Cancel")
                    }
                }
                
                if (currentStep < totalSteps - 1) {
                    Button(
                        onClick = { onStepChange(currentStep + 1) },
                        enabled = when (currentStep) {
                            0 -> uiState.isNostrRelayValid
                            1 -> uiState.isNodeUrlValid
                            2 -> uiState.isPortValid
                            3 -> uiState.isUsernameValid
                            else -> true
                        }
                    ) {
                        Text("Next")
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                } else {
                    val allFieldsValid = uiState.isNostrRelayValid && uiState.isNodeUrlValid &&
                        uiState.isUsernameValid && uiState.isPasswordValid && uiState.isPortValid
                    val hasRequiredFields = uiState.username.isNotBlank() && 
                        uiState.password.isNotBlank() && 
                        uiState.nodeUrl.isNotBlank() && 
                        uiState.port.isNotBlank()
                    val isEnabled = saveOperation !is SaveOperation.InProgress && 
                        allFieldsValid && hasRequiredFields
                    
                    Button(
                        onClick = onSave,
                        enabled = isEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            when {
                                saveOperation is SaveOperation.InProgress -> "Saving..."
                                !hasRequiredFields -> "Fill Required"
                                !allFieldsValid -> "Fix Errors"
                                else -> "Save"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigurationFields(
    uiState: SettingsUiState,
    onNostrRelayChange: (String) -> Unit,
    onNodeUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
) {
    // Nostr Relay
    ValidatedTextField(
        value = uiState.nostrRelay,
        onValueChange = onNostrRelayChange,
        label = "Nostr Relay",
        placeholder = "wss://nostr.fmt.wiz.biz",
        helperText = "WebSocket URL for Nostr relay connection",
        leadingIcon = Icons.Filled.Wifi,
        isValid = uiState.isNostrRelayValid,
        errorMessage = "Invalid WebSocket URL (must start with ws:// or wss://)"
    )
    Spacer(modifier = Modifier.height(12.dp))
    
    // Node URL and Port grouped together
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ValidatedTextField(
            value = uiState.nodeUrl,
            onValueChange = onNodeUrlChange,
            label = "Node URL",
            placeholder = "http://localhost",
            helperText = "HTTP URL of your node",
            leadingIcon = Icons.Filled.Storage,
            isValid = uiState.isNodeUrlValid,
            errorMessage = if (uiState.nodeUrl.isBlank()) "Node URL is required" else "Invalid HTTP URL",
            modifier = Modifier.weight(2f)
        )
        ValidatedTextField(
            value = uiState.port,
            onValueChange = onPortChange,
            label = "Port",
            placeholder = "8332",
            helperText = "RPC port",
            isValid = uiState.isPortValid,
            errorMessage = if (uiState.port.isBlank() && uiState.nodeUrl.isNotBlank()) "Port is required" else "Invalid port (1-65535)",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    
    // Username and Password grouped together
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ValidatedTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            label = "RPC Username",
            placeholder = "rpcuser",
            helperText = "RPC authentication username",
            leadingIcon = Icons.Filled.Person,
            isValid = uiState.isUsernameValid,
            errorMessage = if (uiState.username.isBlank()) "Username is required" else "Username cannot be empty",
            modifier = Modifier.weight(1f)
        )
        ValidatedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = "RPC Password",
            placeholder = "••••••••",
            helperText = "RPC authentication password",
            leadingIcon = Icons.Filled.Lock,
            isValid = uiState.isPasswordValid,
            errorMessage = if (uiState.password.isBlank()) "Password is required" else "Password cannot be empty",
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    helperText: String = "",
    leadingIcon: ImageVector? = null,
    isValid: Boolean,
    errorMessage: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    onNext: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val isPassword = visualTransformation == PasswordVisualTransformation()
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder) } } else null,
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        isError = !isValid,
        maxLines = 1,
        singleLine = true,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = when {
            isPassword -> KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (onNext != null) ImeAction.Next else ImeAction.Done
            )
            keyboardType == KeyboardType.Number -> KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = if (onNext != null) ImeAction.Next else ImeAction.Done
            )
            else -> KeyboardOptions.Default.copy(
                imeAction = if (onNext != null) ImeAction.Next else ImeAction.Done
            )
        },
        keyboardActions = KeyboardActions(
            onNext = {
                onNext?.invoke()
            },
            onDone = {
                keyboardController?.hide()
                onNext?.invoke()
            }
        ),
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                Row {
                    if (isPassword) {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    }

                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear text"
                        )
                    }
                }
            }
        },
        supportingText = {
            Column {
                if (!isValid) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (helperText.isNotEmpty()) {
                    // Show helper text when valid
                    Text(
                        text = helperText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    )
}

@Composable
fun ThemeOption(
    title: String,
    index: Int,
    description: String? = null,
    isSelected: Boolean,
    onOptionSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOptionSelected(index) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = isSelected,
                onClick = { onOptionSelected(index) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                description?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .height(1.dp)
                .padding(top = 10.dp),
            color = Color.LightGray
        )
    }
}

@Composable
fun ScrollState.isScrollingUp(): Boolean {
    var previousScrollOffset by remember { mutableStateOf(0) }
    var previousScrollDirection by remember { mutableStateOf(true) }

    return remember {
        derivedStateOf {
            val currentScrollOffset = value

            val scrollingUp = when {
                currentScrollOffset < previousScrollOffset -> true
                currentScrollOffset > previousScrollOffset -> false
                else -> previousScrollDirection
            }

            previousScrollOffset = currentScrollOffset
            previousScrollDirection = scrollingUp

            scrollingUp
        }
    }.value
}