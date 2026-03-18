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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TickTest {

    @Test
    fun validTickStoresAllFields() {
        val tick = Tick(beat = 1, type = TickType.STRONG, gap = false)
        assertEquals(1, tick.beat)
        assertEquals(TickType.STRONG, tick.type)
        assertFalse(tick.gap)
    }

    @Test
    fun gapTickIsStored() {
        val tick = Tick(beat = 3, type = TickType.WEAK, gap = true)
        assertTrue(tick.gap)
    }

    @Test
    fun beatAtMinIsValid() {
        Tick(beat = Beats.MIN_VALUE, type = TickType.WEAK, gap = false)
    }

    @Test
    fun beatAtMaxIsValid() {
        Tick(beat = Beats.MAX_VALUE, type = TickType.SUB, gap = false)
    }

    @Test
    fun beatBelowMinThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            Tick(beat = Beats.MIN_VALUE - 1, type = TickType.WEAK, gap = false)
        }
    }

    @Test
    fun beatAboveMaxThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            Tick(beat = Beats.MAX_VALUE + 1, type = TickType.WEAK, gap = false)
        }
    }

    @Test
    fun zeroBeatThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            Tick(beat = 0, type = TickType.STRONG, gap = false)
        }
    }

    @Test
    fun allTickTypesAreAccepted() {
        for (type in TickType.entries) {
            Tick(beat = 1, type = type, gap = false)
        }
    }
}
