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

import com.bobek.metronome.data.AppNightMode
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Gaps
import com.bobek.metronome.data.Sound
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo
import com.bobek.metronome.data.Tick
import com.bobek.metronome.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val DEBOUNCE = 1.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class MetronomeViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = UnconfinedTestDispatcher(testScheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        settingsRepository: SettingsRepository = FakeSettingsRepository(),
        timeSource: TimeSource = FakeTimeSource()
    ): MetronomeViewModel = MetronomeViewModel(settingsRepository, timeSource)

    // --- Initial state ---

    @Test
    fun initialStateLoadsFromSettings() = runTest(testDispatcher) {
        val settings = FakeSettingsRepository(
            beats = Beats(6),
            subdivisions = Subdivisions(3),
            tempo = Tempo(120),
            emphasizeFirstBeat = false,
            sound = Sound.SINE_WAVE,
            nightMode = AppNightMode.YES
        )
        val viewModel = createViewModel(settings)

        assertEquals(Beats(6), viewModel.getBeatsFlow().value)
        assertEquals(Subdivisions(3), viewModel.getSubdivisionsFlow().value)
        assertEquals(Tempo(120), viewModel.getTempoFlow().value)
        assertFalse(viewModel.getEmphasizeFirstBeatFlow().value)
        assertEquals(Sound.SINE_WAVE, viewModel.getSoundFlow().value)
        assertEquals(AppNightMode.YES, viewModel.getNightModeFlow().value)
    }

    @Test
    fun initialGapsLoadedFromSettings() = runTest(testDispatcher) {
        val gaps = Gaps(sortedSetOf(2, 4))
        val viewModel = createViewModel(FakeSettingsRepository(gaps = gaps))
        assertEquals(gaps, viewModel.getGapsFlow().value)
    }

    @Test
    fun initialPlayingIsFalse() {
        val viewModel = createViewModel()
        assertFalse(viewModel.getPlayingFlow().value)
    }

    @Test
    fun initialConnectedIsFalse() {
        val viewModel = createViewModel()
        assertFalse(viewModel.getConnectedFlow().value)
    }

    // --- Setters update flows ---

    @Test
    fun setBeatsUpdatesFlow() {
        val viewModel = createViewModel()
        viewModel.setBeats(Beats(7))
        assertEquals(Beats(7), viewModel.getBeatsFlow().value)
    }

    @Test
    fun setSubdivisionsUpdatesFlow() {
        val viewModel = createViewModel()
        viewModel.setSubdivisions(Subdivisions(3))
        assertEquals(Subdivisions(3), viewModel.getSubdivisionsFlow().value)
    }

    @Test
    fun setGapsUpdatesFlow() {
        val viewModel = createViewModel()
        val gaps = Gaps(sortedSetOf(1, 3))
        viewModel.setGaps(gaps)
        assertEquals(gaps, viewModel.getGapsFlow().value)
    }

    @Test
    fun setTempoUpdatesFlow() {
        val viewModel = createViewModel()
        viewModel.setTempo(Tempo(140))
        assertEquals(Tempo(140), viewModel.getTempoFlow().value)
    }

    @Test
    fun setEmphasizeFirstBeatUpdatesFlow() {
        val viewModel = createViewModel()
        viewModel.setEmphasizeFirstBeat(false)
        assertFalse(viewModel.getEmphasizeFirstBeatFlow().value)
    }

    @Test
    fun setSoundUpdatesFlow() {
        val viewModel = createViewModel()
        viewModel.setSound(Sound.PLUCK)
        assertEquals(Sound.PLUCK, viewModel.getSoundFlow().value)
    }

    @Test
    fun setNightModeUpdatesFlow() {
        val viewModel = createViewModel()
        viewModel.setNightMode(AppNightMode.NO)
        assertEquals(AppNightMode.NO, viewModel.getNightModeFlow().value)
    }

    @Test
    fun setPlayingUpdatesFlow() {
        val viewModel = createViewModel()
        viewModel.setPlaying(true)
        assertTrue(viewModel.getPlayingFlow().value)
    }

    // --- changeTempo ---

    @Test
    fun changeTempoIncreases() {
        val viewModel = createViewModel()
        val initial = viewModel.getTempoFlow().value.value
        viewModel.changeTempo(10)
        assertEquals(initial + 10, viewModel.getTempoFlow().value.value)
    }

    @Test
    fun changeTempoDecreases() {
        val viewModel = createViewModel()
        val initial = viewModel.getTempoFlow().value.value
        viewModel.changeTempo(-10)
        assertEquals(initial - 10, viewModel.getTempoFlow().value.value)
    }

    @Test
    fun changeTempoClampedAtMin() {
        val viewModel = createViewModel()
        viewModel.changeTempo(-(Tempo.MAX_VALUE))
        assertEquals(Tempo.MIN_VALUE, viewModel.getTempoFlow().value.value)
    }

    @Test
    fun changeTempoClampedAtMax() {
        val viewModel = createViewModel()
        viewModel.changeTempo(Tempo.MAX_VALUE)
        assertEquals(Tempo.MAX_VALUE, viewModel.getTempoFlow().value.value)
    }

    // --- tapTempo ---

    @Test
    fun singleTapDoesNotChangeTempo() {
        val viewModel = createViewModel()
        val initial = viewModel.getTempoFlow().value
        viewModel.tapTempo()
        assertEquals(initial, viewModel.getTempoFlow().value)
    }

    @Test
    fun twoTapsCalculateTempo() {
        val timeSource = FakeTimeSource()
        val viewModel = createViewModel(timeSource = timeSource)
        timeSource.millis = 0
        viewModel.tapTempo()
        timeSource.millis = 500
        viewModel.tapTempo()
        // 60_000 / 500 = 120 BPM
        assertEquals(Tempo(120), viewModel.getTempoFlow().value)
    }

    @Test
    fun tapTempoClampedAtMax() {
        val timeSource = FakeTimeSource()
        val viewModel = createViewModel(timeSource = timeSource)
        timeSource.millis = 0
        viewModel.tapTempo()
        timeSource.millis = 1
        viewModel.tapTempo()
        // 60_000 / 1 = 60_000 BPM → clamped
        assertEquals(Tempo.MAX_VALUE, viewModel.getTempoFlow().value.value)
    }

    @Test
    fun tapTempoClampedAtMin() {
        val timeSource = FakeTimeSource()
        val viewModel = createViewModel(timeSource = timeSource)
        timeSource.millis = 0
        viewModel.tapTempo()
        timeSource.millis = 4_000
        viewModel.tapTempo()
        // 60_000 / 4_000 = 15 BPM → clamped
        assertEquals(Tempo.MIN_VALUE, viewModel.getTempoFlow().value.value)
    }

    @Test
    fun tapTempoIgnoresOldTapsOutsideWindow() {
        val timeSource = FakeTimeSource()
        val viewModel = createViewModel(timeSource = timeSource)
        timeSource.millis = 0
        viewModel.tapTempo()
        timeSource.millis = 6_000 // outside 5-second window
        viewModel.tapTempo()
        // Only one tap within the window → no interval → tempo unchanged
        assertEquals(Tempo(), viewModel.getTempoFlow().value)
    }

    // --- startStop ---

    @Test
    fun startStopTogglesPlayingToTrue() {
        val viewModel = createViewModel()
        viewModel.startStop()
        assertTrue(viewModel.getPlayingFlow().value)
    }

    @Test
    fun startStopTogglesPlayingToFalse() {
        val viewModel = createViewModel()
        viewModel.setPlaying(true)
        viewModel.startStop()
        assertFalse(viewModel.getPlayingFlow().value)
    }

    // --- setMetronomeService ---

    @Test
    fun connectingServiceSetsConnectedTrue() {
        val viewModel = createViewModel()
        viewModel.setMetronomeService(FakeMetronomeService())
        assertTrue(viewModel.getConnectedFlow().value)
    }

    @Test
    fun disconnectingServiceSetsConnectedFalse() {
        val viewModel = createViewModel()
        viewModel.setMetronomeService(FakeMetronomeService())
        viewModel.setMetronomeService(null)
        assertFalse(viewModel.getConnectedFlow().value)
    }

    @Test
    fun connectingNonPlayingServicePushesViewModelValuesToService() {
        val viewModel = createViewModel()
        viewModel.setBeats(Beats(6))
        viewModel.setTempo(Tempo(120))

        val service = FakeMetronomeService(playing = false)
        viewModel.setMetronomeService(service)

        assertEquals(Beats(6), service.beats)
        assertEquals(Tempo(120), service.tempo)
    }

    @Test
    fun connectingPlayingServicePullsServiceValuesToViewModel() {
        val viewModel = createViewModel()
        val service = FakeMetronomeService(
            playing = true,
            beats = Beats(7),
            tempo = Tempo(150),
            sound = Sound.RISSET_DRUM
        )
        viewModel.setMetronomeService(service)

        assertEquals(Beats(7), viewModel.getBeatsFlow().value)
        assertEquals(Tempo(150), viewModel.getTempoFlow().value)
        assertEquals(Sound.RISSET_DRUM, viewModel.getSoundFlow().value)
        assertTrue(viewModel.getPlayingFlow().value)
    }

    @Test
    fun reconnectingServiceDoesNotLeakJobs() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val service1 = FakeMetronomeService()
        val service2 = FakeMetronomeService()

        viewModel.setMetronomeService(service1)
        viewModel.setMetronomeService(service2)

        // Emitting from the old service should not affect the viewModel (jobs were cancelled)
        service1.beats = Beats(8)
        service1.getRefreshFlow().emit(Unit)

        // viewModel should still reflect service2's values, not service1's
        assertEquals(service2.beats, viewModel.getBeatsFlow().value)
    }

    @Test
    fun servicePropertyUpdatedWhenFlowChanges() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val service = FakeMetronomeService()
        viewModel.setMetronomeService(service)

        viewModel.setBeats(Beats(7))

        assertEquals(Beats(7), service.beats)
    }

    // --- Settings persistence (debounced) ---

    @Test
    fun beatsPersistedToSettingsAfterDebounce() = runTest(testDispatcher) {
        val settings = FakeSettingsRepository()
        val viewModel = createViewModel(settings)

        viewModel.setBeats(Beats(7))
        advanceTimeBy(DEBOUNCE + 1.milliseconds)

        assertEquals(Beats(7), settings.writtenBeats)
    }

    @Test
    fun tempoPersistedToSettingsAfterDebounce() = runTest(testDispatcher) {
        val settings = FakeSettingsRepository()
        val viewModel = createViewModel(settings)

        viewModel.setTempo(Tempo(160))
        advanceTimeBy(DEBOUNCE + 1.milliseconds)

        assertEquals(Tempo(160), settings.writtenTempo)
    }

    @Test
    fun initialValueNotPersistedToSettings() = runTest(testDispatcher) {
        val settings = FakeSettingsRepository()
        createViewModel(settings)

        advanceTimeBy(DEBOUNCE + 1.milliseconds)

        // drop(1) means the initial StateFlow emission must not trigger a write
        assertFalse(settings.beatsWritten)
    }
}

// --- Fakes ---

private class FakeSettingsRepository(
    beats: Beats = Beats(),
    subdivisions: Subdivisions = Subdivisions(),
    gaps: Gaps = Gaps(),
    tempo: Tempo = Tempo(),
    emphasizeFirstBeat: Boolean = true,
    sound: Sound = Sound.SQUARE_WAVE,
    nightMode: AppNightMode = AppNightMode.FOLLOW_SYSTEM
) : SettingsRepository {

    private val beatsFlow = MutableStateFlow(beats)
    private val subdivisionsFlow = MutableStateFlow(subdivisions)
    private val gapsFlow = MutableStateFlow(gaps)
    private val tempoFlow = MutableStateFlow(tempo)
    private val emphasizeFirstBeatFlow = MutableStateFlow(emphasizeFirstBeat)
    private val soundFlow = MutableStateFlow(sound)
    private val nightModeFlow = MutableStateFlow(nightMode)
    private val postNotificationsFlow = MutableStateFlow(false)

    var beatsWritten = false
        private set
    var writtenBeats: Beats? = null
        private set
    var writtenTempo: Tempo? = null
        private set

    override fun getBeats() = beatsFlow
    override suspend fun setBeats(beats: Beats) {
        beatsWritten = true
        writtenBeats = beats
    }

    override fun getSubdivisions() = subdivisionsFlow
    override suspend fun setSubdivisions(subdivisions: Subdivisions) {}

    override fun getGaps() = gapsFlow
    override suspend fun setGaps(gaps: Gaps) {}

    override fun getTempo() = tempoFlow
    override suspend fun setTempo(tempo: Tempo) {
        writtenTempo = tempo
    }

    override fun getEmphasizeFirstBeat() = emphasizeFirstBeatFlow
    override suspend fun setEmphasizeFirstBeat(emphasizeFirstBeat: Boolean) {}

    override fun getSound() = soundFlow
    override suspend fun setSound(sound: Sound) {}

    override fun getNightMode() = nightModeFlow
    override suspend fun setNightMode(nightMode: AppNightMode) {}

    override fun getPostNotificationsPermissionRequested() = postNotificationsFlow
    override suspend fun setPostNotificationsPermissionRequested(postNotificationsPermissionRequested: Boolean) {}
}

private class FakeTimeSource : TimeSource {
    var millis = 0L

    override fun markNow(): TimeMark {
        val captured = millis
        return object : TimeMark {
            override fun elapsedNow(): Duration = (this@FakeTimeSource.millis - captured).milliseconds
        }
    }
}

private class FakeMetronomeService(
    override var beats: Beats = Beats(),
    override var subdivisions: Subdivisions = Subdivisions(),
    override var gaps: Gaps = Gaps(),
    override var tempo: Tempo = Tempo(),
    override var emphasizeFirstBeat: Boolean = true,
    override var sound: Sound = Sound.SQUARE_WAVE,
    override var playing: Boolean = false
) : IMetronomeService {

    private val tickFlow = MutableSharedFlow<Tick>()
    private val refreshFlow = MutableSharedFlow<Unit>()

    override fun getTickFlow() = tickFlow
    override fun getRefreshFlow() = refreshFlow
}
