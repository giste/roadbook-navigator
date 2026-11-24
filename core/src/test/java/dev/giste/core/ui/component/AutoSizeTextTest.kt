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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

// Native graphics measure text with real font metrics. The default mode returns the same size
// for any font size, so auto-size could not be observed.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class AutoSizeTextTest {

    @get:Rule
    val rule = createComposeRule()

    /** Measures reference text at known font sizes, with the density of the test screen. */
    private lateinit var measurer: TextMeasurer

    /** Shows [text] in a box of [width] × [height], with the given style and sizing arguments. */
    private fun show(
        text: String,
        width: Dp = 200.dp,
        height: Dp = 40.dp,
        style: TextStyle = TextStyle.Default,
        fontWeight: FontWeight? = null,
        textAlign: TextAlign? = null,
        widestText: String? = null,
    ) {
        rule.setContent {
            measurer = rememberTextMeasurer()
            if (textAlign == null) {
                AutoSizeText(
                    text = text,
                    color = Color.Red,
                    modifier = Modifier.size(width, height),
                    style = style,
                    fontWeight = fontWeight,
                    widestText = widestText,
                )
            } else {
                AutoSizeText(
                    text = text,
                    color = Color.Red,
                    modifier = Modifier.size(width, height),
                    style = style,
                    fontWeight = fontWeight,
                    textAlign = textAlign,
                    widestText = widestText,
                )
            }
        }
    }

    /** The layout the text was drawn with, which holds the font size auto-size chose. */
    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        return results.single()
    }

    private fun layoutOf(text: String): TextLayoutResult = rule.onNodeWithText(text).textLayout()

    /** The height of the first line of [layout], which grows with the font size. */
    private fun lineHeight(layout: TextLayoutResult): Float = layout.getLineBottom(0) - layout.getLineTop(0)

    /**
     * The line height of [text] at [fontSize]. Auto-size does not report the size it picks, so
     * tests compare line heights instead.
     */
    private fun lineHeightAt(text: String, fontSize: TextUnit): Float =
        lineHeight(measurer.measure(text, TextStyle(fontSize = fontSize, fontFeatureSettings = "tnum")))

    /** The line height of the shown [text]. */
    private fun shownLineHeight(text: String): Float = lineHeight(layoutOf(text))

    @Test
    fun autoSizeText_anyBox_showsText() {
        show("12,34")

        rule.onNodeWithText("12,34").assertIsDisplayed()
    }

    @Test
    fun autoSizeText_largeBox_growsToMaxFontSize() {
        show("1", width = 1_000.dp, height = 1_000.dp)

        assertEquals(lineHeightAt("1", 100.sp), shownLineHeight("1"))
    }

    @Test
    fun autoSizeText_tinyBox_shrinksToMinFontSize() {
        show("123,45", width = 4.dp, height = 2.dp)

        assertEquals(lineHeightAt("123,45", 8.sp), shownLineHeight("123,45"))
    }

    @Test
    fun autoSizeText_boundedBox_fitsTextBetweenLimits() {
        show("123,45", width = 120.dp, height = 40.dp)

        val shown = shownLineHeight("123,45")
        assertTrue("$shown px is not above the minimum size", shown > lineHeightAt("123,45", 8.sp))
        assertTrue("$shown px is not below the maximum size", shown < lineHeightAt("123,45", 100.sp))
        assertTrue("text overflows its box", !layoutOf("123,45").hasVisualOverflow)
    }

    @Test
    fun autoSizeText_textGrows_shrinksFontSize() {
        var text by mutableStateOf("1")
        rule.setContent {
            measurer = rememberTextMeasurer()
            AutoSizeText(text = text, color = Color.Red, modifier = Modifier.size(120.dp, 40.dp))
        }
        val shortTextSize = shownLineHeight("1")

        text = "1234,56"
        rule.waitForIdle()

        val longTextSize = shownLineHeight("1234,56")
        assertTrue("$longTextSize px is not smaller than $shortTextSize px", longTextSize < shortTextSize)
    }

    @Test
    fun autoSizeText_textTooLongForMinFontSize_staysOnOneLine() {
        show("1234567890 1234567890", width = 20.dp, height = 40.dp)

        assertEquals(1, layoutOf("1234567890 1234567890").lineCount)
    }

    @Test
    fun autoSizeText_anyText_usesColorAndTabularFigures() {
        show("12,34")

        val style = layoutOf("12,34").layoutInput.style
        assertEquals(Color.Red, style.color)
        assertEquals("tnum", style.fontFeatureSettings)
    }

    @Test
    fun autoSizeText_defaultAlignment_isCentered() {
        show("12,34")

        assertEquals(TextAlign.Center, layoutOf("12,34").layoutInput.style.textAlign)
    }

    @Test
    fun autoSizeText_givenAlignment_usesIt() {
        show("12,34", textAlign = TextAlign.End)

        assertEquals(TextAlign.End, layoutOf("12,34").layoutInput.style.textAlign)
    }

    @Test
    fun autoSizeText_givenFontWeight_overridesStyleWeight() {
        show("12,34", style = TextStyle(fontWeight = FontWeight.Light), fontWeight = FontWeight.Bold)

        assertEquals(FontWeight.Bold, layoutOf("12,34").layoutInput.style.fontWeight)
    }

    @Test
    fun autoSizeText_nullFontWeight_keepsStyleWeight() {
        show("12,34", style = TextStyle(fontWeight = FontWeight.Light))

        assertEquals(FontWeight.Light, layoutOf("12,34").layoutInput.style.fontWeight)
    }

    @Test
    fun autoSizeText_styleWithFixedLineHeight_clearsLineHeight() {
        show("12,34", style = TextStyle(lineHeight = 44.sp))

        assertEquals(TextUnit.Unspecified, layoutOf("12,34").layoutInput.style.lineHeight)
    }

    @Test
    fun autoSizeText_fixedLineHeightTallerThanBox_stillGrowsAboveMinFontSize() {
        // A 44 sp line height alone is taller than this box, so keeping it would force 8 sp.
        show("12,34", width = 200.dp, height = 30.dp, style = TextStyle(lineHeight = 44.sp))

        assertTrue(shownLineHeight("12,34") > lineHeightAt("12,34", 8.sp))
    }

    // In a box this narrow, "999,99" is limited by the width and "1" only by the height.
    private val WIDTH_LIMITED_BOX_WIDTH = 60.dp

    @Test
    fun autoSizeText_widestTextGiven_usesTheWidestTextSize() {
        rule.setContent {
            Column {
                AutoSizeText(text = "999,99", color = Color.Red, modifier = Modifier.size(WIDTH_LIMITED_BOX_WIDTH, 40.dp))
                AutoSizeText(
                    text = "1",
                    color = Color.Red,
                    modifier = Modifier.size(WIDTH_LIMITED_BOX_WIDTH, 40.dp),
                    widestText = "999,99",
                )
            }
        }

        assertEquals(shownLineHeight("999,99"), shownLineHeight("1"))
    }

    @Test
    fun autoSizeText_widestTextGiven_isSmallerThanTheTextAlone() {
        rule.setContent {
            measurer = rememberTextMeasurer()
            Column {
                AutoSizeText(text = "1", color = Color.Red, modifier = Modifier.size(WIDTH_LIMITED_BOX_WIDTH, 40.dp))
                AutoSizeText(
                    text = "2",
                    color = Color.Red,
                    modifier = Modifier.size(WIDTH_LIMITED_BOX_WIDTH, 40.dp),
                    widestText = "999,99",
                )
            }
        }

        val alone = shownLineHeight("1")
        val sizedForWidest = shownLineHeight("2")
        assertTrue("$sizedForWidest px is not smaller than $alone px", sizedForWidest < alone)
    }

    @Test
    fun autoSizeText_widestTextGivenAndTextGrows_keepsFontSize() {
        var text by mutableStateOf("1")
        rule.setContent {
            AutoSizeText(
                text = text,
                color = Color.Red,
                modifier = Modifier.size(WIDTH_LIMITED_BOX_WIDTH, 40.dp),
                widestText = "999,99",
            )
        }
        val shortTextSize = shownLineHeight("1")

        text = "999,99"
        rule.waitForIdle()

        assertEquals(shortTextSize, shownLineHeight("999,99"))
    }

    @Test
    fun autoSizeText_textWiderThanWidestText_staysOnOneLine() {
        show("1234567890", width = 120.dp, height = 40.dp, widestText = "1")

        assertEquals(1, layoutOf("1234567890").lineCount)
    }

    /**
     * Shows "1" sized for "999,99" in a [WIDTH_LIMITED_BOX_WIDTH] × [TALL_BOX_HEIGHT] box, so the
     * line is shorter than the box, with [modifier] on the text.
     */
    private fun showInTallBox(modifier: Modifier, widestText: String?) {
        rule.setContent {
            Box(modifier = Modifier.size(WIDTH_LIMITED_BOX_WIDTH, TALL_BOX_HEIGHT)) {
                AutoSizeText(text = "1", color = Color.Red, modifier = modifier, widestText = widestText)
            }
        }
    }

    /** Asserts the shown "1" is as far from the top of its tall box as from the bottom. */
    private fun assertCenteredInTallBox() {
        val bounds = rule.onNodeWithText("1").getUnclippedBoundsInRoot()
        val below = TALL_BOX_HEIGHT - bounds.bottom
        assertTrue("${bounds.top} above, $below below", (bounds.top - below).value in -1f..1f)
        assertTrue("${bounds.top} above", bounds.top > 0.dp)
    }

    @Test
    fun autoSizeText_widestTextGivenAndWrapContentHeight_isCenteredVertically() {
        showInTallBox(Modifier.fillMaxSize().wrapContentHeight(), widestText = "999,99")

        assertCenteredInTallBox()
    }

    @Test
    fun autoSizeText_wrapContentHeight_isCenteredVertically() {
        // Without widestText the text is sized for itself, so give it a box limited by the width.
        rule.setContent {
            Box(modifier = Modifier.size(WIDTH_LIMITED_BOX_WIDTH, TALL_BOX_HEIGHT)) {
                AutoSizeText(
                    text = "999,99",
                    color = Color.Red,
                    modifier = Modifier.fillMaxSize().wrapContentHeight(),
                )
            }
        }

        val bounds = rule.onNodeWithText("999,99").getUnclippedBoundsInRoot()
        val below = TALL_BOX_HEIGHT - bounds.bottom
        assertTrue("${bounds.top} above, $below below", (bounds.top - below).value in -1f..1f)
        assertTrue("${bounds.top} above", bounds.top > 0.dp)
    }

    @Test
    fun autoSizeText_widestTextGivenAndFillMaxSize_staysAtTheTop() {
        showInTallBox(Modifier.fillMaxSize(), widestText = "999,99")

        assertEquals(0.dp, rule.onNodeWithText("1").getUnclippedBoundsInRoot().top)
    }

    @Test
    fun autoSizeText_widestTextGivenAndWrapContentHeight_keepsTheWidestTextSize() {
        rule.setContent {
            Column {
                AutoSizeText(text = "999,99", color = Color.Red, modifier = Modifier.size(WIDTH_LIMITED_BOX_WIDTH, 40.dp))
                Box(modifier = Modifier.size(WIDTH_LIMITED_BOX_WIDTH, 40.dp)) {
                    AutoSizeText(
                        text = "1",
                        color = Color.Red,
                        modifier = Modifier.fillMaxSize().wrapContentHeight(),
                        widestText = "999,99",
                    )
                }
            }
        }

        assertEquals(shownLineHeight("999,99"), shownLineHeight("1"))
    }

    // Much taller than the line "999,99" fits in at WIDTH_LIMITED_BOX_WIDTH.
    private val TALL_BOX_HEIGHT = 200.dp
}
