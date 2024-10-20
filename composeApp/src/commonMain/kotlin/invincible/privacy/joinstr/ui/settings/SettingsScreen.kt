package invincible.privacy.joinstr.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType.Companion.PrimaryEditable
import androidx.compose.material3.MenuAnchorType.Companion.PrimaryNotEditable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
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
            val isEnabled = saveOperation !is SaveOperation.InProgress &&
                uiState.isNostrRelayValid && uiState.isNodeUrlValid &&
                uiState.isUsernameValid && uiState.isPasswordValid && uiState.isPortValid

            val containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                Color.Gray.copy(alpha = 0.5f)
            }

            val contentColor = if (isEnabled) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                Color.Gray
            }
            ExtendedFloatingActionButton(
                text = {  Text(if (saveOperation is SaveOperation.InProgress) "Saving..." else "Save") },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = "save-icon"
                    )
                },
                expanded = listState.isScrollingUp(),
                containerColor = containerColor,
                contentColor = contentColor,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                onClick = {
                    if (isEnabled) {
                        viewModel.saveSettings()
                    }
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
                ConfigurationFields(
                    uiState = uiState,
                    onNostrRelayChange = viewModel::updateNostrRelay,
                    onNodeUrlChange = viewModel::updateNodeUrl,
                    onUsernameChange = viewModel::updateUsername,
                    onPasswordChange = viewModel::updatePassword,
                    onPortChange = viewModel::updatePort
                )

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
fun ConfigurationFields(
    uiState: SettingsUiState,
    onNostrRelayChange: (String) -> Unit,
    onNodeUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
) {
    ValidatedTextField(
        value = uiState.nostrRelay,
        onValueChange = onNostrRelayChange,
        label = "Nostr Relay",
        isValid = uiState.isNostrRelayValid,
        errorMessage = "Invalid WebSocket URL"
    )
    Spacer(modifier = Modifier.height(8.dp))
    ValidatedTextField(
        value = uiState.nodeUrl,
        onValueChange = onNodeUrlChange,
        label = "Node URL",
        isValid = uiState.isNodeUrlValid,
        errorMessage = "Invalid HTTP URL"
    )
    Spacer(modifier = Modifier.height(8.dp))
    ValidatedTextField(
        value = uiState.username,
        onValueChange = onUsernameChange,
        label = "RPC Username",
        isValid = uiState.isUsernameValid,
        errorMessage = "Username cannot be empty"
    )
    Spacer(modifier = Modifier.height(8.dp))
    ValidatedTextField(
        value = uiState.password,
        onValueChange = onPasswordChange,
        label = "RPC Password",
        isValid = uiState.isPasswordValid,
        errorMessage = "Password cannot be empty",
        visualTransformation = PasswordVisualTransformation()
    )
    Spacer(modifier = Modifier.height(8.dp))
    ValidatedTextField(
        value = uiState.port,
        onValueChange = onPortChange,
        label = "RPC Port",
        isValid = uiState.isPortValid,
        errorMessage = "Invalid port number"
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isValid: Boolean,
    errorMessage: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val isPassword = visualTransformation == PasswordVisualTransformation()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        isError = !isValid,
        maxLines = 1,
        singleLine = true,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (isPassword) {
            KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
        } else KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
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
            if (!isValid) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
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