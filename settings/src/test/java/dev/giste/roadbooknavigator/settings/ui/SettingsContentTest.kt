/*
 * Copyright (C) 2026  Giste
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.giste.roadbooknavigator.settings.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbooknavigator.settings.ScreenOrientation
import dev.giste.roadbooknavigator.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-w411dp-h891dp")
class SettingsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private val defaults = SettingsUiState.Loaded(
        themeMode = ThemeMode.SYSTEM,
        orientation = ScreenOrientation.FOLLOW_SYSTEM,
        roadbookStyle = RoadbookStyle.APP_THEME,
        shortDistanceThreshold = ShortDistanceThreshold.DEFAULT,
        canDecreaseThreshold = true,
        canIncreaseThreshold = true,
        saveFailed = false,
    )

    private fun show(state: SettingsUiState) {
        composeRule.setContent {
            GisteTheme {
                SettingsContent(
                    state = state,
                    onBack = { calls += "back" },
                    onThemeModeChange = { calls += "theme $it" },
                    onOrientationChange = { calls += "orientation $it" },
                    onRoadbookStyleChange = { calls += "style $it" },
                    onDecreaseThreshold = { calls += "decrease" },
                    onIncreaseThreshold = { calls += "increase" },
                    onSaveErrorShown = { calls += "errorShown" },
                )
            }
        }
    }

    /** The clickable control labeled [text], e.g. a segmented button or a stepper button. */
    private fun control(text: String) = composeRule.onNode(hasText(text) and hasClickAction())

    /**
     * The "System" choice of the theme (index 0) or of the screen orientation (index 1), which
     * share their label. They are found in the order the screen shows them.
     */
    private fun systemChoice(index: Int) =
        composeRule.onAllNodes(hasText("System") and hasClickAction())[index]

    @Test
    fun settingsContent_loaded_showsEverySetting() {
        show(defaults)

        composeRule.onNodeWithText("Settings").assertExists()
        composeRule.onNodeWithText("Theme").assertExists()
        composeRule.onNodeWithText("Screen orientation").assertExists()
        composeRule.onNodeWithText("Roadbook colors").assertExists()
        composeRule.onNodeWithText("Highlight close waypoints").assertExists()
        composeRule.onNodeWithText("250 m").assertExists()
    }

    @Test
    fun settingsContent_loaded_selectsTheCurrentChoices() {
        show(
            defaults.copy(
                themeMode = ThemeMode.DARK,
                orientation = ScreenOrientation.HORIZONTAL,
                roadbookStyle = RoadbookStyle.FIA,
            ),
        )

        control("Dark").assertIsSelected()
        systemChoice(0).assertIsNotSelected()
        control("Horizontal").assertIsSelected()
        control("Vertical").assertIsNotSelected()
        systemChoice(1).assertIsNotSelected()
        control("FIA").assertIsSelected()
        control("App theme").assertIsNotSelected()
    }

    @Test
    fun settingsContent_loading_showsOnlyTheTopBar() {
        show(SettingsUiState.Loading)

        composeRule.onNodeWithText("Settings").assertExists()
        composeRule.onNodeWithText("Theme").assertDoesNotExist()
    }

    @Test
    fun settingsContent_themeModePicked_reportsIt() {
        show(defaults)

        control("Light").performClick()

        assertEquals(listOf("theme LIGHT"), calls)
    }

    @Test
    fun settingsContent_orientationPicked_reportsIt() {
        show(defaults.copy(orientation = ScreenOrientation.VERTICAL))

        control("Horizontal").performClick()
        systemChoice(1).performClick()

        assertEquals(
            listOf("orientation HORIZONTAL", "orientation FOLLOW_SYSTEM"),
            calls,
        )
    }

    @Test
    fun settingsContent_roadbookStylePicked_reportsIt() {
        show(defaults)

        control("FIA").performClick()

        assertEquals(listOf("style FIA"), calls)
    }

    @Test
    fun settingsContent_stepperPressed_reportsIt() {
        show(defaults)

        control("−").performClick()
        control("+").performClick()

        assertEquals(listOf("decrease", "increase"), calls)
    }

    @Test
    fun settingsContent_shortestThreshold_disablesDecrease() {
        show(
            defaults.copy(
                shortDistanceThreshold = ShortDistanceThreshold.of(Distance.meters(50.0)),
                canDecreaseThreshold = false,
            ),
        )

        composeRule.onNodeWithText("50 m").assertExists()
        control("−").assertIsNotEnabled()
        control("+").assertIsEnabled()
    }

    @Test
    fun settingsContent_longestThreshold_disablesIncrease() {
        show(defaults.copy(canIncreaseThreshold = false))

        control("−").assertIsEnabled()
        control("+").assertIsNotEnabled()
    }

    @Test
    fun settingsContent_backPressed_reportsIt() {
        show(defaults)

        composeRule.onNodeWithTag(SettingsTestTags.BACK).performClick()

        assertEquals(listOf("back"), calls)
    }

    @Test
    fun settingsContent_saveFailed_showsTheErrorAndReportsItShown() {
        show(defaults.copy(saveFailed = true))

        composeRule.onNodeWithText("The setting couldn't be saved. Try again.").assertExists()
        composeRule.mainClock.advanceTimeBy(SNACKBAR_SHORT_MILLIS)
        composeRule.waitForIdle()

        assertEquals(listOf("errorShown"), calls)
    }

    @Test
    fun settingsContent_controls_areAtLeastTheMinimumTouchTarget() {
        show(defaults)

        listOf("Dark", "Horizontal", "FIA", "−", "+").forEach { label ->
            val height = control(label).getUnclippedBoundsInRoot().height
            assertTrue("$label is $height high", height >= 56.dp)
        }
        val back = composeRule.onNodeWithTag(SettingsTestTags.BACK).getUnclippedBoundsInRoot()
        assertTrue("Back is ${back.height} high", back.height >= 56.dp)
    }

    @Test
    @Config(qualifiers = "es-w411dp-h891dp")
    fun settingsContent_spanish_usesSpanishTexts() {
        show(defaults)

        composeRule.onNodeWithText("Ajustes").assertExists()
        control("Oscuro").assertExists()
        composeRule.onNodeWithText("Orientación de la pantalla").assertExists()
        composeRule.onNodeWithText("Resaltar viñetas cercanas").assertExists()
    }

    private companion object {
        /** How long a short snackbar stays, plus a margin. */
        const val SNACKBAR_SHORT_MILLIS = 5_000L
    }
}
