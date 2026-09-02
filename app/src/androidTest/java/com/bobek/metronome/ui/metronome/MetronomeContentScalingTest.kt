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

package com.bobek.metronome.ui.metronome

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.bobek.metronome.R
import com.bobek.metronome.ui.TestConstants
import com.bobek.metronome.ui.metronome.MetronomeContentScalingTest.Companion.MAX_FONT_SCALE
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/**
 * Regression coverage for a bug where maxing out the Android accessibility "Font size" and
 * "Display size" settings pushed the metronome's action buttons off-screen and squeezed the
 * tempo marking text instead of the content adapting, making the app unusable.
 */
@LargeTest
class MetronomeContentScalingTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun actionButtonsRemainVisibleInPortraitAtMaxAccessibilityScale() {
        setContentAtMaxAccessibilityScale(windowSize = PORTRAIT_WINDOW_SIZE)

        onDecrementTempoButton().assertIsDisplayed()
        onTapTempoButton().assertIsDisplayed()
        onIncrementTempoButton().assertIsDisplayed()
        onStartStopButton().assertIsDisplayed()
    }

    @Test
    fun actionButtonsRemainVisibleInLandscapeAtMaxAccessibilityScale() {
        setContentAtMaxAccessibilityScale(windowSize = LANDSCAPE_WINDOW_SIZE)

        onDecrementTempoButton().assertIsDisplayed()
        onTapTempoButton().assertIsDisplayed()
        onIncrementTempoButton().assertIsDisplayed()
        onStartStopButton().assertIsDisplayed()
    }

    @Test
    fun tempoLabelAndMarkingDoNotOverlapInPortraitAtMaxAccessibilityScale() {
        setContentAtMaxAccessibilityScale(windowSize = PORTRAIT_WINDOW_SIZE)

        assertTempoLabelAndMarkingDoNotOverlap()
    }

    @Test
    fun tempoLabelAndMarkingDoNotOverlapInLandscapeAtMaxAccessibilityScale() {
        setContentAtMaxAccessibilityScale(windowSize = LANDSCAPE_WINDOW_SIZE)

        assertTempoLabelAndMarkingDoNotOverlap()
    }

    private fun setContentAtMaxAccessibilityScale(windowSize: DpSize) {
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(windowSize) then
                        DeviceConfigurationOverride.FontScale(MAX_FONT_SCALE)
            ) {
                MetronomeContent(viewModel = ComposeMetronomeViewModel(connected = true))
            }
        }
    }

    private fun assertTempoLabelAndMarkingDoNotOverlap() {
        val labelBounds = onNodeWithText(getString(R.string.tempo_label)).fetchSemanticsNode().boundsInRoot
        val markingBounds = onNodeWithTag(TestConstants.TEMPO_MARKING_TEXT).fetchSemanticsNode().boundsInRoot

        assertFalse(
            "Expected tempo label bounds $labelBounds to not overlap tempo marking bounds $markingBounds",
            labelBounds.overlaps(markingBounds)
        )
    }

    private fun onNodeWithText(text: String): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(text)

    private fun onNodeWithTag(testTag: String): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(testTag)

    private fun onDecrementTempoButton(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithContentDescription(getString(R.string.decrement_tempo_button_description))

    private fun onIncrementTempoButton(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithContentDescription(getString(R.string.increment_tempo_button_description))

    private fun onTapTempoButton(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithContentDescription(getString(R.string.tap_tempo_button_description))

    private fun onStartStopButton(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithContentDescription(getString(R.string.start_stop_button_description))

    private fun getString(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    companion object {

        /** Android's accessibility "Font size" setting maxes out at 200%. */
        private const val MAX_FONT_SCALE = 2.0f

        /**
         * Apparent window sizes approximating a phone with the "Display size" accessibility
         * setting maxed out, which shrinks the effective dp budget available to the UI on top of
         * [MAX_FONT_SCALE].
         */
        private val PORTRAIT_WINDOW_SIZE = DpSize(280.dp, 600.dp)
        private val LANDSCAPE_WINDOW_SIZE = DpSize(600.dp, 280.dp)
    }
}
