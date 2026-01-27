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

package com.bobek.metronome.view.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.bobek.metronome.data.AppNightMode
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Gaps
import com.bobek.metronome.data.Sound
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo
import com.bobek.metronome.data.Tick
import com.bobek.metronome.data.TickType
import com.bobek.metronome.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAP_WINDOW_MILLIS = 5_000L
private const val MILLIS_PER_MINUTE = 60_000L

@HiltViewModel
class MetronomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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
    val nightMode = settingsRepository.getNightMode().asLiveData()

    val playing = MutableLiveData(false)
    val connected = MutableLiveData(false)
    val currentTick = MutableLiveData<Tick?>(null)

    private val taps = ArrayDeque<Long>()

    init {
        viewModelScope.launch {
            settingsRepository.getBeats().collect { beatsData.value = it }
        }
        viewModelScope.launch {
            settingsRepository.getSubdivisions().collect { subdivisionsData.value = it }
        }
        viewModelScope.launch {
            settingsRepository.getGaps().collect { gapsData.value = it }
        }
        viewModelScope.launch {
            settingsRepository.getTempo().collect { tempoData.value = it }
        }
        viewModelScope.launch {
            settingsRepository.getEmphasizeFirstBeat().collect { emphasizeFirstBeat.value = it }
        }
        viewModelScope.launch {
            settingsRepository.getSound().collect { sound.value = it }
        }

        beatsData.observeForever {
            viewModelScope.launch { settingsRepository.setBeats(it) }
            if (beatsText.value != it.value.toString()) {
                beatsText.value = it.value.toString()
            }
        }
        subdivisionsData.observeForever {
            viewModelScope.launch { settingsRepository.setSubdivisions(it) }
            if (subdivisionsText.value != it.value.toString()) {
                subdivisionsText.value = it.value.toString()
            }
        }
        gapsData.observeForever {
            viewModelScope.launch { settingsRepository.setGaps(it) }
            updateGapLiveDatas(it)
        }
        tempoData.observeForever {
            viewModelScope.launch { settingsRepository.setTempo(it) }
            if (tempoText.value != it.value.toString()) {
                tempoText.value = it.value.toString()
            }
        }
        emphasizeFirstBeat.observeForever { viewModelScope.launch { settingsRepository.setEmphasizeFirstBeat(it) } }
        sound.observeForever { viewModelScope.launch { settingsRepository.setSound(it) } }

        gap1.observeForever { updateGaps(it, 1) }
        gap2.observeForever { updateGaps(it, 2) }
        gap3.observeForever { updateGaps(it, 3) }
        gap4.observeForever { updateGaps(it, 4) }
        gap5.observeForever { updateGaps(it, 5) }
        gap6.observeForever { updateGaps(it, 6) }
        gap7.observeForever { updateGaps(it, 7) }
        gap8.observeForever { updateGaps(it, 8) }
    }

    private fun updateGapLiveDatas(gaps: Gaps) {
        gap1.value = gaps.value.contains(1)
        gap2.value = gaps.value.contains(2)
        gap3.value = gaps.value.contains(3)
        gap4.value = gaps.value.contains(4)
        gap5.value = gaps.value.contains(5)
        gap6.value = gaps.value.contains(6)
        gap7.value = gaps.value.contains(7)
        gap8.value = gaps.value.contains(8)
    }

    private fun updateGaps(gap: Boolean, position: Int) {
        val currentGaps = gapsData.value ?: Gaps()
        val gaps = if (gap) currentGaps + position else currentGaps - position
        if (gapsData.value != gaps) {
            gapsData.value = gaps
        }
    }

    fun onBeatsTextChanged(text: String) {
        beatsText.value = text
        try {
            val value = text.toInt()
            if (value in Beats.MIN..Beats.MAX) {
                beatsData.value = Beats(value)
                beatsTextError.value = false
            } else {
                beatsTextError.value = true
            }
        } catch (e: NumberFormatException) {
            beatsTextError.value = true
        }
    }

    fun onSubdivisionsTextChanged(text: String) {
        subdivisionsText.value = text
        try {
            val value = text.toInt()
            if (value in Subdivisions.MIN..Subdivisions.MAX) {
                subdivisionsData.value = Subdivisions(value)
                subdivisionsTextError.value = false
            } else {
                subdivisionsTextError.value = true
            }
        } catch (e: NumberFormatException) {
            subdivisionsTextError.value = true
        }
    }

    fun onTempoTextChanged(text: String) {
        tempoText.value = text
        try {
            val value = text.toInt()
            if (value in Tempo.MIN..Tempo.MAX) {
                tempoData.value = Tempo(value)
                tempoTextError.value = false
            } else {
                tempoTextError.value = true
            }
        } catch (e: NumberFormatException) {
            tempoTextError.value = true
        }
    }

    fun startStop() {
        playing.value = playing.value?.not()
    }

    fun onTickReceived(tick: Tick) {
        if (tick.type == TickType.STRONG || tick.type == TickType.WEAK) {
            currentTick.value = tick
        }
    }

    fun tapTempo() {
        val currentTimeMillis = System.currentTimeMillis()
        pruneOldTaps(currentTimeMillis)
        taps.add(currentTimeMillis)

        val averageIntervalMillis = averageTapIntervalInMillis() ?: return
        val tempoValue = (MILLIS_PER_MINUTE / averageIntervalMillis).toInt()

        tempoData.value = when {
            tempoValue > Tempo.MAX -> Tempo(Tempo.MAX)
            tempoValue < Tempo.MIN -> Tempo(Tempo.MIN)
            else -> Tempo(tempoValue)
        }
    }

    private fun pruneOldTaps(currentTimeMillis: Long) {
        taps.removeAll { currentTimeMillis - it > TAP_WINDOW_MILLIS }
    }

    private fun averageTapIntervalInMillis(): Int? = taps
        .zipWithNext { a, b -> b - a }
        .average()
        .toInt()
        .takeIf { it > 0 }

    fun setNightMode(nightMode: AppNightMode) {
        viewModelScope.launch {
            settingsRepository.setNightMode(nightMode)
        }
    }
}
