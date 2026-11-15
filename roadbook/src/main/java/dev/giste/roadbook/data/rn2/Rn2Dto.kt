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

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// The DTOs of a Rally Navigator .rn2 file, which is JSON. They mirror the parts of the file the
// roadbook uses, and nothing else: unknown keys are ignored. Fields the roadbook can do without
// have defaults, so files that leave them out still load.

/** The whole file: a single route. */
@Serializable
internal data class Rn2File(val route: Rn2Route)

/** A route: its metadata and all its points, shown or not, in route order. */
@Serializable
internal data class Rn2Route(
    val name: String = "",
    val description: String = "",
    @SerialName("startlocation") val startLocation: String = "",
    @SerialName("endlocation") val endLocation: String = "",
    val waypoints: List<Rn2Waypoint> = emptyList(),
)

/**
 * A point of the route. In the file every point is a "waypoint", but only those with [show] set
 * are waypoints of the roadbook.
 *
 * @property ele the altitude in meters, or `null` when the file does not give it.
 */
@Serializable
internal data class Rn2Waypoint(
    val lat: Double,
    val lon: Double,
    val ele: Double? = null,
    val show: Boolean,
    val tulip: Rn2Elements = Rn2Elements(),
    val notes: Rn2Elements = Rn2Elements(),
)

/** The elements drawn in a tulip or notes column. */
@Serializable
internal data class Rn2Elements(val elements: List<Rn2Element> = emptyList())

/** An element drawn in a column. The file tells them apart with a `type` field. */
@Serializable(with = Rn2ElementSerializer::class)
internal sealed interface Rn2Element

/**
 * A road. Every field is optional: the mapper fills in what a track needs.
 *
 * @property typeId Rally Navigator's id of the kind of road, or `null` to take the kind from the
 * context.
 */
@Serializable
internal data class Rn2Road(
    val start: Rn2Point? = null,
    val end: Rn2Point? = null,
    val typeId: Int? = null,
    val handles: List<Rn2Point> = emptyList(),
) : Rn2Element

/** The route through a tulip's junction: the road in and the road out. */
@Serializable
internal data class Rn2Track(
    val roadIn: Rn2Road = Rn2Road(),
    val roadOut: Rn2Road = Rn2Road(),
) : Rn2Element

/** A text. Sizes left out get the mapper's defaults. */
@Serializable
internal data class Rn2Text(
    val text: String = "",
    val fontSize: Int? = null,
    val lineHeight: Double? = null,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val maxWidth: Double? = null,
    val maxHeight: Double? = null,
    val x: Double = 0.0,
    val y: Double = 0.0,
) : Rn2Element

/**
 * A symbol, identified by [id] or, for symbols whose id is not known, by [name].
 *
 * Its size has two forms: a scaled one ([width], [height], [scaleX], [scaleY]) and, in files
 * without a scale, a square of side [w].
 */
@Serializable
internal data class Rn2Icon(
    val id: String? = null,
    val name: String? = null,
    val angle: Double? = null,
    val w: Double = DEFAULT_ICON_SIZE,
    val width: Double? = null,
    val height: Double? = null,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val scaleX: Double? = null,
    val scaleY: Double? = null,
) : Rn2Element

/**
 * An element of a type the app does not know, e.g. one added by a newer Rally Navigator. It is
 * skipped, so the rest of the roadbook still loads.
 */
@Serializable
internal data object Rn2UnknownElement : Rn2Element

/** A point on a column's canvas, relative to its center. */
@Serializable
internal data class Rn2Point(val x: Double, val y: Double)

/** The size of a symbol, in canvas units, when the file does not give it: 50. */
internal const val DEFAULT_ICON_SIZE: Double = 50.0

/**
 * Picks the DTO of an element from its `type` field. An element without a type, or with a type
 * the app does not know, becomes [Rn2UnknownElement].
 */
internal object Rn2ElementSerializer :
    JsonContentPolymorphicSerializer<Rn2Element>(Rn2Element::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Rn2Element> =
        when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "Road" -> Rn2Road.serializer()
            "Track" -> Rn2Track.serializer()
            "Text" -> Rn2Text.serializer()
            "Icon" -> Rn2Icon.serializer()
            else -> Rn2UnknownElement.serializer()
        }
}
