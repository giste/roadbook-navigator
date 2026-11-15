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

package dev.giste.roadbook.data.rn2

import dev.giste.roadbook.domain.drawing.IconType
import org.junit.Assert.assertEquals
import org.junit.Test

class Rn2IconTypesTest {

    @Test
    fun iconType_knownId_isTheSymbolOfThatId() {
        val icon = Rn2Icon(id = DANGER_3_ID, name = "Danger Level 3")

        assertEquals(IconType.DANGER_3, icon.iconType())
    }

    @Test
    fun iconType_knownIdWithOtherName_prefersTheId() {
        val icon = Rn2Icon(id = RESET_ID, name = "Church")

        assertEquals(IconType.RESET_PARTIAL, icon.iconType())
    }

    @Test
    fun iconType_unknownIdWithKnownName_isTheSymbolOfThatName() {
        val icon = Rn2Icon(id = "not-a-known-id", name = "Tree")

        assertEquals(IconType.TREE, icon.iconType())
    }

    @Test
    fun iconType_noIdWithKnownName_isTheSymbolOfThatName() {
        val icon = Rn2Icon(id = null, name = "River / Water")

        assertEquals(IconType.RIVER_WATER, icon.iconType())
    }

    @Test
    fun iconType_speedLimitName_isTheSpeedLimit() {
        // Speed limit ids are not known, so they are matched by name.
        val icon = Rn2Icon(id = "some-id", name = "Speed Limit 50")

        assertEquals(IconType.SPEED_LIMIT_50, icon.iconType())
    }

    @Test
    fun iconType_unknownIdAndName_isUnknown() {
        val icon = Rn2Icon(id = "not-a-known-id", name = "Camel")

        assertEquals(IconType.UNKNOWN, icon.iconType())
    }

    @Test
    fun iconType_noIdAndNoName_isUnknown() {
        assertEquals(IconType.UNKNOWN, Rn2Icon().iconType())
    }

    @Test
    fun iconType_everySymbolButUnknown_hasAName() {
        val named = IconType.entries
            .filter { it != IconType.UNKNOWN }
            .filter { type -> SYMBOL_NAMES.any { Rn2Icon(name = it).iconType() == type } }

        assertEquals(IconType.entries.size - 1, named.size)
    }

    private companion object {
        const val DANGER_3_ID = "fab72ac2-f809-4ddc-9a7a-c9a24768bb4e"
        const val RESET_ID = "308c7365-bc3f-451b-9e98-531e9015024f"

        /** Every symbol name Rally Navigator uses that the app knows. */
        val SYMBOL_NAMES = listOf(
            "Danger Level 1", "Danger Level 2", "Danger Level 3", "Fuel Zone",
            "Reset to Distance to Zero", "Above Bridge", "Church", "Fort / Castle", "House",
            "Traffic Light", "Tree", "Tunnel", "Under Bridge", "Alert", "Do Not Enter",
            "Roundabout", "Stop", "River / Water",
        ) + (10..150 step 10).map { "Speed Limit $it" }
    }
}
