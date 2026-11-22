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

package dev.giste.roadbooknavigator.settings

import dev.giste.roadbooknavigator.settings.domain.SettingsRepository
import dev.giste.roadbooknavigator.settings.domain.SettingsUpdateResult
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * [SettingsRepository] for tests that keeps the settings in memory. Updates are applied unless
 * [failUpdates] is set, in which case they change nothing and return
 * [SettingsUpdateResult.NOT_SAVED].
 *
 * @param initial the settings it starts with.
 */
internal class FakeSettingsRepository<T>(initial: T) : SettingsRepository<T> {

    override val settings: MutableStateFlow<T> = MutableStateFlow(initial)

    /** When `true`, [update] stores nothing and returns [SettingsUpdateResult.NOT_SAVED]. */
    var failUpdates: Boolean = false

    override suspend fun update(transform: (T) -> T): SettingsUpdateResult {
        if (failUpdates) return SettingsUpdateResult.NOT_SAVED
        settings.value = transform(settings.value)
        return SettingsUpdateResult.SAVED
    }
}
