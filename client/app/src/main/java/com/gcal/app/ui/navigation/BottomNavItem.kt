package com.gcal.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import com.gcal.app.R

/**
 * BottomNavItem - Bottom Navigation Items
 */
enum class BottomNavItem(
    @StringRes val labelResId: Int,
    val icon: ImageVector,
    val route: String
) {
    RANGLISTE(
        labelResId = R.string.nav_rangliste,
        icon = Icons.Filled.EmojiEvents,
        route = NavRoute.RANGLISTE
    ),
    BERICHTE(
        labelResId = R.string.nav_berichte,
        icon = Icons.Default.BarChart,
        route = NavRoute.BERICHTE
    ),
    PROFILE(
        labelResId = R.string.nav_profil,
        icon = Icons.Default.Person,
        route = NavRoute.PROFILE
    )
}