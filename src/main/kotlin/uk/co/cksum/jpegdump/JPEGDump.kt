/*
 * JPEGDump - Dumps the markers of a jpeg to aid analysis
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

import java.io.File
import java.io.EOFException

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("No file specified")
        return
    }

    val file = File(args[0])
    val stream = Stream(file)

    if (stream.readUint(16) != 0xFFD8) {
        println("Invalid JPEG: Start of image marker not found")
        return
    }

    println("Current: ffd8 \tSOI")

    try {
        var done: Boolean = false
        while (!done) {
            val current = stream.readUint(16)
            when(current) {
                0xFFE0, 0xFFE1, 0xFFE2, 0xFFE3, 0xFFE4, 0xFFE5, 0xFFE6, 0xFFE7, 0xFFE8, 0xFFE9, 0xFFEA, 0xFFEB, 0xFFEC, 0xFFED, 0xFFEE -> {
                    print("Current: ${current.toString(16)}")
                    val length = stream.readUint(16)
                    println("\tLength: ${length.toString(16)}")
                    stream.seekTo(stream.getPosition() + length - 2)
                }
                0xFFC0, 0xFFC1, 0xFFC2, 0xFFC3, 0xFFC5, 0xFFC6, 0xFFC7, 0xFFC9, 0xFFCA, 0xFFCB, 0xFFCD, 0xFFCE, 0xFFCF -> {
                    print("Current: ${current.toString(16)}")
                    // Header len
                    stream.readUint(16)
                    // Sample precision
                    stream.readUint(8)
                    print("\tWidth: ${stream.readUint(16)}, Height: ${stream.readUint(16)}.\n")
                    // Skip to next marker
                    var marker: Boolean = false
                    while (!marker) {
                        marker = stream.readUint(8) == 0xFF
                    }
                    stream.seekTo(stream.getPosition() - 1)
                }
                0xFFC4, 0xFFC8, 0xFFCC, 0xFFDA, 0xFFDB -> {
                    print("Current: ${current.toString(16)}")
                    print("\tSkipping to next marker\n")
                    // Skip to next marker
                    var marker: Boolean = false
                    while (!marker) {
                        marker = stream.readUint(8) == 0xFF
                    }
                    stream.seekTo(stream.getPosition() - 1)
                }
                0xFF00 -> {
                    //skip
                    var marker: Boolean = false
                    while (!marker) {
                        marker = stream.readUint(8) == 0xFF
                    }
                    stream.seekTo(stream.getPosition() - 1)
                }
                0xFFD9 -> {
                    print("Current: ${current.toString(16)}")
                    print("\tEnd of image\n")
                    done = true
                }
                else -> {
                    println("Current: ${current.toString(16)}")
                    println("Didn't find a marker")
                    done = true
                }
            }
        }
    } catch (e:EOFException) {
        error("Got to end of file before finding an EOI marker")
    }
}
