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
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteMapPath
import java.io.File
import java.util.Optional
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * [HttpMapFileDownloader] with the renderer's own check, [VtmMapFileChecker], on real (tiny)
 * mapsforge files: the check runs on the `.part` file, before it gets its `.map` name.
 */
class HttpMapFileDownloaderIntegrationTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val server = MockWebServer()
    private val client = OkHttpClient()

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

    @Test
    fun download_validMap_passesTheRendererCheckAndIsInstalled() = runTest {
        val mapFolder = temporaryFolder.newFolder("map_files")
        val map = writeMapFile(File(temporaryFolder.root, "source.map")).readBytes()
        server.enqueue(MockResponse.Builder().code(200).body(Buffer().write(map)).build())
        val downloader = HttpMapFileDownloader(
            client = client,
            site = MapSite(Optional.of(MapDownloadServer(server.url("/maps/v5/").toString()))),
            folder = mapFolder,
            checker = VtmMapFileChecker(),
            freeSpace = { Long.MAX_VALUE },
            folderChanges = MapFolderChanges(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            logger = FakeLogger(),
        )

        val result = downloader.download(RemoteMapPath(RemoteFolderPath.ROOT, "tiny.map")) { _, _ -> }

        assertEquals(MapFileDownloadResult.Done, result)
        assertTrue(File(mapFolder, "tiny.map").isFile)
    }
}
