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

package dev.giste.roadbook.domain.drawing

/**
 * A text written in a tulip or notes column, e.g. a direction or a place name.
 *
 * Sizes are in canvas units. The text is drawn in a box of [width] by [height], which may grow up
 * to [maxWidth] by [maxHeight].
 *
 * @property text what is written. May be empty.
 * @property fontSize the font size. Must be positive.
 * @property lineHeight the height of each line, as a multiple of [fontSize]. Must be finite and
 * positive.
 * @property width the width of the text box. Must be finite and not negative.
 * @property height the height of the text box. Must be finite and not negative.
 * @property maxWidth the widest the text box may grow. Must be finite and not negative.
 * @property maxHeight the tallest the text box may grow. Must be finite and not negative.
 * @property center where the center of the text box is drawn.
 * @throws IllegalArgumentException if a size is out of range.
 */
internal data class Text(
    val text: String,
    val fontSize: Int = DEFAULT_FONT_SIZE,
    val lineHeight: Double,
    val width: Double,
    val height: Double,
    val maxWidth: Double,
    val maxHeight: Double,
    val center: CanvasPoint,
) : Element {

    init {
        require(fontSize > 0) { "A text's font size must be positive, but was $fontSize" }
        require(lineHeight.isFinite() && lineHeight > 0.0) {
            "A text's line height must be finite and positive, but was $lineHeight"
        }
        listOf("width" to width, "height" to height, "maxWidth" to maxWidth, "maxHeight" to maxHeight)
            .forEach { (name, size) ->
                require(size.isFinite() && size >= 0.0) {
                    "A text's $name must be finite and not negative, but was $size"
                }
            }
    }

    /** Defaults of [Text]. */
    companion object {

        /** The font size used when the file gives none: 18. */
        const val DEFAULT_FONT_SIZE: Int = 18
    }
}
