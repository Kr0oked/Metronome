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

package com.bobek.metronome

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performTextReplacement
import androidx.test.filters.LargeTest
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo
import org.junit.Test

@LargeTest
class InstrumentedTest : AbstractAndroidTest() {

    @Test
    fun contentVisible() {
        onLoadingIndicator().assertIsNotDisplayed()
        onContent().assertIsDisplayed()
    }

    @Test
    fun initialState() {
        onBeatsSlider().setProgress(4f)
        onSubdivisionsSlider().setProgress(1f)
        applyTempo(80)

        onBeatsSlider().assertProgress(4f, Beats.MIN.toFloat()..Beats.MAX.toFloat())
        onBeatsEdit().assertTextEquals("4")
        onSubdivisionsSlider().assertProgress(1f, Subdivisions.MIN.toFloat()..Subdivisions.MAX.toFloat())
        onSubdivisionsEdit().assertTextEquals("1")
        onTempoSlider().assertProgress(80f, Tempo.MIN.toFloat()..Tempo.MAX.toFloat())
        onTempoEdit().assertTextEquals("80")
        verifyTempoMarking(R.string.tempo_marking_andante)
    }

    @Test
    fun beatsSliderAndEditReflectEachOther() {
        onBeatsSlider().setProgress(1f)
        onBeatsEdit().assertTextEquals("1")

        onBeatsEdit().performTextReplacement("2")
        onBeatsSlider().assertProgress(2f, Beats.MIN.toFloat()..Beats.MAX.toFloat())
    }

    @Test
    fun subdivisionsSliderAndEditReflectEachOther() {
        onSubdivisionsSlider().setProgress(1f)
        onSubdivisionsEdit().assertTextEquals("1")

        onSubdivisionsEdit().performTextReplacement("2")
        onSubdivisionsSlider().assertProgress(2f, Subdivisions.MIN.toFloat()..Subdivisions.MAX.toFloat())
    }

    @Test
    fun tempoSliderAndEditReflectEachOther() {
        onTempoSlider().setProgress(30f)
        onTempoEdit().assertTextEquals("30")

        onTempoEdit().performTextReplacement("40")
        onTempoSlider().assertProgress(40f, Tempo.MIN.toFloat()..Tempo.MAX.toFloat())
    }

    @Test
    fun beatsErrorWhenValueTooBig() {
        onBeatsSlider().setProgress(1f)
        onBeatsEdit().assertHasNoError()

        onBeatsEdit().performTextReplacement("9")
        onBeatsEdit().assertHasError()

        onBeatsSlider().assertProgress(1f, Beats.MIN.toFloat()..Beats.MAX.toFloat())
    }

    @Test
    fun beatsErrorWhenValueNotANumber() {
        onBeatsSlider().setProgress(1f)
        onBeatsEdit().assertHasNoError()

        onBeatsEdit().performTextReplacement(".")
        onBeatsEdit().assertHasError()

        onBeatsSlider().assertProgress(1f, Beats.MIN.toFloat()..Beats.MAX.toFloat())
    }

    @Test
    fun subdivisionsErrorWhenValueTooBig() {
        onSubdivisionsSlider().setProgress(1f)
        onBeatsEdit().assertHasNoError()

        onSubdivisionsEdit().performTextReplacement("5")

        onBeatsEdit().assertHasError()
        onSubdivisionsSlider().assertProgress(1f, Subdivisions.MIN.toFloat()..Subdivisions.MAX.toFloat())
    }

    @Test
    fun subdivisionsErrorWhenValueNotANumber() {
        onSubdivisionsSlider().setProgress(1f)
        onBeatsEdit().assertHasNoError()

        onSubdivisionsEdit().performTextReplacement(".")

        onBeatsEdit().assertHasError()
        onSubdivisionsSlider().assertProgress(1f, Subdivisions.MIN.toFloat()..Subdivisions.MAX.toFloat())
    }

    @Test
    fun tempoErrorWhenValueTooBig() {
        onTempoSlider().setProgress(30f)
        onTempoEditLayout().assertHasNoError()

        onTempoEdit().performTextReplacement("253")

        onTempoEditLayout().assertHasError()
        onTempoSlider().assertProgress(30f, Tempo.MIN.toFloat()..Tempo.MAX.toFloat())
    }

    @Test
    fun tempoErrorWhenValueNotANumber() {
        onTempoSlider().setProgress(30f)
        onTempoEditLayout().assertHasNoError()

        onTempoEdit().performTextReplacement(".")

        onTempoEditLayout().assertHasError()
        onTempoSlider().assertProgress(30f, Tempo.MIN.toFloat()..Tempo.MAX.toFloat())
    }

    @Test
    fun tempoMarkings() {
        applyTempo(30)
        verifyTempoMarking(R.string.tempo_marking_largo)

        applyTempo(59)
        verifyTempoMarking(R.string.tempo_marking_largo)

        applyTempo(60)
        verifyTempoMarking(R.string.tempo_marking_larghetto)

        applyTempo(65)
        verifyTempoMarking(R.string.tempo_marking_larghetto)

        applyTempo(66)
        verifyTempoMarking(R.string.tempo_marking_adagio)

        applyTempo(75)
        verifyTempoMarking(R.string.tempo_marking_adagio)

        applyTempo(76)
        verifyTempoMarking(R.string.tempo_marking_andante)

        applyTempo(107)
        verifyTempoMarking(R.string.tempo_marking_andante)

        applyTempo(108)
        verifyTempoMarking(R.string.tempo_marking_moderato)

        applyTempo(119)
        verifyTempoMarking(R.string.tempo_marking_moderato)

        applyTempo(120)
        verifyTempoMarking(R.string.tempo_marking_allegro)

        applyTempo(167)
        verifyTempoMarking(R.string.tempo_marking_allegro)

        applyTempo(168)
        verifyTempoMarking(R.string.tempo_marking_presto)

        applyTempo(169)
        verifyTempoMarking(R.string.tempo_marking_presto)

        applyTempo(200)
        verifyTempoMarking(R.string.tempo_marking_prestissimo)

        applyTempo(252)
        verifyTempoMarking(R.string.tempo_marking_prestissimo)
    }
}
