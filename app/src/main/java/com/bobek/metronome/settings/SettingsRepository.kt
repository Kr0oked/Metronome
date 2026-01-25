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

import com.bobek.metronome.data.AppNightMode
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Gaps
import com.bobek.metronome.data.Sound
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getBeats(): Flow<Beats>
    suspend fun setBeats(beats: Beats)
    fun getSubdivisions(): Flow<Subdivisions>
    suspend fun setSubdivisions(subdivisions: Subdivisions)
    fun getGaps(): Flow<Gaps>
    suspend fun setGaps(gaps: Gaps)
    fun getTempo(): Flow<Tempo>
    suspend fun setTempo(tempo: Tempo)
    fun getEmphasizeFirstBeat(): Flow<Boolean>
    suspend fun setEmphasizeFirstBeat(emphasizeFirstBeat: Boolean)
    fun getSound(): Flow<Sound>
    suspend fun setSound(sound: Sound)
    fun getNightMode(): Flow<AppNightMode>
    suspend fun setNightMode(nightMode: AppNightMode)
    fun getPostNotificationsPermissionRequested(): Flow<Boolean>
    suspend fun setPostNotificationsPermissionRequested(postNotificationsPermissionRequested: Boolean)
}
