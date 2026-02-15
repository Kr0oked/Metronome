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

import android.Manifest
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.bobek.metronome.ui.TestConstants
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
abstract class AbstractAndroidTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    protected fun applyTempo(tempo: Int) {
        onTempoSlider().setProgress(tempo.toFloat())
    }

    protected fun verifyTempoMarking(@StringRes resourceId: Int) {
        val expectedText = composeTestRule.activity.getString(resourceId)
        onTempoMarkingText().assertTextEquals(expectedText)
    }

    protected fun SemanticsNodeInteraction.setProgress(value: Float) {
        performSemanticsAction(SemanticsActions.SetProgress) { it(value) }
    }

    protected fun SemanticsNodeInteraction.assertProgress(value: Float, range: ClosedFloatingPointRange<Float>) {
        assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(value, range)))
    }

    protected fun SemanticsNodeInteraction.assertHasError() {
        assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    protected fun SemanticsNodeInteraction.assertHasNoError() {
        assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
    }

    protected fun onLoadingIndicator(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.LOADING_INDICATOR)

    protected fun onContent(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.CONTENT)

    protected fun onBeatsSlider(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.BEATS_SLIDER)

    protected fun onBeatsEdit(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.BEATS_EDIT)

    protected fun onSubdivisionsSlider(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.SUBDIVISIONS_SLIDER)

    protected fun onSubdivisionsEdit(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.SUBDIVISIONS_EDIT)

    protected fun onTempoSlider(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.TEMPO_SLIDER)

    protected fun onTempoEdit(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.TEMPO_EDIT)

    protected fun onTempoMarkingText(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.TEMPO_MARKING_TEXT)
}
