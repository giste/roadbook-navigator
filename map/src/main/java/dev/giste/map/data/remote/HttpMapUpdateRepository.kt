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

package dev.giste.map.data.remote

import dev.giste.core.logging.Logger
import dev.giste.map.domain.MapUpdateRepository
import dev.giste.map.domain.RemoteMapPath
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Asks the download site when its maps last changed, with an HTTP `HEAD` request per map: its
 * `Last-Modified` header, the same date a downloaded map gets as its modification time.
 *
 * At most [MAX_PARALLEL_REQUESTS] requests run at once, to be polite to the site. Requests run on
 * OkHttp's own threads, so [check] is main-safe without a dispatcher, and they are cancelled when
 * the calling coroutine is. The dates are kept in memory, so the screens that show updates share
 * them; it is a singleton for that reason.
 *
 * @param client the HTTP client.
 * @param site the addresses of the maps.
 * @param logger logs failures.
 */
@Singleton
internal class HttpMapUpdateRepository @Inject constructor(
    @param:MapHttpClient private val client: OkHttpClient,
    private val site: MapSite,
    private val logger: Logger,
) : MapUpdateRepository {

    private val versions = MutableStateFlow<Map<RemoteMapPath, Instant>>(emptyMap())

    override val siteVersions: Flow<Map<RemoteMapPath, Instant>> = versions.asStateFlow()

    override suspend fun check(maps: Collection<RemoteMapPath>) {
        val permits = Semaphore(MAX_PARALLEL_REQUESTS)
        val answers = coroutineScope {
            maps.distinct().map { map -> async { map to permits.withPermit { ask(map) } } }.awaitAll()
        }
        versions.update { known ->
            known.toMutableMap().apply {
                answers.forEach { (map, answer) ->
                    when (answer) {
                        is SiteAnswer.Dated -> put(map, answer.modified)
                        SiteAnswer.Undated -> remove(map)
                        SiteAnswer.Unreachable -> Unit
                    }
                }
            }
        }
        logger.i("Checked %d maps for updates", answers.size)
    }

    /** Asks the site when [map] last changed. */
    private suspend fun ask(map: RemoteMapPath): SiteAnswer {
        val call = client.newCall(Request.Builder().url(site.url(map)).head().build())
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        // A cancelled call fails too, but nobody waits for it any more.
                        if (!call.isCanceled()) logger.w("Cannot ask the map site about %s: %s", map, e.toString())
                        continuation.resume(SiteAnswer.Unreachable)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response.use { it.toAnswer(map) })
                    }
                },
            )
        }
    }

    /** What this answer says about [map]. */
    private fun Response.toAnswer(map: RemoteMapPath): SiteAnswer {
        if (isSuccessful) {
            val modified = headers.getDate("Last-Modified")?.toInstant()
            if (modified == null) logger.w("The map site sends no date for %s", map)
            return modified?.let(SiteAnswer::Dated) ?: SiteAnswer.Undated
        }
        logger.w("The map site answered %d for %s", code, map)
        // The map is gone from the site: there is no update to offer. Other errors may go away.
        return if (code in GONE_CODES) SiteAnswer.Undated else SiteAnswer.Unreachable
    }

    /** What the site said about one map. */
    private sealed interface SiteAnswer {

        /** It last changed at [modified]. */
        data class Dated(val modified: Instant) : SiteAnswer

        /** It has no date for it, or no longer has it. */
        data object Undated : SiteAnswer

        /** It could not be asked, or answered with an error that may go away. */
        data object Unreachable : SiteAnswer
    }

    private companion object {
        /** How many maps are asked about at once. */
        const val MAX_PARALLEL_REQUESTS = 4

        /** Not Found and Gone: the site no longer has the map. */
        val GONE_CODES = setOf(404, 410)
    }
}
