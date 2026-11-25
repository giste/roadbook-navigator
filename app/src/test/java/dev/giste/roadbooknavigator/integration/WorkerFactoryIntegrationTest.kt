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
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.testing.TestListenableWorkerBuilder
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The app's worker factory, which the application gives WorkManager, builds the modules' workers
 * with the real Hilt graph, so their work runs on a device.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class WorkerFactoryIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun workerFactory_mapDownloadWorker_isBuiltWithItsDependencies() {
        val context: Context = ApplicationProvider.getApplicationContext()

        val workerClass = Class.forName(MAP_DOWNLOAD_WORKER).asSubclass(ListenableWorker::class.java)

        val worker = TestListenableWorkerBuilder.from(context, OneTimeWorkRequest.Builder(workerClass).build())
            .setWorkerFactory(workerFactory)
            .build()

        assertEquals(MAP_DOWNLOAD_WORKER, worker::class.java.name)
    }

    private companion object {
        /** `:map`'s download worker, which is internal to `:map`, so it is named here. */
        const val MAP_DOWNLOAD_WORKER = "dev.giste.map.data.MapDownloadWorker"
    }
}
