package com.gcal.app.ui.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gcal.app.model.modelFacade.ModelFacade
import com.gcal.app.ui.view_model.loginViewModel.LoginViewModel
import com.gcal.app.ui.view_model.mainViewModel.MainViewModel
import com.gcal.app.ui.view_model.profileViewModel.ProfileViewModel
import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardViewModel
import com.gcal.app.ui.view_model.reportViewModel.ReportViewModel

/**
 * Custom Factory that enables Dependency Injection for ViewModels.
 * * WHY IS THIS NECESSARY?
 * By default, Android's ViewModelProvider can only instantiate ViewModels with an empty constructor.
 * Since our architecture requires every ViewModel to receive the [ModelFacade] to communicate
 * with the backend/database, this factory acts as the central provider that "injects"
 * the singleton model instance into every screen.
 */
class AppViewModelFactory(private val model: ModelFacade) : ViewModelProvider.Factory {

    /**
     * Maps requested ViewModel classes to their specific implementation with parameters.
     * * Using isAssignableFrom ensures that this factory remains robust even if we
     * use subclasses or specific scoped ViewModels in the future.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            // Screen 1: Login & Onboarding flow
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(model) as T
            }

            // Screen 2: Main Calendar and Entry management
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(model) as T
            }

            // Screen 3: User Profile, Settings, and Friends management
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(model) as T
            }

            // Screen 4: Competitive gamification and Ranking
            modelClass.isAssignableFrom(LeaderboardViewModel::class.java) -> {
                LeaderboardViewModel(model) as T
            }

            // Screen 5: Analytics and Achievement reporting
            modelClass.isAssignableFrom(ReportViewModel::class.java) -> {
                ReportViewModel(model) as T
            }

            else -> throw IllegalArgumentException(
                "Unknown ViewModel class: ${modelClass.name}. " +
                        "Check if the class is registered in AppViewModelFactory."
            )
        }
    }
}