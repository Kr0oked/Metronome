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

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Gaps
import com.bobek.metronome.data.Sound
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo
import com.bobek.metronome.data.Tick
import com.bobek.metronome.domain.Metronome
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

private const val TAG = "MetronomeService"
private const val NOTIFICATION_CHANNEL_PLAYBACK_ID = "metronome-playback"
private const val NOTIFICATION_ID = 1
private const val NO_REQUEST_CODE = 0
private const val ACTION_STOP = "com.bobek.metronome.intent.action.STOP"

interface IMetronomeService {
    var beats: Beats
    var subdivisions: Subdivisions
    var gaps: Gaps
    var tempo: Tempo
    var emphasizeFirstBeat: Boolean
    var sound: Sound
    var playing: Boolean

    fun getTickFlow(): SharedFlow<Tick>

    fun getRefreshFlow(): SharedFlow<Unit>
}

@AndroidEntryPoint
class MetronomeService : LifecycleService(), IMetronomeService {

    private var metronome: Metronome? = null

    private var bound = false

    private val tickFlow = MutableSharedFlow<Tick>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val refreshFlow = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override var beats: Beats = Beats()
        get() = metronome?.beats ?: field
        set(beats) {
            field = beats
            metronome?.beats = beats
        }

    override var subdivisions: Subdivisions = Subdivisions()
        get() = metronome?.subdivisions ?: field
        set(subdivisions) {
            field = subdivisions
            metronome?.subdivisions = subdivisions
        }

    override var gaps: Gaps = Gaps()
        get() = metronome?.gaps ?: field
        set(gaps) {
            field = gaps
            metronome?.gaps = gaps
        }

    override var tempo: Tempo = Tempo()
        get() = metronome?.tempo ?: field
        set(tempo) {
            field = tempo
            metronome?.tempo = tempo
        }

    override var emphasizeFirstBeat: Boolean = true
        get() = metronome?.emphasizeFirstBeat ?: field
        set(emphasizeFirstBeat) {
            field = emphasizeFirstBeat
            metronome?.emphasizeFirstBeat = emphasizeFirstBeat
        }

    override var sound: Sound = Sound.SQUARE_WAVE
        get() = metronome?.sound ?: field
        set(sound) {
            field = sound
            metronome?.sound = sound
        }

    override var playing: Boolean
        get() = metronome?.playing == true
        set(playing) = if (playing) startMetronome() else stopMetronome()

    override fun getTickFlow(): SharedFlow<Tick> = tickFlow

    override fun getRefreshFlow(): SharedFlow<Unit> = refreshFlow

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Lifecycle: onCreate")
        NotificationManagerCompat.from(this)
            .createNotificationChannel(buildPlaybackNotificationChannel())

        metronome = Metronome(this, lifecycle) { tickFlow.tryEmit(it) }
        metronome?.beats = beats
        metronome?.subdivisions = subdivisions
        metronome?.gaps = gaps
        metronome?.tempo = tempo
        metronome?.emphasizeFirstBeat = emphasizeFirstBeat
        metronome?.sound = sound
    }

    private fun buildPlaybackNotificationChannel() = NotificationChannelCompat
        .Builder(NOTIFICATION_CHANNEL_PLAYBACK_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
        .setName(getString(R.string.notification_channel_playback_name))
        .setDescription(getString(R.string.notification_channel_playback_description))
        .setImportance(NotificationManagerCompat.IMPORTANCE_HIGH)
        .build()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Lifecycle: onStartCommand")
        if (intent?.action == ACTION_STOP) {
            performStop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(TAG, "Lifecycle: onBind")
        bound = true
        return LocalBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Lifecycle: onUnbind")
        bound = false
        return true
    }

    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "Lifecycle: onRebind")
        bound = true
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Lifecycle: onDestroy")
        playing = false
        metronome = null
    }

    private fun startMetronome() {
        Log.i(TAG, "Start metronome")
        metronome?.playing = true
        startForegroundNotification()
    }

    private fun startForegroundNotification() {
        val mainActivityPendingIntent = Intent(this, MainActivity::class.java)
            .let { PendingIntent.getActivity(this, NO_REQUEST_CODE, it, PendingIntent.FLAG_IMMUTABLE) }

        val stopMetronomePendingIntent = Intent(this, MetronomeService::class.java)
            .apply { action = ACTION_STOP }
            .let { PendingIntent.getService(this, NO_REQUEST_CODE, it, PendingIntent.FLAG_IMMUTABLE) }

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_PLAYBACK_ID)
            .setContentTitle(getString(R.string.notification_playback_title))
            .setSmallIcon(R.drawable.ic_metronome)
            .setSilent(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.notification_playback_action_stop_title),
                stopMetronomePendingIntent
            )
            .setContentIntent(mainActivityPendingIntent)
            .build()

        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        Log.d(TAG, "Foreground service started")
    }

    private fun stopMetronome() {
        Log.i(TAG, "Stop metronome")
        metronome?.playing = false
        stopForegroundNotification()
        stopNoLongerNeededService()
    }

    private fun stopForegroundNotification() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "Foreground service stopped")
    }

    private fun stopNoLongerNeededService() {
        if (!bound) {
            Log.d(TAG, "Stop no longer needed service")
            stopSelf()
        }
    }

    private fun performStop() {
        Log.d(TAG, "Received stop command")
        playing = false
        refreshFlow.tryEmit(Unit)
    }


    inner class LocalBinder : Binder() {
        fun getService(): IMetronomeService = this@MetronomeService
    }
}
