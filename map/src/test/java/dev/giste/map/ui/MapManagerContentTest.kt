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

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.map.domain.InstalledMapFile
import dev.giste.map.domain.testInstalledFile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class MapManagerContentTest {

    @get:Rule
    val rule = createComposeRule()

    private val spain = testInstalledFile("spain.map", size = 851_443_712)
    private val damaged = testInstalledFile("damaged.map", readable = false, size = 1_024)
    private val corsica = testInstalledFile("corsica.map")
        .copy(path = "/maps/europe/france/corsica.map", folder = "europe/france")
    private val listed = MapManagerUiState.Listed(listOf(damaged, spain))

    private var backs = 0
    private val deleted = mutableListOf<InstalledMapFile>()
    private var errorsShown = 0

    /** What the manager shows. Tests change it to update the screen. */
    private var state: MapManagerUiState by mutableStateOf(MapManagerUiState.Loading)

    /** Shows the manager in [initial], recording its callbacks. */
    private fun show(initial: MapManagerUiState, restorationTester: StateRestorationTester? = null) {
        state = initial
        val content: @Composable () -> Unit = {
            GisteTheme {
                MapManagerContent(
                    state = state,
                    onBack = { backs++ },
                    onDelete = { deleted += it },
                    onDeleteErrorShown = { errorsShown++ },
                )
            }
        }
        if (restorationTester == null) rule.setContent(content) else restorationTester.setContent(content)
    }

    /** Matches a node tagged [tag] in the row of [file]. */
    private fun inRow(file: InstalledMapFile, tag: String): SemanticsMatcher =
        hasTestTag(tag) and hasAnyAncestor(hasTestTag(MapManagerTestTags.file(file)))

    /** The size of [bytes] as the screen formats it in the current locale. */
    private fun formatted(bytes: Long): String =
        Formatter.formatShortFileSize(RuntimeEnvironment.getApplication(), bytes)

    @Test
    fun mapManagerContent_loading_showsTheTitleAndNoList() {
        show(MapManagerUiState.Loading)

        rule.onNodeWithText("Maps").assertIsDisplayed()
        rule.onNodeWithTag(MapManagerTestTags.LOADING).assertExists()
        rule.onNodeWithText("No maps installed").assertDoesNotExist()
    }

    @Test
    fun mapManagerContent_folderUnreadable_showsTheMessage() {
        show(MapManagerUiState.FolderUnreadable)

        rule.onNodeWithText("The map folder cannot be read").assertIsDisplayed()
    }

    @Test
    fun mapManagerContent_noFiles_showsNoMaps() {
        show(MapManagerUiState.Listed(emptyList()))

        rule.onNodeWithText("No maps installed").assertIsDisplayed()
    }

    @Test
    fun mapManagerContent_files_showsTheirNamesAndSizes() {
        show(listed)

        rule.onNode(hasText("spain.map") and hasAnyAncestor(hasTestTag(MapManagerTestTags.file(spain))))
            .assertIsDisplayed()
        rule.onNode(hasText(formatted(851_443_712)) and hasAnyAncestor(hasTestTag(MapManagerTestTags.file(spain))))
            .assertIsDisplayed()
        rule.onNode(hasText("damaged.map") and hasAnyAncestor(hasTestTag(MapManagerTestTags.file(damaged))))
            .assertIsDisplayed()
        rule.onNode(hasText(formatted(1_024)) and hasAnyAncestor(hasTestTag(MapManagerTestTags.file(damaged))))
            .assertIsDisplayed()
    }

    @Test
    fun mapManagerContent_fileInASubFolder_showsTheFolder() {
        show(MapManagerUiState.Listed(listOf(corsica, spain)))

        rule.onNode(hasText("europe/france") and hasAnyAncestor(hasTestTag(MapManagerTestTags.file(corsica))))
            .assertIsDisplayed()
    }

    @Test
    fun mapManagerContent_unreadableFile_isMarkedAndReadableIsNot() {
        show(listed)

        rule.onNode(inRow(damaged, MapManagerTestTags.UNREADABLE)).assertIsDisplayed()
        rule.onNode(inRow(spain, MapManagerTestTags.UNREADABLE)).assertDoesNotExist()
    }

    @Test
    fun mapManagerContent_buttons_areAtLeastTheMinimumTouchTarget() {
        show(listed)

        rule.onNodeWithTag(MapManagerTestTags.BACK)
            .assertWidthIsAtLeast(MIN_TOUCH_TARGET)
            .assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        rule.onNode(inRow(spain, MapManagerTestTags.DELETE))
            .assertWidthIsAtLeast(MIN_TOUCH_TARGET)
            .assertHeightIsAtLeast(MIN_TOUCH_TARGET)
    }

    @Test
    fun mapManagerContent_backPressed_callsOnBack() {
        show(listed)

        rule.onNodeWithTag(MapManagerTestTags.BACK).performClick()

        assertEquals(1, backs)
    }

    @Test
    fun mapManagerContent_deletePressed_asksFirstWithoutDeleting() {
        show(listed)

        rule.onNode(inRow(spain, MapManagerTestTags.DELETE)).performClick()

        rule.onNode(hasText("Delete spain.map?") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        assertEquals(emptyList<InstalledMapFile>(), deleted)
    }

    @Test
    fun mapManagerContent_deleteConfirmed_deletesThatFileAndCloses() {
        show(listed)
        rule.onNode(inRow(spain, MapManagerTestTags.DELETE)).performClick()

        rule.onNodeWithTag(MapManagerTestTags.CONFIRM_DELETE).performClick()

        assertEquals(listOf(spain), deleted)
        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun mapManagerContent_deleteCancelled_closesWithoutDeleting() {
        show(listed)
        rule.onNode(inRow(spain, MapManagerTestTags.DELETE)).performClick()

        rule.onNodeWithTag(MapManagerTestTags.CANCEL_DELETE).performClick()

        assertEquals(emptyList<InstalledMapFile>(), deleted)
        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun mapManagerContent_confirmationOpen_survivesRecreation() {
        val restorationTester = StateRestorationTester(rule)
        show(listed, restorationTester)
        rule.onNode(inRow(damaged, MapManagerTestTags.DELETE)).performClick()

        restorationTester.emulateSavedInstanceStateRestore()

        rule.onNode(hasText("Delete damaged.map?") and hasAnyAncestor(isDialog())).assertIsDisplayed()
    }

    @Test
    fun mapManagerContent_fileGoneWhileConfirming_closesTheDialog() {
        show(listed)
        rule.onNode(inRow(spain, MapManagerTestTags.DELETE)).performClick()

        state = MapManagerUiState.Listed(listOf(damaged))
        rule.waitForIdle()

        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun mapManagerContent_deleteFailed_saysSoAndReportsItShown() {
        show(listed.copy(deleteFailed = spain))

        rule.onNodeWithText("spain.map could not be deleted").assertIsDisplayed()
        // The snackbar goes away on its own, then the screen reports it shown.
        rule.mainClock.advanceTimeBy(SNACKBAR_SHORT_MILLIS)
        rule.waitForIdle()
        assertEquals(1, errorsShown)
    }

    @Test
    @Config(qualifiers = "es")
    fun mapManagerContent_spanish_isTranslated() {
        show(listed)
        rule.onNode(inRow(spain, MapManagerTestTags.DELETE)).performClick()

        rule.onNodeWithText("Mapas").assertIsDisplayed()
        rule.onNode(hasText("¿Borrar spain.map?") and hasAnyAncestor(isDialog())).assertIsDisplayed()
    }

    private companion object {
        val MIN_TOUCH_TARGET = 56.dp

        /** Longer than a short snackbar is shown. */
        const val SNACKBAR_SHORT_MILLIS = 10_000L
    }
}
