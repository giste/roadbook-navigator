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

package dev.giste.roadbook.presentation.drawing

import androidx.compose.ui.geometry.Offset
import dev.giste.roadbook.domain.drawing.CanvasPoint
import dev.giste.roadbook.domain.drawing.Road
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasGeometryTest {

    private fun assertNear(expected: Offset, actual: Offset) {
        assertTrue(
            "Expected $expected but was $actual",
            (expected - actual).getDistance() < TOLERANCE,
        )
    }

    private fun assertNear(expected: Float, actual: Float) {
        assertEquals(expected, actual, TOLERANCE)
    }

    @Test
    fun canvasPoints_roadWithoutEnd_isNotDrawn() {
        assertNull(Road(start = null, end = null).canvasPoints())
    }

    @Test
    fun canvasPoints_roadWithoutStart_startsAtJunction() {
        val points = Road(start = null, end = CanvasPoint(0.0, -55.0)).canvasPoints()

        assertEquals(listOf(JUNCTION, Offset(100f, 30f)), points)
    }

    @Test
    fun canvasPoints_measuresEveryPointFromJunction() {
        val road = Road(
            start = CanvasPoint(0.0, 35.0),
            end = CanvasPoint(-80.5, -49.0),
            handles = listOf(CanvasPoint(-24.0, -34.0), CanvasPoint(-47.0, -47.0)),
        )

        val points = road.canvasPoints()

        assertEquals(
            listOf(Offset(100f, 120f), Offset(76f, 51f), Offset(53f, 38f), Offset(19.5f, 36f)),
            points,
        )
    }

    @Test
    fun toOffset_iconOrTextCenter_isNotMovedByJunction() {
        assertEquals(Offset(99.5f, 84f), CanvasPoint(99.5, 84.0).toOffset())
    }

    @Test
    fun smoothSegments_twoPoints_isStraightLine() {
        val segments = smoothSegments(listOf(Offset(0f, 0f), Offset(10f, 0f)))

        assertEquals(listOf(RoadSegment.Line(Offset(10f, 0f))), segments)
    }

    @Test
    fun smoothSegments_threePoints_areTwoQuadraticCurvesThroughEveryPoint() {
        val points = listOf(Offset(0f, 0f), Offset(10f, 10f), Offset(20f, 0f))

        val segments = smoothSegments(points)

        // The control points follow the direction from the point before to the point after.
        assertEquals(
            listOf(
                RoadSegment.Quadratic(control = Offset(6f, 10f), end = Offset(10f, 10f)),
                RoadSegment.Quadratic(control = Offset(14f, 10f), end = Offset(20f, 0f)),
            ),
            segments,
        )
    }

    @Test
    fun smoothSegments_fourPoints_haveCubicCurvesInTheMiddle() {
        val points = listOf(Offset(0f, 0f), Offset(10f, 0f), Offset(20f, 0f), Offset(30f, 0f))

        val segments = smoothSegments(points)

        assertEquals(3, segments.size)
        assertTrue(segments[0] is RoadSegment.Quadratic)
        val middle = segments[1] as RoadSegment.Cubic
        assertNear(Offset(14f, 0f), middle.control1)
        assertNear(Offset(16f, 0f), middle.control2)
        assertEquals(Offset(20f, 0f), middle.end)
        assertTrue(segments[2] is RoadSegment.Quadratic)
        assertEquals(points.drop(1), segments.map { it.end })
    }

    @Test
    fun smoothSegments_onePoint_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            smoothSegments(listOf(Offset.Zero))
        }
    }

    @Test
    fun endAngle_straightRoadToTheRight_isZero() {
        assertNear(0f, endAngle(listOf(Offset(0f, 0f), Offset(10f, 0f))))
    }

    @Test
    fun endAngle_straightRoadUp_isMinusHalfPi() {
        // y grows downwards, so up is -π/2.
        assertNear((-PI / 2).toFloat(), endAngle(listOf(Offset(0f, 0f), Offset(0f, -10f))))
    }

    @Test
    fun endAngle_curvedRoad_followsLastControlPoint() {
        val points = listOf(Offset(0f, 0f), Offset(10f, 10f), Offset(20f, 0f))

        // The last curve's control point is (14, 10), so the road ends heading from it to (20, 0).
        assertNear(atan2(-10f, 6f), endAngle(points))
    }

    @Test
    fun endAngle_onePoint_throws() {
        assertThrows(IllegalArgumentException::class.java) { endAngle(listOf(Offset.Zero)) }
    }

    @Test
    fun arrowHead_roadToTheRight_pointsRightBeyondEnd() {
        val corners = arrowHead(end = Offset.Zero, angle = 0f)

        val height = ARROW_SIDE * sqrt(3f) / 2
        assertNear(Offset(height, 0f), corners[0])
        assertNear(Offset(0f, ARROW_SIDE / 2), corners[1])
        assertNear(Offset(0f, -ARROW_SIDE / 2), corners[2])
    }

    @Test
    fun endBar_roadToTheRight_isVerticalAndCentered() {
        val (start, end) = endBar(end = Offset(5f, 5f), angle = 0f)

        assertNear(Offset(5f, 5f + END_BAR_LENGTH / 2), start)
        assertNear(Offset(5f, 5f - END_BAR_LENGTH / 2), end)
    }

    @Test
    fun endBar_roadUp_isHorizontal() {
        val (start, end) = endBar(end = Offset.Zero, angle = (-PI / 2).toFloat())

        assertNear(Offset(END_BAR_LENGTH / 2, 0f), start)
        assertNear(Offset(-END_BAR_LENGTH / 2, 0f), end)
    }

    private companion object {
        const val TOLERANCE = 1e-4f
    }
}
