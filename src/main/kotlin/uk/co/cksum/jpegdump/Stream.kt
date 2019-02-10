/*
 * jpegdump - Dumps the markers of a jpeg to aid analysis
 * Copyright (C) 2019 Samuel Lee
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package uk.co.cksum.jpegdump

import java.io.*

/**
 * Random Access File Stream to read from
 */
class Stream(file: File) {
    var randomAccessFile: RandomAccessFile = RandomAccessFile(file, "r")
    private var bytePosition: Long = 0
    private var byteBuffer: InputStream? = null
    private var bitBuffer: Long = 0
    private var bitBufferLen: Int = 0

    init {
        seekTo(0)
    }

    fun seekTo(pos: Long) {
        randomAccessFile.seek(pos)
        bytePosition = pos
        byteBuffer = BufferedInputStream(object : InputStream() {
            @Throws(IOException::class)
            override fun read(): Int {
                return randomAccessFile.read()
            }

            @Throws(IOException::class)
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                return randomAccessFile.read(b, off, len)
            }
        })
        bitBufferLen = 0
    }

    fun readUint(n: Int): Int {
        while (bitBufferLen < n) {
            val temp: Int = byteBuffer!!.read()
            if (temp == -1) {
                throw EOFException()
            }
            bytePosition++
            bitBuffer = (bitBuffer shl 8) or temp.toLong()
            bitBufferLen += 8
        }

        bitBufferLen -= n
        var result: Int = (bitBuffer ushr bitBufferLen).toInt()
        if (n < 32) {
            result = result and (1 shl n) - 1
        }
        return result
    }

    fun readByte(): Int {
        if (bitBufferLen >= 8) {
            return readUint(8)
        } else {
            val result: Int = byteBuffer!!.read()
            if (result != -1) {
                bytePosition++
            }
            return result
        }
    }

    fun readSignedInt(n: Int): Int {
        return readUint(n) shl 32 - n shr 32 - n
    }

    fun getPosition(): Long {
        return bytePosition
    }

    fun alignToByte() {
        bitBufferLen -= bitBufferLen % 8
    }
}