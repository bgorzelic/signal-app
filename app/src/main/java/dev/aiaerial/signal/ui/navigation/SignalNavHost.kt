package dev.aiaerial.signal.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.aiaerial.signal.ui.logimport.LogImportScreen
import dev.aiaerial.signal.ui.scanner.ScannerScreen
import dev.aiaerial.signal.ui.settings.SettingsScreen
import dev.aiaerial.signal.ui.syslog.SyslogScreen
import dev.aiaerial.signal.ui.timeline.TimelineScreen
import kotlinx.serialization.Serializable

// Type-safe route objects
@Serializable object ScannerRoute
@Serializable object SyslogRoute
@Serializable object TimelineRoute
@Serializable object SettingsRoute
@Serializable object ImportRoute

data class TopLevelRoute<T : Any>(
    val name: String,
    val route: T,
    val icon: ImageVector,
)

val topLevelRoutes = listOf(
    TopLevelRoute("Scanner", ScannerRoute, Icons.Outlined.Wifi),
    TopLevelRoute("Syslog", SyslogRoute, Icons.AutoMirrored.Outlined.Message),
    TopLevelRoute("Timeline", TimelineRoute, Icons.Outlined.Timeline),
    TopLevelRoute("Settings", SettingsRoute, Icons.Outlined.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalNavHost() {
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SIGNAL") })
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                topLevelRoutes.forEach { topLevelRoute ->
                    NavigationBarItem(
                        icon = { Icon(topLevelRoute.icon, contentDescription = topLevelRoute.name) },
                        label = { Text(topLevelRoute.name) },
                        selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(topLevelRoute.route::class)
                        } == true,
                        onClick = {
                            navController.navigate(topLevelRoute.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ScannerRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<ScannerRoute> {
                ScannerScreen()
            }
            composable<SyslogRoute> {
                SyslogScreen(
                    onImportClick = { navController.navigate(ImportRoute) },
                )
            }
            composable<ImportRoute> {
                LogImportScreen(onBack = { navController.popBackStack() })
            }
            composable<TimelineRoute> {
                TimelineScreen()
            }
            composable<SettingsRoute> {
                SettingsScreen()
            }
        }
    }
}
