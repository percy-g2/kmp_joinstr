package invincible.privacy.joinstr

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import invincible.privacy.joinstr.model.NavItem
import invincible.privacy.joinstr.theme.DarkColorScheme
import invincible.privacy.joinstr.theme.JoinstrTheme
import invincible.privacy.joinstr.theme.LightColorScheme
import invincible.privacy.joinstr.theme.Settings
import invincible.privacy.joinstr.theme.SettingsManager
import invincible.privacy.joinstr.theme.Theme
import invincible.privacy.joinstr.ui.ListUnspentCloudsScreen
import invincible.privacy.joinstr.ui.SettingsScreen
import io.github.xxfast.kstore.KStore
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val themeState by SettingsManager.themeState.collectAsState()

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

                NavItem.Settings.path -> {
                    selectedItem = 1
                }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                BottomAppBar(
                    actions = {
                        val navItems = listOf(NavItem.Home, NavItem.Settings)

                        NavigationBar {
                            navItems.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    alwaysShowLabel = true,
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title
                                        )
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
                composable(route = NavItem.Settings.path) {
                    SettingsScreen {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}

expect fun getKStore(): KStore<Settings>