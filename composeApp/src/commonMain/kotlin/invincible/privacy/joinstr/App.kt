package invincible.privacy.joinstr

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import invincible.privacy.joinstr.model.NavItem
import invincible.privacy.joinstr.model.PoolContent
import invincible.privacy.joinstr.network.NostrClient
import invincible.privacy.joinstr.theme.DarkColorScheme
import invincible.privacy.joinstr.theme.JoinstrTheme
import invincible.privacy.joinstr.theme.LightColorScheme
import invincible.privacy.joinstr.theme.Settings
import invincible.privacy.joinstr.theme.SettingsManager
import invincible.privacy.joinstr.theme.Theme
import invincible.privacy.joinstr.ui.RegisterInputScreen
import invincible.privacy.joinstr.ui.SettingsScreen
import invincible.privacy.joinstr.ui.components.CustomStackedSnackbar
import invincible.privacy.joinstr.ui.components.SnackbarControllerProvider
import invincible.privacy.joinstr.ui.pools.PoolScreen
import invincible.privacy.joinstr.utils.CryptoUtils
import invincible.privacy.joinstr.utils.Event
import invincible.privacy.joinstr.utils.NostrUtil
import io.github.xxfast.kstore.KStore
import io.ktor.client.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val themeState by SettingsManager.themeState.collectAsState()

    LaunchedEffect(Unit) {
        //sendTestEvent()
        SettingsManager.store.get()?.let { settings ->
            SettingsManager.themeState.value = settings.selectedTheme
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

        LaunchedEffect(navBackStackEntry?.destination?.route) {
            when (navBackStackEntry?.destination?.route) {
                NavItem.Home.path -> {
                    selectedItem = 0
                }

                NavItem.Pools.path -> {
                    selectedItem = 1
                }

                NavItem.Settings.path -> {
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
                    BottomAppBar(
                        actions = {
                            val navItems = listOf(NavItem.Home, NavItem.Pools, NavItem.Settings)

                            NavigationBar {
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
            ) { innerPadding ->
                NavHost(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    navController = navController,
                    startDestination = NavItem.Home.path
                ) {
                    animatedComposable(NavItem.Home.path) {
                        RegisterInputScreen()
                    }
                    animatedComposable(NavItem.Pools.path) {
                        PoolScreen()
                    }
                    animatedComposable(NavItem.Settings.path) {
                        SettingsScreen {
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.animatedComposable(
    route: String,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
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

suspend fun sendTestEvent() {
    val privateKey = CryptoUtils.generatePrivateKey()
    val publicKey = CryptoUtils.getPublicKey(privateKey)
    val sharedSecret = getSharedSecret(privateKey, publicKey)
    val message = "This is a secret message"

    // Encrypt the message
    val encryptedMessage = CryptoUtils.encrypt(message, sharedSecret)
    println("Encrypted Message: $encryptedMessage")

    // Decrypt the message
    val decryptedMessage = CryptoUtils.decrypt(encryptedMessage, sharedSecret)
    println("Decrypted Message: $decryptedMessage")

    // Verify that the decrypted message matches the original message
    if (message == decryptedMessage) {
        println("Decryption successful! The decrypted message matches the original.")
    } else {
        println("Decryption failed! The decrypted message does not match the original.")
    }
    val content = "This is a test Nostr event"
    val nostrUtil = NostrUtil()
    val nostrEvent = nostrUtil.createEvent(content, Event.NOTE)
    println("Event to be sent: $nostrEvent")
    NostrClient().sendEvent(
        event = nostrEvent,
        onError = {
            println("Error sending event")
        },
        onSuccess = {
            println("Event sent successfully")
        }
    )
}

expect fun getWebSocketClient(): HttpClient

expect fun getSettingsStore(): KStore<Settings>
expect fun getPoolsStore(): KStore<List<PoolContent>>

expect fun Float.convertFloatExponentialToString(): String

expect fun getSharedSecret(privateKey: ByteArray, pubKey: ByteArray): ByteArray
expect fun pubkeyCreate(privateKey: ByteArray): ByteArray
expect suspend fun signSchnorr(content: ByteArray, privateKey: ByteArray, freshRandomBytes: ByteArray): ByteArray