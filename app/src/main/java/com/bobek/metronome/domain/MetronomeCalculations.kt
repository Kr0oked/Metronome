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

internal fun getCurrentBeat(tickCount: Long, beats: Int, subdivisions: Int): Int =
    (((tickCount / subdivisions) % beats) + 1).toInt()

internal fun getCurrentTickType(
    tickCount: Long,
    beats: Int,
    subdivisions: Int,
    emphasizeFirstBeat: Boolean
): TickType = when {
    emphasizeFirstBeat && tickCount % (beats * subdivisions) == 0L -> TickType.STRONG
    tickCount % subdivisions == 0L -> TickType.WEAK
    else -> TickType.SUB
}

internal fun isGap(beat: Int, gapIndices: Set<Int>): Boolean = gapIndices.contains(beat)

internal fun calculatePeriodSize(tempo: Int, subdivisions: Int): Int =
    60 * SAMPLE_RATE_IN_HZ / tempo / subdivisions
