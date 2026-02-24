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

package com.bobek.metronome.ui.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.bobek.metronome.BuildConfig
import com.bobek.metronome.ComposeMetronomeViewModel
import com.bobek.metronome.IMetronomeViewModel
import com.bobek.metronome.R
import com.bobek.metronome.data.AppNightMode
import com.bobek.metronome.data.Sound

@PreviewScreenSizes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: IMetronomeViewModel = ComposeMetronomeViewModel(),
    onBackClick: () -> Unit = {},
    onThirdPartyLicensesClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val emphasizeFirstBeat by viewModel.getEmphasizeFirstBeat().collectAsState()
    val sound by viewModel.getSound().collectAsState()
    val nightMode by viewModel.getNightMode().collectAsState()

    var showSoundDialog by remember { mutableStateOf(false) }
    var showNightModeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsCategory(title = stringResource(R.string.metronome)) // TODO: icons

            ListItem(
                headlineContent = { Text(stringResource(R.string.emphasize_first_beat)) },
                trailingContent = {
                    Switch(
                        checked = emphasizeFirstBeat,
                        onCheckedChange = { viewModel.setEmphasizeFirstBeat(it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.sound_square_wave)) },
                supportingContent = { Text(stringResource(sound.labelResourceId)) },
                modifier = Modifier.clickable { showSoundDialog = true }
            )

            HorizontalDivider()
            SettingsCategory(title = stringResource(R.string.display))

            ListItem(
                headlineContent = { Text(stringResource(R.string.night_mode)) },
                supportingContent = { Text(stringResource(nightMode.labelResourceId)) },
                modifier = Modifier.clickable { showNightModeDialog = true }
            )

            HorizontalDivider()
            SettingsCategory(title = stringResource(R.string.about))

            ListItem(
                headlineContent = { Text(stringResource(R.string.author)) },
                supportingContent = { Text(stringResource(R.string.author_name)) },
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, "mailto:philipp.bobek@mailbox.org".toUri())
                    context.startActivity(intent)
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.license)) },
                supportingContent = { Text(stringResource(R.string.license_name)) },
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, "https://www.gnu.org/licenses/gpl-3.0.txt".toUri())
                    context.startActivity(intent)
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.third_party_licenses)) },
                supportingContent = { Text(stringResource(R.string.third_party_licenses_summary)) },
                modifier = Modifier.clickable { onThirdPartyLicensesClick() }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.source_code)) },
                supportingContent = { Text(stringResource(R.string.source_code_name)) },
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, "https://github.com/Kr0oked/Metronome".toUri())
                    context.startActivity(intent)
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.version)) },
                supportingContent = { Text(BuildConfig.VERSION_NAME) }
            )
        }
    }

    if (showSoundDialog) {
        SingleChoiceDialog(
            SingleChoiceDialogState(
                title = stringResource(R.string.sound),
                entries = Sound.entries,
                currentValue = sound.preferenceValue
            ),
            onValueSelected = { newValue ->
                viewModel.setSound(newValue)
                @Suppress("AssignedValueIsNeverRead")
                showSoundDialog = false
            },
            onDismiss = {
                @Suppress("AssignedValueIsNeverRead")
                showSoundDialog = false
            }
        )
    }

    if (showNightModeDialog) {
        SingleChoiceDialog(
            SingleChoiceDialogState(
                title = stringResource(R.string.night_mode),
                entries = AppNightMode.entries,
                currentValue = nightMode.preferenceValue
            ),
            onValueSelected = { newValue ->
                viewModel.setNightMode(newValue)
                @Suppress("AssignedValueIsNeverRead")
                showNightModeDialog = false
            },
            onDismiss = {
                @Suppress("AssignedValueIsNeverRead")
                showNightModeDialog = false
            }
        )
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}
