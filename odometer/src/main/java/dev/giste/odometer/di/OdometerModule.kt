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

package dev.giste.odometer.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.giste.core.coroutines.IoDispatcher
import dev.giste.odometer.OdometerController
import dev.giste.odometer.data.DataStoreOdometerStateRepository
import dev.giste.odometer.data.OdometerStateDataStore
import dev.giste.odometer.data.OdometerTimeSource
import dev.giste.odometer.domain.DefaultOdometerController
import dev.giste.odometer.domain.OdometerStateRepository
import javax.inject.Singleton
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Binds the `:odometer` implementations for the whole app.
 *
 * The app must bind an `OdometerLocationSource` and an `OdometerSettingsSource`, which
 * `:odometer` does not provide.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface OdometerModule {

    /**
     * Provides [DefaultOdometerController] wherever an [OdometerController] is injected.
     *
     * A [Singleton], so every collector shares one measurement.
     */
    @Binds
    @Singleton
    fun bindOdometerController(controller: DefaultOdometerController): OdometerController

    /**
     * Provides [DataStoreOdometerStateRepository] wherever an [OdometerStateRepository] is
     * injected.
     *
     * A [Singleton], because it holds the state in memory.
     */
    @Binds
    @Singleton
    fun bindOdometerStateRepository(
        repository: DataStoreOdometerStateRepository,
    ): OdometerStateRepository

    /** Storage and time used by the bindings above. */
    companion object {

        /**
         * Provides the DataStore of the odometer state.
         *
         * A [Singleton], because DataStore allows only one instance per file. A corrupted file is
         * replaced with an empty one, so the odometer starts from zero instead of failing.
         *
         * @param context the application context, to locate the file.
         * @param ioDispatcher the dispatcher DataStore reads and writes the file on.
         * @return the DataStore stored in the `odometer_state` file.
         */
        @Provides
        @Singleton
        @OdometerStateDataStore
        fun provideOdometerStateDataStore(
            @ApplicationContext context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
            produceFile = {
                context.preferencesDataStoreFile(DataStoreOdometerStateRepository.FILE_NAME)
            },
        )

        /**
         * Provides the monotonic time source, which does not jump when the device clock changes.
         *
         * @return [TimeSource.Monotonic].
         */
        @Provides
        @OdometerTimeSource
        fun provideOdometerTimeSource(): TimeSource = TimeSource.Monotonic
    }
}
