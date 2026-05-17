package com.example.currencyexchange

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import com.example.currencyexchange.ui.navigation.Screen
import com.example.currencyexchange.ui.home.HomeScreen
import com.example.currencyexchange.ui.details.DetailsScreen
import com.example.currencyexchange.ui.edit.EditListScreen
import com.example.currencyexchange.ui.settings.SettingsScreen
import com.example.currencyexchange.ui.AppViewModelFactory

import com.example.currencyexchange.ui.home.HomeViewModel
import com.example.currencyexchange.ui.details.DetailsViewModel
import com.example.currencyexchange.ui.settings.SettingsViewModel
import com.example.currencyexchange.ui.edit.EditListViewModel
import com.example.currencyexchange.util.ConnectivityObserver

@Composable
fun MainScreen(factory: AppViewModelFactory, connectivityObserver: ConnectivityObserver) {
    val navController = rememberNavController()
    val status by connectivityObserver.observe().collectAsState(initial = ConnectivityObserver.Status.Unavailable)
    val isOnline = status == ConnectivityObserver.Status.Available

    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600

    var selectedCurrencyCode by remember { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val items = listOf(Screen.Home, Screen.EditList, Screen.Settings)

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            val icon = when(screen) {
                                Screen.Home -> Icons.Default.Home
                                Screen.EditList -> Icons.Default.List
                                Screen.Settings -> Icons.Default.Settings
                                else -> Icons.Default.Home
                            }
                            Icon(icon, contentDescription = screen.title)
                        },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        ),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val homeVm: HomeViewModel = viewModel(factory = factory)
                
                if (isTablet) {
                    if (selectedCurrencyCode != null) {
                        // Tablet - wybrana waluta: Split-pane
                        Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                            Box(modifier = Modifier.weight(1f)) {
                                HomeScreen(
                                    viewModel = homeVm,
                                    onCurrencyClick = { code -> selectedCurrencyCode = code },
                                    isOnline = isOnline
                                )
                            }
                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                            Box(modifier = Modifier.weight(1.5f)) {
                                val detailsVm: DetailsViewModel = viewModel(factory = factory, key = selectedCurrencyCode)
                                DetailsScreen(
                                    currencyCode = selectedCurrencyCode!!,
                                    viewModel = detailsVm,
                                    onBackClick = { selectedCurrencyCode = null },
                                    showBackButton = true,
                                    forceVerticalLayout = true
                                )
                            }
                        }
                    } else {
                        // Tablet - brak wyboru: Jedna kolumna na środku
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.fillMaxWidth(if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 0.6f else 1f)) {
                                HomeScreen(
                                    viewModel = homeVm,
                                    onCurrencyClick = { code -> selectedCurrencyCode = code },
                                    isOnline = isOnline
                                )
                            }
                        }
                    }
                } else {
                    // Telefon: Zawsze najpierw sama lista (pion/poziom)
                    HomeScreen(
                        viewModel = homeVm,
                        onCurrencyClick = { code -> 
                            navController.navigate(Screen.Details.createRoute(code))
                        },
                        isOnline = isOnline
                    )
                }
            }

            composable(
                route = Screen.Details.route,
                arguments = listOf(navArgument("currencyCode") { type = NavType.StringType })
            ) { backStackEntry ->
                val code = backStackEntry.arguments?.getString("currencyCode") ?: ""
                val vm: DetailsViewModel = viewModel(factory = factory, key = code)
                DetailsScreen(currencyCode = code, viewModel = vm, onBackClick = {
                    navController.popBackStack()
                })
            }

            composable(Screen.EditList.route) {
                val vm: EditListViewModel = viewModel(factory = factory)
                EditListScreen(viewModel = vm)
            }

            composable(Screen.Settings.route) {
                val vm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(viewModel = vm, isOnline = isOnline)
            }
        }
    }
}