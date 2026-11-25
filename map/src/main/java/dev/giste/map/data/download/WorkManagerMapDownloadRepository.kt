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

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.giste.core.logging.Logger
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.MapDownload
import dev.giste.map.domain.MapDownloadFailure
import dev.giste.map.domain.MapDownloadRepository
import dev.giste.map.domain.RemoteMapPath
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.withLock

/**
 * Downloads maps with WorkManager, which runs them in the background and keeps them across
 * process deaths and restarts.
 *
 * All downloads are one chain of unique work, [QUEUE], so they run one at a time, in the order
 * they were started: several big downloads at once would only share the bandwidth. Each work is
 * a [MapDownloadWorker] tagged with its map, which is how [downloads] tells them apart, and with
 * its place in the queue ([ORDER_TAG_PREFIX]), as [WorkInfo] does not show a work's input. A
 * download that fails for good still succeeds as work (see [MapDownloadWorker]), so the rest of
 * the chain runs.
 *
 * Works wait for Wi-Fi unless the rider allowed mobile data, which then applies to every work in
 * the queue ([WorkManager.updateWork]; a running work keeps its network until its next try).
 *
 * WorkManager cannot take one work out of a chain: cancelling it cancels the works after it, and
 * the next work appended to a chain with a cancelled end deletes the whole chain, running work
 * included. So [cancel] cancels the whole queue, throws away the cancelled map's download, and
 * queues the other maps again in their order with what they were allowed. A download that was
 * running stops for a moment and resumes where it was.
 *
 * WorkManager keeps finished work for a while (about a day), so a map's last failure is listed
 * until it is started again, the queue is rebuilt by a cancel, or WorkManager forgets it. A failure
 * followed by a successful download of the same map may still be listed: the map is installed
 * then, which the screens show instead.
 *
 * @param context the application context, to get the app's [WorkManager]. It is got when first
 * used, after the app has configured it.
 * @param downloader throws away the download of a cancelled map.
 * @param logger logs what happens to the queue and failures of WorkManager's own storage.
 */
@Singleton
internal class WorkManagerMapDownloadRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloader: MapFileDownloader,
    private val logger: Logger,
) : MapDownloadRepository {

    private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }

    /** Makes each change to the queue read and write it in one step. */
    private val changing = Mutex()

    override val downloads: Flow<List<MapDownload>>
        get() = workManager.getWorkInfosForUniqueWorkFlow(QUEUE)
            .map { it.toDownloads() }
            .catch { error ->
                // WorkManager keeps its work in a database, which may fail to be read.
                logger.e(error, "Cannot read the map downloads")
                emit(emptyList())
            }

    override suspend fun start(map: RemoteMapPath, allowMobileData: Boolean) {
        changingQueue("start the download of the map $map") {
            val works = workManager.getWorkInfosForUniqueWorkFlow(QUEUE).first()
            val pending = works.filter { !it.state.isFinished }
            if (allowMobileData) {
                pending.filter { !it.allowsMobileData() }.forEach { allowMobileData(it) }
            }
            if (pending.any { it.map() == map }) return@changingQueue
            val order = (works.mapNotNull { it.order() }.maxOrNull() ?: -1) + 1
            enqueue(map, order, allowMobileData)
            logger.i("Queued the download of the map %s", map)
        }
    }

    override suspend fun cancel(map: RemoteMapPath) {
        changingQueue("cancel the download of the map $map") {
            val pending = workManager.getWorkInfosForUniqueWorkFlow(QUEUE).first().filter { !it.state.isFinished }
            if (pending.none { it.map() == map }) return@changingQueue
            workManager.cancelUniqueWork(QUEUE).await()
            // Waits until a running download of it has stopped writing.
            downloader.discard(map)
            val rest = pending.filter { it.map() != map }.sortedBy { it.order() ?: -1 }
            rest.forEach { work ->
                val restMap = work.map() ?: return@forEach
                enqueue(restMap, work.order() ?: 0, work.allowsMobileData())
            }
            logger.i("Cancelled the download of the map %s, %d maps still queued", map, rest.size)
        }
    }

    /**
     * Runs [change] on the queue under [changing]. WorkManager reports a failure of its own
     * storage as an exception: it is logged instead, so the screens never crash on it.
     *
     * @param what what [change] does, for the log.
     */
    private suspend fun changingQueue(what: String, change: suspend () -> Unit) {
        changing.withLock {
            try {
                change()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.e(error, "Cannot %s", what)
            }
        }
    }

    /** Appends the download of [map] to the queue. */
    private suspend fun enqueue(map: RemoteMapPath, order: Int, allowMobileData: Boolean) {
        // APPEND_OR_REPLACE: after the downloads already queued. A chain that ends in cancelled
        // or failed work (after a cancel, or a bug) is replaced, rather than failing at once.
        val request = request(map, order, allowMobileData)
        workManager.enqueueUniqueWork(QUEUE, ExistingWorkPolicy.APPEND_OR_REPLACE, request).await()
    }

    /** Lets the waiting or running [work] download on mobile data, from its next try. */
    private suspend fun allowMobileData(work: WorkInfo) {
        val map = work.map() ?: return
        workManager.updateWork(request(map, work.order() ?: 0, allowMobileData = true, id = work.id)).result()
    }

    /**
     * Waits for this future's result without blocking. WorkManager answers with Guava's
     * `ListenableFuture`, and `:map` has no coroutine adapter for it, so this is a small one.
     */
    private suspend fun <T> ListenableFuture<T>.result(): T = suspendCancellableCoroutine { continuation ->
        addListener(
            {
                try {
                    continuation.resume(get())
                } catch (error: ExecutionException) {
                    continuation.resumeWithException(error.cause ?: error)
                }
            },
            // The listener only hands the result over, so it may run on whichever thread completes it.
            Runnable::run,
        )
    }

    /**
     * The work that downloads [map]: on Wi-Fi only unless [allowMobileData], retried with a
     * growing delay, and tagged with its map and its place in the queue.
     */
    private fun request(
        map: RemoteMapPath,
        order: Int,
        allowMobileData: Boolean,
        id: UUID? = null,
    ): OneTimeWorkRequest {
        val network = if (allowMobileData) NetworkType.CONNECTED else NetworkType.UNMETERED
        return OneTimeWorkRequestBuilder<MapDownloadWorker>()
            .apply { if (id != null) setId(id) }
            .setInputData(workDataOf(MapDownloadWorker.KEY_MAP to map.toString()))
            .setConstraints(Constraints(requiredNetworkType = network))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .addTag(tagOf(map))
            .addTag(ORDER_TAG_PREFIX + order)
            .build()
    }

    /** Whether this work may use mobile data. */
    private fun WorkInfo.allowsMobileData(): Boolean = constraints.requiredNetworkType != NetworkType.UNMETERED

    /** This work's place in the queue, or `null` if it has none (queued before places existed). */
    private fun WorkInfo.order(): Int? =
        tags.firstOrNull { it.startsWith(ORDER_TAG_PREFIX) }?.removePrefix(ORDER_TAG_PREFIX)?.toIntOrNull()

    /** The name of the work chain and the tags of its works. */
    companion object {

        /** The unique work that holds every map download. */
        const val QUEUE: String = "map_downloads"

        /** The start of the tag that says which map a work downloads. */
        const val TAG_PREFIX: String = "map_download:"

        /** The start of the tag that says a work's place in the queue, e.g. `map_queue_order:3`. */
        const val ORDER_TAG_PREFIX: String = "map_queue_order:"

        /** The first delay before a download that stopped is tried again. It doubles each time. */
        private const val BACKOFF_SECONDS = 30L

        /**
         * The tag of the work that downloads [map].
         *
         * @param map the map.
         * @return [TAG_PREFIX] and the map's path, e.g. `map_download:europe/andorra.map`.
         */
        fun tagOf(map: RemoteMapPath): String = TAG_PREFIX + map
    }
}

/**
 * The downloads these works stand for, at most one per map: the waiting or running work of a map
 * if it has one, or else its last failure. Successful works are left out, as their maps are
 * installed, and so are works that failed or were cancelled, which only a bug causes.
 *
 * @return the downloads.
 */
internal fun List<WorkInfo>.toDownloads(): List<MapDownload> =
    mapNotNull { info -> info.map()?.let { it to info } }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        .mapNotNull { (map, infos) ->
            val active = infos.firstOrNull { !it.state.isFinished }
            if (active != null) active.toActiveDownload(map) else infos.firstNotNullOfOrNull { it.toFailure(map) }
        }

/** The map this work downloads, from its tag, or `null` if it has none that can be read. */
private fun WorkInfo.map(): RemoteMapPath? {
    val path = tags.firstOrNull { it.startsWith(WorkManagerMapDownloadRepository.TAG_PREFIX) }
        ?.removePrefix(WorkManagerMapDownloadRepository.TAG_PREFIX)
        ?: return null
    return try {
        RemoteMapPath.parse(path)
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** The download of [map] that this waiting or running work stands for. */
private fun WorkInfo.toActiveDownload(map: RemoteMapPath): MapDownload =
    if (state == WorkInfo.State.RUNNING) {
        val total = progress.getLong(MapDownloadWorker.KEY_TOTAL, MapDownloadWorker.UNKNOWN)
        MapDownload.Running(
            map = map,
            downloaded = FileSize(progress.getLong(MapDownloadWorker.KEY_DOWNLOADED, 0L).coerceAtLeast(0L)),
            total = total.takeIf { it >= 0 }?.let(::FileSize),
        )
    } else {
        // ENQUEUED or BLOCKED: waiting for Wi-Fi, for a retry or for the downloads before it.
        MapDownload.Queued(map)
    }

/** The failure of [map] this finished work reports, or `null` if it reports none. */
private fun WorkInfo.toFailure(map: RemoteMapPath): MapDownload.Failed? {
    if (state != WorkInfo.State.SUCCEEDED) return null
    val name = outputData.getString(MapDownloadWorker.KEY_FAILURE) ?: return null
    val reason = MapDownloadFailure.entries.firstOrNull { it.name == name } ?: return null
    return MapDownload.Failed(map, reason)
}
