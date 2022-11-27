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
private const val SILENCE_CHUNK_SIZE = 8_000

class Metronome(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val tickListener: MetronomeTickListener
) : LifecycleOwner {

    private val strongTick = loadSound(R.raw.strong_tick)
    private val weakTick = loadSound(R.raw.weak_tick)
    private val subTick = loadSound(R.raw.sub_tick)
    private val silence = FloatArray(SILENCE_CHUNK_SIZE)

    private var metronomeJob: Job? = null

    @Volatile
    var beats: Beats = Beats()

    @Volatile
    var subdivisions: Subdivisions = Subdivisions()

    @Volatile
    var tempo: Tempo = Tempo()

    @Volatile
    var emphasizeFirstBeat = true

    var playing: Boolean = false
        set(playing) {
            if (field != playing) {
                field = playing
                if (playing) start() else stop()
            }
        }
        get() = metronomeJob != null

    private fun start() {
        metronomeJob = lifecycleScope.launch(Dispatchers.IO) { metronomeLoop() }
        Log.i(TAG, "Started metronome job")
    }

    private suspend fun metronomeLoop() {
        val track = getNewAudioTrack()
        track.play()

        try {
            var tickCount = 0L
            while (true) {
                writeTickPeriod(track, tickCount)
                tickCount++
            }
        } catch (exception: CancellationException) {
            Log.d(TAG, "Received cancellation")
            track.pause()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.d(TAG, "Underrun count was ${track.underrunCount}")
            }
        } finally {
            track.release()
        }
    }

    private fun getNewAudioTrack(): AudioTrack {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setSampleRate(SAMPLE_RATE_IN_HZ)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        return AudioTrack(
            audioAttributes,
            audioFormat,
            AudioTrack.getMinBufferSize(audioFormat.sampleRate, audioFormat.channelMask, audioFormat.encoding),
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    private suspend fun writeTickPeriod(track: AudioTrack, tickCount: Long) {
        var sizeWritten = 0

        val tick = getCurrentTick(tickCount)
        val tickSound = getTickSound(tick.type)
        val periodSize = calculatePeriodSize()

        sizeWritten += writeNextAudioData(track, tickSound, periodSize, sizeWritten)
        Log.v(TAG, "Wrote tick sound for $tick")
        tickListener.onTick(tick)
        yield()

        writeSilenceUntilPeriodFinished(track, sizeWritten)
    }

    private suspend fun writeSilenceUntilPeriodFinished(track: AudioTrack, previousSizeWritten: Int) {
        var sizeWritten = previousSizeWritten
        while (true) {
            val periodSize = calculatePeriodSize()
            if (sizeWritten >= periodSize) {
                break
            }

            sizeWritten += writeNextAudioData(track, silence, periodSize, sizeWritten)
            Log.v(TAG, "Wrote silence")
            yield()
        }
    }

    private fun getCurrentTick(tickCount: Long) = Tick(getCurrentBeat(tickCount), getCurrentTickType(tickCount))

    private fun getCurrentBeat(tickCount: Long) = (((tickCount / subdivisions.value) % beats.value) + 1).toInt()

    private fun getCurrentTickType(tickCount: Long): TickType {
        return when {
            isStrongTick(tickCount) -> TickType.STRONG
            isWeakTick(tickCount) -> TickType.WEAK
            else -> TickType.SUB
        }
    }

    private fun isStrongTick(tickCount: Long) =
        emphasizeFirstBeat && (tickCount % (beats.value * subdivisions.value) == 0L)

    private fun isWeakTick(tickCount: Long) = tickCount % subdivisions.value == 0L

    private fun calculatePeriodSize() = 60 * SAMPLE_RATE_IN_HZ / tempo.value / subdivisions.value

    private fun getTickSound(tickType: TickType): FloatArray {
        return when (tickType) {
            TickType.STRONG -> strongTick
            TickType.WEAK -> weakTick
            TickType.SUB -> subTick
        }
    }

    private fun writeNextAudioData(track: AudioTrack, data: FloatArray, periodSize: Int, sizeWritten: Int): Int {
        val size = calculateAudioSizeToWriteNext(data, periodSize, sizeWritten)
        writeAudio(track, data, size)
        return size
    }

    private fun calculateAudioSizeToWriteNext(data: FloatArray, periodSize: Int, sizeWritten: Int): Int {
        val sizeLeft = periodSize - sizeWritten
        return if (data.size > sizeLeft) sizeLeft else data.size
    }

    private fun writeAudio(track: AudioTrack, data: FloatArray, size: Int) {
        val result = track.write(data, 0, size, AudioTrack.WRITE_BLOCKING)
        if (result < 0) {
            throw IllegalStateException("Failed to play audio data. Error code: $result")
        }
    }

    private fun stop() {
        metronomeJob?.cancel()
        metronomeJob = null
        Log.i(TAG, "Stopped metronome job")
    }

    override fun getLifecycle(): Lifecycle = lifecycle

    private fun loadSound(@RawRes id: Int): FloatArray = context.resources
        .openRawResource(id)
        .use(SoundLoader::readDataFromWavPcmFloat)
}
