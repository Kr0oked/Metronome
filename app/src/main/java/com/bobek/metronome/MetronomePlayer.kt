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

import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

private const val MEDIA_ITEM_UID = "metronome"

/**
 * Minimal [Player] backing the media session that exists while the metronome is playing.
 *
 * It only exposes the playing state and the commands to stop it, so that system media controls (lock screen, quick
 * settings, headset buttons) can stop playback without unlocking the device. Starting playback is left to the app.
 *
 * @param title Shown as the media title in the system media controls.
 * @param onStop Called (posted to [looper]) when a media controller requests to pause or stop playback.
 */
@OptIn(markerClass = [UnstableApi::class])
class MetronomePlayer(
    looper: Looper,
    private val title: String,
    private val onStop: () -> Unit
) : SimpleBasePlayer(looper) {

    private val handler = Handler(looper)

    private var playing = true

    override fun getState(): State = State.Builder()
        .setAvailableCommands(
            Player.Commands.Builder()
                .addAll(
                    COMMAND_PLAY_PAUSE,
                    COMMAND_STOP,
                    COMMAND_GET_CURRENT_MEDIA_ITEM,
                    COMMAND_GET_METADATA
                )
                .build()
        )
        .setPlaylist(listOf(buildMediaItemData()))
        .setPlayWhenReady(playing, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        .setPlaybackState(if (playing) STATE_READY else STATE_IDLE)
        .build()

    private fun buildMediaItemData() = MediaItemData.Builder(MEDIA_ITEM_UID)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
        .build()

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (!playWhenReady) {
            requestStop()
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        requestStop()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()

    private fun requestStop() {
        if (playing) {
            playing = false
            // Posted because stopping releases this player, which must not happen while it handles a command.
            handler.post(onStop)
        }
    }
}
