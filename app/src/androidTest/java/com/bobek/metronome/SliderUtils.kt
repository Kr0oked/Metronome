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

package com.bobek.metronome

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.BoundedMatcher
import com.google.android.material.slider.Slider
import org.hamcrest.Description
import org.hamcrest.Matcher

object SliderUtils {

    fun withValue(value: Float): Matcher<View> {
        return object : BoundedMatcher<View, Slider>(Slider::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("Slider value is ").appendValue(value)
            }

            override fun matchesSafely(slider: Slider): Boolean {
                return slider.value == value
            }
        }
    }

    fun setValue(value: Float): ViewAction {
        return object : ViewAction {
            override fun getDescription(): String {
                return "Sets Slider value to $value"
            }

            override fun getConstraints(): Matcher<View> {
                return androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(Slider::class.java)
            }

            override fun perform(uiController: UiController, view: View) {
                val slider = view as Slider
                slider.value = value
            }
        }
    }
}
