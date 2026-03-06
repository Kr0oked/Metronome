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

import android.content.res.Configuration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
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
import kotlin.math.ceil

@Composable
@PreviewScreenSizes
@OptIn(ExperimentalMaterial3Api::class)
fun MetronomeContent(viewModel: IMetronomeViewModel = ComposeMetronomeViewModel(connected = true)) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        LandscapeContent(viewModel)
    } else {
        PortraitContent(viewModel)
    }

}

@Composable
private fun LandscapeContent(viewModel: IMetronomeViewModel) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.root_layout_padding)),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            BeatsControlSection(viewModel)

            SubdivisionsControlSection(viewModel)

            TempoControlSection(viewModel)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.root_layout_padding)),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TickVisualizationArea(
                viewModel = viewModel,
                rows = 2
            )

            Spacer(modifier = Modifier.height(generalSpacing()))

            Column(verticalArrangement = Arrangement.spacedBy(generalSpacing())) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(generalSpacing()),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        DecrementTempoButton(
                            viewModel = viewModel,
                            modifier = Modifier.largeButton(),
                            iconSize = buttonIconSize()
                        )
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        IncrementTempoButton(
                            viewModel = viewModel,
                            modifier = Modifier.largeButton(),
                            iconSize = buttonIconSize()
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(generalSpacing()),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TapTempoButton(
                            viewModel = viewModel,
                            modifier = Modifier.largeButton(),
                            iconSize = buttonIconSize()
                        )
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        StartStopButton(
                            viewModel = viewModel,
                            modifier = Modifier.largeButton(),
                            iconSize = buttonIconSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortraitContent(viewModel: IMetronomeViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.root_layout_padding)),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        TickVisualizationArea(
            viewModel = viewModel,
            rows = 1
        )

        BeatsControlSection(viewModel)

        SubdivisionsControlSection(viewModel)

        TempoControlSection(viewModel)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(generalSpacing())
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                DecrementTempoButton(
                    viewModel = viewModel,
                    modifier = Modifier.largeButton(),
                    iconSize = buttonIconSize()
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                TapTempoButton(
                    viewModel = viewModel,
                    modifier = Modifier.largeButton(),
                    iconSize = buttonIconSize()
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                IncrementTempoButton(
                    viewModel = viewModel,
                    modifier = Modifier.largeButton(),
                    iconSize = buttonIconSize()
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(generalSpacing())
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                StartStopButton(
                    viewModel = viewModel,
                    modifier = Modifier.largeButton(),
                    iconSize = buttonIconSize()
                )
            }
        }
    }
}

@Composable
private fun TickVisualizationArea(
    viewModel: IMetronomeViewModel,
    rows: Int = 1
) {
    val maxItemsInEachRow = ceil(Beats.MAX_VALUE.toFloat() / rows).toInt()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(generalSpacing())
    ) {
        for (rowIndex in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.beat_visualization_size)),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (position in 1..maxItemsInEachRow) {
                    TickVisualization(
                        state = TickVisualizationState(
                            viewModel = viewModel,
                            beatsValue = position + rowIndex * maxItemsInEachRow
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BeatsControlSection(viewModel: IMetronomeViewModel) {
    val beats by viewModel.getBeatsFlow().collectAsState()

    ControlSection(
        label = stringResource(R.string.beats_label),
        value = beats.value,
        onValueChange = { viewModel.setBeats(Beats(it)) },
        valueRange = Beats.valueRange,
        sliderTestTag = TestConstants.BEATS_SLIDER,
        editTestTag = TestConstants.BEATS_EDIT
    )
}

@Composable
private fun SubdivisionsControlSection(viewModel: IMetronomeViewModel) {
    val subdivisions by viewModel.getSubdivisionsFlow().collectAsState()

    ControlSection(
        label = stringResource(R.string.subdivisions_label),
        value = subdivisions.value,
        onValueChange = { viewModel.setSubdivisions(Subdivisions(it)) },
        valueRange = Subdivisions.valueRange,
        sliderTestTag = TestConstants.SUBDIVISIONS_SLIDER,
        editTestTag = TestConstants.SUBDIVISIONS_EDIT
    )
}

@Composable
private fun TempoControlSection(viewModel: IMetronomeViewModel) {
    val tempo by viewModel.getTempoFlow().collectAsState()

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

@Composable
private fun ControlSection(
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
                .padding(start = generalSpacing())
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
private fun StartStopButton(
    viewModel: IMetronomeViewModel,
    modifier: Modifier = Modifier,
    iconSize: Dp = Dp.Unspecified
) {
    val playing by viewModel.getPlayingFlow().collectAsState()

    FilledIconButton(
        onClick = { viewModel.startStop() },
        modifier = modifier,
        shape = metronomeButtonShape()
    ) {
        Icon(
            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = stringResource(R.string.start_stop_button_description),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun TapTempoButton(
    viewModel: IMetronomeViewModel,
    modifier: Modifier = Modifier,
    iconSize: Dp = Dp.Unspecified
) {
    val hapticFeedback = LocalHapticFeedback.current

    FilledTonalIconButton(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
            viewModel.tapTempo()
        },
        modifier = modifier,
        shape = metronomeButtonShape()
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_drum),
            contentDescription = stringResource(R.string.tap_tempo_button_description),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun IncrementTempoButton(
    viewModel: IMetronomeViewModel,
    modifier: Modifier = Modifier,
    iconSize: Dp = Dp.Unspecified
) {
    TempoActionButton(
        modifier = modifier,
        imageVector = Icons.Filled.Add,
        iconSize = iconSize,
        contentDescription = stringResource(R.string.increment_tempo_button_description),
        onClick = { viewModel.changeTempo(1) },
        onLongClick = { viewModel.changeTempo(10) }
    )
}

@Composable
private fun DecrementTempoButton(
    viewModel: IMetronomeViewModel,
    modifier: Modifier = Modifier,
    iconSize: Dp = Dp.Unspecified
) {
    TempoActionButton(
        modifier = modifier,
        imageVector = Icons.Filled.Remove,
        iconSize = iconSize,
        contentDescription = stringResource(R.string.decrement_tempo_button_description),
        onClick = { viewModel.changeTempo(-1) },
        onLongClick = { viewModel.changeTempo(-10) }
    )
}

@Composable
private fun TempoActionButton(
    imageVector: ImageVector,
    iconSize: Dp,
    contentDescription: String,
    modifier: Modifier = Modifier,
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

    FilledTonalIconButton(
        onClick = {},
        modifier = modifier,
        shape = metronomeButtonShape(),
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Stable
@Composable
private fun metronomeButtonShape(): RoundedCornerShape = RoundedCornerShape(28.dp)

@Stable
@Composable
private fun Modifier.largeButton(): Modifier = this
    .widthIn(Dp.Unspecified, 184.dp)
    .heightIn(Dp.Unspecified, 136.dp)
    .fillMaxSize()

@Stable
@Composable
private fun buttonIconSize(): Dp = 40.dp

@Stable
@Composable
private fun generalSpacing(): Dp = 8.dp
