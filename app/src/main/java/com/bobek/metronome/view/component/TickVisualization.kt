/*
 * This file is part of Metronome.
 * Copyright (C) 2025 Philipp Bobek <philipp.bobek@mailbox.org>
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
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.InverseBindingListener
import com.bobek.metronome.databinding.TickVisualizationBinding

class TickVisualization(context: Context, attributes: AttributeSet) : ConstraintLayout(context, attributes) {

    private val binding: TickVisualizationBinding

    var gap: Boolean
        get() = binding.gap
        set(gap) {
            binding.gap = gap
        }

    var gapChangedListener: InverseBindingListener? = null

    init {
        val layoutInflater = LayoutInflater.from(context)

        binding = TickVisualizationBinding.inflate(layoutInflater, this, true)
        binding.gap = false

        setOnClickListener {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            binding.gap = binding.gap.not()
            gapChangedListener?.onChange()
        }
    }

    fun blink() {
        val background = binding.tickVisualizationImage.background

        if (background is AnimationDrawable) {
            background.stop()
            background.start()
        }
    }
}
