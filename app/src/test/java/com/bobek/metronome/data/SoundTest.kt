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
import org.junit.Assert.assertEquals
import org.junit.Test

class SoundTest {

    @Test
    fun squareWavePreferenceValue() {
        assertEquals(Sound.SQUARE_WAVE, Sound.forPreferenceValue(PreferenceConstants.SOUND_VALUE_SQUARE_WAVE))
    }

    @Test
    fun sineWavePreferenceValue() {
        assertEquals(Sound.SINE_WAVE, Sound.forPreferenceValue(PreferenceConstants.SOUND_VALUE_SINE_WAVE))
    }

    @Test
    fun rissetDrumPreferenceValue() {
        assertEquals(Sound.RISSET_DRUM, Sound.forPreferenceValue(PreferenceConstants.SOUND_VALUE_RISSET_DRUM))
    }

    @Test
    fun pluckPreferenceValue() {
        assertEquals(Sound.PLUCK, Sound.forPreferenceValue(PreferenceConstants.SOUND_VALUE_PLUCK))
    }

    @Test
    fun unknownPreferenceValueFallsBackToSquareWave() {
        assertEquals(Sound.SQUARE_WAVE, Sound.forPreferenceValue("unknown"))
    }
}
