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
import dev.giste.location.LocationSettingsSource
import dev.giste.roadbooknavigator.settings.DefaultLocationSettingsSource

/**
 * Binds the [LocationSettingsSource] that `:location` requires.
 *
 * Temporary: `:settings` will bind the real source, and this module will be deleted then.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface LocationSettingsModule {

    /** Provides [DefaultLocationSettingsSource] wherever a [LocationSettingsSource] is injected. */
    @Binds
    fun bindLocationSettingsSource(source: DefaultLocationSettingsSource): LocationSettingsSource
}
