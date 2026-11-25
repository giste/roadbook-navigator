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
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteListing
import dev.giste.map.domain.RemoteListingFailure
import dev.giste.map.domain.RemoteMapRepository
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Reads the folders of the download site over HTTP, with [DirectoryListingParser].
 *
 * Requests run on OkHttp's own threads, so [list] is main-safe without a dispatcher, and they are
 * cancelled when the calling coroutine is. Failures are logged and returned as
 * [RemoteListing.Failed].
 *
 * @param client the HTTP client.
 * @param site the addresses of the download site's folders.
 * @param logger logs failures.
 */
internal class HttpRemoteMapRepository @Inject constructor(
    @param:MapHttpClient private val client: OkHttpClient,
    private val site: MapSite,
    private val logger: Logger,
) : RemoteMapRepository {

    override suspend fun list(folder: RemoteFolderPath): RemoteListing {
        val call = client.newCall(Request.Builder().url(site.url(folder)).build())
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        // A cancelled call fails too, but nobody waits for it any more.
                        if (!call.isCanceled()) logger.w("Cannot reach the map site: %s", e.toString())
                        continuation.resume(RemoteListing.Failed(RemoteListingFailure.NO_CONNECTION))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response.use { it.toListing() })
                    }
                },
            )
        }
    }

    /** What this answer says about the folder. Reading its body may fail like the request. */
    private fun Response.toListing(): RemoteListing {
        if (!isSuccessful) {
            logger.w("The map site answered %d for %s", code, request.url.encodedPath)
            return RemoteListing.Failed(RemoteListingFailure.SERVER_ERROR)
        }
        val html = try {
            body.string()
        } catch (e: IOException) {
            logger.w("Cannot read the map site's answer: %s", e.toString())
            return RemoteListing.Failed(RemoteListingFailure.NO_CONNECTION)
        }
        val entries = DirectoryListingParser.parse(html)
        if (entries == null) {
            logger.w("The map site's answer for %s is not a folder listing", request.url.encodedPath)
            return RemoteListing.Failed(RemoteListingFailure.UNREADABLE_LISTING)
        }
        return RemoteListing.Listed(entries)
    }
}
