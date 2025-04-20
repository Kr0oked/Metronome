/*
 * This file is part of Metronome.
 * Copyright (C) 2024 Philipp Bobek <philipp.bobek@mailbox.org>
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

package com.bobek.metronome.preference

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.bobek.metronome.data.AppNightMode
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Gaps
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo
import java.util.SortedSet
import androidx.core.content.edit

private const val TAG = "PreferenceStore"
private const val NUMBERS_DELIMITER = ","

class PreferenceStore(context: Context, lifecycle: Lifecycle) {

    val beats = MutableLiveData<Beats>()
    val subdivisions = MutableLiveData<Subdivisions>()
    val gaps = MutableLiveData<Gaps>()
    val tempo = MutableLiveData<Tempo>()
    val emphasizeFirstBeat = MutableLiveData<Boolean>()
    val nightMode = MutableLiveData<AppNightMode>()
    val postNotificationsPermissionRequested = MutableLiveData<Boolean>()

    private val preferenceStoreLifecycleObserver = PreferenceStoreLifecycleObserver()

    private val sharedPreferenceChangeListener = SharedPreferenceChangeListener()

    private val beatsObserver = getBeatsObserver()
    private val subdivisionsObserver = getSubdivisionsObserver()
    private val gapsObserver = getGapsObserver()
    private val tempoObserver = getTempoObserver()
    private val emphasizeFirstBeatObserver = getEmphasizeFirstBeatObserver()
    private val nightModeObserver = getNightModeObserver()
    private val postNotificationsPermissionRequestedObserver = getPostNotificationsPermissionRequestedObserver()

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        updateBeats()
        updateSubdivisions()
        updateTempo()
        updateGaps()
        updateEmphasizeFirstBeat()
        updateNightMode()
        updatePostNotificationsPermissionRequested()

        lifecycle.addObserver(preferenceStoreLifecycleObserver)
    }

    private inner class PreferenceStoreLifecycleObserver : DefaultLifecycleObserver {

        override fun onCreate(owner: LifecycleOwner) {
            beats.observeForever(beatsObserver)
            subdivisions.observeForever(subdivisionsObserver)
            tempo.observeForever(tempoObserver)
            gaps.observeForever(gapsObserver)
            emphasizeFirstBeat.observeForever(emphasizeFirstBeatObserver)
            nightMode.observeForever(nightModeObserver)
            postNotificationsPermissionRequested.observeForever(postNotificationsPermissionRequestedObserver)

            sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

            beats.removeObserver(beatsObserver)
            subdivisions.removeObserver(subdivisionsObserver)
            tempo.removeObserver(tempoObserver)
            gaps.removeObserver(gapsObserver)
            emphasizeFirstBeat.removeObserver(emphasizeFirstBeatObserver)
            nightMode.removeObserver(nightModeObserver)
            postNotificationsPermissionRequested.removeObserver(postNotificationsPermissionRequestedObserver)
        }
    }

    private inner class SharedPreferenceChangeListener : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            when (key) {
                PreferenceConstants.BEATS -> updateBeats()
                PreferenceConstants.SUBDIVISIONS -> updateSubdivisions()
                PreferenceConstants.GAPS -> updateGaps()
                PreferenceConstants.TEMPO -> updateTempo()
                PreferenceConstants.EMPHASIZE_FIRST_BEAT -> updateEmphasizeFirstBeat()
                PreferenceConstants.NIGHT_MODE -> updateNightMode()
                PreferenceConstants.POST_NOTIFICATIONS_PERMISSION_REQUESTED -> updatePostNotificationsPermissionRequested()
            }
        }
    }

    private fun updateBeats() {
        val storedValue = sharedPreferences.getInt(PreferenceConstants.BEATS, Beats.DEFAULT)
        val storedBeats = Beats(storedValue)
        if (beats.value != storedBeats) {
            beats.value = storedBeats
        }
    }

    private fun updateSubdivisions() {
        val storedValue = sharedPreferences.getInt(PreferenceConstants.SUBDIVISIONS, Subdivisions.DEFAULT)
        val storedSubdivisions = Subdivisions(storedValue)
        if (subdivisions.value != storedSubdivisions) {
            subdivisions.value = storedSubdivisions
        }
    }

    private fun updateGaps() {
        val storedValue = (sharedPreferences.getString(PreferenceConstants.GAPS, "") ?: "")
        val storedGapsValue = restoreGapsValue(storedValue)
        val storedGaps = Gaps(storedGapsValue)
        if (gaps.value != storedGaps) {
            gaps.value = storedGaps
        }
    }

    private fun restoreGapsValue(value: String?): SortedSet<Int> = (value ?: "")
        .split(NUMBERS_DELIMITER)
        .filter(String::isNotEmpty)
        .map(String::toInt)
        .toSortedSet()

    private fun updateTempo() {
        val storedValue = sharedPreferences.getInt(PreferenceConstants.TEMPO, Tempo.DEFAULT)
        val storedTempo = Tempo(storedValue)
        if (tempo.value != storedTempo) {
            tempo.value = storedTempo
        }
    }

    private fun updateEmphasizeFirstBeat() {
        val storedValue = sharedPreferences.getBoolean(PreferenceConstants.EMPHASIZE_FIRST_BEAT, true)
        if (emphasizeFirstBeat.value != storedValue) {
            emphasizeFirstBeat.value = storedValue
        }
    }

    private fun updateNightMode() {
        val storedNightMode = sharedPreferences
            .getString(PreferenceConstants.NIGHT_MODE, AppNightMode.FOLLOW_SYSTEM.preferenceValue)
            ?.let { AppNightMode.forPreferenceValue(it) }
            ?: run { AppNightMode.FOLLOW_SYSTEM }

        if (nightMode.value != storedNightMode) {
            nightMode.value = storedNightMode
        }
    }

    private fun updatePostNotificationsPermissionRequested() {
        val storedValue =
            sharedPreferences.getBoolean(PreferenceConstants.POST_NOTIFICATIONS_PERMISSION_REQUESTED, false)
        if (postNotificationsPermissionRequested.value != storedValue) {
            postNotificationsPermissionRequested.value = storedValue
        }
    }

    private fun getBeatsObserver(): (t: Beats) -> Unit = {
        sharedPreferences.edit {
            putInt(PreferenceConstants.BEATS, it.value)
        }
        Log.d(TAG, "Persisted beats: ${it.value}")
    }

    private fun getSubdivisionsObserver(): (t: Subdivisions) -> Unit = {
        sharedPreferences.edit {
            putInt(PreferenceConstants.SUBDIVISIONS, it.value)
        }
        Log.d(TAG, "Persisted subdivisions: ${it.value}")
    }

    private fun getGapsObserver(): (t: Gaps) -> Unit = {
        val gapsString = it.value.joinToString(NUMBERS_DELIMITER)
        sharedPreferences.edit {
            putString(PreferenceConstants.GAPS, gapsString)
        }
        Log.d(TAG, "Persisted gaps: $gapsString")
    }

    private fun getTempoObserver(): (t: Tempo) -> Unit = {
        sharedPreferences.edit {
            putInt(PreferenceConstants.TEMPO, it.value)
        }
        Log.d(TAG, "Persisted tempo: ${it.value}")
    }

    private fun getEmphasizeFirstBeatObserver(): (t: Boolean) -> Unit = {
        sharedPreferences.edit {
            putBoolean(PreferenceConstants.EMPHASIZE_FIRST_BEAT, it)
        }
        Log.d(TAG, "Persisted emphasizeFirstBeat: $it")
    }

    private fun getNightModeObserver(): (t: AppNightMode) -> Unit = {
        sharedPreferences.edit {
            putString(PreferenceConstants.NIGHT_MODE, it.preferenceValue)
        }
        Log.d(TAG, "Persisted nightMode: ${it.preferenceValue}")
    }

    private fun getPostNotificationsPermissionRequestedObserver(): (t: Boolean) -> Unit = {
        sharedPreferences.edit {
            putBoolean(PreferenceConstants.POST_NOTIFICATIONS_PERMISSION_REQUESTED, it)
        }
        Log.d(TAG, "Persisted postNotificationsPermissionRequested: $it")
    }
}
