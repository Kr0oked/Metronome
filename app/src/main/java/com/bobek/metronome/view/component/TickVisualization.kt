/*
 * This file is part of Metronome.
 * Copyright (C) 2023 Philipp Bobek <philipp.bobek@mailbox.org>
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

package com.bobek.metronome.view.component

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.bobek.metronome.databinding.TickVisualizationBinding

class TickVisualization(context: Context, attributes: AttributeSet) : ConstraintLayout(context, attributes) {

    private val binding: TickVisualizationBinding

    init {
        val layoutInflater = LayoutInflater.from(context)
        binding = TickVisualizationBinding.inflate(layoutInflater, this, true)
    }

    fun blink() {
        val blinkAnimation = binding.tickVisualizationImage.background as AnimationDrawable
        blinkAnimation.stop()
        blinkAnimation.start()
    }
}
