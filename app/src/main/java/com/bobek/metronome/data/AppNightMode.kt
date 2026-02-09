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

package com.bobek.metronome.data

import com.bobek.metronome.settings.PreferenceConstants

enum class AppNightMode(val preferenceValue: String) {

    FOLLOW_SYSTEM(PreferenceConstants.NIGHT_MODE_VALUE_FOLLOW_SYSTEM),
    NO(PreferenceConstants.NIGHT_MODE_VALUE_NO),
    YES(PreferenceConstants.NIGHT_MODE_VALUE_YES);

    companion object {
        fun forPreferenceValue(preferenceValue: String): AppNightMode {
            return when (preferenceValue) {
                PreferenceConstants.NIGHT_MODE_VALUE_NO -> NO
                PreferenceConstants.NIGHT_MODE_VALUE_YES -> YES
                else -> FOLLOW_SYSTEM
            }
        }
    }
}
