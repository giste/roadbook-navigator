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

package dev.giste.roadbooknavigator.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dev.giste.geo.Speed
import dev.giste.location.LocationSettings
import dev.giste.roadbooknavigator.settings.FakeLogger
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** [DataStoreLocationSettingsRepository] on a real DataStore file in a temporary folder. */
class DataStoreSettingsRepositoryIntegrationTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val file: File by lazy {
        File(folder.root, "${DataStoreLocationSettingsRepository.FILE_NAME}.preferences_pb")
    }

    /**
     * Opens the DataStore file in a child scope of the test, so the test can close it again
     * before reopening the same file: DataStore allows one open instance per file.
     */
    private fun TestScope.openDataStore(job: Job): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(coroutineContext + job),
            produceFile = { file },
        )

    private fun createRepository(dataStore: DataStore<Preferences>) =
        DataStoreLocationSettingsRepository(dataStore, FakeLogger())

    @Test
    fun settings_newFile_emitsTheDefaults() = runTest {
        val job = Job()

        assertEquals(LocationSettings(), createRepository(openDataStore(job)).settings.first())
        job.cancelAndJoin()
    }

    @Test
    fun update_savedSettings_areReadAfterReopeningTheFile() = runTest {
        val expected = LocationSettings(
            updateInterval = 2.seconds,
            movingSpeed = Speed.kilometersPerHour(4.0),
            stoppedSpeed = Speed.kilometersPerHour(2.0),
        )
        val firstJob = Job()
        createRepository(openDataStore(firstJob)).update { expected }
        firstJob.cancelAndJoin()

        val secondJob = Job()
        val reopened = createRepository(openDataStore(secondJob))

        assertEquals(expected, reopened.settings.first())
        secondJob.cancelAndJoin()
    }
}
