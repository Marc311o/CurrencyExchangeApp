package com.example.currencyexchange

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.currencyexchange.ui.AppViewModelFactory
import com.example.currencyexchange.ui.navigation.AppBottomNavigation
import com.example.currencyexchange.ui.navigation.AppNavigation
import com.example.currencyexchange.util.ConnectivityObserver

@Composable
fun MainScreen(factory: AppViewModelFactory, connectivityObserver: ConnectivityObserver) {
    val navController = rememberNavController()
    val status by connectivityObserver.observe().collectAsState(initial = ConnectivityObserver.Status.Unavailable)
    val isOnline = status == ConnectivityObserver.Status.Available

    Scaffold(
        bottomBar = {
            AppBottomNavigation(navController = navController)
        }
    ) { innerPadding ->
        Box(modifier = androidx.compose.ui.Modifier.padding(innerPadding)) {
            AppNavigation(
                navController = navController,
                factory = factory,
                isOnline = isOnline
            )
        }
    }
}
