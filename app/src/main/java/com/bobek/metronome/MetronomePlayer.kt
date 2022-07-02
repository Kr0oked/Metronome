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

package com.bobek.metronome

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.bobek.metronome.domain.TickType

private const val TAG = "MetronomePlayer"
private const val MAX_STREAMS = 8
private const val DEFAULT_SOUND_PRIORITY = 1
private const val NO_LOOP = 0
private const val VOLUME_MAX = 1.0f
private const val NORMAL_PLAYBACK = 1.0f

class MetronomePlayer(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .build()
        )
        .build()

    private val strongTickSoundId: Int
    private val weakTickSoundId: Int
    private val subTickSoundId: Int

    private var active = true

    init {
        strongTickSoundId = soundPool.load(context, R.raw.strong_tick, DEFAULT_SOUND_PRIORITY)
        weakTickSoundId = soundPool.load(context, R.raw.weak_tick, DEFAULT_SOUND_PRIORITY)
        subTickSoundId = soundPool.load(context, R.raw.sub_tick, DEFAULT_SOUND_PRIORITY)
        Log.d(TAG, "Initialized")
    }

    fun play(tickType: TickType) {
        if (active) {
            when (tickType) {
                TickType.STRONG -> playSound(strongTickSoundId)
                TickType.WEAK -> playSound(weakTickSoundId)
                TickType.SUB -> playSound(subTickSoundId)
            }
        } else {
            Log.w(TAG, "Instance was already released")
        }
    }

    private fun playSound(soundId: Int) {
        val streamId = soundPool.play(
            soundId,
            VOLUME_MAX,
            VOLUME_MAX,
            DEFAULT_SOUND_PRIORITY,
            NO_LOOP,
            NORMAL_PLAYBACK
        )
        Log.v(TAG, "Started stream with ID <$streamId>")
    }

    fun release() {
        active = false
        soundPool.release()
        Log.d(TAG, "Released")
    }
}
