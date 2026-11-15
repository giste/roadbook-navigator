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

import dev.giste.core.coroutines.DefaultDispatcher
import dev.giste.core.logging.Logger
import dev.giste.geo.Altitude
import dev.giste.geo.GeoPoint
import dev.giste.roadbook.domain.drawing.IconType
import dev.giste.roadbook.domain.drawing.RoadType
import dev.giste.roadbook.domain.drawing.tulipExitPoint
import dev.giste.roadbook.domain.roadbook.DangerLevel
import dev.giste.roadbook.domain.roadbook.ParseRoadbookFailure
import dev.giste.roadbook.domain.roadbook.ParseRoadbookResult
import dev.giste.roadbook.domain.roadbook.Roadbook
import dev.giste.roadbook.domain.roadbook.RoadbookParser
import dev.giste.roadbook.domain.roadbook.RoutePoint
import dev.giste.roadbook.domain.roadbook.toWaypoints
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** The latitudes a position can have, in degrees. */
private val VALID_LATITUDES: ClosedFloatingPointRange<Double> = -90.0..90.0

/** The longitudes a position can have, in degrees. */
private val VALID_LONGITUDES: ClosedFloatingPointRange<Double> = -180.0..180.0

/** Danger levels by the symbol that marks them, from the lowest. */
private val DANGER_LEVELS_BY_ICON: Map<IconType, DangerLevel> = mapOf(
    IconType.DANGER_1 to DangerLevel.LOW,
    IconType.DANGER_2 to DangerLevel.MEDIUM,
    IconType.DANGER_3 to DangerLevel.HIGH,
)

/**
 * Parses Rally Navigator `.rn2` files.
 *
 * The file's format details stay here: the JSON layout, the symbol and road ids, and the defaults
 * for what a file leaves out (through [Rn2ElementMapper]). How a route becomes waypoints
 * (distances, resets, numbering) is domain logic, in [toWaypoints].
 *
 * A point's partial is reset, and its danger level set, by the symbols in its notes column, as
 * Rally Navigator draws them. Exit roads are aimed at the next point of the route, shown or not.
 */
internal class Rn2RoadbookParser @Inject constructor(
    private val elementMapper: Rn2ElementMapper,
    private val logger: Logger,
    @param:DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : RoadbookParser {

    /**
     * Lenient on purpose: unknown keys are ignored, so files from newer Rally Navigator versions
     * still load, and a `null` where a value has a default takes the default.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override suspend fun parse(content: String): ParseRoadbookResult = withContext(dispatcher) {
        val file = decode(content)
            ?: return@withContext ParseRoadbookResult.Failure(ParseRoadbookFailure.MALFORMED)
        val route = file.route
        if (route.waypoints.any { !it.hasValidPosition() }) {
            logger.w("Rejected a .rn2 file with an impossible position")
            return@withContext ParseRoadbookResult.Failure(ParseRoadbookFailure.MALFORMED)
        }

        val waypoints = route.waypoints.toRoutePoints().toWaypoints()
        if (waypoints.isEmpty()) {
            logger.w("Rejected a .rn2 file without waypoints to show")
            return@withContext ParseRoadbookResult.Failure(ParseRoadbookFailure.NO_WAYPOINTS)
        }
        logger.i("Parsed a .rn2 roadbook with %d waypoints", waypoints.size)
        ParseRoadbookResult.Success(
            Roadbook(
                name = route.name,
                description = route.description,
                startLocation = route.startLocation,
                endLocation = route.endLocation,
                waypoints = waypoints,
            ),
        )
    }

    /**
     * Decodes [content], or returns `null` when it is not a `.rn2` file.
     *
     * This is the data boundary for the file's content, so decoding failures are caught here and
     * turned into a result. kotlinx.serialization reports every one of them (bad JSON, missing
     * fields, wrong types) as an `IllegalArgumentException`: `SerializationException` is one.
     */
    private fun decode(content: String): Rn2File? = try {
        json.decodeFromString<Rn2File>(content)
    } catch (e: IllegalArgumentException) {
        logger.w("Rejected a malformed .rn2 file: %s", e.message)
        null
    }

    /**
     * Maps the points of the route in order. Each point needs its neighbors (to aim its exit
     * road) and the kind of road the last shown tulip left on (for tracks without a kind).
     */
    private fun List<Rn2Waypoint>.toRoutePoints(): List<RoutePoint> {
        var inheritedRoadType = RoadType.TRACK
        return mapIndexed { index, point ->
            val position = point.position()
            if (!point.show) {
                return@mapIndexed RoutePoint(
                    position = position,
                    altitude = point.altitude(),
                    shown = false,
                    resetsPartial = point.resetsPartial(),
                )
            }

            val previous = getOrNull(index - 1)?.position()
            val next = getOrNull(index + 1)?.position()
            val tulip = elementMapper.mapTulip(point.tulip.elements, inheritedRoadType) {
                next?.let { tulipExitPoint(previous, position, it) } ?: DEFAULT_ROAD_OUT_END
            }
            inheritedRoadType = tulip.roadTypeOut
            RoutePoint(
                position = position,
                altitude = point.altitude(),
                shown = true,
                resetsPartial = point.resetsPartial(),
                dangerLevel = point.dangerLevel(),
                tulip = tulip.elements,
                notes = elementMapper.mapNotes(point.notes.elements, inheritedRoadType),
            )
        }
    }

    private fun Rn2Waypoint.hasValidPosition(): Boolean =
        lat in VALID_LATITUDES && lon in VALID_LONGITUDES

    /** Only called after [hasValidPosition] was checked for every point. */
    private fun Rn2Waypoint.position(): GeoPoint = GeoPoint(latitude = lat, longitude = lon)

    /** The altitude, or `null` when the file gives none or it is not finite. */
    private fun Rn2Waypoint.altitude(): Altitude? =
        ele?.takeIf { it.isFinite() }?.let(Altitude::meters)

    /** The types of the symbols in the notes column, in order. */
    private fun Rn2Waypoint.noteIconTypes(): List<IconType> =
        notes.elements.filterIsInstance<Rn2Icon>().map { it.iconType() }

    private fun Rn2Waypoint.resetsPartial(): Boolean = IconType.RESET_PARTIAL in noteIconTypes()

    /** The danger level of the first danger symbol in the notes column, or none. */
    private fun Rn2Waypoint.dangerLevel(): DangerLevel =
        noteIconTypes().firstNotNullOfOrNull(DANGER_LEVELS_BY_ICON::get) ?: DangerLevel.NONE
}
