package invincible.privacy.joinstr.ui

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import invincible.privacy.joinstr.theme.NodeConfig
import invincible.privacy.joinstr.theme.Settings
import invincible.privacy.joinstr.theme.SettingsManager
import invincible.privacy.joinstr.theme.SettingsManager.store
import invincible.privacy.joinstr.theme.Theme
import invincible.privacy.joinstr.ui.components.ProgressDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SettingsScreen(
    onBackPress: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var nostrRelay by remember { mutableStateOf("") }
    var nodeUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberScrollState()
    val hasScrolled by remember {
        derivedStateOf {
            listState.value > 0
        }
    }

    val appBarElevation by animateDpAsState(targetValue = if (hasScrolled) 4.dp else 0.dp)
    val (nostrRelayFocusRequester, nodeUrlFocusRequester,
        usernameFocusRequester, passwordFocusRequester,
        portFocusRequester
    ) = FocusRequester.createRefs()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val settings by store.updates.collectAsState(
        initial = Settings(
            selectedTheme = Theme.SYSTEM.id,
            nodeConfig = NodeConfig()
        )
    )

    val themesList = Theme.entries.map { theme ->
        ThemeData(
            title = theme.title,
            description = theme.description
        )
    }

    LaunchedEffect(settings) {
        coroutineScope.launch {
            isLoading = true
            nostrRelay = settings?.nostrRelay ?: Settings().nostrRelay
            nodeUrl = settings?.nodeConfig?.url ?: Settings().nodeConfig.url
            username = settings?.nodeConfig?.userName ?: Settings().nodeConfig.userName
            password = settings?.nodeConfig?.password ?: Settings().nodeConfig.password
            port = settings?.nodeConfig?.port?.toString() ?: Settings().nodeConfig.port.toString()
            isLoading = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isSystemInDarkTheme()) {
                        MaterialTheme.colorScheme.surface.copy(alpha = if (hasScrolled) 1f else 0f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ),
                modifier = Modifier.shadow(appBarElevation),
                title = {
                    Text("Settings")
                },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Localized description"
                        )
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
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(listState)
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                    }
                }
        ) {
            if (isLoading) {
                ProgressDialog()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Theme",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    themesList.forEachIndexed { index, themeData ->
                        ThemeOption(
                            title = themeData.title,
                            description = themeData.description,
                            index = index,
                            isSelected = (settings?.selectedTheme ?: Theme.SYSTEM.id) == index,
                            onOptionSelected = {
                                coroutineScope.launch {
                                    SettingsManager.updateTheme(index)
                                }
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Configuration",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = nostrRelay,
                        onValueChange = { nostrRelay = it },
                        label = { Text("Nostr Relay") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nostrRelayFocusRequester),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { nodeUrlFocusRequester.requestFocus() }
                        ),
                        trailingIcon = {
                            if (nostrRelay.isNotEmpty()) {
                                IconButton(onClick = { nostrRelay = "" }) {
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
                        value = nodeUrl,
                        onValueChange = { nodeUrl = it },
                        label = { Text("Node URL") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nodeUrlFocusRequester),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { usernameFocusRequester.requestFocus() }
                        ),
                        trailingIcon = {
                            if (nodeUrl.isNotEmpty()) {
                                IconButton(onClick = { nodeUrl = "" }) {
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
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("RPC Username") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(usernameFocusRequester),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { passwordFocusRequester.requestFocus() }
                        ),
                        trailingIcon = {
                            if (username.isNotEmpty()) {
                                IconButton(onClick = { username = "" }) {
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
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("RPC Password") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { portFocusRequester.requestFocus() }
                        ),
                        trailingIcon = {
                            if (password.isNotEmpty()) {
                                IconButton(onClick = { password = "" }) {
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
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("RPC Port") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { keyboardController?.hide() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(portFocusRequester),
                        trailingIcon = {
                            if (port.isNotEmpty()) {
                                IconButton(onClick = { port = "" }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear text"
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        enabled = nodeUrl.isNotEmpty() && username.isNotEmpty()
                            && password.isNotEmpty() && port.isNotEmpty(),
                        shape = RoundedCornerShape(8.dp),
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                delay(500)
                                val nodeConfig = NodeConfig(
                                    url = nodeUrl,
                                    userName = username,
                                    password = password,
                                    port = port.toInt()
                                )
                                SettingsManager.updateNodeConfig(nodeConfig, nostrRelay)
                                isLoading = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
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

data class ThemeData(
    val title: String,
    val description: String? = null
)