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

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteSettingsTest {

    private val custom = RemoteKeys(
        mapOf(RemoteAction.NEXT_WAYPOINT to setOf(RemoteKey(KeyEvent.KEYCODE_ENTER))),
    )

    @Test
    fun defaults_areTerraPirataWithItsKeysAsCustomKeys() {
        assertEquals(RemoteSettings(RemoteModel.TERRA_PIRATA, RemoteKeys.TERRA_PIRATA), RemoteSettings())
    }

    @Test
    fun keys_dnd2_areTheDnd2Keys() {
        val settings = RemoteSettings(model = RemoteModel.DND2, customKeys = custom)

        assertEquals(RemoteKeys.DND2, settings.keys)
    }

    @Test
    fun keys_terraPirata_areTheTerraPirataKeys() {
        val settings = RemoteSettings(model = RemoteModel.TERRA_PIRATA, customKeys = custom)

        assertEquals(RemoteKeys.TERRA_PIRATA, settings.keys)
    }

    @Test
    fun keys_custom_areTheCustomKeys() {
        val settings = RemoteSettings(model = RemoteModel.CUSTOM, customKeys = custom)

        assertEquals(custom, settings.keys)
    }
}
