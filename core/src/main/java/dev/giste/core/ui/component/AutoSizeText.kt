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

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Font feature that gives every digit the same width, so numbers do not jump as they change. */
private const val TABULAR_FIGURES: String = "tnum"

/** The smallest the text may shrink to fit its box, in sp. */
private const val MIN_FONT_SIZE_SP: Float = 8f

/** The largest the text may grow to fill its box, in sp. */
private const val MAX_FONT_SIZE_SP: Float = 100f

/** The difference between two font sizes tried, in sp, as in Compose's step-based auto-size. */
private const val FONT_SIZE_STEP_SP: Float = 0.25f

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
 * By default the font size depends on [text], so it changes when the text gets longer or shorter
 * (e.g. from `9.99` to `10.00`). Give [widestText] to keep it fixed: the size is then the largest
 * at which [widestText] fits, and every text that is not wider is drawn at that same size. For a
 * number, the widest text is its largest value (e.g. `999.99`), because tabular digits all have
 * the same width. A text wider than [widestText] is clipped.
 *
 * The box comes from [modifier]: give the text a bounded size (e.g. `fillMaxSize()` or `weight()`
 * in a bounded parent). With unbounded constraints the text takes the largest size.
 *
 * As with [Text], the line is drawn at the top of a box taller than it. To center it vertically,
 * let the text be as tall as its line within the box: `Modifier.fillMaxSize().wrapContentHeight()`.
 * The font size still fits the whole box, since `wrapContentHeight()` keeps its maximum height.
 *
 * @param text what to write.
 * @param color the text color.
 * @param modifier the modifier applied to the text, which gives it its box.
 * @param style the base style. Its font size is replaced by the one that fits, and its line height
 * by the font's own, which scales with the size.
 * @param fontWeight the weight, e.g. bold to highlight a value. `null` keeps the one in [style].
 * @param textAlign how the text is aligned in its box.
 * @param widestText the text the font size is computed for, instead of [text], or `null` to
 * compute it for [text] itself.
 */
@Composable
public fun AutoSizeText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Center,
    widestText: String? = null,
) {
    // A fixed line height, as the Material styles have, would be taller than small boxes at any
    // font size, so auto-size would always fall back to the smallest one.
    val textStyle = style.copy(lineHeight = TextUnit.Unspecified).merge(
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        fontFeatureSettings = TABULAR_FIGURES,
    )
    if (widestText == null) {
        Text(
            text = text,
            modifier = modifier,
            style = textStyle,
            autoSize = TextAutoSize.StepBased(
                minFontSize = MIN_FONT_SIZE_SP.sp,
                maxFontSize = MAX_FONT_SIZE_SP.sp,
                stepSize = FONT_SIZE_STEP_SP.sp,
            ),
            maxLines = 1,
            softWrap = false,
        )
    } else {
        // Compose's auto-size always measures the text it draws, so the size for widestText is
        // computed here, for the box the modifier gives, and the text is drawn at that fixed size.
        BoxWithConstraints(modifier = modifier) {
            val measurer = rememberTextMeasurer()
            // Text merges its style into LocalTextStyle, so measure with the same merged style.
            val measuredStyle = LocalTextStyle.current.merge(textStyle)
            val fontSize = remember(measurer, widestText, measuredStyle, constraints) {
                measurer.largestFittingFontSize(widestText, measuredStyle, constraints)
            }
            // Only as tall as its line, like Text: a box given a fixed height still draws the line
            // at the top, and wrapContentHeight() in the caller's modifier can center it.
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = textStyle.copy(fontSize = fontSize),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/**
 * The largest font size, between [MIN_FONT_SIZE_SP] and [MAX_FONT_SIZE_SP] in steps of
 * [FONT_SIZE_STEP_SP], at which [text] fits [constraints] on one line, or the smallest one if it
 * never fits.
 *
 * Fitting is monotonic (a text that fits at one size fits at every smaller one), so this is a
 * binary search over the step index: about 9 measurements for the 369 sizes. Unbounded
 * constraints never overflow, so they give the largest size.
 *
 * @param text the text to fit.
 * @param style the style the text is drawn with. Its font size is ignored.
 * @param constraints the box the text must fit.
 * @return the font size, in sp.
 */
private fun TextMeasurer.largestFittingFontSize(
    text: String,
    style: TextStyle,
    constraints: Constraints,
): TextUnit {
    val box = Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)
    fun sizeAt(step: Int): Float = MIN_FONT_SIZE_SP + step * FONT_SIZE_STEP_SP
    fun fitsAt(step: Int): Boolean = !measure(
        text = text,
        style = style.copy(fontSize = sizeAt(step).sp),
        overflow = TextOverflow.Clip,
        softWrap = false,
        maxLines = 1,
        constraints = box,
    ).hasVisualOverflow

    // Invariant: every step up to `low` fits (or `low` is 0), and no step above `high` fits.
    var low = 0
    var high = ((MAX_FONT_SIZE_SP - MIN_FONT_SIZE_SP) / FONT_SIZE_STEP_SP).toInt()
    while (low < high) {
        val middle = (low + high + 1) / 2
        if (fitsAt(middle)) low = middle else high = middle - 1
    }
    return sizeAt(low).sp
}
