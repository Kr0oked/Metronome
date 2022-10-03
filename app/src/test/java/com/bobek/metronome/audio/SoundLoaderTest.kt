/*
 * This file is part of Metronome.
 * Copyright (C) 2022 Philipp Bobek <philipp.bobek@mailbox.org>
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

package com.bobek.metronome.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SoundLoaderTest {

    @Test
    fun singleFrame() {
        val data = ClassLoader.getSystemResourceAsStream("singleFrame.wav")
            .use(SoundLoader::readDataFromWavPcmFloat)
        assertArrayEquals(floatArrayOf(1.0f), data, 0.0f)
    }

    @Test
    fun noContent() {
        val data = ClassLoader.getSystemResourceAsStream("no_payload.wav")
            .use(SoundLoader::readDataFromWavPcmFloat)
        assertArrayEquals(FloatArray(0), data, 0.0f)
    }

    @Test
    fun invalidData() {
        assertThrows(IllegalStateException::class.java) {
            ClassLoader.getSystemResourceAsStream("invalid_data.wav")
                .use(SoundLoader::readDataFromWavPcmFloat)
        }
    }

    @Test
    fun noDataMarker() {
        assertThrows(IllegalStateException::class.java) {
            ClassLoader.getSystemResourceAsStream("no_data_marker.wav")
                .use(SoundLoader::readDataFromWavPcmFloat)
        }
    }
}
