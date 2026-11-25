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

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.giste.map.MainDispatcherRule
import dev.giste.map.domain.CheckMapUpdatesUseCase
import dev.giste.map.domain.DeleteMapResult
import dev.giste.map.domain.FakeMapDownloadRepository
import dev.giste.map.domain.FakeMapUpdateRepository
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.InstalledMapFile
import dev.giste.map.domain.FakeMapFileRepository
import dev.giste.map.domain.InstalledMapFiles
import dev.giste.map.domain.MapDownload
import dev.giste.map.domain.MapDownloadFailure
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteMapPath
import dev.giste.map.domain.TEST_MODIFIED
import dev.giste.map.domain.testInstalledFile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MapManagerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val spain = testInstalledFile("spain.map")
    private val damaged = testInstalledFile("damaged.map", readable = false)
    private val repository = FakeMapFileRepository(InstalledMapFiles.Listed(listOf(damaged, spain)))

    private val downloads = FakeMapDownloadRepository()
    private val updates = FakeMapUpdateRepository()

    /** Whether the device is on Wi-Fi. */
    private var onWiFi = true

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) = MapManagerViewModel(
        savedStateHandle = savedState,
        repository = repository,
        downloadRepository = downloads,
        updateRepository = updates,
        checkUpdates = CheckMapUpdatesUseCase(repository, updates),
        network = { onWiFi },
    )

    private val spainPath = RemoteMapPath(RemoteFolderPath.ROOT, "spain.map")
    private val damagedPath = RemoteMapPath(RemoteFolderPath.ROOT, "damaged.map")

    /** Both files outdated: the site has newer versions. */
    private fun bothOutdated() {
        val newer = TEST_MODIFIED.plusSeconds(60)
        updates.current.value = mapOf(spainPath to newer, damagedPath to newer)
    }

    /** [files] with nothing known about their updates. */
    private fun managed(vararg files: InstalledMapFile) = files.map(::ManagedMapFile)

    /** The state once the files are listed, after [action]. */
    private suspend fun MapManagerViewModel.listedAfter(
        action: MapManagerViewModel.() -> Unit,
    ): MapManagerUiState.Listed {
        var state: MapManagerUiState.Listed? = null
        uiState.test {
            skipItems(2)
            action()
            state = awaitItem() as MapManagerUiState.Listed
        }
        return checkNotNull(state)
    }

    @Test
    fun uiState_beforeTheFilesAreFound_isLoading() {
        assertEquals(MapManagerUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun uiState_files_areListedReadableOrNot() = runTest {
        viewModel().uiState.test {
            assertEquals(MapManagerUiState.Loading, awaitItem())
            assertEquals(MapManagerUiState.Listed(managed(damaged, spain)), awaitItem())
        }
    }

    @Test
    fun uiState_noFiles_isAnEmptyList() = runTest {
        repository.result = InstalledMapFiles.Listed(emptyList())

        viewModel().uiState.test {
            skipItems(1)
            assertEquals(MapManagerUiState.Listed(managed()), awaitItem())
        }
    }

    @Test
    fun uiState_folderUnreadable_isFolderUnreadable() = runTest {
        repository.result = InstalledMapFiles.FolderUnreadable

        viewModel().uiState.test {
            skipItems(1)
            assertEquals(MapManagerUiState.FolderUnreadable, awaitItem())
        }
    }

    @Test
    fun delete_deleted_deletesTheFile() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(2)

            viewModel.delete(damaged)
            testScheduler.advanceUntilIdle()

            // The list changes when the repository finds the files again, not before.
            expectNoEvents()
        }
        assertEquals(listOf(damaged), repository.deleted)
    }

    @Test
    fun uiState_filesChange_followsThem() = runTest {
        viewModel().uiState.test {
            skipItems(2)

            // E.g. after a deletion, or a download from the map downloader.
            repository.result = InstalledMapFiles.Listed(listOf(spain))

            assertEquals(MapManagerUiState.Listed(managed(spain)), awaitItem())
        }
        // Once for the list, once for the update check.
        assertEquals(2, repository.collections)
    }

    @Test
    fun delete_failed_saysWhichFileAndListsTheFilesAgain() = runTest {
        repository.deleteResult = DeleteMapResult.Failed
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(2)

            viewModel.delete(spain)

            assertEquals(MapManagerUiState.Listed(managed(damaged, spain), deleteFailed = spain), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun dismissDeleteError_afterAFailure_clearsIt() = runTest {
        repository.deleteResult = DeleteMapResult.Failed
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(2)
            viewModel.delete(spain)
            skipItems(1)

            viewModel.dismissDeleteError()

            assertEquals(MapManagerUiState.Listed(managed(damaged, spain)), awaitItem())
        }
    }

    @Test
    fun uiState_resubscribedAfterTheTimeout_looksForTheFilesAgain() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test { skipItems(2) }
        testScheduler.advanceTimeBy(6_000)
        repository.result = InstalledMapFiles.Listed(emptyList())

        viewModel.uiState.test {
            // The stale value is replayed first, then the new search replaces it.
            skipItems(1)
            assertEquals(MapManagerUiState.Listed(managed()), awaitItem())
        }
        // Twice for the list, once for the update check.
        assertEquals(3, repository.collections)
    }

    @Test
    fun uiState_resubscribedWithinTheTimeout_keepsTheFiles() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test { skipItems(2) }
        testScheduler.advanceTimeBy(1_000)

        viewModel.uiState.test {
            assertEquals(MapManagerUiState.Listed(managed(damaged, spain)), awaitItem())
        }
        // Once for the list, once for the update check.
        assertEquals(2, repository.collections)
    }

    // --- Updates ---

    @Test
    fun init_checksTheInstalledMapsForUpdates() = runTest {
        viewModel()
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(listOf(damagedPath, spainPath)), updates.checked)
    }

    @Test
    fun uiState_siteNewer_marksTheFileOutdated() = runTest {
        updates.current.value = mapOf(spainPath to TEST_MODIFIED.plusSeconds(60))

        viewModel().uiState.test {
            skipItems(1)
            assertEquals(
                MapManagerUiState.Listed(listOf(ManagedMapFile(damaged), ManagedMapFile(spain, outdated = true))),
                awaitItem(),
            )
        }
    }

    @Test
    fun uiState_siteDatesFoundWhileShown_markTheOutdatedFiles() = runTest {
        viewModel().uiState.test {
            skipItems(2)

            updates.current.value = mapOf(spainPath to TEST_MODIFIED.plusSeconds(60))

            val listed = awaitItem() as MapManagerUiState.Listed
            assertEquals(listOf(false, true), listed.files.map { it.outdated })
        }
    }

    @Test
    fun uiState_updateInstalled_isNoLongerOutdated() = runTest {
        val newer = TEST_MODIFIED.plusSeconds(60)
        updates.current.value = mapOf(spainPath to newer)

        viewModel().uiState.test {
            skipItems(2)

            // The update installed takes the site's date.
            repository.result = InstalledMapFiles.Listed(listOf(testInstalledFile("spain.map", modified = newer)))

            assertEquals(listOf(false), (awaitItem() as MapManagerUiState.Listed).files.map { it.outdated })
        }
    }

    @Test
    fun uiState_downloadsOfTheFiles_areShownWithThem() = runTest {
        val running = MapDownload.Running(spainPath, FileSize(10), FileSize(100))
        downloads.current.value = listOf(
            running,
            // Another map, e.g. a new one downloading from the downloader.
            MapDownload.Queued(RemoteMapPath(RemoteFolderPath.ROOT.child("europe"), "spain.map")),
        )

        viewModel().uiState.test {
            skipItems(1)
            assertEquals(
                MapManagerUiState.Listed(listOf(ManagedMapFile(damaged), ManagedMapFile(spain, download = running))),
                awaitItem(),
            )
        }
    }

    @Test
    fun updatable_outdatedFiles_leaveOutThoseDownloading() = runTest {
        bothOutdated()
        downloads.current.value = listOf(MapDownload.Queued(spainPath))

        viewModel().uiState.test {
            skipItems(1)
            assertEquals(listOf(damaged), (awaitItem() as MapManagerUiState.Listed).updatable.map { it.file })
        }
    }

    @Test
    fun updatable_failedUpdate_canBeStartedAgain() = runTest {
        bothOutdated()
        downloads.current.value = listOf(MapDownload.Failed(spainPath, MapDownloadFailure.SERVER_ERROR))

        viewModel().uiState.test {
            skipItems(1)
            assertEquals(listOf(damaged, spain), (awaitItem() as MapManagerUiState.Listed).updatable.map { it.file })
        }
    }

    @Test
    fun update_onWiFi_startsItWithoutMobileData() = runTest {
        viewModel().update(spain)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(spainPath to false), downloads.started)
    }

    @Test
    fun update_unsafePath_doesNothing() = runTest {
        val viewModel = viewModel()

        viewModel.update(testInstalledFile("bad\\name.map"))
        testScheduler.advanceUntilIdle()

        assertEquals(emptyList<Pair<RemoteMapPath, Boolean>>(), downloads.started)
    }

    @Test
    fun update_offWiFi_asksFirstWithoutStarting() = runTest {
        onWiFi = false

        val listed = viewModel().listedAfter { update(spain) }

        assertEquals(listOf(spainPath), listed.mobileDataQuestion)
        assertEquals(emptyList<Pair<RemoteMapPath, Boolean>>(), downloads.started)
    }

    @Test
    fun updateAll_onWiFi_startsEveryUpdatableFileInOrder() = runTest {
        bothOutdated()
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(2)
            viewModel.updateAll()
            testScheduler.advanceUntilIdle()
        }

        assertEquals(listOf(damagedPath to false, spainPath to false), downloads.started)
    }

    @Test
    fun updateAll_nothingUpdatable_doesNothing() = runTest {
        onWiFi = false
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(2)
            viewModel.updateAll()
            testScheduler.advanceUntilIdle()
            expectNoEvents()
        }

        assertEquals(emptyList<Pair<RemoteMapPath, Boolean>>(), downloads.started)
    }

    @Test
    fun updateAll_beforeTheFilesAreListed_doesNothing() = runTest {
        bothOutdated()

        viewModel().updateAll()
        testScheduler.advanceUntilIdle()

        assertEquals(emptyList<Pair<RemoteMapPath, Boolean>>(), downloads.started)
    }

    @Test
    fun updateAll_offWiFi_asksOnceForAll() = runTest {
        bothOutdated()
        onWiFi = false
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(2)
            viewModel.updateAll()

            assertEquals(listOf(damagedPath, spainPath), (awaitItem() as MapManagerUiState.Listed).mobileDataQuestion)
        }
        assertEquals(emptyList<Pair<RemoteMapPath, Boolean>>(), downloads.started)
    }

    @Test
    fun answerMobileData_yes_startsEveryUpdateWithMobileDataAndClosesTheQuestion() = runTest {
        val viewModel = viewModel(SavedStateHandle(mapOf("mobile_data_question" to "damaged.map\nspain.map")))

        val listed = viewModel.listedAfter { answerMobileData(useMobileData = true) }

        assertEquals(emptyList<RemoteMapPath>(), listed.mobileDataQuestion)
        assertEquals(listOf(damagedPath to true, spainPath to true), downloads.started)
    }

    @Test
    fun answerMobileData_no_startsTheUpdatesWaitingForWiFi() = runTest {
        val viewModel = viewModel(SavedStateHandle(mapOf("mobile_data_question" to "spain.map")))

        viewModel.listedAfter { answerMobileData(useMobileData = false) }

        assertEquals(listOf(spainPath to false), downloads.started)
    }

    @Test
    fun answerMobileData_withoutQuestion_doesNothing() = runTest {
        val viewModel = viewModel()

        viewModel.answerMobileData(useMobileData = true)
        testScheduler.advanceUntilIdle()

        assertEquals(emptyList<Pair<RemoteMapPath, Boolean>>(), downloads.started)
    }

    @Test
    fun dismissMobileData_closesTheQuestionWithoutStarting() = runTest {
        val viewModel = viewModel(SavedStateHandle(mapOf("mobile_data_question" to "spain.map")))

        val listed = viewModel.listedAfter { dismissMobileData() }

        assertEquals(emptyList<RemoteMapPath>(), listed.mobileDataQuestion)
        assertEquals(emptyList<Pair<RemoteMapPath, Boolean>>(), downloads.started)
    }

    @Test
    fun mobileDataQuestion_isSavedSoItSurvivesTheProcess() = runTest {
        onWiFi = false
        val savedState = SavedStateHandle()

        viewModel(savedState).listedAfter { update(spain) }

        assertEquals("spain.map", savedState.get<String>("mobile_data_question"))
    }

    @Test
    fun cancel_file_cancelsItsUpdate() = runTest {
        viewModel().cancel(spain)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(spainPath), downloads.cancelled)
    }

    @Test
    fun cancel_unsafePath_doesNothing() = runTest {
        viewModel().cancel(testInstalledFile("bad\\name.map"))
        testScheduler.advanceUntilIdle()

        assertEquals(emptyList<RemoteMapPath>(), downloads.cancelled)
    }
}
