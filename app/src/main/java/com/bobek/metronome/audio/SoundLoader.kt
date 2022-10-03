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

package com.bobek.metronome.audio

import com.google.common.primitives.Bytes
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

object SoundLoader {

    private const val DATA_CHUNK_SIZE = 8

    private val DATA_MARKER = "data".toByteArray(Charset.forName("ASCII"))

    @JvmStatic
    fun readDataFromWavPcmFloat(input: InputStream): FloatArray {
        val bytes = input.readBytes()
        val indexOfDataMarker = Bytes.indexOf(bytes, DATA_MARKER)
        check(indexOfDataMarker >= 0) { "Could not find data marker in the content" }

        val startOfContent = indexOfDataMarker + DATA_CHUNK_SIZE
        check(startOfContent <= bytes.size) { "Too short data chunk" }

        val byteBuffer = ByteBuffer.wrap(bytes, startOfContent, bytes.size - startOfContent)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val floatBuffer = byteBuffer.asFloatBuffer()

        val floatArray = FloatArray(floatBuffer.remaining())
        floatBuffer[floatArray]
        return floatArray
    }
}
