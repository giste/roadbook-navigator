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

/**
 * Rally Navigator's ids of the symbols the app knows. Speed limits are not here: their ids are not
 * known, so they are matched by name only.
 */
private val ICON_TYPES_BY_ID: Map<String, IconType> = mapOf(
    // Cross
    "bffeadbd-116b-49a7-921e-20dff8deec4b" to IconType.DANGER_1,
    "a6c80c12-49b1-4e68-a21f-a6d48ef0a0ed" to IconType.DANGER_2,
    "fab72ac2-f809-4ddc-9a7a-c9a24768bb4e" to IconType.DANGER_3,
    "e5167bd4-314b-47d3-ba23-708182be76a9" to IconType.FUEL_ZONE,
    "308c7365-bc3f-451b-9e98-531e9015024f" to IconType.RESET_PARTIAL,
    // Landmarks
    "a49a0b2e-3be5-4659-8251-8205fd4e9571" to IconType.ABOVE_BRIDGE,
    "2b631045-4022-44d0-b966-c27c992fd7a2" to IconType.CHURCH,
    "da5ec2a7-612a-411f-aeb2-d1f9514d3dc7" to IconType.FORT_CASTLE,
    "3965bf45-97ee-4c6b-b087-0e128510c4e3" to IconType.HOUSE,
    "1d752896-09fd-498d-b416-21f31a356be5" to IconType.TRAFFIC_LIGHT,
    "75b46651-d46d-4655-b4ab-6a0dcff4fb38" to IconType.TREE,
    "0539c8e3-393b-4416-8002-b30700cf68de" to IconType.TUNNEL,
    "79f8c10f-d67b-4ba5-bf12-6a801ed79ed3" to IconType.UNDER_BRIDGE,
    // Signs
    "2598a2c0-6a8b-4dc5-8211-8ad64d986bde" to IconType.ALERT,
    "2a0a575d-b1b7-4307-864f-e4a9df1618eb" to IconType.DO_NOT_ENTER,
    "5d157992-6013-4bef-86cb-92fea891944c" to IconType.ROUNDABOUT,
    "5a4ced4c-68e2-41d3-a1b4-9c8b86ec2109" to IconType.STOP,
    // Terrain
    "aabe9acd-ab1b-467d-9bbb-877bb0d0da23" to IconType.RIVER_WATER,
)

/** Rally Navigator's names of the symbols the app knows, used when the id is not known. */
private val ICON_TYPES_BY_NAME: Map<String, IconType> = mapOf(
    // Cross
    "Danger Level 1" to IconType.DANGER_1,
    "Danger Level 2" to IconType.DANGER_2,
    "Danger Level 3" to IconType.DANGER_3,
    "Fuel Zone" to IconType.FUEL_ZONE,
    "Reset to Distance to Zero" to IconType.RESET_PARTIAL,
    // Landmarks
    "Above Bridge" to IconType.ABOVE_BRIDGE,
    "Church" to IconType.CHURCH,
    "Fort / Castle" to IconType.FORT_CASTLE,
    "House" to IconType.HOUSE,
    "Traffic Light" to IconType.TRAFFIC_LIGHT,
    "Tree" to IconType.TREE,
    "Tunnel" to IconType.TUNNEL,
    "Under Bridge" to IconType.UNDER_BRIDGE,
    // Signs
    "Alert" to IconType.ALERT,
    "Do Not Enter" to IconType.DO_NOT_ENTER,
    "Roundabout" to IconType.ROUNDABOUT,
    "Stop" to IconType.STOP,
    // Speed limits
    "Speed Limit 10" to IconType.SPEED_LIMIT_10,
    "Speed Limit 20" to IconType.SPEED_LIMIT_20,
    "Speed Limit 30" to IconType.SPEED_LIMIT_30,
    "Speed Limit 40" to IconType.SPEED_LIMIT_40,
    "Speed Limit 50" to IconType.SPEED_LIMIT_50,
    "Speed Limit 60" to IconType.SPEED_LIMIT_60,
    "Speed Limit 70" to IconType.SPEED_LIMIT_70,
    "Speed Limit 80" to IconType.SPEED_LIMIT_80,
    "Speed Limit 90" to IconType.SPEED_LIMIT_90,
    "Speed Limit 100" to IconType.SPEED_LIMIT_100,
    "Speed Limit 110" to IconType.SPEED_LIMIT_110,
    "Speed Limit 120" to IconType.SPEED_LIMIT_120,
    "Speed Limit 130" to IconType.SPEED_LIMIT_130,
    "Speed Limit 140" to IconType.SPEED_LIMIT_140,
    "Speed Limit 150" to IconType.SPEED_LIMIT_150,
    // Terrain
    "River / Water" to IconType.RIVER_WATER,
)

/**
 * Which symbol this is: the one with this [id][Rn2Icon.id], or else the one with this
 * [name][Rn2Icon.name], or else [IconType.UNKNOWN].
 *
 * The id comes first, as in the previous app: it identifies the symbol even if its name
 * changes.
 */
internal fun Rn2Icon.iconType(): IconType =
    id?.let(ICON_TYPES_BY_ID::get) ?: name?.let(ICON_TYPES_BY_NAME::get) ?: IconType.UNKNOWN
