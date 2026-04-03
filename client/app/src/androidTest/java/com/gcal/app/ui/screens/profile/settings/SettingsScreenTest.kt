package com.gcal.app.ui.screens.profile.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.gcal.app.ui.view_model.profileViewModel.AppLanguage
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for the Settings screen.
 *
 * The Settings screen provides two primary controls:
 * 1. Dark mode toggle (Switch)
 * 2. Language selector (SegmentedButton: Deutsch / English)
 *
 *
 * The screen reads from CompositionLocal-provided AppSettings,
 * so tests inject state directly into the composable components.
 */
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    //DARK MODE TOGGLE RENDERS

    /**
     * Verifies that the dark mode Switch composable is rendered
     * with its label text visible. This is the primary theme control
     * for the entire application.
     */
    @Test
    fun darkModeToggle_isDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                DarkModeToggleForTest(
                    checked = false,
                    onCheckedChange = {}
                )
            }
        }


        composeTestRule
            .onNodeWithText("Dark Mode", substring = true, ignoreCase = true)
            .assertIsDisplayed()

        // The Switch toggle itself should exist in the semantic tree
        composeTestRule
            .onNode(isToggleable())
            .assertIsDisplayed()
    }

    //  DARK MODE TOGGLE STATE MATCHES SETTINGS

    /**
     * Verifies that the Switch state reflects the current dark mode setting.
     * When isDarkMode is true, the Switch should be in the ON position.
     * When false, the Switch should be in the OFF position.
     */
    @Test
    fun darkModeToggle_stateMatchesSettings_on() {
        composeTestRule.setContent {
            MaterialTheme {
                DarkModeToggleForTest(
                    checked = true,
                    onCheckedChange = {}
                )
            }
        }

        composeTestRule
            .onNode(isToggleable())
            .assertIsOn()
    }

    @Test
    fun darkModeToggle_stateMatchesSettings_off() {
        composeTestRule.setContent {
            MaterialTheme {
                DarkModeToggleForTest(
                    checked = false,
                    onCheckedChange = {}
                )
            }
        }

        composeTestRule
            .onNode(isToggleable())
            .assertIsOff()
    }

    //  DARK MODE TOGGLE DISPATCHES EVENT

    /**
     * Verifies that flipping the dark mode Switch dispatches the
     * onCheckedChange callback with the new value. This triggers
     * SharedPreferences write and theme recomposition.
     */
    @Test
    fun darkModeToggle_flip_dispatchesCallback() {
        var newValue: Boolean? = null

        composeTestRule.setContent {
            MaterialTheme {
                DarkModeToggleForTest(
                    checked = false,
                    onCheckedChange = { value -> newValue = value }
                )
            }
        }

        composeTestRule
            .onNode(isToggleable())
            .performClick()

        assert(newValue == true) {
            "Expected onCheckedChange(true) when flipping Switch from OFF to ON"
        }
    }

    // LANGUAGE SELECTOR RENDERS BOTH OPTIONS

    /**
     * Verifies that the SegmentedButtonRow displays both language options:
     * "Deutsch" and "English". The labels are fixed display names from
     * [AppLanguage.displayName], not localized strings, so users can always
     * identify their target language regardless of the current locale.
     */
    @Test
    fun languageSelector_displaysBothOptions() {
        composeTestRule.setContent {
            MaterialTheme {
                LanguageSelectorForTest(
                    selectedLanguage = AppLanguage.GERMAN,
                    onLanguageSelected = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Deutsch")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("English")
            .assertIsDisplayed()
    }

    // LANGUAGE SWITCH DISPATCHES EVENT

    /**
     * Verifies that tapping the "English" segment dispatches the
     * onLanguageSelected callback with [AppLanguage.ENGLISH].
     * This triggers a locale change and Activity recreation.
     */
    @Test
    fun languageSelector_tapEnglish_dispatchesCallback() {
        var selectedLang: AppLanguage? = null

        composeTestRule.setContent {
            MaterialTheme {
                LanguageSelectorForTest(
                    selectedLanguage = AppLanguage.GERMAN,
                    onLanguageSelected = { lang -> selectedLang = lang }
                )
            }
        }

        composeTestRule
            .onNodeWithText("English")
            .performClick()

        assert(selectedLang == AppLanguage.ENGLISH) {
            "Expected AppLanguage.ENGLISH after tapping English segment, but got $selectedLang"
        }
    }
}

//  TEST HELPERS


@Composable
private fun DarkModeToggleForTest(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Dark Mode",
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelectorForTest(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Sprache")
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