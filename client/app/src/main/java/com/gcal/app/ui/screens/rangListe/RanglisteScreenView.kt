package com.gcal.app.ui.screens.rangliste

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gcal.app.ui.components.GCalBottomBar
import com.gcal.app.ui.components.GCalTopBar
import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardEntryUi
import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardScreenState
import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardUiEvent
import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardUiState
import com.gcal.app.ui.view_model.leaderboardViewModel.LeaderboardViewModel
import androidx.compose.ui.res.stringResource
import com.gcal.app.R

/**
 * RanglisteScreenView - UI For Rangliste
 *
 */
@Composable
fun RanglisteScreenView(
    viewModel: LeaderboardViewModel,
    currentRoute: String?,
    onBackClicked: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(LeaderboardUiEvent.OnScreenEntered)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            GCalTopBar(
                title = stringResource(R.string.leaderboard_title),
                onBackClicked = onBackClicked
            )
        },
        bottomBar = {
            GCalBottomBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when (uiState.screenState) {
                LeaderboardScreenState.LOADING -> {
                    LoadingStateView()
                }
                LeaderboardScreenState.ERROR -> {
                    ErrorStateView(
                        errorMessage = uiState.errorMessage,
                        onRetryClicked = {
                            viewModel.onEvent(LeaderboardUiEvent.OnRetryClicked)
                        }
                    )
                }
                LeaderboardScreenState.LOADED -> {
                    if (uiState.entries.isEmpty()) {
                        EmptyStateView()
                    } else {
                        LeaderboardList(
                            entries = uiState.entries,
                            userRankIndex = uiState.currentUserRankIndex ?: -1,
                            contentPadding = PaddingValues(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Stateless Version for Tets and Previews
 */
@Composable
fun RanglisteScreenContent(
    state: LeaderboardUiState,
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state.screenState) {
            LeaderboardScreenState.LOADING -> {
                LoadingStateView()
            }
            LeaderboardScreenState.ERROR -> {
                ErrorStateView(
                    errorMessage = state.errorMessage,
                    onRetryClicked = onRetryClicked
                )
            }
            LeaderboardScreenState.LOADED -> {
                if (state.entries.isEmpty()) {
                    EmptyStateView()
                } else {
                    LeaderboardList(
                        entries = state.entries,
                        userRankIndex = state.currentUserRankIndex ?: -1,
                        contentPadding = PaddingValues(16.dp)
                    )
                }
            }
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true)
@Composable
private fun RanglisteScreenLoadingPreview() {
    MaterialTheme {
        RanglisteScreenContent(
            state = LeaderboardUiState(screenState = LeaderboardScreenState.LOADING),
            onRetryClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RanglisteScreenLoadedPreview() {
    MaterialTheme {
        RanglisteScreenContent(
            state = LeaderboardUiState(
                screenState = LeaderboardScreenState.LOADED,
                entries = listOf(
                    LeaderboardEntryUi(1, "ProGamer", 10, 10, "XP: 1000", 0.5f),
                    LeaderboardEntryUi(2, "Alice", 5, 5, "XP: 500", 0.0f),
                    LeaderboardEntryUi(3, "Bob", 3, 3, "XP: 300", 0.5f)
                ),
                currentUserRankIndex = 2
            ),
            onRetryClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RanglisteScreenErrorPreview() {
    MaterialTheme {
        RanglisteScreenContent(
            state = LeaderboardUiState(
                screenState = LeaderboardScreenState.ERROR,
                errorMessage = "Netzwerkfehler: Bitte prüfen Sie Ihre Internetverbindung."
            ),
            onRetryClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RanglisteScreenEmptyPreview() {
    MaterialTheme {
        RanglisteScreenContent(
            state = LeaderboardUiState(
                screenState = LeaderboardScreenState.LOADED,
                entries = emptyList()
            ),
            onRetryClicked = {}
        )
    }
}