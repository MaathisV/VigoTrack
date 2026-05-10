package com.maathisv.vigotrack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.maathisv.vigotrack.ui.screens.HomeScreen
import com.maathisv.vigotrack.ui.screens.ActivitySessionScreen
import com.maathisv.vigotrack.ui.screens.HomeViewModel

sealed class NavRoutes(val route: String) {
    object Home : NavRoutes("home")
    object ActivitySession : NavRoutes("activitySession/{activityId}") {
        fun createRoute(activityId: String) = "activitySession/$activityId"
    }
}

@Composable
fun VigoTrackNavGraph(
    navController: NavHostController = rememberNavController(),
    homeViewModel: HomeViewModel
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home.route
    ) {
        composable(NavRoutes.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onActivityClick = { activityId ->
                    navController.navigate(NavRoutes.ActivitySession.createRoute(activityId))
                }
            )
        }
        composable(NavRoutes.ActivitySession.route) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId") ?: return@composable
            ActivitySessionScreen(
                activityId = activityId,
                homeViewModel = homeViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
