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
import dev.giste.roadbook.domain.drawing.Element
import dev.giste.roadbook.domain.drawing.Icon
import dev.giste.roadbook.domain.drawing.IconType
import dev.giste.roadbook.domain.drawing.NoteElement
import dev.giste.roadbook.domain.drawing.Road
import dev.giste.roadbook.domain.drawing.RoadType
import dev.giste.roadbook.domain.drawing.Text
import dev.giste.roadbook.domain.drawing.Track
import dev.giste.roadbook.domain.drawing.TulipElement
import dev.giste.roadbook.domain.roadbook.DangerLevel
import dev.giste.roadbook.domain.roadbook.Roadbook
import dev.giste.roadbook.domain.roadbook.Waypoint
import dev.giste.roadbook.domain.roadbook.WaypointNumber

// Maps the domain model to the persisted format (StoredRoadbookDto.kt) and back.
//
// Mapping to the format always succeeds. Mapping back builds the domain objects with their
// constructors, so a stored value that breaks an invariant (a file edited or damaged on disk)
// throws IllegalArgumentException, like a value that does not fit its type. The caller treats
// both as an unreadable file.

/** Maps this roadbook to its persisted format. */
internal fun Roadbook.toStored(): StoredRoadbookDto = StoredRoadbookDto(
    name = name,
    description = description,
    startLocation = startLocation,
    endLocation = endLocation,
    waypoints = waypoints.map { it.toStored() },
)

/**
 * Maps a persisted roadbook back to the domain.
 *
 * @throws IllegalArgumentException if a value is not valid for the domain.
 */
internal fun StoredRoadbookDto.toDomain(): Roadbook = Roadbook(
    name = name,
    description = description,
    startLocation = startLocation,
    endLocation = endLocation,
    waypoints = waypoints.map { it.toDomain() },
)

private fun Waypoint.toStored(): StoredWaypointDto = StoredWaypointDto(
    number = number.value,
    latitude = position.latitude,
    longitude = position.longitude,
    altitudeMeters = altitude?.inMeters,
    partialMeters = partial.inMeters,
    distanceFromPreviousMeters = distanceFromPrevious.inMeters,
    resetsPartial = resetsPartial,
    dangerLevel = dangerLevel.name,
    tulip = tulip.map { it.toStored() },
    notes = notes.map { it.toStored() },
)

private fun StoredWaypointDto.toDomain(): Waypoint = Waypoint(
    number = WaypointNumber(number),
    position = GeoPoint(latitude = latitude, longitude = longitude),
    altitude = altitudeMeters?.let(Altitude::meters),
    partial = Distance.meters(partialMeters),
    distanceFromPrevious = Distance.meters(distanceFromPreviousMeters),
    resetsPartial = resetsPartial,
    dangerLevel = enumByName<DangerLevel>(dangerLevel),
    tulip = tulip.map { it.toTulipElement() },
    notes = notes.map { it.toNoteElement() },
)

private fun Element.toStored(): StoredElementDto = when (this) {
    is Track -> StoredTrackDto(roadIn = roadIn.toStored(), roadOut = roadOut.toStored())
    is Road -> toStored()
    is Icon -> StoredIconDto(
        iconType = type.name,
        width = width,
        height = height,
        center = center.toStored(),
        angle = angle,
        scaleX = scaleX,
        scaleY = scaleY,
        originalId = originalId,
        originalName = originalName,
    )
    is Text -> StoredTextDto(
        text = text,
        fontSize = fontSize,
        lineHeight = lineHeight,
        width = width,
        height = height,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        center = center.toStored(),
    )
}

private fun Road.toStored(): StoredRoadDto = StoredRoadDto(
    start = start?.toStored(),
    end = end?.toStored(),
    roadType = type.name,
    handles = handles.map { it.toStored() },
)

private fun CanvasPoint.toStored(): StoredCanvasPointDto = StoredCanvasPointDto(x = x, y = y)

/** Maps a persisted element of a tulip back to the domain. Every kind of element is allowed. */
private fun StoredElementDto.toTulipElement(): TulipElement = when (this) {
    is StoredTrackDto -> Track(roadIn = roadIn.toDomain(), roadOut = roadOut.toDomain())
    is StoredRoadDto -> toDomain()
    is StoredIconDto -> toDomain()
    is StoredTextDto -> toDomain()
}

/**
 * Maps a persisted element of a notes column back to the domain.
 *
 * @throws IllegalArgumentException for a road or a track, which a notes column cannot have.
 */
private fun StoredElementDto.toNoteElement(): NoteElement = when (this) {
    is StoredIconDto -> toDomain()
    is StoredTextDto -> toDomain()
    is StoredTrackDto, is StoredRoadDto ->
        throw IllegalArgumentException("A notes column cannot have a ${this::class.simpleName}")
}

private fun StoredRoadDto.toDomain(): Road = Road(
    start = start?.toDomain(),
    end = end?.toDomain(),
    type = enumByName<RoadType>(roadType),
    handles = handles.map { it.toDomain() },
)

private fun StoredIconDto.toDomain(): Icon = Icon(
    type = enumByName<IconType>(iconType),
    width = width,
    height = height,
    center = center.toDomain(),
    angle = angle,
    scaleX = scaleX,
    scaleY = scaleY,
    originalId = originalId,
    originalName = originalName,
)

private fun StoredTextDto.toDomain(): Text = Text(
    text = text,
    fontSize = fontSize,
    lineHeight = lineHeight,
    width = width,
    height = height,
    maxWidth = maxWidth,
    maxHeight = maxHeight,
    center = center.toDomain(),
)

private fun StoredCanvasPointDto.toDomain(): CanvasPoint = CanvasPoint(x = x, y = y)

/**
 * The entry of [E] named [name].
 *
 * `enumValueOf` would do the same, but its exception does not say which type it was looking in.
 *
 * @throws IllegalArgumentException if [E] has no entry named [name].
 */
private inline fun <reified E : Enum<E>> enumByName(name: String): E =
    enumValues<E>().firstOrNull { it.name == name }
        ?: throw IllegalArgumentException("${E::class.simpleName} has no entry named $name")
