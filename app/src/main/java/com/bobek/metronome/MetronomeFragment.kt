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

package com.bobek.metronome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.*
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.bobek.metronome.data.Tempo
import com.bobek.metronome.data.Tick
import com.bobek.metronome.data.TickType
import com.bobek.metronome.databinding.AboutAlertDialogViewBinding
import com.bobek.metronome.databinding.FragmentMetronomeBinding
import com.bobek.metronome.preference.PreferenceStore
import com.bobek.metronome.view.component.TickVisualization
import com.bobek.metronome.view.model.MetronomeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

private const val TAG = "MetronomeFragment"
private const val LARGE_TEMPO_CHANGE_SIZE = 10

class MetronomeFragment : Fragment() {

    private val viewModel: MetronomeViewModel by activityViewModels()
    private val tickReceiver = TickReceiver()

    private lateinit var preferenceStore: PreferenceStore
    private lateinit var binding: FragmentMetronomeBinding

    private var optionsMenu: Menu? = null
    private var lastTap: Long = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        preferenceStore = PreferenceStore(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMetronomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPreferenceStore()
        initViewModel()
        initBinding()
        registerTickReceiver()
        setupMenu()
    }

    private fun initPreferenceStore() {
        preferenceStore.nightMode.observe(viewLifecycleOwner) { updateNightModeIcon() }
        Log.d(TAG, "Initialized preference store")
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

    private fun getKeepScreenOnView() = binding.content.startStopButton

    private fun initBinding() {
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
    }

    private fun registerTickReceiver() {
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(tickReceiver, IntentFilter(MetronomeService.ACTION_TICK))
        Log.d(TAG, "Registered tickReceiver")
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

    private fun setupMenu() {
        requireActivity().addMenuProvider(getMenuProvider(), viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun getMenuProvider() = object : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_metronome, menu)
            this@MetronomeFragment.optionsMenu = menu
            updateNightModeIcon()
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.action_night_mode -> {
                    toggleNightMode()
                    true
                }
                R.id.action_settings -> {
                    showSettings()
                    true
                }
                R.id.action_about -> {
                    showAboutPopup()
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleNightMode() {
        preferenceStore.nightMode.value?.let {
            when (it) {
                MODE_NIGHT_NO -> preferenceStore.nightMode.value = MODE_NIGHT_YES
                MODE_NIGHT_YES -> preferenceStore.nightMode.value = MODE_NIGHT_FOLLOW_SYSTEM
                else -> preferenceStore.nightMode.value = MODE_NIGHT_NO
            }
        }
    }

    private fun showSettings() {
        findNavController().navigate(R.id.action_MetronomeFragment_to_SettingsFragment)
    }

    private fun showAboutPopup() {
        val alertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
        val dialogContextInflater = LayoutInflater.from(alertDialogBuilder.context)

        val dialogBinding = AboutAlertDialogViewBinding.inflate(dialogContextInflater, null, false)
        dialogBinding.version = BuildConfig.VERSION_NAME
        dialogBinding.copyrightText.movementMethod = LinkMovementMethod.getInstance()
        dialogBinding.licenseText.movementMethod = LinkMovementMethod.getInstance()
        dialogBinding.sourceCodeText.movementMethod = LinkMovementMethod.getInstance()

        alertDialogBuilder
            .setTitle(R.string.metronome)
            .setView(dialogBinding.root)
            .setNeutralButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun updateNightModeIcon() {
        optionsMenu
            ?.findItem(R.id.action_night_mode)
            ?.setIcon(getNightModeIcon())
    }

    @DrawableRes
    private fun getNightModeIcon(): Int {
        return when (preferenceStore.nightMode.value) {
            MODE_NIGHT_NO -> R.drawable.ic_light_mode
            MODE_NIGHT_YES -> R.drawable.ic_dark_mode
            else -> R.drawable.ic_auto_mode
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterTickReceiver()
    }

    private fun unregisterTickReceiver() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(tickReceiver)
        Log.d(TAG, "Unregistered tickReceiver")
    }

    override fun onDetach() {
        super.onDetach()
        closePreferenceStore()
    }

    private fun closePreferenceStore() {
        preferenceStore.close()
        Log.d(TAG, "Closed preference store")
    }


    private inner class TickReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            extractTick(intent)
                ?.also { tick -> Log.v(TAG, "Received $tick") }
                ?.also { tick -> visualizeTick(tick) }
        }
    }

    private fun extractTick(intent: Intent): Tick? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(MetronomeService.EXTRA_TICK, Tick::class.java)
        } else {
            intent.getParcelableExtra(MetronomeService.EXTRA_TICK)
        }
    }

    private fun visualizeTick(tick: Tick) {
        if (tick.type == TickType.STRONG || tick.type == TickType.WEAK) {
            getTickVisualization(tick.beat)?.blink()
        }
    }

    private fun getTickVisualization(beat: Int): TickVisualization? {
        return when (beat) {
            1 -> binding.content.tickVisualization1
            2 -> binding.content.tickVisualization2
            3 -> binding.content.tickVisualization3
            4 -> binding.content.tickVisualization4
            5 -> binding.content.tickVisualization5
            6 -> binding.content.tickVisualization6
            7 -> binding.content.tickVisualization7
            8 -> binding.content.tickVisualization8
            else -> null
        }
    }
}
