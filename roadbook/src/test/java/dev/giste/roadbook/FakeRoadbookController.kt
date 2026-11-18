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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * [RoadbookController] for tests: records the imports it is asked for, and returns [result].
 *
 * Set [gate] to keep an import running until the test completes it.
 */
internal class FakeRoadbookController : RoadbookController {

    override val events: SharedFlow<RoadbookEvent> = MutableSharedFlow()

    /** What [importRoadbook] returns. */
    var result: ImportRoadbookResult = ImportRoadbookResult.Success

    /** When set, [importRoadbook] waits for it before returning. */
    var gate: CompletableDeferred<Unit>? = null

    /** The URIs passed to [importRoadbook], in call order. */
    val importedUris: MutableList<Uri> = mutableListOf()

    override suspend fun importRoadbook(uri: Uri): ImportRoadbookResult {
        importedUris += uri
        gate?.await()
        return result
    }

    override suspend fun moveToNextWaypoint() = Unit

    override suspend fun moveToPreviousWaypoint() = Unit
}
