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
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat.getParcelableExtra
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bobek.metronome.data.AppNightMode
import com.bobek.metronome.data.Tick
import com.bobek.metronome.settings.SettingsRepository
import com.bobek.metronome.ui.licenses.ThirdPartyLicenseScreen
import com.bobek.metronome.ui.licenses.ThirdPartyLicenseScreenState
import com.bobek.metronome.ui.licenses.ThirdPartyLicensesScreen
import com.bobek.metronome.ui.metronome.MetronomeScreen
import com.bobek.metronome.ui.settings.SettingsScreen
import com.bobek.metronome.ui.theme.AppTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import de.philipp_bobek.oss_licenses_parser.OssLicensesParser
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

    private var metronomeService: MetronomeService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "Lifecycle: onCreate")
        super.onCreate(savedInstanceState)
        initViewModel()
        registerReceivers()
        enableEdgeToEdge()
        setContent {
            MainContent(viewModel)
        }
    }

    private fun initViewModel() {
        viewModel.beatsData.observe(this) { metronomeService?.beats = it }
        viewModel.subdivisionsData.observe(this) { metronomeService?.subdivisions = it }
        viewModel.gapsData.observe(this) { metronomeService?.gaps = it }
        viewModel.tempoData.observe(this) { metronomeService?.tempo = it }
        viewModel.emphasizeFirstBeat.observe(this) { metronomeService?.emphasizeFirstBeat = it }
        viewModel.sound.observe(this) { metronomeService?.sound = it }
        viewModel.playing.observe(this) { metronomeService?.playing = it }
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
        MaterialAlertDialogBuilder(this)
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
        if (metronomeService?.playing == false) {
            stopService(Intent(this, MetronomeService::class.java))
        }
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
            metronomeService = binder.getService()
            viewModel.connected.value = true
            synchronizeViewModelWithService()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            metronomeService = null
            viewModel.connected.value = false
        }
    }

    inner class RefreshReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            synchronizeViewModelWithService()
        }
    }

    inner class TickReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            getParcelableExtra(intent, MetronomeService.EXTRA_TICK, Tick::class.java)?.let {
                viewModel.onTickReceived(it)
            }
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
        viewModel.gapsData.value = service.gaps
        viewModel.tempoData.value = service.tempo
        viewModel.emphasizeFirstBeat.value = service.emphasizeFirstBeat
        viewModel.sound.value = service.sound
    }

    private fun initServiceValues(service: MetronomeService) {
        viewModel.beatsData.value?.let { service.beats = it }
        viewModel.subdivisionsData.value?.let { service.subdivisions = it }
        viewModel.gapsData.value?.let { service.gaps = it }
        viewModel.tempoData.value?.let { service.tempo = it }
        viewModel.emphasizeFirstBeat.value?.let { service.emphasizeFirstBeat = it }
        viewModel.sound.value?.let { service.sound = it }
    }
}

@PreviewScreenSizes
@Composable
fun MainContent(
    viewModel: IMetronomeViewModel = ComposeMetronomeViewModel(connected = MutableLiveData(true))
) {
    val navController = rememberNavController()
    val nightMode by viewModel.nightMode.observeAsState(AppNightMode.FOLLOW_SYSTEM)
    val playing by viewModel.playing.observeAsState(false)

    val isDarkTheme = when (nightMode) {
        AppNightMode.NO -> false
        AppNightMode.YES -> true
        AppNightMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }

    val activity = LocalActivity.current
    LaunchedEffect(playing) {
        if (playing) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    AppTheme(darkTheme = isDarkTheme) {
        NavHost(navController = navController, startDestination = "metronome") {
            composable("metronome") {
                MetronomeScreen(
                    viewModel = viewModel,
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onThirdPartyLicensesClick = { navController.navigate("licenses") }
                )
            }
            composable("licenses") {
                val context = LocalContext.current
                val licenses = remember {
                    context.resources
                        .openRawResource(R.raw.third_party_license_metadata)
                        .use(OssLicensesParser::parseMetadata)
                        .sortedBy { it.libraryName }
                }

                ThirdPartyLicensesScreen(
                    licenses = licenses,
                    onBackClick = { navController.popBackStack() },
                    onLicenseClick = { metadata ->
                        navController.navigate("license/${metadata.libraryName}")
                    }
                )
            }
            composable("license/{libraryName}") { backStackEntry ->
                val libraryName = backStackEntry.arguments?.getString("libraryName") ?: ""
                val context = LocalContext.current
                val licenseContent = remember(libraryName) {
                    val metadata = context.resources
                        .openRawResource(R.raw.third_party_license_metadata)
                        .use(OssLicensesParser::parseMetadata)
                        .find { it.libraryName == libraryName }

                    metadata?.let {
                        context.resources
                            .openRawResource(R.raw.third_party_licenses)
                            .use { stream -> OssLicensesParser.parseLicense(it, stream).licenseContent }
                    } ?: ""
                }

                ThirdPartyLicenseScreen(
                    state = ThirdPartyLicenseScreenState(
                        libraryName = libraryName,
                        licenseContent = licenseContent,
                    ),
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
