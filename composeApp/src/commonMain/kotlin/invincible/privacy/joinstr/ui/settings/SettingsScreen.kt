package invincible.privacy.joinstr.ui.settings

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import invincible.privacy.joinstr.theme.Theme
import invincible.privacy.joinstr.ui.components.SnackbarController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel{ SettingsViewModel() },
    onBackPress: () -> Unit
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
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (uiState.selectedTheme == Theme.DARK.id || (uiState.selectedTheme == Theme.SYSTEM.id && isSystemInDarkTheme())) {
                        MaterialTheme.colorScheme.surface.copy(alpha = if (hasScrolled) 1f else 0f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ),
                modifier = Modifier.shadow(appBarElevation),
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
            }

            // Save button
            Button(
                onClick = { viewModel.saveSettings() },
                enabled = saveOperation !is SaveOperation.InProgress,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            ) {
                Text(if (saveOperation is SaveOperation.InProgress) "Saving..." else "Save")
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
            index= index,
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
    onPortChange: (String) -> Unit
) {
    OutlinedTextField(
        value = uiState.nostrRelay,
        onValueChange = onNostrRelayChange,
        label = { Text("Nostr Relay") },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            if (uiState.nostrRelay.isNotEmpty()) {
                IconButton(onClick = { onNostrRelayChange.invoke("") }) {
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
        value = uiState.nodeUrl,
        onValueChange = onNodeUrlChange,
        label = { Text("Node URL") },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            if (uiState.nodeUrl.isNotEmpty()) {
                IconButton(onClick = { onNodeUrlChange.invoke("") }) {
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
        value = uiState.username,
        onValueChange = onUsernameChange,
        label = { Text("RPC Username") },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            if (uiState.username.isNotEmpty()) {
                IconButton(onClick = { onUsernameChange.invoke("") }) {
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
        value = uiState.password,
        onValueChange = onPasswordChange,
        label = { Text("RPC Password") },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            if (uiState.password.isNotEmpty()) {
                IconButton(onClick = { onPasswordChange.invoke("") }) {
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
        value = uiState.port,
        onValueChange = onPortChange,
        label = { Text("RPC Port") },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            if (uiState.port.isNotEmpty()) {
                IconButton(onClick = { onPortChange.invoke("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear text"
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
    onOptionSelected: (Int) -> Unit
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