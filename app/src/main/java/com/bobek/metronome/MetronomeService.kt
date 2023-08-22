/*
 * This file is part of Metronome.
 * Copyright (C) 2023 Philipp Bobek <philipp.bobek@mailbox.org>
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
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo
import com.bobek.metronome.data.Tick
import com.bobek.metronome.domain.Metronome

private const val TAG = "MetronomeService"
private const val NOTIFICATION_CHANNEL_PLAYBACK_ID = "metronome-playback"
private const val NOTIFICATION_ID = 1
private const val NO_REQUEST_CODE = 0

class MetronomeService : LifecycleService() {

    private val stopReceiver = StopReceiver()

    private lateinit var metronome: Metronome

    var beats: Beats
        get() = metronome.beats
        set(beats) {
            metronome.beats = beats
        }

    var subdivisions: Subdivisions
        get() = metronome.subdivisions
        set(subdivisions) {
            metronome.subdivisions = subdivisions
        }

    var tempo: Tempo
        get() = metronome.tempo
        set(tempo) {
            metronome.tempo = tempo
        }

    var emphasizeFirstBeat: Boolean
        get() = metronome.emphasizeFirstBeat
        set(emphasizeFirstBeat) {
            metronome.emphasizeFirstBeat = emphasizeFirstBeat
        }

    var playing: Boolean
        get() = metronome.playing
        set(playing) = if (playing) startMetronome() else stopMetronome()

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Lifecycle: onCreate")
        NotificationManagerCompat.from(this)
            .createNotificationChannel(buildPlaybackNotificationChannel())
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            registerReceiver(stopReceiver, IntentFilter(ACTION_STOP), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stopReceiver, IntentFilter(ACTION_STOP))
        }
        metronome = Metronome(this, lifecycle) { publishTick(it) }
    }

    private fun buildPlaybackNotificationChannel() = NotificationChannelCompat
        .Builder(NOTIFICATION_CHANNEL_PLAYBACK_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
        .setName(getString(R.string.notification_channel_playback_name))
        .setDescription(getString(R.string.notification_channel_playback_description))
        .setImportance(NotificationManagerCompat.IMPORTANCE_HIGH)
        .build()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Lifecycle: onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(TAG, "Lifecycle: onBind")
        return LocalBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Lifecycle: onUnbind")
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "Lifecycle: onRebind")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Lifecycle: onDestroy")
        playing = false
        unregisterReceiver(stopReceiver)
    }

    private fun startMetronome() {
        Log.i(TAG, "Start metronome")
        metronome.playing = true
        startForegroundNotification()
    }

    private fun startForegroundNotification() {
        val mainActivityPendingIntent =
            Intent(this, MainActivity::class.java)
                .let { intent -> PendingIntent.getActivity(this, NO_REQUEST_CODE, intent, getPendingIntentFlags()) }

        val stopPendingIntent = Intent(ACTION_STOP)
            .let { intent -> PendingIntent.getBroadcast(this, NO_REQUEST_CODE, intent, getPendingIntentFlags()) }

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
                stopPendingIntent
            )
            .setContentIntent(mainActivityPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "Foreground service started")
    }

    private fun getPendingIntentFlags() =
        if (VERSION.SDK_INT >= VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    private fun stopMetronome() {
        Log.i(TAG, "Stop metronome")
        metronome.playing = false
        stopForegroundNotification()
    }

    private fun stopForegroundNotification() {
        if (VERSION.SDK_INT >= VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        Log.d(TAG, "Foreground service stopped")
    }

    private fun publishTick(tick: Tick) {
        Intent(ACTION_TICK)
            .apply { putExtra(EXTRA_TICK, tick) }
            .let { LocalBroadcastManager.getInstance(this).sendBroadcast(it) }
    }

    private inner class StopReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Received stop command")
            performStop()
        }
    }

    private fun performStop() {
        playing = false
        Intent(ACTION_REFRESH)
            .let { LocalBroadcastManager.getInstance(this).sendBroadcast(it) }
    }

    inner class LocalBinder : Binder() {
        fun getService(): MetronomeService = this@MetronomeService
    }

    companion object {
        private const val ACTION_STOP = "com.bobek.metronome.intent.action.STOP"
        const val ACTION_REFRESH = "com.bobek.metronome.intent.action.REFRESH"
        const val ACTION_TICK = "com.bobek.metronome.intent.action.TICK"
        const val EXTRA_TICK = "com.bobek.metronome.intent.extra.TICK"
    }
}
