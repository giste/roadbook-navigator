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

package dev.giste.roadbooknavigator.settings.domain

import kotlinx.coroutines.flow.Flow

/**
 * The stored settings of one module, of type [T], e.g. `RoadbookSettings`.
 *
 * @param T the module's settings model.
 */
internal interface SettingsRepository<T> {

    /**
     * The current settings. Collecting it first emits the stored settings, then every change,
     * without repeated values. Values that were never stored, or cannot be read, are the module's
     * defaults. It never throws.
     */
    val settings: Flow<T>

    /**
     * Changes the settings and stores them at once.
     *
     * @param transform gives the new settings from the current ones.
     * @return [SettingsUpdateResult.SAVED], or [SettingsUpdateResult.NOT_SAVED] if storage failed,
     * in which case the settings do not change.
     */
    suspend fun update(transform: (T) -> T): SettingsUpdateResult
}

/** Whether [SettingsRepository.update] stored the new settings. */
internal enum class SettingsUpdateResult {

    /** The new settings are stored, and [SettingsRepository.settings] emits them. */
    SAVED,

    /** Storage failed: the settings did not change. */
    NOT_SAVED,
}
