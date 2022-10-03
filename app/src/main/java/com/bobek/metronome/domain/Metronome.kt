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

package com.bobek.metronome.domain

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import androidx.annotation.RawRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bobek.metronome.R
import com.bobek.metronome.audio.SoundLoader
import com.bobek.metronome.data.*
import kotlinx.coroutines.*

private const val TAG = "Metronome"
private const val SAMPLE_RATE_IN_HZ = 48_000

class Metronome(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val tickListener: MetronomeTickListener
) : LifecycleOwner {

    private val strongTick = loadSound(R.raw.strong_tick)
    private val weakTick = loadSound(R.raw.weak_tick)
    private val subTick = loadSound(R.raw.sub_tick)

    private var counter = 0L

    @Volatile
    private var metronomeJob: Job? = null

    @Volatile
    var beats: Beats = Beats()

    @Volatile
    var subdivisions: Subdivisions = Subdivisions()

    @Volatile
    var tempo: Tempo = Tempo()

    var playing: Boolean = false
        set(playing) {
            if (field != playing) {
                field = playing
                if (playing) start() else stop()
            }
        }
        get() = metronomeJob != null

    private fun start() {
        counter = 0L

        metronomeJob = lifecycleScope.launch(Dispatchers.IO) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setSampleRate(SAMPLE_RATE_IN_HZ)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val audioTrack = AudioTrack(
                audioAttributes,
                audioFormat,
                AudioTrack.getMinBufferSize(audioFormat.sampleRate, audioFormat.channelMask, audioFormat.encoding),
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            audioTrack.play()

            try {
                while (true) {
                    val currentBeat = getCurrentBeat()
                    val currentTickType = getCurrentTickType()
                    val tick = Tick(currentBeat, currentTickType)

                    when (tick.type) {
                        TickType.STRONG -> audioTrack.write(strongTick, 0, strongTick.size, AudioTrack.WRITE_BLOCKING)
                        TickType.WEAK -> audioTrack.write(weakTick, 0, weakTick.size, AudioTrack.WRITE_BLOCKING)
                        TickType.SUB -> audioTrack.write(subTick, 0, subTick.size, AudioTrack.WRITE_BLOCKING)
                    }

                    tickListener.onTick(tick)
                    Log.d(TAG, "Wrote silence")
                    yield()

                    val silenceUntilNextTick = FloatArray(calculateSilenceBetweenTicks())
                    audioTrack.write(silenceUntilNextTick, 0, silenceUntilNextTick.size, AudioTrack.WRITE_BLOCKING)
                    Log.d(TAG, "Wrote silence")
                    yield()

                    counter++
                }
            } catch (exception: CancellationException) {
                audioTrack.pause()
                Log.d(TAG, "Cancelled")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Log.d(TAG, "Underrun count was ${audioTrack.underrunCount}")
                }
                audioTrack.release()
            }
        }

        Log.d(TAG, "Started metronome job")
    }

    private fun stop() {
        metronomeJob?.cancel()
        metronomeJob = null
        Log.d(TAG, "Stopped metronome job")
    }

    private fun getCurrentBeat() = (((counter / subdivisions.value) % beats.value) + 1).toInt()

    private fun getCurrentTickType(): TickType {
        return when {
            isStrongTick() -> TickType.STRONG
            isWeakTick() -> TickType.WEAK
            else -> TickType.SUB
        }
    }

    private fun isStrongTick() = counter % (beats.value * subdivisions.value) == 0L

    private fun isWeakTick() = counter % subdivisions.value == 0L

    private fun calculateSilenceBetweenTicks() = 60 * SAMPLE_RATE_IN_HZ / tempo.value / subdivisions.value

    override fun getLifecycle(): Lifecycle = lifecycle

    private fun loadSound(@RawRes id: Int): FloatArray = context.resources
        .openRawResource(id)
        .use(SoundLoader::readDataFromWavPcmFloat)
}
