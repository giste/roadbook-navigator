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

package dev.giste.roadbooknavigator.integration

import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The cancel action of a map download notification reaches `:map`'s receiver, which is injected
 * by the real Hilt graph and cancels that download in the real queue.
 *
 * `:map`'s receiver and queue are internal to `:map`, so they are reached as Android does: by the
 * receiver's class name and the queue's names, which are repeated here.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class MapDownloadCancelIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** A work that is never run: it waits for Wi-Fi, which the test WorkManager never reports. */
    class IdleWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result = Result.success()
    }

    @Before
    fun setUp() {
        hiltRule.inject()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    /** Queues a download of [path] in `:map`'s queue, as `:map` tags it. */
    private fun queue(path: String, order: Int) {
        val request = OneTimeWorkRequestBuilder<IdleWorker>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.UNMETERED))
            .addTag(MAP_TAG_PREFIX + path)
            .addTag(ORDER_TAG_PREFIX + order)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(QUEUE, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
            .result.get()
    }

    /** The states of the works that download [path]. */
    private fun statesOf(path: String): List<WorkInfo.State> =
        WorkManager.getInstance(context).getWorkInfosByTag(MAP_TAG_PREFIX + path).get().map { it.state }

    @Test
    fun cancelAction_cancelsThatMapAndKeepsTheOthersQueued() {
        queue("europe/andorra.map", order = 0)
        queue("europe/spain.map", order = 1)

        context.sendBroadcast(
            Intent().setClassName(context, RECEIVER).putExtra(EXTRA_MAP, "europe/andorra.map"),
        )
        shadowOf(Looper.getMainLooper()).idle()

        // The receiver cancels on the application scope: wait for the queue to be rebuilt.
        runBlocking {
            withTimeout(TIMEOUT_MILLIS) {
                WorkManager.getInstance(context).getWorkInfosByTagFlow(MAP_TAG_PREFIX + "europe/spain.map")
                    .first { works -> works.any { it.state == WorkInfo.State.ENQUEUED } }
            }
        }
        // Cancelled, then forgotten when the queue was rebuilt: nothing of it waits or runs.
        assertTrue(statesOf("europe/andorra.map").all { it.isFinished })
        // Queued again, now first.
        assertEquals(WorkInfo.State.ENQUEUED, statesOf("europe/spain.map").single { !it.isFinished })
    }

    private companion object {
        /** `:map`'s `MapDownloadCancelReceiver` and its extra. */
        const val RECEIVER = "dev.giste.map.data.MapDownloadCancelReceiver"
        const val EXTRA_MAP = "dev.giste.map.extra.MAP"

        /** `:map`'s download queue and the tags of its works. */
        const val QUEUE = "map_downloads"
        const val MAP_TAG_PREFIX = "map_download:"
        const val ORDER_TAG_PREFIX = "map_queue_order:"
    }
}
