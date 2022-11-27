/*
 * This file is part of Metronome.
 * Copyright (C) 2022 Philipp Bobek <philipp.bobek@mailbox.org>
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
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo

private const val TAG = "PreferenceStore"
private const val BEATS = "beats"
private const val SUBDIVISIONS = "subdivisions"
private const val TEMPO = "tempo"
private const val EMPHASIZE_FIRST_BEAT = "emphasize_first_beat"
private const val NIGHT_MODE = "night_mode"

class PreferenceStore(context: Context) {

    val beats = MutableLiveData<Beats>()
    val subdivisions = MutableLiveData<Subdivisions>()
    val tempo = MutableLiveData<Tempo>()
    val emphasizeFirstBeat = MutableLiveData<Boolean>()
    val nightMode = MutableLiveData<Int>()

    private val sharedPreferenceChangeListener = SharedPreferenceChangeListener()
    private val beatsObserver = getBeatsObserver()
    private val subdivisionsObserver = getSubdivisionsObserver()
    private val tempoObserver = getTempoObserver()
    private val emphasizeFirstBeatObserver = getEmphasizeFirstBeatObserver()
    private val nightModeObserver = getNightModeObserver()

    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        updateBeats()
        updateSubdivisions()
        updateTempo()
        updateEmphasizeFirstBeat()
        updateNightMode()

        beats.observeForever(beatsObserver)
        subdivisions.observeForever(subdivisionsObserver)
        tempo.observeForever(tempoObserver)
        emphasizeFirstBeat.observeForever(emphasizeFirstBeatObserver)
        nightMode.observeForever(nightModeObserver)

        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    fun close() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

        beats.removeObserver(beatsObserver)
        subdivisions.removeObserver(subdivisionsObserver)
        tempo.removeObserver(tempoObserver)
        emphasizeFirstBeat.removeObserver(emphasizeFirstBeatObserver)
        nightMode.removeObserver(nightModeObserver)
    }

    private inner class SharedPreferenceChangeListener : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            when (key) {
                BEATS -> updateBeats()
                SUBDIVISIONS -> updateSubdivisions()
                TEMPO -> updateTempo()
                EMPHASIZE_FIRST_BEAT -> updateEmphasizeFirstBeat()
                NIGHT_MODE -> updateNightMode()
            }
        }
    }

    private fun updateBeats() {
        val storedValue = sharedPreferences.getInt(BEATS, Beats.DEFAULT)
        val storedBeats = Beats(storedValue)
        if (beats.value != storedBeats) {
            beats.value = storedBeats
        }
    }

    private fun updateSubdivisions() {
        val storedValue = sharedPreferences.getInt(SUBDIVISIONS, Subdivisions.DEFAULT)
        val storedSubdivisions = Subdivisions(storedValue)
        if (subdivisions.value != storedSubdivisions) {
            subdivisions.value = storedSubdivisions
        }
    }

    private fun updateTempo() {
        val storedValue = sharedPreferences.getInt(TEMPO, Tempo.DEFAULT)
        val storedTempo = Tempo(storedValue)
        if (tempo.value != storedTempo) {
            tempo.value = storedTempo
        }
    }

    private fun updateEmphasizeFirstBeat() {
        val storedValue = sharedPreferences.getBoolean(EMPHASIZE_FIRST_BEAT, true)
        if (emphasizeFirstBeat.value != storedValue) {
            emphasizeFirstBeat.value = storedValue
        }
    }

    private fun updateNightMode() {
        val storedValue = sharedPreferences.getInt(NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        if (nightMode.value != storedValue) {
            nightMode.value = storedValue
        }
    }

    private fun getBeatsObserver(): (t: Beats) -> Unit = {
        val edit = sharedPreferences.edit()
        edit.putInt(BEATS, it.value)
        edit.apply()
        Log.d(TAG, "Persisted beats: ${it.value}")
    }

    private fun getSubdivisionsObserver(): (t: Subdivisions) -> Unit = {
        val edit = sharedPreferences.edit()
        edit.putInt(SUBDIVISIONS, it.value)
        edit.apply()
        Log.d(TAG, "Persisted subdivisions: ${it.value}")
    }

    private fun getTempoObserver(): (t: Tempo) -> Unit = {
        val edit = sharedPreferences.edit()
        edit.putInt(TEMPO, it.value)
        edit.apply()
        Log.d(TAG, "Persisted tempo: ${it.value}")
    }

    private fun getEmphasizeFirstBeatObserver(): (t: Boolean) -> Unit = {
        val edit = sharedPreferences.edit()
        edit.putBoolean(EMPHASIZE_FIRST_BEAT, it)
        edit.apply()
        Log.d(TAG, "Persisted emphasizeFirstBeat: $it")
    }

    private fun getNightModeObserver(): (t: Int) -> Unit = {
        val edit = sharedPreferences.edit()
        edit.putInt(NIGHT_MODE, it)
        edit.apply()
        Log.d(TAG, "Persisted nightMode: $it")
    }
}
