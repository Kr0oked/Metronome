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

import com.bobek.metronome.data.TickType

internal const val SAMPLE_RATE_IN_HZ = 48_000

/**
 * Position of a tick within the bar.
 *
 * Ticks are anchored to the beat rather than counted globally, so that changing the beats or subdivisions while
 * playing never moves the beat grid: subdivision ticks are positions inside the current beat, and changing the
 * subdivisions only changes which of those positions sound.
 *
 * @param beatInBar Zero-based index of the beat within the bar.
 * @param subdivisionIndex Zero-based index of the subdivision within the beat; 0 is the beat itself.
 * @param tickStartInBeat Number of PCM float frames from the start of the beat to the start of this tick.
 */
internal data class TickPosition(
    val beatInBar: Int = 0,
    val subdivisionIndex: Int = 0,
    val tickStartInBeat: Int = 0
)

internal fun getCurrentBeat(position: TickPosition): Int = position.beatInBar + 1

internal fun getCurrentTickType(position: TickPosition, emphasizeFirstBeat: Boolean): TickType = when {
    position.subdivisionIndex != 0 -> TickType.SUB
    emphasizeFirstBeat && position.beatInBar == 0 -> TickType.STRONG
    else -> TickType.WEAK
}

internal fun isGap(beat: Int, gapIndices: Set<Int>): Boolean = gapIndices.contains(beat)

/** Number of PCM float frames in one beat at [tempo]. */
internal fun calculateBeatSize(tempo: Int): Int = 60 * SAMPLE_RATE_IN_HZ / tempo

/**
 * Frame offset of subdivision [subdivisionIndex] from the start of the beat.
 *
 * Calculated from the beat start rather than by adding up subdivision periods, so rounding never accumulates; the
 * offset of index [subdivisions] is exactly [beatSize].
 */
internal fun calculateSubdivisionOffset(subdivisionIndex: Int, beatSize: Int, subdivisions: Int): Int =
    (subdivisionIndex.toLong() * beatSize / subdivisions).toInt()

/**
 * Returns the index of the next subdivision to play, or [subdivisions] if the next tick is the start of the next beat.
 *
 * That is the first subdivision starting after [tickStartInBeat] that hasn't been passed yet, i.e. that doesn't start
 * before the [framesWritten] frames already written for the current tick have ended. Subdivisions that were passed
 * because a change was only picked up after their start are skipped rather than played late.
 */
internal fun getNextSubdivisionIndex(tickStartInBeat: Int, framesWritten: Int, beatSize: Int, subdivisions: Int): Int {
    val earliestNextTickStart = tickStartInBeat + framesWritten.coerceAtLeast(1)
    return (1 until subdivisions)
        .firstOrNull { calculateSubdivisionOffset(it, beatSize, subdivisions) >= earliestNextTickStart }
        ?: subdivisions
}

/**
 * Number of frames from the start of the tick at [position] to the start of the next tick, given the current
 * [beatSize] and [subdivisions] and the [framesWritten] for the current tick so far.
 *
 * Not more than [framesWritten] only when the beat has already ended, which can happen after the tempo was increased;
 * the next beat is then due immediately.
 */
internal fun calculateFramesUntilNextTick(
    position: TickPosition,
    framesWritten: Int,
    beatSize: Int,
    subdivisions: Int
): Int {
    val nextIndex = getNextSubdivisionIndex(position.tickStartInBeat, framesWritten, beatSize, subdivisions)
    val nextOffset = calculateSubdivisionOffset(nextIndex, beatSize, subdivisions)
    return (nextOffset - position.tickStartInBeat).coerceAtLeast(0)
}

/**
 * Returns the position of the tick that follows the tick at [position] once [framesWritten] frames of it were written,
 * i.e. once [calculateFramesUntilNextTick] was reached.
 *
 * A new beat always starts at frame 0 of that beat. When [beats] was reduced so that the current beat is now past the
 * end of the bar, the next beat is the first one.
 */
internal fun getNextTickPosition(
    position: TickPosition,
    framesWritten: Int,
    beats: Int,
    beatSize: Int,
    subdivisions: Int
): TickPosition {
    val nextIndex = getNextSubdivisionIndex(position.tickStartInBeat, framesWritten, beatSize, subdivisions)
    return if (nextIndex < subdivisions) {
        position.copy(subdivisionIndex = nextIndex, tickStartInBeat = position.tickStartInBeat + framesWritten)
    } else {
        TickPosition(beatInBar = getNextBeatInBar(position.beatInBar, beats))
    }
}

internal fun getNextBeatInBar(beatInBar: Int, beats: Int): Int =
    if (beatInBar + 1 >= beats) 0 else beatInBar + 1
