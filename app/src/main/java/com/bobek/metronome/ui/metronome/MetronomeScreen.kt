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

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import com.bobek.metronome.ComposeMetronomeViewModel
import com.bobek.metronome.IMetronomeViewModel
import com.bobek.metronome.R
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo
import com.bobek.metronome.ui.TestConstants
import kotlinx.coroutines.launch

@PreviewScreenSizes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetronomeScreen(
    @PreviewParameter(MetronomeScreenViewModelProvider::class) viewModel: IMetronomeViewModel,
    onSettingsClick: () -> Unit = {}
) {
    val connected by viewModel.connected.observeAsState(false)
    val playing by viewModel.playing.observeAsState(false)
    val currentTick by viewModel.currentTick.observeAsState()

    val beats by viewModel.beatsData.observeAsState(Beats())
    val beatsText by viewModel.beatsText.observeAsState("")
    val beatsError by viewModel.beatsTextError.observeAsState(false)

    val subdivisions by viewModel.subdivisionsData.observeAsState(Subdivisions())
    val subdivisionsText by viewModel.subdivisionsText.observeAsState("")
    val subdivisionsError by viewModel.subdivisionsTextError.observeAsState(false)

    val tempo by viewModel.tempoData.observeAsState(Tempo())
    val tempoText by viewModel.tempoText.observeAsState("")
    val tempoError by viewModel.tempoTextError.observeAsState(false)

    val gaps by viewModel.gapsData.observeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.metronome)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
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
                    for (i in 1..8) {
                        if (i <= beats.value) {
                            TickVisualization(
                                state = TickVisualizationState(
                                    isBlinking = currentTick?.beat == i,
                                    isGap = gaps?.value?.contains(i) == true
                                ),
                                onGapToggle = {
                                    toggleGap(viewModel, i)
                                }
                            )
                        } else {
                            Spacer(modifier = Modifier.size(40.dp))
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Beats Control
                    ControlSection(
                        label = stringResource(R.string.beats_label),
                        value = beats.value.toFloat(),
                        onValueChange = { viewModel.beatsData.value = Beats(it.toInt()) },
                        valueRange = Beats.MIN.toFloat()..Beats.MAX.toFloat(),
                        steps = Beats.MAX - Beats.MIN - 1,
                        textValue = beatsText,
                        onTextChange = { viewModel.onBeatsTextChanged(it) },
                        isError = beatsError,
                        sliderTestTag = "beats_slider",
                        editTestTag = "beats_edit"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subdivisions Control
                    ControlSection(
                        label = stringResource(R.string.subdivisions_label),
                        value = subdivisions.value.toFloat(),
                        onValueChange = { viewModel.subdivisionsData.value = Subdivisions(it.toInt()) },
                        valueRange = Subdivisions.MIN.toFloat()..Subdivisions.MAX.toFloat(),
                        steps = Subdivisions.MAX - Subdivisions.MIN - 1,
                        textValue = subdivisionsText,
                        onTextChange = { viewModel.onSubdivisionsTextChanged(it) },
                        isError = subdivisionsError,
                        sliderTestTag = "subdivisions_slider",
                        editTestTag = "subdivisions_edit"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tempo Control
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.tempo_label), style = MaterialTheme.typography.labelLarge)
                            tempo.marking.labelResourceId.let {
                                Text(
                                    stringResource(it),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.testTag(TestConstants.TEMPO_MARKING_TEXT)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = tempo.value.toFloat(),
                                onValueChange = { viewModel.tempoData.value = Tempo(it.toInt()) },
                                valueRange = Tempo.MIN.toFloat()..Tempo.MAX.toFloat(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag(TestConstants.TEMPO_SLIDER)
                            )
                            OutlinedTextField(
                                value = tempoText,
                                onValueChange = { viewModel.onTempoTextChanged(it) },
                                modifier = Modifier
                                    .width(80.dp)
                                    .padding(start = 8.dp)
                                    .testTag(TestConstants.TEMPO_EDIT)
                                    .semantics { if (tempoError) error("Invalid input") },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                isError = tempoError
                            )
                        }
                    }
                }

                // Tempo Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TempoActionButton(
                        iconRes = R.drawable.ic_remove,
                        onClick = { changeTempo(viewModel, -1) },
                        onLongClick = { repeat(10) { changeTempo(viewModel, -1) } }
                    )
                    IconButton(
                        onClick = { viewModel.tapTempo() },
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_drum),
                            contentDescription = stringResource(R.string.tap_tempo_button_description),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    TempoActionButton(
                        iconRes = R.drawable.ic_add,
                        onClick = { changeTempo(viewModel, 1) },
                        onLongClick = { repeat(10) { changeTempo(viewModel, 1) } }
                    )
                }

                // Start/Stop Button
                IconButton(
                    onClick = { viewModel.startStop() },
                    modifier = Modifier
                        .size(64.dp)
                ) {
                    Icon(
                        painter = if (playing) painterResource(R.drawable.ic_pause) else painterResource(R.drawable.ic_play_arrow),
                        contentDescription = stringResource(R.string.start_stop_button_description),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ControlSection(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    textValue: String,
    onTextChange: (String) -> Unit,
    isError: Boolean,
    sliderTestTag: String,
    editTestTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier
                    .weight(1f)
                    .testTag(sliderTestTag)
            )
            OutlinedTextField(
                value = textValue,
                onValueChange = onTextChange,
                modifier = Modifier
                    .width(64.dp)
                    .padding(start = 8.dp)
                    .testTag(editTestTag)
                    .semantics { if (isError) error("Invalid input") }, // TODO: translate
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = isError
            )
        }
    }
}

@Composable
fun TempoActionButton(
    iconRes: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    IconButton(
        onClick = onClick,
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { onClick() },
                onLongPress = {
                    scope.launch {
                        onLongClick()
                    }
                }
            )
        }
    ) {
        Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(32.dp))
    }
}

private fun toggleGap(viewModel: IMetronomeViewModel, position: Int) {
    when (position) {
        1 -> viewModel.gap1.value = viewModel.gap1.value?.not()
        2 -> viewModel.gap2.value = viewModel.gap2.value?.not()
        3 -> viewModel.gap3.value = viewModel.gap3.value?.not()
        4 -> viewModel.gap4.value = viewModel.gap4.value?.not()
        5 -> viewModel.gap5.value = viewModel.gap5.value?.not()
        6 -> viewModel.gap6.value = viewModel.gap6.value?.not()
        7 -> viewModel.gap7.value = viewModel.gap7.value?.not()
        8 -> viewModel.gap8.value = viewModel.gap8.value?.not()
    }
}

private fun changeTempo(viewModel: IMetronomeViewModel, delta: Int) {
    viewModel.tempoData.value?.value?.let {
        val newVal = it + delta
        if (newVal in Tempo.MIN..Tempo.MAX) {
            viewModel.tempoData.value = Tempo(newVal)
        }
    }
}

private class MetronomeScreenViewModelProvider : PreviewParameterProvider<IMetronomeViewModel> {
    override val values: Sequence<IMetronomeViewModel> = sequenceOf(
        ComposeMetronomeViewModel(connected = MutableLiveData(true)),
        ComposeMetronomeViewModel(connected = MutableLiveData(false))
    )

    override fun getDisplayName(index: Int): String? =
        when (index) {
            0 -> "Connected"
            1 -> "Disconnected"
            else -> null
        }
}
