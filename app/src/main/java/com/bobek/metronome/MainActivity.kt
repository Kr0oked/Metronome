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
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.IBinder
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import androidx.core.content.ContextCompat.getColor
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bobek.metronome.databinding.AboutAlertDialogViewBinding
import com.bobek.metronome.databinding.ActivityMainBinding
import com.bobek.metronome.domain.Tempo
import com.bobek.metronome.domain.Tick
import com.bobek.metronome.domain.TickType
import com.bobek.metronome.view.model.MetronomeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

private const val TAG = "MainActivity"
private const val TICK_VISUALIZATION_DURATION_MILLIS = 200L

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
            .also { service -> bindService(service, metronomeServiceConnection, BIND_ABOVE_CLIENT) }
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

    private fun tapTempo() {
        val currentTime = System.currentTimeMillis()
        val tempoValue = calculateTapTempo(lastTap, currentTime)

        try {
            viewModel.tempoData.value = Tempo(tempoValue)
        } catch (exception: IllegalArgumentException) {
            Log.v(TAG, "Illegal tapped tempo", exception)
        }

        lastTap = currentTime
    }

    private fun calculateTapTempo(firstTap: Long, secondTap: Long): Int = (60_000 / (secondTap - firstTap)).toInt()

    override fun onDestroy() {
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
            synchronizeViewModel()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "MetronomeService disconnected")
            metronomeService = null
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
        getBeatsVisualizationImage(tick.beat)
            ?.let { image ->
                if (tick.type == TickType.STRONG) {
                    visualizeStrongTick(image)
                } else if (tick.type == TickType.WEAK) {
                    visualizeWeakTick(image)
                }
            }
    }

    private fun getBeatsVisualizationImage(beat: Int): ImageView? {
        return when (beat) {
            1 -> binding.contentMain.beatsVisualization1Image
            2 -> binding.contentMain.beatsVisualization2Image
            3 -> binding.contentMain.beatsVisualization3Image
            4 -> binding.contentMain.beatsVisualization4Image
            5 -> binding.contentMain.beatsVisualization5Image
            6 -> binding.contentMain.beatsVisualization6Image
            7 -> binding.contentMain.beatsVisualization7Image
            8 -> binding.contentMain.beatsVisualization8Image
            else -> null
        }
    }

    private fun visualizeStrongTick(image: ImageView) {
        visualizeTick(image, R.color.beat_visualization_strong_tick, R.color.beat_visualization_strong)
    }

    private fun visualizeWeakTick(it: ImageView) {
        visualizeTick(it, R.color.beat_visualization_weak_tick, R.color.beat_visualization_weak)
    }

    private fun visualizeTick(image: ImageView, @ColorRes flashColorId: Int, @ColorRes originalColorId: Int) {
        runOnUiThread {
            image
                .animate()
                .setDuration(TICK_VISUALIZATION_DURATION_MILLIS)
                .withStartAction {
                    image.imageTintList = ColorStateList.valueOf(getColor(this, flashColorId))
                }
                .withEndAction {
                    image.imageTintList = ColorStateList.valueOf(getColor(this, originalColorId))
                }
                .start()
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
