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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.giste.roadbooknavigator.domain.OpenSourceLibrary
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The library list the AboutLibraries plugin generates for this build, read from the app's raw
 * resources as the About screen reads it.
 */
@RunWith(RobolectricTestRunner::class)
class AboutLibrariesOpenSourceLibraryRepositoryIntegrationTest {

    private val libraries: List<OpenSourceLibrary> by lazy {
        parseOpenSourceLibraries(
            ApplicationProvider.getApplicationContext<Context>().readLibraryDefinitions(),
        )
    }

    @Test
    fun generatedList_includesTheAppsLibraries() {
        val ids = libraries.map { it.id }

        listOf(
            "com.google.dagger:hilt-android",
            "com.squareup.okhttp3:okhttp",
            "com.jakewharton.timber:timber",
            "org.mapsforge:vtm",
            "org.mapsforge:vtm-android",
        ).forEach { assertTrue("$it is missing", it in ids) }
    }

    @Test
    fun generatedList_everyLibrary_hasALicenceWithItsText() {
        libraries.forEach { library ->
            assertTrue("${library.id} has no licence", library.licences.isNotEmpty())
            library.licences.forEach { licence ->
                assertTrue("${library.id}: ${licence.name} has no text", licence.text != null)
            }
        }
    }

    @Test
    fun generatedList_names_areUnique() {
        val repeated = libraries.groupBy { it.name }.filterValues { it.size > 1 }.keys

        assertTrue("Repeated names: $repeated", repeated.isEmpty())
    }

    @Test
    fun generatedList_vtm_isUnderTheLgpl() {
        val vtm = libraries.single { it.id == "org.mapsforge:vtm" }

        assertTrue(vtm.licences.single().name.contains("Lesser General Public License v3.0"))
        assertTrue(vtm.licences.single().text!!.startsWith("GNU LESSER GENERAL PUBLIC LICENSE"))
    }
}
