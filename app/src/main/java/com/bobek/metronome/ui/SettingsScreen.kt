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

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bobek.metronome.BuildConfig
import com.bobek.metronome.R
import com.bobek.metronome.data.AppNightMode
import com.bobek.metronome.data.Sound
import com.bobek.metronome.view.model.MetronomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MetronomeViewModel,
    onBackClick: () -> Unit,
    onThirdPartyLicensesClick: () -> Unit
) {
    val context = LocalContext.current
    val emphasizeFirstBeat by viewModel.emphasizeFirstBeat.observeAsState(true)
    val sound by viewModel.sound.observeAsState(Sound.SQUARE_WAVE)
    val nightMode by viewModel.nightMode.observeAsState(AppNightMode.FOLLOW_SYSTEM)

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
            SettingsCategory(title = stringResource(R.string.metronome))

            ListItem(
                headlineContent = { Text(stringResource(R.string.emphasize_first_beat)) },
                trailingContent = {
                    Switch(
                        checked = emphasizeFirstBeat,
                        onCheckedChange = { viewModel.emphasizeFirstBeat.value = it }
                    )
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.sound_square_wave)) },
                supportingContent = { Text(getSoundLabel(sound)) },
                modifier = Modifier.clickable { showSoundDialog = true }
            )

            HorizontalDivider()
            SettingsCategory(title = stringResource(R.string.display))

            ListItem(
                headlineContent = { Text(stringResource(R.string.night_mode)) },
                supportingContent = { Text(getNightModeLabel(nightMode)) },
                modifier = Modifier.clickable { showNightModeDialog = true }
            )

            HorizontalDivider()
            SettingsCategory(title = stringResource(R.string.about))

            ListItem(
                headlineContent = { Text(stringResource(R.string.author)) },
                supportingContent = { Text(stringResource(R.string.author_name)) },
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:philipp.bobek@mailbox.org"))
                    context.startActivity(intent)
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.license)) },
                supportingContent = { Text(stringResource(R.string.license_name)) },
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gnu.org/licenses/gpl-3.0.txt"))
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
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Kr0oked/Metronome"))
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
        val soundEntries = stringArrayResource(R.array.sound_entries)
        val soundValues = stringArrayResource(R.array.sound_values)

        SingleChoiceDialog(
            title = stringResource(R.string.sound_square_wave),
            entries = soundEntries.toList(),
            values = soundValues.toList(),
            currentValue = sound.preferenceValue,
            onValueSelected = { newValue ->
                viewModel.sound.value = Sound.forPreferenceValue(newValue)
                showSoundDialog = false
            },
            onDismiss = { showSoundDialog = false }
        )
    }

    if (showNightModeDialog) {
        val nightModeEntries = stringArrayResource(R.array.night_mode_entries)
        val nightModeValues = stringArrayResource(R.array.night_mode_values)

        SingleChoiceDialog(
            title = stringResource(R.string.night_mode),
            entries = nightModeEntries.toList(),
            values = nightModeValues.toList(),
            currentValue = nightMode.preferenceValue,
            onValueSelected = { newValue ->
                viewModel.setNightMode(AppNightMode.forPreferenceValue(newValue))
                showNightModeDialog = false
            },
            onDismiss = { showNightModeDialog = false }
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

@Composable
fun SingleChoiceDialog(
    title: String,
    entries: List<String>,
    values: List<String>,
    currentValue: String,
    onValueSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                entries.forEachIndexed { index, entry ->
                    val value = values[index]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (value == currentValue),
                                onClick = { onValueSelected(value) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (value == currentValue),
                            onClick = null
                        )
                        Text(
                            text = entry,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
fun getSoundLabel(sound: Sound): String {
    return when (sound) {
        Sound.SQUARE_WAVE -> stringResource(R.string.sound_square_wave)
        Sound.SINE_WAVE -> stringResource(R.string.sound_sine_wave)
        Sound.RISSET_DRUM -> stringResource(R.string.sound_risset_drum)
        Sound.PLUCK -> stringResource(R.string.sound_pluck)
    }
}

@Composable
fun getNightModeLabel(nightMode: AppNightMode): String {
    return when (nightMode) {
        AppNightMode.FOLLOW_SYSTEM -> stringResource(R.string.night_mode_follow_system)
        AppNightMode.NO -> stringResource(R.string.night_mode_no)
        AppNightMode.YES -> stringResource(R.string.night_mode_yes)
    }
}
