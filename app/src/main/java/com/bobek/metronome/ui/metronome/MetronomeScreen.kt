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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.bobek.metronome.ComposeMetronomeViewModel
import com.bobek.metronome.IMetronomeViewModel
import com.bobek.metronome.R
import com.bobek.metronome.ui.TestConstants

@Composable
@PreviewScreenSizes
@OptIn(ExperimentalMaterial3Api::class)
fun MetronomeScreen(
    @PreviewParameter(MetronomeScreenViewModelProvider::class) viewModel: IMetronomeViewModel,
    onSettingsClick: () -> Unit = {}
) {
    val connected by viewModel.getConnected().collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.metronome)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        },
        content = { padding ->
            if (!connected) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding)
                        .testTag(TestConstants.LOADING_INDICATOR),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding)
                        .testTag(TestConstants.CONTENT),
                ) {
                    MetronomeContent(viewModel = viewModel)
                }
            }
        }
    )
}

private class MetronomeScreenViewModelProvider : PreviewParameterProvider<IMetronomeViewModel> {
    override val values: Sequence<IMetronomeViewModel> = sequenceOf(
        ComposeMetronomeViewModel(connected = true),
        ComposeMetronomeViewModel(connected = false)
    )

    override fun getDisplayName(index: Int): String? =
        when (index) {
            0 -> "Connected"
            1 -> "Disconnected"
            else -> null
        }
}
