package com.gcal.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gcal.app.model.modelFacade.ModelFacade
import com.gcal.app.ui.screens.berichte.BerichteScreenView
import com.gcal.app.ui.screens.login.LoginScreenView
import com.gcal.app.ui.screens.main.MainScreenView
import com.gcal.app.ui.screens.profile.ProfileScreenView
import com.gcal.app.ui.screens.profile.settings.SettingsScreenView
import com.gcal.app.ui.screens.rangliste.RanglisteScreenView
import com.gcal.app.ui.view_model.AppViewModelFactory
import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardViewModel
import com.gcal.app.ui.view_model.loginViewModel.LoginViewModel
import com.gcal.app.ui.view_model.mainViewModel.MainViewModel
import com.gcal.app.ui.view_model.profileViewModel.ProfileViewModel
import com.gcal.app.ui.view_model.reportViewModel.ReportViewModel

/**
 * GCalNavHost — Main navigation host that manages screen transitions.
 *
 * This composable is the root navigation controller for the entire app.
 * It creates ViewModels through a shared [AppViewModelFactory] and wires each screen
 * to its corresponding ViewModel with proper navigation callbacks.
 *
 * Screen Flow:
 * LOGIN → MAIN ↔ RANGLISTE / BERICHTE / PROFILE → SETTINGS
 *
 *
 * @param model ModelFacade instance — must be provided by caller (no default mock).
 * @param modifier Modifier for the host container.
 * @param navController NavHostController for navigation state management.
 * @param startDestination The initial screen route (defaults to LOGIN).
 */
@Composable
fun GCalNavHost(
    model: ModelFacade,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = NavRoute.LOGIN
) {

    val viewModelFactory = AppViewModelFactory(model)


    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        //  LOGIN SCREEN
        composable(route = NavRoute.LOGIN) {
            val loginViewModel: LoginViewModel = viewModel(factory = viewModelFactory)
            val uiState by loginViewModel.uiState.collectAsState()


            LaunchedEffect(uiState.isAuthenticated) {
                if (uiState.isAuthenticated) {
                    navController.navigate(NavRoute.MAIN) {
                        popUpTo(NavRoute.LOGIN) { inclusive = true }
                    }
                }
            }

            LoginScreenView(
                viewModel = loginViewModel
            )
        }

        //  MAIN SCREEN
        composable(route = NavRoute.MAIN) {
            val mainViewModel: MainViewModel = viewModel(factory = viewModelFactory)

            MainScreenView(
                viewModel = mainViewModel,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(NavRoute.MAIN) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        //  RANGLISTE SCREEN
        composable(route = NavRoute.RANGLISTE) {
            val leaderboardViewModel: LeaderboardViewModel = viewModel(factory = viewModelFactory)

            RanglisteScreenView(
                viewModel = leaderboardViewModel,
                currentRoute = currentRoute,
                onBackClicked = {
                    navController.navigate(NavRoute.MAIN) {
                        popUpTo(NavRoute.MAIN) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(NavRoute.MAIN) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        //  BERICHTE SCREEN
        composable(route = NavRoute.BERICHTE) {
            val reportViewModel: ReportViewModel = viewModel(factory = viewModelFactory)

            BerichteScreenView(
                viewModel = reportViewModel,
                currentRoute = currentRoute,
                onBackClicked = {
                    navController.navigate(NavRoute.MAIN) {
                        popUpTo(NavRoute.MAIN) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(NavRoute.MAIN) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // PROFILE SCREEN
        composable(route = NavRoute.PROFILE) {
            val profileViewModel: ProfileViewModel = viewModel(factory = viewModelFactory)
            val uiState by profileViewModel.uiState.collectAsState()


            LaunchedEffect(uiState.isLoggedOut) {
                if (uiState.isLoggedOut) {
                    navController.navigate(NavRoute.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            ProfileScreenView(
                viewModel = profileViewModel,
                currentRoute = currentRoute,
                onBackClicked = {
                    navController.navigate(NavRoute.MAIN) {
                        popUpTo(NavRoute.MAIN) { inclusive = true }
                    }
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(NavRoute.MAIN) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoute.SETTINGS)
                }
            )
        }

        // SETTINGS SCREEN
        composable(route = NavRoute.SETTINGS) {

            val profileEntry = remember(navBackStackEntry) {
                navController.getBackStackEntry(NavRoute.PROFILE)
            }
            val profileViewModel: ProfileViewModel = viewModel(
                viewModelStoreOwner = profileEntry,
                factory = viewModelFactory
            )

            SettingsScreenView(
                viewModel = profileViewModel,
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }
    }
}