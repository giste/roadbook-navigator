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

import android.content.ActivityNotFoundException
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.roadbooknavigator.BuildConfig
import dev.giste.roadbooknavigator.R
import dev.giste.roadbooknavigator.domain.LibraryLicence
import dev.giste.roadbooknavigator.domain.OpenSourceLibrary
import dev.giste.roadbooknavigator.ui.RiderTypography
import kotlinx.coroutines.launch

/** Where the app's own licence, the GNU GPL version 3, can be read. */
internal const val GPL_URL: String = "https://www.gnu.org/licenses/gpl-3.0.html"

/** OpenStreetMap's copyright and licence page, which its attribution guidelines ask to link. */
internal const val OSM_COPYRIGHT_URL: String = "https://www.openstreetmap.org/copyright"

/** The website of VTM, the library that draws the map. */
internal const val VTM_URL: String = "https://github.com/mapsforge/vtm"

/** Test tags of the About screen parts that tests cannot find by their own text. */
internal object AboutTestTags {

    /** The back button, which shows an icon instead of text. */
    const val BACK: String = "about_back"

    /** The indicator shown while the library list is read. */
    const val LIBRARIES_LOADING: String = "about_libraries_loading"

    /** The button that closes a library's licences, which shows an icon instead of text. */
    const val CLOSE_LICENCES: String = "about_close_licences"
}

/**
 * The About screen: the app, its version and licence, the credits of the map data and of the map
 * renderer, and every open-source library the app ships, each with its licences.
 *
 * @param onBack called when the back button is pressed.
 * @param modifier applied to the root layout.
 */
@Composable
internal fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: AboutViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AboutContent(
        appVersion = BuildConfig.VERSION_NAME,
        state = state,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * The About screen for [state], in one scrolling list:
 *
 * - **The app:** its name, [appVersion] and its licence, the GNU GPL version 3 or later.
 * - **Map data:** the OpenStreetMap attribution, its licence and a link to OpenStreetMap's
 *   copyright page, as its attribution guidelines ask.
 * - **Map renderer:** VTM, from the mapsforge project, and its licence.
 * - **Open-source libraries:** a spinner while loading, then every library with its version and
 *   licences, or a message if the list cannot be read. Tapping a library opens its licence texts
 *   in a dialog; which library is open survives configuration changes.
 *
 * Links open in the browser through [LocalUriHandler]. When no app can open them, a snackbar
 * says so.
 *
 * @param appVersion the app's version, e.g. "1.0".
 * @param state the library list.
 * @param onBack called when the back button is pressed.
 * @param modifier applied to the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutContent(
    appVersion: String,
    state: AboutUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = GisteTheme.dimensions
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val linkFailedMessage = stringResource(R.string.app_about_link_failed)
    val openLink: (String) -> Unit = { url ->
        // Compose reports a missing browser as IllegalArgumentException; older Android versions
        // let ActivityNotFoundException through. Either way the rider only needs to know.
        val opened = try {
            uriHandler.openUri(url)
            true
        } catch (_: IllegalArgumentException) {
            false
        } catch (_: ActivityNotFoundException) {
            false
        }
        if (!opened) scope.launch { snackbarHostState.showSnackbar(linkFailedMessage) }
    }
    // The id of the library whose licences are shown, if any.
    var openLibraryId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_about_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(dimensions.minTouchTarget)
                            .testTag(AboutTestTags.BACK),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.app_ic_arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(dimensions.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium),
        ) {
            item {
                AppSection(appVersion = appVersion, onOpenLink = openLink)
            }
            item {
                Credit(
                    title = stringResource(R.string.app_about_section_map_data),
                    lines = listOf(
                        stringResource(R.string.app_about_osm_attribution),
                        stringResource(R.string.app_about_osm_licence),
                    ),
                    linkLabel = stringResource(R.string.app_about_osm_copyright_link),
                    onOpenLink = { openLink(OSM_COPYRIGHT_URL) },
                )
            }
            item {
                Credit(
                    title = stringResource(R.string.app_about_section_map_renderer),
                    lines = listOf(stringResource(R.string.app_about_vtm)),
                    linkLabel = stringResource(R.string.app_about_vtm_link),
                    onOpenLink = { openLink(VTM_URL) },
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)) {
                    SectionTitle(stringResource(R.string.app_about_section_libraries))
                    BodyText(stringResource(R.string.app_about_libraries_hint))
                }
            }
            when (state) {
                AboutUiState.Loading -> item {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag(AboutTestTags.LIBRARIES_LOADING),
                    )
                }
                AboutUiState.LibrariesUnavailable -> item {
                    BodyText(stringResource(R.string.app_about_libraries_unavailable))
                }
                is AboutUiState.Loaded -> items(state.libraries, key = { it.id }) { library ->
                    LibraryRow(library = library, onClick = { openLibraryId = library.id })
                }
            }
        }
    }

    val openLibrary = (state as? AboutUiState.Loaded)?.libraries?.find { it.id == openLibraryId }
    if (openLibrary != null) {
        LicencesDialog(
            library = openLibrary,
            onOpenLink = openLink,
            onDismiss = { openLibraryId = null },
        )
    }
}

/**
 * The app: its name, [appVersion], its licence and a link to the licence text.
 *
 * @param appVersion the app's version.
 * @param onOpenLink opens a link.
 */
@Composable
private fun AppSection(appVersion: String, onOpenLink: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GisteTheme.dimensions.spacingSmall)) {
        SectionTitle(stringResource(R.string.app_name))
        BodyText(stringResource(R.string.app_about_version, appVersion))
        BodyText(stringResource(R.string.app_about_app_licence))
        LinkRow(
            label = stringResource(R.string.app_about_app_licence_link),
            onClick = { onOpenLink(GPL_URL) },
        )
    }
}

/**
 * The credit of something the app uses: a title, a few lines and a link.
 *
 * @param title what is credited, e.g. "Map data".
 * @param lines the credit, one paragraph each.
 * @param linkLabel the text of the link.
 * @param onOpenLink called when the link is tapped.
 */
@Composable
private fun Credit(
    title: String,
    lines: List<String>,
    linkLabel: String,
    onOpenLink: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GisteTheme.dimensions.spacingSmall)) {
        SectionTitle(title)
        lines.forEach { BodyText(it) }
        LinkRow(label = linkLabel, onClick = onOpenLink)
    }
}

/**
 * A link to a web page: its label and an "open in new" icon, in a row at least the minimum touch
 * target high, so it works with gloves.
 *
 * @param label the text of the link.
 * @param onClick called when the link is tapped.
 */
@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = GisteTheme.dimensions.minTouchTarget)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GisteTheme.dimensions.spacingSmall),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            painter = painterResource(R.drawable.app_ic_open_in_new),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * One library: its name, then its version and licences. The whole row opens its licences.
 *
 * @param library the library.
 * @param onClick called when the row is tapped.
 */
@Composable
private fun LibraryRow(library: OpenSourceLibrary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = GisteTheme.dimensions.minTouchTarget)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = library.name, style = MaterialTheme.typography.titleMedium)
        BodyText(library.details())
    }
}

/**
 * The licences of [library], in a dialog: each licence's name and full text, or a link to it when
 * the text is unknown. The texts are long, so the dialog's content scrolls.
 *
 * @param library the library whose licences are shown.
 * @param onOpenLink opens a link.
 * @param onDismiss called when the close button or the outside of the dialog is tapped.
 */
@Composable
private fun LicencesDialog(
    library: OpenSourceLibrary,
    onOpenLink: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(GisteTheme.dimensions.minTouchTarget)
                    .testTag(AboutTestTags.CLOSE_LICENCES),
            ) {
                Icon(painter = painterResource(R.drawable.app_ic_close), contentDescription = null)
            }
        },
        title = { Text(library.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GisteTheme.dimensions.spacingMedium),
            ) {
                library.licences.forEach { licence ->
                    Text(text = licence.name, style = MaterialTheme.typography.titleMedium)
                    when {
                        licence.text != null -> Text(
                            text = licence.text,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        licence.url != null -> LinkRow(
                            label = stringResource(R.string.app_about_app_licence_link),
                            onClick = { onOpenLink(licence.url) },
                        )
                    }
                }
            }
        },
    )
}

/** The title of a part of the screen. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** A paragraph of secondary text. */
@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * This library's version and licence names, as one line, e.g. "5.5.1 · Apache License 2.0". Only
 * the licence names when the version is unknown.
 */
@Composable
internal fun OpenSourceLibrary.details(): String {
    val licenceNames = licences.joinToString { it.name }
    return if (version == null) {
        licenceNames
    } else {
        stringResource(R.string.app_about_library_details, version, licenceNames)
    }
}

// --- Previews ---

/** Samsung Galaxy Tab Active3 in landscape: 1920 × 1200 px at 240 dpi. */
private const val TAB_ACTIVE3_LANDSCAPE = "spec:width=1280dp,height=800dp,dpi=240"

/** A typical phone in portrait. */
private const val PHONE_PORTRAIT = "spec:width=411dp,height=891dp,dpi=420"

/** Preview sample: the Apache licence, without its long text. */
private val APACHE = LibraryLicence(
    name = "Apache License 2.0",
    url = "https://spdx.org/licenses/Apache-2.0.html",
    text = "Apache License\nVersion 2.0, January 2004\n\nTERMS AND CONDITIONS FOR USE…",
)

/** Preview sample: a few of the app's libraries. */
private val LIBRARIES = AboutUiState.Loaded(
    listOf(
        OpenSourceLibrary("com.google.dagger:hilt-android", "Hilt Android", "2.60.1", listOf(APACHE)),
        OpenSourceLibrary("com.squareup.okhttp3:okhttp", "OkHttp", "5.5.1", listOf(APACHE)),
        OpenSourceLibrary("com.jakewharton.timber:timber", "Timber", null, listOf(APACHE)),
        OpenSourceLibrary(
            id = "org.mapsforge:vtm",
            name = "VTM",
            version = "0.25.0",
            licences = listOf(
                LibraryLicence(
                    name = "GNU Lesser General Public License v3.0 or later",
                    url = "https://www.gnu.org/licenses/lgpl-3.0.html",
                    text = null,
                ),
            ),
        ),
    ),
)

/** The About screen in [state], with the app's typography. */
@Composable
private fun AboutPreview(state: AboutUiState) {
    GisteTheme(typography = RiderTypography) {
        AboutContent(appVersion = "1.0", state = state, onBack = {})
    }
}

@Preview(name = "Phone, libraries, light", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Phone, libraries, dark", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tab Active3, libraries, light", device = TAB_ACTIVE3_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun AboutLoadedPreview() {
    AboutPreview(LIBRARIES)
}

@Preview(name = "Phone, loading, light", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Phone, loading, dark", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AboutLoadingPreview() {
    AboutPreview(AboutUiState.Loading)
}

@Preview(name = "Phone, unavailable, light", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Phone, unavailable, dark", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AboutUnavailablePreview() {
    AboutPreview(AboutUiState.LibrariesUnavailable)
}

@Preview(name = "Phone, licences, light", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Phone, licences, dark", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LicencesDialogPreview() {
    GisteTheme(typography = RiderTypography) {
        LicencesDialog(library = LIBRARIES.libraries.first(), onOpenLink = {}, onDismiss = {})
    }
}
