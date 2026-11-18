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

package dev.giste.roadbook.presentation.waypoint

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Font feature that gives every digit the same width, so distances do not jump as they change. */
private const val TABULAR_FIGURES: String = "tnum"

/** The smallest a distance may shrink to fit its box. */
private val MIN_FONT_SIZE = 8.sp

/** The largest a distance may grow to fill its box. */
private val MAX_FONT_SIZE = 100.sp

/**
 * A single line of text that grows or shrinks to fill its box, with digits of equal width. The
 * roadbook uses it for distances and waypoint numbers, which must be as large as the row allows
 * to be read while riding.
 *
 * @param text what to write.
 * @param color the text color.
 * @param modifier the modifier applied to the text, which gives it its box.
 * @param style the base style. Its size is replaced by the one that fits, and its line height by
 * the font's own, which scales with the size.
 * @param fontWeight the weight, e.g. bold for the partial.
 * @param textAlign how the text is aligned in its box.
 */
@Composable
internal fun AutoSizeText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(
        text = text,
        modifier = modifier,
        // A fixed line height, as the Material styles have, would be taller than small boxes at any
        // font size, so auto-size would always fall back to the smallest one.
        style = style.copy(lineHeight = TextUnit.Unspecified).merge(
            color = color,
            fontWeight = fontWeight,
            textAlign = textAlign,
            fontFeatureSettings = TABULAR_FIGURES,
        ),
        autoSize = TextAutoSize.StepBased(minFontSize = MIN_FONT_SIZE, maxFontSize = MAX_FONT_SIZE),
        maxLines = 1,
        softWrap = false,
    )
}
