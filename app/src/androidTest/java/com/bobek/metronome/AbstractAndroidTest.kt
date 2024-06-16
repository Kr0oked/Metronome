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

import android.Manifest
import android.content.Intent
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
abstract class AbstractAndroidTest {

    private val intent: Intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)

    @get:Rule
    var activityRule = ActivityScenarioRule<MainActivity>(intent)

    @get:Rule
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    protected fun applyTempo(tempo: Int) {
        onTempoSlider().perform(SliderUtils.setValue(tempo.toFloat()))
    }

    protected fun verifyTempoMarking(@StringRes resourceId: Int) {
        onTempoMarkingText().check(ViewAssertions.matches(ViewMatchers.withText(resourceId)))
    }

    protected fun onBeatsSlider(): ViewInteraction = onView(withId(R.id.beats_slider))
    protected fun onBeatsEdit(): ViewInteraction = onView(withId(R.id.beats_edit))
    protected fun onBeatsEditLayout(): ViewInteraction = onView(withId(R.id.beats_edit_layout))
    protected fun onSubdivisionsSlider(): ViewInteraction = onView(withId(R.id.subdivisions_slider))
    protected fun onSubdivisionsEdit(): ViewInteraction = onView(withId(R.id.subdivisions_edit))
    protected fun onSubdivisionsEditLayout(): ViewInteraction = onView(withId(R.id.subdivisions_edit_layout))
    protected fun onTempoSlider(): ViewInteraction = onView(withId(R.id.tempo_slider))
    protected fun onTempoEdit(): ViewInteraction = onView(withId(R.id.tempo_edit))
    protected fun onTempoEditLayout(): ViewInteraction = onView(withId(R.id.tempo_edit_layout))
    protected fun onTempoMarkingText(): ViewInteraction = onView(withId(R.id.tempo_marking_text))
}
