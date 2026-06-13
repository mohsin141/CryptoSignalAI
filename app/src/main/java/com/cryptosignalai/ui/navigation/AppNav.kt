package com.cryptosignalai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cryptosignalai.ui.history.HistoryScreen
import com.cryptosignalai.ui.home.HomeScreen
import com.cryptosignalai.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenHistory = { nav.navigate(Routes.HISTORY) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { nav.popBackStack() })
        }
    }
}
