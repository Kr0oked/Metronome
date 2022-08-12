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

package com.bobek.metronome.data

import androidx.annotation.StringRes
import com.bobek.metronome.R

enum class TempoMarking(@StringRes val labelResourceId: Int) {
    LARGO(R.string.tempo_marking_largo),
    LARGHETTO(R.string.tempo_marking_larghetto),
    ADAGIO(R.string.tempo_marking_adagio),
    ANDANTE(R.string.tempo_marking_andante),
    MODERATO(R.string.tempo_marking_moderato),
    ALLEGRO(R.string.tempo_marking_allegro),
    PRESTO(R.string.tempo_marking_presto),
    PRESTISSIMO(R.string.tempo_marking_prestissimo);

    companion object {
        fun forTempo(tempo: Int): TempoMarking {
            return when (tempo) {
                in 0 until 60 -> LARGO
                in 60 until 66 -> LARGHETTO
                in 66 until 76 -> ADAGIO
                in 76 until 108 -> ANDANTE
                in 108 until 120 -> MODERATO
                in 120 until 168 -> ALLEGRO
                in 168 until 200 -> PRESTO
                else -> PRESTISSIMO
            }
        }
    }
}
