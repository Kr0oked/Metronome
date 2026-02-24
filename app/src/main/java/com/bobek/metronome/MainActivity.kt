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

package com.bobek.metronome

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat.getParcelableExtra
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bobek.metronome.data.Tick
import com.bobek.metronome.settings.SettingsRepository
import com.bobek.metronome.ui.MainContent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MetronomeViewModel by viewModels()
    private val postNotificationsPermissionRequest = registerPostNotificationsPermissionRequest()
    private val metronomeServiceConnection = MetronomeServiceConnection()
    private val refreshReceiver = RefreshReceiver()
    private val tickReceiver = TickReceiver()

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "Lifecycle: onCreate")
        super.onCreate(savedInstanceState)
        registerReceivers()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.DESTROYED)
            {
                if (!viewModel.getPlaying().first()) {
                    stopService(Intent(baseContext, MetronomeService::class.java))
                }
            }
        }
        enableEdgeToEdge()
        setContent {
            MainContent(viewModel)
        }
    }

    private fun registerReceivers() {
        ContextCompat.registerReceiver(
            this,
            refreshReceiver,
            IntentFilter(MetronomeService.ACTION_REFRESH),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            this,
            tickReceiver,
            IntentFilter(MetronomeService.ACTION_TICK),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onResume() {
        super.onResume()
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            runBlocking {
                handlePostNotificationsPermission()
            }
        }
        startAndBindToMetronomeService()
    }

    @RequiresApi(VERSION_CODES.TIRAMISU)
    private suspend fun handlePostNotificationsPermission() {
        if (neverRequestedPostNotificationsPermission() && postNotificationsPermissionNotGranted()) {
            startPostNotificationsPermissionRequestWorkflow()
        }
    }

    private suspend fun neverRequestedPostNotificationsPermission(): Boolean =
        settingsRepository.getPostNotificationsPermissionRequested().first().not()

    @RequiresApi(VERSION_CODES.TIRAMISU)
    private fun postNotificationsPermissionNotGranted() =
        checkSelfPermission(POST_NOTIFICATIONS) == PERMISSION_DENIED

    @RequiresApi(VERSION_CODES.TIRAMISU)
    private fun startPostNotificationsPermissionRequestWorkflow() {
        if (shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)) {
            showRequestNotificationsPermissionRationale()
        } else {
            launchPostNotificationsPermissionRequest()
        }
    }

    @RequiresApi(VERSION_CODES.TIRAMISU)
    private fun showRequestNotificationsPermissionRationale() {
        MaterialAlertDialogBuilder(this) // todo: replace with compose
            .setTitle(R.string.request_notifications_permission_rationale_title)
            .setMessage(R.string.request_notifications_permission_rationale_message)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                launchPostNotificationsPermissionRequest()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no_thanks) { dialog, _ ->
                lifecycleScope.launch {
                    settingsRepository.setPostNotificationsPermissionRequested(true)
                }
                dialog.dismiss()
            }
            .show()
    }

    @RequiresApi(VERSION_CODES.TIRAMISU)
    private fun launchPostNotificationsPermissionRequest() {
        postNotificationsPermissionRequest.launch(POST_NOTIFICATIONS)
    }

    private fun startAndBindToMetronomeService() {
        Intent(this, MetronomeService::class.java).also { service ->
            startService(service)
            bindService(service, metronomeServiceConnection, BIND_AUTO_CREATE or BIND_ABOVE_CLIENT)
        }
    }

    override fun onPause() {
        super.onPause()
        unbindService(metronomeServiceConnection)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(refreshReceiver)
        unregisterReceiver(tickReceiver)
    }

    private fun registerPostNotificationsPermissionRequest() =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                lifecycleScope.launch {
                    settingsRepository.setPostNotificationsPermissionRequested(true)
                }
            }
        }

    private inner class MetronomeServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MetronomeService.LocalBinder
            viewModel.setMetronomeService(binder.getService())
        }

        override fun onServiceDisconnected(name: ComponentName) {
            viewModel.setMetronomeService(null)
        }
    }

    inner class RefreshReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            viewModel.setMetronomeService() // TODO
        }
    }

    inner class TickReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            getParcelableExtra(intent, MetronomeService.EXTRA_TICK, Tick::class.java)?.let {
                viewModel.onTickReceived(it)
            }
        }
    }
}
