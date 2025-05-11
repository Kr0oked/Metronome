/*
 * This file is part of Metronome.
 * Copyright (C) 2025 Philipp Bobek <philipp.bobek@mailbox.org>
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
import com.bobek.metronome.data.Gaps
import com.bobek.metronome.data.Sound
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

    val gapsData = MutableLiveData(Gaps())
    val gap1 = MutableLiveData(false)
    val gap2 = MutableLiveData(false)
    val gap3 = MutableLiveData(false)
    val gap4 = MutableLiveData(false)
    val gap5 = MutableLiveData(false)
    val gap6 = MutableLiveData(false)
    val gap7 = MutableLiveData(false)
    val gap8 = MutableLiveData(false)

    val tempoData = MutableLiveData(Tempo())
    val tempoText = MutableLiveData("")
    val tempoTextError = MutableLiveData(false)

    val emphasizeFirstBeat = MutableLiveData(true)
    val sound = MutableLiveData(Sound.SQUARE_WAVE)
    val playing = MutableLiveData(false)
    val connected = MutableLiveData(false)

    private val beatsDataObserver = getBeatsDataObserver()
    private val beatsTextObserver = getBeatsTextObserver()

    private val subdivisionsDataObserver = getSubdivisionsDataObserver()
    private val subdivisionsTextObserver = getSubdivisionsTextObserver()

    private val gapsDataObserver = getGapsDataObserver()
    private val gap1Observer = getGap1Observer()
    private val gap2Observer = getGap2Observer()
    private val gap3Observer = getGap3Observer()
    private val gap4Observer = getGap4Observer()
    private val gap5Observer = getGap5Observer()
    private val gap6Observer = getGap6Observer()
    private val gap7Observer = getGap7Observer()
    private val gap8Observer = getGap8Observer()

    private val tempoDataObserver = getTempoDataObserver()
    private val tempoTextObserver = getTempoTextObserver()

    private val emphasizeFirstBeatObserver = getEmphasizeFirstBeatObserver()
    private val soundObserver = getSoundObserver()
    private val playingObserver = getPlayingObserver()
    private val connectedObserver = getConnectedObserver()

    init {
        beatsData.observeForever(beatsDataObserver)
        beatsText.observeForever(beatsTextObserver)

        subdivisionsData.observeForever(subdivisionsDataObserver)
        subdivisionsText.observeForever(subdivisionsTextObserver)

        gapsData.observeForever(gapsDataObserver)
        gap1.observeForever(gap1Observer)
        gap2.observeForever(gap2Observer)
        gap3.observeForever(gap3Observer)
        gap4.observeForever(gap4Observer)
        gap5.observeForever(gap5Observer)
        gap6.observeForever(gap6Observer)
        gap7.observeForever(gap7Observer)
        gap8.observeForever(gap8Observer)

        tempoData.observeForever(tempoDataObserver)
        tempoText.observeForever(tempoTextObserver)

        emphasizeFirstBeat.observeForever(emphasizeFirstBeatObserver)
        sound.observeForever(soundObserver)
        playing.observeForever(playingObserver)
        connected.observeForever(connectedObserver)
    }

    fun startStop() {
        playing.value = playing.value?.not()
    }

    override fun onCleared() {
        beatsData.removeObserver(beatsDataObserver)
        beatsText.removeObserver(beatsTextObserver)

        subdivisionsData.removeObserver(subdivisionsDataObserver)
        subdivisionsText.removeObserver(subdivisionsTextObserver)

        gapsData.removeObserver(gapsDataObserver)
        gap1.removeObserver(gap1Observer)
        gap2.removeObserver(gap2Observer)
        gap3.removeObserver(gap3Observer)
        gap4.removeObserver(gap4Observer)
        gap5.removeObserver(gap5Observer)
        gap6.removeObserver(gap6Observer)
        gap7.removeObserver(gap7Observer)
        gap8.removeObserver(gap8Observer)

        tempoData.removeObserver(tempoDataObserver)
        tempoText.removeObserver(tempoTextObserver)

        emphasizeFirstBeat.removeObserver(emphasizeFirstBeatObserver)
        sound.removeObserver(soundObserver)
        playing.removeObserver(playingObserver)
        connected.removeObserver(connectedObserver)
    }

    override fun toString(): String {
        return "MetronomeViewModel(" +
                "beats=${beatsData.value?.value}, " +
                "subdivisions=${subdivisionsData.value?.value}, " +
                "gaps=${gapsData.value?.value?.joinToString()}, " +
                "tempo=${tempoData.value?.value}, " +
                "emphasizeFirstBeat=${emphasizeFirstBeat.value}," +
                "sound=${sound.value}," +
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
        } catch (_: NumberFormatException) {
            beatsTextError.value = true
        } catch (_: IllegalArgumentException) {
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
        } catch (_: NumberFormatException) {
            subdivisionsTextError.value = true
        } catch (_: IllegalArgumentException) {
            subdivisionsTextError.value = true
        }
    }

    private fun getGapsDataObserver(): (t: Gaps) -> Unit = {
        gap1.value = it.value.contains(1)
        gap2.value = it.value.contains(2)
        gap3.value = it.value.contains(3)
        gap4.value = it.value.contains(4)
        gap5.value = it.value.contains(5)
        gap6.value = it.value.contains(6)
        gap7.value = it.value.contains(7)
        gap8.value = it.value.contains(8)
        Log.d(TAG, "gaps: $it")
    }

    private fun getGap1Observer(): (t: Boolean) -> Unit = { updateGaps(it, 1) }

    private fun getGap2Observer(): (t: Boolean) -> Unit = { updateGaps(it, 2) }

    private fun getGap3Observer(): (t: Boolean) -> Unit = { updateGaps(it, 3) }

    private fun getGap4Observer(): (t: Boolean) -> Unit = { updateGaps(it, 4) }

    private fun getGap5Observer(): (t: Boolean) -> Unit = { updateGaps(it, 5) }

    private fun getGap6Observer(): (t: Boolean) -> Unit = { updateGaps(it, 6) }

    private fun getGap7Observer(): (t: Boolean) -> Unit = { updateGaps(it, 7) }

    private fun getGap8Observer(): (t: Boolean) -> Unit = { updateGaps(it, 8) }

    private fun updateGaps(gap: Boolean, position: Int) {
        val currentGaps = gapsData.value ?: Gaps()

        val gaps = when (gap) {
            true -> currentGaps + position
            false -> currentGaps - position
        }

        if (gapsData.value != gaps) {
            gapsData.value = gaps
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
        } catch (_: NumberFormatException) {
            tempoTextError.value = true
        } catch (_: IllegalArgumentException) {
            tempoTextError.value = true
        }
    }

    private fun getEmphasizeFirstBeatObserver(): (t: Boolean) -> Unit = {
        Log.d(TAG, "emphasizeFirstBeat: $it")
    }

    private fun getSoundObserver(): (t: Sound) -> Unit = {
        Log.d(TAG, "sound: $it")
    }

    private fun getPlayingObserver(): (t: Boolean) -> Unit = {
        Log.d(TAG, "playing: $it")
    }

    private fun getConnectedObserver(): (t: Boolean) -> Unit = {
        Log.d(TAG, "connected: $it")
    }
}
