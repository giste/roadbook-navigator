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
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.unit.dp
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.MapDownload
import dev.giste.map.domain.MapDownloadFailure
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteListingFailure
import dev.giste.map.domain.RemoteMapEntry
import dev.giste.map.domain.RemoteMapPath
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    private val downloaded = mutableListOf<RemoteMapEntry.MapFile>()
    private val cancelled = mutableListOf<RemoteMapEntry.MapFile>()
    private val mobileDataAnswers = mutableListOf<Boolean>()
    private var mobileDataDismissals = 0

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
                    onDownload = { downloaded += it },
                    onCancel = { cancelled += it },
                    onMobileData = { mobileDataAnswers += it },
                    onDismissMobileData = { mobileDataDismissals++ },
                )
            }
        }
    }

    /** [listed] with [download] for Albania, which is not installed. */
    private fun albaniaDownload(download: MapDownload): MapDownloaderUiState.Listed =
        listed.copy(downloads = mapOf("albania.map" to download))

    /** Albania's path on the site. */
    private val albaniaPath = RemoteMapPath(europe, "albania.map")

    /** Matches a node with [text] in the row of [entry]. */
    private fun textInRow(entry: RemoteMapEntry, text: String): SemanticsMatcher =
        hasText(text) and hasAnyAncestor(hasTestTag(MapDownloaderTestTags.entry(entry)))

    /**
     * Matches a node tagged [tag] in the row of [entry]. The waiting mark and the progress are
     * inside the button that stops the download, which merges them: find those in the unmerged tree.
     */
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
    fun mapDownloaderContent_mapNotInstalled_hasADownloadButtonAndInstalledHasNone() {
        show(listed)

        rule.onNode(tagInRow(albania, MapDownloaderTestTags.DOWNLOAD)).assertIsDisplayed()
        rule.onNode(tagInRow(andorra, MapDownloaderTestTags.DOWNLOAD)).assertDoesNotExist()
    }

    @Test
    fun mapDownloaderContent_downloadPressed_callsOnDownloadWithTheMap() {
        show(listed)

        rule.onNode(tagInRow(albania, MapDownloaderTestTags.DOWNLOAD)).performClick()

        assertEquals(listOf(albania), downloaded)
    }

    @Test
    fun mapDownloaderContent_queuedMap_isMarkedWithoutButton() {
        show(albaniaDownload(MapDownload.Queued(albaniaPath)))

        rule.onNode(tagInRow(albania, MapDownloaderTestTags.QUEUED), useUnmergedTree = true).assertIsDisplayed()
        rule.onNode(tagInRow(albania, MapDownloaderTestTags.DOWNLOAD)).assertDoesNotExist()
    }

    @Test
    fun mapDownloaderContent_runningMapOfKnownSize_showsItsProgress() {
        show(albaniaDownload(MapDownload.Running(albaniaPath, FileSize(25), FileSize(100))))

        rule.onNode(
            tagInRow(albania, MapDownloaderTestTags.PROGRESS) and
                hasProgressBarRangeInfo(ProgressBarRangeInfo(0.25f, 0f..1f)),
            useUnmergedTree = true,
        ).assertIsDisplayed()
        rule.onNode(tagInRow(albania, MapDownloaderTestTags.DOWNLOAD)).assertDoesNotExist()
    }

    @Test
    fun mapDownloaderContent_runningMap_hasAStopMarkInsideItsProgress() {
        show(listed)
        for (total in listOf(FileSize(100), null)) {
            state = albaniaDownload(MapDownload.Running(albaniaPath, FileSize(25), total))

            rule.onNode(tagInRow(albania, MapDownloaderTestTags.STOP_MARK), useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun mapDownloaderContent_queuedMap_hasNoStopMark() {
        show(albaniaDownload(MapDownload.Queued(albaniaPath)))

        rule.onNode(tagInRow(albania, MapDownloaderTestTags.STOP_MARK), useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun mapDownloaderContent_runningMapOfUnknownSize_showsASpinner() {
        show(albaniaDownload(MapDownload.Running(albaniaPath, FileSize(25), total = null)))

        rule.onNode(
            tagInRow(albania, MapDownloaderTestTags.PROGRESS) and
                hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate),
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }

    @Test
    fun mapDownloaderContent_failedMap_saysWhyAndRetries() {
        show(albaniaDownload(MapDownload.Failed(albaniaPath, MapDownloadFailure.NOT_ENOUGH_SPACE)))

        rule.onNode(textInRow(albania, "Not enough free space")).assertIsDisplayed()
        rule.onNode(tagInRow(albania, MapDownloaderTestTags.RETRY_DOWNLOAD)).performClick()

        assertEquals(listOf(albania), downloaded)
    }

    @Test
    fun mapDownloaderContent_failures_showTheirMessages() {
        val messages = mapOf(
            MapDownloadFailure.SERVER_ERROR to "The map site did not send it",
            MapDownloadFailure.NOT_ENOUGH_SPACE to "Not enough free space",
            MapDownloadFailure.UNREADABLE_MAP to "The downloaded map cannot be read",
            MapDownloadFailure.STORAGE_ERROR to "The map cannot be saved",
        )
        show(listed)

        for ((reason, message) in messages) {
            state = albaniaDownload(MapDownload.Failed(albaniaPath, reason))
            rule.onNode(textInRow(albania, message)).assertIsDisplayed()
        }
    }

    @Test
    fun mapDownloaderContent_installedMapWithAnOldFailure_isOnlyMarkedInstalled() {
        val andorraPath = RemoteMapPath(europe, "andorra.map")
        val oldFailure = MapDownload.Failed(andorraPath, MapDownloadFailure.STORAGE_ERROR)
        show(listed.copy(downloads = mapOf("andorra.map" to oldFailure)))

        rule.onNode(tagInRow(andorra, MapDownloaderTestTags.INSTALLED)).assertIsDisplayed()
        rule.onNode(tagInRow(andorra, MapDownloaderTestTags.RETRY_DOWNLOAD)).assertDoesNotExist()
        rule.onNode(textInRow(andorra, "The map cannot be saved")).assertDoesNotExist()
    }

    @Test
    fun mapDownloaderContent_installedMapDownloadingAgain_showsTheDownload() {
        val andorraPath = RemoteMapPath(europe, "andorra.map")
        show(listed.copy(downloads = mapOf("andorra.map" to MapDownload.Queued(andorraPath))))

        rule.onNode(tagInRow(andorra, MapDownloaderTestTags.QUEUED), useUnmergedTree = true).assertIsDisplayed()
        rule.onNode(tagInRow(andorra, MapDownloaderTestTags.INSTALLED)).assertDoesNotExist()
    }

    // --- Stopping a download ---

    @Test
    fun mapDownloaderContent_queuedMapPressed_asksWhetherToStopWithoutStopping() {
        show(albaniaDownload(MapDownload.Queued(albaniaPath)))

        rule.onNode(tagInRow(albania, MapDownloaderTestTags.CANCEL_DOWNLOAD)).performClick()

        rule.onNode(hasText("Stop downloading albania.map?") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        assertTrue(cancelled.isEmpty())
    }

    @Test
    fun mapDownloaderContent_runningMapPressed_asksWhetherToStop() {
        show(albaniaDownload(MapDownload.Running(albaniaPath, FileSize(25), FileSize(100))))

        rule.onNode(tagInRow(albania, MapDownloaderTestTags.CANCEL_DOWNLOAD)).performClick()

        rule.onNode(hasText("Stop downloading albania.map?") and hasAnyAncestor(isDialog())).assertIsDisplayed()
    }

    @Test
    fun mapDownloaderContent_stopConfirmed_cancelsThatMapAndCloses() {
        show(albaniaDownload(MapDownload.Queued(albaniaPath)))
        rule.onNode(tagInRow(albania, MapDownloaderTestTags.CANCEL_DOWNLOAD)).performClick()

        rule.onNodeWithTag(MapDownloaderTestTags.CONFIRM_CANCEL).performClick()

        assertEquals(listOf(albania), cancelled)
        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun mapDownloaderContent_keepDownloading_closesWithoutCancelling() {
        show(albaniaDownload(MapDownload.Queued(albaniaPath)))
        rule.onNode(tagInRow(albania, MapDownloaderTestTags.CANCEL_DOWNLOAD)).performClick()

        rule.onNodeWithTag(MapDownloaderTestTags.KEEP_DOWNLOADING).performClick()

        assertTrue(cancelled.isEmpty())
        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun mapDownloaderContent_downloadEndsWhileConfirming_closesTheDialog() {
        show(albaniaDownload(MapDownload.Running(albaniaPath, FileSize(99), FileSize(100))))
        rule.onNode(tagInRow(albania, MapDownloaderTestTags.CANCEL_DOWNLOAD)).performClick()

        state = listed.copy(installed = setOf("andorra.map", "albania.map"))
        rule.waitForIdle()

        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun mapDownloaderContent_stopDialog_survivesRecreation() {
        val restorationTester = StateRestorationTester(rule)
        state = albaniaDownload(MapDownload.Queued(albaniaPath))
        restorationTester.setContent {
            GisteTheme {
                MapDownloaderContent(
                    state = state,
                    onBack = {},
                    onOpen = {},
                    onUp = {},
                    onRetry = {},
                    onDownload = {},
                    onCancel = {},
                    onMobileData = {},
                    onDismissMobileData = {},
                )
            }
        }
        rule.onNode(tagInRow(albania, MapDownloaderTestTags.CANCEL_DOWNLOAD)).performClick()

        restorationTester.emulateSavedInstanceStateRestore()

        rule.onNode(hasText("Stop downloading albania.map?") and hasAnyAncestor(isDialog())).assertIsDisplayed()
    }

    // --- Mobile data ---

    private val mobileDataQuestion = listed.copy(mobileDataQuestion = albaniaPath)

    @Test
    fun mapDownloaderContent_mobileDataQuestion_asksAboutThatMap() {
        show(mobileDataQuestion)

        rule.onNode(hasText("Not on Wi-Fi") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        rule.onNode(
            hasText("Download albania.map now with mobile data, or wait for Wi-Fi?", substring = true) and
                hasAnyAncestor(isDialog()),
        ).assertIsDisplayed()
    }

    @Test
    fun mapDownloaderContent_noMobileDataQuestion_showsNoDialog() {
        show(listed)

        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun mapDownloaderContent_useMobileData_answersYes() {
        show(mobileDataQuestion)

        rule.onNodeWithTag(MapDownloaderTestTags.USE_MOBILE_DATA).performClick()

        assertEquals(listOf(true), mobileDataAnswers)
    }

    @Test
    fun mapDownloaderContent_waitForWiFi_answersNo() {
        show(mobileDataQuestion)

        rule.onNodeWithTag(MapDownloaderTestTags.WAIT_FOR_WIFI).performClick()

        assertEquals(listOf(false), mobileDataAnswers)
    }

    @Test
    fun mapDownloaderContent_closeMobileData_dismissesWithoutAnswer() {
        show(mobileDataQuestion)

        rule.onNodeWithTag(MapDownloaderTestTags.DISMISS_MOBILE_DATA).performClick()

        assertEquals(1, mobileDataDismissals)
        assertTrue(mobileDataAnswers.isEmpty())
    }

    @Test
    fun mapDownloaderContent_dialogButtons_areAtLeastTheMinimumTouchTarget() {
        show(mobileDataQuestion)

        for (tag in listOf(
            MapDownloaderTestTags.USE_MOBILE_DATA,
            MapDownloaderTestTags.WAIT_FOR_WIFI,
            MapDownloaderTestTags.DISMISS_MOBILE_DATA,
        )) {
            rule.onNodeWithTag(tag).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        }
        rule.onNodeWithTag(MapDownloaderTestTags.DISMISS_MOBILE_DATA).assertWidthIsAtLeast(MIN_TOUCH_TARGET)

        state = albaniaDownload(MapDownload.Queued(albaniaPath))
        rule.onNode(tagInRow(albania, MapDownloaderTestTags.CANCEL_DOWNLOAD))
            .assertWidthIsAtLeast(MIN_TOUCH_TARGET)
            .assertHeightIsAtLeast(MIN_TOUCH_TARGET)
            .performClick()
        rule.onNodeWithTag(MapDownloaderTestTags.CONFIRM_CANCEL).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        rule.onNodeWithTag(MapDownloaderTestTags.KEEP_DOWNLOADING).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
    }

    @Test
    @Config(qualifiers = "es")
    fun mapDownloaderContent_spanishDialogs_areTranslated() {
        show(mobileDataQuestion)

        rule.onNode(hasText("Sin wifi") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        rule.onNodeWithTag(MapDownloaderTestTags.DISMISS_MOBILE_DATA).performClick()
        state = albaniaDownload(MapDownload.Queued(albaniaPath))
        rule.onNode(tagInRow(albania, MapDownloaderTestTags.CANCEL_DOWNLOAD)).performClick()

        rule.onNode(hasText("¿Dejar de descargar albania.map?") and hasAnyAncestor(isDialog())).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es")
    fun mapDownloaderContent_spanishFailure_isTranslated() {
        show(albaniaDownload(MapDownload.Failed(albaniaPath, MapDownloadFailure.NOT_ENOUGH_SPACE)))

        rule.onNode(textInRow(albania, "No hay espacio libre suficiente")).assertIsDisplayed()
    }

    @Test
    fun mapDownloaderContent_rowsAndButtons_areAtLeastTheMinimumTouchTarget() {
        show(listed)

        rule.onNodeWithTag(MapDownloaderTestTags.BACK)
            .assertWidthIsAtLeast(MIN_TOUCH_TARGET)
            .assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        rule.onNodeWithTag(MapDownloaderTestTags.entry(italy)).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        rule.onNodeWithTag(MapDownloaderTestTags.entry(andorra)).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        rule.onNode(tagInRow(albania, MapDownloaderTestTags.DOWNLOAD))
            .assertWidthIsAtLeast(MIN_TOUCH_TARGET)
            .assertHeightIsAtLeast(MIN_TOUCH_TARGET)

        state = albaniaDownload(MapDownload.Failed(albaniaPath, MapDownloadFailure.SERVER_ERROR))
        rule.onNode(tagInRow(albania, MapDownloaderTestTags.RETRY_DOWNLOAD))
            .assertWidthIsAtLeast(MIN_TOUCH_TARGET)
            .assertHeightIsAtLeast(MIN_TOUCH_TARGET)

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
