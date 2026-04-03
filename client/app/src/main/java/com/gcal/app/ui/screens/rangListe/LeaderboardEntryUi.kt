package com.gcal.ui.screens.rangliste

/**
 * LeaderboardEntryUi — Visual data for a single user card in the leaderboard list.
 *
 * @param rank        The user's position in the leaderboard
 * @param username    The user's display name.
 * @param level       The computed level based on total XP.
 * @param starCount   Number of stars for level visualization
 * @param xpDisplayString Pre-formatted XP string
 * @param nextLevelProgress Normalized progress toward the next level
 */
data class LeaderboardEntryUi(
    val rank: Int,
    val username: String,
    val level: Int,
    val starCount: Int,
    val xpDisplayString: String,
    val nextLevelProgress: Float
)