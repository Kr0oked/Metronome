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

package com.bobek.metronome

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAP_WINDOW_MILLIS = 5_000L
private const val MILLIS_PER_MINUTE = 60_000L

interface IMetronomeViewModel {
    fun getBeats(): Flow<Beats>
    fun setBeats(beats: Beats)
    fun getSubdivisions(): Flow<Subdivisions>
    fun setSubdivisions(subdivisions: Subdivisions)
    fun getTempo(): Flow<Tempo>
    fun setTempo(tempo: Tempo)
    fun changeTempo(delta: Int)
    fun tapTempo()
    fun getGaps(): Flow<Gaps>
    fun setGaps(gaps: Gaps)
    fun getEmphasizeFirstBeat(): Flow<Boolean>
    fun setEmphasizeFirstBeat(emphasizeFirstBeat: Boolean)
    fun getSound(): Flow<Sound>
    fun setSound(sound: Sound)
    fun getNightMode(): Flow<AppNightMode>
    fun setNightMode(nightMode: AppNightMode)
    fun getPlaying(): Flow<Boolean>
    fun setPlaying(playing: Boolean)
    fun startStop()
    fun getConnected(): Flow<Boolean>
    fun setConnected(connected: Boolean)
    fun getCurrentTick(): Flow<Tick?>
    fun onTickReceived(tick: Tick)
    fun setMetronomeService(metronomeService: MetronomeService?)

}

@HiltViewModel
class MetronomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel(), IMetronomeViewModel {

    private val beatsData = MutableLiveData(Beats())
    private val subdivisionsData = MutableLiveData(Subdivisions())
    private val tempoData = MutableLiveData(Tempo())
    private val gapsData = MutableLiveData(Gaps())
    private val emphasizeFirstBeatData = MutableLiveData(true)
    private val soundData = MutableLiveData(Sound.SQUARE_WAVE)
    private val playingData = MutableLiveData(false)
    private val connectedData = MutableLiveData(false)
    private val currentTickData = MutableLiveData<Tick?>(null)
    private val taps = ArrayDeque<Long>()

    private var metronomeService: MetronomeService? = null

    override fun getBeats(): Flow<Beats> = beatsData.asFlow()

    override fun setBeats(beats: Beats) {
        beatsData.value = beats
    }

    override fun getSubdivisions(): Flow<Subdivisions> = subdivisionsData.asFlow()

    override fun setSubdivisions(subdivisions: Subdivisions) {
        subdivisionsData.value = subdivisions
    }

    override fun getTempo(): Flow<Tempo> = tempoData.asFlow()

    override fun setTempo(tempo: Tempo) {
        tempoData.value = tempo
    }

    override fun changeTempo(delta: Int) {
        val currentTempoValue = tempoData.value?.value ?: Tempo.DEFAULT
        val newTempoValue = currentTempoValue + delta

        tempoData.value = when {
            newTempoValue < Tempo.MIN -> Tempo(Tempo.MIN) // TODO Tempo.MIN and Tempo.MIN_VALUE
            newTempoValue > Tempo.MAX -> Tempo(Tempo.MAX)
            else -> Tempo(newTempoValue)
        }
    }

    override fun tapTempo() {
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

    override fun getGaps(): Flow<Gaps> = gapsData.asFlow()

    override fun setGaps(gaps: Gaps) {
        gapsData.value = gaps
    }

    override fun getEmphasizeFirstBeat() = emphasizeFirstBeatData.asFlow()

    override fun setEmphasizeFirstBeat(emphasizeFirstBeat: Boolean) {
        emphasizeFirstBeatData.value = emphasizeFirstBeat
    }

    override fun getSound(): Flow<Sound> = soundData.asFlow()

    override fun setSound(sound: Sound) {
        soundData.value = sound
    }

    override fun getNightMode(): Flow<AppNightMode> = settingsRepository.getNightMode()

    override fun setNightMode(nightMode: AppNightMode) {
        viewModelScope.launch {
            settingsRepository.setNightMode(nightMode)
        }
    }

    override fun getPlaying(): Flow<Boolean> = playingData.asFlow()

    override fun setPlaying(playing: Boolean) {
        playingData.value = playing
    }

    override fun startStop() {
        playingData.value = playingData.value?.not()
    }

    override fun getConnected(): Flow<Boolean> = connectedData.asFlow()

    override fun setConnected(connected: Boolean) {
        connectedData.value = connected
    }

    override fun getCurrentTick(): Flow<Tick?> = currentTickData.asFlow()

    override fun onTickReceived(tick: Tick) {
        if (tick.type == TickType.STRONG || tick.type == TickType.WEAK) {
            currentTickData.value = tick
        }
    }

    override fun setMetronomeService(metronomeService: MetronomeService?) {
        this.metronomeService = metronomeService
    }
}

class ComposeMetronomeViewModel(
    val connected: Boolean = true
) : IMetronomeViewModel {
    override fun getBeats(): Flow<Beats> = emptyFlow()
    override fun setBeats(beats: Beats) = Unit
    override fun getSubdivisions(): Flow<Subdivisions> = emptyFlow()
    override fun setSubdivisions(subdivisions: Subdivisions) = Unit
    override fun getTempo(): Flow<Tempo> = emptyFlow()
    override fun setTempo(tempo: Tempo) = Unit
    override fun changeTempo(delta: Int) = Unit
    override fun tapTempo() = Unit
    override fun getGaps(): Flow<Gaps> = emptyFlow()
    override fun setGaps(gaps: Gaps) = Unit
    override fun getEmphasizeFirstBeat(): Flow<Boolean> = emptyFlow()
    override fun setEmphasizeFirstBeat(emphasizeFirstBeat: Boolean) = Unit
    override fun getSound(): Flow<Sound> = emptyFlow()
    override fun setSound(sound: Sound) = Unit
    override fun getNightMode(): Flow<AppNightMode> = emptyFlow()
    override fun setNightMode(nightMode: AppNightMode) = Unit
    override fun getPlaying(): Flow<Boolean> = emptyFlow()
    override fun setPlaying(playing: Boolean) = Unit
    override fun startStop() = Unit
    override fun getConnected(): Flow<Boolean> = flowOf(connected)
    override fun setConnected(connected: Boolean) = Unit
    override fun getCurrentTick(): Flow<Tick?> = emptyFlow()
    override fun onTickReceived(tick: Tick) = Unit
    override fun setMetronomeService(metronomeService: MetronomeService?) = Unit
}
