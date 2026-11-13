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

package dev.giste.core.coroutines.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.giste.core.coroutines.ApplicationScope
import dev.giste.core.coroutines.DefaultDispatcher
import dev.giste.core.coroutines.IoDispatcher
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Provides the qualified coroutine dispatchers and the application-wide scope.
 *
 * Dispatchers are unscoped because the `Dispatchers` objects are already singletons. The scope is
 * a [Singleton] so the whole app shares one scope and one `SupervisorJob`.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object CoroutinesModule {

    /** Provides `Dispatchers.IO` wherever an [IoDispatcher] is injected. */
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /** Provides `Dispatchers.Default` wherever a [DefaultDispatcher] is injected. */
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /**
     * Provides the [ApplicationScope].
     *
     * A `SupervisorJob` keeps the scope alive when one child fails, so an unrelated failure
     * cannot stop every other long-running collector in the app.
     *
     * @param dispatcher the dispatcher the scope runs on by default.
     * @return a scope that is never cancelled during the life of the process.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(@DefaultDispatcher dispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcher)
}
