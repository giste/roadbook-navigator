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
import dev.giste.map.domain.FakeMapDownloadRepository
import dev.giste.map.domain.FakeMapFileRepository
import dev.giste.map.domain.FakeRemoteMapRepository
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.InstalledMapFiles
import dev.giste.map.domain.MapDownload
import dev.giste.map.domain.MapDownloadFailure
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteListing
import dev.giste.map.domain.RemoteListingFailure
import dev.giste.map.domain.RemoteMapEntry
import dev.giste.map.domain.RemoteMapPath
import dev.giste.map.domain.testInstalledFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MapDownloaderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val root = RemoteFolderPath.ROOT
    private val europe = root.child("europe")
    private val italy = europe.child("italy")

    private val europeFolder = RemoteMapEntry.Folder("europe")
    private val italyFolder = RemoteMapEntry.Folder("italy")
    private val andorra = RemoteMapEntry.MapFile("andorra.map", null, null)
    private val spain = RemoteMapEntry.MapFile("spain.map", null, null)

    private val remote = FakeRemoteMapRepository().apply {
        results[root] = RemoteListing.Listed(listOf(europeFolder))
        results[europe] = RemoteListing.Listed(listOf(italyFolder, andorra, spain))
    }

    /** Andorra is installed in `europe`, as a download puts it; Spain only in the map folder itself. */
    private val installed = FakeMapFileRepository(
        InstalledMapFiles.Listed(
            listOf(
                testInstalledFile("andorra.map").copy(path = "/maps/europe/andorra.map", folder = "europe"),
                testInstalledFile("spain.map"),
            ),
        ),
    )

    private val downloads = FakeMapDownloadRepository()

    /** Whether the device is on Wi-Fi. */
    private var onWiFi = true

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) =
        MapDownloaderViewModel(savedState, remote, installed, downloads) { onWiFi }

    /** A view model showing `europe`, with [extra] saved state. */
    private fun inEurope(vararg extra: Pair<String, Any>) =
        viewModel(SavedStateHandle(mapOf("folder" to "europe", *extra)))

    private val spainPath = RemoteMapPath(europe, "spain.map")

    /** The state once `europe` is listed, after [action]. */
    private suspend fun MapDownloaderViewModel.listedAfter(
        action: MapDownloaderViewModel.() -> Unit,
    ): MapDownloaderUiState.Listed {
        var state: MapDownloaderUiState.Listed? = null
        uiState.test {
            skipItems(2)
            action()
            state = awaitItem() as MapDownloaderUiState.Listed
        }
        return checkNotNull(state)
    }

    @Test
    fun uiState_beforeTheFolderIsRead_isLoadingTheRoot() {
        assertEquals(MapDownloaderUiState.Loading(root), viewModel().uiState.value)
    }

    @Test
    fun uiState_root_isListed() = runTest {
        viewModel().uiState.test {
            assertEquals(MapDownloaderUiState.Loading(root), awaitItem())
            assertEquals(MapDownloaderUiState.Listed(root, listOf(europeFolder)), awaitItem())
        }
        assertEquals(listOf(root), remote.listed)
    }

    @Test
    fun uiState_whileReading_staysLoading() = runTest {
        remote.gate = CompletableDeferred()

        viewModel().uiState.test {
            assertEquals(MapDownloaderUiState.Loading(root), awaitItem())
            expectNoEvents()

            remote.gate?.complete(Unit)
            assertEquals(MapDownloaderUiState.Listed(root, listOf(europeFolder)), awaitItem())
        }
    }

    @Test
    fun uiState_mapsInstalledInTheSameFolder_areMarked() = runTest {
        val viewModel = viewModel(SavedStateHandle(mapOf("folder" to "europe")))

        viewModel.uiState.test {
            skipItems(1)
            // Spain is installed, but in another folder: a download would not clash with it.
            assertEquals(
                MapDownloaderUiState.Listed(europe, listOf(italyFolder, andorra, spain), setOf("andorra.map")),
                awaitItem(),
            )
        }
    }

    @Test
    fun uiState_mapFolderUnreadable_marksNothing() = runTest {
        installed.result = InstalledMapFiles.FolderUnreadable
        val viewModel = viewModel(SavedStateHandle(mapOf("folder" to "europe")))

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(MapDownloaderUiState.Listed(europe, listOf(italyFolder, andorra, spain)), awaitItem())
        }
    }

    @Test
    fun uiState_mapInstalledWhileShown_isMarked() = runTest {
        installed.result = InstalledMapFiles.Listed(emptyList())

        inEurope().uiState.test {
            skipItems(2)

            // E.g. its download finished.
            installed.result = InstalledMapFiles.Listed(
                listOf(testInstalledFile("spain.map").copy(path = "/maps/europe/spain.map", folder = "europe")),
            )

            assertEquals(
                MapDownloaderUiState.Listed(europe, listOf(italyFolder, andorra, spain), setOf("spain.map")),
                awaitItem(),
            )
        }
    }

    @Test
    fun uiState_downloadsOfTheFolder_areShownByName() = runTest {
        val running = MapDownload.Running(RemoteMapPath(europe, "spain.map"), FileSize(10), FileSize(100))
        val failed = MapDownload.Failed(RemoteMapPath(europe, "andorra.map"), MapDownloadFailure.NOT_ENOUGH_SPACE)
        downloads.current.value = listOf(running, failed)

        inEurope().uiState.test {
            skipItems(1)
            assertEquals(
                MapDownloaderUiState.Listed(
                    europe,
                    listOf(italyFolder, andorra, spain),
                    setOf("andorra.map"),
                    mapOf("spain.map" to running, "andorra.map" to failed),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun uiState_downloadsOfOtherFoldersOrMaps_areLeftOut() = runTest {
        downloads.current.value = listOf(
            // Same name, other folder.
            MapDownload.Queued(RemoteMapPath(root, "spain.map")),
            // Same folder, a map the folder does not list.
            MapDownload.Queued(RemoteMapPath(europe, "gone.map")),
        )

        inEurope().uiState.test {
            skipItems(1)
            assertEquals(
                MapDownloaderUiState.Listed(europe, listOf(italyFolder, andorra, spain), setOf("andorra.map")),
                awaitItem(),
            )
        }
    }

    @Test
    fun uiState_downloadChangesWhileShown_followsIt() = runTest {
        val map = RemoteMapPath(europe, "spain.map")
        downloads.current.value = listOf(MapDownload.Queued(map))

        inEurope().uiState.test {
            skipItems(2)

            val running = MapDownload.Running(map, FileSize(50), FileSize(100))
            downloads.current.value = listOf(running)

            assertEquals(mapOf("spain.map" to running), (awaitItem() as MapDownloaderUiState.Listed).downloads)
        }
    }

    @Test
    fun download_onWiFi_startsItInTheFolderShownWithoutMobileData() = runTest {
        val viewModel = inEurope()

        viewModel.uiState.test {
            skipItems(2)

            viewModel.download(spain)
            testScheduler.advanceUntilIdle()

            expectNoEvents()
        }
        assertEquals(listOf(spainPath to false), downloads.started)
    }

    @Test
    fun download_offWiFi_asksFirstWithoutStarting() = runTest {
        onWiFi = false
        val viewModel = inEurope()

        val state = viewModel.listedAfter { download(spain) }

        assertEquals(spainPath, state.mobileDataQuestion)
        assertTrue(downloads.started.isEmpty())
    }

    @Test
    fun answerMobileData_yes_startsItWithMobileDataAndClosesTheQuestion() = runTest {
        val viewModel = inEurope("mobile_data_question" to spainPath.toString())

        val state = viewModel.listedAfter { answerMobileData(useMobileData = true) }
        testScheduler.advanceUntilIdle()

        assertNull(state.mobileDataQuestion)
        assertEquals(listOf(spainPath to true), downloads.started)
    }

    @Test
    fun answerMobileData_no_startsItWaitingForWiFi() = runTest {
        val viewModel = inEurope("mobile_data_question" to spainPath.toString())

        viewModel.listedAfter { answerMobileData(useMobileData = false) }
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(spainPath to false), downloads.started)
    }

    @Test
    fun answerMobileData_withoutQuestion_doesNothing() = runTest {
        val viewModel = inEurope()

        viewModel.uiState.test {
            skipItems(2)
            viewModel.answerMobileData(useMobileData = true)
            testScheduler.advanceUntilIdle()
            expectNoEvents()
        }
        assertTrue(downloads.started.isEmpty())
    }

    @Test
    fun dismissMobileData_closesTheQuestionWithoutStarting() = runTest {
        val viewModel = inEurope("mobile_data_question" to spainPath.toString())

        val state = viewModel.listedAfter { dismissMobileData() }
        testScheduler.advanceUntilIdle()

        assertNull(state.mobileDataQuestion)
        assertTrue(downloads.started.isEmpty())
    }

    @Test
    fun mobileDataQuestion_isSavedSoItSurvivesTheProcess() = runTest {
        onWiFi = false
        val savedState = SavedStateHandle(mapOf("folder" to "europe"))
        val viewModel = viewModel(savedState)

        viewModel.listedAfter { download(spain) }

        assertEquals(spainPath.toString(), savedState.get<String>("mobile_data_question"))
    }

    @Test
    fun cancel_map_cancelsItInTheFolderShown() = runTest {
        val viewModel = inEurope()

        viewModel.uiState.test {
            skipItems(2)
            viewModel.cancel(spain)
            testScheduler.advanceUntilIdle()
        }
        assertEquals(listOf(spainPath), downloads.cancelled)
    }

    @Test
    fun uiState_failure_isFailedWithItsReason() = runTest {
        remote.results[root] = RemoteListing.Failed(RemoteListingFailure.NO_CONNECTION)

        viewModel().uiState.test {
            skipItems(1)
            assertEquals(MapDownloaderUiState.Failed(root, RemoteListingFailure.NO_CONNECTION), awaitItem())
        }
    }

    @Test
    fun open_folder_readsAndShowsIt() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(2)
            viewModel.open(europeFolder)

            assertEquals(MapDownloaderUiState.Loading(europe), awaitItem())
            assertEquals(
                MapDownloaderUiState.Listed(europe, listOf(italyFolder, andorra, spain), setOf("andorra.map")),
                awaitItem(),
            )
        }
        assertEquals(listOf(root, europe), remote.listed)
    }

    @Test
    fun open_folder_isSavedSoItSurvivesTheProcess() = runTest {
        val savedState = SavedStateHandle()
        val viewModel = viewModel(savedState)
        viewModel.uiState.test {
            skipItems(2)
            viewModel.open(europeFolder)
            skipItems(2)
        }

        assertEquals(MapDownloaderUiState.Loading(europe), viewModel(savedState).uiState.value)
    }

    @Test
    fun open_folderInsideASubFolder_addsItToThePath() = runTest {
        val viewModel = viewModel(SavedStateHandle(mapOf("folder" to "europe")))

        viewModel.uiState.test {
            skipItems(2)
            viewModel.open(italyFolder)

            assertEquals(MapDownloaderUiState.Loading(italy), awaitItem())
            assertEquals(MapDownloaderUiState.Listed(italy, emptyList()), awaitItem())
        }
    }

    @Test
    fun goUp_inASubFolder_showsItsParent() = runTest {
        val viewModel = viewModel(SavedStateHandle(mapOf("folder" to "europe/italy")))

        viewModel.uiState.test {
            skipItems(2)
            viewModel.goUp()

            assertEquals(MapDownloaderUiState.Loading(europe), awaitItem())
            assertEquals(
                MapDownloaderUiState.Listed(europe, listOf(italyFolder, andorra, spain), setOf("andorra.map")),
                awaitItem(),
            )
        }
    }

    @Test
    fun goUp_inTheRoot_doesNothing() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(2)
            viewModel.goUp()

            expectNoEvents()
        }
        assertEquals(listOf(root), remote.listed)
    }

    @Test
    fun retry_readsTheFolderAgain() = runTest {
        remote.results[root] = RemoteListing.Failed(RemoteListingFailure.SERVER_ERROR)
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(2)
            remote.results[root] = RemoteListing.Listed(listOf(europeFolder))
            viewModel.retry()

            assertEquals(MapDownloaderUiState.Loading(root), awaitItem())
            assertEquals(MapDownloaderUiState.Listed(root, listOf(europeFolder)), awaitItem())
        }
        assertEquals(listOf(root, root), remote.listed)
    }
}
