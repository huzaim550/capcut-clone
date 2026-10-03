package com.reelforge.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reelforge.app.editor.EditorPlaceholderScreen
import com.reelforge.core.database.ProjectRepository
import com.reelforge.feature.home.HomeRoute

/**
 * The app's navigation graph.
 *
 * The browser asks to "open a project"; this is where that intent becomes a
 * route. Keeping the mapping here means the home feature never imports a
 * navigation library, and swapping the editor in later is a one-line change.
 */
@Composable
fun ReelforgeNavHost(
    repository: ProjectRepository,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = ReelforgeRoutes.HOME) {
        composable(ReelforgeRoutes.HOME) {
            HomeRoute(
                repository = repository,
                onOpenProject = { id -> navController.navigate(ReelforgeRoutes.editor(id.value)) },
            )
        }

        composable(
            route = ReelforgeRoutes.EDITOR_PATTERN,
            arguments = listOf(navArgument(ReelforgeRoutes.ARG_PROJECT_ID) { type = NavType.StringType }),
        ) { entry ->
            EditorPlaceholderScreen(
                projectId = entry.arguments?.getString(ReelforgeRoutes.ARG_PROJECT_ID).orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
