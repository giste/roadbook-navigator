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

import app.cash.turbine.test
import dev.giste.map.MainDispatcherRule
import dev.giste.map.domain.DeleteMapResult
import dev.giste.map.domain.FakeMapFileRepository
import dev.giste.map.domain.InstalledMapFiles
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

    private fun viewModel() = MapManagerViewModel(repository)

    @Test
    fun uiState_beforeTheFilesAreFound_isLoading() {
        assertEquals(MapManagerUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun uiState_files_areListedReadableOrNot() = runTest {
        viewModel().uiState.test {
            assertEquals(MapManagerUiState.Loading, awaitItem())
            assertEquals(MapManagerUiState.Listed(listOf(damaged, spain)), awaitItem())
        }
    }

    @Test
    fun uiState_noFiles_isAnEmptyList() = runTest {
        repository.result = InstalledMapFiles.Listed(emptyList())

        viewModel().uiState.test {
            skipItems(1)
            assertEquals(MapManagerUiState.Listed(emptyList()), awaitItem())
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

            assertEquals(MapManagerUiState.Listed(listOf(spain)), awaitItem())
        }
        assertEquals(1, repository.collections)
    }

    @Test
    fun delete_failed_saysWhichFileAndListsTheFilesAgain() = runTest {
        repository.deleteResult = DeleteMapResult.Failed
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(2)

            viewModel.delete(spain)

            assertEquals(MapManagerUiState.Listed(listOf(damaged, spain), deleteFailed = spain), awaitItem())
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

            assertEquals(MapManagerUiState.Listed(listOf(damaged, spain)), awaitItem())
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
            assertEquals(MapManagerUiState.Listed(emptyList()), awaitItem())
        }
        assertEquals(2, repository.collections)
    }

    @Test
    fun uiState_resubscribedWithinTheTimeout_keepsTheFiles() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test { skipItems(2) }
        testScheduler.advanceTimeBy(1_000)

        viewModel.uiState.test {
            assertEquals(MapManagerUiState.Listed(listOf(damaged, spain)), awaitItem())
        }
        assertEquals(1, repository.collections)
    }
}
