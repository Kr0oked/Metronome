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
import org.junit.Test

class TempoMarkingTest {

    @Test
    fun largoAtLowerBoundary() {
        assertEquals(TempoMarking.LARGO, TempoMarking.forTempo(1))
    }

    @Test
    fun largoAtUpperBoundary() {
        assertEquals(TempoMarking.LARGO, TempoMarking.forTempo(59))
    }

    @Test
    fun larghettoAtLowerBoundary() {
        assertEquals(TempoMarking.LARGHETTO, TempoMarking.forTempo(60))
    }

    @Test
    fun larghettoAtUpperBoundary() {
        assertEquals(TempoMarking.LARGHETTO, TempoMarking.forTempo(65))
    }

    @Test
    fun adagioAtLowerBoundary() {
        assertEquals(TempoMarking.ADAGIO, TempoMarking.forTempo(66))
    }

    @Test
    fun adagioAtUpperBoundary() {
        assertEquals(TempoMarking.ADAGIO, TempoMarking.forTempo(75))
    }

    @Test
    fun andanteAtLowerBoundary() {
        assertEquals(TempoMarking.ANDANTE, TempoMarking.forTempo(76))
    }

    @Test
    fun andanteAtUpperBoundary() {
        assertEquals(TempoMarking.ANDANTE, TempoMarking.forTempo(107))
    }

    @Test
    fun moderatoAtLowerBoundary() {
        assertEquals(TempoMarking.MODERATO, TempoMarking.forTempo(108))
    }

    @Test
    fun moderatoAtUpperBoundary() {
        assertEquals(TempoMarking.MODERATO, TempoMarking.forTempo(119))
    }

    @Test
    fun allegroAtLowerBoundary() {
        assertEquals(TempoMarking.ALLEGRO, TempoMarking.forTempo(120))
    }

    @Test
    fun allegroAtUpperBoundary() {
        assertEquals(TempoMarking.ALLEGRO, TempoMarking.forTempo(167))
    }

    @Test
    fun prestoAtLowerBoundary() {
        assertEquals(TempoMarking.PRESTO, TempoMarking.forTempo(168))
    }

    @Test
    fun prestoAtUpperBoundary() {
        assertEquals(TempoMarking.PRESTO, TempoMarking.forTempo(199))
    }

    @Test
    fun prestissimoAtLowerBoundary() {
        assertEquals(TempoMarking.PRESTISSIMO, TempoMarking.forTempo(200))
    }

    @Test
    fun prestissimoAtMaxTempo() {
        assertEquals(TempoMarking.PRESTISSIMO, TempoMarking.forTempo(Tempo.MAX_VALUE))
    }
}
