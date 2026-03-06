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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.bobek.metronome.ComposeMetronomeViewModel
import com.bobek.metronome.IMetronomeViewModel
import com.bobek.metronome.data.Gaps
import com.bobek.metronome.data.TickType
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Preview(widthDp = 40)
@Composable
fun TickVisualization(
    @PreviewParameter(TickVisualizationStateProvider::class) state: TickVisualizationState,
    modifier: Modifier = Modifier,
    animationDuration: Duration = 200.milliseconds
) {
    val beats by state.viewModel.getBeatsFlow().collectAsState()
    val gaps by state.viewModel.getGapsFlow().collectAsState()
    var blinking by remember { mutableStateOf(false) }

    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        state.viewModel.getTickFlow().collect { tick ->
            if (tick.beat == state.beatsValue && tick.type != TickType.SUB) {
                blinking = true
                delay(animationDuration)
                blinking = false
            }
        }
    }

    val isGap = gaps.value.contains(state.beatsValue)

    val backgroundColor by animateColorAsState(
        targetValue = when {
            blinking -> MaterialTheme.colorScheme.primary
            isGap -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.tertiaryContainer
        },
        animationSpec = tween(durationMillis = animationDuration.inWholeMilliseconds.toInt())
    )

    if (state.beatsValue <= beats.value) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxSize()
                    .clickable {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        state.viewModel.setGaps(gaps.toggle(state.beatsValue))
                    },
                onDraw = {
                    val minDimension = this.size.minDimension

                    if (isGap) {
                        drawCircle(
                            color = backgroundColor,
                            radius = (minDimension * 0.45f),
                            style = Stroke(width = (minDimension * 0.1f))
                        )
                    } else {
                        drawCircle(
                            color = backgroundColor,
                            radius = (minDimension / 2.0f),
                        )
                    }
                }
            )
        }
    }
}

data class TickVisualizationState(
    val viewModel: IMetronomeViewModel,
    val beatsValue: Int
)

private class TickVisualizationStateProvider : PreviewParameterProvider<TickVisualizationState> {
    override val values: Sequence<TickVisualizationState> = sequenceOf(
        TickVisualizationState(viewModel = ComposeMetronomeViewModel(gaps = Gaps(sortedSetOf())), beatsValue = 1),
        TickVisualizationState(viewModel = ComposeMetronomeViewModel(gaps = Gaps(sortedSetOf(1))), beatsValue = 1)
    )

    override fun getDisplayName(index: Int): String? =
        when (index) {
            0 -> "No gap"
            1 -> "Gap"
            else -> null
        }
}
