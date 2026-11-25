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

import android.app.Notification
import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.ForegroundUpdater
import androidx.work.ListenableWorker
import androidx.work.ProgressUpdater
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import dev.giste.map.FakeLogger
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.MapDownloadFailure
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteMapPath
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapDownloadWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val logger = FakeLogger()
    private val map = RemoteMapPath(RemoteFolderPath.ROOT.child("europe"), "andorra.map")

    /** What the fake downloader answers. */
    private var result: MapFileDownloadResult = MapFileDownloadResult.Done

    /** The progress the fake downloader reports before answering. */
    private var reports: List<Pair<FileSize, FileSize?>> = emptyList()

    /** The maps the fake downloader was asked for. */
    private val downloaded = mutableListOf<RemoteMapPath>()

    private val downloader = object : MapFileDownloader {
        override suspend fun download(
            map: RemoteMapPath,
            onProgress: suspend (downloaded: FileSize, total: FileSize?) -> Unit,
        ): MapFileDownloadResult {
            downloaded += map
            reports.forEach { (done, total) -> onProgress(done, total) }
            return result
        }
    }

    /** The progress data the worker set, in order. */
    private val progress = mutableListOf<Data>()

    /** The notifications the worker ran in the foreground with, in order. */
    private val foregrounds = mutableListOf<ForegroundInfo>()

    /** When set, going to the foreground fails with it, as Android may refuse it. */
    private var foregroundFailure: Exception? = null

    private fun worker(input: Data = workDataOf(MapDownloadWorker.KEY_MAP to map.toString())): MapDownloadWorker =
        TestListenableWorkerBuilder<MapDownloadWorker>(context)
            .setInputData(input)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker = MapDownloadWorker(
                        appContext,
                        workerParameters,
                        downloader,
                        MapDownloadNotifications(appContext),
                        logger,
                    )
                },
            )
            .setProgressUpdater(
                ProgressUpdater { _, _, data ->
                    progress += data
                    done()
                },
            )
            .setForegroundUpdater(
                ForegroundUpdater { _, _, info ->
                    foregrounds += info
                    foregroundFailure?.let(::failed) ?: done()
                },
            )
            .build()

    /** A future that has completed. */
    private fun done() = CallbackToFutureAdapter.getFuture<Void?> { it.set(null) }

    /** A future that has failed with [error]. */
    private fun failed(error: Exception) = CallbackToFutureAdapter.getFuture<Void?> { it.setException(error) }

    @Test
    fun doWork_downloadsTheMapOfItsInput() = runTest {
        worker().doWork()

        assertEquals(listOf(map), downloaded)
    }

    @Test
    fun doWork_done_succeeds() = runTest {
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
    }

    @Test
    fun doWork_tryAgain_retries() = runTest {
        result = MapFileDownloadResult.TryAgain

        assertEquals(ListenableWorker.Result.retry(), worker().doWork())
    }

    @Test
    fun doWork_failedForGood_succeedsWithTheReason() = runTest {
        result = MapFileDownloadResult.Failed(MapDownloadFailure.NOT_ENOUGH_SPACE)

        val output = worker().doWork()

        // A failed work would cancel the downloads queued after it.
        assertEquals(
            ListenableWorker.Result.success(workDataOf(MapDownloadWorker.KEY_FAILURE to "NOT_ENOUGH_SPACE")),
            output,
        )
    }

    @Test
    fun doWork_noMap_failsWithoutDownloading() = runTest {
        assertEquals(ListenableWorker.Result.failure(), worker(Data.EMPTY).doWork())

        assertTrue(downloaded.isEmpty())
    }

    @Test
    fun doWork_unsafeMap_failsWithoutDownloading() = runTest {
        val input = workDataOf(MapDownloadWorker.KEY_MAP to "../../secret.map")

        assertEquals(ListenableWorker.Result.failure(), worker(input).doWork())

        assertTrue(downloaded.isEmpty())
    }

    @Test
    fun doWork_progress_isSetAsWorkProgress() = runTest {
        reports = listOf(FileSize(0) to FileSize(100), FileSize(100) to null)

        worker().doWork()

        assertEquals(
            listOf(
                workDataOf(MapDownloadWorker.KEY_DOWNLOADED to 0L, MapDownloadWorker.KEY_TOTAL to 100L),
                workDataOf(
                    MapDownloadWorker.KEY_DOWNLOADED to 100L,
                    MapDownloadWorker.KEY_TOTAL to MapDownloadWorker.UNKNOWN,
                ),
            ),
            progress,
        )
    }

    @Test
    fun doWork_runsInTheForegroundAndUpdatesTheNotification() = runTest {
        reports = listOf(FileSize(50) to FileSize(100))

        worker().doWork()

        // Once at the start, then once per report.
        assertEquals(2, foregrounds.size)
        val last = foregrounds.last().notification
        assertEquals(50, last.extras.getInt(Notification.EXTRA_PROGRESS))
    }

    @Test
    fun doWork_foregroundRefused_downloadsAnywayAndLogs() = runTest {
        foregroundFailure = IllegalStateException("Not allowed from the background")

        assertEquals(ListenableWorker.Result.success(), worker().doWork())

        assertEquals(listOf(map), downloaded)
        assertTrue(logger.warnings.isNotEmpty())
    }
}
