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

package dev.giste.roadbook.data.persistence

import androidx.datastore.core.Serializer
import dev.giste.core.coroutines.IoDispatcher
import dev.giste.core.logging.Logger
import dev.giste.roadbook.domain.roadbook.SavedRoadbook
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Reads and writes the roadbook file of the DataStore, as versioned JSON (see
 * [StoredRoadbookFileDto]).
 *
 * Reading never fails for the content of the file: an empty file is [SavedRoadbook.None], and a
 * file that cannot be read back, because it is damaged, has a version this one does not know, or
 * breaks a domain invariant, is [SavedRoadbook.Unreadable]. Throwing a `CorruptionException`
 * instead would make DataStore replace the file, and the user would not know their roadbook was
 * lost. Only I/O errors of the stream reach DataStore.
 *
 * Reading and writing the streams block, so both run on [ioDispatcher]. DataStore already calls
 * them on the dispatcher of its scope, which is the same one in production, but the serializer
 * does not rely on how its DataStore was created: switching to the dispatcher it is already on
 * costs almost nothing.
 *
 * @param logger logs why a file could not be read back.
 * @param ioDispatcher the dispatcher the streams are read and written on.
 */
internal class RoadbookFileSerializer @Inject constructor(
    private val logger: Logger,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : Serializer<SavedRoadbook> {

    /** Strict on purpose: the file was written by this serializer, so anything unknown is wrong. */
    private val json = Json { classDiscriminator = CLASS_DISCRIMINATOR }

    /** Reads only the version, so a file of another version is recognized before decoding it. */
    private val versionJson = Json { ignoreUnknownKeys = true }

    override val defaultValue: SavedRoadbook = SavedRoadbook.None

    override suspend fun readFrom(input: InputStream): SavedRoadbook {
        val content = withContext(ioDispatcher) { input.readBytes() }.decodeToString()
        if (content.isEmpty()) return SavedRoadbook.None
        return try {
            val version = versionJson.decodeFromString<StoredVersionDto>(content).version
            if (version != CURRENT_VERSION) {
                logger.w("The saved roadbook has format version %d, which cannot be read", version)
                return SavedRoadbook.Unreadable
            }
            val file = json.decodeFromString<StoredRoadbookFileDto>(content)
            SavedRoadbook.Available(file.roadbook.toDomain())
        } catch (error: IllegalArgumentException) {
            // kotlinx.serialization reports bad JSON, missing fields and wrong types as
            // SerializationException, which is an IllegalArgumentException, like the domain
            // invariants that toDomain() checks.
            logger.w("The saved roadbook cannot be read: %s", error.message)
            SavedRoadbook.Unreadable
        }
    }

    override suspend fun writeTo(t: SavedRoadbook, output: OutputStream) {
        when (t) {
            is SavedRoadbook.Available -> {
                val file = StoredRoadbookFileDto(
                    version = CURRENT_VERSION,
                    roadbook = t.roadbook.toStored(),
                )
                val bytes = json.encodeToString(file).encodeToByteArray()
                withContext(ioDispatcher) { output.write(bytes) }
            }
            // An empty file is read back as None. The repository never writes Unreadable, but if
            // it did, an empty file is the honest content: there is no roadbook to keep.
            SavedRoadbook.None, SavedRoadbook.Unreadable -> Unit
        }
    }

    internal companion object {

        /** Name of the DataStore file. Prefixed with the module name (AGENTS.md §7). */
        const val FILE_NAME: String = "roadbook_route.json"

        /** The version of the format this serializer writes, and the only one it reads. */
        const val CURRENT_VERSION: Int = 1

        /** The JSON field that holds the subtype of an element. */
        private const val CLASS_DISCRIMINATOR: String = "type"
    }
}
