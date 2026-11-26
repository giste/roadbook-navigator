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

import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import dev.giste.roadbook.ImportRoadbookResult
import dev.giste.roadbook.RoadbookController
import dev.giste.roadbooknavigator.ui.dashboard.DashboardTestTags
import java.io.File
import java.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** How long to wait for work on the real dispatchers (DataStore, the application scope). */
const val TIMEOUT_MILLIS: Long = 5_000

/**
 * Imports `route_example.rn2` (4 waypoints, the second one at 0.11 km) through the real
 * controller, as the file picker would, and checks that it succeeded.
 */
fun RoadbookController.importExample() {
    val file = File(RuntimeEnvironment.getApplication().cacheDir, "route_example.rn2")
    val resource = checkNotNull(
        IntegrationTestSupport::class.java.classLoader?.getResource("route_example.rn2"),
    ) { "route_example.rn2 not found" }
    file.writeText(resource.readText())
    assertEquals(ImportRoadbookResult.Success, runBlocking { importRoadbook(Uri.fromFile(file)) })
}

/** Waits until the partial reading on the dashboard shows [value], e.g. `"0.11"`. */
fun ComposeTestRule.awaitPartial(value: String) {
    waitUntil(TIMEOUT_MILLIS) {
        onAllNodes(hasText(value) and hasAnyAncestor(hasTestTag(DashboardTestTags.PARTIAL)))
            .fetchSemanticsNodes().isNotEmpty()
    }
}

/** Waits until a node with [text] exists ([exists]) or no longer exists. */
fun ComposeTestRule.awaitText(text: String, exists: Boolean = true) {
    waitUntil(TIMEOUT_MILLIS) {
        onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() == exists
    }
}

/** Waits until a node tagged [tag] exists ([exists]) or no longer exists. */
fun ComposeTestRule.awaitTag(tag: String, exists: Boolean = true) {
    waitUntil(TIMEOUT_MILLIS) {
        onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() == exists
    }
}

/** How long [awaitValue] waits for a value before it lets the main thread run again. */
private const val AWAIT_SLICE_MILLIS: Long = 50

/**
 * Waits until [flow] emits a value that matches [predicate], or fails after [TIMEOUT_MILLIS] of
 * real time. Use it to wait for a change the UI made, e.g. a setting stored after a click.
 *
 * It never blocks the main thread for long. DataStore runs the transform of an `edit` in its
 * caller's context, and a change made from the UI comes from `viewModelScope`, on the main thread,
 * which Robolectric runs on the test thread. Blocking that thread until the value arrives would
 * deadlock whenever DataStore asks for it after the click's `waitForIdle`. So it waits in short
 * slices, and lets the main thread run in between.
 *
 * The deadline is in real time, because DataStore writes in real time on its own thread: the first
 * write of a test JVM can take a while as its classes load. `waitUntil` would not do, as under
 * Robolectric it measures its timeout on the test clock, which runs much faster than real time.
 *
 * @param flow the values to watch, e.g. a settings source's `settings`.
 * @param predicate whether a value is the one awaited.
 */
fun <T> ComposeTestRule.awaitValue(flow: Flow<T>, predicate: (T) -> Boolean) {
    val deadline = System.nanoTime() + TIMEOUT_MILLIS * 1_000_000
    while (true) {
        waitForIdle()
        val found = runBlocking {
            withTimeoutOrNull(AWAIT_SLICE_MILLIS) { flow.first(predicate) } != null
        }
        if (found) return
        if (System.nanoTime() > deadline) {
            throw AssertionError("No matching value within $TIMEOUT_MILLIS ms")
        }
    }
}

/** Meters per degree of latitude, roughly: only used to move test fixes apart. */
private const val METERS_PER_DEGREE_OF_LATITUDE: Double = 111_000.0

/**
 * Makes the device report a GPS fix moving at [kilometersPerHour], [metersNorth] of a fixed
 * point, through Robolectric's location manager, so it reaches the app through the real
 * `:location` module.
 *
 * Robolectric applies the app's location request like a device does: it drops fixes that come
 * sooner or closer than the request allows. So the clock first moves on by a second, and each fix
 * of a test needs a different [metersNorth] (a few meters apart).
 */
fun simulateFixAt(kilometersPerHour: Float, metersNorth: Double) {
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
    val locationManager = RuntimeEnvironment.getApplication()
        .getSystemService(LocationManager::class.java)
    val fix = Location(LocationManager.GPS_PROVIDER).apply {
        latitude = 40.0 + metersNorth / METERS_PER_DEGREE_OF_LATITUDE
        longitude = -3.0
        accuracy = 5f
        speed = kilometersPerHour / 3.6f
        time = System.currentTimeMillis()
        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
    }
    shadowOf(locationManager).simulateLocation(fix)
}

/** Anchor for [IntegrationTestSupport::class] to find the test resources. */
private object IntegrationTestSupport

