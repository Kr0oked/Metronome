/*
 * This file is part of Metronome.
 * Copyright (C) 2026 Philipp Bobek <philipp.bobek@mailbox.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metronome is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bobek.metronome.settings

import androidx.preference.PreferenceDataStore
import com.bobek.metronome.data.AppNightMode
import com.bobek.metronome.data.Sound
import com.bobek.metronome.view.model.MetronomeViewModel

class SettingsPreferenceDataStoreAdapter(
    private val viewModel: MetronomeViewModel
) : PreferenceDataStore() {

    override fun putString(key: String, value: String?) {
        when (key) {
            PreferenceConstants.SOUND -> {
                value?.let {
                    viewModel.sound.value = Sound.forPreferenceValue(it)
                }
            }

            PreferenceConstants.NIGHT_MODE -> {
                value?.let {
                    viewModel.nightMode.value = AppNightMode.forPreferenceValue(it)
                }
            }
        }
    }

    override fun getString(key: String, defValue: String?): String? {
        return when (key) {
            PreferenceConstants.SOUND -> viewModel.sound.value?.preferenceValue ?: defValue
            PreferenceConstants.NIGHT_MODE -> viewModel.nightMode.value?.preferenceValue ?: defValue
            else -> defValue
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        when (key) {
            PreferenceConstants.EMPHASIZE_FIRST_BEAT -> viewModel.emphasizeFirstBeat.value = value
        }
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return when (key) {
            PreferenceConstants.EMPHASIZE_FIRST_BEAT -> viewModel.emphasizeFirstBeat.value ?: defValue
            else -> defValue
        }
    }
}
