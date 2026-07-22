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
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.bobek.metronome.ui.TestConstants
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
@LargeTest
class InstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Before
    fun setup() {
        waitUntilContentIsDisplayed()
    }

    @Test
    fun initialState() {
        onTopBarTitle().assertIsDisplayed()
        onSettingsButton().assertIsDisplayed()
    }

    @Test
    fun initialMetronomeState() {
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

    @Test
    fun navigatingToSettingsAndBackShowsMetronomeScreenAgain() {
        onSettingsButton().performClick()
        composeTestRule.waitForIdle()
        onTopBarTitle(R.string.settings).assertIsDisplayed()

        pressBack()
        composeTestRule.waitForIdle()
        onTopBarTitle().assertIsDisplayed()
    }

    @Test
    fun changingNightModeUpdatesSelectedTheme() {
        openSettings()

        onNightModeOption(R.string.night_mode_follow_system).assertIsDisplayed()

        selectNightMode(R.string.night_mode_yes)
        onNightModeOption(R.string.night_mode_yes).assertIsDisplayed()

        selectNightMode(R.string.night_mode_follow_system)
        onNightModeOption(R.string.night_mode_follow_system).assertIsDisplayed()
    }

    @Test
    fun navigatingToLicenseShowsGplLicenseTextAndBackReturnsToSettings() {
        openSettings()

        onLicenseListItem().performClick()
        composeTestRule.waitForIdle()

        onTopBarTitle(R.string.license_name).assertIsDisplayed()
        waitUntilTextExists("GNU GENERAL PUBLIC LICENSE")

        pressBack()
        composeTestRule.waitForIdle()
        onTopBarTitle(R.string.settings).assertIsDisplayed()
    }

    @Test
    fun navigatingToThirdPartyLicenseShowsApacheLicenseForMaterialSymbols() {
        openSettings()

        onThirdPartyLicensesListItem().performScrollTo().performClick()
        composeTestRule.waitForIdle()
        onTopBarTitle(R.string.third_party_licenses).assertIsDisplayed()

        scrollToListItem("Material Symbols")
        onListItem("Material Symbols").performClick()
        composeTestRule.waitForIdle()

        onTopBarTitle("Material Symbols").assertIsDisplayed()
        waitUntilTextExists("Apache License")

        pressBack()
        composeTestRule.waitForIdle()
        onTopBarTitle(R.string.third_party_licenses).assertIsDisplayed()
    }

    private fun waitUntilContentIsDisplayed() {
        composeTestRule.waitUntil(timeoutMillis = 15_000L) { onContent().isDisplayed() }
        composeTestRule.waitForIdle()
    }

    private fun verifyTempoMarking(@StringRes resourceId: Int) {
        val expectedText = composeTestRule.activity.getString(resourceId)
        onTempoMarkingText().assertTextEquals(expectedText)
    }

    private fun SemanticsNodeInteraction.setProgress(value: Float) {
        performSemanticsAction(SemanticsActions.SetProgress) { it(value) }
    }

    private fun SemanticsNodeInteraction.assertBeatsProgress(value: Float) {
        assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(value, 1f..8f, 6)))
    }

    private fun SemanticsNodeInteraction.assertSubdivisionsProgress(value: Float) {
        assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(value, 1f..4f, 2)))
    }

    private fun SemanticsNodeInteraction.assertTempoProgress(value: Float) {
        assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(value, 30f..252f, 221)))
    }

    private fun SemanticsNodeInteraction.assertHasError() {
        assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    private fun SemanticsNodeInteraction.assertHasNoError() {
        assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
    }

    private fun openSettings() {
        composeTestRule.waitForIdle()
        onSettingsButton().performClick()
        composeTestRule.waitForIdle()
    }

    private fun selectNightMode(@StringRes labelResId: Int) {
        composeTestRule.waitForIdle()
        onNightModeListItem().performClick()
        composeTestRule.waitForIdle()
        onNightModeOption(labelResId).performClick()
        composeTestRule.waitForIdle()
    }

    private fun onTopBarTitle(@StringRes titleResId: Int = R.string.metronome): SemanticsNodeInteraction =
        onTopBarTitle(getString(titleResId))

    private fun onTopBarTitle(title: String): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(title)

    private fun onSettingsButton(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithContentDescription(getString(R.string.settings))

    private fun onLoadingIndicator(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.LOADING_INDICATOR)

    private fun onContent(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.CONTENT)

    private fun onBeatsSlider(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.BEATS_SLIDER)

    private fun onBeatsEdit(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.BEATS_EDIT)

    private fun onSubdivisionsSlider(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.SUBDIVISIONS_SLIDER)

    private fun onSubdivisionsEdit(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.SUBDIVISIONS_EDIT)

    private fun onTempoSlider(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.TEMPO_SLIDER)

    private fun onTempoEdit(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.TEMPO_EDIT)

    private fun onTempoMarkingText(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(TestConstants.TEMPO_MARKING_TEXT)

    private fun onLicenseListItem(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(getString(R.string.license))

    private fun onThirdPartyLicensesListItem(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(getString(R.string.third_party_licenses))

    private fun onNightModeListItem(): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(getString(R.string.night_mode))

    private fun onNightModeOption(@StringRes labelResId: Int): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(getString(labelResId))

    private fun onListItem(text: String): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(text)

    private fun scrollToListItem(text: String) {
        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
    }

    private fun waitUntilTextExists(text: String, timeoutMillis: Long = 5_000) {
        composeTestRule.waitUntilAtLeastOneExists(hasText(text, substring = true), timeoutMillis = timeoutMillis)
    }

    private fun getString(@StringRes resId: Int): String = composeTestRule.activity.getString(resId)
}
