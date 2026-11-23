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

import android.view.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbooknavigator.settings.RemoteAction
import dev.giste.roadbooknavigator.settings.RemoteKey
import dev.giste.roadbooknavigator.settings.RemoteKeys
import dev.giste.roadbooknavigator.settings.RemoteModel
import dev.giste.roadbooknavigator.settings.ScreenOrientation
import dev.giste.roadbooknavigator.settings.ThemeMode
import kotlin.math.abs
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
        remoteModel = RemoteModel.DND2,
        remoteKeys = RemoteKeys.DND2,
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
                    onRemoteModelChange = { calls += "remote $it" },
                    onAssignCustomKey = { action, key -> calls += "assign $action ${key.code}" },
                    onRoadbookStyleChange = { calls += "style $it" },
                    onDecreaseThreshold = { calls += "decrease" },
                    onIncreaseThreshold = { calls += "increase" },
                    onSaveErrorShown = { calls += "errorShown" },
                    onKeyMoveShown = { calls += "keyMoveShown" },
                )
            }
        }
    }

    /** The clickable control labeled [text], e.g. a segmented button or a stepper button. */
    private fun control(text: String) =
        composeRule.onNode(hasText(text) and hasClickAction()).performScrollTo()

    /** The text [text] in the row of [action] and its keys. */
    private fun inBinding(action: RemoteAction, text: String) = composeRule.onNode(
        hasText(text) and hasAnyAncestor(hasTestTag(SettingsTestTags.keyBinding(action))),
    )

    /** The button with the keys of [action], which only custom keys have. */
    private fun keyButton(action: RemoteAction) = composeRule.onNode(
        hasClickAction() and hasAnyAncestor(hasTestTag(SettingsTestTags.keyBinding(action))),
    )

    /** The defaults with the custom remote selected, which starts with DND2's keys. */
    private val custom = defaults.copy(remoteModel = RemoteModel.CUSTOM)

    /**
     * The "System" choice of the theme (index 0) or of the screen orientation (index 1), which
     * share their label. They are found in the order the screen shows them.
     */
    private fun systemChoice(index: Int) =
        composeRule.onAllNodes(hasText("System") and hasClickAction())[index].performScrollTo()

    @Test
    fun settingsContent_loaded_showsEverySetting() {
        show(defaults)

        composeRule.onNodeWithText("Settings").assertExists()
        composeRule.onNodeWithText("Theme").assertExists()
        composeRule.onNodeWithText("Screen orientation").assertExists()
        composeRule.onNodeWithText("Remote control").assertExists()
        composeRule.onNodeWithText("Keys").assertExists()
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
    fun settingsContent_selectedChoice_showsNoCheckMark() {
        show(defaults.copy(remoteModel = RemoteModel.TERRA_PIRATA))

        // A check mark would push the label off the button's center, towards the end.
        val button = control("Terra Pirata").getUnclippedBoundsInRoot()
        val label = composeRule.onNode(hasText("Terra Pirata"), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val buttonCenter = (button.left + button.right) / 2
        val labelCenter = (label.left + label.right) / 2
        assertTrue(
            "Label centered at $labelCenter, button at $buttonCenter",
            abs((labelCenter - buttonCenter).value) < 1f,
        )
    }

    @Test
    fun settingsContent_remoteModel_selectsTheCurrentOne() {
        show(defaults.copy(remoteModel = RemoteModel.TERRA_PIRATA))

        control("Terra Pirata").assertIsSelected()
        control("DND2").assertIsNotSelected()
        control("Custom").assertIsNotSelected()
    }

    @Test
    fun settingsContent_remoteModelPicked_reportsIt() {
        show(defaults)

        control("Custom").performClick()

        assertEquals(listOf("remote CUSTOM"), calls)
    }

    @Test
    fun settingsContent_dnd2_showsEachActionWithItsKey() {
        show(defaults)

        inBinding(RemoteAction.NEXT_WAYPOINT, "Next waypoint").assertExists()
        inBinding(RemoteAction.NEXT_WAYPOINT, "D-pad up").assertExists()
        inBinding(RemoteAction.PREVIOUS_WAYPOINT, "D-pad down").assertExists()
        inBinding(RemoteAction.INCREASE_PARTIAL, "D-pad right").assertExists()
        inBinding(RemoteAction.DECREASE_PARTIAL, "D-pad left").assertExists()
        inBinding(RemoteAction.RESET_PARTIAL, "F6").assertExists()
    }

    @Test
    fun settingsContent_terraPirata_showsEveryKeyOfAnAction() {
        show(
            defaults.copy(
                remoteModel = RemoteModel.TERRA_PIRATA,
                remoteKeys = RemoteKeys.TERRA_PIRATA,
            ),
        )

        inBinding(RemoteAction.NEXT_WAYPOINT, "Next track").assertExists()
        inBinding(RemoteAction.PREVIOUS_WAYPOINT, "Previous track").assertExists()
        inBinding(RemoteAction.INCREASE_PARTIAL, "Volume up").assertExists()
        inBinding(RemoteAction.DECREASE_PARTIAL, "Volume down").assertExists()
        inBinding(RemoteAction.RESET_PARTIAL, "Play/pause").assertExists()
        inBinding(RemoteAction.RESET_PARTIAL, "Play").assertExists()
    }

    @Test
    fun settingsContent_customKeys_namesAnyKeyAndActionsWithoutKeys() {
        val custom = RemoteKeys(
            mapOf(
                RemoteAction.NEXT_WAYPOINT to setOf(RemoteKey(KeyEvent.KEYCODE_F1)),
                RemoteAction.PREVIOUS_WAYPOINT to setOf(RemoteKey(KeyEvent.KEYCODE_BUTTON_A)),
                RemoteAction.INCREASE_PARTIAL to setOf(RemoteKey(KeyEvent.KEYCODE_ENTER)),
                RemoteAction.DECREASE_PARTIAL to setOf(RemoteKey(KeyEvent.KEYCODE_MEDIA_PAUSE)),
            ),
        )

        show(defaults.copy(remoteModel = RemoteModel.CUSTOM, remoteKeys = custom))

        inBinding(RemoteAction.NEXT_WAYPOINT, "F1").assertExists()
        // Android's own name, which Robolectric gives as the bare number.
        val buttonA = KeyEvent.keyCodeToString(KeyEvent.KEYCODE_BUTTON_A).removePrefix("KEYCODE_")
        inBinding(RemoteAction.PREVIOUS_WAYPOINT, "Key $buttonA").assertExists()
        inBinding(RemoteAction.INCREASE_PARTIAL, "Enter").assertExists()
        inBinding(RemoteAction.DECREASE_PARTIAL, "Pause").assertExists()
        inBinding(RemoteAction.RESET_PARTIAL, "No key").assertExists()
    }

    @Test
    fun settingsContent_preset_keysCannotBeChanged() {
        show(defaults)

        RemoteAction.entries.forEach { keyButton(it).assertDoesNotExist() }
        composeRule.onNodeWithText("Tap an action's key to change it.").assertDoesNotExist()
    }

    @Test
    fun settingsContent_custom_everyActionsKeysAreAButton() {
        show(custom)

        composeRule.onNodeWithText("Tap an action's key to change it.").assertExists()
        RemoteAction.entries.forEach { keyButton(it).performScrollTo().assertIsEnabled() }
    }

    @Test
    fun settingsContent_customKeyButton_opensTheCaptureDialog() {
        show(custom)

        keyButton(RemoteAction.NEXT_WAYPOINT).performScrollTo().performClick()

        composeRule.onNodeWithText("Press the key on your remote for this action.").assertExists()
        composeRule.onNodeWithText("Now: D-pad up").assertExists()
    }

    @Test
    fun settingsContent_keyPressedInTheDialog_assignsItAndCloses() {
        show(custom)
        keyButton(RemoteAction.RESET_PARTIAL).performScrollTo().performClick()

        composeRule.onNodeWithText("Now: F6").performKeyInput { pressKey(Key(KeyEvent.KEYCODE_F7)) }

        assertEquals(listOf("assign RESET_PARTIAL ${KeyEvent.KEYCODE_F7}"), calls)
        composeRule.onNodeWithText("Now: F6").assertDoesNotExist()
    }

    @Test
    fun settingsContent_dialogCanceled_assignsNothing() {
        show(custom)
        keyButton(RemoteAction.RESET_PARTIAL).performScrollTo().performClick()

        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(emptyList<String>(), calls)
        composeRule.onNodeWithText("Now: F6").assertDoesNotExist()
    }

    @Test
    fun settingsContent_dialogOpen_survivesRecreation() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            GisteTheme {
                SettingsContent(
                    state = custom,
                    onBack = {},
                    onThemeModeChange = {},
                    onOrientationChange = {},
                    onRemoteModelChange = {},
                    onAssignCustomKey = { _, _ -> },
                    onRoadbookStyleChange = {},
                    onDecreaseThreshold = {},
                    onIncreaseThreshold = {},
                    onSaveErrorShown = {},
                    onKeyMoveShown = {},
                )
            }
        }
        keyButton(RemoteAction.RESET_PARTIAL).performScrollTo().performClick()

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("Now: F6").assertExists()
    }

    @Test
    fun settingsContent_keyMoved_showsTheMoveAndReportsItShown() {
        val dpadUp = RemoteKey(KeyEvent.KEYCODE_DPAD_UP)
        show(custom.copy(keyMoved = KeyMove(dpadUp, RemoteAction.NEXT_WAYPOINT, RemoteAction.RESET_PARTIAL)))

        composeRule.onNodeWithText("D-pad up moved from “Next waypoint” to “Reset partial”.").assertExists()
        composeRule.mainClock.advanceTimeBy(SNACKBAR_SHORT_MILLIS)
        composeRule.waitForIdle()

        assertEquals(listOf("keyMoveShown"), calls)
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

        listOf("Dark", "Horizontal", "Terra Pirata", "FIA", "−", "+").forEach { label ->
            val height = control(label).getUnclippedBoundsInRoot().height
            assertTrue("$label is $height high", height >= 56.dp)
        }
        val back = composeRule.onNodeWithTag(SettingsTestTags.BACK).getUnclippedBoundsInRoot()
        assertTrue("Back is ${back.height} high", back.height >= 56.dp)
    }

    @Test
    fun settingsContent_customKeyButtons_areAtLeastTheMinimumTouchTarget() {
        show(custom)

        RemoteAction.entries.forEach { action ->
            val bounds = keyButton(action).performScrollTo().getUnclippedBoundsInRoot()
            assertTrue("$action keys are ${bounds.height} high", bounds.height >= 56.dp)
        }
    }

    @Test
    @Config(qualifiers = "es-w411dp-h891dp")
    fun settingsContent_spanishCustomKeys_useSpanishTexts() {
        val dpadUp = RemoteKey(KeyEvent.KEYCODE_DPAD_UP)
        show(custom.copy(keyMoved = KeyMove(dpadUp, RemoteAction.NEXT_WAYPOINT, RemoteAction.RESET_PARTIAL)))

        composeRule.onNodeWithText("Toca la tecla de una acción para cambiarla.").assertExists()
        composeRule.onNodeWithText("Cruceta arriba pasa de «Viñeta siguiente» a «Poner el parcial a cero».").assertExists()
        keyButton(RemoteAction.NEXT_WAYPOINT).performScrollTo().performClick()
        composeRule.onNodeWithText("Pulsa en el mando la tecla para esta acción.").assertExists()
        composeRule.onNodeWithText("Ahora: Cruceta arriba").assertExists()
        composeRule.onNodeWithText("Cancelar").assertExists()
    }

    @Test
    @Config(qualifiers = "es-w411dp-h891dp")
    fun settingsContent_spanish_usesSpanishTexts() {
        show(defaults)

        composeRule.onNodeWithText("Ajustes").assertExists()
        control("Oscuro").assertExists()
        composeRule.onNodeWithText("Orientación de la pantalla").assertExists()
        control("Personalizado").assertExists()
        inBinding(RemoteAction.NEXT_WAYPOINT, "Viñeta siguiente").assertExists()
        inBinding(RemoteAction.NEXT_WAYPOINT, "Cruceta arriba").assertExists()
        composeRule.onNodeWithText("Resaltar viñetas cercanas").assertExists()
    }

    private companion object {
        /** How long a short snackbar stays, plus a margin. */
        const val SNACKBAR_SHORT_MILLIS = 5_000L
    }
}
