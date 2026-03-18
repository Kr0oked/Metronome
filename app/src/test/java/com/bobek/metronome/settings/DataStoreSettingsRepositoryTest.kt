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

package com.bobek.metronome.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.bobek.metronome.data.AppNightMode
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Gaps
import com.bobek.metronome.data.Sound
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun createRepository(): DataStoreSettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_preferences.preferences_pb") }
        )
        return DataStoreSettingsRepository(dataStore)
    }

    // Beats

    @Test
    fun beatsDefaultValue() = testScope.runTest {
        val repo = createRepository()
        assertEquals(Beats(Beats.DEFAULT_VALUE), repo.getBeats().first())
    }

    @Test
    fun beatsRoundTrip() = testScope.runTest {
        val repo = createRepository()
        val beats = Beats(6)
        repo.setBeats(beats)
        assertEquals(beats, repo.getBeats().first())
    }

    // Subdivisions

    @Test
    fun subdivisionsDefaultValue() = testScope.runTest {
        val repo = createRepository()
        assertEquals(Subdivisions(Subdivisions.DEFAULT_VALUE), repo.getSubdivisions().first())
    }

    @Test
    fun subdivisionsRoundTrip() = testScope.runTest {
        val repo = createRepository()
        val subdivisions = Subdivisions(3)
        repo.setSubdivisions(subdivisions)
        assertEquals(subdivisions, repo.getSubdivisions().first())
    }

    // Gaps

    @Test
    fun gapsDefaultValueIsEmpty() = testScope.runTest {
        val repo = createRepository()
        assertTrue(repo.getGaps().first().value.isEmpty())
    }

    @Test
    fun gapsRoundTrip() = testScope.runTest {
        val repo = createRepository()
        val gaps = Gaps(sortedSetOf(2, 4))
        repo.setGaps(gaps)
        assertEquals(gaps, repo.getGaps().first())
    }

    @Test
    fun gapsEmptyRoundTrip() = testScope.runTest {
        val repo = createRepository()
        val gaps = Gaps(sortedSetOf(1, 3))
        repo.setGaps(gaps)
        repo.setGaps(Gaps())
        assertTrue(repo.getGaps().first().value.isEmpty())
    }

    // Tempo

    @Test
    fun tempoDefaultValue() = testScope.runTest {
        val repo = createRepository()
        assertEquals(Tempo(Tempo.DEFAULT_VALUE), repo.getTempo().first())
    }

    @Test
    fun tempoRoundTrip() = testScope.runTest {
        val repo = createRepository()
        val tempo = Tempo(120)
        repo.setTempo(tempo)
        assertEquals(tempo, repo.getTempo().first())
    }

    // EmphasizeFirstBeat

    @Test
    fun emphasizeFirstBeatDefaultIsTrue() = testScope.runTest {
        val repo = createRepository()
        assertTrue(repo.getEmphasizeFirstBeat().first())
    }

    @Test
    fun emphasizeFirstBeatRoundTripFalse() = testScope.runTest {
        val repo = createRepository()
        repo.setEmphasizeFirstBeat(false)
        assertFalse(repo.getEmphasizeFirstBeat().first())
    }

    @Test
    fun emphasizeFirstBeatRoundTripTrue() = testScope.runTest {
        val repo = createRepository()
        repo.setEmphasizeFirstBeat(false)
        repo.setEmphasizeFirstBeat(true)
        assertTrue(repo.getEmphasizeFirstBeat().first())
    }

    // Sound

    @Test
    fun soundDefaultIsSquareWave() = testScope.runTest {
        val repo = createRepository()
        assertEquals(Sound.SQUARE_WAVE, repo.getSound().first())
    }

    @Test
    fun soundRoundTrip() = testScope.runTest {
        val repo = createRepository()
        repo.setSound(Sound.SINE_WAVE)
        assertEquals(Sound.SINE_WAVE, repo.getSound().first())
    }

    @Test
    fun allSoundsRoundTrip() = testScope.runTest {
        val repo = createRepository()
        for (sound in Sound.entries) {
            repo.setSound(sound)
            assertEquals(sound, repo.getSound().first())
        }
    }

    // NightMode

    @Test
    fun nightModeDefaultIsFollowSystem() = testScope.runTest {
        val repo = createRepository()
        assertEquals(AppNightMode.FOLLOW_SYSTEM, repo.getNightMode().first())
    }

    @Test
    fun nightModeRoundTrip() = testScope.runTest {
        val repo = createRepository()
        repo.setNightMode(AppNightMode.YES)
        assertEquals(AppNightMode.YES, repo.getNightMode().first())
    }

    @Test
    fun allNightModesRoundTrip() = testScope.runTest {
        val repo = createRepository()
        for (nightMode in AppNightMode.entries) {
            repo.setNightMode(nightMode)
            assertEquals(nightMode, repo.getNightMode().first())
        }
    }

    // PostNotificationsPermissionRequested

    @Test
    fun postNotificationsPermissionRequestedDefaultIsFalse() = testScope.runTest {
        val repo = createRepository()
        assertFalse(repo.getPostNotificationsPermissionRequested().first())
    }

    @Test
    fun postNotificationsPermissionRequestedRoundTrip() = testScope.runTest {
        val repo = createRepository()
        repo.setPostNotificationsPermissionRequested(true)
        assertTrue(repo.getPostNotificationsPermissionRequested().first())
    }
}
