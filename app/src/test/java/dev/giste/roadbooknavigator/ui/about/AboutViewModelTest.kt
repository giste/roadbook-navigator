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

package dev.giste.roadbooknavigator.ui.about

import app.cash.turbine.test
import dev.giste.roadbooknavigator.MainDispatcherRule
import dev.giste.roadbooknavigator.domain.FakeOpenSourceLibraryRepository
import dev.giste.roadbooknavigator.domain.LibraryLicence
import dev.giste.roadbooknavigator.domain.OpenSourceLibrariesResult
import dev.giste.roadbooknavigator.domain.OpenSourceLibrary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AboutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val libraries = FakeOpenSourceLibraryRepository()

    // Created in each test, after the rule has replaced Main.
    private val viewModel by lazy { AboutViewModel(libraries) }

    private val okHttp = OpenSourceLibrary(
        id = "com.squareup.okhttp3:okhttp",
        name = "OkHttp",
        version = "5.5.1",
        licences = listOf(LibraryLicence("Apache License 2.0", url = null, text = "Apache")),
    )

    @Test
    fun uiState_beforeTheListIsRead_isLoading() = runTest {
        viewModel.uiState.test {
            assertEquals(AboutUiState.Loading, awaitItem())
        }
    }

    @Test
    fun uiState_listRead_isLoadedWithTheLibraries() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            libraries.complete(OpenSourceLibrariesResult.Read(listOf(okHttp)))

            assertEquals(AboutUiState.Loaded(listOf(okHttp)), awaitItem())
        }
    }

    @Test
    fun uiState_listUnavailable_isLibrariesUnavailable() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            libraries.complete(OpenSourceLibrariesResult.Unavailable)

            assertEquals(AboutUiState.LibrariesUnavailable, awaitItem())
        }
    }

    @Test
    fun uiState_neverCollected_doesNotReadTheList() = runTest {
        viewModel.uiState

        assertEquals(0, libraries.reads)
    }
}
