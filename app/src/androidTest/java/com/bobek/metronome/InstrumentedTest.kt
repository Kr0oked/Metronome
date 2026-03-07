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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performTextReplacement
import androidx.test.filters.LargeTest
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
        onTempoSlider().setProgress(80f)

        onBeatsSlider().assertBeatsProgress(4f)
        onBeatsEdit().assertTextEquals("4")
        onSubdivisionsSlider().assertSubdivisionsProgress(1f)
        onSubdivisionsEdit().assertTextEquals("1")
        onTempoSlider().assertTempoProgress(80f)
        onTempoEdit().assertTextEquals("80")
        verifyTempoMarking(R.string.tempo_marking_andante)
    }

    @Test
    fun beatsSliderAndEditReflectEachOther() {
        onBeatsSlider().setProgress(1f)
        onBeatsEdit().assertTextEquals("1")

        onBeatsEdit().performTextReplacement("2")
        onBeatsSlider().assertBeatsProgress(2f)
    }

    @Test
    fun subdivisionsSliderAndEditReflectEachOther() {
        onSubdivisionsSlider().setProgress(1f)
        onSubdivisionsEdit().assertTextEquals("1")

        onSubdivisionsEdit().performTextReplacement("2")
        onSubdivisionsSlider().assertSubdivisionsProgress(2f)
    }

    @Test
    fun tempoSliderAndEditReflectEachOther() {
        onTempoSlider().setProgress(30f)
        onTempoEdit().assertTextEquals("30")

        onTempoEdit().performTextReplacement("40")
        onTempoSlider().assertTempoProgress(40f)
    }

    @Test
    fun beatsErrorWhenValueTooBig() {
        onBeatsSlider().setProgress(1f)
        onBeatsEdit().assertHasNoError()

        onBeatsEdit().performTextReplacement("9")
        onBeatsEdit().assertHasError()
        onBeatsSlider().assertBeatsProgress(1f)
    }

    @Test
    fun beatsErrorWhenValueNotANumber() {
        onBeatsSlider().setProgress(1f)
        onBeatsEdit().assertHasNoError()

        onBeatsEdit().performTextReplacement(".")
        onBeatsEdit().assertHasError()
        onBeatsSlider().assertBeatsProgress(1f)
    }

    @Test
    fun subdivisionsErrorWhenValueTooBig() {
        onSubdivisionsSlider().setProgress(1f)
        onSubdivisionsEdit().assertHasNoError()

        onSubdivisionsEdit().performTextReplacement("5")
        onSubdivisionsEdit().assertHasError()
        onSubdivisionsSlider().assertSubdivisionsProgress(1f)
    }

    @Test
    fun subdivisionsErrorWhenValueNotANumber() {
        onSubdivisionsSlider().setProgress(1f)
        onSubdivisionsEdit().assertHasNoError()

        onSubdivisionsEdit().performTextReplacement(".")
        onSubdivisionsEdit().assertHasError()
        onSubdivisionsSlider().assertSubdivisionsProgress(1f)
    }

    @Test
    fun tempoErrorWhenValueTooBig() {
        onTempoSlider().setProgress(30f)
        onTempoEdit().assertHasNoError()

        onTempoEdit().performTextReplacement("253")
        onTempoEdit().assertHasError()
        onTempoSlider().assertTempoProgress(30f)
    }

    @Test
    fun tempoErrorWhenValueNotANumber() {
        onTempoSlider().setProgress(30f)
        onTempoEdit().assertHasNoError()

        onTempoEdit().performTextReplacement(".")
        onTempoEdit().assertHasError()
        onTempoSlider().assertTempoProgress(30f)
    }

    @Test
    fun tempoMarkings() {
        onTempoSlider().setProgress(30f)
        verifyTempoMarking(R.string.tempo_marking_largo)

        onTempoSlider().setProgress(59f)
        verifyTempoMarking(R.string.tempo_marking_largo)

        onTempoSlider().setProgress(60f)
        verifyTempoMarking(R.string.tempo_marking_larghetto)

        onTempoSlider().setProgress(65f)
        verifyTempoMarking(R.string.tempo_marking_larghetto)

        onTempoSlider().setProgress(66f)
        verifyTempoMarking(R.string.tempo_marking_adagio)

        onTempoSlider().setProgress(75f)
        verifyTempoMarking(R.string.tempo_marking_adagio)

        onTempoSlider().setProgress(76f)
        verifyTempoMarking(R.string.tempo_marking_andante)

        onTempoSlider().setProgress(107f)
        verifyTempoMarking(R.string.tempo_marking_andante)

        onTempoSlider().setProgress(108f)
        verifyTempoMarking(R.string.tempo_marking_moderato)

        onTempoSlider().setProgress(119f)
        verifyTempoMarking(R.string.tempo_marking_moderato)

        onTempoSlider().setProgress(120f)
        verifyTempoMarking(R.string.tempo_marking_allegro)

        onTempoSlider().setProgress(167f)
        verifyTempoMarking(R.string.tempo_marking_allegro)

        onTempoSlider().setProgress(168f)
        verifyTempoMarking(R.string.tempo_marking_presto)

        onTempoSlider().setProgress(169f)
        verifyTempoMarking(R.string.tempo_marking_presto)

        onTempoSlider().setProgress(200f)
        verifyTempoMarking(R.string.tempo_marking_prestissimo)

        onTempoSlider().setProgress(252f)
        verifyTempoMarking(R.string.tempo_marking_prestissimo)
    }
}
