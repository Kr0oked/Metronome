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
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate.NightMode
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.bobek.metronome.databinding.ActivityMainBinding
import com.bobek.metronome.preference.PreferenceStore
import com.bobek.metronome.view.model.MetronomeViewModel

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private val viewModel: MetronomeViewModel by viewModels()
    private val metronomeServiceConnection = MetronomeServiceConnection()
    private val refreshReceiver = RefreshReceiver()

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var preferenceStore: PreferenceStore

    private var metronomeService: MetronomeService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "Lifecycle: onCreate")
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(binding.toolbar)

        val navController = getNavController()
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        initPreferenceStore()
        initViewModel()
        registerRefreshReceiver()
        startAndBindToMetronomeService()
    }

    private fun initPreferenceStore() {
        preferenceStore = PreferenceStore(this)
        preferenceStore.beats.observe(this) { viewModel.beatsData.value = it }
        preferenceStore.subdivisions.observe(this) { viewModel.subdivisionsData.value = it }
        preferenceStore.tempo.observe(this) { viewModel.tempoData.value = it }
        preferenceStore.emphasizeFirstBeat.observe(this) { viewModel.emphasizeFirstBeat.value = it }
        preferenceStore.nightMode.observe(this) { setNightMode(it) }
        Log.d(TAG, "Initialized preference store")
    }

    private fun setNightMode(@NightMode mode: Int) {
        Log.d(TAG, "Setting night mode to value $mode")
        setDefaultNightMode(mode)
    }

    private fun initViewModel() {
        viewModel.beatsData.observe(this) { metronomeService?.beats = it }
        viewModel.subdivisionsData.observe(this) { metronomeService?.subdivisions = it }
        viewModel.tempoData.observe(this) { metronomeService?.tempo = it }
        viewModel.emphasizeFirstBeat.observe(this) { metronomeService?.emphasizeFirstBeat = it }
        viewModel.playing.observe(this) { metronomeService?.playing = it }
        Log.d(TAG, "Initialized view model")
    }

    private fun registerRefreshReceiver() {
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(refreshReceiver, IntentFilter(MetronomeService.ACTION_REFRESH))
        Log.d(TAG, "Registered refreshReceiver")
    }

    private fun startAndBindToMetronomeService() {
        Intent(this, MetronomeService::class.java)
            .also { service -> startService(service) }
            .also { Log.d(TAG, "MetronomeService started") }
            .also { service -> bindService(service, metronomeServiceConnection, BIND_AUTO_CREATE or BIND_ABOVE_CLIENT) }
            .also { Log.d(TAG, "MetronomeService binding") }
    }

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
        updatePreferenceStore()
    }

    private fun updatePreferenceStore() {
        preferenceStore.beats.value = viewModel.beatsData.value
        preferenceStore.subdivisions.value = viewModel.subdivisionsData.value
        preferenceStore.tempo.value = viewModel.tempoData.value
        preferenceStore.emphasizeFirstBeat.value = viewModel.emphasizeFirstBeat.value
        Log.d(TAG, "Updated preference store")
    }

    override fun onDestroy() {
        Log.d(TAG, "Lifecycle: onDestroy")
        super.onDestroy()
        closePreferenceStore()
        unregisterRefreshReceiver()
        unbindFromMetronomeService()
    }

    private fun closePreferenceStore() {
        preferenceStore.close()
        Log.d(TAG, "Closed preference store")
    }

    private fun unregisterRefreshReceiver() {
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(refreshReceiver)
        Log.d(TAG, "Unregistered refreshReceiver")
    }

    private fun unbindFromMetronomeService() {
        Intent(this, MetronomeService::class.java).also { unbindService(metronomeServiceConnection) }
        Log.d(TAG, "MetronomeService unbound")
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = getNavController()
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun getNavController(): NavController {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        return navHostFragment.navController
    }


    private inner class MetronomeServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.i(TAG, "MetronomeService connected")
            val binder = service as MetronomeService.LocalBinder
            metronomeService = binder.getService()
            viewModel.connected.value = true
            synchronizeViewModelWithService()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "MetronomeService disconnected")
            metronomeService = null
            viewModel.connected.value = false
        }
    }


    private inner class RefreshReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Received refresh command")
            synchronizeViewModelWithService()
        }
    }


    private fun synchronizeViewModelWithService() {
        metronomeService?.let { service ->
            if (service.playing) updateViewModel(service) else initServiceValues(service)
            viewModel.playing.value = service.playing
        }
    }

    private fun updateViewModel(service: MetronomeService) {
        viewModel.beatsData.value = service.beats
        viewModel.subdivisionsData.value = service.subdivisions
        viewModel.tempoData.value = service.tempo
        viewModel.emphasizeFirstBeat.value = service.emphasizeFirstBeat
    }

    private fun initServiceValues(service: MetronomeService) {
        viewModel.beatsData.value?.let { service.beats = it }
        viewModel.subdivisionsData.value?.let { service.subdivisions = it }
        viewModel.tempoData.value?.let { service.tempo = it }
        viewModel.emphasizeFirstBeat.value?.let { service.emphasizeFirstBeat = it }
    }
}
