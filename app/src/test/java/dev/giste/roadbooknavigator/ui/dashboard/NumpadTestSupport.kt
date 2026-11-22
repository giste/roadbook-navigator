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

package dev.giste.roadbooknavigator.ui.dashboard

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick

/**
 * Presses the set-partial numpad keys for [keys]: digits press their key, `.` or `,` press the
 * separator key (labeled with the locale's separator, which [separator] gives), and `<` presses
 * delete.
 */
internal fun SemanticsNodeInteractionsProvider.pressNumpadKeys(keys: String, separator: Char = '.') {
    keys.forEach { key ->
        when (key) {
            '<' -> onNodeWithTag(SetPartialTestTags.DELETE).performClick()
            '.', ',' -> onNode(numpadKey("$separator")).performClick()
            else -> onNode(numpadKey("$key")).performClick()
        }
    }
}

/** Matches the numpad key labeled [label]: the clickable node with that text. */
internal fun numpadKey(label: String): SemanticsMatcher = hasText(label) and hasClickAction()

/** Matches [text] shown in the typed-partial display, e.g. `"12.35 km"`. */
internal fun shownInPartialDisplay(text: String): SemanticsMatcher =
    hasText(text) and hasAnyAncestor(hasTestTag(SetPartialTestTags.DISPLAY))
