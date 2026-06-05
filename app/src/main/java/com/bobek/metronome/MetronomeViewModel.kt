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
import jakarta.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private val TAP_WINDOW = 5.seconds
private val SETTINGS_DEBOUNCE = 1.seconds

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
    fun setMetronomeService(metronomeService: IMetronomeService?)
}

@HiltViewModel
@OptIn(FlowPreview::class)
class MetronomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    timeSource: TimeSource
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

    private val tickFlow = MutableSharedFlow<Tick>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val startMark = timeSource.markNow()

    private val taps = ArrayDeque<Duration>()

    private var metronomeService: IMetronomeService? = null

    private var serviceFlowJobs: List<Job> = emptyList()

    init {
        viewModelScope.launch {
            initFromSettings()
        }

        setupFlowsToMetronomeService()
        setupFlowsToSettings()
    }

    private suspend fun initFromSettings() {
        settingsRepository.getBeats().firstOrNull()?.let { beatsFlow.value = it }
        settingsRepository.getSubdivisions().firstOrNull()?.let { subdivisionsFlow.value = it }
        settingsRepository.getGaps().firstOrNull()?.let { gapsFlow.value = it }
        settingsRepository.getTempo().firstOrNull()?.let { tempoFlow.value = it }
        settingsRepository.getEmphasizeFirstBeat().firstOrNull()?.let { emphasizeFirstBeatFlow.value = it }
        settingsRepository.getSound().firstOrNull()?.let { soundFlow.value = it }
        settingsRepository.getNightMode().firstOrNull()?.let { nightModeFlow.value = it }
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
            beatsFlow.drop(1).debounce(SETTINGS_DEBOUNCE)
                .collect { settingsRepository.setBeats(it) }
        }
        viewModelScope.launch {
            subdivisionsFlow.drop(1).debounce(SETTINGS_DEBOUNCE)
                .collect { settingsRepository.setSubdivisions(it) }
        }
        viewModelScope.launch {
            gapsFlow.drop(1).debounce(SETTINGS_DEBOUNCE)
                .collect { settingsRepository.setGaps(it) }
        }
        viewModelScope.launch {
            tempoFlow.drop(1).debounce(SETTINGS_DEBOUNCE)
                .collect { settingsRepository.setTempo(it) }
        }
        viewModelScope.launch {
            emphasizeFirstBeatFlow.drop(1).debounce(SETTINGS_DEBOUNCE)
                .collect { settingsRepository.setEmphasizeFirstBeat(it) }
        }
        viewModelScope.launch {
            soundFlow.drop(1).debounce(SETTINGS_DEBOUNCE)
                .collect { settingsRepository.setSound(it) }
        }
        viewModelScope.launch {
            nightModeFlow.drop(1).debounce(SETTINGS_DEBOUNCE)
                .collect { settingsRepository.setNightMode(it) }
        }
    }

    override fun getBeatsFlow() = beatsFlow

    override fun setBeats(beats: Beats) {
        beatsFlow.value = beats
    }

    override fun getSubdivisionsFlow() = subdivisionsFlow

    override fun setSubdivisions(subdivisions: Subdivisions) {
        subdivisionsFlow.value = subdivisions
    }

    override fun getGapsFlow() = gapsFlow

    override fun setGaps(gaps: Gaps) {
        gapsFlow.value = gaps
    }

    override fun getTempoFlow() = tempoFlow

    override fun setTempo(tempo: Tempo) {
        tempoFlow.value = tempo
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
        val elapsed = startMark.elapsedNow()
        pruneOldTaps(elapsed)
        taps.add(elapsed)

        val averageInterval = averageTapInterval() ?: return
        val tempoValue = (1.minutes / averageInterval).toInt()

        tempoFlow.value = when {
            tempoValue > Tempo.MAX_VALUE -> Tempo(Tempo.MAX_VALUE)
            tempoValue < Tempo.MIN_VALUE -> Tempo(Tempo.MIN_VALUE)
            else -> Tempo(tempoValue)
        }
    }

    private fun pruneOldTaps(elapsed: Duration) {
        taps.removeAll { elapsed - it > TAP_WINDOW }
    }

    private fun averageTapInterval(): Duration? {
        val intervals = taps.zipWithNext { a, b -> b - a }
        return intervals
            .reduceOrNull { acc, d -> acc + d }
            ?.div(intervals.size)
    }

    override fun getEmphasizeFirstBeatFlow() = emphasizeFirstBeatFlow

    override fun setEmphasizeFirstBeat(emphasizeFirstBeat: Boolean) {
        emphasizeFirstBeatFlow.value = emphasizeFirstBeat
    }

    override fun getSoundFlow() = soundFlow

    override fun setSound(sound: Sound) {
        soundFlow.value = sound
    }

    override fun getNightModeFlow() = nightModeFlow

    override fun setNightMode(nightMode: AppNightMode) {
        nightModeFlow.value = nightMode
    }

    override fun getPlayingFlow() = playingFlow

    override fun setPlaying(playing: Boolean) {
        playingFlow.value = playing
    }

    override fun startStop() {
        playingFlow.value = playingFlow.value.not()
    }

    override fun getConnectedFlow() = connectedFlow

    override fun getTickFlow() = tickFlow

    override fun setMetronomeService(metronomeService: IMetronomeService?) {
        this.metronomeService = metronomeService
        connectedFlow.value = metronomeService != null

        serviceFlowJobs.forEach { it.cancel() }
        serviceFlowJobs = emptyList()

        metronomeService?.let {
            setupFlowsFromMetronomeService(metronomeService)

            if (it.playing) {
                updateViewModel(it)
            } else {
                initServiceValues(it)
            }
        }
    }

    private fun setupFlowsFromMetronomeService(metronomeService: IMetronomeService) {
        serviceFlowJobs = listOf(
            viewModelScope.launch {
                metronomeService.getTickFlow().collect { tickFlow.tryEmit(it) }
            },
            viewModelScope.launch {
                metronomeService.getRefreshFlow().collect { updateViewModel(metronomeService) }
            }
        )
    }

    private fun updateViewModel(metronomeService: IMetronomeService) {
        beatsFlow.value = metronomeService.beats
        subdivisionsFlow.value = metronomeService.subdivisions
        gapsFlow.value = metronomeService.gaps
        tempoFlow.value = metronomeService.tempo
        emphasizeFirstBeatFlow.value = metronomeService.emphasizeFirstBeat
        soundFlow.value = metronomeService.sound
        playingFlow.value = metronomeService.playing
    }

    private fun initServiceValues(metronomeService: IMetronomeService) {
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
    override fun getBeatsFlow() = MutableStateFlow(beats)
    override fun setBeats(beats: Beats) = Unit
    override fun getSubdivisionsFlow() = MutableStateFlow(subdivisions)
    override fun setSubdivisions(subdivisions: Subdivisions) = Unit
    override fun getGapsFlow() = MutableStateFlow(gaps)
    override fun setGaps(gaps: Gaps) = Unit
    override fun getTempoFlow() = MutableStateFlow(tempo)
    override fun setTempo(tempo: Tempo) = Unit
    override fun changeTempo(delta: Int) = Unit
    override fun tapTempo() = Unit
    override fun getEmphasizeFirstBeatFlow() = MutableStateFlow(emphasizeFirstBeat)
    override fun setEmphasizeFirstBeat(emphasizeFirstBeat: Boolean) = Unit
    override fun getSoundFlow() = MutableStateFlow(sound)
    override fun setSound(sound: Sound) = Unit
    override fun getNightModeFlow() = MutableStateFlow(nightMode)
    override fun setNightMode(nightMode: AppNightMode) = Unit
    override fun getPlayingFlow() = MutableStateFlow(playing)
    override fun setPlaying(playing: Boolean) = Unit
    override fun startStop() = Unit
    override fun getConnectedFlow() = MutableStateFlow(connected)
    override fun getTickFlow() = MutableSharedFlow<Tick>()
    override fun setMetronomeService(metronomeService: IMetronomeService?) = Unit
}
