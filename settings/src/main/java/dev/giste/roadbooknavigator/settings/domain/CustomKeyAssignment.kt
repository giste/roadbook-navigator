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

import dev.giste.roadbooknavigator.settings.RemoteAction
import dev.giste.roadbooknavigator.settings.RemoteKey
import dev.giste.roadbooknavigator.settings.RemoteKeys

/**
 * These keys with [key] as the only key of [action]. If another action had [key], it loses it, so
 * a key still triggers one action; that action may be left with no key. The other actions keep
 * their keys.
 *
 * @param action the action to assign [key] to.
 * @param key the key.
 * @return the new keys.
 */
internal fun RemoteKeys.withOnlyKey(action: RemoteAction, key: RemoteKey): RemoteKeys =
    RemoteKeys(
        RemoteAction.entries.associateWith { other ->
            if (other == action) setOf(key) else keysFor(other) - key
        },
    )
