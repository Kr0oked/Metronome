/*
 * This file is part of Metronome.
 * Copyright (C) 2025 Philipp Bobek <philipp.bobek@mailbox.org>
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

import android.content.Context
import androidx.annotation.RawRes
import com.bobek.metronome.data.Sound
import com.bobek.metronome.data.TickType

class SoundProvider(private val context: Context) {

    private val loadedSounds = Sound.entries
        .map { listOf(it.strongSoundResourceId, it.weakSoundResourceId, it.subSoundResourceId) }
        .flatten()
        .associateWith { loadSound(it) }

    private fun loadSound(@RawRes id: Int): FloatArray = context.resources
        .openRawResource(id)
        .use(SoundLoader::readDataFromWavPcmFloat)

    fun getTickSound(tickType: TickType, sound: Sound): FloatArray = when (tickType) {
        TickType.STRONG -> getLoadedSound(sound.strongSoundResourceId)
        TickType.WEAK -> getLoadedSound(sound.weakSoundResourceId)
        TickType.SUB -> getLoadedSound(sound.subSoundResourceId)
    }

    private fun getLoadedSound(@RawRes id: Int): FloatArray = loadedSounds[id] ?: throw SoundNotLoadedException(id)
}
