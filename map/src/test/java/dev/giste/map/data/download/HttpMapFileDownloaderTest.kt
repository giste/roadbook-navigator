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

package dev.giste.map.data.download

import dev.giste.map.FakeLogger
import dev.giste.map.MapDownloadServer
import dev.giste.map.data.MapFileChecker
import dev.giste.map.data.MapFolderChanges
import dev.giste.map.data.remote.MapSite
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.MapDownloadFailure
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteMapPath
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Optional
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** [HttpMapFileDownloader] against a local web server, so no test uses the network. */
class HttpMapFileDownloaderTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val server = MockWebServer()
    private val client = OkHttpClient()
    private val logger = FakeLogger()
    private val changes = MapFolderChanges()

    /** The free space the downloader is told about. */
    private var freeBytes = Long.MAX_VALUE

    /** The files the checker rejects, by content. Every other file is readable. */
    private val unreadableContents = mutableListOf<ByteArray>()

    private val checker = MapFileChecker { file ->
        val bytes = file.readBytes()
        if (unreadableContents.any { it.contentEquals(bytes) }) "damaged" else null
    }

    /** The progress reported, in order. */
    private val progress = mutableListOf<Pair<FileSize, FileSize?>>()

    private val map = RemoteMapPath(RemoteFolderPath.ROOT.child("europe"), "andorra.map")

    /** The map's content on the site. */
    private val content = ByteArray(MAP_SIZE) { (it % 251).toByte() }

    private lateinit var mapFolder: File
    private val target: File get() = File(mapFolder, "europe/andorra.map")
    private val part: File get() = File(mapFolder, "europe/andorra.map.part")

    @Before
    fun setUp() {
        server.start()
        mapFolder = temporaryFolder.newFolder("map_files")
    }

    @After
    fun tearDown() {
        server.close()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private fun TestScope.downloader(
        dispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler),
    ) = HttpMapFileDownloader(
        client = client,
        site = MapSite(Optional.of(MapDownloadServer(server.url("/maps/v5/").toString()))),
        folder = mapFolder,
        checker = checker,
        freeSpace = { freeBytes },
        folderChanges = changes,
        ioDispatcher = dispatcher,
        logger = logger,
    )

    private suspend fun HttpMapFileDownloader.download(): MapFileDownloadResult =
        download(map) { downloaded, total -> progress += downloaded to total }

    /** Leaves the first [bytes] of the map as an earlier try would, with the site's date. */
    private fun leavePart(bytes: Int, modified: Long? = LAST_MODIFIED_MILLIS) {
        part.parentFile?.mkdirs()
        part.writeBytes(content.copyOfRange(0, bytes))
        if (modified != null) part.setLastModified(modified)
    }

    /** The whole map, with the site's date unless [lastModified] is `false`. */
    private fun whole(body: ByteArray = content, lastModified: Boolean = true): MockResponse =
        MockResponse.Builder().code(200).body(Buffer().write(body)).apply {
            if (lastModified) addHeader("Last-Modified", LAST_MODIFIED)
        }.build()

    /** The map from byte [from], saying its whole size is [total]. */
    private fun rest(from: Int, total: Int = MAP_SIZE, body: ByteArray = content.copyOfRange(from, MAP_SIZE)) =
        MockResponse.Builder()
            .code(206)
            .addHeader("Content-Range", "bytes $from-${from + body.size - 1}/$total")
            .addHeader("Last-Modified", LAST_MODIFIED)
            .body(Buffer().write(body))
            .build()

    private fun code(code: Int) = MockResponse.Builder().code(code).body("error").build()

    // --- Fresh downloads ---

    @Test
    fun download_noPart_requestsTheWholeMapFromItsFolder() = runTest {
        server.enqueue(whole())

        downloader().download()

        val request = server.takeRequest()
        assertEquals("/maps/v5/europe/andorra.map", request.url.encodedPath)
        assertNull(request.headers["Range"])
        assertNull(request.headers["If-Range"])
    }

    @Test
    fun download_wholeMap_installsItWithTheSiteDate() = runTest {
        server.enqueue(whole())

        assertEquals(MapFileDownloadResult.Done, downloader().download())

        assertArrayEquals(content, target.readBytes())
        assertEquals(LAST_MODIFIED_MILLIS, target.lastModified())
        assertFalse(part.exists())
    }

    @Test
    fun download_installed_tellsTheListings() = runTest {
        server.enqueue(whole())
        val before = changes.changes.value

        downloader().download()

        assertEquals(before + 1, changes.changes.value)
    }

    @Test
    fun download_withoutLastModified_installsItAnyway() = runTest {
        server.enqueue(whole(lastModified = false))

        assertEquals(MapFileDownloadResult.Done, downloader().download())

        assertArrayEquals(content, target.readBytes())
    }

    @Test
    fun download_olderMapInstalled_replacesIt() = runTest {
        target.parentFile?.mkdirs()
        target.writeText("the older map")
        server.enqueue(whole())

        assertEquals(MapFileDownloadResult.Done, downloader().download())

        assertArrayEquals(content, target.readBytes())
    }

    @Test
    fun download_mapInTheMapsFolder_goesToTheMapFolder() = runTest {
        server.enqueue(whole())
        val world = RemoteMapPath(RemoteFolderPath.ROOT, "world.map")

        assertEquals(MapFileDownloadResult.Done, downloader().download(world) { _, _ -> })

        assertEquals("/maps/v5/world.map", server.takeRequest().url.encodedPath)
        assertArrayEquals(content, File(mapFolder, "world.map").readBytes())
    }

    // --- Resuming ---

    @Test
    fun download_partLeft_asksForTheRestIfTheMapIsUnchanged() = runTest {
        leavePart(400)
        server.enqueue(rest(400))

        downloader().download()

        val request = server.takeRequest()
        assertEquals("bytes=400-", request.headers["Range"])
        assertEquals(LAST_MODIFIED, request.headers["If-Range"])
    }

    @Test
    fun download_restOfTheMap_installsTheWholeMap() = runTest {
        leavePart(400)
        server.enqueue(rest(400))

        assertEquals(MapFileDownloadResult.Done, downloader().download())

        assertArrayEquals(content, target.readBytes())
        assertFalse(part.exists())
    }

    @Test
    fun download_mapChangedOnTheSite_startsAgainWithTheWholeNewMap() = runTest {
        leavePart(400)
        val newContent = ByteArray(MAP_SIZE) { 7 }
        // The site sends the whole map when If-Range does not match.
        server.enqueue(whole(newContent))

        assertEquals(MapFileDownloadResult.Done, downloader().download())

        assertArrayEquals(newContent, target.readBytes())
    }

    @Test
    fun download_restFromAnotherByte_deletesThePartAndTriesAgain() = runTest {
        leavePart(400)
        server.enqueue(rest(100))

        assertEquals(MapFileDownloadResult.TryAgain, downloader().download())

        assertFalse(part.exists())
        assertFalse(target.exists())
    }

    @Test
    fun download_partialAnswerWithoutRange_deletesThePartAndTriesAgain() = runTest {
        leavePart(400)
        val rest = Buffer().write(content.copyOfRange(400, MAP_SIZE))
        server.enqueue(MockResponse.Builder().code(206).body(rest).build())

        assertEquals(MapFileDownloadResult.TryAgain, downloader().download())

        assertFalse(part.exists())
    }

    @Test
    fun download_rangeNotSatisfiable_deletesThePartAndTriesAgain() = runTest {
        leavePart(400)
        server.enqueue(code(416))

        assertEquals(MapFileDownloadResult.TryAgain, downloader().download())

        assertFalse(part.exists())
    }

    // --- Stopping early ---

    @Test
    fun download_answerEndsEarly_keepsThePartToResume() = runTest {
        leavePart(400)
        // The site says the map is bigger than what it sends.
        server.enqueue(rest(400, total = MAP_SIZE * 2))

        assertEquals(MapFileDownloadResult.TryAgain, downloader().download())

        assertArrayEquals(content, part.readBytes())
        assertEquals(LAST_MODIFIED_MILLIS, part.lastModified())
        assertFalse(target.exists())
    }

    @Test
    fun download_answerEndsEarlyWithoutLastModified_deletesThePart() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(206)
                .addHeader("Content-Range", "bytes 0-${MAP_SIZE - 1}/${MAP_SIZE * 2}")
                .body(Buffer().write(content))
                .build(),
        )

        assertEquals(MapFileDownloadResult.TryAgain, downloader().download())

        assertFalse(part.exists())
    }

    @Test
    fun download_connectionLost_keepsThePartWithTheSiteDate() = runTest {
        leavePart(400)
        server.enqueue(
            MockResponse.Builder()
                .code(206)
                .addHeader("Content-Range", "bytes 400-${MAP_SIZE - 1}/$MAP_SIZE")
                .addHeader("Last-Modified", LAST_MODIFIED)
                .body(Buffer().write(content.copyOfRange(400, MAP_SIZE)))
                .onResponseBody(SocketEffect.CloseSocket())
                .build(),
        )

        assertEquals(MapFileDownloadResult.TryAgain, downloader().download())

        assertTrue(part.length() >= 400)
        assertEquals(LAST_MODIFIED_MILLIS, part.lastModified())
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun download_connectionLostWithoutLastModified_deletesThePart() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body(Buffer().write(content))
                .onResponseBody(SocketEffect.CloseSocket())
                .build(),
        )

        assertEquals(MapFileDownloadResult.TryAgain, downloader().download())

        assertFalse(part.exists())
    }

    @Test
    fun download_siteUnreachable_triesAgainAndKeepsThePart() = runTest {
        leavePart(400)
        val downloader = downloader()
        server.close()

        assertEquals(MapFileDownloadResult.TryAgain, downloader.download())

        assertEquals(400L, part.length())
    }

    @Test
    fun download_cancelled_stopsAndKeepsThePartToResume() = runTest {
        val executor = Executors.newSingleThreadExecutor()
        try {
            // The body comes slowly, so the test cancels while it is being written.
            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .addHeader("Last-Modified", LAST_MODIFIED)
                    .body(Buffer().write(content))
                    .throttleBody(100, 1, TimeUnit.SECONDS)
                    .build(),
            )
            val downloader = downloader(executor.asCoroutineDispatcher())
            val job = launch { downloader.download() }
            // Starts the download, which then runs on the executor.
            testScheduler.runCurrent()
            // With a timeout, so a broken download fails the test rather than blocking it.
            checkNotNull(server.takeRequest(10, TimeUnit.SECONDS)) { "The download did not start" }

            job.cancelAndJoin()

            assertTrue(job.isCancelled)
            assertTrue(part.exists())
            assertEquals(LAST_MODIFIED_MILLIS, part.lastModified())
            assertFalse(target.exists())
        } finally {
            executor.shutdownNow()
        }
    }

    // --- Failures ---

    @Test
    fun download_notOnTheSite_failsAndDeletesThePart() = runTest {
        leavePart(400)
        server.enqueue(code(404))

        assertEquals(MapFileDownloadResult.Failed(MapDownloadFailure.SERVER_ERROR), downloader().download())

        assertFalse(part.exists())
    }

    @Test
    fun download_busyOrBrokenSite_triesAgainAndKeepsThePart() = runTest {
        for (busy in listOf(408, 429, 500, 503)) {
            leavePart(400)
            server.enqueue(code(busy))

            assertEquals("HTTP $busy", MapFileDownloadResult.TryAgain, downloader().download())
            assertEquals("HTTP $busy", 400L, part.length())
        }
    }

    @Test
    fun download_notEnoughSpace_failsAndKeepsNothing() = runTest {
        leavePart(400)
        // The rest needs 600 bytes.
        freeBytes = 599
        server.enqueue(rest(400))

        assertEquals(MapFileDownloadResult.Failed(MapDownloadFailure.NOT_ENOUGH_SPACE), downloader().download())

        assertFalse(part.exists())
        assertFalse(target.exists())
    }

    @Test
    fun download_justEnoughSpace_installsIt() = runTest {
        freeBytes = MAP_SIZE.toLong()
        server.enqueue(whole())

        assertEquals(MapFileDownloadResult.Done, downloader().download())
    }

    @Test
    fun download_unreadableMap_failsAndKeepsNothing() = runTest {
        unreadableContents += content
        server.enqueue(whole())
        val before = changes.changes.value

        assertEquals(MapFileDownloadResult.Failed(MapDownloadFailure.UNREADABLE_MAP), downloader().download())

        assertFalse(part.exists())
        assertFalse(target.exists())
        assertEquals(before, changes.changes.value)
    }

    @Test
    fun download_unreadableMap_keepsTheOlderMap() = runTest {
        target.parentFile?.mkdirs()
        target.writeText("the older map")
        unreadableContents += content
        server.enqueue(whole())

        downloader().download()

        assertEquals("the older map", target.readText())
    }

    @Test
    fun download_folderCannotBeCreated_failsWithAStorageError() = runTest {
        // A file where the map's folder should be.
        File(mapFolder, "europe").writeText("not a folder")

        assertEquals(MapFileDownloadResult.Failed(MapDownloadFailure.STORAGE_ERROR), downloader().download())

        assertEquals(1, logger.loggedErrors.size)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun download_partCannotBeWritten_failsWithAStorageError() = runTest {
        // A folder where the part file should be.
        part.mkdirs()
        server.enqueue(whole())

        assertEquals(MapFileDownloadResult.Failed(MapDownloadFailure.STORAGE_ERROR), downloader().download())
        assertFalse(target.exists())
    }

    // --- Discarding ---

    @Test
    fun discard_partLeft_deletesIt() = runTest {
        leavePart(400)

        downloader().discard(map)

        assertFalse(part.exists())
    }

    @Test
    fun discard_installedMap_keepsIt() = runTest {
        target.parentFile?.mkdirs()
        target.writeText("an installed map")

        downloader().discard(map)

        assertEquals("an installed map", target.readText())
    }

    @Test
    fun discard_nothingDownloaded_doesNothing() = runTest {
        downloader().discard(map)

        assertTrue(logger.warnings.isEmpty())
    }

    @Test
    fun discard_afterCancellingARunningDownload_leavesNoPart() = runTest {
        val executor = Executors.newSingleThreadExecutor()
        try {
            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .addHeader("Last-Modified", LAST_MODIFIED)
                    .body(Buffer().write(content))
                    .throttleBody(100, 1, TimeUnit.SECONDS)
                    .build(),
            )
            val downloader = downloader(executor.asCoroutineDispatcher())
            val job = launch { downloader.download() }
            testScheduler.runCurrent()
            checkNotNull(server.takeRequest(10, TimeUnit.SECONDS)) { "The download did not start" }
            job.cancel()

            // Waits for the download to stop writing, as the repository does after a cancel.
            downloader.discard(map)

            assertFalse(part.exists())
            assertFalse(target.exists())
        } finally {
            executor.shutdownNow()
        }
    }

    // --- Progress ---

    @Test
    fun download_wholeMap_reportsFromTheStartToTheEnd() = runTest {
        server.enqueue(whole())

        downloader().download()

        val total = FileSize(MAP_SIZE.toLong())
        assertEquals(FileSize(0) to total, progress.first())
        assertEquals(FileSize(MAP_SIZE.toLong()) to total, progress.last())
        assertEquals(progress.map { it.first.bytes }.sorted(), progress.map { it.first.bytes })
    }

    @Test
    fun download_resumed_reportsFromWhatWasKept() = runTest {
        leavePart(400)
        server.enqueue(rest(400))

        downloader().download()

        assertEquals(FileSize(400) to FileSize(MAP_SIZE.toLong()), progress.first())
    }

    @Test
    fun download_bigMap_reportsAboutEachPercent() = runTest {
        val big = ByteArray(1_000_000)
        server.enqueue(whole(big))

        downloader().download()

        // A report at the start, one per percent at most, and one at the end.
        assertTrue("${progress.size} reports", progress.size in 3..102)
        progress.zipWithNext().forEach { (before, after) ->
            assertTrue(after.first.bytes - before.first.bytes >= 10_000 || after == progress.last())
        }
    }

    @Test
    fun download_unknownSize_reportsNoTotal() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Last-Modified", LAST_MODIFIED)
                .chunkedBody(Buffer().write(content), 100)
                .build(),
        )

        assertEquals(MapFileDownloadResult.Done, downloader().download())

        assertTrue(progress.all { it.second == null })
        assertEquals(FileSize(MAP_SIZE.toLong()), progress.last().first)
    }

    private companion object {
        const val MAP_SIZE = 1_000

        /** The map's date on the site. */
        const val LAST_MODIFIED = "Sun, 06 Sep 2026 08:49:37 GMT"

        val LAST_MODIFIED_MILLIS: Long =
            ZonedDateTime.parse(LAST_MODIFIED, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    }
}
