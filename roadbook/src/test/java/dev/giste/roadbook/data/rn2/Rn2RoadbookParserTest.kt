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

package dev.giste.roadbook.data.rn2

import dev.giste.geo.Altitude
import dev.giste.geo.Distance
import dev.giste.geo.GeoPoint
import dev.giste.roadbook.FakeLogger
import dev.giste.roadbook.domain.drawing.Icon
import dev.giste.roadbook.domain.drawing.IconType
import dev.giste.roadbook.domain.drawing.RoadType
import dev.giste.roadbook.domain.drawing.Track
import dev.giste.roadbook.domain.roadbook.DangerLevel
import dev.giste.roadbook.domain.roadbook.ParseRoadbookFailure
import dev.giste.roadbook.domain.roadbook.ParseRoadbookResult
import dev.giste.roadbook.domain.roadbook.Roadbook
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Rn2RoadbookParserTest {

    private val logger = FakeLogger()

    // Failures

    @Test
    fun parse_notJson_isMalformed() = runTest {
        assertFailure(ParseRoadbookFailure.MALFORMED, parser().parse("this is not a roadbook"))
    }

    @Test
    fun parse_jsonWithoutRoute_isMalformed() = runTest {
        assertFailure(ParseRoadbookFailure.MALFORMED, parser().parse("""{"name": "x"}"""))
    }

    @Test
    fun parse_pointWithoutLatitude_isMalformed() = runTest {
        val content = rn2("""{"lon": -3.0, "show": true}""")

        assertFailure(ParseRoadbookFailure.MALFORMED, parser().parse(content))
    }

    @Test
    fun parse_pointWithoutShow_isMalformed() = runTest {
        val content = rn2("""{"lat": 40.0, "lon": -3.0}""")

        assertFailure(ParseRoadbookFailure.MALFORMED, parser().parse(content))
    }

    @Test
    fun parse_impossibleLatitude_isMalformed() = runTest {
        val content = rn2(point(lat = 120.0, lon = -3.0))

        assertFailure(ParseRoadbookFailure.MALFORMED, parser().parse(content))
    }

    @Test
    fun parse_impossibleLongitude_isMalformed() = runTest {
        val content = rn2(point(lat = 40.0, lon = 200.0))

        assertFailure(ParseRoadbookFailure.MALFORMED, parser().parse(content))
    }

    @Test
    fun parse_impossiblePositionOnAHiddenPoint_isMalformed() = runTest {
        val content = rn2(
            point(lat = 40.0, lon = -3.0),
            point(lat = -95.0, lon = -3.0, show = false),
        )

        assertFailure(ParseRoadbookFailure.MALFORMED, parser().parse(content))
    }

    @Test
    fun parse_elementThatIsNotAnObject_isMalformed() = runTest {
        val content = rn2(point(lat = 40.0, lon = -3.0, tulip = """["Road"]"""))

        assertFailure(ParseRoadbookFailure.MALFORMED, parser().parse(content))
    }

    @Test
    fun parse_malformedFile_logsAWarning() = runTest {
        parser().parse("{")

        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun parse_routeWithoutPoints_hasNoWaypoints() = runTest {
        assertFailure(ParseRoadbookFailure.NO_WAYPOINTS, parser().parse(rn2()))
    }

    @Test
    fun parse_noPointShown_hasNoWaypoints() = runTest {
        val content = rn2(point(lat = 40.0, lon = -3.0, show = false))

        assertFailure(ParseRoadbookFailure.NO_WAYPOINTS, parser().parse(content))
    }

    // Route

    @Test
    fun parse_metadata_isKept() = runTest {
        val content = """
            {"route": {
                "name": "Stage 1", "description": "Desert loop",
                "startlocation": "Erfoud", "endlocation": "Merzouga",
                "waypoints": [${point(lat = 40.0, lon = -3.0)}]
            }}
        """.trimIndent()

        val roadbook = parseRoadbook(content)

        assertEquals("Stage 1", roadbook.name)
        assertEquals("Desert loop", roadbook.description)
        assertEquals("Erfoud", roadbook.startLocation)
        assertEquals("Merzouga", roadbook.endLocation)
    }

    @Test
    fun parse_missingOrNullMetadata_isEmpty() = runTest {
        val waypoint = point(lat = 40.0, lon = -3.0)
        val content = """{"route": {"name": null, "waypoints": [$waypoint]}}"""

        val roadbook = parseRoadbook(content)

        assertEquals("", roadbook.name)
        assertEquals("", roadbook.description)
    }

    @Test
    fun parse_unknownKeys_areIgnored() = runTest {
        val content = """
            {"route": {"version": 4, "current_style": "x", "waypoints": [
                {"t_uuid": "a", "waypointid": 0, "lat": 40.0, "lon": -3.0, "show": true,
                 "tulip": {"elements": []}, "notes": {"elements": []}, "extra": 1}
            ]}, "other": true}
        """.trimIndent()

        assertEquals(1, parseRoadbook(content).waypoints.size)
    }

    @Test
    fun parse_shownPoints_becomeWaypointsAndHiddenOnesAddDistance() = runTest {
        val content = rn2(
            point(lat = 0.0, lon = 0.0),
            point(lat = 0.0, lon = 0.001, show = false),
            point(lat = 0.0, lon = 0.002),
        )

        val waypoints = parseRoadbook(content).waypoints

        assertEquals(2, waypoints.size)
        assertEquals(GeoPoint(0.0, 0.002), waypoints[1].position)
        assertEquals(
            2 * STEP_METERS,
            waypoints[1].distanceFromPrevious.inMeters,
            2 * DISTANCE_TOLERANCE_METERS,
        )
    }

    @Test
    fun parse_elevation_isTheAltitude() = runTest {
        val content = rn2(point(lat = 40.0, lon = -3.0, ele = 730.0))

        val waypoint = parseRoadbook(content).waypoints.single()

        assertEquals(Altitude.meters(730.0), waypoint.altitude)
    }

    @Test
    fun parse_noElevation_hasNoAltitude() = runTest {
        val waypoint = parseRoadbook(rn2(point(lat = 40.0, lon = -3.0))).waypoints.single()

        assertNull(waypoint.altitude)
    }

    // Symbols in the notes

    @Test
    fun parse_resetSymbolInNotes_resetsThePartial() = runTest {
        val content = rn2(
            point(lat = 0.0, lon = 0.0),
            point(lat = 0.0, lon = 0.001, notes = "[$RESET_ICON]"),
            point(lat = 0.0, lon = 0.002),
        )

        val waypoints = parseRoadbook(content).waypoints

        assertTrue(waypoints[1].resetsPartial)
        assertEquals(STEP_METERS, waypoints[2].partial.inMeters, DISTANCE_TOLERANCE_METERS)
    }

    @Test
    fun parse_resetSymbolOnAHiddenPoint_stillResetsThePartial() = runTest {
        val content = rn2(
            point(lat = 0.0, lon = 0.0),
            point(lat = 0.0, lon = 0.001, show = false, notes = "[$RESET_ICON]"),
            point(lat = 0.0, lon = 0.002),
        )

        val partial = parseRoadbook(content).waypoints[1].partial

        assertEquals(STEP_METERS, partial.inMeters, DISTANCE_TOLERANCE_METERS)
    }

    @Test
    fun parse_resetSymbolInTulip_doesNotResetThePartial() = runTest {
        val content = rn2(point(lat = 0.0, lon = 0.0, tulip = "[$RESET_ICON]"))

        assertFalse(parseRoadbook(content).waypoints.single().resetsPartial)
    }

    @Test
    fun parse_dangerSymbolsInNotes_setTheDangerLevel() = runTest {
        val levels = mapOf(1 to DangerLevel.LOW, 2 to DangerLevel.MEDIUM, 3 to DangerLevel.HIGH)

        levels.forEach { (level, expected) ->
            val content = rn2(point(lat = 0.0, lon = 0.0, notes = "[${dangerIcon(level)}]"))

            val waypoint = parseRoadbook(content).waypoints.single()

            assertEquals("level $level", expected, waypoint.dangerLevel)
        }
    }

    @Test
    fun parse_severalDangerSymbols_takesTheFirst() = runTest {
        val notes = "[${dangerIcon(2)}, ${dangerIcon(3)}]"
        val content = rn2(point(lat = 0.0, lon = 0.0, notes = notes))

        assertEquals(DangerLevel.MEDIUM, parseRoadbook(content).waypoints.single().dangerLevel)
    }

    @Test
    fun parse_noDangerSymbol_hasNoDanger() = runTest {
        val content = rn2(point(lat = 0.0, lon = 0.0, notes = "[$RESET_ICON]"))

        assertEquals(DangerLevel.NONE, parseRoadbook(content).waypoints.single().dangerLevel)
    }

    @Test
    fun parse_notes_areMapped() = runTest {
        val content = rn2(point(lat = 0.0, lon = 0.0, notes = "[${dangerIcon(1)}]"))

        val icon = parseRoadbook(content).waypoints.single().notes.single() as Icon

        assertEquals(IconType.DANGER_1, icon.type)
    }

    // Tulips

    @Test
    fun parse_trackWithoutRoadOutEnd_aimsAtTheNextPoint() = runTest {
        // The old app's case: arriving heading east, leaving heading north, is a left turn.
        val content = rn2(
            point(lat = 0.0, lon = -0.001, show = false),
            point(lat = 0.0, lon = 0.0, tulip = """[{"type": "Track"}]"""),
            point(lat = 0.001, lon = 0.0),
        )

        val track = parseRoadbook(content).waypoints[0].tulip.single() as Track

        assertEquals(-75.0, track.roadOut.end!!.x, 1e-6)
        assertEquals(0.0, track.roadOut.end!!.y, 1e-6)
    }

    @Test
    fun parse_trackOnTheLastPoint_goesStraightOn() = runTest {
        val content = rn2(point(lat = 0.0, lon = 0.0, tulip = """[{"type": "Track"}]"""))

        val track = parseRoadbook(content).waypoints.single().tulip.single() as Track

        assertEquals(DEFAULT_ROAD_OUT_END, track.roadOut.end)
    }

    @Test
    fun parse_trackWithoutType_inheritsFromThePreviousShownTulip() = runTest {
        val tarmacTrack = """[{"type": "Track", "roadOut": {"typeId": 18}}]"""
        val smallTrack = """[{"type": "Track", "roadOut": {"typeId": 4}}]"""
        val untypedTrack = """[{"type": "Track"}]"""
        val content = rn2(
            point(lat = 0.0, lon = 0.0, tulip = tarmacTrack),
            point(lat = 0.0, lon = 0.001, show = false, tulip = smallTrack),
            point(lat = 0.0, lon = 0.002, tulip = untypedTrack),
        )

        val track = parseRoadbook(content).waypoints[1].tulip.single() as Track

        // The hidden point's tulip is not drawn, so it does not pass its kind on.
        assertEquals(RoadType.TARMAC_ROAD, track.roadIn.type)
    }

    @Test
    fun parse_roadsInNotes_areSkipped() = runTest {
        val content = rn2(
            point(
                lat = 0.0,
                lon = 0.0,
                notes = """[{"type": "Track"}, {"type": "Road"}, $RESET_ICON]""",
            ),
        )

        val notes = parseRoadbook(content).waypoints.single().notes

        assertEquals(listOf(Icon::class), notes.map { it::class })
    }

    @Test
    fun parse_hiddenPoint_hasNoDrawing() = runTest {
        val content = rn2(
            point(lat = 0.0, lon = 0.0, show = false, tulip = """[{"type": "Track"}]"""),
            point(lat = 0.0, lon = 0.001),
        )

        assertTrue(parseRoadbook(content).waypoints.single().tulip.isEmpty())
    }

    @Test
    fun parse_unknownElementType_isSkipped() = runTest {
        val tulip = """[{"type": "Hologram", "x": 1}, {"type": "Road"}]"""
        val content = rn2(point(lat = 0.0, lon = 0.0, tulip = tulip))

        assertEquals(1, parseRoadbook(content).waypoints.single().tulip.size)
    }

    @Test
    fun parse_elementWithoutType_isSkipped() = runTest {
        val content = rn2(point(lat = 0.0, lon = 0.0, tulip = """[{"x": 1}]"""))

        assertTrue(parseRoadbook(content).waypoints.single().tulip.isEmpty())
    }

    @Test
    fun parse_validFile_returnsAValidRoadbook() = runTest {
        val content = rn2(point(lat = 0.0, lon = 0.0, show = false), point(lat = 0.0, lon = 0.001))

        val roadbook = parseRoadbook(content)

        assertEquals(Distance.ZERO, roadbook.waypoints.first().distanceFromPrevious)
    }

    private fun TestScope.parser() =
        Rn2RoadbookParser(Rn2ElementMapper(logger), logger, StandardTestDispatcher(testScheduler))

    private suspend fun TestScope.parseRoadbook(content: String): Roadbook =
        when (val result = parser().parse(content)) {
            is ParseRoadbookResult.Success -> result.roadbook
            is ParseRoadbookResult.Failure ->
                throw AssertionError("Expected a roadbook, but got $result")
        }

    private fun assertFailure(expected: ParseRoadbookFailure, actual: ParseRoadbookResult) =
        assertEquals(ParseRoadbookResult.Failure(expected), actual)

    private companion object {
        /** 0.001° along the equator on WGS84: the equatorial radius times the angle. */
        const val STEP_METERS: Double = 6_378_137.0 * 0.001 * Math.PI / 180

        /**
         * How close a measured distance must be to the expected one, per step measured: 10 µm.
         * Vincenty's iteration stops within about 6 µm of the exact geodesic (see
         * GeoPoint.distanceTo), and a distance over several steps adds up their errors.
         */
        const val DISTANCE_TOLERANCE_METERS: Double = 1e-5

        const val RESET_ICON = """{"type": "Icon", "id": "308c7365-bc3f-451b-9e98-531e9015024f"}"""

        fun dangerIcon(level: Int) = """{"type": "Icon", "name": "Danger Level $level"}"""

        /** A file whose route has the given points. */
        fun rn2(vararg points: String): String =
            """{"route": {"waypoints": [${points.joinToString(", ")}]}}"""

        /** A point of the route, as JSON. */
        fun point(
            lat: Double,
            lon: Double,
            show: Boolean = true,
            ele: Double? = null,
            tulip: String = "[]",
            notes: String = "[]",
        ): String {
            val elevation = ele?.let { """"ele": $it, """ } ?: ""
            return """{"lat": $lat, "lon": $lon, $elevation"show": $show, """ +
                """"tulip": {"elements": $tulip}, "notes": {"elements": $notes}}"""
        }
    }
}
