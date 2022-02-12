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

package com.bobek.metronome.view

import androidx.databinding.InverseMethod

class Tempo(value: Int = MIN) {

    val value = if (value in MIN..MAX) value else MIN

    companion object {
        const val MIN = 40
        const val MAX = 208

        @InverseMethod("stringToTempo")
        @JvmStatic
        fun tempoToString(tempo: Tempo): String = tempo.value.toString()

        @JvmStatic
        fun stringToTempo(string: String): Tempo {
            return try {
                Tempo(string.toInt())
            } catch (exception: NumberFormatException) {
                Tempo()
            }
        }

        @InverseMethod("floatToTempo")
        @JvmStatic
        fun tempoToFloat(tempo: Tempo): Float = tempo.value.toFloat()

        @JvmStatic
        fun floatToTempo(float: Float): Tempo = Tempo(float.toInt())
    }

    override fun toString(): String {
        return "tempo(value=$value)"
    }
}
