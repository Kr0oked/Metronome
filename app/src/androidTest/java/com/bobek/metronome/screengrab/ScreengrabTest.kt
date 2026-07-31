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

package com.bobek.metronome.screengrab

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.filters.LargeTest
import com.bobek.metronome.data.AppNightMode
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo
import com.bobek.metronome.ui.ComposeAppViewModel
import com.bobek.metronome.ui.MainContent
import com.bobek.metronome.ui.metronome.ComposeMetronomeViewModel
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.cleanstatusbar.CleanStatusBar
import tools.fastlane.screengrab.cleanstatusbar.IconVisibility
import tools.fastlane.screengrab.locale.LocaleTestRule

@LargeTest
class ScreengrabTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Rule
    @JvmField
    val localeTestRule = LocaleTestRule()

    @Test
    fun grabScreenshot() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

        val appViewModel = ComposeAppViewModel(AppNightMode.NO)

        composeTestRule.setContent {
            MainContent(
                appViewModel = appViewModel,
                metronomeViewModel = ComposeMetronomeViewModel(
                    beats = Beats(),
                    subdivisions = Subdivisions(),
                    tempo = Tempo(),
                    emphasizeFirstBeat = true,
                    playing = false,
                    connected = true
                )
            )
        }
        composeTestRule.waitForIdle()
        enableCleanStatusBar()
        Screengrab.screenshot("1")

        appViewModel.setNightMode(AppNightMode.YES)
        composeTestRule.waitForIdle()
        enableCleanStatusBar()
        Screengrab.screenshot("2")
    }

    companion object {

        @BeforeClass
        @JvmStatic
        fun beforeAll() {
            enableCleanStatusBar()
        }

        @AfterClass
        @JvmStatic
        fun afterAll() {
            CleanStatusBar.disable()
        }

        /**
         * The mobile network icon doesn't reliably respect the app's light/dark status bar
         * appearance on all system images, sometimes rendering white regardless of theme.
         * Hiding it avoids that inconsistency instead of chasing it.
         */
        private fun enableCleanStatusBar() {
            CleanStatusBar()
                .setMobileNetworkVisibility(IconVisibility.HIDE)
                .enable()
        }
    }
}
