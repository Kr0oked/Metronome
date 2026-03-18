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

class SubdivisionsTest {

    @Test
    fun defaultValue() {
        assertEquals(Subdivisions.DEFAULT_VALUE, Subdivisions().value)
    }

    @Test
    fun minValue() {
        assertEquals(Subdivisions.MIN_VALUE, Subdivisions(Subdivisions.MIN_VALUE).value)
    }

    @Test
    fun maxValue() {
        assertEquals(Subdivisions.MAX_VALUE, Subdivisions(Subdivisions.MAX_VALUE).value)
    }

    @Test
    fun belowMinThrows() {
        assertThrows(IllegalArgumentException::class.java) { Subdivisions(Subdivisions.MIN_VALUE - 1) }
    }

    @Test
    fun aboveMaxThrows() {
        assertThrows(IllegalArgumentException::class.java) { Subdivisions(Subdivisions.MAX_VALUE + 1) }
    }

    @Test
    fun zeroThrows() {
        assertThrows(IllegalArgumentException::class.java) { Subdivisions(0) }
    }
}
