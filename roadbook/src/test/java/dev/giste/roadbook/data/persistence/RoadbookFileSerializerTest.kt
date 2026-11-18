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

import dev.giste.roadbook.FakeLogger
import dev.giste.roadbook.domain.roadbook.SavedRoadbook
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class RoadbookFileSerializerTest {

    private val logger = FakeLogger()

    /** The serializer under test, on the dispatcher of the test. */
    private fun TestScope.serializer() =
        RoadbookFileSerializer(logger, StandardTestDispatcher(testScheduler))

    /** Writes DTOs the way the serializer does, to build files the domain could not produce. */
    private val dtoJson = Json { classDiscriminator = "type" }

    private suspend fun TestScope.write(saved: SavedRoadbook): String =
        ByteArrayOutputStream()
            .also { serializer().writeTo(saved, it) }
            .toByteArray()
            .decodeToString()

    private suspend fun TestScope.read(content: String): SavedRoadbook =
        serializer().readFrom(content.byteInputStream())

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource(name)) { "Missing test resource $name" }
            .readText()

    /** The stored form of [FULL_ROADBOOK], to change one value in it. */
    private fun storedFullRoadbook(): StoredRoadbookDto = FULL_ROADBOOK.toStored()

    private fun fileOf(roadbook: StoredRoadbookDto, version: Int = 1): String =
        dtoJson.encodeToString(StoredRoadbookFileDto(version = version, roadbook = roadbook))

    @Test
    fun writeTo_thenReadFrom_restoresTheSameRoadbook() = runTest {
        val content = write(SavedRoadbook.Available(FULL_ROADBOOK))

        assertEquals(SavedRoadbook.Available(FULL_ROADBOOK), read(content))
    }

    @Test
    fun writeTo_available_writesCurrentVersion() = runTest {
        val content = write(SavedRoadbook.Available(FULL_ROADBOOK))

        assertEquals(
            JsonPrimitive(RoadbookFileSerializer.CURRENT_VERSION),
            Json.parseToJsonElement(content).jsonObject["version"],
        )
    }

    @Test
    fun writeTo_none_writesNothing() = runTest {
        assertEquals("", write(SavedRoadbook.None))
    }

    @Test
    fun writeTo_unreadable_writesNothing() = runTest {
        assertEquals("", write(SavedRoadbook.Unreadable))
    }

    @Test
    fun readFrom_emptyFile_isNone() = runTest {
        assertEquals(SavedRoadbook.None, read(""))
    }

    @Test
    fun defaultValue_isNone() = runTest {
        assertEquals(SavedRoadbook.None, serializer().defaultValue)
    }

    @Test
    fun readFrom_version1Fixture_readsExpectedRoadbook() = runTest {
        val result = read(fixture("stored_roadbook_v1.json"))

        assertEquals(SavedRoadbook.Available(FULL_ROADBOOK), result)
    }

    @Test
    fun writeTo_expectedRoadbook_matchesVersion1Fixture() = runTest {
        // Fails when the format changes. If the change is wanted, raise CURRENT_VERSION and decide
        // what happens to files of the previous version, instead of only updating the fixture.
        val content = write(SavedRoadbook.Available(FULL_ROADBOOK))

        assertEquals(
            Json.parseToJsonElement(fixture("stored_roadbook_v1.json")),
            Json.parseToJsonElement(content),
        )
    }

    @Test
    fun readFrom_invalidJson_isUnreadableAndWarns() = runTest {
        assertEquals(SavedRoadbook.Unreadable, read("{not json"))
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun readFrom_otherVersion_isUnreadableAndWarns() = runTest {
        val content = fileOf(storedFullRoadbook(), version = 2)

        assertEquals(SavedRoadbook.Unreadable, read(content))
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun readFrom_otherVersionWithOtherLayout_isUnreadable() = runTest {
        assertEquals(SavedRoadbook.Unreadable, read("""{"version":2,"stages":[]}"""))
    }

    @Test
    fun readFrom_noVersion_isUnreadable() = runTest {
        val content = dtoJson.encodeToString(storedFullRoadbook())

        assertEquals(SavedRoadbook.Unreadable, read(content))
    }

    @Test
    fun readFrom_unknownField_isUnreadable() = runTest {
        val file = Json.parseToJsonElement(fileOf(storedFullRoadbook())).jsonObject
        val content = JsonObject(file + ("extra" to JsonPrimitive(true))).toString()

        assertEquals(SavedRoadbook.Unreadable, read(content))
    }

    @Test
    fun readFrom_brokenRoadbookInvariant_isUnreadable() = runTest {
        // Waypoints numbered 1, 1 instead of 1, 2.
        val stored = storedFullRoadbook()
        val renumbered = stored.copy(
            waypoints = stored.waypoints.map { it.copy(number = 1) },
        )

        assertEquals(SavedRoadbook.Unreadable, read(fileOf(renumbered)))
    }

    @Test
    fun readFrom_invalidValue_isUnreadable() = runTest {
        val stored = storedFullRoadbook()
        val negative = stored.copy(
            waypoints = stored.waypoints.map { it.copy(partialMeters = -1.0) },
        )

        assertEquals(SavedRoadbook.Unreadable, read(fileOf(negative)))
    }

    @Test
    fun readFrom_unknownDangerLevel_isUnreadable() = runTest {
        val stored = storedFullRoadbook()
        val unknown = stored.copy(
            waypoints = stored.waypoints.map { it.copy(dangerLevel = "EXTREME") },
        )

        assertEquals(SavedRoadbook.Unreadable, read(fileOf(unknown)))
    }

    @Test
    fun readFrom_unknownRoadType_isUnreadable() = runTest {
        val road = storedRoad(roadType = "MOTORWAY")

        assertEquals(SavedRoadbook.Unreadable, read(fileOf(withTulipOfFirstWaypoint(road))))
    }

    @Test
    fun readFrom_unknownIconType_isUnreadable() = runTest {
        val icon = StoredIconDto(
            iconType = "CAMEL",
            width = 50.0,
            height = 50.0,
            center = StoredCanvasPointDto(0.0, 0.0),
            angle = 0,
            scaleX = 1.0,
            scaleY = 1.0,
            originalId = null,
            originalName = null,
        )

        assertEquals(SavedRoadbook.Unreadable, read(fileOf(withTulipOfFirstWaypoint(icon))))
    }

    @Test
    fun readFrom_roadInNotes_isUnreadable() = runTest {
        val road = storedRoad(roadType = "TRACK")

        assertEquals(SavedRoadbook.Unreadable, read(fileOf(withNotesOfFirstWaypoint(road))))
    }

    @Test
    fun readFrom_trackInNotes_isUnreadable() = runTest {
        val road = storedRoad(roadType = "TRACK")
        val track = StoredTrackDto(roadIn = road, roadOut = road)

        assertEquals(SavedRoadbook.Unreadable, read(fileOf(withNotesOfFirstWaypoint(track))))
    }

    /** A stored straight road without ends, of [roadType]. */
    private fun storedRoad(roadType: String) =
        StoredRoadDto(start = null, end = null, roadType = roadType, handles = emptyList())

    /** [FULL_ROADBOOK] stored, with [element] as the only element of the first tulip. */
    private fun withTulipOfFirstWaypoint(element: StoredElementDto): StoredRoadbookDto {
        val stored = storedFullRoadbook()
        val first = stored.waypoints.first().copy(tulip = listOf(element))
        return stored.copy(waypoints = listOf(first) + stored.waypoints.drop(1))
    }

    /** [FULL_ROADBOOK] stored, with [element] as the only element of the first notes column. */
    private fun withNotesOfFirstWaypoint(element: StoredElementDto): StoredRoadbookDto {
        val stored = storedFullRoadbook()
        val first = stored.waypoints.first().copy(notes = listOf(element))
        return stored.copy(waypoints = listOf(first) + stored.waypoints.drop(1))
    }
}
