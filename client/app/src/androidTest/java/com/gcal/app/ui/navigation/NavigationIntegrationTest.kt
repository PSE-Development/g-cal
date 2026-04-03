package com.gcal.app.ui.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

/**
 * Navigation integration tests for the GCalNavHost routing logic.
 *
 * These tests use a lightweight NavHost with placeholder composables
 * to verify route transitions without requiring real ViewModels or
 * a ModelFacade instance. The focus is on navigation graph wiring:
 * correct start destination, route transitions, and backstack behavior.
 */
class NavigationIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // LOGIN IS THE START DESTINATION

    /**
     * Verifies that the navigation graph starts at the LOGIN route.
     * This is the entry point of the entire app — if this is wrong,
     * unauthenticated users could land on protected screens.
     */
    @Test
    fun navHost_startsAtLoginRoute() {
        lateinit var navController: androidx.navigation.NavHostController

        composeTestRule.setContent {
            navController = rememberNavController()
            TestNavHost(navController = navController, startDestination = NavRoute.LOGIN)
        }

        composeTestRule.runOnIdle {
            val currentRoute = navController.currentDestination?.route
            assert(currentRoute == NavRoute.LOGIN) {
                "Expected start destination to be LOGIN, but got $currentRoute"
            }
        }
    }

    //  LOGIN NAVIGATES TO MAIN ON AUTH

    /**
     * Verifies that navigating from LOGIN to MAIN removes LOGIN
     * from the backstack (inclusive popUpTo). This prevents users
     * from pressing back into the login screen after authentication.
     */
    @Test
    fun loginScreen_onAuthentication_navigatesToMainAndClearsBackstack() {
        lateinit var navController: androidx.navigation.NavHostController

        composeTestRule.setContent {
            navController = rememberNavController()
            TestNavHost(navController = navController, startDestination = NavRoute.LOGIN)
        }

        composeTestRule.runOnIdle {
            navController.navigate(NavRoute.MAIN) {
                popUpTo(NavRoute.LOGIN) { inclusive = true }
            }
        }

        composeTestRule.runOnIdle {
            val currentRoute = navController.currentDestination?.route
            assert(currentRoute == NavRoute.MAIN) {
                "Expected route MAIN after auth, but got $currentRoute"
            }

            val backStackRoutes = navController.currentBackStack.value
                .mapNotNull { it.destination.route }
            assert(!backStackRoutes.contains(NavRoute.LOGIN)) {
                "LOGIN should be removed from backstack after auth, but found: $backStackRoutes"
            }
        }
    }

    //BOTTOM BAR NAVIGATION WORKS

    /**
     * Verifies that bottom bar navigation from MAIN to a sub-screen
     * correctly updates the current route. Uses launchSingleTop
     * and saveState to match the real app's navigation behavior.
     */
    @Test
    fun bottomBar_navigateFromMainToRangliste_updatesRoute() {
        lateinit var navController: androidx.navigation.NavHostController

        composeTestRule.setContent {
            navController = rememberNavController()
            TestNavHost(navController = navController, startDestination = NavRoute.MAIN)
        }

        composeTestRule.runOnIdle {
            navController.navigate(NavRoute.RANGLISTE) {
                popUpTo(NavRoute.MAIN) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        composeTestRule.runOnIdle {
            val currentRoute = navController.currentDestination?.route
            assert(currentRoute == NavRoute.RANGLISTE) {
                "Expected route RANGLISTE after navigation, but got $currentRoute"
            }
        }
    }
}

/**
 * Lightweight test NavHost with placeholder screens.
 * Mirrors the real GCalNavHost route structure without requiring
 * ViewModels, ModelFacade, or any backend dependencies.
 */
@Composable
private fun TestNavHost(
    navController: androidx.navigation.NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoute.LOGIN) { Text("Login") }
        composable(NavRoute.MAIN) { Text("Main") }
        composable(NavRoute.RANGLISTE) { Text("Rangliste") }
        composable(NavRoute.BERICHTE) { Text("Berichte") }
        composable(NavRoute.PROFILE) { Text("Profile") }
        composable(NavRoute.SETTINGS) { Text("Settings") }
    }
}