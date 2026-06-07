package com.maathisv.vigotrack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.maathisv.vigotrack.ui.screens.ActivitySessionScreen
import com.maathisv.vigotrack.ui.screens.BilanScreen
import com.maathisv.vigotrack.ui.screens.HomeViewModel
import com.maathisv.vigotrack.ui.screens.StageDetailScreen
import com.maathisv.vigotrack.ui.screens.StagesListScreen

sealed class NavRoutes(val route: String) {
    object StagesList : NavRoutes("stages")
    object StageDetail : NavRoutes("stageDetail/{stageId}") {
        fun createRoute(stageId: Long) = "stageDetail/$stageId"
    }
    object Bilan : NavRoutes("bilan/{stageId}") {
        fun createRoute(stageId: Long) = "bilan/$stageId"
    }
    object ActivitySession : NavRoutes("activitySession/{activityId}") {
        fun createRoute(activityId: String) = "activitySession/$activityId"
    }
}

@Composable
fun VigoTrackNavGraph(
    navController: NavHostController,
    homeViewModel: HomeViewModel
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.StagesList.route
    ) {
        composable(NavRoutes.StagesList.route) {
            StagesListScreen(
                homeViewModel = homeViewModel,
                onStageClick = { stageId ->
                    navController.navigate(NavRoutes.StageDetail.createRoute(stageId))
                }
            )
        }
        composable(
            route = NavRoutes.StageDetail.route,
            arguments = listOf(navArgument("stageId") { type = NavType.LongType })
        ) { backStackEntry ->
            val stageId = backStackEntry.arguments?.getLong("stageId") ?: return@composable
            StageDetailScreen(
                stageId = stageId,
                homeViewModel = homeViewModel,
                onBilanClick = { sid ->
                    navController.navigate(NavRoutes.Bilan.createRoute(sid))
                },
                onActivityClick = { activityId ->
                    navController.navigate(NavRoutes.ActivitySession.createRoute(activityId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = NavRoutes.Bilan.route,
            arguments = listOf(navArgument("stageId") { type = NavType.LongType })
        ) { backStackEntry ->
            val stageId = backStackEntry.arguments?.getLong("stageId") ?: return@composable
            BilanScreen(
                stageId = stageId,
                homeViewModel = homeViewModel,
                onActivityClick = { activityId ->
                    navController.navigate(NavRoutes.ActivitySession.createRoute(activityId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.ActivitySession.route) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId") ?: return@composable
            ActivitySessionScreen(
                activityId = activityId,
                homeViewModel = homeViewModel,
                onBack = { navController.popBackStack() },
                onTypeChanged = { newActivityId ->
                    navController.navigate(NavRoutes.ActivitySession.createRoute(newActivityId)) {
                        popUpTo(NavRoutes.ActivitySession.createRoute(activityId)) { inclusive = true }
                    }
                }
            )
        }
    }
}
