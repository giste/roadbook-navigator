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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import app.cash.turbine.test
import dev.giste.map.FakeLogger
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.MapDownload
import dev.giste.map.domain.MapDownloadFailure
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteMapPath
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkManagerMapDownloadRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val europe = RemoteFolderPath.ROOT.child("europe")
    private val andorra = RemoteMapPath(europe, "andorra.map")
    private val spain = RemoteMapPath(europe, "spain.map")

    private lateinit var repository: WorkManagerMapDownloadRepository

    @Before
    fun setUp() {
        // Its works are never run: the test WorkManager waits for the constraints to be met.
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        repository = WorkManagerMapDownloadRepository(context, FakeLogger())
    }

    private fun worksOf(map: RemoteMapPath): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosByTag(WorkManagerMapDownloadRepository.tagOf(map)).get()

    // --- start ---

    @Test
    fun start_queuesTheMapOnWiFi() = runTest {
        repository.start(andorra)

        val work = worksOf(andorra).single()
        assertEquals(WorkInfo.State.ENQUEUED, work.state)
        assertEquals(NetworkType.UNMETERED, work.constraints.requiredNetworkType)
    }

    @Test
    fun start_sameMapTwice_queuesItOnce() = runTest {
        repository.start(andorra)

        repository.start(andorra)

        assertEquals(1, worksOf(andorra).size)
    }

    @Test
    fun start_twoMaps_runOneAfterTheOther() = runTest {
        repository.start(andorra)

        repository.start(spain)

        assertEquals(WorkInfo.State.ENQUEUED, worksOf(andorra).single().state)
        // Blocked until the download before it ends.
        assertEquals(WorkInfo.State.BLOCKED, worksOf(spain).single().state)
    }

    @Test
    fun downloads_queuedMaps_areQueued() = runTest {
        repository.start(andorra)
        repository.start(spain)

        repository.downloads.test {
            assertEquals(
                setOf(MapDownload.Queued(andorra), MapDownload.Queued(spain)),
                awaitItem().toSet(),
            )
        }
    }

    @Test
    fun downloads_nothingStarted_isEmpty() = runTest {
        repository.downloads.test {
            assertEquals(emptyList<MapDownload>(), awaitItem())
        }
    }

    // --- toDownloads ---

    private fun work(
        map: RemoteMapPath?,
        state: WorkInfo.State,
        progress: Data = Data.EMPTY,
        output: Data = Data.EMPTY,
    ) = WorkInfo(
        id = UUID.randomUUID(),
        state = state,
        tags = setOfNotNull(map?.let(WorkManagerMapDownloadRepository::tagOf), "dev.giste.map.data.MapDownloadWorker"),
        outputData = output,
        progress = progress,
    )

    private fun failure(reason: String) = workDataOf(MapDownloadWorker.KEY_FAILURE to reason)

    @Test
    fun toDownloads_waitingWork_isQueued() {
        for (state in listOf(WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED)) {
            assertEquals(listOf(MapDownload.Queued(andorra)), listOf(work(andorra, state)).toDownloads())
        }
    }

    @Test
    fun toDownloads_runningWork_isRunningWithItsProgress() {
        val progress = workDataOf(MapDownloadWorker.KEY_DOWNLOADED to 25L, MapDownloadWorker.KEY_TOTAL to 100L)

        assertEquals(
            listOf(MapDownload.Running(andorra, FileSize(25), FileSize(100))),
            listOf(work(andorra, WorkInfo.State.RUNNING, progress)).toDownloads(),
        )
    }

    @Test
    fun toDownloads_runningWorkOfUnknownSize_hasNoTotal() {
        val progress = workDataOf(
            MapDownloadWorker.KEY_DOWNLOADED to 25L,
            MapDownloadWorker.KEY_TOTAL to MapDownloadWorker.UNKNOWN,
        )

        assertEquals(
            listOf(MapDownload.Running(andorra, FileSize(25), total = null)),
            listOf(work(andorra, WorkInfo.State.RUNNING, progress)).toDownloads(),
        )
    }

    @Test
    fun toDownloads_runningWorkBeforeItsFirstReport_hasNothingDownloaded() {
        assertEquals(
            listOf(MapDownload.Running(andorra, FileSize(0), total = null)),
            listOf(work(andorra, WorkInfo.State.RUNNING)).toDownloads(),
        )
    }

    @Test
    fun toDownloads_successfulWork_isLeftOut() {
        assertTrue(listOf(work(andorra, WorkInfo.State.SUCCEEDED)).toDownloads().isEmpty())
    }

    @Test
    fun toDownloads_workThatFailedForGood_isFailedWithItsReason() {
        for (reason in MapDownloadFailure.entries) {
            assertEquals(
                listOf(MapDownload.Failed(andorra, reason)),
                listOf(work(andorra, WorkInfo.State.SUCCEEDED, output = failure(reason.name))).toDownloads(),
            )
        }
    }

    @Test
    fun toDownloads_unknownFailure_isLeftOut() {
        assertTrue(listOf(work(andorra, WorkInfo.State.SUCCEEDED, output = failure("GONE"))).toDownloads().isEmpty())
    }

    @Test
    fun toDownloads_failedOrCancelledWork_isLeftOut() {
        for (state in listOf(WorkInfo.State.FAILED, WorkInfo.State.CANCELLED)) {
            assertTrue(listOf(work(andorra, state)).toDownloads().isEmpty())
        }
    }

    @Test
    fun toDownloads_workWithoutAMapTag_isLeftOut() {
        assertTrue(listOf(work(null, WorkInfo.State.ENQUEUED)).toDownloads().isEmpty())
    }

    @Test
    fun toDownloads_workWithAnUnsafeMapTag_isLeftOut() {
        val unsafe = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.ENQUEUED,
            tags = setOf(WorkManagerMapDownloadRepository.TAG_PREFIX + "../secret.map"),
        )

        assertTrue(listOf(unsafe).toDownloads().isEmpty())
    }

    @Test
    fun toDownloads_mapStartedAgainAfterAFailure_isTheNewDownload() {
        val works = listOf(
            work(andorra, WorkInfo.State.SUCCEEDED, output = failure("SERVER_ERROR")),
            work(andorra, WorkInfo.State.ENQUEUED),
        )

        assertEquals(listOf(MapDownload.Queued(andorra)), works.toDownloads())
    }

    @Test
    fun toDownloads_severalMaps_hasOneDownloadEach() {
        val works = listOf(
            work(andorra, WorkInfo.State.SUCCEEDED, output = failure("SERVER_ERROR")),
            work(andorra, WorkInfo.State.SUCCEEDED, output = failure("STORAGE_ERROR")),
            work(spain, WorkInfo.State.BLOCKED),
        )

        val downloads = works.toDownloads()

        assertEquals(2, downloads.size)
        assertEquals(setOf(andorra, spain), downloads.map { it.map }.toSet())
    }
}
