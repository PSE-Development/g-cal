package com.gcal.app.ui.screens.profile

/**
 * UserProfileUi — Data container for displaying user identity and gamification stats.
 *
 *
 * @param displayName   The user's chosen username.
 * @param avatarUrl     Optional URL for the user's profile picture.
 * @param currentLevel  Computed from total XP (level = totalXp / 100).
 * @param currentXp     Total accumulated experience points.
 * @param xpForNextLevel XP threshold required to reach the next level.
 */
data class UserProfileUi(
    val displayName: String,
    val avatarUrl: String?,
    val currentLevel: Int,
    val currentXp: Int,
    val xpForNextLevel: Int
) {

    val progressToNextLevel: Float
        get() {
            if (xpForNextLevel <= 0) return 1f
            val xpInCurrentLevel = currentXp % 1000
            return (xpInCurrentLevel.toFloat() / 1000f).coerceIn(0f, 1f)
        }


    val starCount: Int
        get() = currentLevel.coerceAtMost(5)
}