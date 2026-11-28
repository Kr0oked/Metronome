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
        assertEquals(1, getCurrentBeat(TickPosition()))
    }

    @Test
    fun beatIsOneBasedBeatInBar() {
        assertEquals(3, getCurrentBeat(TickPosition(beatInBar = 2, subdivisionIndex = 1)))
    }

    // getCurrentTickType

    @Test
    fun firstTickIsStrongWhenEmphasizeFirstBeatEnabled() {
        assertEquals(TickType.STRONG, getCurrentTickType(TickPosition(), emphasizeFirstBeat = true))
    }

    @Test
    fun firstTickIsWeakWhenEmphasizeFirstBeatDisabled() {
        assertEquals(TickType.WEAK, getCurrentTickType(TickPosition(), emphasizeFirstBeat = false))
    }

    @Test
    fun beatStartIsWeak() {
        assertEquals(TickType.WEAK, getCurrentTickType(TickPosition(beatInBar = 1), emphasizeFirstBeat = true))
    }

    @Test
    fun subdivisionIsSubType() {
        assertEquals(
            TickType.SUB,
            getCurrentTickType(TickPosition(beatInBar = 0, subdivisionIndex = 1), emphasizeFirstBeat = true),
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

    // calculateBeatSize

    @Test
    fun beatSizeAt60Bpm() {
        assertEquals(48_000, calculateBeatSize(tempo = 60))
    }

    @Test
    fun beatSizeAt120Bpm() {
        assertEquals(24_000, calculateBeatSize(tempo = 120))
    }

    // calculateSubdivisionOffset

    @Test
    fun subdivisionOffsetsSplitBeatEvenly() {
        assertEquals(0, calculateSubdivisionOffset(subdivisionIndex = 0, beatSize = 48_000, subdivisions = 4))
        assertEquals(12_000, calculateSubdivisionOffset(subdivisionIndex = 1, beatSize = 48_000, subdivisions = 4))
        assertEquals(36_000, calculateSubdivisionOffset(subdivisionIndex = 3, beatSize = 48_000, subdivisions = 4))
    }

    @Test
    fun lastSubdivisionOffsetIsExactlyBeatSizeEvenIfNotDivisible() {
        assertEquals(10, calculateSubdivisionOffset(subdivisionIndex = 3, beatSize = 10, subdivisions = 3))
    }

    // getNextSubdivisionIndex

    @Test
    fun nextSubdivisionIndexFromBeatStart() {
        assertEquals(
            1,
            getNextSubdivisionIndex(tickStartInBeat = 0, framesWritten = 2_000, beatSize = 48_000, subdivisions = 4),
        )
    }

    @Test
    fun nextSubdivisionIndexIsNeverCurrentTickWhenNothingWrittenYet() {
        // Gap ticks start with nothing written
        assertEquals(
            2,
            getNextSubdivisionIndex(tickStartInBeat = 12_000, framesWritten = 0, beatSize = 48_000, subdivisions = 4),
        )
    }

    @Test
    fun nextSubdivisionIndexSkipsPassedSubdivisions() {
        // Subdivisions changed from 1 to 4 while 30 000 frames into the beat: next tick at index 3 (36 000)
        assertEquals(
            3,
            getNextSubdivisionIndex(tickStartInBeat = 0, framesWritten = 30_000, beatSize = 48_000, subdivisions = 4),
        )
    }

    @Test
    fun nextSubdivisionIndexIsSubdivisionStartingExactlyWhereWritingEnded() {
        assertEquals(
            1,
            getNextSubdivisionIndex(tickStartInBeat = 0, framesWritten = 12_000, beatSize = 48_000, subdivisions = 4),
        )
    }

    @Test
    fun nextSubdivisionIndexIsNextBeatWithoutSubdivisions() {
        assertEquals(
            1,
            getNextSubdivisionIndex(tickStartInBeat = 0, framesWritten = 2_000, beatSize = 48_000, subdivisions = 1),
        )
    }

    @Test
    fun nextSubdivisionIndexIsNextBeatAfterLastSubdivision() {
        assertEquals(
            2,
            getNextSubdivisionIndex(
                tickStartInBeat = 24_000,
                framesWritten = 2_000,
                beatSize = 48_000,
                subdivisions = 2,
            ),
        )
    }

    // calculateFramesUntilNextTick

    @Test
    fun framesUntilNextTickIsSubdivisionPeriod() {
        assertEquals(
            12_000,
            calculateFramesUntilNextTick(TickPosition(), framesWritten = 2_000, beatSize = 48_000, subdivisions = 4),
        )
    }

    @Test
    fun framesUntilNextTickKeepsBeatGridWhenSubdivisionsReduced() {
        // Tick started as 2nd of 4 subdivisions; subdivisions changed to 1: next tick is the next beat, not 48 000
        // frames after this tick
        val position = TickPosition(beatInBar = 0, subdivisionIndex = 1, tickStartInBeat = 12_000)
        assertEquals(
            36_000,
            calculateFramesUntilNextTick(position, framesWritten = 2_000, beatSize = 48_000, subdivisions = 1),
        )
    }

    @Test
    fun framesUntilNextTickIsZeroWhenTempoIncreasedPastBeatEnd() {
        val position = TickPosition(beatInBar = 0, subdivisionIndex = 1, tickStartInBeat = 30_000)
        assertEquals(
            0,
            calculateFramesUntilNextTick(position, framesWritten = 2_000, beatSize = 24_000, subdivisions = 2),
        )
    }

    // getNextTickPosition

    @Test
    fun nextTickPositionWithinBeat() {
        assertEquals(
            TickPosition(beatInBar = 0, subdivisionIndex = 1, tickStartInBeat = 12_000),
            getNextTickPosition(TickPosition(), framesWritten = 12_000, beats = 4, beatSize = 48_000, subdivisions = 4),
        )
    }

    @Test
    fun nextTickPositionSkipsPassedSubdivisions() {
        assertEquals(
            TickPosition(beatInBar = 0, subdivisionIndex = 3, tickStartInBeat = 36_000),
            getNextTickPosition(TickPosition(), framesWritten = 36_000, beats = 4, beatSize = 48_000, subdivisions = 4),
        )
    }

    @Test
    fun nextTickPositionStartsNextBeat() {
        val position = TickPosition(beatInBar = 1, subdivisionIndex = 1, tickStartInBeat = 24_000)
        assertEquals(
            TickPosition(beatInBar = 2),
            getNextTickPosition(position, framesWritten = 24_000, beats = 4, beatSize = 48_000, subdivisions = 2),
        )
    }

    @Test
    fun nextTickPositionWrapsToFirstBeat() {
        assertEquals(
            TickPosition(beatInBar = 0),
            getNextTickPosition(
                TickPosition(beatInBar = 3),
                framesWritten = 48_000,
                beats = 4,
                beatSize = 48_000,
                subdivisions = 1,
            ),
        )
    }

    // getNextBeatInBar

    @Test
    fun nextBeatInBarAdvances() {
        assertEquals(2, getNextBeatInBar(beatInBar = 1, beats = 4))
    }

    @Test
    fun nextBeatInBarWrapsAfterLastBeat() {
        assertEquals(0, getNextBeatInBar(beatInBar = 3, beats = 4))
    }

    @Test
    fun nextBeatInBarIsFirstBeatWhenBeatsReducedBelowCurrentBeat() {
        assertEquals(0, getNextBeatInBar(beatInBar = 5, beats = 3))
    }

    // Simulated playback (regression tests for changing settings while playing)

    @Test
    fun beatGridIsKeptWhenSubdivisionsIncreasedMidBeat() {
        val ticks =
            simulate(ticks = 30) { frame ->
                Settings(beats = 4, tempo = 60, subdivisions = if (frame < 2 * 48_000 + 30_000) 1 else 4)
            }

        assertBeatsOnGrid(ticks, beatSize = 48_000)
        // Index 3 of the changed beat is the first subdivision after 30 000 frames
        assertTrue(ticks.any { it.frame == 2 * 48_000 + 36_000L && it.position.subdivisionIndex == 3 })
    }

    @Test
    fun beatGridIsKeptWhenSubdivisionsReducedMidBeat() {
        val ticks =
            simulate(ticks = 30) { frame ->
                Settings(beats = 4, tempo = 60, subdivisions = if (frame < 2 * 48_000 + 13_000) 4 else 1)
            }

        assertBeatsOnGrid(ticks, beatSize = 48_000)
    }

    @Test
    fun downbeatIsKeptWhenSubdivisionsChangedOnLastBeat() {
        val ticks =
            simulate(ticks = 10) { frame ->
                Settings(beats = 4, tempo = 60, subdivisions = if (frame < 3 * 48_000 + 1_000) 1 else 4)
            }

        val tickAfterBar = ticks.single { it.frame == 4 * 48_000L }
        assertEquals(TickType.STRONG, getCurrentTickType(tickAfterBar.position, emphasizeFirstBeat = true))
        assertEquals(1, getCurrentBeat(tickAfterBar.position))
    }

    @Test
    fun subdivisionsDoNotDriftWhenBeatSizeNotDivisible() {
        val beatSize = calculateBeatSize(tempo = 77)
        assertTrue(beatSize % 3 != 0)

        val ticks = simulate(ticks = 300) { Settings(beats = 4, tempo = 77, subdivisions = 3) }

        assertBeatsOnGrid(ticks, beatSize = beatSize)
    }

    @Test
    fun barRestartsWhenBeatsReducedBelowCurrentBeat() {
        // Beats reduced from 4 to 3 while on beat 4: the next beat is beat 1
        val ticks =
            simulate(ticks = 8) { frame ->
                Settings(beats = if (frame < 3 * 48_000 + 1_000) 4 else 3, tempo = 60, subdivisions = 1)
            }

        assertEquals(listOf(1, 2, 3, 4, 1, 2, 3, 1), ticks.map { getCurrentBeat(it.position) })
    }

    @Test
    fun barContinuesWhenBeatsReducedAboveCurrentBeat() {
        // Beats reduced from 4 to 3 while on beat 2: the bar continues with beat 3
        val ticks =
            simulate(ticks = 6) { frame ->
                Settings(beats = if (frame < 48_000 + 1_000) 4 else 3, tempo = 60, subdivisions = 1)
            }

        assertEquals(listOf(1, 2, 3, 1, 2, 3), ticks.map { getCurrentBeat(it.position) })
    }

    private data class Settings(val beats: Int, val tempo: Int, val subdivisions: Int)

    private data class SimulatedTick(val frame: Long, val position: TickPosition)

    /**
     * Mirrors the write loop of [Metronome]: writes a tick sound of [tickSoundSize] frames, then silence in chunks of
     * [chunkSize] frames, re-reading the settings (as returned by [settingsAt] for the current absolute frame) before
     * every chunk.
     */
    private fun simulate(
        ticks: Int,
        tickSoundSize: Int = 2_000,
        chunkSize: Int = 8_000,
        settingsAt: (Long) -> Settings,
    ): List<SimulatedTick> {
        val result = mutableListOf<SimulatedTick>()
        var position = TickPosition()
        var frame = 0L

        repeat(ticks) {
            result += SimulatedTick(frame, position)

            val tickSettings = settingsAt(frame)
            val tickBeatSize = calculateBeatSize(tickSettings.tempo)
            var written =
                minOf(
                    tickSoundSize,
                    calculateFramesUntilNextTick(position, 0, tickBeatSize, tickSettings.subdivisions),
                )

            while (true) {
                val settings = settingsAt(frame + written)
                val beatSize = calculateBeatSize(settings.tempo)
                val framesUntilNextTick =
                    calculateFramesUntilNextTick(position, written, beatSize, settings.subdivisions)
                if (written >= framesUntilNextTick) {
                    position = getNextTickPosition(position, written, settings.beats, beatSize, settings.subdivisions)
                    break
                }
                written += minOf(chunkSize, framesUntilNextTick - written)
            }

            frame += written
        }

        return result
    }

    private fun assertBeatsOnGrid(ticks: List<SimulatedTick>, beatSize: Int) {
        val beatStarts = ticks.filter { it.position.subdivisionIndex == 0 }
        beatStarts.forEachIndexed { index, tick ->
            assertEquals("Beat $index starts off the beat grid", index.toLong() * beatSize, tick.frame)
            assertEquals("Beat $index has the wrong position in the bar", index % 4, tick.position.beatInBar)
        }
    }
}
