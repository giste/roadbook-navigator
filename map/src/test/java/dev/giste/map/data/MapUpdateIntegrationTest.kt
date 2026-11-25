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

package dev.giste.map.data

import dev.giste.map.FakeLogger
import dev.giste.map.MapDownloadServer
import dev.giste.map.domain.CheckMapUpdatesUseCase
import dev.giste.map.domain.InstalledMapFile
import dev.giste.map.domain.InstalledMapFiles
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteMapPath
import dev.giste.map.domain.isOutdated
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Optional
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Update detection end to end, with the real parts that must agree on a map's date: the
 * downloader writes the site's `Last-Modified` as the installed file's modification time, the
 * file listing reads it back, and the update check compares it with the date the site sends to
 * `HEAD` requests. Only the site is local (MockWebServer) and the free space is faked. The map is
 * a real (tiny) mapsforge file, checked by VTM.
 */
class MapUpdateIntegrationTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val server = MockWebServer()
    private val client = OkHttpClient()
    private val logger = FakeLogger()

    private val europe = RemoteFolderPath.ROOT.child("europe")
    private val andorra = RemoteMapPath(europe, "andorra.map")

    private val september = Instant.parse("2026-09-07T07:30:19Z")
    private val october = Instant.parse("2026-10-07T07:30:19Z")

    /** When the site says Andorra last changed. Tests move it to publish a new version. */
    @Volatile
    private var siteDate: Instant = september

    /** How many times the site sent the whole map. */
    private val downloads = AtomicInteger(0)

    private lateinit var mapFolder: File
    private lateinit var mapBytes: ByteArray

    @Before
    fun setUp() {
        mapFolder = temporaryFolder.newFolder("map_files")
        mapBytes = writeMapFile(File(temporaryFolder.root, "source.map")).readBytes()
        // The site has Andorra only, with its date in both answers, as the mapsforge server does.
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.url.encodedPath != "/maps/v5/europe/andorra.map") return MockResponse.Builder().code(404).build()
                val answer = MockResponse.Builder().code(200).addHeader("Last-Modified", httpDate(siteDate))
                if (request.method == "GET") {
                    downloads.incrementAndGet()
                    answer.body(Buffer().write(mapBytes))
                }
                return answer.build()
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

    /** The real parts, on one map folder and one site. */
    private inner class Parts(scope: TestScope) {
        private val site = MapSite(Optional.of(MapDownloadServer(server.url("/maps/v5/").toString())))
        private val folderChanges = MapFolderChanges()
        private val io = StandardTestDispatcher(scope.testScheduler)

        val downloader = HttpMapFileDownloader(
            client = client,
            site = site,
            folder = mapFolder,
            checker = VtmMapFileChecker(),
            freeSpace = { Long.MAX_VALUE },
            folderChanges = folderChanges,
            ioDispatcher = io,
            logger = logger,
        )
        val files = LocalMapFileRepository(mapFolder, VtmMapFileChecker(), folderChanges, io, logger)
        val updates = HttpMapUpdateRepository(client, site, logger)
        val checkUpdates = CheckMapUpdatesUseCase(files, updates)

        /** Andorra as the map folder lists it now. */
        suspend fun installedAndorra(): InstalledMapFile =
            (files.installedFiles.first() as InstalledMapFiles.Listed).files.single()

        /** Checks for updates, then tells whether Andorra is outdated. */
        suspend fun andorraOutdatedAfterCheck(): Boolean {
            checkUpdates()
            return installedAndorra().isOutdated(updates.siteVersions.first())
        }
    }

    @Test
    fun downloadedMap_siteUnchanged_isNotOutdated() = runTest {
        val parts = Parts(this)
        assertEquals(MapFileDownloadResult.Done, parts.downloader.download(andorra) { _, _ -> })

        assertFalse(parts.andorraOutdatedAfterCheck())
    }

    @Test
    fun downloadedMap_siteChangedIt_isOutdated() = runTest {
        val parts = Parts(this)
        parts.downloader.download(andorra) { _, _ -> }

        siteDate = october

        assertTrue(parts.andorraOutdatedAfterCheck())
    }

    @Test
    fun outdatedMap_downloadedAgain_isReplacedAndUpToDate() = runTest {
        val parts = Parts(this)
        parts.downloader.download(andorra) { _, _ -> }
        siteDate = october
        assertTrue(parts.andorraOutdatedAfterCheck())

        assertEquals(MapFileDownloadResult.Done, parts.downloader.download(andorra) { _, _ -> })

        // The check's dates are still October's: the new file now has that date, so no recheck
        // is needed for the mark to go.
        assertFalse(parts.installedAndorra().isOutdated(parts.updates.siteVersions.first()))
        assertEquals(october, parts.installedAndorra().modified)
        assertEquals(2, downloads.get())
        assertEquals(listOf("andorra.map"), File(mapFolder, "europe").list()?.toList())
    }

    @Test
    fun mapCopiedByHand_afterTheSitesDate_isNotOutdated() = runTest {
        copyAndorraByHand(modified = september.plusSeconds(3_600))

        assertFalse(Parts(this).andorraOutdatedAfterCheck())
    }

    @Test
    fun mapCopiedByHand_beforeTheSitesDate_isOutdated() = runTest {
        copyAndorraByHand(modified = september.minusSeconds(3_600))

        assertTrue(Parts(this).andorraOutdatedAfterCheck())
    }

    /** Puts Andorra in the map folder as if copied there by hand at [modified]. */
    private fun copyAndorraByHand(modified: Instant) {
        File(mapFolder, "europe").mkdirs()
        File(mapFolder, "europe/andorra.map").apply {
            writeBytes(mapBytes)
            setLastModified(modified.toEpochMilli())
        }
    }

    private companion object {
        /** [instant] as an HTTP date, e.g. `Mon, 7 Sep 2026 07:30:19 GMT`. */
        fun httpDate(instant: Instant): String = DateTimeFormatter.RFC_1123_DATE_TIME.format(instant.atZone(ZoneOffset.UTC))
    }
}
