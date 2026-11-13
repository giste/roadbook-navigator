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

package dev.giste.location.di

import android.content.Context
import android.location.LocationManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.giste.location.LocationSource
import dev.giste.location.data.GpsLocationRepository
import dev.giste.location.domain.DefaultLocationSource
import dev.giste.location.domain.LocationRepository
import javax.inject.Singleton

/**
 * Binds the `:location` implementations for the whole app.
 *
 * The app must bind a `LocationSettingsSource`, which `:location` does not provide.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface LocationModule {

    /**
     * Provides [DefaultLocationSource] wherever a [LocationSource] is injected.
     *
     * A [Singleton], so every collector in the app shares one platform request.
     */
    @Binds
    @Singleton
    fun bindLocationSource(source: DefaultLocationSource): LocationSource

    /** Provides [GpsLocationRepository] wherever a [LocationRepository] is injected. */
    @Binds
    fun bindLocationRepository(repository: GpsLocationRepository): LocationRepository

    /** Platform services used by the bindings above. */
    companion object {

        /**
         * Provides the platform [LocationManager].
         *
         * @param context the application context, which lives as long as the manager is used.
         * @return the system location service.
         */
        @Provides
        fun provideLocationManager(@ApplicationContext context: Context): LocationManager =
            context.getSystemService(LocationManager::class.java)
    }
}
