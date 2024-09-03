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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import invincible.privacy.joinstr.model.LocalPoolContent
import invincible.privacy.joinstr.model.NavItem
import invincible.privacy.joinstr.theme.DarkColorScheme
import invincible.privacy.joinstr.theme.JoinstrTheme
import invincible.privacy.joinstr.theme.LightColorScheme
import invincible.privacy.joinstr.ui.components.CustomStackedSnackbar
import invincible.privacy.joinstr.ui.components.SnackbarControllerProvider
import invincible.privacy.joinstr.ui.home.HomeScreen
import invincible.privacy.joinstr.ui.pools.PoolScreen
import invincible.privacy.joinstr.ui.pools.PoolsViewModel
import invincible.privacy.joinstr.ui.registerInput.RegisterInputScreen
import invincible.privacy.joinstr.ui.settings.SettingsScreen
import invincible.privacy.joinstr.utils.Settings
import invincible.privacy.joinstr.utils.SettingsManager
import invincible.privacy.joinstr.utils.Theme
import io.github.xxfast.kstore.KStore
import io.ktor.client.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(
    poolsViewModel: PoolsViewModel = viewModel { PoolsViewModel() }
) {
    val themeState by SettingsManager.themeState.collectAsState()
    val activePoolReady by poolsViewModel.activePoolReady.collectAsState()

    LaunchedEffect(Unit) {
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
            ) { innerPadding ->

                if (activePoolReady && navBackStackEntry?.destination?.route != NavItem.InputRegistration.path) {
                    navController.navigate(NavItem.InputRegistration.path)
                }

                NavHost(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    navController = navController,
                    startDestination = NavItem.Home.path
                ) {
                    animatedComposable(NavItem.Home.path) {
                        HomeScreen()
                    }

                    animatedComposable(NavItem.InputRegistration.path) {
                        RegisterInputScreen()
                    }

                    animatedComposable(NavItem.Pools.path) {
                        PoolScreen(
                            poolsViewModel = poolsViewModel
                        )
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

expect fun getWebSocketClient(): HttpClient

expect fun getSettingsStore(): KStore<Settings>
expect fun getPoolsStore(): KStore<List<LocalPoolContent>>

expect fun Float.convertFloatExponentialToString(): String

expect fun getSharedSecret(privateKey: ByteArray, pubKey: ByteArray): ByteArray
expect fun pubkeyCreate(privateKey: ByteArray): ByteArray
expect suspend fun signSchnorr(content: ByteArray, privateKey: ByteArray, freshRandomBytes: ByteArray): ByteArray