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

package dev.giste.core.logging.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.giste.core.logging.Logger
import dev.giste.core.logging.data.TimberLogger

/**
 * Binds the public [Logger] to its Timber implementation for the whole app.
 *
 * Unscoped on purpose: [TimberLogger] is stateless, so sharing one instance gains nothing.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface LoggerModule {

    /** Provides [TimberLogger] wherever a [Logger] is injected. */
    @Binds
    fun bindLogger(logger: TimberLogger): Logger
}
