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

package dev.giste.roadbooknavigator.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.giste.map.MapLocationSource
import dev.giste.odometer.OdometerLocationSource
import dev.giste.roadbooknavigator.data.adapter.LocationMapLocationSource
import dev.giste.roadbooknavigator.data.adapter.LocationOdometerLocationSource

/** Binds the adapters that connect one module's outputs to another module's input ports. */
@Module
@InstallIn(SingletonComponent::class)
internal interface AdapterModule {

    /** Feeds the odometer from `:location` through [LocationOdometerLocationSource]. */
    @Binds
    fun bindOdometerLocationSource(source: LocationOdometerLocationSource): OdometerLocationSource

    /** Makes the map follow `:location` through [LocationMapLocationSource]. */
    @Binds
    fun bindMapLocationSource(source: LocationMapLocationSource): MapLocationSource
}
