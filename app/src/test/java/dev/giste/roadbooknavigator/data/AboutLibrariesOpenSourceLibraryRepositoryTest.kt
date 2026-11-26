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

package dev.giste.roadbooknavigator.data

import android.content.res.Resources
import dev.giste.roadbooknavigator.FakeLogger
import dev.giste.roadbooknavigator.domain.LibraryLicence
import dev.giste.roadbooknavigator.domain.OpenSourceLibrariesResult
import dev.giste.roadbooknavigator.domain.OpenSourceLibrary
import java.io.IOException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** AboutLibraries parses its JSON with Android's org.json, so these tests run on Robolectric. */
@RunWith(RobolectricTestRunner::class)
class AboutLibrariesOpenSourceLibraryRepositoryTest {

    private val logger = FakeLogger()

    private val sample: String = checkNotNull(javaClass.getResource("/about/aboutlibraries_sample.json")).readText()

    private val apache = LibraryLicence(
        name = "Apache License 2.0",
        url = "https://spdx.org/licenses/Apache-2.0.html",
        text = "Apache License text",
    )

    /** A reader of [readDefinitions] on this test's scheduler. */
    private fun TestScope.reader(readDefinitions: () -> String) = AboutLibrariesOpenSourceLibraryRepository(
        readDefinitions = readDefinitions,
        ioDispatcher = StandardTestDispatcher(testScheduler),
        logger = logger,
    )

    @Test
    fun parseOpenSourceLibraries_sample_sortsByNameIgnoringCase() {
        val libraries = parseOpenSourceLibraries(sample)

        assertEquals(listOf("another library", "OkHttp", "VTM"), libraries.map { it.name })
    }

    @Test
    fun parseOpenSourceLibraries_sample_mapsIdVersionAndLicence() {
        val okHttp = parseOpenSourceLibraries(sample).single { it.name == "OkHttp" }

        assertEquals(
            OpenSourceLibrary("com.squareup.okhttp3:okhttp", "OkHttp", "5.5.1", listOf(apache)),
            okHttp,
        )
    }

    @Test
    fun parseOpenSourceLibraries_blankVersionUrlAndText_areUnknown() {
        val library = parseOpenSourceLibraries(sample).single { it.name == "another library" }

        assertEquals(null, library.version)
        assertEquals(LibraryLicence(name = "MIT License", url = null, text = null), library.licences[1])
    }

    @Test
    fun parseOpenSourceLibraries_severalLicences_sortsThemByName() {
        val library = parseOpenSourceLibraries(sample).single { it.name == "another library" }

        assertEquals(listOf("Apache License 2.0", "MIT License"), library.licences.map { it.name })
    }

    @Test
    fun parseOpenSourceLibraries_invalidJson_isEmpty() {
        assertEquals(emptyList<OpenSourceLibrary>(), parseOpenSourceLibraries("not json"))
    }

    @Test
    fun read_validList_givesTheLibraries() = runTest {
        val result = reader { sample }.read()

        assertEquals(OpenSourceLibrariesResult.Read(parseOpenSourceLibraries(sample)), result)
    }

    @Test
    fun read_readFails_isUnavailableAndLogged() = runTest {
        val failure = IOException("disk")

        val result = reader { throw failure }.read()

        assertEquals(OpenSourceLibrariesResult.Unavailable, result)
        assertEquals(listOf<Throwable>(failure), logger.loggedErrors)
    }

    @Test
    fun read_resourceMissing_isUnavailableAndLogged() = runTest {
        val failure = Resources.NotFoundException("aboutlibraries")

        val result = reader { throw failure }.read()

        assertEquals(OpenSourceLibrariesResult.Unavailable, result)
        assertEquals(listOf<Throwable>(failure), logger.loggedErrors)
    }

    @Test
    fun read_invalidJson_isUnavailableAndLogged() = runTest {
        val result = reader { "not json" }.read()

        assertEquals(OpenSourceLibrariesResult.Unavailable, result)
        assertTrue(logger.loggedWarnings.isNotEmpty())
    }

    @Test
    fun read_emptyList_isUnavailable() = runTest {
        val result = reader { """{"libraries": [], "licenses": {}}""" }.read()

        assertEquals(OpenSourceLibrariesResult.Unavailable, result)
    }
}
