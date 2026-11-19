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

package dev.giste.roadbook

/**
 * The result of [RoadbookController.importRoadbook].
 *
 * Importing never throws: every problem with the file, or with saving it, is a [Failure].
 */
public sealed interface ImportRoadbookResult {

    /**
     * The roadbook was imported and saved. It replaced the previous one, and is shown from its
     * first waypoint.
     */
    public data object Success : ImportRoadbookResult

    /**
     * The roadbook was not imported. The previous roadbook, if any, is kept.
     *
     * @property reason why the import failed, so the app can tell the user.
     */
    public data class Failure(public val reason: ImportRoadbookFailure) : ImportRoadbookResult
}

/** Why [RoadbookController.importRoadbook] failed. */
public enum class ImportRoadbookFailure {

    /**
     * The file could not be read: it does not exist anymore, the app has no permission to read it,
     * or reading it failed.
     */
    UNREADABLE,

    /**
     * The file is not a roadbook of a supported format: it is not valid, lacks required data, or
     * has values that are not possible (e.g. a latitude of 120°).
     */
    MALFORMED,

    /** The file is a valid roadbook, but it has no waypoint to show. */
    NO_WAYPOINTS,

    /**
     * The roadbook is valid, but it could not be saved on the device (e.g. the storage is full),
     * so it would be lost on the next start.
     */
    NOT_SAVED,
}
