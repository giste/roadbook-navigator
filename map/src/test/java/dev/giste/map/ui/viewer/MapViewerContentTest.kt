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

package dev.giste.map.ui.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.map.MapSettings
import dev.giste.map.MapZoomLevel
import dev.giste.map.domain.testMapFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
class MapViewerContentTest {

    @get:Rule
    val rule = createComposeRule()

    private val ready = MapUiState.Ready(
        files = listOf(testMapFile()),
        settings = MapSettings(),
        center = null,
    )

    /** The states the map slot was drawn with, in order. */
    private val drawnStates = mutableListOf<MapUiState.Ready>()

    /** Whether the map slot was asked for an interactive map, each time it was drawn, in order. */
    private val drawnInteractive = mutableListOf<Boolean>()

    /** What the viewer shows. Tests change it to update the viewer. */
    private var state: MapUiState by mutableStateOf(MapUiState.Loading)

    /** How many times the manage button was pressed. */
    private var manageCalls = 0

    /**
     * Shows the viewer in a 300 × 200 box, with a tagged placeholder for the map.
     *
     * @param interactive passed to the viewer, or `null` to leave its default.
     */
    private fun showViewer(initial: MapUiState, interactive: Boolean? = null) {
        state = initial
        val map: @Composable (MapUiState.Ready, Boolean, Modifier) -> Unit =
            { readyState, isInteractive, mapModifier ->
                drawnStates += readyState
                drawnInteractive += isInteractive
                Box(mapModifier.testTag(MAP_TAG))
            }
        val modifier = Modifier.size(width = 300.dp, height = 200.dp).testTag(VIEWER_TAG)
        rule.setContent {
            GisteTheme {
                if (interactive == null) {
                    MapViewerContent(state = state, modifier = modifier, map = map)
                } else {
                    MapViewerContent(state = state, modifier = modifier, interactive = interactive, map = map)
                }
            }
        }
    }

    /**
     * Shows the viewer in [initial] with a manage action, the manage button as [showManageButton]
     * says, and a placeholder for the map.
     */
    private fun showManageable(initial: MapUiState, showManageButton: Boolean) {
        state = initial
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            GisteTheme {
                MapViewerContent(
                    state = state,
                    modifier = Modifier.size(width = 300.dp, height = 200.dp),
                    showManageButton = showManageButton,
                    onManageMaps = { manageCalls++ },
                    map = { _, _, mapModifier -> Box(mapModifier.testTag(MAP_TAG)) },
                )
            }
        }
    }

    /** The input mode of the shown viewer, set by [showManageable]. */
    private lateinit var inputModeManager: InputModeManager

    @Test
    fun mapViewerContent_byDefault_drawsAReadOnlyMap() {
        showViewer(ready)

        assertEquals(false, drawnInteractive.last())
    }

    @Test
    fun mapViewerContent_interactive_drawsAnInteractiveMap() {
        showViewer(ready, interactive = true)

        assertEquals(true, drawnInteractive.last())
    }

    @Test
    fun mapViewerContent_loading_showsAnEmptyAreaAndNoMap() {
        showViewer(MapUiState.Loading)

        rule.onNodeWithTag(MapViewerTestTags.LOADING)
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(200.dp)
        rule.onNodeWithTag(MAP_TAG).assertDoesNotExist()
    }

    @Test
    fun mapViewerContent_noMaps_showsTheMessage() {
        showViewer(MapUiState.NoMaps)

        rule.onNodeWithText("No maps installed").assertIsDisplayed()
        rule.onNodeWithTag(MAP_TAG).assertDoesNotExist()
    }

    @Test
    fun mapViewerContent_unreadableMaps_showsTheMessage() {
        showViewer(MapUiState.UnreadableMaps)

        rule.onNodeWithText("The installed maps cannot be read").assertIsDisplayed()
        rule.onNodeWithTag(MAP_TAG).assertDoesNotExist()
    }

    @Test
    fun mapViewerContent_ready_drawsTheMapOnTheWholeAreaWithTheAttribution() {
        showViewer(ready)

        rule.onNodeWithTag(MAP_TAG).assertWidthIsEqualTo(300.dp).assertHeightIsEqualTo(200.dp)
        rule.onNodeWithText("© OpenStreetMap").assertIsDisplayed()
        assertEquals(ready, drawnStates.last())
    }

    @Test
    @Config(qualifiers = "es")
    fun mapViewerContent_spanish_keepsTheAttributionUntranslated() {
        showViewer(ready)

        rule.onNodeWithText("© OpenStreetMap").assertIsDisplayed()
    }

    @Test
    fun mapViewerContent_newState_drawsTheMapWithIt() {
        showViewer(ready)
        val moved = ready.copy(settings = MapSettings(zoomLevel = MapZoomLevel.MIN))

        state = moved
        rule.waitForIdle()

        assertEquals(moved, drawnStates.last())
    }

    @Test
    fun mapViewerContent_eachState_fillsTheGivenSize() {
        listOf(MapUiState.Loading, MapUiState.NoMaps, MapUiState.UnreadableMaps, ready)
            .forEachIndexed { index, viewerState ->
                if (index == 0) showViewer(viewerState) else state = viewerState
                rule.waitForIdle()

                rule.onNodeWithTag(VIEWER_TAG).assertWidthIsEqualTo(300.dp).assertHeightIsEqualTo(200.dp)
            }
    }

    @Test
    fun mapViewerContent_withoutManageAction_neverShowsTheButton() {
        listOf(MapUiState.Loading, MapUiState.NoMaps, MapUiState.UnreadableMaps, ready)
            .forEachIndexed { index, viewerState ->
                if (index == 0) showViewer(viewerState) else state = viewerState
                rule.waitForIdle()

                rule.onNodeWithTag(MapViewerTestTags.MANAGE).assertDoesNotExist()
            }
    }

    @Test
    fun mapViewerContent_readyAndShowManageButton_showsItInTheBottomEndCorner() {
        showManageable(ready, showManageButton = true)

        val button = rule.onNodeWithTag(MapViewerTestTags.MANAGE)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(56.dp)
            .assertHeightIsEqualTo(56.dp)
            .getUnclippedBoundsInRoot()
        // Material's 16 dp FAB margin from the bottom and end edges of the 300 × 200 dp viewer.
        assertEquals(184.dp, button.bottom)
        assertEquals(284.dp, button.right)
    }

    @Test
    fun mapViewerContent_ready_showsTheAttributionInTheBottomStartCornerClearOfTheButton() {
        showManageable(ready, showManageButton = true)

        val attribution = rule.onNodeWithText("© OpenStreetMap").getUnclippedBoundsInRoot()
        val button = rule.onNodeWithTag(MapViewerTestTags.MANAGE).getUnclippedBoundsInRoot()

        // The text's own bounds: its background reaches the edge, 4 dp of padding before it.
        assertEquals(4.dp, attribution.left)
        assertEquals(200.dp, attribution.bottom)
        assertTrue("The attribution reaches the button", attribution.right < button.left)
    }

    @Test
    fun mapViewerContent_readyWithoutShowManageButton_hidesIt() {
        showManageable(ready, showManageButton = false)

        rule.onNodeWithTag(MapViewerTestTags.MANAGE).assertDoesNotExist()
    }

    @Test
    fun mapViewerContent_loadingWithoutShowManageButton_hidesIt() {
        showManageable(MapUiState.Loading, showManageButton = false)

        rule.onNodeWithTag(MapViewerTestTags.MANAGE).assertDoesNotExist()
    }

    @Test
    fun mapViewerContent_noMapsWithoutShowManageButton_showsItAnyway() {
        showManageable(MapUiState.NoMaps, showManageButton = false)

        rule.onNodeWithTag(MapViewerTestTags.MANAGE).assertIsDisplayed()
    }

    @Test
    fun mapViewerContent_unreadableMapsWithoutShowManageButton_showsItAnyway() {
        showManageable(MapUiState.UnreadableMaps, showManageButton = false)

        rule.onNodeWithTag(MapViewerTestTags.MANAGE).assertIsDisplayed()
    }

    @Test
    fun mapViewerContent_managePressed_callsOnManageMaps() {
        showManageable(ready, showManageButton = true)

        rule.onNodeWithTag(MapViewerTestTags.MANAGE).performClick()

        assertEquals(1, manageCalls)
    }

    @Test
    fun mapViewerContent_keyModeFocusRequestedOnTheManageButton_buttonDoesNotTakeIt() {
        showManageable(ready, showManageButton = true)
        // As after a key of a remote: in touch mode no button takes the focus anyway.
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        val button = rule.onNodeWithTag(MapViewerTestTags.MANAGE)

        // Where an unbound arrow or Tab key would move the focus.
        button.requestFocus()

        button.assert(isFocused().not())
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun mapViewerContent_manageButton_isARoundedSquareInThePrimaryColor() {
        var primary = Color.Unspecified
        rule.setContent {
            GisteTheme {
                primary = MaterialTheme.colorScheme.primary
                MapViewerContent(
                    state = ready,
                    modifier = Modifier.size(width = 300.dp, height = 200.dp),
                    showManageButton = true,
                    onManageMaps = {},
                    map = { _, _, mapModifier -> Box(mapModifier) },
                )
            }
        }

        val pixels = rule.onNodeWithTag(MapViewerTestTags.MANAGE).captureToImage().toPixelMap()
        // 8 dp in from the top start corner of the 56 dp button: outside a circle, which only
        // reaches there at 8.2 dp, but inside the rounded corner of a floating action button.
        val inset = with(rule.density) { 8.dp.roundToPx() }

        assertEquals(primary, pixels[inset, inset])
    }

    private companion object {
        const val VIEWER_TAG = "viewer"
        const val MAP_TAG = "map"
    }
}
