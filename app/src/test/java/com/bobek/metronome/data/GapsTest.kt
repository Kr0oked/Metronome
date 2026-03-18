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
import org.junit.Assert.assertTrue
import org.junit.Test

class GapsTest {

    @Test
    fun emptyGapsIsValid() {
        assertTrue(Gaps().value.isEmpty())
    }

    @Test
    fun validGapIndicesAreAccepted() {
        val gaps = Gaps(sortedSetOf(1, 4, 8))
        assertEquals(sortedSetOf(1, 4, 8), gaps.value)
    }

    @Test
    fun minValidGapIndex() {
        Gaps(sortedSetOf(Beats.MIN_VALUE))
    }

    @Test
    fun maxValidGapIndex() {
        Gaps(sortedSetOf(Beats.MAX_VALUE))
    }

    @Test
    fun gapBelowMinThrows() {
        assertThrows(IllegalArgumentException::class.java) { Gaps(sortedSetOf(Beats.MIN_VALUE - 1)) }
    }

    @Test
    fun gapAboveMaxThrows() {
        assertThrows(IllegalArgumentException::class.java) { Gaps(sortedSetOf(Beats.MAX_VALUE + 1)) }
    }

    @Test
    fun zeroGapIndexThrows() {
        assertThrows(IllegalArgumentException::class.java) { Gaps(sortedSetOf(0)) }
    }

    @Test
    fun toggleAddsMissingGap() {
        val gaps = Gaps().toggle(3)
        assertEquals(sortedSetOf(3), gaps.value)
    }

    @Test
    fun toggleRemovesPresentGap() {
        val gaps = Gaps(sortedSetOf(3)).toggle(3)
        assertTrue(gaps.value.isEmpty())
    }

    @Test
    fun togglePreservesOtherGaps() {
        val gaps = Gaps(sortedSetOf(1, 3)).toggle(2)
        assertEquals(sortedSetOf(1, 2, 3), gaps.value)
    }

    @Test
    fun toggleIsIdempotentWhenAppliedTwice() {
        val original = Gaps(sortedSetOf(2))
        assertEquals(original, original.toggle(2).toggle(2))
    }

    @Test
    fun valueIsSortedAscending() {
        val gaps = Gaps(sortedSetOf(5, 1, 3))
        assertEquals(listOf(1, 3, 5), gaps.value.toList())
    }
}
