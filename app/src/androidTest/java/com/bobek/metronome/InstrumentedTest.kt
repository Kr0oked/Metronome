/*
 * This file is part of Metronome.
 * Copyright (C) 2023 Philipp Bobek <philipp.bobek@mailbox.org>
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

import android.content.Intent
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.bobek.metronome.SliderUtils.setValue
import com.bobek.metronome.SliderUtils.withValue
import com.bobek.metronome.TextInputLayoutUtils.displaysError
import com.bobek.metronome.TextInputLayoutUtils.doesNotDisplayError
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class InstrumentedTest {

    private val intent: Intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)

    @get:Rule
    var activityRule = ActivityScenarioRule<MainActivity>(intent)

    @Test
    fun contentVisible() {
        onView(withId(R.id.loading_indicator)).check(matches(not(isDisplayed())))
        onView(withId(R.id.content)).check(matches(isDisplayed()))
    }

    @Test
    fun initialState() {
        onBeatsSlider().perform(setValue(4.toFloat()))
        onSubdivisionsSlider().perform(setValue(1.toFloat()))
        applyTempo(80)

        onBeatsSlider().check(matches(withValue(4.0f)))
        onBeatsEdit().check(matches(withText("4")))
        onSubdivisionsSlider().check(matches(withValue(1.0f)))
        onSubdivisionsEdit().check(matches(withText("1")))
        onTempoSlider().check(matches(withValue(80.0f)))
        onTempoEdit().check(matches(withText("80")))
        onTempoMarkingText().check(matches(withText(R.string.tempo_marking_andante)))
    }

    @Test
    fun beatsSliderAndEditReflectEachOther() {
        onBeatsSlider().perform(setValue(1.0f))
        onBeatsEdit().check(matches(withText("1")))

        onBeatsEdit().perform(ViewActions.replaceText("2"))
        onBeatsSlider().check(matches(withValue(2.0f)))
    }

    @Test
    fun subdivisionsSliderAndEditReflectEachOther() {
        onSubdivisionsSlider().perform(setValue(1.0f))
        onSubdivisionsEdit().check(matches(withText("1")))

        onSubdivisionsEdit().perform(ViewActions.replaceText("2"))
        onSubdivisionsSlider().check(matches(withValue(2.0f)))
    }

    @Test
    fun tempoSliderAndEditReflectEachOther() {
        onTempoSlider().perform(setValue(40.0f))
        onTempoEdit().check(matches(withText("40")))

        onTempoEdit().perform(ViewActions.replaceText("50"))
        onTempoSlider().check(matches(withValue(50.0f)))
    }

    @Test
    fun beatsErrorWhenValueTooBig() {
        onBeatsSlider().perform(setValue(1.0f))
        onBeatsEditLayout().check(matches(doesNotDisplayError()))

        onBeatsEdit().perform(ViewActions.replaceText("9"))
        onBeatsEditLayout().check(matches(displaysError()))

        onBeatsSlider().check(matches(withValue(1.0f)))
    }

    @Test
    fun beatsErrorWhenValueNotANumber() {
        onBeatsSlider().perform(setValue(1.0f))
        onBeatsEditLayout().check(matches(doesNotDisplayError()))

        onBeatsEdit().perform(ViewActions.replaceText("."))
        onBeatsEditLayout().check(matches(displaysError()))

        onBeatsSlider().check(matches(withValue(1.0f)))
    }

    @Test
    fun subdivisionsErrorWhenValueTooBig() {
        onSubdivisionsSlider().perform(setValue(1.0f))
        onSubdivisionsEditLayout().check(matches(doesNotDisplayError()))

        onSubdivisionsEdit().perform(ViewActions.replaceText("5"))

        onSubdivisionsEditLayout().check(matches(displaysError()))
        onSubdivisionsSlider().check(matches(withValue(1.0f)))
    }

    @Test
    fun subdivisionsErrorWhenValueNotANumber() {
        onSubdivisionsSlider().perform(setValue(1.0f))
        onSubdivisionsEditLayout().check(matches(doesNotDisplayError()))

        onSubdivisionsEdit().perform(ViewActions.replaceText("."))

        onSubdivisionsEditLayout().check(matches(displaysError()))
        onSubdivisionsSlider().check(matches(withValue(1.0f)))
    }

    @Test
    fun tempoErrorWhenValueTooBig() {
        onTempoSlider().perform(setValue(40.0f))
        onTempoEditLayout().check(matches(doesNotDisplayError()))

        onTempoEdit().perform(ViewActions.replaceText("209"))

        onTempoEditLayout().check(matches(displaysError()))
        onTempoSlider().check(matches(withValue(40.0f)))
    }

    @Test
    fun tempoErrorWhenValueNotANumber() {
        onTempoSlider().perform(setValue(40.0f))
        onTempoEditLayout().check(matches(doesNotDisplayError()))

        onTempoEdit().perform(ViewActions.replaceText("."))

        onTempoEditLayout().check(matches(displaysError()))
        onTempoSlider().check(matches(withValue(40.0f)))
    }

    @Test
    fun tempoMarkings() {
        applyTempo(40)
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

        applyTempo(208)
        verifyTempoMarking(R.string.tempo_marking_prestissimo)
    }

    private fun applyTempo(tempo: Int) {
        onTempoSlider().perform(setValue(tempo.toFloat()))
    }

    private fun verifyTempoMarking(@StringRes resourceId: Int) {
        onTempoMarkingText().check(matches(withText(resourceId)))
    }

    private fun onBeatsSlider() = onView(withId(R.id.beats_slider))
    private fun onBeatsEdit() = onView(withId(R.id.beats_edit))
    private fun onBeatsEditLayout() = onView(withId(R.id.beats_edit_layout))
    private fun onSubdivisionsSlider() = onView(withId(R.id.subdivisions_slider))
    private fun onSubdivisionsEdit() = onView(withId(R.id.subdivisions_edit))
    private fun onSubdivisionsEditLayout() = onView(withId(R.id.subdivisions_edit_layout))
    private fun onTempoSlider() = onView(withId(R.id.tempo_slider))
    private fun onTempoEdit() = onView(withId(R.id.tempo_edit))
    private fun onTempoEditLayout() = onView(withId(R.id.tempo_edit_layout))
    private fun onTempoMarkingText() = onView(withId(R.id.tempo_marking_text))
}
