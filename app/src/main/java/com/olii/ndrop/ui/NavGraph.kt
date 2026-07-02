package com.olii.ndrop.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.olii.ndrop.ui.about.AboutScreen
import com.olii.ndrop.ui.discovery.DiscoveryScreen
import com.olii.ndrop.ui.home.ArCompassScreen
import com.olii.ndrop.ui.home.HomeScreen
import com.olii.ndrop.ui.settings.SettingsScreen
import com.olii.ndrop.ui.timer.TimerScreen
import com.olii.ndrop.viewmodel.HomeViewModel

/**
 * NDrop — NavGraph
 * Signature: Olii-8882
 */

object Routes {
    const val HOME       = "home"
    const val DISCOVERY  = "discovery"
    const val SETTINGS   = "settings"
    const val TIMER      = "timer"
    const val AR_COMPASS = "ar_compass"
    const val ABOUT      = "about"
}

@Composable
fun NDropNavGraph(
    navController: NavHostController = rememberNavController(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    isDarkTheme: Boolean = true
) {
    LaunchedEffect(Unit) {
        homeViewModel.navigateToTimer.collect {
            navController.navigate(Routes.TIMER) { launchSingleTop = true }
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToDiscovery  = { navController.navigate(Routes.DISCOVERY) },
                onNavigateToSettings   = { navController.navigate(Routes.SETTINGS) },
                onNavigateToTimer      = { navController.navigate(Routes.TIMER) },
                onNavigateToArCompass  = { navController.navigate(Routes.AR_COMPASS) },
                viewModel              = homeViewModel
            )
        }

        composable(Routes.DISCOVERY) {
            DiscoveryScreen(
                onBack = { navController.popBackStack() },
                onOpenInMaps = { lat, lng, title ->
                    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($title)")
                    navController.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                },
                isDarkTheme = isDarkTheme
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack            = { navController.popBackStack() },
                onNavigateToAbout = { navController.navigate(Routes.ABOUT) },
                isDarkTheme       = isDarkTheme
            )
        }

        composable(Routes.TIMER) {
            TimerScreen(
                onBack = { navController.popBackStack() },
                isDarkTheme = isDarkTheme
            )
        }

        composable(Routes.AR_COMPASS) {
            ArCompassScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                onBack      = { navController.popBackStack() },
                isDarkTheme = isDarkTheme
            )
        }
    }
}
