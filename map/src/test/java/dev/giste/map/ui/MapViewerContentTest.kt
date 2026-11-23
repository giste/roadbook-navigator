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

package dev.giste.map.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.map.MapSettings
import dev.giste.map.MapZoomLevel
import dev.giste.map.domain.testMapFile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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

    /** What the viewer shows. Tests change it to update the viewer. */
    private var state: MapUiState by mutableStateOf(MapUiState.Loading)

    /** Shows the viewer in a 300 × 200 box, with a tagged placeholder for the map. */
    private fun showViewer(initial: MapUiState) {
        state = initial
        rule.setContent {
            GisteTheme {
                MapViewerContent(
                    state = state,
                    modifier = Modifier.size(width = 300.dp, height = 200.dp).testTag(VIEWER_TAG),
                    map = { readyState, mapModifier ->
                        drawnStates += readyState
                        Box(mapModifier.testTag(MAP_TAG))
                    },
                )
            }
        }
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

    private companion object {
        const val VIEWER_TAG = "viewer"
        const val MAP_TAG = "map"
    }
}
