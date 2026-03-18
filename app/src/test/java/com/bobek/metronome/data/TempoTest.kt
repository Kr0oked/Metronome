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

package com.bobek.metronome.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TempoTest {

    @Test
    fun defaultValue() {
        assertEquals(Tempo.DEFAULT_VALUE, Tempo().value)
    }

    @Test
    fun minValue() {
        assertEquals(Tempo.MIN_VALUE, Tempo(Tempo.MIN_VALUE).value)
    }

    @Test
    fun maxValue() {
        assertEquals(Tempo.MAX_VALUE, Tempo(Tempo.MAX_VALUE).value)
    }

    @Test
    fun belowMinThrows() {
        assertThrows(IllegalArgumentException::class.java) { Tempo(Tempo.MIN_VALUE - 1) }
    }

    @Test
    fun aboveMaxThrows() {
        assertThrows(IllegalArgumentException::class.java) { Tempo(Tempo.MAX_VALUE + 1) }
    }

    @Test
    fun markingForDefaultTempo() {
        assertEquals(TempoMarking.ANDANTE, Tempo().marking)
    }

    @Test
    fun markingForMinTempo() {
        assertEquals(TempoMarking.LARGO, Tempo(Tempo.MIN_VALUE).marking)
    }

    @Test
    fun markingForMaxTempo() {
        assertEquals(TempoMarking.PRESTISSIMO, Tempo(Tempo.MAX_VALUE).marking)
    }
}
