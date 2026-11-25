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

import dev.giste.map.FakeLogger
import dev.giste.map.MapDownloadServer
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteListing
import dev.giste.map.domain.RemoteListingFailure
import dev.giste.map.domain.RemoteMapEntry
import java.util.Optional
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** [HttpRemoteMapRepository] against a local web server, so no test uses the network. */
class HttpRemoteMapRepositoryTest {

    private val server = MockWebServer()
    private val logger = FakeLogger()

    /** Set when a call is cancelled. */
    private val cancelled = AtomicBoolean(false)

    private val client = OkHttpClient.Builder()
        .eventListener(
            object : EventListener() {
                override fun canceled(call: Call) {
                    cancelled.set(true)
                }
            },
        )
        .build()

    private val listing = """
        <html><head><title>Index of /maps/v5</title></head><body><table>
        <tr><td><img alt="[DIR]"></td><td><a href="europe/">europe/</a></td><td>2026-09-24 09:50</td><td> - </td></tr>
        <tr><td><img alt="[   ]"></td><td><a href="world.map">world.map</a></td><td>2026-05-22 21:24</td><td>2.4M</td></tr>
        </table></body></html>
    """.trimIndent()

    @Before
    fun startServer() {
        server.start()
    }

    @After
    fun stopServer() {
        server.close()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    /** A repository for the maps folder `/maps/v5/` of the local server. */
    private fun repository(): HttpRemoteMapRepository {
        val site = MapSite(Optional.of(MapDownloadServer(server.url("/maps/v5/").toString())))
        return HttpRemoteMapRepository(client, site, logger)
    }

    private fun ok(body: String = listing) = MockResponse.Builder().code(200).body(body).build()

    @Test
    fun list_root_requestsTheMapsFolder() = runTest {
        server.enqueue(ok())

        repository().list(RemoteFolderPath.ROOT)

        assertEquals("/maps/v5/", server.takeRequest().url.encodedPath)
    }

    @Test
    fun list_subFolder_requestsItWithATrailingSlash() = runTest {
        server.enqueue(ok())

        repository().list(RemoteFolderPath.parse("europe/italy"))

        assertEquals("/maps/v5/europe/italy/", server.takeRequest().url.encodedPath)
    }

    @Test
    fun list_nameNeedingEncoding_isEncoded() = runTest {
        server.enqueue(ok())

        repository().list(RemoteFolderPath.parse("la rioja"))

        assertEquals("/maps/v5/la%20rioja/", server.takeRequest().url.encodedPath)
    }

    @Test
    fun list_listing_isListed() = runTest {
        server.enqueue(ok())

        val result = repository().list(RemoteFolderPath.ROOT)

        assertEquals(
            RemoteListing.Listed(checkNotNull(DirectoryListingParser.parse(listing))),
            result,
        )
        assertEquals(
            listOf("europe", "world.map"),
            (result as RemoteListing.Listed).entries.map(RemoteMapEntry::name),
        )
        assertTrue(logger.warnings.isEmpty())
    }

    @Test
    fun list_errorStatus_isAServerError() = runTest {
        server.enqueue(MockResponse.Builder().code(404).body("Not found").build())

        val result = repository().list(RemoteFolderPath.ROOT)

        assertEquals(RemoteListing.Failed(RemoteListingFailure.SERVER_ERROR), result)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun list_notAListing_isUnreadable() = runTest {
        server.enqueue(ok("<html><title>Maintenance</title></html>"))

        val result = repository().list(RemoteFolderPath.ROOT)

        assertEquals(RemoteListing.Failed(RemoteListingFailure.UNREADABLE_LISTING), result)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun list_serverUnreachable_isNoConnection() = runTest {
        val repository = repository()
        server.close()

        val result = repository.list(RemoteFolderPath.ROOT)

        assertEquals(RemoteListing.Failed(RemoteListingFailure.NO_CONNECTION), result)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun list_connectionLostWhileReadingTheBody_isNoConnection() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body(listing)
                .onResponseBody(SocketEffect.CloseSocket())
                .build(),
        )

        val result = repository().list(RemoteFolderPath.ROOT)

        assertEquals(RemoteListing.Failed(RemoteListingFailure.NO_CONNECTION), result)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun list_cancelled_cancelsTheRequestWithoutLogging() = runTest {
        // The server waits long enough for the test to cancel first.
        server.enqueue(MockResponse.Builder().code(200).body(listing).headersDelay(30, TimeUnit.SECONDS).build())
        val repository = repository()
        val job = launch { repository.list(RemoteFolderPath.ROOT) }
        testScheduler.runCurrent()
        server.takeRequest()

        job.cancelAndJoin()

        assertTrue(cancelled.get())
        assertTrue(job.isCancelled)
        assertTrue(logger.warnings.isEmpty())
    }
}
