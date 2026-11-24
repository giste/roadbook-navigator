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
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteListingFailure
import dev.giste.map.domain.RemoteMapEntry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en")
class MapDownloaderContentTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val europe = RemoteFolderPath.ROOT.child("europe")
    private val italy = RemoteMapEntry.Folder("italy")
    private val andorra = RemoteMapEntry.MapFile("andorra.map", FileSize(2_516_582), LocalDate.of(2026, 9, 7))
    private val albania = RemoteMapEntry.MapFile("albania.map", FileSize(45_088_768), LocalDate.of(2026, 8, 21))
    private val undated = RemoteMapEntry.MapFile("undated.map", null, null)
    private val listed = MapDownloaderUiState.Listed(europe, listOf(italy, albania, andorra), setOf("andorra.map"))

    private var backs = 0
    private var ups = 0
    private var retries = 0
    private val opened = mutableListOf<RemoteMapEntry.Folder>()

    /** What the downloader shows. Tests change it to update the screen. */
    private var state: MapDownloaderUiState by mutableStateOf(MapDownloaderUiState.Loading(RemoteFolderPath.ROOT))

    /** Shows the downloader in [initial], recording its callbacks. */
    private fun show(initial: MapDownloaderUiState) {
        state = initial
        rule.setContent {
            GisteTheme {
                MapDownloaderContent(
                    state = state,
                    onBack = { backs++ },
                    onOpen = { opened += it },
                    onUp = { ups++ },
                    onRetry = { retries++ },
                )
            }
        }
    }

    /** Matches a node with [text] in the row of [entry]. */
    private fun textInRow(entry: RemoteMapEntry, text: String): SemanticsMatcher =
        hasText(text) and hasAnyAncestor(hasTestTag(MapDownloaderTestTags.entry(entry)))

    /** Matches a node tagged [tag] in the row of [entry]. */
    private fun tagInRow(entry: RemoteMapEntry, tag: String): SemanticsMatcher =
        hasTestTag(tag) and hasAnyAncestor(hasTestTag(MapDownloaderTestTags.entry(entry)))

    /** The size of [bytes] as the screen formats it in the current locale. */
    private fun formatted(bytes: Long): String =
        Formatter.formatShortFileSize(RuntimeEnvironment.getApplication(), bytes)

    /** Presses the system back button. */
    private fun pressSystemBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test
    fun mapDownloaderContent_loading_showsTheTitleAndNoList() {
        show(MapDownloaderUiState.Loading(RemoteFolderPath.ROOT))

        rule.onNodeWithText("Download maps").assertIsDisplayed()
        rule.onNodeWithTag(MapDownloaderTestTags.LOADING).assertExists()
    }

    @Test
    fun mapDownloaderContent_root_showsNoFolderUnderTheTitle() {
        show(MapDownloaderUiState.Listed(RemoteFolderPath.ROOT, listOf(RemoteMapEntry.Folder("europe"))))

        rule.onNodeWithText("Download maps").assertIsDisplayed()
        // "europe" is only the folder row, not a sub-title.
        rule.onAllNodesWithTextCount("europe", 1)
    }

    @Test
    fun mapDownloaderContent_subFolder_showsItsPathUnderTheTitle() {
        show(MapDownloaderUiState.Loading(europe.child("italy")))

        rule.onNodeWithText("europe/italy").assertIsDisplayed()
    }

    @Test
    fun mapDownloaderContent_failures_showTheirMessagesAndRetry() {
        val messages = mapOf(
            RemoteListingFailure.NO_CONNECTION to "Cannot reach the map site. Check the connection.",
            RemoteListingFailure.SERVER_ERROR to "The map site answered with an error",
            RemoteListingFailure.UNREADABLE_LISTING to "The map site's list cannot be read",
        )
        show(MapDownloaderUiState.Loading(RemoteFolderPath.ROOT))

        messages.forEach { (reason, message) ->
            state = MapDownloaderUiState.Failed(RemoteFolderPath.ROOT, reason)
            rule.onNodeWithText(message).assertIsDisplayed()
            rule.onNodeWithTag(MapDownloaderTestTags.RETRY).assertIsDisplayed()
        }
    }

    @Test
    fun mapDownloaderContent_retryPressed_callsOnRetry() {
        show(MapDownloaderUiState.Failed(RemoteFolderPath.ROOT, RemoteListingFailure.NO_CONNECTION))

        rule.onNodeWithTag(MapDownloaderTestTags.RETRY).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun mapDownloaderContent_emptyFolder_showsTheMessage() {
        show(MapDownloaderUiState.Listed(europe, emptyList()))

        rule.onNodeWithText("No maps in this folder").assertIsDisplayed()
    }

    @Test
    fun mapDownloaderContent_folderRow_showsItsName() {
        show(listed)

        // The row is clickable, so it merges its text: look in the unmerged tree.
        rule.onNode(textInRow(italy, "italy"), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun mapDownloaderContent_folderRowPressed_opensIt() {
        show(listed)

        rule.onNodeWithTag(MapDownloaderTestTags.entry(italy)).performClick()

        assertEquals(listOf(italy), opened)
    }

    @Test
    fun mapDownloaderContent_mapRow_showsItsNameDayAndSize() {
        show(listed)

        rule.onNode(textInRow(albania, "albania.map")).assertIsDisplayed()
        rule.onNode(textInRow(albania, "Aug 21, 2026")).assertIsDisplayed()
        rule.onNode(textInRow(albania, formatted(45_088_768))).assertIsDisplayed()
    }

    @Test
    fun mapDownloaderContent_mapWithoutSizeOrDay_showsOnlyItsName() {
        show(MapDownloaderUiState.Listed(europe, listOf(undated)))

        rule.onNode(textInRow(undated, "undated.map")).assertIsDisplayed()
        rule.onNodeWithTag(MapDownloaderTestTags.entry(undated))
            .assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        rule.onAllNodesWithTextCount("undated.map", 1)
    }

    @Test
    fun mapDownloaderContent_installedMap_isMarkedAndOthersAreNot() {
        show(listed)

        rule.onNode(tagInRow(andorra, MapDownloaderTestTags.INSTALLED)).assertIsDisplayed()
        rule.onNode(tagInRow(albania, MapDownloaderTestTags.INSTALLED)).assertDoesNotExist()
    }

    @Test
    fun mapDownloaderContent_rowsAndButtons_areAtLeastTheMinimumTouchTarget() {
        show(listed)

        rule.onNodeWithTag(MapDownloaderTestTags.BACK)
            .assertWidthIsAtLeast(MIN_TOUCH_TARGET)
            .assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        rule.onNodeWithTag(MapDownloaderTestTags.entry(italy)).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        rule.onNodeWithTag(MapDownloaderTestTags.entry(andorra)).assertHeightIsAtLeast(MIN_TOUCH_TARGET)

        state = MapDownloaderUiState.Failed(europe, RemoteListingFailure.NO_CONNECTION)
        rule.onNodeWithTag(MapDownloaderTestTags.RETRY)
            .assertWidthIsAtLeast(MIN_TOUCH_TARGET)
            .assertHeightIsAtLeast(MIN_TOUCH_TARGET)
    }

    @Test
    fun mapDownloaderContent_backPressedInTheRoot_callsOnBack() {
        show(MapDownloaderUiState.Listed(RemoteFolderPath.ROOT, emptyList()))

        rule.onNodeWithTag(MapDownloaderTestTags.BACK).performClick()

        assertEquals(1, backs)
        assertEquals(0, ups)
    }

    @Test
    fun mapDownloaderContent_backPressedInASubFolder_goesUp() {
        show(listed)

        rule.onNodeWithTag(MapDownloaderTestTags.BACK).performClick()

        assertEquals(1, ups)
        assertEquals(0, backs)
    }

    @Test
    fun mapDownloaderContent_systemBackInASubFolder_goesUp() {
        show(listed)

        pressSystemBack()

        assertEquals(1, ups)
        assertEquals(0, backs)
    }

    @Test
    fun mapDownloaderContent_systemBackInTheRoot_isLeftToTheCaller() {
        show(MapDownloaderUiState.Listed(RemoteFolderPath.ROOT, emptyList()))

        pressSystemBack()

        assertEquals(0, ups)
        assertEquals(0, backs)
    }

    /** Checks that exactly [count] nodes show [text]. */
    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTextCount(text: String, count: Int) {
        assertEquals(count, onAllNodes(hasText(text)).fetchSemanticsNodes().size)
    }

    private companion object {
        /** `:core`'s minimum touch target, which the rows and buttons must reach. */
        val MIN_TOUCH_TARGET = 56.dp
    }
}
