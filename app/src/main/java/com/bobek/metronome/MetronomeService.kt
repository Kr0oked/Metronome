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

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bobek.metronome.domain.*
import java.util.*

private const val TAG = "MetronomeService"
private const val MINUTE_IN_MILLIS = 60_000L
private const val NOTIFICATION_CHANNEL_PLAYBACK_ID = "metronome-playback"
private const val NOTIFICATION_ID = 1
private const val NO_REQUEST_CODE = 0

class MetronomeService : Service() {

    private val stopReceiver = StopReceiver()

    private lateinit var metronomePlayer: MetronomePlayer

    private var timer = Timer()
    private var counter = 0L
    private var lastTickTime = 0L

    var beats: Beats = Beats()
        set(beats) {
            if (field != beats) {
                field = beats
                metronomeParametersChanged()
            }
        }

    var subdivisions: Subdivisions = Subdivisions()
        set(subdivisions) {
            if (field != subdivisions) {
                field = subdivisions
                metronomeParametersChanged()
            }
        }

    var tempo: Tempo = Tempo()
        set(tempo) {
            if (field != tempo) {
                field = tempo
                metronomeParametersChanged()
            }
        }

    var playing = false
        set(playing) {
            if (field != playing) {
                field = playing
                if (playing) startMetronome() else stopMetronome()
            }
        }

    override fun onCreate() {
        NotificationManagerCompat.from(this)
            .createNotificationChannel(buildPlaybackNotificationChannel())
        registerReceiver(stopReceiver, IntentFilter(ACTION_STOP))
        metronomePlayer = MetronomePlayer(this)
    }

    private fun buildPlaybackNotificationChannel() = NotificationChannelCompat
        .Builder(NOTIFICATION_CHANNEL_PLAYBACK_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
        .setName(getString(R.string.notification_channel_playback_name))
        .setDescription(getString(R.string.notification_channel_playback_description))
        .build()

    override fun onBind(intent: Intent): IBinder = LocalBinder()

    override fun onDestroy() {
        playing = false
        metronomePlayer.release()
        unregisterReceiver(stopReceiver)
    }

    private fun startMetronome() {
        Log.i(TAG, "Start metronome")
        startForegroundNotification()
        resetTimer()
        counter = 0L
        lastTickTime = System.currentTimeMillis()
        timer.scheduleAtFixedRate(getTickerTask(), Date(lastTickTime), calculateTickerPeriod())
    }

    private fun calculateTickerPeriod() = MINUTE_IN_MILLIS / tempo.value / subdivisions.value

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
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.notification_playback_action_stop_title),
                stopPendingIntent
            )
            .setContentIntent(mainActivityPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun getPendingIntentFlags() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }

    private fun stopMetronome() {
        Log.i(TAG, "Stop metronome")
        stopForeground(true)
        resetTimer()
    }

    private fun metronomeParametersChanged() {
        if (playing) {
            adjustMetronome()
        }
    }

    private fun adjustMetronome() {
        resetTimer()
        val tickerPeriod = calculateTickerPeriod()
        val startTime = lastTickTime + tickerPeriod
        timer.scheduleAtFixedRate(getTickerTask(), Date(startTime), tickerPeriod)
        Log.i(TAG, "Adjusted metronome")
    }

    private fun resetTimer() {
        timer.cancel()
        timer = Timer()
    }

    private fun getTickerTask() = object : TimerTask() {
        override fun run() {
            lastTickTime = System.currentTimeMillis()
            val currentBeat = getCurrentBeat()
            val currentTickType = getCurrentTickType()
            val tick = Tick(currentBeat, currentTickType)
            onTick(tick)
            counter++
        }
    }

    private fun getCurrentBeat() = (((counter / subdivisions.value) % beats.value) + 1).toInt()

    private fun getCurrentTickType(): TickType {
        return when {
            isStrongTick() -> TickType.STRONG
            isWeakTick() -> TickType.WEAK
            else -> TickType.SUB
        }
    }

    private fun isStrongTick() = counter % (beats.value * subdivisions.value) == 0L

    private fun isWeakTick() = counter % subdivisions.value == 0L

    private fun onTick(tick: Tick) {
        Log.d(TAG, "Playing tick $counter $tick")
        metronomePlayer.play(tick.type)
        publishTick(tick)
    }

    private fun publishTick(tick: Tick) {
        Intent(ACTION_TICK)
            .apply { putExtra(EXTRA_TICK, tick) }
            .let { LocalBroadcastManager.getInstance(this).sendBroadcast(it) }
    }

    inner class LocalBinder : Binder() {
        fun getService(): MetronomeService = this@MetronomeService
    }

    inner class StopReceiver : BroadcastReceiver() {
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

    companion object {
        private const val ACTION_STOP = "com.bobek.metronome.intent.action.STOP"
        const val ACTION_REFRESH = "com.bobek.metronome.intent.action.REFRESH"
        const val ACTION_TICK = "com.bobek.metronome.intent.action.TICK"
        const val EXTRA_TICK = "com.bobek.metronome.intent.extra.TICK"
    }
}
