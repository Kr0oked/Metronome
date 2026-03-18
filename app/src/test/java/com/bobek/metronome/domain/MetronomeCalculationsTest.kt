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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetronomeTickTest {

    // getCurrentBeat

    @Test
    fun firstTickIsFirstBeat() {
        assertEquals(1, getCurrentBeat(tickCount = 0L, beats = 4, subdivisions = 1))
    }

    @Test
    fun beatWrapsAfterLastBeat() {
        assertEquals(1, getCurrentBeat(tickCount = 4L, beats = 4, subdivisions = 1))
    }

    @Test
    fun secondBeatWithNoSubdivisions() {
        assertEquals(2, getCurrentBeat(tickCount = 1L, beats = 4, subdivisions = 1))
    }

    @Test
    fun beatAdvancesEverySubdivision() {
        // 2 beats, 3 subdivisions: ticks 0,1,2 = beat 1; ticks 3,4,5 = beat 2
        assertEquals(1, getCurrentBeat(tickCount = 2L, beats = 2, subdivisions = 3))
        assertEquals(2, getCurrentBeat(tickCount = 3L, beats = 2, subdivisions = 3))
    }

    @Test
    fun beatWrapsWithSubdivisions() {
        // 2 beats, 3 subdivisions: 6 ticks per cycle; tick 6 = back to beat 1
        assertEquals(1, getCurrentBeat(tickCount = 6L, beats = 2, subdivisions = 3))
    }

    // getCurrentTickType

    @Test
    fun firstTickIsStrongWhenEmphasizeFirstBeatEnabled() {
        assertEquals(
            TickType.STRONG,
            getCurrentTickType(tickCount = 0L, beats = 4, subdivisions = 1, emphasizeFirstBeat = true)
        )
    }

    @Test
    fun firstTickIsWeakWhenEmphasizeFirstBeatDisabled() {
        assertEquals(
            TickType.WEAK,
            getCurrentTickType(tickCount = 0L, beats = 4, subdivisions = 1, emphasizeFirstBeat = false)
        )
    }

    @Test
    fun beatStartIsWeak() {
        // tick 1 is the start of beat 2 (no subdivisions), should be WEAK
        assertEquals(
            TickType.WEAK,
            getCurrentTickType(tickCount = 1L, beats = 4, subdivisions = 1, emphasizeFirstBeat = true)
        )
    }

    @Test
    fun subdivisionIsSubType() {
        // 4 beats, 2 subdivisions: tick 1 is mid-beat (not beat start), should be SUB
        assertEquals(
            TickType.SUB,
            getCurrentTickType(tickCount = 1L, beats = 4, subdivisions = 2, emphasizeFirstBeat = true)
        )
    }

    @Test
    fun firstBeatAfterFullCycleIsStrongWhenEmphasized() {
        // 4 beats, 2 subdivisions: tick 8 = start of new cycle
        assertEquals(
            TickType.STRONG,
            getCurrentTickType(tickCount = 8L, beats = 4, subdivisions = 2, emphasizeFirstBeat = true)
        )
    }

    @Test
    fun firstBeatAfterFullCycleIsWeakWhenNotEmphasized() {
        assertEquals(
            TickType.WEAK,
            getCurrentTickType(tickCount = 8L, beats = 4, subdivisions = 2, emphasizeFirstBeat = false)
        )
    }

    // isGap

    @Test
    fun beatInGapSetIsGap() {
        assertTrue(isGap(beat = 2, gapIndices = setOf(2, 4)))
    }

    @Test
    fun beatNotInGapSetIsNotGap() {
        assertFalse(isGap(beat = 3, gapIndices = setOf(2, 4)))
    }

    @Test
    fun emptyGapSetIsNeverGap() {
        assertFalse(isGap(beat = 1, gapIndices = emptySet()))
    }

    // calculatePeriodSize

    @Test
    fun periodSizeAt60BpmNoSubdivisions() {
        // 60 BPM, 1 subdivision: 48000 samples per beat
        assertEquals(48_000, calculatePeriodSize(tempo = 60, subdivisions = 1))
    }

    @Test
    fun periodSizeAt120BpmNoSubdivisions() {
        // 120 BPM, 1 subdivision: 24000 samples per beat
        assertEquals(24_000, calculatePeriodSize(tempo = 120, subdivisions = 1))
    }

    @Test
    fun periodSizeWithSubdivisions() {
        // 60 BPM, 2 subdivisions: 24000 samples per tick
        assertEquals(24_000, calculatePeriodSize(tempo = 60, subdivisions = 2))
    }
}
