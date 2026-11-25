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
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.InstalledMapFile
import dev.giste.map.domain.MapDownload
import dev.giste.map.domain.MapDownloadFailure
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteMapPath
import dev.giste.map.domain.testInstalledFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    private val listed = MapManagerUiState.Listed(managed(damaged, spain))

    /** More files than fit on the screen, so the list scrolls. */
    private val manyFiles = (1..30).map { testInstalledFile("map$it.map") }

    private var backs = 0
    private var downloads = 0
    private val deleted = mutableListOf<InstalledMapFile>()
    private var errorsShown = 0
    private val updated = mutableListOf<InstalledMapFile>()
    private var updatesAll = 0
    private val updatesCancelled = mutableListOf<InstalledMapFile>()
    private val mobileDataAnswers = mutableListOf<Boolean>()
    private var mobileDataDismissals = 0

    /** What the manager shows. Tests change it to update the screen. */
    private var state: MapManagerUiState by mutableStateOf(MapManagerUiState.Loading)

    /**
     * Shows the manager in [initial], recording its callbacks. It has an add button unless
     * [downloadable] is `false`.
     */
    private fun show(
        initial: MapManagerUiState,
        restorationTester: StateRestorationTester? = null,
        downloadable: Boolean = true,
    ) {
        state = initial
        val content: @Composable () -> Unit = {
            GisteTheme {
                MapManagerContent(
                    state = state,
                    onBack = { backs++ },
                    onDownloadMaps = if (downloadable) ({ downloads++ }) else null,
                    onDelete = { deleted += it },
                    onDeleteErrorShown = { errorsShown++ },
                    onUpdate = { updated += it },
                    onUpdateAll = { updatesAll++ },
                    onCancelUpdate = { updatesCancelled += it },
                    onMobileData = { mobileDataAnswers += it },
                    onDismissMobileData = { mobileDataDismissals++ },
                )
            }
        }
        if (restorationTester == null) rule.setContent(content) else restorationTester.setContent(content)
    }

    /** [files] with nothing known about their updates. */
    private fun managed(vararg files: InstalledMapFile) = files.map(::ManagedMapFile)

    private val spainPath = RemoteMapPath(RemoteFolderPath.ROOT, "spain.map")
    private val damagedPath = RemoteMapPath(RemoteFolderPath.ROOT, "damaged.map")

    /** Spain outdated; the damaged file up to date. */
    private val spainOutdated = MapManagerUiState.Listed(
        listOf(ManagedMapFile(damaged), ManagedMapFile(spain, outdated = true)),
    )

    /** Both files outdated. */
    private val bothOutdated = MapManagerUiState.Listed(
        listOf(ManagedMapFile(damaged, outdated = true), ManagedMapFile(spain, outdated = true)),
    )

    /** [spainOutdated] with Spain's update in [download]. */
    private fun spainUpdate(download: MapDownload) = MapManagerUiState.Listed(
        listOf(ManagedMapFile(damaged), ManagedMapFile(spain, outdated = true, download = download)),
    )

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
    fun mapManagerContent_addPressed_callsOnDownloadMaps() {
        show(listed)

        rule.onNodeWithTag(MapManagerTestTags.ADD).performClick()

        assertEquals(1, downloads)
    }

    @Test
    fun mapManagerContent_everyState_showsTheAddButton() {
        show(MapManagerUiState.Loading)

        val states = listOf(
            MapManagerUiState.Loading,
            MapManagerUiState.FolderUnreadable,
            MapManagerUiState.Listed(emptyList()),
            listed,
        )
        for (each in states) {
            state = each
            rule.onNodeWithTag(MapManagerTestTags.ADD).assertIsDisplayed()
        }
    }

    @Test
    fun mapManagerContent_withoutOnDownloadMaps_hasNoAddButton() {
        show(listed, downloadable = false)

        rule.onNodeWithTag(MapManagerTestTags.ADD).assertDoesNotExist()
    }

    @Test
    fun mapManagerContent_scrolledToTheEnd_lastRowIsClearOfTheAddButton() {
        show(MapManagerUiState.Listed(manyFiles.map(::ManagedMapFile)))
        rule.onNodeWithTag(MapManagerTestTags.LIST).performScrollToIndex(manyFiles.lastIndex)
        // Scrolling to the last item only brings it into view; swipe up to reach the list's end.
        rule.onNodeWithTag(MapManagerTestTags.LIST).performTouchInput { swipeUp() }

        val lastRow = rule.onNodeWithTag(MapManagerTestTags.file(manyFiles.last())).getBoundsInRoot()
        val addButton = rule.onNodeWithTag(MapManagerTestTags.ADD).getBoundsInRoot()
        assertTrue(
            "last row ends at ${lastRow.bottom}, button starts at ${addButton.top}",
            lastRow.bottom <= addButton.top,
        )
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
        show(MapManagerUiState.Listed(managed(corsica, spain)))

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
        rule.onNodeWithTag(MapManagerTestTags.ADD)
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

        state = MapManagerUiState.Listed(managed(damaged))
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

    // --- Updates ---

    @Test
    fun mapManagerContent_nothingOutdated_hasNoUpdateButtons() {
        show(listed)

        rule.onNodeWithTag(MapDownloadTestTags.UPDATE).assertDoesNotExist()
        rule.onNodeWithTag(MapManagerTestTags.UPDATE_ALL).assertDoesNotExist()
    }

    @Test
    fun mapManagerContent_outdatedFile_hasAnUpdateButtonAndOthersNone() {
        show(spainOutdated)

        rule.onNode(inRow(spain, MapDownloadTestTags.UPDATE)).assertIsDisplayed()
        rule.onNode(inRow(damaged, MapDownloadTestTags.UPDATE)).assertDoesNotExist()
    }

    @Test
    fun mapManagerContent_updatePressed_callsOnUpdateWithTheFile() {
        show(spainOutdated)

        rule.onNode(inRow(spain, MapDownloadTestTags.UPDATE)).performClick()

        assertEquals(listOf(spain), updated)
    }

    @Test
    fun mapManagerContent_oneUpdatableFile_hasNoUpdateAllButton() {
        show(spainOutdated)

        rule.onNodeWithTag(MapManagerTestTags.UPDATE_ALL).assertDoesNotExist()
    }

    @Test
    fun mapManagerContent_twoUpdatableFiles_hasAnUpdateAllButton() {
        show(bothOutdated)

        rule.onNodeWithTag(MapManagerTestTags.UPDATE_ALL).performClick()

        assertEquals(1, updatesAll)
    }

    @Test
    fun mapManagerContent_twoOutdatedOneUpdating_hasNoUpdateAllButton() {
        show(
            MapManagerUiState.Listed(
                listOf(
                    ManagedMapFile(damaged, outdated = true),
                    ManagedMapFile(spain, outdated = true, download = MapDownload.Queued(spainPath)),
                ),
            ),
        )

        rule.onNodeWithTag(MapManagerTestTags.UPDATE_ALL).assertDoesNotExist()
    }

    @Test
    fun mapManagerContent_updateButtons_areAtLeastTheMinimumTouchTarget() {
        show(bothOutdated)

        listOf(rule.onNode(inRow(spain, MapDownloadTestTags.UPDATE)), rule.onNodeWithTag(MapManagerTestTags.UPDATE_ALL))
            .forEach { it.assertWidthIsAtLeast(MIN_TOUCH_TARGET).assertHeightIsAtLeast(MIN_TOUCH_TARGET) }
    }

    @Test
    fun mapManagerContent_updateQueued_showsTheWaitingMarkInsteadOfTheButton() {
        show(spainUpdate(MapDownload.Queued(spainPath)))

        rule.onNode(inRow(spain, MapDownloadTestTags.QUEUED), useUnmergedTree = true).assertIsDisplayed()
        rule.onNode(inRow(spain, MapDownloadTestTags.UPDATE)).assertDoesNotExist()
    }

    @Test
    fun mapManagerContent_updateRunning_showsItsProgress() {
        show(spainUpdate(MapDownload.Running(spainPath, FileSize(50), FileSize(100))))

        rule.onNode(inRow(spain, MapDownloadTestTags.PROGRESS), useUnmergedTree = true).assertIsDisplayed()
        rule.onNode(inRow(spain, MapDownloadTestTags.STOP_MARK), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun mapManagerContent_updateFailed_showsWhyAndTriesAgain() {
        show(spainUpdate(MapDownload.Failed(spainPath, MapDownloadFailure.NOT_ENOUGH_SPACE)))

        rule.onNode(hasText("Not enough free space") and hasAnyAncestor(hasTestTag(MapManagerTestTags.file(spain))))
            .assertIsDisplayed()
        rule.onNode(inRow(spain, MapDownloadTestTags.RETRY_DOWNLOAD)).performClick()

        assertEquals(listOf(spain), updated)
    }

    @Test
    fun mapManagerContent_failureOfAnUpToDateFile_isNotShown() {
        val oldFailure = MapDownload.Failed(damagedPath, MapDownloadFailure.NOT_ENOUGH_SPACE)
        show(MapManagerUiState.Listed(listOf(ManagedMapFile(damaged, download = oldFailure), ManagedMapFile(spain))))

        rule.onNodeWithText("Not enough free space").assertDoesNotExist()
        rule.onNodeWithTag(MapDownloadTestTags.RETRY_DOWNLOAD).assertDoesNotExist()
    }

    @Test
    fun mapManagerContent_updatingPressed_asksAndStopsOnConfirm() {
        show(spainUpdate(MapDownload.Running(spainPath, FileSize(50), FileSize(100))))
        rule.onNode(inRow(spain, MapDownloadTestTags.CANCEL_DOWNLOAD)).performClick()

        rule.onNode(hasText("Stop downloading spain.map?") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        assertEquals(emptyList<InstalledMapFile>(), updatesCancelled)

        rule.onNodeWithTag(MapDownloadTestTags.CONFIRM_CANCEL).performClick()

        assertEquals(listOf(spain), updatesCancelled)
        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun mapManagerContent_stopDismissed_keepsUpdating() {
        show(spainUpdate(MapDownload.Queued(spainPath)))
        rule.onNode(inRow(spain, MapDownloadTestTags.CANCEL_DOWNLOAD)).performClick()

        rule.onNodeWithTag(MapDownloadTestTags.KEEP_DOWNLOADING).performClick()

        assertEquals(emptyList<InstalledMapFile>(), updatesCancelled)
        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun mapManagerContent_updateFinishedWhileAskingToStop_closesTheDialog() {
        show(spainUpdate(MapDownload.Queued(spainPath)))
        rule.onNode(inRow(spain, MapDownloadTestTags.CANCEL_DOWNLOAD)).performClick()

        state = MapManagerUiState.Listed(managed(damaged, spain))
        rule.waitForIdle()

        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun mapManagerContent_mobileDataQuestionForOne_namesTheMap() {
        show(spainOutdated.copy(mobileDataQuestion = listOf(spainPath)))

        rule.onNode(
            hasText("Download spain.map now with mobile data, or wait for Wi-Fi?", substring = true) and
                hasAnyAncestor(isDialog()),
        ).assertIsDisplayed()
    }

    @Test
    fun mapManagerContent_mobileDataQuestionForSeveral_saysHowMany() {
        show(bothOutdated.copy(mobileDataQuestion = listOf(damagedPath, spainPath)))

        rule.onNode(
            hasText("Download the updates of 2 maps now with mobile data, or wait for Wi-Fi?", substring = true) and
                hasAnyAncestor(isDialog()),
        ).assertIsDisplayed()
    }

    @Test
    fun mapManagerContent_mobileDataAnswers_areReported() {
        show(spainOutdated.copy(mobileDataQuestion = listOf(spainPath)))

        rule.onNodeWithTag(MapDownloadTestTags.USE_MOBILE_DATA).performClick()
        rule.onNodeWithTag(MapDownloadTestTags.WAIT_FOR_WIFI).performClick()
        rule.onNodeWithTag(MapDownloadTestTags.DISMISS_MOBILE_DATA).performClick()

        assertEquals(listOf(true, false), mobileDataAnswers)
        assertEquals(1, mobileDataDismissals)
    }

    @Test
    @Config(qualifiers = "es")
    fun mapManagerContent_spanishMobileDataQuestionForSeveral_isTranslated() {
        show(bothOutdated.copy(mobileDataQuestion = listOf(damagedPath, spainPath)))

        rule.onNode(
            hasText("¿Descargar ahora las actualizaciones de 2 mapas", substring = true) and hasAnyAncestor(isDialog()),
        ).assertIsDisplayed()
    }

    private companion object {
        val MIN_TOUCH_TARGET = 56.dp

        /** Longer than a short snackbar is shown. */
        const val SNACKBAR_SHORT_MILLIS = 10_000L
    }
}
