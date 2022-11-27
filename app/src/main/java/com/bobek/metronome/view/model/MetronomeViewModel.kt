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

package com.bobek.metronome.view.model

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo

private const val TAG = "MetronomeViewModel"

class MetronomeViewModel : ViewModel() {

    val beatsData = MutableLiveData(Beats())
    val beatsText = MutableLiveData("")
    val beatsTextError = MutableLiveData(false)

    val subdivisionsData = MutableLiveData(Subdivisions())
    val subdivisionsText = MutableLiveData("")
    val subdivisionsTextError = MutableLiveData(false)

    val tempoData = MutableLiveData(Tempo())
    val tempoText = MutableLiveData("")
    val tempoTextError = MutableLiveData(false)

    val emphasizeFirstBeat = MutableLiveData(true)
    val playing = MutableLiveData(false)
    val connected = MutableLiveData(false)

    private val beatsDataObserver = getBeatsDataObserver()
    private val beatsTextObserver = getBeatsTextObserver()

    private val subdivisionsDataObserver = getSubdivisionsDataObserver()
    private val subdivisionsTextObserver = getSubdivisionsTextObserver()

    private val tempoDataObserver = getTempoDataObserver()
    private val tempoTextObserver = getTempoTextObserver()

    private val emphasizeFirstBeatObserver = getEmphasizeFirstBeatObserver()
    private val playingObserver = getPlayingObserver()
    private val connectedObserver = getConnectedObserver()

    init {
        beatsData.observeForever(beatsDataObserver)
        beatsText.observeForever(beatsTextObserver)

        subdivisionsData.observeForever(subdivisionsDataObserver)
        subdivisionsText.observeForever(subdivisionsTextObserver)

        tempoData.observeForever(tempoDataObserver)
        tempoText.observeForever(tempoTextObserver)

        emphasizeFirstBeat.observeForever(emphasizeFirstBeatObserver)
        playing.observeForever(playingObserver)
        connected.observeForever(connectedObserver)
    }

    fun startStop() {
        playing.value?.let { playing.value = it.not() }
    }

    override fun onCleared() {
        beatsData.removeObserver(beatsDataObserver)
        beatsText.removeObserver(beatsTextObserver)

        subdivisionsData.removeObserver(subdivisionsDataObserver)
        subdivisionsText.removeObserver(subdivisionsTextObserver)

        tempoData.removeObserver(tempoDataObserver)
        tempoText.removeObserver(tempoTextObserver)

        emphasizeFirstBeat.removeObserver(emphasizeFirstBeatObserver)
        playing.removeObserver(playingObserver)
        connected.removeObserver(connectedObserver)
    }

    override fun toString(): String {
        return "MetronomeViewModel(" +
                "beats=${beatsData.value?.value}, " +
                "subdivisions=${subdivisionsData.value?.value}, " +
                "tempo=${tempoData.value?.value}, " +
                "emphasizeFirstBeat=${emphasizeFirstBeat.value}," +
                "playing=${playing.value}, " +
                "connected=${connected.value}" +
                ")"
    }

    private fun getBeatsDataObserver(): (t: Beats) -> Unit = {
        beatsText.value = it.value.toString()
        Log.d(TAG, "beats: $it")
    }

    private fun getBeatsTextObserver(): (t: String) -> Unit = {
        try {
            val numericValue = it.toInt()
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

    private fun getSubdivisionsDataObserver(): (t: Subdivisions) -> Unit = {
        subdivisionsText.value = it.value.toString()
        Log.d(TAG, "subdivisions: $it")
    }

    private fun getSubdivisionsTextObserver(): (t: String) -> Unit = {
        try {
            val numericValue = it.toInt()
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

    private fun getTempoDataObserver(): (t: Tempo) -> Unit = {
        tempoText.value = it.value.toString()
        Log.d(TAG, "tempo: $it")
    }

    private fun getTempoTextObserver(): (t: String) -> Unit = {
        try {
            val numericValue = it.toInt()
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

    private fun getEmphasizeFirstBeatObserver(): (t: Boolean) -> Unit = {
        Log.d(TAG, "emphasizeFirstBeat: $it")
    }

    private fun getPlayingObserver(): (t: Boolean) -> Unit = {
        Log.d(TAG, "playing: $it")
    }

    private fun getConnectedObserver(): (t: Boolean) -> Unit = {
        Log.d(TAG, "connected: $it")
    }
}
