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

import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bobek.metronome.R
import com.bobek.metronome.data.AppNightMode
import com.bobek.metronome.licenses.LicenseRepository
import com.bobek.metronome.ui.licenses.LicenseScreen
import com.bobek.metronome.ui.licenses.LicenseScreenState
import com.bobek.metronome.ui.licenses.ThirdPartyLicensesScreen
import com.bobek.metronome.ui.metronome.ComposeMetronomeViewModel
import com.bobek.metronome.ui.metronome.IMetronomeViewModel
import com.bobek.metronome.ui.metronome.MetronomeScreen
import com.bobek.metronome.ui.settings.SettingsScreen
import com.bobek.metronome.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
@PreviewScreenSizes
fun MainContent(
    appViewModel: IAppViewModel = ComposeAppViewModel(),
    metronomeViewModel: IMetronomeViewModel = ComposeMetronomeViewModel(connected = true)
) {
    val navController = rememberNavController()
    val nightMode by appViewModel.getNightModeFlow().collectAsState()
    val playing by metronomeViewModel.getPlayingFlow().collectAsState()

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
        val motionScheme = MaterialTheme.motionScheme
        val slideSpec = motionScheme.defaultSpatialSpec<IntOffset>()
        val fadeSpec = motionScheme.defaultEffectsSpec<Float>()
        val slideOffset = { fullOffset: Int -> fullOffset / 4 }

        NavHost(
            navController = navController,
            startDestination = "metronome",
            enterTransition = {
                slideIntoContainer(SlideDirection.Start, slideSpec, slideOffset) + fadeIn(fadeSpec)
            },
            exitTransition = {
                slideOutOfContainer(SlideDirection.Start, slideSpec, slideOffset) + fadeOut(fadeSpec)
            },
            popEnterTransition = {
                slideIntoContainer(SlideDirection.End, slideSpec, slideOffset) + fadeIn(fadeSpec)
            },
            popExitTransition = {
                slideOutOfContainer(SlideDirection.End, slideSpec, slideOffset) + fadeOut(fadeSpec)
            }
        ) {
            composable("metronome") {
                MetronomeScreen(
                    viewModel = metronomeViewModel,
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    appViewModel = appViewModel,
                    metronomeViewModel = metronomeViewModel,
                    onBackClick = { navController.popBackStack() },
                    onLicenseClick = { navController.navigate("license") },
                    onThirdPartyLicensesClick = { navController.navigate("licenses") }
                )
            }
            composable("license") {
                val resources = LocalResources.current
                val licenseContent by produceState(initialValue = "") {
                    value = withContext(Dispatchers.IO) {
                        LicenseRepository.getAppLicenseContent(resources)
                    }
                }

                LicenseScreen(
                    state = LicenseScreenState(
                        title = stringResource(R.string.license_name),
                        licenseContent = licenseContent,
                    ),
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("licenses") {
                val resources = LocalResources.current
                val libraryNames by produceState(initialValue = emptyList()) {
                    value = withContext(Dispatchers.IO) {
                        LicenseRepository.getThirdPartyLibraryNames(resources)
                    }
                }

                ThirdPartyLicensesScreen(
                    libraryNames = libraryNames,
                    onBackClick = { navController.popBackStack() },
                    onLibraryClick = { libraryName ->
                        navController.navigate("license/${Uri.encode(libraryName)}")
                    }
                )
            }
            composable("license/{libraryName}") { backStackEntry ->
                val libraryName = Uri.decode(backStackEntry.arguments?.getString("libraryName") ?: "")
                val resources = LocalResources.current
                val licenseContent by produceState(initialValue = "", key1 = libraryName) {
                    value = withContext(Dispatchers.IO) {
                        LicenseRepository.getThirdPartyLicenseContent(resources, libraryName)
                    }
                }

                LicenseScreen(
                    state = LicenseScreenState(
                        title = libraryName,
                        licenseContent = licenseContent,
                    ),
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
