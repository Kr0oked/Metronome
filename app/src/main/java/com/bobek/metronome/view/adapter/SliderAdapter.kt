/*
 * This file is part of Metronome.
 * Copyright (C) 2022 Philipp Bobek <philipp.bobek@mailbox.org>
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

package com.bobek.metronome.view.adapter

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.google.android.material.slider.Slider

object SliderAdapter {

    @BindingAdapter("android:value")
    @JvmStatic
    fun setValue(slider: Slider, value: Float) {
        if (slider.value != value) {
            slider.value = value
        }
    }

    @InverseBindingAdapter(attribute = "android:value")
    @JvmStatic
    fun getValue(slider: Slider): Float = slider.value

    @BindingAdapter("android:valueAttrChanged")
    @JvmStatic
    fun setListener(slider: Slider, attrChange: InverseBindingListener) =
        slider.addOnChangeListener { _, _, _ -> attrChange.onChange() }
}
