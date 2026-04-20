package com.example.currencyexchange.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object EditList : Screen("edit_list", "Edit list")
    object Settings : Screen("settings", "Settings")

    object Details : Screen("details/{currencyCode}", "Details") {
        fun createRoute(code: String) = "details/$code"
    }
}