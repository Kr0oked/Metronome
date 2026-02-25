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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.bobek.metronome.ComposeMetronomeViewModel
import com.bobek.metronome.IMetronomeViewModel
import com.bobek.metronome.R
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo
import com.bobek.metronome.ui.TestConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@PreviewScreenSizes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetronomeScreen(
    @PreviewParameter(MetronomeScreenViewModelProvider::class) viewModel: IMetronomeViewModel,
    onSettingsClick: () -> Unit = {}
) {
    val connected by viewModel.getConnected().collectAsState()
    val playing by viewModel.getPlaying().collectAsState()
    val currentTick by viewModel.getCurrentTick().collectAsState()
    val beats by viewModel.getBeats().collectAsState()
    val subdivisions by viewModel.getSubdivisions().collectAsState()
    val gaps by viewModel.getGaps().collectAsState()
    val tempo by viewModel.getTempo().collectAsState()

    val hapticFeedback = LocalHapticFeedback.current

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
        }
    ) { padding ->
        if (!connected) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
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
                    .padding(16.dp)
                    .testTag(TestConstants.CONTENT),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Tick Visualization
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in Beats.MIN..beats.value) {
                        TickVisualization(
                            state = TickVisualizationState(
                                isBlinking = currentTick?.beat == i,
                                isGap = gaps.value.contains(i)
                            ),
                            onGapToggle = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                viewModel.setGaps(gaps.toggle(i))
                            }
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Beats Control
                    ControlSection(
                        label = stringResource(R.string.beats_label),
                        value = beats.value,
                        onValueChange = { viewModel.setBeats(Beats(it)) },
                        valueRange = Beats.valueRange,
                        sliderTestTag = TestConstants.BEATS_SLIDER,
                        editTestTag = TestConstants.BEATS_EDIT
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subdivisions Control
                    ControlSection(
                        label = stringResource(R.string.subdivisions_label),
                        value = subdivisions.value,
                        onValueChange = { viewModel.setSubdivisions(Subdivisions(it)) },
                        valueRange = Subdivisions.valueRange,
                        sliderTestTag = TestConstants.SUBDIVISIONS_SLIDER,
                        editTestTag = TestConstants.SUBDIVISIONS_EDIT
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tempo Control
                    ControlSection(
                        label = stringResource(R.string.tempo_label),
                        marking = stringResource(tempo.marking.labelResourceId),
                        value = tempo.value,
                        onValueChange = { viewModel.setTempo(Tempo(it)) },
                        valueRange = Tempo.valueRange,
                        sliderTestTag = TestConstants.TEMPO_SLIDER,
                        editTestTag = TestConstants.TEMPO_EDIT,
                        markingTestTag = TestConstants.TEMPO_MARKING_TEXT
                    )
                }

                // Tempo Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TempoActionButton(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = stringResource(R.string.decrement_tempo_button_description),
                        onClick = { viewModel.changeTempo(-1) },
                        onLongClick = { viewModel.changeTempo(-10) }
                    )
                    FilledIconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            viewModel.tapTempo()
                        },
                        modifier = Modifier.size(dimensionResource(R.dimen.action_button_icon_size)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_drum),
                            contentDescription = stringResource(R.string.tap_tempo_button_description)
                        )
                    }
                    TempoActionButton(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.increment_tempo_button_description),
                        onClick = { viewModel.changeTempo(1) },
                        onLongClick = { viewModel.changeTempo(10) }
                    )
                }

                // Start/Stop Button
                FilledIconButton(
                    onClick = { viewModel.startStop() },
                    modifier = Modifier.size(dimensionResource(R.dimen.action_button_icon_size)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.start_stop_button_description),
                    )
                }
            }
        }
    }
}

@Composable
fun ControlSection(
    label: String = "",
    marking: String = "",
    value: Int = 0,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange = IntRange.EMPTY,
    sliderTestTag: String = "",
    editTestTag: String = "",
    markingTestTag: String = ""
) {
    var text by rememberSaveable { mutableStateOf(value.toString()) }

    var previousValue by rememberSaveable { mutableIntStateOf(value) }
    if (value != previousValue) {
        text = value.toString()
        @Suppress("AssignedValueIsNeverRead")
        previousValue = value
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text(marking, style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag(markingTestTag))
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                steps = valueRange.last - valueRange.first - 1,
                modifier = Modifier.testTag(sliderTestTag)
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = { value ->
                text = value
                    .dropWhile { it == '0' }
                    .take(3)
                if (isValidNumber(text, valueRange)) {
                    onValueChange(text.toInt())
                }
            },
            modifier = Modifier
                .width(72.dp)
                .padding(start = 8.dp)
                .testTag(editTestTag),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            isError = !isValidNumber(text, valueRange)
        )
    }
}

private fun isValidNumber(text: String, range: IntRange): Boolean =
    text.toIntOrNull()
        ?.let { it in range }
        ?: false

@Composable
fun TempoActionButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val viewConfiguration = LocalViewConfiguration.current
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(interactionSource) {
        var isLongClick = false

        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isLongClick = false
                    delay(viewConfiguration.longPressTimeoutMillis)
                    isLongClick = true
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }

                is PressInteraction.Release -> {
                    if (isLongClick.not()) {
                        onClick()
                    }
                }

                is PressInteraction.Cancel -> {
                    isLongClick = true
                }
            }
        }
    }

    FilledIconButton(
        onClick = {},
        modifier = Modifier
            .size(dimensionResource(R.dimen.action_button_icon_size)),
        shape = RoundedCornerShape(10.dp),
        interactionSource = interactionSource
    ) {
        Icon(imageVector = imageVector, contentDescription = contentDescription)
    }
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
