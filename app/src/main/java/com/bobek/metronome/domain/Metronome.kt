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

package com.bobek.metronome.domain

import android.util.Log
import com.bobek.metronome.data.*
import java.util.*

private const val TAG = "Metronome"
private const val MINUTE_IN_MILLIS = 60_000L

class Metronome(
    private val tickListener: MetronomeTickListener,
    private val timerProvider: TimerProvider
) {

    private var timer = timerProvider.createTimer()
    private var counter = 0L
    private var lastTickTime = 0L

    var beats: Beats = Beats()
        set(beats) {
            if (field != beats) {
                field = beats
                parametersChanged()
            }
        }

    var subdivisions: Subdivisions = Subdivisions()
        set(subdivisions) {
            if (field != subdivisions) {
                field = subdivisions
                parametersChanged()
            }
        }

    var tempo: Tempo = Tempo()
        set(tempo) {
            if (field != tempo) {
                field = tempo
                parametersChanged()
            }
        }

    var playing = false
        set(playing) {
            if (field != playing) {
                field = playing
                if (playing) start() else stop()
            }
        }

    private fun start() {
        Log.i(TAG, "Start")
        resetTimer()
        counter = 0L
        lastTickTime = System.currentTimeMillis()
        timer.scheduleAtFixedRate(getTickerTask(), Date(lastTickTime), calculateTickerPeriod())
    }

    private fun getTickerTask() = object : TimerTask() {
        override fun run() {
            lastTickTime = System.currentTimeMillis()
            val currentBeat = getCurrentBeat()
            val currentTickType = getCurrentTickType()
            val tick = Tick(currentBeat, currentTickType)
            Log.d(TAG, "Tick $counter $tick")
            tickListener.onTick(tick)
            counter++
        }
    }

    private fun getCurrentBeat() = (((counter / subdivisions.value) % beats.value) + 1).toInt()

    private fun getCurrentTickType(): TickType {
        return when {
            isStrongTick() -> TickType.STRONG
            isWeakTick() -> TickType.WEAK
            else -> TickType.SUB
        }
    }

    private fun isStrongTick() = counter % (beats.value * subdivisions.value) == 0L

    private fun isWeakTick() = counter % subdivisions.value == 0L

    private fun stop() {
        Log.i(TAG, "Stop")
        resetTimer()
    }

    private fun parametersChanged() {
        if (playing) {
            adjustCurrentPlayback()
        }
    }

    private fun adjustCurrentPlayback() {
        resetTimer()
        val tickerPeriod = calculateTickerPeriod()
        val startTime = lastTickTime + tickerPeriod
        timer.scheduleAtFixedRate(getTickerTask(), Date(startTime), tickerPeriod)
        Log.i(TAG, "Adjusted current playback")
    }

    private fun resetTimer() {
        timer.cancel()
        timer = timerProvider.createTimer()
    }

    private fun calculateTickerPeriod() = MINUTE_IN_MILLIS / tempo.value / subdivisions.value
}
