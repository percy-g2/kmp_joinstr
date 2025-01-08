package invincible.privacy.joinstr

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.serialization.generateHashCode
import androidx.navigation.toRoute
import invincible.privacy.joinstr.model.CoinJoinHistory
import invincible.privacy.joinstr.model.Home
import invincible.privacy.joinstr.model.InputRegistration
import invincible.privacy.joinstr.model.ListUnspentResponseItem
import invincible.privacy.joinstr.model.LocalPoolContent
import invincible.privacy.joinstr.model.NavItem
import invincible.privacy.joinstr.model.Pools
import invincible.privacy.joinstr.model.Settings
import invincible.privacy.joinstr.theme.DarkColorScheme
import invincible.privacy.joinstr.theme.JoinstrTheme
import invincible.privacy.joinstr.theme.LightColorScheme
import invincible.privacy.joinstr.tor.LOG_HOLDER_NAME
import invincible.privacy.joinstr.tor.LogItem
import invincible.privacy.joinstr.tor.Tor
import invincible.privacy.joinstr.ui.PoolsViewModel
import invincible.privacy.joinstr.ui.components.CustomStackedSnackbar
import invincible.privacy.joinstr.ui.components.SnackbarControllerProvider
import invincible.privacy.joinstr.ui.home.HomeScreen
import invincible.privacy.joinstr.ui.pools.PoolScreen
import invincible.privacy.joinstr.ui.registerInput.RegisterInputScreen
import invincible.privacy.joinstr.ui.settings.SettingsScreen
import invincible.privacy.joinstr.utils.SettingsManager
import invincible.privacy.joinstr.utils.SettingsStore
import invincible.privacy.joinstr.utils.Theme
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.github.xxfast.kstore.KStore
import io.ktor.client.*
import io.matthewnelson.kmp.tor.runtime.Action
import io.matthewnelson.kmp.tor.runtime.Action.Companion.executeSync
import io.matthewnelson.kmp.tor.runtime.RuntimeEvent
import io.matthewnelson.kmp.tor.runtime.TorRuntime
import io.matthewnelson.kmp.tor.runtime.core.OnFailure
import io.matthewnelson.kmp.tor.runtime.core.OnSuccess
import io.matthewnelson.kmp.tor.runtime.core.ctrl.TorCmd
import io.matthewnelson.kmp.tor.runtime.core.util.executeAsync
import io.matthewnelson.kmp.tor.runtime.core.util.executeSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(
    poolsViewModel: PoolsViewModel = viewModel { PoolsViewModel() },
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val themeState by SettingsManager.themeState.collectAsState()
    val activePoolReady by poolsViewModel.activePoolReady.collectAsState()

    LaunchedEffect(Unit) {
        SettingsManager.store.get()?.let { settings ->
            SettingsManager.themeState.value = settings.selectedTheme
        }
        Napier.base(DebugAntilog())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                poolsViewModel.startInitialChecks()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val colorScheme = when (themeState) {
        Theme.SYSTEM.id -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
        Theme.LIGHT.id -> LightColorScheme
        Theme.DARK.id -> DarkColorScheme
        else -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
    }

    JoinstrTheme(colorScheme = colorScheme) {
        val navController: NavHostController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        var selectedItem by rememberSaveable { mutableIntStateOf(0) }

        LaunchedEffect(navBackStackEntry?.destination?.id) {
            when (navBackStackEntry?.destination?.id) {
                Home.serializer().generateHashCode() -> {
                    selectedItem = 0
                }

                Pools.serializer().generateHashCode() -> {
                    selectedItem = 1
                }

                Settings.serializer().generateHashCode() -> {
                    selectedItem = 2
                }
            }
        }

        SnackbarControllerProvider { host ->
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                snackbarHost = {
                    SnackbarHost(
                        hostState = host,
                        snackbar = { data ->
                            CustomStackedSnackbar(
                                snackbarData = data,
                                onActionClicked = {
                                    data.performAction()
                                }
                            )
                        }
                    )
                },
                bottomBar = {
                    if (navBackStackEntry?.destination?.id != InputRegistration.serializer().generateHashCode()) {
                        BottomAppBar(
                            actions = {
                                val navItems = listOf(NavItem.HomeScreen, NavItem.PoolsScreen, NavItem.SettingsScreen)

                                NavigationBar(
                                    tonalElevation = 0.dp
                                ) {
                                    navItems.forEachIndexed { index, item ->
                                        NavigationBarItem(
                                            alwaysShowLabel = true,
                                            icon = {
                                                item.icon?.let { icon ->
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = item.title
                                                    )
                                                }
                                                item.drawableResource?.let { icon ->
                                                    Icon(
                                                        modifier = Modifier.size(24.dp),
                                                        painter = painterResource(icon),
                                                        contentDescription = item.title
                                                    )
                                                }
                                            },
                                            label = { if (selectedItem == index) Text(item.title) },
                                            selected = selectedItem == index,
                                            onClick = {
                                                if (selectedItem != index) {
                                                    selectedItem = index
                                                    navController.navigate(item.path)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->

                NavHost(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    navController = navController,
                    startDestination = NavItem.HomeScreen.path
                ) {

                    animatedComposable<Home> {
                        HomeScreen()
                    }

                    animatedComposable<InputRegistration> { backStackEntry ->
                        val poolId = backStackEntry.toRoute<InputRegistration>().id
                        RegisterInputScreen(poolId = poolId) {
                            poolsViewModel.startActiveReadyPoolsCheck()
                            navController.navigate(NavItem.HomeScreen.path)
                        }
                    }

                    animatedComposable<Pools> {
                        PoolScreen(
                            poolsViewModel = poolsViewModel
                        )
                    }

                    animatedComposable<Settings> {
                        SettingsScreen {
                            navController.popBackStack()
                        }
                    }
                }

                if (activePoolReady.first && navBackStackEntry?.destination?.id != InputRegistration.serializer().generateHashCode()) {
                    val pool = InputRegistration(id = activePoolReady.second)
                    navController.navigate(pool)
                    poolsViewModel.cancelReadyActivePoolsCheck()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
inline fun <reified T : Any> NavGraphBuilder.animatedComposable(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T>(
        enterTransition = { expandFromCenter() },
        exitTransition = { shrinkToCenter() },
        content = content
    )
}

@ExperimentalAnimationApi
fun expandFromCenter(): EnterTransition {
    return scaleIn(
        animationSpec = tween(300),
        initialScale = 0.8f,
        transformOrigin = TransformOrigin.Center
    ) + fadeIn(animationSpec = tween(300))
}

@ExperimentalAnimationApi
fun shrinkToCenter(): ExitTransition {
    return scaleOut(
        animationSpec = tween(300),
        targetScale = 0.8f,
        transformOrigin = TransformOrigin.Center
    ) + fadeOut(animationSpec = tween(300))
}

expect fun getWebSocketClient(): HttpClient

expect fun getSettingsStore(): KStore<SettingsStore>
expect fun getPoolsStore(): KStore<List<LocalPoolContent>>
expect fun getHistoryStore(): KStore<List<CoinJoinHistory>>

val vpnConnected: MutableStateFlow<Boolean> = MutableStateFlow(false)
val currentChain: MutableStateFlow<String> = MutableStateFlow("Signet")

expect suspend fun connectVpn(
    vpnHost: String,
    vpnIpAddress: String,
    vpnPort: String,
)

expect fun disconnectVpn()

expect fun getPlatform(): Platform

enum class Platform {
    ANDROID,
    IOS,
    DESKTOP,
    WASM_JS
}

expect suspend fun createPsbt(
    poolId: String,
    unspentItem: ListUnspentResponseItem,
): String?

expect suspend fun joinPsbts(
    listOfPsbts: List<String>,
): Pair<String?, String?>

expect fun getSharedSecret(
    privateKey: ByteArray,
    pubKey: ByteArray,
): ByteArray

expect fun pubkeyCreate(
    privateKey: ByteArray,
): ByteArray

expect suspend fun signSchnorr(
    content: ByteArray,
    privateKey: ByteArray,
    freshRandomBytes: ByteArray,
): ByteArray

expect object LocalNotification {
    fun showNotification(
        title: String,
        message: String,
    )

    suspend fun requestPermission(): Boolean
}

expect fun openLink(link: String)

expect fun runtimeEnvironment(): TorRuntime.Environment?

private val ThrowOnFailure = OnFailure { throw it }

@Composable
@Preview
fun AppTest() {
    val lifecycleOwner = LocalLifecycleOwner.current
    var showContent by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val logItems by remember { LogItem.Holder.getOrCreate(LOG_HOLDER_NAME).items }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch(Dispatchers.Main) {
                    while (true) {
                        println(Tor.listeners().socks.joinToString())
                        val ip = invincible.privacy.joinstr.network.HttpClient().ipAddress(Tor.listeners().socks.firstOrNull()?.port?.value)
                        logItems.add(LogItem(Clock.System.now().toEpochMilliseconds(), RuntimeEvent.STATE, ip ?: "h"))
                        delay(5000) // Wait for 5 seconds
                    }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    Box(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(showContent, Modifier.fillMaxHeight()) {

            LazyColumn(
                modifier = Modifier.padding(bottom = 48.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(count = logItems.size, key = { logItems[it].id }) { index ->
                    val item = logItems[index]
                    LogCardItem(item)
                }
            }
        }

        if (!showContent) {
            Button(
                onClick = {
                    Tor.enqueue(
                        action = Action.StartDaemon,
                        onFailure = ThrowOnFailure,
                        onSuccess = OnSuccess.noOp(),
                    )

                    showContent = !showContent
                }
            ) {
                Text("Start Tor")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Button(
                    onClick = {
                        Tor.enqueue(
                            action = Action.StopDaemon,
                            onFailure = ThrowOnFailure,
                            onSuccess = OnSuccess.noOp(),
                        )
                    }
                ) {
                    Text("Stop Tor")
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        Tor.enqueue(
                            action = Action.StartDaemon,
                            onFailure = ThrowOnFailure,
                            onSuccess = OnSuccess.noOp(),
                        )
                    }
                ) {
                    Text("Start Tor")
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        Tor.enqueue(
                            action = Action.RestartDaemon,
                            onFailure = ThrowOnFailure,
                            onSuccess = OnSuccess.noOp(),
                        )
                    }
                ) {
                    Text("Restart Tor")
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        /*Tor.enqueue(
                            cmd = TorCmd.Signal.NewNym,
                            onFailure = ThrowOnFailure,
                            onSuccess = OnSuccess.noOp(),
                        )*/
                     //  Tor.executeSync(TorCmd.Signal.NewNym)
                        scope.launch {
                            val result = Tor.executeAsync(TorCmd.Signal.NewNym)
                            println("NewNym command result: $result")
                        }
                    }
                ) {
                    Text("New Identity")
                }
            }
        }
    }
}

@Composable
private fun LogCardItem(item: LogItem?) {
    var textColor = Color.White

    val bg = when (item?.event) {
        is RuntimeEvent.ERROR -> Color.Red
        is RuntimeEvent.LOG.DEBUG -> {
            var color = Color.Blue
            if (item.data.startsWith("RealTorCtrl")) {
                color = color.copy(alpha = 0.5f)
            }
            color
        }

        is RuntimeEvent.LOG.INFO -> {
            textColor = Color.DarkGray
            Color.Yellow
        }

        is RuntimeEvent.LOG.WARN -> Color.Red.copy(alpha = 0.75f)
        is RuntimeEvent.READY -> {
            textColor = Color.DarkGray
            Color.Green
        }

        else -> Color.DarkGray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Text(
            modifier = Modifier.padding(4.dp),
            text = item?.data ?: "",
            fontSize = 12.sp,
            color = textColor,
        )
    }
}

