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
import com.bobek.metronome.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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
    fun getBeatsFlow(): StateFlow<Beats>
    fun setBeats(beats: Beats)
    fun getSubdivisionsFlow(): StateFlow<Subdivisions>
    fun setSubdivisions(subdivisions: Subdivisions)
    fun getGapsFlow(): StateFlow<Gaps>
    fun setGaps(gaps: Gaps)
    fun getTempoFlow(): StateFlow<Tempo>
    fun setTempo(tempo: Tempo)
    fun changeTempo(delta: Int)
    fun tapTempo()
    fun getEmphasizeFirstBeatFlow(): StateFlow<Boolean>
    fun setEmphasizeFirstBeat(emphasizeFirstBeat: Boolean)
    fun getSoundFlow(): StateFlow<Sound>
    fun setSound(sound: Sound)
    fun getNightModeFlow(): StateFlow<AppNightMode>
    fun setNightMode(nightMode: AppNightMode)
    fun getPlayingFlow(): StateFlow<Boolean>
    fun setPlaying(playing: Boolean)
    fun startStop()
    fun getConnectedFlow(): StateFlow<Boolean>
    fun getTickFlow(): SharedFlow<Tick>
    fun emitTick(tick: Tick)
    fun setMetronomeService(metronomeService: MetronomeService?)
}

@OptIn(FlowPreview::class)
@HiltViewModel
class MetronomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel(), IMetronomeViewModel {

    private val beatsFlow = MutableStateFlow(Beats())
    private val subdivisionsFlow = MutableStateFlow(Subdivisions())
    private val gapsFlow = MutableStateFlow(Gaps())
    private val tempoFlow = MutableStateFlow(Tempo())
    private val emphasizeFirstBeatFlow = MutableStateFlow(true)
    private val soundFlow = MutableStateFlow(Sound.SQUARE_WAVE)
    private val nightModeFlow = MutableStateFlow(AppNightMode.FOLLOW_SYSTEM)
    private val playingFlow = MutableStateFlow(false)
    private val connectedFlow = MutableStateFlow(false)
    private val tickFlow =
        MutableSharedFlow<Tick>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
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
        settingsRepository.getBeats().firstOrNull()?.let { beatsFlow.value = it }
        settingsRepository.getSubdivisions().firstOrNull()?.let { subdivisionsFlow.value = it }
        settingsRepository.getGaps().firstOrNull()?.let { gapsFlow.value = it }
        settingsRepository.getTempo().firstOrNull()?.let { tempoFlow.value = it }
        settingsRepository.getEmphasizeFirstBeat().firstOrNull()?.let { emphasizeFirstBeatFlow.value = it }
        return settingsRepository.getSound().firstOrNull()?.let { soundFlow.value = it }
    }

    private fun setupFlowsToMetronomeService() {
        viewModelScope.launch {
            beatsFlow.collect { metronomeService?.beats = it }
        }
        viewModelScope.launch {
            subdivisionsFlow.collect { metronomeService?.subdivisions = it }
        }
        viewModelScope.launch {
            gapsFlow.collect { metronomeService?.gaps = it }
        }
        viewModelScope.launch {
            tempoFlow.collect { metronomeService?.tempo = it }
        }
        viewModelScope.launch {
            emphasizeFirstBeatFlow.collect { metronomeService?.emphasizeFirstBeat = it }
        }
        viewModelScope.launch {
            soundFlow.collect { metronomeService?.sound = it }
        }
        viewModelScope.launch {
            playingFlow.collect { metronomeService?.playing = it }
        }
    }

    private fun setupFlowsToSettings() {
        viewModelScope.launch {
            beatsFlow.debounce(SETTINGS_DEBOUNCE_MILLIS)
                .collect { settingsRepository.setBeats(it) }
        }
        viewModelScope.launch {
            subdivisionsFlow.debounce(SETTINGS_DEBOUNCE_MILLIS)
                .collect { settingsRepository.setSubdivisions(it) }
        }
        viewModelScope.launch {
            gapsFlow.debounce(SETTINGS_DEBOUNCE_MILLIS)
                .collect { settingsRepository.setGaps(it) }
        }
        viewModelScope.launch {
            tempoFlow.debounce(SETTINGS_DEBOUNCE_MILLIS)
                .collect { settingsRepository.setTempo(it) }
        }
        viewModelScope.launch {
            emphasizeFirstBeatFlow.debounce(SETTINGS_DEBOUNCE_MILLIS)
                .collect { settingsRepository.setEmphasizeFirstBeat(it) }
        }
        viewModelScope.launch {
            soundFlow.debounce(SETTINGS_DEBOUNCE_MILLIS)
                .collect { settingsRepository.setSound(it) }
        }
        viewModelScope.launch {
            nightModeFlow.debounce(SETTINGS_DEBOUNCE_MILLIS)
                .collect { settingsRepository.setNightMode(it) }
        }
    }

    override fun getBeatsFlow(): StateFlow<Beats> = beatsFlow

    override fun setBeats(beats: Beats) {
        this.beatsFlow.value = beats
    }

    override fun getSubdivisionsFlow(): StateFlow<Subdivisions> = subdivisionsFlow

    override fun setSubdivisions(subdivisions: Subdivisions) {
        this.subdivisionsFlow.value = subdivisions
    }

    override fun getGapsFlow(): StateFlow<Gaps> = gapsFlow

    override fun setGaps(gaps: Gaps) {
        this.gapsFlow.value = gaps
    }

    override fun getTempoFlow(): StateFlow<Tempo> = tempoFlow

    override fun setTempo(tempo: Tempo) {
        this.tempoFlow.value = tempo
    }

    override fun changeTempo(delta: Int) {
        val tempoValue = tempoFlow.value.value + delta

        tempoFlow.value = when {
            tempoValue < Tempo.MIN_VALUE -> Tempo(Tempo.MIN_VALUE)
            tempoValue > Tempo.MAX_VALUE -> Tempo(Tempo.MAX_VALUE)
            else -> Tempo(tempoValue)
        }
    }

    override fun tapTempo() {
        val currentTimeMillis = System.currentTimeMillis()
        pruneOldTaps(currentTimeMillis)
        taps.add(currentTimeMillis)

        val averageIntervalMillis = averageTapIntervalInMillis() ?: return
        val tempoValue = (MILLIS_PER_MINUTE / averageIntervalMillis).toInt()

        tempoFlow.value = when {
            tempoValue > Tempo.MAX_VALUE -> Tempo(Tempo.MAX_VALUE)
            tempoValue < Tempo.MIN_VALUE -> Tempo(Tempo.MIN_VALUE)
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

    override fun getEmphasizeFirstBeatFlow(): StateFlow<Boolean> = emphasizeFirstBeatFlow

    override fun setEmphasizeFirstBeat(emphasizeFirstBeat: Boolean) {
        this.emphasizeFirstBeatFlow.value = emphasizeFirstBeat
    }

    override fun getSoundFlow(): StateFlow<Sound> = soundFlow

    override fun setSound(sound: Sound) {
        this.soundFlow.value = sound
    }

    override fun getNightModeFlow(): StateFlow<AppNightMode> = nightModeFlow

    override fun setNightMode(nightMode: AppNightMode) {
        this.nightModeFlow.value = nightMode
    }

    override fun getPlayingFlow(): StateFlow<Boolean> = playingFlow

    override fun setPlaying(playing: Boolean) {
        this.playingFlow.value = playing
    }

    override fun startStop() {
        this.playingFlow.value = this.playingFlow.value.not()
    }

    override fun getConnectedFlow(): StateFlow<Boolean> = connectedFlow

    override fun getTickFlow(): SharedFlow<Tick> = tickFlow

    override fun emitTick(tick: Tick) {
        tickFlow.tryEmit(tick)
    }

    override fun setMetronomeService(metronomeService: MetronomeService?) {
        this.metronomeService = metronomeService
        connectedFlow.value = metronomeService != null
        metronomeService?.let {
            if (it.playing) {
                updateViewModel(it)
            } else {
                initServiceValues(it)
            }
        }
    }

    private fun updateViewModel(metronomeService: MetronomeService) {
        beatsFlow.value = metronomeService.beats
        subdivisionsFlow.value = metronomeService.subdivisions
        gapsFlow.value = metronomeService.gaps
        tempoFlow.value = metronomeService.tempo
        emphasizeFirstBeatFlow.value = metronomeService.emphasizeFirstBeat
        soundFlow.value = metronomeService.sound
        playingFlow.value = metronomeService.playing
    }

    private fun initServiceValues(metronomeService: MetronomeService) {
        metronomeService.beats = beatsFlow.value
        metronomeService.subdivisions = subdivisionsFlow.value
        metronomeService.gaps = gapsFlow.value
        metronomeService.tempo = tempoFlow.value
        metronomeService.emphasizeFirstBeat = emphasizeFirstBeatFlow.value
        metronomeService.sound = soundFlow.value
        metronomeService.playing = playingFlow.value
    }
}

class ComposeMetronomeViewModel(
    val beats: Beats = Beats(7),
    val subdivisions: Subdivisions = Subdivisions(2),
    val gaps: Gaps = Gaps(sortedSetOf(3, 6)),
    val tempo: Tempo = Tempo(90),
    val emphasizeFirstBeat: Boolean = true,
    val sound: Sound = Sound.SQUARE_WAVE,
    val nightMode: AppNightMode = AppNightMode.FOLLOW_SYSTEM,
    val playing: Boolean = true,
    val connected: Boolean = true
) : IMetronomeViewModel {
    override fun getBeatsFlow(): StateFlow<Beats> = MutableStateFlow(beats)
    override fun setBeats(beats: Beats) = Unit
    override fun getSubdivisionsFlow(): StateFlow<Subdivisions> = MutableStateFlow(subdivisions)
    override fun setSubdivisions(subdivisions: Subdivisions) = Unit
    override fun getGapsFlow(): StateFlow<Gaps> = MutableStateFlow(gaps)
    override fun setGaps(gaps: Gaps) = Unit
    override fun getTempoFlow(): StateFlow<Tempo> = MutableStateFlow(tempo)
    override fun setTempo(tempo: Tempo) = Unit
    override fun changeTempo(delta: Int) = Unit
    override fun tapTempo() = Unit
    override fun getEmphasizeFirstBeatFlow(): StateFlow<Boolean> = MutableStateFlow(emphasizeFirstBeat)
    override fun setEmphasizeFirstBeat(emphasizeFirstBeat: Boolean) = Unit
    override fun getSoundFlow(): StateFlow<Sound> = MutableStateFlow(sound)
    override fun setSound(sound: Sound) = Unit
    override fun getNightModeFlow(): StateFlow<AppNightMode> = MutableStateFlow(nightMode)
    override fun setNightMode(nightMode: AppNightMode) = Unit
    override fun getPlayingFlow(): StateFlow<Boolean> = MutableStateFlow(playing)
    override fun setPlaying(playing: Boolean) = Unit
    override fun startStop() = Unit
    override fun getConnectedFlow(): StateFlow<Boolean> = MutableStateFlow(connected)
    override fun getTickFlow(): SharedFlow<Tick> = MutableSharedFlow()
    override fun emitTick(tick: Tick) = Unit
    override fun setMetronomeService(metronomeService: MetronomeService?) = Unit
}
