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

package dev.giste.roadbooknavigator.settings

/**
 * The remote control settings: which remote the rider uses, and the keys they assigned for a
 * [RemoteModel.CUSTOM] one.
 *
 * @property model the remote in use. Defaults to [DEFAULT_MODEL].
 * @property customKeys the keys of [RemoteModel.CUSTOM], kept while another model is in use so
 * they come back when the rider selects it again. Defaults to [DEFAULT_CUSTOM_KEYS].
 */
public data class RemoteSettings(
    public val model: RemoteModel = DEFAULT_MODEL,
    public val customKeys: RemoteKeys = DEFAULT_CUSTOM_KEYS,
) {

    /**
     * The keys in use: the fixed keys of [model], or [customKeys] for [RemoteModel.CUSTOM].
     */
    public val keys: RemoteKeys
        get() = when (model) {
            RemoteModel.DND2 -> RemoteKeys.DND2
            RemoteModel.TERRA_PIRATA -> RemoteKeys.TERRA_PIRATA
            RemoteModel.CUSTOM -> customKeys
        }

    /** Default values of [RemoteSettings]. */
    public companion object {

        /** Default [model]: the Terra Pirata remote. */
        public val DEFAULT_MODEL: RemoteModel = RemoteModel.TERRA_PIRATA

        /**
         * Default [customKeys]: the keys of the default remote, Terra Pirata, a starting point the
         * rider changes.
         */
        public val DEFAULT_CUSTOM_KEYS: RemoteKeys = RemoteKeys.TERRA_PIRATA
    }
}
