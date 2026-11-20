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

package dev.giste.core.ui.component

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

/** Font feature that gives every digit the same width, so numbers do not jump as they change. */
private const val TABULAR_FIGURES: String = "tnum"

/** The smallest the text may shrink to fit its box. */
private val MIN_FONT_SIZE = 8.sp

/** The largest the text may grow to fill its box. */
private val MAX_FONT_SIZE = 100.sp

/**
 * A single line of text that grows or shrinks to fill its box, with digits of equal width.
 *
 * Use it for values that must be as large as their box allows, so they can be read at a glance,
 * such as distances or counters on a screen used while driving. The font size is the largest step
 * between 8 sp and 100 sp at which the text fits the box on one line, and it is recomputed
 * whenever the text or the box changes. Text that does not fit even at 8 sp is clipped, never
 * wrapped.
 *
 * Digits use tabular figures (the `tnum` font feature), so a changing number keeps its width and
 * does not jump.
 *
 * The box comes from [modifier]: give the text a bounded size (e.g. `fillMaxSize()` or `weight()`
 * in a bounded parent). With unbounded constraints the text takes the largest size.
 *
 * @param text what to write.
 * @param color the text color.
 * @param modifier the modifier applied to the text, which gives it its box.
 * @param style the base style. Its font size is replaced by the one that fits, and its line height
 * by the font's own, which scales with the size.
 * @param fontWeight the weight, e.g. bold to highlight a value. `null` keeps the one in [style].
 * @param textAlign how the text is aligned in its box.
 */
@Composable
public fun AutoSizeText(
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
