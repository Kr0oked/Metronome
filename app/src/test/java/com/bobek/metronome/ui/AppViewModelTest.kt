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

package com.bobek.metronome.ui

import com.bobek.metronome.data.AppNightMode
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Gaps
import com.bobek.metronome.data.Sound
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo
import com.bobek.metronome.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val DEBOUNCE = 1.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

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
        settingsRepository: SettingsRepository = FakeSettingsRepository()
    ): AppViewModel = AppViewModel(settingsRepository)

    @Test
    fun initialStateLoadsFromSettings() = runTest(testDispatcher) {
        val settings = FakeSettingsRepository(nightMode = AppNightMode.YES)
        val viewModel = createViewModel(settings)
        assertEquals(AppNightMode.YES, viewModel.getNightModeFlow().value)
    }

    @Test
    fun setNightModeUpdatesFlow() {
        val viewModel = createViewModel()
        viewModel.setNightMode(AppNightMode.NO)
        assertEquals(AppNightMode.NO, viewModel.getNightModeFlow().value)
    }

    @Test
    fun nightModePersistedToSettingsAfterDebounce() = runTest(testDispatcher) {
        val settings = FakeSettingsRepository()
        val viewModel = createViewModel(settings)

        viewModel.setNightMode(AppNightMode.YES)
        advanceTimeBy(DEBOUNCE + 1.milliseconds)

        assertEquals(AppNightMode.YES, settings.writtenNightMode)
    }

    @Test
    fun initialValueNotPersistedToSettings() = runTest(testDispatcher) {
        val settings = FakeSettingsRepository()
        createViewModel(settings)

        advanceTimeBy(DEBOUNCE + 1.milliseconds)

        assertFalse(settings.nightModeWritten)
    }
}

private class FakeSettingsRepository(
    nightMode: AppNightMode = AppNightMode.FOLLOW_SYSTEM
) : SettingsRepository {

    private val nightModeFlow = MutableStateFlow(nightMode)

    var nightModeWritten = false
        private set
    var writtenNightMode: AppNightMode? = null
        private set

    override fun getNightMode(): Flow<AppNightMode> = nightModeFlow
    override suspend fun setNightMode(nightMode: AppNightMode) {
        nightModeWritten = true
        writtenNightMode = nightMode
    }

    override fun getBeats(): Flow<Beats> = MutableStateFlow(Beats())
    override suspend fun setBeats(beats: Beats) = Unit
    override fun getSubdivisions(): Flow<Subdivisions> = MutableStateFlow(Subdivisions())
    override suspend fun setSubdivisions(subdivisions: Subdivisions) = Unit
    override fun getGaps(): Flow<Gaps> = MutableStateFlow(Gaps())
    override suspend fun setGaps(gaps: Gaps) = Unit
    override fun getTempo(): Flow<Tempo> = MutableStateFlow(Tempo())
    override suspend fun setTempo(tempo: Tempo) = Unit
    override fun getEmphasizeFirstBeat(): Flow<Boolean> = MutableStateFlow(true)
    override suspend fun setEmphasizeFirstBeat(emphasizeFirstBeat: Boolean) = Unit
    override fun getSound(): Flow<Sound> = MutableStateFlow(Sound.SQUARE_WAVE)
    override suspend fun setSound(sound: Sound) = Unit
    override fun getPostNotificationsPermissionRequested(): Flow<Boolean> = MutableStateFlow(false)
    override suspend fun setPostNotificationsPermissionRequested(postNotificationsPermissionRequested: Boolean) = Unit
}
