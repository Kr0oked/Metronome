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

import com.bobek.metronome.data.*

private const val MINUTE_IN_MILLIS = 60_000L

class Metronome(
    private val tickListener: MetronomeTickListener,
    timerFactory: MetronomeTimerFactory
) {

    private val timer = timerFactory.getAdjustableTimer { onTick() }

    private var counter = 0L

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

    var playing: Boolean = false
        set(playing) {
            if (field != playing) {
                field = playing
                if (playing) start() else stop()
            }
        }

    private fun start() {
        counter = 0L
        timer.schedule(calculateTickerPeriod())
    }

    private fun stop() {
        timer.stop()
    }

    private fun onTick() {
        val currentBeat = getCurrentBeat()
        val currentTickType = getCurrentTickType()
        val tick = Tick(currentBeat, currentTickType)
        tickListener.onTick(tick)
        counter++
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

    private fun parametersChanged() {
        if (playing) {
            adjustCurrentPlayback()
        }
    }

    private fun adjustCurrentPlayback() {
        timer.schedule(calculateTickerPeriod())
    }

    private fun calculateTickerPeriod() = MINUTE_IN_MILLIS / tempo.value / subdivisions.value
}
