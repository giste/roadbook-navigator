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

package dev.giste.roadbook.data.source

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.giste.core.coroutines.IoDispatcher
import dev.giste.core.logging.Logger
import dev.giste.roadbook.domain.roadbook.ReadRoadbookSourceResult
import dev.giste.roadbook.domain.roadbook.RoadbookSource
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * [RoadbookSource] that reads the file at [uri] through a [ContentResolver].
 *
 * It reads `content://` URIs, such as the documents the system file picker returns, and
 * `file://` URIs. The file is read as UTF-8 text, which is what `.rn2` files are.
 *
 * This is the data boundary for the file system, so every failure to open or read the file is
 * caught here and returned as [ReadRoadbookSourceResult.Unreadable]. The URI is not logged: its
 * file name may be personal data.
 *
 * Create it with [ContentResolverRoadbookSourceFactory].
 *
 * @param uri where the file is.
 * @param contentResolver opens [uri].
 * @param ioDispatcher the dispatcher the file is read on.
 * @param logger logs why the file could not be read.
 */
internal class ContentResolverRoadbookSource(
    private val uri: Uri,
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger,
) : RoadbookSource {

    override suspend fun read(): ReadRoadbookSourceResult = withContext(ioDispatcher) {
        try {
            // A provider that has crashed may give no stream instead of throwing.
            val stream = contentResolver.openInputStream(uri) ?: run {
                logger.w("The roadbook file could not be opened: no stream")
                return@withContext ReadRoadbookSourceResult.Unreadable
            }
            ReadRoadbookSourceResult.Success(stream.bufferedReader().use { it.readText() })
        } catch (error: IOException) {
            // Includes FileNotFoundException, for a file that is gone or a provider that is
            // unknown.
            logger.e(error, "The roadbook file could not be read")
            ReadRoadbookSourceResult.Unreadable
        } catch (error: SecurityException) {
            // The permission to read a picked document is lost, e.g. after a restart, unless the
            // app took a persistable permission.
            logger.e(error, "The app may not read the roadbook file")
            ReadRoadbookSourceResult.Unreadable
        }
    }
}

/**
 * Creates the [ContentResolverRoadbookSource] of a URI, with the dependencies Hilt provides.
 *
 * @param context the application context, whose [ContentResolver] opens the files.
 * @param ioDispatcher the dispatcher files are read on.
 * @param logger logs why a file could not be read.
 */
internal class ContentResolverRoadbookSourceFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger,
) {

    /**
     * Creates the source of the file at [uri]. Nothing is opened until it is read.
     *
     * @param uri where the file is.
     * @return the source that reads it.
     */
    fun create(uri: Uri): RoadbookSource =
        ContentResolverRoadbookSource(uri, context.contentResolver, ioDispatcher, logger)
}
