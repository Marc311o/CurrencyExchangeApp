package com.example.currencyexchange

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
fun MainScreen(factory: AppViewModelFactory) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFF5F5F5)) {
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
                val vm: HomeViewModel = viewModel(factory = factory)
                HomeScreen(viewModel = vm, onCurrencyClick = { code ->
                    navController.navigate(Screen.Details.createRoute(code))
                })
            }

            composable(
                route = Screen.Details.route,
                arguments = listOf(navArgument("currencyCode") { type = NavType.StringType })
            ) { backStackEntry ->
                val code = backStackEntry.arguments?.getString("currencyCode") ?: ""
                val vm: DetailsViewModel = viewModel(factory = factory)
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
                SettingsScreen(viewModel = vm)
            }
        }
    }
}