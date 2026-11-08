package com.smapra.exambro.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.smapra.exambro.presentation.exam.ExamScreen
import com.smapra.exambro.presentation.setup.SetupScreen
import com.smapra.exambro.presentation.splash.SplashScreen
import java.net.URLDecoder
import java.net.URLEncoder

object NavRoutes {
    const val SPLASH = "splash"
    const val SETUP = "setup"
    const val EXAM = "exam/{url}"

    fun examRoute(url: String): String {
        val encoded = URLEncoder.encode(url, "UTF-8")
        return "exam/$encoded"
    }
}

@Composable
fun ExamBroNavGraph(
    navController: NavHostController,
    startDestination: String = NavRoutes.SPLASH,
    sebUrl: String? = null,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(NavRoutes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(NavRoutes.SETUP) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(NavRoutes.SETUP) {
            SetupScreen(
                onNavigateToExam = { url ->
                    navController.navigate(NavRoutes.examRoute(url)) {
                        popUpTo(NavRoutes.SETUP) { inclusive = false }
                    }
                },
            )
        }

        composable(
            route = NavRoutes.EXAM,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url").orEmpty()
            val decodedUrl = URLDecoder.decode(encodedUrl, "UTF-8")

            ExamScreen(
                url = decodedUrl,
                onExitExam = {
                    navController.popBackStack(NavRoutes.SETUP, inclusive = false)
                },
            )
        }
    }
}
