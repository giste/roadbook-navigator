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

import dev.giste.roadbook.FakeLogger
import dev.giste.roadbook.domain.drawing.Icon
import dev.giste.roadbook.domain.roadbook.ParseRoadbookResult
import dev.giste.roadbook.domain.roadbook.Roadbook
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parses `route_example.rn2`, a real file exported by Rally Navigator. */
class Rn2RoadbookParserIntegrationTest {

    private val logger = FakeLogger()

    @Test
    fun parse_exampleFile_hasItsFourShownWaypoints() = runTest {
        val roadbook = parseExample(StandardTestDispatcher(testScheduler))

        assertEquals("", roadbook.name)
        assertEquals(listOf(1, 2, 3, 4), roadbook.waypoints.map { it.number.value })
    }

    @Test
    fun parse_exampleFile_measuresDistancesAlongTheRoute() = runTest {
        val waypoints = parseExample(StandardTestDispatcher(testScheduler)).waypoints

        // Every waypoint after the first is some distance along the route.
        assertTrue(waypoints.drop(1).all { it.distanceFromPrevious.inMeters > 0.0 })
        assertTrue(
            waypoints.zipWithNext().all { (a, b) -> b.partial >= a.partial || a.resetsPartial },
        )
    }

    @Test
    fun parse_exampleFile_drawsEveryWaypoint() = runTest {
        val waypoints = parseExample(StandardTestDispatcher(testScheduler)).waypoints

        assertTrue(waypoints.all { it.tulip.isNotEmpty() })
    }

    @Test
    fun parse_exampleFile_knowsItsSymbols() = runTest {
        val waypoints = parseExample(StandardTestDispatcher(testScheduler)).waypoints

        val icons = waypoints.flatMap { it.tulip + it.notes }.filterIsInstance<Icon>()

        assertTrue(icons.isNotEmpty())
        assertTrue(logger.warnings.none { it.startsWith("Skipping") })
    }

    private suspend fun parseExample(dispatcher: CoroutineDispatcher): Roadbook {
        val resource = javaClass.classLoader?.getResource("route_example.rn2")
        val content = checkNotNull(resource) { "route_example.rn2 not found" }.readText()
        val parser = Rn2RoadbookParser(Rn2ElementMapper(logger), logger, dispatcher)
        val result = parser.parse(content)
        check(result is ParseRoadbookResult.Success) { "Expected a roadbook, but got $result" }
        return result.roadbook
    }
}
