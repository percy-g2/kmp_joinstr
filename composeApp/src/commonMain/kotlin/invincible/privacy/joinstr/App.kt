package invincible.privacy.joinstr

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.whyoleg.cryptography.CryptographyProvider
import invincible.privacy.joinstr.model.NavItem
import invincible.privacy.joinstr.theme.DarkColorScheme
import invincible.privacy.joinstr.theme.JoinstrTheme
import invincible.privacy.joinstr.theme.LightColorScheme
import invincible.privacy.joinstr.theme.Settings
import invincible.privacy.joinstr.theme.SettingsManager
import invincible.privacy.joinstr.theme.Theme
import invincible.privacy.joinstr.ui.ListUnspentCloudsScreen
import invincible.privacy.joinstr.ui.pools.PoolScreen
import invincible.privacy.joinstr.ui.SettingsScreen
import io.github.xxfast.kstore.KStore
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val themeState by SettingsManager.themeState.collectAsState()

    LaunchedEffect(Unit) {
      //  main()
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

        Scaffold(
            modifier = Modifier.fillMaxSize(),
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
                                        selectedItem = index
                                        navController.navigate(item.path)
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
                composable(route = NavItem.Home.path) {
                    ListUnspentCloudsScreen()
                }
                composable(route = NavItem.Pools.path) {
                    PoolScreen()
                }
                composable(route = NavItem.Settings.path) {
                    SettingsScreen {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}

/*
@OptIn(DelicateCoroutinesApi::class)
fun main() {
    GlobalScope.launch {
        // Initialize CryptoUtils
        val privateKey = CryptoUtils.generatePrivateKey()
        val publicKey = CryptoUtils.getPublicKey(privateKey)
        val sharedSecret = CryptoUtils.getSharedSecret(privateKey, publicKey)
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

        sendTestEvent()
    }
}

suspend fun sendTestEvent() {
    val content = "This is a test Nostr note"
    val nostrUtil = NostrUtil()
    val nostrEvent = nostrUtil.createKindEvent(content, Events.NOTE)
    println("Event to be sent: $nostrEvent")
    NostrClient().sendEvent(nostrEvent)
}
*/

expect fun getKStore(): KStore<Settings>


expect fun getCryptoProvider(): CryptographyProvider