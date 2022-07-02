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

import androidx.databinding.InverseMethod

data class Tempo(val value: Int = DEFAULT) {

    init {
        require(value in MIN..MAX) { "value must be between $MIN and $MAX but was $value" }
    }

    companion object {
        const val MIN = 40
        const val MAX = 208
        const val DEFAULT = 80

        @InverseMethod("floatToTempo")
        @JvmStatic
        fun tempoToFloat(tempo: Tempo): Float = tempo.value.toFloat()

        @JvmStatic
        fun floatToTempo(float: Float): Tempo = Tempo(float.toInt())
    }
}
