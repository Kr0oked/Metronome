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

data class Subdivisions(val value: Int = DEFAULT_VALUE) {

    init {
        require(value in valueRange) { "value must be between $MIN_VALUE and $MAX_VALUE but was $value" }
    }

    companion object {
        const val MIN_VALUE = 1
        const val MAX_VALUE = 4
        const val DEFAULT_VALUE = 1

        val valueRange = MIN_VALUE..MAX_VALUE
    }
}
