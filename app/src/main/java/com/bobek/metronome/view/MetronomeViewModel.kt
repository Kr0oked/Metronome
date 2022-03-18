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

import androidx.lifecycle.MutableLiveData

class MetronomeViewModel {

    val beatsData = MutableLiveData(Beats())

    val beatsText = MutableLiveData<String>()

    val beatsTextError = MutableLiveData<Boolean>()

    val subdivisionsData = MutableLiveData(Subdivisions())

    val subdivisionsText = MutableLiveData<String>()

    val subdivisionsTextError = MutableLiveData<Boolean>()

    val tempoData = MutableLiveData(Tempo())

    val tempoText = MutableLiveData<String>()

    val tempoTextError = MutableLiveData<Boolean>()

    init {
        beatsData.observeForever { beats -> beatsText.value = beats.value.toString() }
        beatsText.observeForever(this::processBeatsText)

        subdivisionsData.observeForever { subdivisions -> subdivisionsText.value = subdivisions.value.toString() }
        subdivisionsText.observeForever(this::processSubdivisionsText)

        tempoData.observeForever { tempo -> tempoText.value = tempo.value.toString() }
        tempoText.observeForever(this::processTempoText)
    }

    private fun processBeatsText(text: String) {
        try {
            val numericValue = text.toInt()
            val beats = Beats(numericValue)

            if (beatsData.value != beats) {
                beatsData.value = beats
            }

            beatsTextError.value = false
        } catch (exception: NumberFormatException) {
            beatsTextError.value = true
        } catch (exception: IllegalArgumentException) {
            beatsTextError.value = true
        }
    }

    private fun processSubdivisionsText(text: String) {
        try {
            val numericValue = text.toInt()
            val subdivisions = Subdivisions(numericValue)

            if (subdivisionsData.value != subdivisions) {
                subdivisionsData.value = subdivisions
            }

            subdivisionsTextError.value = false
        } catch (exception: NumberFormatException) {
            subdivisionsTextError.value = true
        } catch (exception: IllegalArgumentException) {
            subdivisionsTextError.value = true
        }
    }

    private fun processTempoText(text: String) {
        try {
            val numericValue = text.toInt()
            val tempo = Tempo(numericValue)

            if (tempoData.value != tempo) {
                tempoData.value = tempo
            }

            tempoTextError.value = false
        } catch (exception: NumberFormatException) {
            tempoTextError.value = true
        } catch (exception: IllegalArgumentException) {
            tempoTextError.value = true
        }
    }
}
