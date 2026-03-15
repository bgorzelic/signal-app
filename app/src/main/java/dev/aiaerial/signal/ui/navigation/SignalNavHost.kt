package dev.aiaerial.signal.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.aiaerial.signal.data.openclaw.OpenClawClient
import dev.aiaerial.signal.ui.logimport.LogImportScreen
import dev.aiaerial.signal.ui.scanner.ScannerScreen
import dev.aiaerial.signal.ui.settings.SettingsScreen
import dev.aiaerial.signal.ui.syslog.SyslogScreen
import dev.aiaerial.signal.ui.theme.ElectricTeal
import dev.aiaerial.signal.ui.theme.TextSecondary
import dev.aiaerial.signal.ui.theme.TextTertiary
import dev.aiaerial.signal.ui.theme.Void
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
    TopLevelRoute("Scanner", ScannerRoute, Icons.Outlined.NetworkCheck),
    TopLevelRoute("Syslog", SyslogRoute, Icons.AutoMirrored.Outlined.Message),
    TopLevelRoute("Timeline", TimelineRoute, Icons.Outlined.Timeline),
    TopLevelRoute("Settings", SettingsRoute, Icons.Outlined.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalNavHost(
    openClawClient: OpenClawClient,
) {
    val navController = rememberNavController()

    Scaffold(
        containerColor = Void,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SIGNAL",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        color = ElectricTeal,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Void,
                    titleContentColor = ElectricTeal,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Void,
                tonalElevation = 0.dp,
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                topLevelRoutes.forEach { topLevelRoute ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.hasRoute(topLevelRoute.route::class)
                    } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                topLevelRoute.icon,
                                contentDescription = topLevelRoute.name,
                            )
                        },
                        label = {
                            Text(
                                topLevelRoute.name.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 1.sp,
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(topLevelRoute.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricTeal,
                            selectedTextColor = ElectricTeal,
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary,
                            indicatorColor = ElectricTeal.copy(alpha = 0.12f),
                        ),
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
                    openClawClient = openClawClient,
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
