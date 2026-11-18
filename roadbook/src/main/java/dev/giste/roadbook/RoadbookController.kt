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

import android.net.Uri
import kotlinx.coroutines.flow.SharedFlow

/**
 * The roadbook seen from outside its screen: what it reports to the app, and the commands the app
 * can give it.
 *
 * Hilt binds it as a singleton in `SingletonComponent`, so it lives as long as the app process,
 * whether or not the roadbook screen is shown. Inject it where the app reacts to the roadbook
 * (e.g. a coordinator that changes the odometer) or drives it (e.g. the keys of a handlebar remote control).
 *
 * All functions are main-safe.
 */
public interface RoadbookController {

    /**
     * What happens in the roadbook that the app may need to react to, such as a new roadbook being
     * loaded or the user asking to set the partial.
     *
     * It is a hot flow with no replay: an event reaches only the collectors that are collecting
     * when it is emitted, so collect it for as long as the app runs (e.g. in the application
     * scope). It never throws and never completes.
     */
    public val events: SharedFlow<RoadbookEvent>

    /**
     * Imports the roadbook file at [uri], which replaces the current roadbook when it succeeds.
     *
     * The file is read, parsed and saved, so the roadbook survives process death and restarts.
     * When it succeeds, the roadbook is shown from its first waypoint, and [RoadbookEvent.Loaded]
     * is emitted. When it fails, nothing changes.
     *
     * The supported format is Rally Navigator's `.rn2`.
     *
     * @param uri where the file is, usually a `content://` URI from the system file picker (e.g.
     * `ActivityResultContracts.OpenDocument`). The app must be able to read it when this is
     * called. `file://` URIs work too.
     * @return [ImportRoadbookResult.Success], or why the import failed. It never throws.
     */
    public suspend fun importRoadbook(uri: Uri): ImportRoadbookResult

    /**
     * Moves the roadbook forward by one waypoint, so the next waypoint is shown at the top.
     *
     * Nothing changes when the last waypoint is already at the top, or when no roadbook is loaded.
     * The new position is persisted, so it survives process death and restarts.
     */
    public suspend fun moveToNextWaypoint()

    /**
     * Moves the roadbook back by one waypoint, so the previous waypoint is shown at the top.
     *
     * Nothing changes when the first waypoint is already at the top, or when no roadbook is
     * loaded. The new position is persisted, so it survives process death and restarts.
     */
    public suspend fun moveToPreviousWaypoint()
}
