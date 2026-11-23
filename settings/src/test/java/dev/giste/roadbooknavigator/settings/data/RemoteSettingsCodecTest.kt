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

package dev.giste.roadbooknavigator.settings.data

import android.view.KeyEvent
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import dev.giste.roadbooknavigator.settings.FakeLogger
import dev.giste.roadbooknavigator.settings.RemoteAction
import dev.giste.roadbooknavigator.settings.RemoteKey
import dev.giste.roadbooknavigator.settings.RemoteKeys
import dev.giste.roadbooknavigator.settings.RemoteModel
import dev.giste.roadbooknavigator.settings.RemoteSettings
import dev.giste.roadbooknavigator.settings.data.RemoteSettingsCodec.CUSTOM_KEYS
import dev.giste.roadbooknavigator.settings.data.RemoteSettingsCodec.MODEL_KEY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteSettingsCodecTest {

    private val logger = FakeLogger()

    /** Custom keys with two keys on one action and none on another. */
    private val custom = RemoteKeys(
        mapOf(
            RemoteAction.NEXT_WAYPOINT to setOf(RemoteKey(KeyEvent.KEYCODE_ENTER)),
            RemoteAction.PREVIOUS_WAYPOINT to setOf(RemoteKey(KeyEvent.KEYCODE_DPAD_DOWN)),
            RemoteAction.INCREASE_PARTIAL to
                setOf(RemoteKey(KeyEvent.KEYCODE_PAGE_UP), RemoteKey(KeyEvent.KEYCODE_PLUS)),
            RemoteAction.DECREASE_PARTIAL to setOf(RemoteKey(KeyEvent.KEYCODE_PAGE_DOWN)),
        ),
    )

    private fun customKey(action: RemoteAction) = checkNotNull(CUSTOM_KEYS[action])

    @Test
    fun read_nothingStored_isTheDefaults() {
        assertEquals(RemoteSettings(), RemoteSettingsCodec.read(preferencesOf(), logger))
    }

    @Test
    fun read_writtenSettings_isTheSameSettings() {
        val settings = RemoteSettings(model = RemoteModel.CUSTOM, customKeys = custom)
        val preferences = mutablePreferencesOf()

        RemoteSettingsCodec.write(preferences, settings)

        assertEquals(settings, RemoteSettingsCodec.read(preferences, logger))
        assertTrue(logger.warnings.isEmpty())
    }

    @Test
    fun write_defaults_storesNothing() {
        val preferences = mutablePreferencesOf()

        RemoteSettingsCodec.write(preferences, RemoteSettings())

        assertTrue(preferences.asMap().isEmpty())
    }

    @Test
    fun write_backToTheDefaults_removesEveryKey() {
        val preferences = mutablePreferencesOf()
        RemoteSettingsCodec.write(preferences, RemoteSettings(RemoteModel.CUSTOM, custom))

        RemoteSettingsCodec.write(preferences, RemoteSettings())

        assertTrue(preferences.asMap().isEmpty())
    }

    @Test
    fun write_actionWithoutKeys_storesAnEmptyList() {
        val preferences = mutablePreferencesOf()

        RemoteSettingsCodec.write(preferences, RemoteSettings(customKeys = custom))

        assertEquals("", preferences[customKey(RemoteAction.RESET_PARTIAL)])
    }

    @Test
    fun write_severalKeys_storesTheirCodesSorted() {
        val preferences = mutablePreferencesOf()

        RemoteSettingsCodec.write(preferences, RemoteSettings(customKeys = custom))

        assertEquals(
            "${KeyEvent.KEYCODE_PLUS},${KeyEvent.KEYCODE_PAGE_UP}",
            preferences[customKey(RemoteAction.INCREASE_PARTIAL)],
        )
    }

    @Test
    fun read_unknownModel_usesTheDefaultAndWarns() {
        val settings = RemoteSettingsCodec.read(preferencesOf(MODEL_KEY to "WIIMOTE"), logger)

        assertEquals(RemoteModel.DND2, settings.model)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun read_notANumber_usesThatActionsDefaultAndWarns() {
        val settings = RemoteSettingsCodec.read(
            preferencesOf(
                customKey(RemoteAction.RESET_PARTIAL) to "66,F6",
                customKey(RemoteAction.NEXT_WAYPOINT) to "${KeyEvent.KEYCODE_ENTER}",
            ),
            logger,
        )

        assertEquals(
            RemoteKeys.DND2.keysFor(RemoteAction.RESET_PARTIAL),
            settings.customKeys.keysFor(RemoteAction.RESET_PARTIAL),
        )
        assertEquals(
            setOf(RemoteKey(KeyEvent.KEYCODE_ENTER)),
            settings.customKeys.keysFor(RemoteAction.NEXT_WAYPOINT),
        )
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun read_notPositive_usesThatActionsDefaultAndWarns() {
        val settings = RemoteSettingsCodec.read(
            preferencesOf(customKey(RemoteAction.RESET_PARTIAL) to "0"),
            logger,
        )

        assertEquals(RemoteSettings(), settings)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun read_keyBoundTwice_usesEveryDefaultAndWarns() {
        // F6 also stays on reset partial, its default.
        val settings = RemoteSettingsCodec.read(
            preferencesOf(
                MODEL_KEY to "CUSTOM",
                customKey(RemoteAction.NEXT_WAYPOINT) to "${KeyEvent.KEYCODE_F6}",
                customKey(RemoteAction.DECREASE_PARTIAL) to "${KeyEvent.KEYCODE_ENTER}",
            ),
            logger,
        )

        assertEquals(RemoteSettings(model = RemoteModel.CUSTOM), settings)
        assertEquals(1, logger.warnings.size)
    }
}
