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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Preview
@Composable
fun TickVisualization(
    @PreviewParameter(TickVisualizationStateProvider::class) state: TickVisualizationState,
    onGapToggle: () -> Unit = {}
) {
    var blinking by remember { mutableStateOf(false) }

    LaunchedEffect(state.isBlinking) {
        if (state.isBlinking) {
            blinking = true
            delay(200)
            blinking = false
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            blinking -> MaterialTheme.colorScheme.primary
            state.isGap -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.tertiaryContainer
        },
        animationSpec = tween(durationMillis = 200)
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onGapToggle() }
    )
}

data class TickVisualizationState(
    val isBlinking: Boolean,
    val isGap: Boolean
)

private class TickVisualizationStateProvider : PreviewParameterProvider<TickVisualizationState> {
    override val values: Sequence<TickVisualizationState> = sequenceOf(
        TickVisualizationState(isBlinking = false, isGap = false),
        TickVisualizationState(isBlinking = false, isGap = true),
        TickVisualizationState(isBlinking = true, isGap = false),
        TickVisualizationState(isBlinking = true, isGap = true)
    )

    override fun getDisplayName(index: Int): String = values.elementAt(index).toString()
}
