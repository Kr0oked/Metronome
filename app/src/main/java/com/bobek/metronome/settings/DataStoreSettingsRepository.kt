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

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bobek.metronome.data.AppNightMode
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Gaps
import com.bobek.metronome.data.Sound
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.SortedSet
import javax.inject.Inject

private const val TAG = "DataStoreSettingsRepository"
private const val NUMBERS_DELIMITER = ","

class DataStoreSettingsRepository @Inject constructor(
    private val preferencesDataStore: DataStore<Preferences>
) : SettingsRepository {

    companion object {
        val BEATS_KEY = intPreferencesKey(PreferenceConstants.BEATS)
        val SUBDIVISIONS_KEY = intPreferencesKey(PreferenceConstants.SUBDIVISIONS)
        val GAPS_KEY = stringPreferencesKey(PreferenceConstants.GAPS)
        val TEMPO_KEY = intPreferencesKey(PreferenceConstants.TEMPO)
        val EMPHASIZE_FIRST_BEAT_KEY = booleanPreferencesKey(PreferenceConstants.EMPHASIZE_FIRST_BEAT)
        val SOUND_KEY = stringPreferencesKey(PreferenceConstants.SOUND)
        val NIGHT_MODE_KEY = stringPreferencesKey(PreferenceConstants.NIGHT_MODE)
        val POST_NOTIFICATION_PERMISSION_REQUESTED_KEY =
            booleanPreferencesKey(PreferenceConstants.POST_NOTIFICATIONS_PERMISSION_REQUESTED)
    }

    override fun getBeats(): Flow<Beats> = preferencesDataStore.data
        .map { it[BEATS_KEY] ?: Beats.DEFAULT }
        .map { Beats(it) }

    override suspend fun setBeats(beats: Beats) {
        preferencesDataStore.edit { it[BEATS_KEY] = beats.value }
        Log.d(TAG, "Persisted beats: ${beats.value}")
    }

    override fun getSubdivisions(): Flow<Subdivisions> = preferencesDataStore.data
        .map { it[SUBDIVISIONS_KEY] ?: Subdivisions.DEFAULT }
        .map { Subdivisions(it) }

    override suspend fun setSubdivisions(subdivisions: Subdivisions) {
        preferencesDataStore.edit { it[SUBDIVISIONS_KEY] = subdivisions.value }
        Log.d(TAG, "Persisted subdivisions: ${subdivisions.value}")
    }

    override fun getGaps(): Flow<Gaps> = preferencesDataStore.data
        .map { it[GAPS_KEY] ?: "" }
        .map { restoreGapsValue(it) }
        .map { Gaps(it) }

    private fun restoreGapsValue(value: String?): SortedSet<Int> = (value ?: "")
        .split(NUMBERS_DELIMITER)
        .filter(String::isNotEmpty)
        .map(String::toInt)
        .toSortedSet()

    override suspend fun setGaps(gaps: Gaps) {
        val gapsString = gaps.value.joinToString(NUMBERS_DELIMITER)
        preferencesDataStore.edit { it[GAPS_KEY] = gapsString }
        Log.d(TAG, "Persisted gaps: $gapsString")
    }

    override fun getTempo(): Flow<Tempo> = preferencesDataStore.data
        .map { it[TEMPO_KEY] ?: Tempo.DEFAULT }
        .map { Tempo(it) }

    override suspend fun setTempo(tempo: Tempo) {
        preferencesDataStore.edit { it[TEMPO_KEY] = tempo.value }
        Log.d(TAG, "Persisted tempo: ${tempo.value}")
    }

    override fun getEmphasizeFirstBeat(): Flow<Boolean> = preferencesDataStore.data
        .map { it[EMPHASIZE_FIRST_BEAT_KEY] ?: true }

    override suspend fun setEmphasizeFirstBeat(emphasizeFirstBeat: Boolean) {
        preferencesDataStore.edit { it[EMPHASIZE_FIRST_BEAT_KEY] = emphasizeFirstBeat }
        Log.d(TAG, "Persisted emphasizeFirstBeat: $emphasizeFirstBeat")
    }

    override fun getSound(): Flow<Sound> = preferencesDataStore.data
        .map { it[SOUND_KEY] ?: Sound.SQUARE_WAVE.preferenceValue }
        .map { Sound.forPreferenceValue(it) }

    override suspend fun setSound(sound: Sound) {
        preferencesDataStore.edit { it[SOUND_KEY] = sound.preferenceValue }
        Log.d(TAG, "Persisted sound: ${sound.preferenceValue}")
    }

    override fun getNightMode(): Flow<AppNightMode> = preferencesDataStore.data
        .map { it[NIGHT_MODE_KEY] ?: AppNightMode.FOLLOW_SYSTEM.preferenceValue }
        .map { AppNightMode.forPreferenceValue(it) }

    override suspend fun setNightMode(nightMode: AppNightMode) {
        preferencesDataStore.edit { it[NIGHT_MODE_KEY] = nightMode.preferenceValue }
        Log.d(TAG, "Persisted nightMode: ${nightMode.preferenceValue}")
    }

    override fun getPostNotificationsPermissionRequested(): Flow<Boolean> = preferencesDataStore.data
        .map { it[POST_NOTIFICATION_PERMISSION_REQUESTED_KEY] ?: false }

    override suspend fun setPostNotificationsPermissionRequested(postNotificationsPermissionRequested: Boolean) {
        preferencesDataStore.edit {
            it[POST_NOTIFICATION_PERMISSION_REQUESTED_KEY] = postNotificationsPermissionRequested
        }
        Log.d(TAG, "Persisted postNotificationsPermissionRequested: $postNotificationsPermissionRequested")
    }
}
