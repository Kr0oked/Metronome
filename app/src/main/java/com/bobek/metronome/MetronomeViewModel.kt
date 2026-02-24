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

import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

private const val TAP_WINDOW_MILLIS = 5_000L
private const val MILLIS_PER_MINUTE = 60_000L
private const val SETTINGS_DEBOUNCE_MILLIS = 1_000L

interface IMetronomeViewModel {
    fun getBeats(): StateFlow<Beats>
    fun setBeats(beats: Beats)
    fun getSubdivisions(): StateFlow<Subdivisions>
    fun setSubdivisions(subdivisions: Subdivisions)
    fun getGaps(): StateFlow<Gaps>
    fun setGaps(gaps: Gaps)
    fun getTempo(): StateFlow<Tempo>
    fun setTempo(tempo: Tempo)
    fun changeTempo(delta: Int)
    fun tapTempo()
    fun getEmphasizeFirstBeat(): StateFlow<Boolean>
    fun setEmphasizeFirstBeat(emphasizeFirstBeat: Boolean)
    fun getSound(): StateFlow<Sound>
    fun setSound(sound: Sound)
    fun getNightMode(): StateFlow<AppNightMode>
    fun setNightMode(nightMode: AppNightMode)
    fun getPlaying(): StateFlow<Boolean>
    fun setPlaying(playing: Boolean)
    fun startStop()
    fun getConnected(): StateFlow<Boolean>
    fun getCurrentTick(): StateFlow<Tick?>
    fun onTickReceived(tick: Tick)
    fun setMetronomeService(metronomeService: MetronomeService?)
}

@OptIn(FlowPreview::class)
@HiltViewModel
class MetronomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel(), IMetronomeViewModel {

    private val beats = MutableStateFlow(Beats())
    private val subdivisions = MutableStateFlow(Subdivisions())
    private val gaps = MutableStateFlow(Gaps())
    private val tempo = MutableStateFlow(Tempo())
    private val emphasizeFirstBeat = MutableStateFlow(true)
    private val sound = MutableStateFlow(Sound.SQUARE_WAVE)
    private val nightMode = MutableStateFlow(AppNightMode.FOLLOW_SYSTEM)
    private val playing = MutableStateFlow(false)
    private val connected = MutableStateFlow(false)
    private val currentTick = MutableStateFlow<Tick?>(null)
    private val taps = ArrayDeque<Long>()

    private var metronomeService: MetronomeService? = null

    init {
        runBlocking {
            initFromSettings()
        }

        setupFlowsToMetronomeService()
        setupFlowsToSettings()
    }

    private suspend fun initFromSettings(): Unit? {
        settingsRepository.getBeats().firstOrNull()?.let { beats.value = it }
        settingsRepository.getSubdivisions().firstOrNull()?.let { subdivisions.value = it }
        settingsRepository.getGaps().firstOrNull()?.let { gaps.value = it }
        settingsRepository.getTempo().firstOrNull()?.let { tempo.value = it }
        settingsRepository.getEmphasizeFirstBeat().firstOrNull()?.let { emphasizeFirstBeat.value = it }
        return settingsRepository.getSound().firstOrNull()?.let { sound.value = it }
    }

    private fun setupFlowsToMetronomeService() {
        viewModelScope.launch {
            beats.collect { metronomeService?.beats = it }
        }
        viewModelScope.launch {
            subdivisions.collect { metronomeService?.subdivisions = it }
        }
        viewModelScope.launch {
            gaps.collect { metronomeService?.gaps = it }
        }
        viewModelScope.launch {
            tempo.collect { metronomeService?.tempo = it }
        }
        viewModelScope.launch {
            emphasizeFirstBeat.collect { metronomeService?.emphasizeFirstBeat = it }
        }
        viewModelScope.launch {
            sound.collect { metronomeService?.sound = it }
        }
        viewModelScope.launch {
            playing.collect { metronomeService?.playing = it }
        }
    }

    private fun setupFlowsToSettings() {
        viewModelScope.launch {
            beats.debounce(SETTINGS_DEBOUNCE_MILLIS)
                .collect { settingsRepository.setBeats(it) }
        }
        viewModelScope.launch {
            subdivisions.debounce(SETTINGS_DEBOUNCE_MILLIS)
                .collect { settingsRepository.setSubdivisions(it) }
        }
        viewModelScope.launch {
            gaps.debounce(SETTINGS_DEBOUNCE_MILLIS)
                .collect { settingsRepository.setGaps(it) }
        }
        viewModelScope.launch {
            tempo.debounce(SETTINGS_DEBOUNCE_MILLIS)
                .collect { settingsRepository.setTempo(it) }
        }
        viewModelScope.launch {
            emphasizeFirstBeat.debounce(SETTINGS_DEBOUNCE_MILLIS)
                .collect { settingsRepository.setEmphasizeFirstBeat(it) }
        }
        viewModelScope.launch {
            sound.debounce(SETTINGS_DEBOUNCE_MILLIS)
                .collect { settingsRepository.setSound(it) }
        }
        viewModelScope.launch {
            nightMode.debounce(SETTINGS_DEBOUNCE_MILLIS)
                .collect { settingsRepository.setNightMode(it) }
        }
    }

    override fun getBeats(): StateFlow<Beats> = beats

    override fun setBeats(beats: Beats) {
        this.beats.value = beats
    }

    override fun getSubdivisions(): StateFlow<Subdivisions> = subdivisions

    override fun setSubdivisions(subdivisions: Subdivisions) {
        this.subdivisions.value = subdivisions
    }

    override fun getGaps(): StateFlow<Gaps> = gaps

    override fun setGaps(gaps: Gaps) {
        this.gaps.value = gaps
    }

    override fun getTempo(): StateFlow<Tempo> = tempo

    override fun setTempo(tempo: Tempo) {
        this.tempo.value = tempo
    }

    override fun changeTempo(delta: Int) {
        val tempoValue = tempo.value.value + delta

        tempo.value = when {
            tempoValue < Tempo.MIN -> Tempo(Tempo.MIN)
            tempoValue > Tempo.MAX -> Tempo(Tempo.MAX)
            else -> Tempo(tempoValue)
        }
    }

    override fun tapTempo() {
        val currentTimeMillis = System.currentTimeMillis()
        pruneOldTaps(currentTimeMillis)
        taps.add(currentTimeMillis)

        val averageIntervalMillis = averageTapIntervalInMillis() ?: return
        val tempoValue = (MILLIS_PER_MINUTE / averageIntervalMillis).toInt()

        tempo.value = when {
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

    override fun getEmphasizeFirstBeat(): StateFlow<Boolean> = emphasizeFirstBeat

    override fun setEmphasizeFirstBeat(emphasizeFirstBeat: Boolean) {
        this.emphasizeFirstBeat.value = emphasizeFirstBeat
    }

    override fun getSound(): StateFlow<Sound> = sound

    override fun setSound(sound: Sound) {
        this.sound.value = sound
    }

    override fun getNightMode(): StateFlow<AppNightMode> = nightMode

    override fun setNightMode(nightMode: AppNightMode) {
        this.nightMode.value = nightMode
    }

    override fun getPlaying(): StateFlow<Boolean> = playing

    override fun setPlaying(playing: Boolean) {
        this.playing.value = playing
    }

    override fun startStop() {
        this.playing.value = this.playing.value.not()
    }

    override fun getConnected(): StateFlow<Boolean> = connected

    override fun getCurrentTick(): StateFlow<Tick?> = currentTick

    override fun onTickReceived(tick: Tick) {
        if (tick.type == TickType.STRONG || tick.type == TickType.WEAK) {
            currentTick.value = tick
        }
    }

    override fun setMetronomeService(metronomeService: MetronomeService?) {
        this.metronomeService = metronomeService
        connected.value = metronomeService != null
        metronomeService?.let {
            if (it.playing) {
                updateViewModel(it)
            } else {
                initServiceValues(it)
            }
        }
    }

    private fun updateViewModel(metronomeService: MetronomeService) {
        beats.value = metronomeService.beats
        subdivisions.value = metronomeService.subdivisions
        gaps.value = metronomeService.gaps
        tempo.value = metronomeService.tempo
        emphasizeFirstBeat.value = metronomeService.emphasizeFirstBeat
        sound.value = metronomeService.sound
        playing.value = metronomeService.playing
    }

    private fun initServiceValues(metronomeService: MetronomeService) {
        metronomeService.beats = beats.value
        metronomeService.subdivisions = subdivisions.value
        metronomeService.gaps = gaps.value
        metronomeService.tempo = tempo.value
        metronomeService.emphasizeFirstBeat = emphasizeFirstBeat.value
        metronomeService.sound = sound.value
        metronomeService.playing = playing.value
    }
}

class ComposeMetronomeViewModel(
    val connected: Boolean = true
) : IMetronomeViewModel {
    override fun getBeats(): StateFlow<Beats> = MutableStateFlow(Beats())
    override fun setBeats(beats: Beats) = Unit
    override fun getSubdivisions(): StateFlow<Subdivisions> = MutableStateFlow(Subdivisions())
    override fun setSubdivisions(subdivisions: Subdivisions) = Unit
    override fun getGaps(): StateFlow<Gaps> = MutableStateFlow(Gaps())
    override fun setGaps(gaps: Gaps) = Unit
    override fun getTempo(): StateFlow<Tempo> = MutableStateFlow(Tempo())
    override fun setTempo(tempo: Tempo) = Unit
    override fun changeTempo(delta: Int) = Unit
    override fun tapTempo() = Unit
    override fun getEmphasizeFirstBeat(): StateFlow<Boolean> = MutableStateFlow(true)
    override fun setEmphasizeFirstBeat(emphasizeFirstBeat: Boolean) = Unit
    override fun getSound(): StateFlow<Sound> = MutableStateFlow(Sound.SQUARE_WAVE)
    override fun setSound(sound: Sound) = Unit
    override fun getNightMode(): StateFlow<AppNightMode> = MutableStateFlow(AppNightMode.FOLLOW_SYSTEM)
    override fun setNightMode(nightMode: AppNightMode) = Unit
    override fun getPlaying(): StateFlow<Boolean> = MutableStateFlow(false)
    override fun setPlaying(playing: Boolean) = Unit
    override fun startStop() = Unit
    override fun getConnected(): StateFlow<Boolean> = MutableStateFlow(connected)
    override fun getCurrentTick(): StateFlow<Tick?> = MutableStateFlow(null)
    override fun onTickReceived(tick: Tick) = Unit
    override fun setMetronomeService(metronomeService: MetronomeService?) = Unit
}
