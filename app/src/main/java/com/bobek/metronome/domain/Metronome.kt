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

package com.bobek.metronome.domain

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bobek.metronome.audio.SoundProvider
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Gaps
import com.bobek.metronome.data.Sound
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo
import com.bobek.metronome.data.Tick
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

private const val TAG = "Metronome"

/**
 * Maximum number of PCM float frames written per silence-fill call.
 *
 * Silence is written in chunks rather than one large write so that the coroutine can yield between chunks, allowing
 * tempo/subdivision changes to take effect mid-period without waiting for the entire remaining silence to drain through
 * the [AudioTrack] buffer.
 */
private const val SILENCE_CHUNK_SIZE = 8_000

/**
 * Core metronome engine.
 *
 * Audio is produced by writing PCM FLOAT mono frames at [SAMPLE_RATE_IN_HZ] (48 kHz) to a streaming [AudioTrack]. The
 * engine runs a continuous coroutine ([metronomeLoop]) on [Dispatchers.IO] that fills the track one *tick period* at a
 * time.
 *
 * ## Tick period
 * A tick period is the fixed-length block of audio belonging to a single subdivision tick. Its length in frames is:
 * ```
 * periodSize = 60 * SAMPLE_RATE_IN_HZ / tempo / subdivisions
 * ```
 * For example, at 120 BPM with 2 subdivisions the period is 12 000 frames (= 250 ms).
 *
 * Each period consists of:
 * 1. **Tick sound** — the preloaded PCM sample for the tick's type (STRONG / WEAK / SUB), written at the start of the
 *    period. Gap beats skip this part entirely.
 * 2. **Silence** — zero-valued frames that pad the rest of the period up to `periodSize` so that the next tick starts
 *    at the correct moment.
 *
 * ## Size / frame accounting
 * All "size" variables throughout this class count **PCM float frames** (= samples for mono audio), *not* bytes. One
 * frame equals one `Float` value (4 bytes).
 *
 * ## Tick notification timing
 * To keep the UI beat visualization in sync with the audible click, notifications are scheduled against
 * [AudioTimestamp] rather than fired immediately. The timestamp gives the wall-clock nanosecond at which a known frame
 * position will reach the speaker; from that the coroutine derives how far in the future the *first frame* of the
 * current period will be presented and delays the [MetronomeTickListener.onTick] call by exactly that amount.
 *
 * @param context Used to load audio assets via [SoundProvider].
 * @param lifecycle Scopes the internal coroutines; the engine is automatically cleaned up when the lifecycle is
 *   destroyed.
 * @param tickListener Callback fired (on the main dispatcher) once per tick, timed to coincide with the audible
 *   click.
 */
class Metronome(
    context: Context,
    override val lifecycle: Lifecycle,
    private val tickListener: MetronomeTickListener
) : LifecycleOwner {

    private val soundProvider = SoundProvider(context)

    /** Pre-allocated silent PCM float buffer reused for every silence-fill write. */
    private val silence = FloatArray(SILENCE_CHUNK_SIZE)

    private var metronomeJob: Job? = null

    @Volatile
    var beats: Beats = Beats()

    @Volatile
    var subdivisions: Subdivisions = Subdivisions()

    @Volatile
    var gaps: Gaps = Gaps()

    @Volatile
    var tempo: Tempo = Tempo()

    @Volatile
    var emphasizeFirstBeat = true

    @Volatile
    var sound = Sound.SQUARE_WAVE

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

    /**
     * Main audio loop. Runs until canceled (i.e. [stop] is called).
     *
     * Creates a single [AudioTrack] for the entire playback session and repeatedly calls [writeTickPeriod] to advance
     * `totalFramesWritten` — the running total of PCM frames pushed into the track since playback started. This counter
     * is needed by [scheduleTickNotification] to calculate how far ahead of the current playback position the *next*
     * tick sound begins.
     */
    private suspend fun metronomeLoop() {
        val track = getNewAudioTrack()
        track.play()

        try {
            var tickCount = 0L
            var totalFramesWritten = 0L
            while (true) {
                totalFramesWritten = writeTickPeriod(track, tickCount, totalFramesWritten)
                tickCount++
            }
        } catch (_: CancellationException) {
            Log.d(TAG, "Received cancellation")
            track.pause()
            Log.d(TAG, "Underrun count was ${track.underrunCount}")
        } finally {
            track.release()
        }
    }

    /**
     * Creates a streaming [AudioTrack] configured for 48 kHz mono PCM FLOAT output.
     *
     * The hardware buffer is sized to [AudioTrack.getMinBufferSize] — the smallest value that avoids underruns under
     * normal conditions. Streaming mode ([AudioTrack.MODE_STREAM]) means the app feeds audio data continuously via
     * [AudioTrack.write] rather than loading a fixed clip up front.
     */
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

    /**
     * Writes one complete tick period to [track] and returns the updated cumulative frame count.
     *
     * A period has exactly `calculatePeriodSize(tempo, subdivisions)` frames. The method first writes the tick sound
     * (unless the tick is a gap), then pads the remainder with silence via [writeSilenceUntilPeriodFinished].
     *
     * @param tickCount Zero-based sequential index of this tick across the entire playback session. Used to derive
     *   beat position and tick type.
     * @param totalFramesWritten Cumulative PCM frames written to [track] before this period. Passed to
     *   [scheduleTickNotification] so it can calculate the presentation timestamp of this period's first frame.
     * @return The updated cumulative frame count: `totalFramesWritten + sizeWritten`, where `sizeWritten` is the total
     *   frames for this period (tick sound + silence). Because tempo or subdivisions may change while silence is being
     *   filled, `periodSize` is re-evaluated on each iteration inside [writeSilenceUntilPeriodFinished], so the actual
     *   increment equals the period size that was in effect when the tick sound was written plus the silence that was
     *   added to reach the (possibly re-evaluated) boundary.
     */
    private suspend fun writeTickPeriod(track: AudioTrack, tickCount: Long, totalFramesWritten: Long): Long {
        var sizeWritten = 0

        val tick = getCurrentTick(tickCount)

        if (tick.gap) {
            Log.v(TAG, "Skipped gap for $tick")
        } else {
            val tickSound = soundProvider.getTickSound(tick.type, sound)
            val periodSize = calculatePeriodSize(tempo.value, subdivisions.value)

            sizeWritten += writeNextAudioData(track, tickSound, periodSize, sizeWritten)
            Log.v(TAG, "Wrote tick sound for $tick")
        }

        scheduleTickNotification(track, tick, totalFramesWritten)
        yield()

        sizeWritten += writeSilenceUntilPeriodFinished(track, sizeWritten)
        return totalFramesWritten + sizeWritten
    }

    /**
     * Schedules a [MetronomeTickListener.onTick] call to fire at the moment this tick's audio will actually be heard
     * by the user.
     *
     * The presentation delay is calculated via [calculatePresentationDelay]. If it is zero (timestamp not yet
     * available at the very start of playback) the notification is fired immediately; otherwise a coroutine delay is
     * used.
     *
     * @param totalFramesWritten Cumulative frames written to [track] up to (but not including) this period — i.e. the
     *   frame index where this period's first sample will be played.
     */
    private fun scheduleTickNotification(track: AudioTrack, tick: Tick, totalFramesWritten: Long) {
        val presentationDelay = calculatePresentationDelay(track, totalFramesWritten)

        if (presentationDelay == Duration.ZERO) {
            tickListener.onTick(tick)
        } else {
            lifecycleScope.launch {
                delay(presentationDelay)
                tickListener.onTick(tick)
            }
        }
        Log.v(TAG, "Scheduled tick notification for $tick with delay ${presentationDelay.inWholeMilliseconds}ms")
    }

    /**
     * Returns the [Duration] from now until the first frame at `totalFramesWritten` will be presented by the audio
     * hardware.
     *
     * Uses [AudioTrack.getTimestamp] to anchor the calculation to a known frame/time pair, then extrapolates forward
     * to `totalFramesWritten`:
     * ```
     * delayNanos = (totalFramesWritten - timestamp.framePosition) * nanosPerFrame - (now - timestamp.nanoTime)
     * ```
     * Returns [Duration.ZERO] if the timestamp is not yet available (common at the very start of playback).
     */
    private fun calculatePresentationDelay(track: AudioTrack, totalFramesWritten: Long): Duration {
        val audioTimestamp = AudioTimestamp()
        if (!track.getTimestamp(audioTimestamp)) return Duration.ZERO

        val nanosPerFrame = 1_000_000_000L / SAMPLE_RATE_IN_HZ
        val timestampAgeNanos = System.nanoTime() - audioTimestamp.nanoTime
        val framesAheadOfTimestamp = totalFramesWritten - audioTimestamp.framePosition
        val delayNanos = framesAheadOfTimestamp * nanosPerFrame - timestampAgeNanos
        return delayNanos.nanoseconds.coerceAtLeast(Duration.ZERO)
    }

    /**
     * Fills the rest of the current tick period with silence by looping until the total number of frames written for
     * this period reaches `periodSize`.
     *
     * Silence is written in chunks of at most [SILENCE_CHUNK_SIZE] frames, with a [yield] after each chunk. This gives
     * the coroutine dispatcher a chance to pick up tempo/subdivision changes between chunks so that the period boundary
     * is re-evaluated with fresh values.
     *
     * @param previousSizeWritten Frames already written for this period before silence filling began (i.e. the tick
     *   sound size, or 0 for a gap).
     * @return The number of silence frames written.
     */
    private suspend fun writeSilenceUntilPeriodFinished(track: AudioTrack, previousSizeWritten: Int): Int {
        var silenceWritten = 0

        while (true) {
            val periodSize = calculatePeriodSize(tempo.value, subdivisions.value)
            val totalWritten = previousSizeWritten + silenceWritten
            if (totalWritten >= periodSize) break

            silenceWritten += writeNextAudioData(track, silence, periodSize, totalWritten)
            Log.v(TAG, "Wrote silence")
            yield()
        }

        return silenceWritten
    }

    private fun getCurrentTick(tickCount: Long): Tick {
        val beat = getCurrentBeat(tickCount, beats.value, subdivisions.value)
        return Tick(
            beat = beat,
            type = getCurrentTickType(tickCount, beats.value, subdivisions.value, emphasizeFirstBeat),
            gap = isGap(beat, gaps.value)
        )
    }

    /**
     * Writes the next slice of [data] into [track], capped so that the total written for this period never exceeds
     * `periodSize`.
     *
     * @param sizeWritten Frames already written in this period before this call.
     * @return The number of frames actually written (≤ `data.size` and ≤ `periodSize - sizeWritten`).
     */
    private fun writeNextAudioData(track: AudioTrack, data: FloatArray, periodSize: Int, sizeWritten: Int): Int {
        val size = minOf(data.size, periodSize - sizeWritten)
        writeAudio(track, data, size)
        return size
    }

    /**
     * Writes [size] frames from [data] to [track] using [AudioTrack.WRITE_BLOCKING].
     *
     * Blocking mode means the call parks the calling thread until the hardware buffer has room for the data, which is
     * the primary back-pressure mechanism keeping the audio loop from running ahead of the speaker.
     *
     * @throws IllegalStateException if [AudioTrack.write] returns an error code.
     */
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
}
