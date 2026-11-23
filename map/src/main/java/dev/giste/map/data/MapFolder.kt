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

package dev.giste.map.data

import javax.inject.Qualifier

/**
 * Qualifies the `java.io.File` of the folder that holds the offline map files (`map_files`, in
 * the app's no-backup folder).
 *
 * It keeps this binding apart from any other `File` an app provides, and lets tests use a
 * temporary folder.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class MapFolder
