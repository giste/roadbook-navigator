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
import android.content.res.Resources
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import dev.giste.core.logging.Logger
import dev.giste.roadbooknavigator.R
import dev.giste.roadbooknavigator.domain.LibraryLicence
import dev.giste.roadbooknavigator.domain.OpenSourceLibrariesResult
import dev.giste.roadbooknavigator.domain.OpenSourceLibrary
import dev.giste.roadbooknavigator.domain.OpenSourceLibraryRepository
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Reads the library list the AboutLibraries Gradle plugin generates at build time (every
 * dependency of the app with its licences, see the app's README).
 *
 * @param readDefinitions reads the generated JSON. It may throw [IOException] or
 * [Resources.NotFoundException]. A parameter so tests can give broken input.
 * @param ioDispatcher where the file is read and parsed.
 * @param logger reports why the list could not be read.
 */
internal class AboutLibrariesOpenSourceLibraryRepository(
    private val readDefinitions: () -> String,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger,
) : OpenSourceLibraryRepository {

    override suspend fun read(): OpenSourceLibrariesResult = withContext(ioDispatcher) {
        // Only the failures of reading the resource are expected. AboutLibraries itself does not
        // throw on invalid JSON: it logs it and gives an empty list, handled below.
        val json = try {
            readDefinitions()
        } catch (e: IOException) {
            logger.e(e, "Cannot read the open-source library list")
            return@withContext OpenSourceLibrariesResult.Unavailable
        } catch (e: Resources.NotFoundException) {
            logger.e(e, "The open-source library list is missing from the build")
            return@withContext OpenSourceLibrariesResult.Unavailable
        }
        val libraries = parseOpenSourceLibraries(json)
        if (libraries.isEmpty()) {
            logger.w("The open-source library list is empty or invalid")
            OpenSourceLibrariesResult.Unavailable
        } else {
            OpenSourceLibrariesResult.Read(libraries)
        }
    }
}

/**
 * Reads the library list the AboutLibraries plugin put in the app's raw resources.
 *
 * @receiver the app's context.
 * @return the JSON file's text.
 * @throws IOException if the file cannot be read.
 * @throws Resources.NotFoundException if the build did not include it.
 */
internal fun Context.readLibraryDefinitions(): String =
    resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { it.readText() }

/**
 * Turns the AboutLibraries JSON into the About screen's libraries.
 *
 * @param json the JSON the AboutLibraries plugin generates.
 * @return every library, sorted by name, ignoring case; empty if [json] is invalid.
 */
internal fun parseOpenSourceLibraries(json: String): List<OpenSourceLibrary> =
    Libs.Builder().withJson(json).build().libraries
        .map { it.toOpenSourceLibrary() }
        .sortedBy { it.name.lowercase() }

/** This library, as the About screen shows it. */
private fun Library.toOpenSourceLibrary(): OpenSourceLibrary = OpenSourceLibrary(
    id = uniqueId,
    name = name,
    version = artifactVersion?.takeIf { it.isNotBlank() },
    licences = licenses.map { it.toLibraryLicence() }.sortedBy { it.name.lowercase() },
)

/** This licence, as the About screen shows it. Blank values count as unknown. */
private fun License.toLibraryLicence(): LibraryLicence = LibraryLicence(
    name = name,
    url = url?.takeIf { it.isNotBlank() },
    text = licenseContent?.takeIf { it.isNotBlank() },
)
