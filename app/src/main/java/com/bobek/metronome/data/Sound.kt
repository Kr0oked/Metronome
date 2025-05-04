/*
 * This file is part of Metronome.
 * Copyright (C) 2023 Philipp Bobek <philipp.bobek@mailbox.org>
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

import androidx.annotation.RawRes
import com.bobek.metronome.R
import com.bobek.metronome.preference.PreferenceConstants

enum class Sound(
    @RawRes val strongSoundResourceId: Int,
    @RawRes val weakSoundResourceId: Int,
    @RawRes val subSoundResourceId: Int,
    val preferenceValue: String
) {
    SQUARE_WAVE(
        R.raw.square_wave_strong,
        R.raw.square_wave_weak,
        R.raw.square_wave_sub,
        PreferenceConstants.SOUND_VALUE_SQUARE_WAVE
    ),
    SINE_WAVE(
        R.raw.sine_wave_strong,
        R.raw.sine_wave_weak,
        R.raw.sine_wave_sub,
        PreferenceConstants.SOUND_VALUE_SINE_WAVE
    ),
    RISSET_DRUM(
        R.raw.risset_drum_strong,
        R.raw.risset_drum_weak,
        R.raw.risset_drum_sub,
        PreferenceConstants.SOUND_VALUE_RISSET_DRUM
    ),
    PLUCK(
        R.raw.pluck_strong,
        R.raw.pluck_weak,
        R.raw.pluck_sub,
        PreferenceConstants.SOUND_VALUE_PLUCK
    );

    companion object {
        fun forPreferenceValue(preferenceValue: String): Sound {
            return when (preferenceValue) {
                PreferenceConstants.SOUND_VALUE_SQUARE_WAVE -> SQUARE_WAVE
                PreferenceConstants.SOUND_VALUE_SINE_WAVE -> SINE_WAVE
                PreferenceConstants.SOUND_VALUE_RISSET_DRUM -> RISSET_DRUM
                PreferenceConstants.SOUND_VALUE_PLUCK -> PLUCK
                else -> SQUARE_WAVE
            }
        }
    }
}
