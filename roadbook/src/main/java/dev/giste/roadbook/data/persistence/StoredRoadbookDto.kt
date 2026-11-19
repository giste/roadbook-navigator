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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The persisted format of the current roadbook, written as JSON by RoadbookFileSerializer.
//
// It mirrors the domain model, but it is a separate set of classes on purpose: the domain stays
// free of serialization, and changing the domain does not change this format by accident. Any
// change here that an older file cannot be read with needs a new
// RoadbookFileSerializer.CURRENT_VERSION (and a migration, or the saved roadbook is dropped).
//
// Distances and altitudes are in meters. Enum values are stored by the name of the domain entry.
// No field has a default, so every field is always written (null included) and must be read:
// the format does not depend on which values happen to be defaults.

/**
 * The roadbook file: the format version, followed by the roadbook.
 *
 * @property version the version of the format the file was written with.
 * @property roadbook the saved roadbook.
 */
@Serializable
internal data class StoredRoadbookFileDto(
    val version: Int,
    val roadbook: StoredRoadbookDto,
)

/**
 * Only the version of a roadbook file, read first to know whether the rest can be read.
 *
 * @property version the version of the format the file was written with.
 */
@Serializable
internal data class StoredVersionDto(val version: Int)

/** A saved `Roadbook`. */
@Serializable
internal data class StoredRoadbookDto(
    val name: String,
    val description: String,
    val startLocation: String,
    val endLocation: String,
    val waypoints: List<StoredWaypointDto>,
)

/** A saved `Waypoint`. */
@Serializable
internal data class StoredWaypointDto(
    val number: Int,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val partialMeters: Double,
    val distanceFromPreviousMeters: Double,
    val resetsPartial: Boolean,
    val dangerLevel: String,
    val tulip: List<StoredElementDto>,
    val notes: List<StoredElementDto>,
)

/** A saved `CanvasPoint`. */
@Serializable
internal data class StoredCanvasPointDto(val x: Double, val y: Double)

/**
 * A saved drawing `Element`. The subtype is written in the `type` field of the JSON object, so
 * the names given with [SerialName] are part of the format.
 */
@Serializable
internal sealed interface StoredElementDto

/** A saved `Track`. */
@Serializable
@SerialName("track")
internal data class StoredTrackDto(
    val roadIn: StoredRoadDto,
    val roadOut: StoredRoadDto,
) : StoredElementDto

/** A saved `Road`. */
@Serializable
@SerialName("road")
internal data class StoredRoadDto(
    val start: StoredCanvasPointDto?,
    val end: StoredCanvasPointDto?,
    val roadType: String,
    val handles: List<StoredCanvasPointDto>,
) : StoredElementDto

/** A saved `Icon`. */
@Serializable
@SerialName("icon")
internal data class StoredIconDto(
    val iconType: String,
    val width: Double,
    val height: Double,
    val center: StoredCanvasPointDto,
    val angle: Int,
    val scaleX: Double,
    val scaleY: Double,
    val originalId: String?,
    val originalName: String?,
) : StoredElementDto

/** A saved `Text`. */
@Serializable
@SerialName("text")
internal data class StoredTextDto(
    val text: String,
    val fontSize: Int,
    val lineHeight: Double,
    val width: Double,
    val height: Double,
    val maxWidth: Double,
    val maxHeight: Double,
    val center: StoredCanvasPointDto,
) : StoredElementDto
