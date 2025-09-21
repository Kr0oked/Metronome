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

package com.bobek.metronome

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.bobek.metronome.data.Tempo
import com.bobek.metronome.data.Tick
import com.bobek.metronome.data.TickType
import com.bobek.metronome.databinding.FragmentMetronomeBinding
import com.bobek.metronome.view.component.TickVisualization
import com.bobek.metronome.view.model.MetronomeViewModel

private const val TAG = "MetronomeFragment"
private const val LARGE_TEMPO_CHANGE_SIZE = 10

class MetronomeFragment : Fragment() {

    private val viewModel: MetronomeViewModel by activityViewModels()
    private val tickReceiver = TickReceiver()

    private var binding: FragmentMetronomeBinding? = null
    private var lastTap: Long = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMetronomeBinding.inflate(inflater, container, false)
        return requireBinding().root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        initBinding()
        adjustLayoutToSystemBars()
        registerTickReceiver()
        setupMenu()
    }

    private fun initViewModel() {
        viewModel.playing.observe(viewLifecycleOwner) { playing ->
            if (playing) keepScreenOn() else doNotKeepScreenOn()
        }
    }

    private fun keepScreenOn() {
        getKeepScreenOnView().keepScreenOn = true
    }

    private fun doNotKeepScreenOn() {
        getKeepScreenOnView().keepScreenOn = false
    }

    private fun getKeepScreenOnView() = requireBinding().content.startStopButton

    @SuppressLint("ClickableViewAccessibility")
    private fun initBinding() {
        val binding = requireBinding()
        binding.lifecycleOwner = viewLifecycleOwner
        binding.metronome = viewModel

        binding.content.incrementTempoButton.setOnClickListener { incrementTempo() }
        binding.content.incrementTempoButton.setOnLongClickListener {
            repeat(LARGE_TEMPO_CHANGE_SIZE) { incrementTempo() }
            true
        }

        binding.content.decrementTempoButton.setOnClickListener { decrementTempo() }
        binding.content.decrementTempoButton.setOnLongClickListener {
            repeat(LARGE_TEMPO_CHANGE_SIZE) { decrementTempo() }
            true
        }

        binding.content.tapTempoButton.setOnClickListener { tapTempo() }
        binding.content.tapTempoButton.setOnTouchListener { view, event -> tapTempoOnTouchListener(view, event) }
    }

    private fun incrementTempo() {
        viewModel.tempoData.value?.value?.let {
            if (it < Tempo.MAX) {
                viewModel.tempoData.value = Tempo(it + 1)
            }
        }
    }

    private fun decrementTempo() {
        viewModel.tempoData.value?.value?.let {
            if (it > Tempo.MIN) {
                viewModel.tempoData.value = Tempo(it - 1)
            }
        }
    }

    private fun tapTempo() {
        val currentTime = System.currentTimeMillis()
        val tempoValue = calculateTapTempo(lastTap, currentTime)

        if (tempoValue > Tempo.MAX) {
            viewModel.tempoData.value = Tempo(Tempo.MAX)
        } else if (tempoValue >= Tempo.MIN) {
            viewModel.tempoData.value = Tempo(tempoValue)
        }

        lastTap = currentTime
    }

    private fun calculateTapTempo(firstTap: Long, secondTap: Long): Int = (60_000 / (secondTap - firstTap)).toInt()

    private fun tapTempoOnTouchListener(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                view.isPressed = true
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                view.performClick()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.isPressed = false
            }
        }

        return true
    }

    private fun adjustLayoutToSystemBars() {
        ViewCompat.setOnApplyWindowInsetsListener(requireBinding().content.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun registerTickReceiver() {
        ContextCompat.registerReceiver(
            requireContext(),
            tickReceiver,
            IntentFilter(MetronomeService.ACTION_TICK),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        Log.d(TAG, "Registered tickReceiver")
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(getMenuProvider(), viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun getMenuProvider() = object : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_metronome, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.action_settings -> {
                    showSettings()
                    true
                }

                else -> false
            }
        }
    }

    private fun showSettings() {
        findNavController().navigate(R.id.action_MetronomeFragment_to_SettingsFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        unregisterTickReceiver()
    }

    private fun unregisterTickReceiver() {
        requireContext().unregisterReceiver(tickReceiver)
        Log.d(TAG, "Unregistered tickReceiver")
    }

    inner class TickReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            extractTick(intent)
                ?.also { tick -> Log.v(TAG, "Received $tick") }
                ?.also { tick -> visualizeTick(tick) }
        }
    }

    private fun extractTick(intent: Intent): Tick? {
        return if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(MetronomeService.EXTRA_TICK, Tick::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(MetronomeService.EXTRA_TICK)
        }
    }

    private fun visualizeTick(tick: Tick) {
        if (tick.type == TickType.STRONG || tick.type == TickType.WEAK) {
            getTickVisualization(tick.beat)?.blink()
        }
    }

    private fun getTickVisualization(beat: Int): TickVisualization? = when (beat) {
        1 -> requireBinding().content.tickVisualization1
        2 -> requireBinding().content.tickVisualization2
        3 -> requireBinding().content.tickVisualization3
        4 -> requireBinding().content.tickVisualization4
        5 -> requireBinding().content.tickVisualization5
        6 -> requireBinding().content.tickVisualization6
        7 -> requireBinding().content.tickVisualization7
        8 -> requireBinding().content.tickVisualization8
        else -> null
    }

    private fun requireBinding(): FragmentMetronomeBinding = binding!!
}
