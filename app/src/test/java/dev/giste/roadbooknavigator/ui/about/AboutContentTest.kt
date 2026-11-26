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

package dev.giste.roadbooknavigator.ui.about

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.roadbooknavigator.domain.LibraryLicence
import dev.giste.roadbooknavigator.domain.OpenSourceLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-w411dp-h891dp")
class AboutContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    /** Records the links the screen opens, or fails as Compose does when no app can open them. */
    private inner class FakeUriHandler(private val fails: Boolean = false) : UriHandler {
        override fun openUri(uri: String) {
            if (fails) throw IllegalArgumentException("Can't open $uri.")
            calls += "open $uri"
        }
    }

    private val apache = LibraryLicence(
        name = "Apache License 2.0",
        url = "https://spdx.org/licenses/Apache-2.0.html",
        text = "Apache License text",
    )

    private val okHttp = OpenSourceLibrary("com.squareup.okhttp3:okhttp", "OkHttp", "5.5.1", listOf(apache))

    private val unversioned = OpenSourceLibrary(
        id = "com.example:unversioned",
        name = "Unversioned",
        version = null,
        licences = listOf(apache, LibraryLicence("MIT License", url = "https://mit.example", text = null)),
    )

    private val loaded = AboutUiState.Loaded(listOf(okHttp, unversioned))

    private fun show(state: AboutUiState, uriHandler: UriHandler = FakeUriHandler()) {
        composeRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                GisteTheme {
                    AboutContent(appVersion = "1.0", state = state, onBack = { calls += "back" })
                }
            }
        }
    }

    /** Scrolls the screen's list until the node matching [matcher] is shown, and returns it. */
    private fun scrollTo(matcher: SemanticsMatcher) = composeRule.apply {
        onNode(hasScrollAction() and !isDialog()).performScrollToNode(matcher)
    }.onNode(matcher)

    /** The tappable link or row labeled [text] on the screen, scrolled into view. */
    private fun clickable(text: String) = scrollTo(hasText(text) and hasClickAction())

    /** The node with [text] inside the licences dialog. */
    private fun inDialog(text: String) = composeRule.onNode(hasText(text) and hasAnyAncestor(isDialog()))

    @Test
    fun aboutContent_showsTheAppItsVersionAndLicence() {
        show(loaded)

        composeRule.onNodeWithText("About").assertExists()
        composeRule.onNodeWithText("Roadbook Navigator").assertExists()
        composeRule.onNodeWithText("Version 1.0").assertExists()
        composeRule.onNodeWithText(
            "Roadbook Navigator is free software, under the GNU General Public License, version 3 or later.",
        ).assertExists()
    }

    @Test
    fun aboutContent_showsTheMapCredits() {
        show(loaded)

        scrollTo(hasText("Map data")).assertExists()
        scrollTo(hasText("© OpenStreetMap contributors")).assertExists()
        scrollTo(hasText("The map data is available under the Open Database License.")).assertExists()
        scrollTo(hasText("Map renderer")).assertExists()
        scrollTo(
            hasText(
                "The map is drawn with VTM, from the mapsforge project, under the GNU Lesser General " +
                    "Public License, version 3 or later.",
            ),
        ).assertExists()
    }

    @Test
    fun aboutContent_appLicenceLink_opensTheGpl() {
        show(loaded)

        clickable("Read the licence").performClick()

        assertEquals(listOf("open $GPL_URL"), calls)
    }

    @Test
    fun aboutContent_copyrightLink_opensOpenStreetMapsCopyrightPage() {
        show(loaded)

        clickable("Copyright and licence").performClick()

        assertEquals(listOf("open $OSM_COPYRIGHT_URL"), calls)
    }

    @Test
    fun aboutContent_vtmLink_opensItsWebsite() {
        show(loaded)

        clickable("VTM website").performClick()

        assertEquals(listOf("open $VTM_URL"), calls)
    }

    @Test
    fun aboutContent_noAppForALink_saysSo() {
        show(loaded, uriHandler = FakeUriHandler(fails = true))

        clickable("VTM website").performClick()

        composeRule.onNodeWithText("No app can open this link.").assertExists()
    }

    @Test
    fun aboutContent_backPressed_reportsIt() {
        show(loaded)

        composeRule.onNodeWithTag(AboutTestTags.BACK).performClick()

        assertEquals(listOf("back"), calls)
    }

    @Test
    fun aboutContent_loading_showsTheIndicatorAndTheHint() {
        show(AboutUiState.Loading)

        scrollTo(hasText("Tap a library to read its licence.")).assertExists()
        composeRule.onNodeWithTag(AboutTestTags.LIBRARIES_LOADING).assertExists()
    }

    @Test
    fun aboutContent_unavailable_saysSoAndKeepsTheCredits() {
        show(AboutUiState.LibrariesUnavailable)

        scrollTo(hasText("The list of libraries couldn't be read.")).assertExists()
        composeRule.onNodeWithTag(AboutTestTags.LIBRARIES_LOADING).assertDoesNotExist()
        scrollTo(hasText("© OpenStreetMap contributors")).assertExists()
    }

    @Test
    fun aboutContent_loaded_listsEachLibraryWithVersionAndLicences() {
        show(loaded)

        clickable("OkHttp").assertExists()
        scrollTo(hasText("5.5.1 · Apache License 2.0")).assertExists()
        clickable("Unversioned").assertExists()
        scrollTo(hasText("Apache License 2.0, MIT License")).assertExists()
        composeRule.onNodeWithTag(AboutTestTags.LIBRARIES_LOADING).assertDoesNotExist()
    }

    @Test
    fun aboutContent_libraryTapped_showsItsLicenceText() {
        show(loaded)

        clickable("OkHttp").performClick()

        inDialog("OkHttp").assertExists()
        inDialog("Apache License 2.0").assertExists()
        inDialog("Apache License text").assertExists()
    }

    @Test
    fun aboutContent_licenceWithoutText_linksToIt() {
        show(loaded)
        clickable("Unversioned").performClick()

        composeRule.onNode(hasText("Read the licence") and hasAnyAncestor(isDialog())).performClick()

        assertEquals(listOf("open https://mit.example"), calls)
    }

    @Test
    fun aboutContent_closePressed_closesTheLicences() {
        show(loaded)
        clickable("OkHttp").performClick()

        composeRule.onNodeWithTag(AboutTestTags.CLOSE_LICENCES).performClick()

        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun aboutContent_licencesOpen_surviveRecreation() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            GisteTheme {
                AboutContent(appVersion = "1.0", state = loaded, onBack = {})
            }
        }
        clickable("OkHttp").performClick()

        restoration.emulateSavedInstanceStateRestore()

        inDialog("Apache License text").assertExists()
    }

    @Test
    fun aboutContent_controls_areAtLeastTheMinimumTouchTarget() {
        show(loaded)

        listOf("Read the licence", "Copyright and licence", "VTM website", "OkHttp").forEach { label ->
            val height = clickable(label).getUnclippedBoundsInRoot().height
            assertTrue("$label is $height high", height >= 56.dp)
        }
        val back = composeRule.onNodeWithTag(AboutTestTags.BACK).getUnclippedBoundsInRoot()
        assertTrue("Back is ${back.height} high", back.height >= 56.dp)
        clickable("OkHttp").performClick()
        val close = composeRule.onNodeWithTag(AboutTestTags.CLOSE_LICENCES).getUnclippedBoundsInRoot()
        assertTrue("Close is ${close.height} high", close.height >= 56.dp)
    }

    @Test
    @Config(qualifiers = "es-w411dp-h891dp")
    fun aboutContent_spanish_usesSpanishTextsAndTheFixedAttribution() {
        show(AboutUiState.LibrariesUnavailable)

        composeRule.onNodeWithText("Acerca de").assertExists()
        composeRule.onNodeWithText("Versión 1.0").assertExists()
        clickable("Leer la licencia").assertExists()
        scrollTo(hasText("Datos del mapa")).assertExists()
        // The attribution's wording is fixed by OpenStreetMap, so it is not translated.
        scrollTo(hasText("© OpenStreetMap contributors")).assertExists()
        clickable("Derechos de autor y licencia").assertExists()
        scrollTo(hasText("Motor del mapa")).assertExists()
        clickable("Web de VTM").assertExists()
        scrollTo(hasText("Bibliotecas de código abierto")).assertExists()
        scrollTo(hasText("No se ha podido leer la lista de bibliotecas.")).assertExists()
    }
}
