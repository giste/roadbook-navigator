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

package dev.giste.roadbooknavigator.domain

/**
 * An open-source library the app ships, as the About screen lists it.
 *
 * @property id the library's Maven coordinates, e.g. `com.squareup.okhttp3:okhttp`: unique in the
 * list, unlike [name].
 * @property name the library's name, e.g. "OkHttp".
 * @property version the version the app ships, if known.
 * @property licences the licences it is distributed under, sorted by name. Never empty in the
 * app's own list, since the build fails for a library without an allowed licence.
 */
internal data class OpenSourceLibrary(
    val id: String,
    val name: String,
    val version: String?,
    val licences: List<LibraryLicence>,
)

/**
 * A licence an open-source library is distributed under.
 *
 * @property name the licence's name, e.g. "Apache License 2.0".
 * @property url where the licence can be read online, if known.
 * @property text the full licence text, if known. The app's build includes it for every licence
 * its libraries use.
 */
internal data class LibraryLicence(
    val name: String,
    val url: String?,
    val text: String?,
)
