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

import com.bobek.metronome.concurrent.AdjustableTimer
import com.bobek.metronome.data.*
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MetronomeTest {

    private lateinit var metronome: Metronome
    private lateinit var tickListener: MetronomeTickListener
    private lateinit var timer: AdjustableTimer
    private lateinit var command: Runnable

    @Before
    fun setUp() {
        tickListener = mockk()
        justRun { tickListener.onTick(any()) }

        timer = mockk()
        justRun { timer.schedule(any()) }
        justRun { timer.stop() }

        metronome = Metronome(tickListener) {
            command = it
            timer
        }
    }

    @Test
    fun initialValues() {
        assertEquals(Beats(4), metronome.beats)
        assertEquals(Subdivisions(1), metronome.subdivisions)
        assertEquals(Tempo(80), metronome.tempo)
        assertFalse(metronome.playing)
    }

    @Test
    fun startWithInitialValues() {
        metronome.playing = true

        verify { timer.schedule(750) }
        confirmVerified(timer)
    }

    @Test
    fun startWithCustomValues() {
        metronome.beats = Beats(1)
        metronome.subdivisions = Subdivisions(2)
        metronome.tempo = Tempo(123)
        metronome.playing = true

        verify { timer.schedule(243) }
        confirmVerified(timer)
    }

    @Test
    fun startStop() {
        metronome.playing = true
        metronome.playing = false

        verifySequence {
            timer.schedule(any())
            timer.stop()
        }
        confirmVerified(timer)
    }

    @Test
    fun adjustBeatsAfterStart() {
        metronome.playing = true

        verify { timer.schedule(750) }
        confirmVerified(timer)

        metronome.beats = Beats(1)

        verify { timer.schedule(750) }
        confirmVerified(timer)
    }

    @Test
    fun adjustSubdivisionsAfterStart() {
        metronome.playing = true

        verify { timer.schedule(750) }
        confirmVerified(timer)

        metronome.subdivisions = Subdivisions(2)

        verify { timer.schedule(375) }
        confirmVerified(timer)
    }

    @Test
    fun adjustTempoAfterStart() {
        metronome.playing = true

        verify { timer.schedule(750) }
        confirmVerified(timer)

        metronome.tempo = Tempo(123)

        verify { timer.schedule(487) }
        confirmVerified(timer)
    }

    @Test
    fun playingReturnsTrue() {
        metronome.playing = true

        assertTrue(metronome.playing)
    }

    @Test
    fun playingReturnsFalse() {
        metronome.playing = true
        metronome.playing = false

        assertFalse(metronome.playing)
    }

    @Test
    fun ticks() {
        metronome.beats = Beats(2)
        metronome.subdivisions = Subdivisions(2)
        metronome.tempo = Tempo(123)

        command.run()
        verify { tickListener.onTick(Tick(1, TickType.STRONG)) }
        confirmVerified(timer)

        command.run()
        verify { tickListener.onTick(Tick(1, TickType.SUB)) }
        confirmVerified(timer)

        command.run()
        verify { tickListener.onTick(Tick(2, TickType.WEAK)) }
        confirmVerified(timer)

        command.run()
        verify { tickListener.onTick(Tick(2, TickType.SUB)) }
        confirmVerified(timer)

        command.run()
        verify { tickListener.onTick(Tick(1, TickType.STRONG)) }
        confirmVerified(timer)
    }

    @Test
    fun ticksFollowAdjustments() {
        metronome.beats = Beats(2)
        metronome.subdivisions = Subdivisions(2)
        metronome.tempo = Tempo(123)

        command.run()
        verify { tickListener.onTick(Tick(1, TickType.STRONG)) }
        confirmVerified(timer)

        metronome.subdivisions = Subdivisions(1)

        command.run()
        verify { tickListener.onTick(Tick(2, TickType.WEAK)) }
        confirmVerified(timer)
    }
}
