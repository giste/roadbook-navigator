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

import dev.giste.geo.Altitude
import dev.giste.geo.Distance
import dev.giste.geo.GeoPoint
import dev.giste.roadbook.domain.drawing.CanvasPoint
import dev.giste.roadbook.domain.drawing.Icon
import dev.giste.roadbook.domain.drawing.IconType
import dev.giste.roadbook.domain.drawing.Road
import dev.giste.roadbook.domain.drawing.RoadType
import dev.giste.roadbook.domain.drawing.Text
import dev.giste.roadbook.domain.drawing.Track
import dev.giste.roadbook.domain.roadbook.DangerLevel
import dev.giste.roadbook.domain.roadbook.Roadbook
import dev.giste.roadbook.domain.roadbook.Waypoint
import dev.giste.roadbook.domain.roadbook.WaypointNumber

/**
 * A roadbook that uses every field of the persisted format, with values that differ from the
 * defaults: every kind of element, roads with and without ends, an altitude and a missing one, a
 * reset, a danger level, a mirrored icon, an icon the app does not know, …
 *
 * It is the roadbook stored in `src/test/resources/stored_roadbook_v1.json`.
 */
internal val FULL_ROADBOOK: Roadbook = Roadbook(
    name = "Stage 1",
    description = "Loop around the dunes",
    startLocation = "Bivouac",
    endLocation = "Finish",
    waypoints = listOf(
        Waypoint(
            number = WaypointNumber(1),
            position = GeoPoint(latitude = 37.125, longitude = -2.5),
            altitude = Altitude.meters(512.5),
            partial = Distance.ZERO,
            distanceFromPrevious = Distance.ZERO,
            tulip = listOf(
                Track(
                    roadIn = Road(
                        start = CanvasPoint(0.0, 35.0),
                        end = null,
                        type = RoadType.TRACK,
                    ),
                    roadOut = Road(
                        start = null,
                        end = CanvasPoint(75.0, -12.5),
                        type = RoadType.TARMAC_ROAD,
                        handles = listOf(CanvasPoint(10.0, -5.0), CanvasPoint(40.0, -10.0)),
                    ),
                ),
                Road(start = null, end = CanvasPoint(-60.0, 0.0), type = RoadType.OFF_TRACK),
                Icon(
                    type = IconType.ROUNDABOUT,
                    width = 35.67,
                    height = 50.0,
                    center = CanvasPoint(-20.0, 15.5),
                    angle = 90,
                    scaleX = -1.0,
                    scaleY = 1.5,
                ),
                Text(
                    text = "CAP 120",
                    fontSize = 22,
                    lineHeight = 1.2,
                    width = 80.0,
                    height = 24.0,
                    maxWidth = 180.0,
                    maxHeight = 100.0,
                    center = CanvasPoint(30.0, 40.0),
                ),
            ),
            notes = listOf(
                Icon(
                    type = IconType.UNKNOWN,
                    width = 50.0,
                    height = 50.0,
                    center = CanvasPoint.CENTER,
                    originalId = "x-42",
                    originalName = "Camel",
                ),
            ),
        ),
        Waypoint(
            number = WaypointNumber(2),
            position = GeoPoint(latitude = 37.13, longitude = -2.49),
            altitude = null,
            partial = Distance.meters(1_234.567),
            distanceFromPrevious = Distance.meters(1_234.567),
            resetsPartial = true,
            dangerLevel = DangerLevel.HIGH,
            notes = listOf(
                Icon(
                    type = IconType.RESET_PARTIAL,
                    width = 50.0,
                    height = 50.0,
                    center = CanvasPoint(0.0, -20.0),
                ),
                Text(
                    text = "",
                    lineHeight = 1.0,
                    width = 0.0,
                    height = 0.0,
                    maxWidth = 180.0,
                    maxHeight = 100.0,
                    center = CanvasPoint(0.0, 30.0),
                ),
            ),
        ),
    ),
)
