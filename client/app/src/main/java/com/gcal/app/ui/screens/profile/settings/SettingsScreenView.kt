package com.gcal.app.ui.screens.profile.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gcal.app.R
import com.gcal.app.ui.theme.LocalAppSettings
import com.gcal.app.ui.theme.LocaleHelper
import com.gcal.app.ui.view_model.profileViewModel.AppLanguage
import com.gcal.app.ui.view_model.profileViewModel.ProfileUiEvent
import com.gcal.app.ui.view_model.profileViewModel.ProfileViewModel
import android.content.Context
import com.gcal.app.ui.PREFS_NAME
import com.gcal.app.ui.KEY_DARK_MODE

/**
 * Settings sub-screen providing Dark Mode toggle and language selection.
 *
 * All user-visible text is resolved through [stringResource] so that switching
 * the app locale via the language selector correctly updates every label on
 * this screen. The Activity is recreated by [LocaleHelper.setLocale] after a
 * language change, which causes Compose to re-read all string resources from
 * the new locale's values directory.
 *
 * @param viewModel Shared [ProfileViewModel] instance obtained from the Profile
 *                  back-stack entry so that state survives navigation between
 *                  Profile and Settings.
 * @param onBackClicked Navigates back to the Profile screen.
 * @param modifier Optional [Modifier] for the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenView(
    viewModel: ProfileViewModel,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Read the global app settings from the CompositionLocal provided by GCalTheme.
    val appSettings = LocalAppSettings.current

    // Synchronize the ViewModel with the current theme/language state on first
    // composition so that the ProfileUiState reflects the actual device settings
    LaunchedEffect(Unit) {
        viewModel.onEvent(ProfileUiEvent.SelectLanguage(appSettings.language))
        viewModel.onEvent(ProfileUiEvent.ToggleDarkMode(appSettings.isDarkMode))
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Dark Mode toggle — reads title and description from string resources
            // so that both labels translate when the user switches languages.
            SettingsSwitchItem(
                icon = Icons.Filled.DarkMode,
                title = stringResource(R.string.settings_dark_mode),
                description = stringResource(R.string.settings_dark_mode_desc),
                checked = appSettings.isDarkMode,
                onCheckedChange = { enabled ->


                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_DARK_MODE, enabled)
                        .apply()



                    appSettings.toggleDarkMode(enabled)
                    viewModel.onEvent(ProfileUiEvent.ToggleDarkMode(enabled))
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Language selector — the SegmentedButton labels ("Deutsch", "English")
            // are intentionally kept as fixed display names from the AppLanguage enum
            // so users can always identify their language regardless of the current locale.
            SettingsLanguageItem(
                selectedLanguage = appSettings.language,
                onLanguageSelected = { language ->
                    appSettings.setLanguage(language)
                    viewModel.onEvent(ProfileUiEvent.SelectLanguage(language))


                    LocaleHelper.setLocale(context, language)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))


        }
    }
}

/**
 * Reusable settings row displaying an icon, title, description, and a [Switch].
 *
 * @param icon Leading icon rendered at the primary color.
 * @param title Primary label for the setting.
 * @param description Secondary descriptive text below the title.
 * @param checked Current toggle state.
 * @param onCheckedChange Callback when the user flips the switch.
 * @param modifier Optional [Modifier] for the row container.
 */
@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Language selection row with a [SingleChoiceSegmentedButtonRow] offering
 * German and English options.
 *
 * The button labels use [AppLanguage.displayName] (fixed strings "Deutsch" /
 * "English") rather than localized resources so that the user can always
 * identify the target language even when the current locale differs.
 *
 * @param selectedLanguage The currently active [AppLanguage].
 * @param onLanguageSelected Callback when the user taps a different language segment.
 * @param modifier Optional [Modifier] for the row container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsLanguageItem(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Language,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = selectedLanguage == AppLanguage.GERMAN,
                    onClick = { onLanguageSelected(AppLanguage.GERMAN) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text(AppLanguage.GERMAN.displayName)
                }
                SegmentedButton(
                    selected = selectedLanguage == AppLanguage.ENGLISH,
                    onClick = { onLanguageSelected(AppLanguage.ENGLISH) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text(AppLanguage.ENGLISH.displayName)
                }
            }
        }
    }
}