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

package com.bobek.metronome.ui

import android.annotation.SuppressLint
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bobek.metronome.ComposeMetronomeViewModel
import com.bobek.metronome.IMetronomeViewModel
import com.bobek.metronome.R
import com.bobek.metronome.data.AppNightMode
import com.bobek.metronome.ui.licenses.ThirdPartyLicenseScreen
import com.bobek.metronome.ui.licenses.ThirdPartyLicenseScreenState
import com.bobek.metronome.ui.licenses.ThirdPartyLicensesScreen
import com.bobek.metronome.ui.metronome.MetronomeScreen
import com.bobek.metronome.ui.settings.SettingsScreen
import com.bobek.metronome.ui.theme.AppTheme
import de.philipp_bobek.oss_licenses_parser.OssLicensesParser

@Composable
@PreviewScreenSizes
@SuppressLint("LocalContextResourcesRead")
fun MainContent(
    viewModel: IMetronomeViewModel = ComposeMetronomeViewModel(connected = true)
) {
    val navController = rememberNavController()
    val nightMode by viewModel.getNightMode().collectAsState()
    val playing by viewModel.getPlaying().collectAsState()

    val isDarkTheme = when (nightMode) {
        AppNightMode.NO -> false
        AppNightMode.YES -> true
        AppNightMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }

    val activity = LocalActivity.current
    LaunchedEffect(playing) {
        if (playing) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    AppTheme(darkTheme = isDarkTheme) {
        NavHost(navController = navController, startDestination = "metronome") {
            composable("metronome") {
                MetronomeScreen(
                    viewModel = viewModel,
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onThirdPartyLicensesClick = { navController.navigate("licenses") }
                )
            }
            composable("licenses") {
                val context = LocalContext.current
                val licenses = remember {
                    context.resources
                        .openRawResource(R.raw.third_party_license_metadata)
                        .use(OssLicensesParser::parseMetadata)
                        .sortedBy { it.libraryName }
                }

                ThirdPartyLicensesScreen(
                    licenses = licenses,
                    onBackClick = { navController.popBackStack() },
                    onLicenseClick = { metadata ->
                        navController.navigate("license/${metadata.libraryName}")
                    }
                )
            }
            composable("license/{libraryName}") { backStackEntry ->
                val libraryName = backStackEntry.arguments?.getString("libraryName") ?: ""
                val context = LocalContext.current
                val licenseContent = remember(libraryName) {
                    val metadata = context.resources
                        .openRawResource(R.raw.third_party_license_metadata)
                        .use(OssLicensesParser::parseMetadata)
                        .find { it.libraryName == libraryName }

                    metadata?.let {
                        context.resources
                            .openRawResource(R.raw.third_party_licenses)
                            .use { stream -> OssLicensesParser.parseLicense(it, stream).licenseContent }
                    } ?: ""
                }

                ThirdPartyLicenseScreen(
                    state = ThirdPartyLicenseScreenState(
                        libraryName = libraryName,
                        licenseContent = licenseContent,
                    ),
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
