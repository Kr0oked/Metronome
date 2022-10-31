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

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bobek.metronome.data.Tempo
import com.bobek.metronome.data.Tick
import com.bobek.metronome.data.TickType
import com.bobek.metronome.databinding.AboutAlertDialogViewBinding
import com.bobek.metronome.databinding.ActivityMainBinding
import com.bobek.metronome.view.component.TickVisualization
import com.bobek.metronome.view.model.MetronomeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

private const val TAG = "MainActivity"
private const val LARGE_TEMPO_CHANGE_SIZE = 10

class MainActivity : AppCompatActivity() {

    private val metronomeServiceConnection = MetronomeServiceConnection()
    private val tickReceiver = TickReceiver()
    private val refreshReceiver = RefreshReceiver()

    private lateinit var viewModel: MetronomeViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var optionsMenu: Menu

    private var metronomeService: MetronomeService? = null

    private var lastTap: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "Lifecycle: onCreate")
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)

        viewModel = MetronomeViewModel()
        viewModel.playing.observe(this) { playing -> if (playing) startMetronome() }
        viewModel.playing.observe(this) { playing -> if (!playing) stopMetronome() }
        viewModel.beatsData.observe(this) { beats -> metronomeService?.beats = beats }
        viewModel.subdivisionsData.observe(this) { subdivisions -> metronomeService?.subdivisions = subdivisions }
        viewModel.tempoData.observe(this) { tempo -> metronomeService?.tempo = tempo }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.metronome = viewModel

        binding.contentMain.incrementTempoButton.setOnClickListener { incrementTempo() }
        binding.contentMain.incrementTempoButton.setOnLongClickListener {
            repeat(LARGE_TEMPO_CHANGE_SIZE) { incrementTempo() }
            true
        }

        binding.contentMain.decrementTempoButton.setOnClickListener { decrementTempo() }
        binding.contentMain.decrementTempoButton.setOnLongClickListener {
            repeat(LARGE_TEMPO_CHANGE_SIZE) { decrementTempo() }
            true
        }

        binding.contentMain.tapTempoButton.setOnClickListener { tapTempo() }

        setSupportActionBar(binding.toolbar)

        registerForMetronomeActions()
        startAndBindToMetronomeService()
    }

    private fun registerForMetronomeActions() {
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(refreshReceiver, IntentFilter(MetronomeService.ACTION_REFRESH))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(tickReceiver, IntentFilter(MetronomeService.ACTION_TICK))
        Log.d(TAG, "Registered for metronome actions")
    }

    private fun startAndBindToMetronomeService() {
        Intent(this, MetronomeService::class.java)
            .also { service -> startService(service) }
            .also { Log.d(TAG, "MetronomeService started") }
            .also { service -> bindService(service, metronomeServiceConnection, BIND_AUTO_CREATE or BIND_ABOVE_CLIENT) }
            .also { Log.d(TAG, "MetronomeService binding") }
    }

    private fun startMetronome() {
        metronomeService?.playing = true
        window.addFlags(FLAG_KEEP_SCREEN_ON)
        Log.i(TAG, "Started metronome")
    }

    private fun stopMetronome() {
        metronomeService?.playing = false
        window.clearFlags(FLAG_KEEP_SCREEN_ON)
        Log.i(TAG, "Stopped metronome")
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

    override fun onStart() {
        Log.d(TAG, "Lifecycle: onStart")
        super.onStart()
    }

    override fun onResume() {
        Log.d(TAG, "Lifecycle: onResume")
        super.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "Lifecycle: onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "Lifecycle: onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "Lifecycle: onDestroy")
        super.onDestroy()
        unregisterFromMetronomeActions()
        unbindFromMetronomeService()
    }

    private fun unregisterFromMetronomeActions() {
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(refreshReceiver)
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(tickReceiver)
        Log.d(TAG, "Unregistered for metronome actions")
    }

    private fun unbindFromMetronomeService() {
        Intent(this, MetronomeService::class.java)
            .also { unbindService(metronomeServiceConnection) }
        Log.d(TAG, "MetronomeService unbound")
    }

    private inner class MetronomeServiceConnection : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.i(TAG, "MetronomeService connected")
            val binder = service as MetronomeService.LocalBinder
            metronomeService = binder.getService()
            viewModel.connected.value = true
            synchronizeViewModel()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "MetronomeService disconnected")
            metronomeService = null
            viewModel.connected.value = false
        }
    }

    private inner class TickReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.getParcelableExtra<Tick>(MetronomeService.EXTRA_TICK)
                ?.also { tick -> Log.v(TAG, "Received $tick") }
                ?.also { tick -> visualizeTick(tick) }
        }
    }

    private fun visualizeTick(tick: Tick) {
        if (tick.type == TickType.STRONG || tick.type == TickType.WEAK) {
            getTickVisualization(tick.beat)?.blink()
        }
    }

    private fun getTickVisualization(beat: Int): TickVisualization? {
        return when (beat) {
            1 -> binding.contentMain.tickVisualization1
            2 -> binding.contentMain.tickVisualization2
            3 -> binding.contentMain.tickVisualization3
            4 -> binding.contentMain.tickVisualization4
            5 -> binding.contentMain.tickVisualization5
            6 -> binding.contentMain.tickVisualization6
            7 -> binding.contentMain.tickVisualization7
            8 -> binding.contentMain.tickVisualization8
            else -> null
        }
    }

    private inner class RefreshReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.v(TAG, " Received refresh command")
            synchronizeViewModel()
        }
    }

    private fun synchronizeViewModel() {
        metronomeService?.let { service ->
            viewModel.beatsData.value = service.beats
            viewModel.subdivisionsData.value = service.subdivisions
            viewModel.tempoData.value = service.tempo
            viewModel.playing.value = service.playing
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        optionsMenu = menu
        updateNightModeIcon()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_night_mode -> {
                toggleNightMode()
                true
            }
            R.id.action_about -> {
                showAboutPopup()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleNightMode() {
        when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> changeNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            AppCompatDelegate.MODE_NIGHT_YES -> changeNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            else -> changeNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun changeNightMode(@NightMode mode: Int) {
        Log.d(TAG, "Setting night mode to value $mode")
        AppCompatDelegate.setDefaultNightMode(mode)
        updateNightModeIcon()
    }

    private fun showAboutPopup() {
        val alertDialogBuilder = MaterialAlertDialogBuilder(this)
        val dialogContextInflater = LayoutInflater.from(alertDialogBuilder.context)

        val dialogBinding = AboutAlertDialogViewBinding.inflate(dialogContextInflater, null, false)
        dialogBinding.version = BuildConfig.VERSION_NAME
        dialogBinding.copyrightText.movementMethod = LinkMovementMethod.getInstance()
        dialogBinding.licenseText.movementMethod = LinkMovementMethod.getInstance()
        dialogBinding.sourceCodeText.movementMethod = LinkMovementMethod.getInstance()

        alertDialogBuilder
            .setTitle(R.string.app_name)
            .setView(dialogBinding.root)
            .setNeutralButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun updateNightModeIcon() {
        optionsMenu
            .findItem(R.id.action_night_mode)
            .setIcon(getNightModeIcon())
    }

    @DrawableRes
    private fun getNightModeIcon(): Int {
        return when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> R.drawable.ic_night_mode_no
            AppCompatDelegate.MODE_NIGHT_YES -> R.drawable.ic_night_mode_yes
            else -> R.drawable.ic_night_mode_auto
        }
    }
}
