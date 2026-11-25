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

import app.cash.turbine.test
import dev.giste.map.FakeLogger
import dev.giste.map.MapDownloadServer
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteMapPath
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** [HttpMapUpdateRepository] against a local web server, so no test uses the network. */
class HttpMapUpdateRepositoryTest {

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

    private val europe = RemoteFolderPath.ROOT.child("europe")
    private val andorra = RemoteMapPath(europe, "andorra.map")
    private val spain = RemoteMapPath(europe, "spain.map")

    private val september = Instant.parse("2026-09-07T10:15:30Z")
    private val october = Instant.parse("2026-10-07T08:00:00Z")

    /** What the server answers for each path below the maps folder; anything else is a 404. */
    private val answers = ConcurrentHashMap<String, MockResponse>()

    /** The requests the server got: method and path. */
    private val requests = ConcurrentHashMap.newKeySet<String>()

    @Before
    fun startServer() {
        // Requests run in parallel, so each map's answer is found by its path, not by order.
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath.removePrefix("/maps/v5/")
                requests += "${request.method} $path"
                return answers[path] ?: MockResponse.Builder().code(404).build()
            }
        }
        server.start()
    }

    @After
    fun stopServer() {
        server.close()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    /** A repository for the maps folder `/maps/v5/` of the local server. */
    private fun repository(): HttpMapUpdateRepository {
        val site = MapSite(Optional.of(MapDownloadServer(server.url("/maps/v5/").toString())))
        return HttpMapUpdateRepository(client, site, logger)
    }

    /** A successful answer that dates the map [modified], or gives no date if it is `null`. */
    private fun dated(modified: Instant?) = MockResponse.Builder().code(200).apply {
        if (modified != null) {
            addHeader("Last-Modified", DateTimeFormatter.RFC_1123_DATE_TIME.format(modified.atZone(ZoneOffset.UTC)))
        }
    }.build()

    @Test
    fun siteVersions_beforeAnyCheck_isEmpty() = runTest {
        repository().siteVersions.test {
            assertEquals(emptyMap<RemoteMapPath, Instant>(), awaitItem())
        }
    }

    @Test
    fun check_maps_asksTheSiteWithHeadRequests() = runTest {
        answers["europe/andorra.map"] = dated(september)
        answers["europe/spain.map"] = dated(october)

        repository().check(listOf(andorra, spain))

        assertEquals(setOf("HEAD europe/andorra.map", "HEAD europe/spain.map"), requests)
    }

    @Test
    fun check_datedMaps_keepsTheirDates() = runTest {
        answers["europe/andorra.map"] = dated(september)
        answers["europe/spain.map"] = dated(october)
        val repository = repository()

        repository.check(listOf(andorra, spain))

        repository.siteVersions.test {
            assertEquals(mapOf(andorra to september, spain to october), awaitItem())
        }
        assertTrue(logger.warnings.isEmpty())
    }

    @Test
    fun check_sameMapTwice_asksOnce() = runTest {
        answers["europe/andorra.map"] = dated(september)

        repository().check(listOf(andorra, andorra))

        assertEquals(1, server.requestCount)
    }

    @Test
    fun check_again_replacesTheDatesOfTheMapsAskedAndKeepsTheOthers() = runTest {
        answers["europe/andorra.map"] = dated(september)
        answers["europe/spain.map"] = dated(september)
        val repository = repository()
        repository.check(listOf(andorra, spain))
        answers["europe/andorra.map"] = dated(october)

        repository.check(listOf(andorra))

        repository.siteVersions.test {
            assertEquals(mapOf(andorra to october, spain to september), awaitItem())
        }
    }

    @Test
    fun check_noDate_dropsWhatWasKnown() = runTest {
        answers["europe/andorra.map"] = dated(september)
        val repository = repository()
        repository.check(listOf(andorra))
        answers["europe/andorra.map"] = dated(null)

        repository.check(listOf(andorra))

        repository.siteVersions.test {
            assertEquals(emptyMap<RemoteMapPath, Instant>(), awaitItem())
        }
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun check_mapGone_dropsWhatWasKnown() = runTest {
        answers["europe/andorra.map"] = dated(september)
        val repository = repository()
        repository.check(listOf(andorra))
        answers["europe/andorra.map"] = MockResponse.Builder().code(410).build()

        repository.check(listOf(andorra))

        repository.siteVersions.test {
            assertEquals(emptyMap<RemoteMapPath, Instant>(), awaitItem())
        }
    }

    @Test
    fun check_mapNotFound_isNotKept() = runTest {
        val repository = repository()

        repository.check(listOf(andorra))

        repository.siteVersions.test {
            assertEquals(emptyMap<RemoteMapPath, Instant>(), awaitItem())
        }
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun check_serverError_keepsWhatWasKnown() = runTest {
        answers["europe/andorra.map"] = dated(september)
        val repository = repository()
        repository.check(listOf(andorra))
        answers["europe/andorra.map"] = MockResponse.Builder().code(503).build()

        repository.check(listOf(andorra))

        repository.siteVersions.test {
            assertEquals(mapOf(andorra to september), awaitItem())
        }
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun check_serverUnreachable_keepsWhatWasKnown() = runTest {
        answers["europe/andorra.map"] = dated(september)
        val repository = repository()
        repository.check(listOf(andorra))
        server.close()

        repository.check(listOf(andorra))

        repository.siteVersions.test {
            assertEquals(mapOf(andorra to september), awaitItem())
        }
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun check_cancelled_cancelsTheRequestsWithoutLogging() = runTest {
        // The server waits long enough for the test to cancel first.
        answers["europe/andorra.map"] = MockResponse.Builder().code(200).headersDelay(30, TimeUnit.SECONDS).build()
        val repository = repository()
        val job = launch { repository.check(listOf(andorra)) }
        testScheduler.runCurrent()
        server.takeRequest()

        job.cancelAndJoin()

        assertTrue(cancelled.get())
        assertTrue(job.isCancelled)
        assertTrue(logger.warnings.isEmpty())
    }
}
