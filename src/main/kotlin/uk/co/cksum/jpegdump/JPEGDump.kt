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

    println("Marker\tName                   Position\tLength/Action")
    println("ffd8\tSOI                    0")

    try {
        var done: Boolean = false
        while (!done) {
            val current = stream.readUint(16)
            when(current) {
                0xFFE0, 0xFFE1, 0xFFE2, 0xFFE3, 0xFFE4, 0xFFE5, 0xFFE6, 0xFFE7, 0xFFE8, 0xFFE9, 0xFFEA, 0xFFEB, 0xFFEC, 0xFFED, 0xFFEE, 0xFFEF -> {
                    printAndSkipAPP(current, stream)
                }
                0xFFC0, 0xFFC1, 0xFFC2, 0xFFC3, 0xFFC5, 0xFFC6, 0xFFC7, 0xFFC9, 0xFFCA, 0xFFCB, 0xFFCD, 0xFFCE, 0xFFCF -> {
                    printAndSkipMaths(current, stream)
                }
                0xFFC4, 0xFFC8, 0xFFCC, 0xFFDB, 0xFFDC, 0xFFDD, 0xFFDE -> {
                    printAndSkipDefines(current, stream)
                }
                0xFFDA -> {
                    print(current.toString(16))
                    print("\tStart of Scan          ")
                    print(stream.getPosition().toString(16))
                    print("\n")
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
                    print("${current.toString(16)} \tEOI                    ${stream.getPosition().toString(16)}")
                    done = true
                }
                else -> {
                    println("Current: ${current.toString(16)} \tposition:${stream.getPosition().toString(16)}")
                    println("Didn't find a marker")
                    done = true
                }
            }
        }
    } catch (e:EOFException) {
        error("Got to end of file before finding an EOI marker")
    }
}

fun printAndSkipAPP(current: Int, stream: Stream) {
    print(current.toString(16))
    when(current) {
        0xFFE0 -> print("\tJFIF                   ")
        0xFFE1 -> print("\tEXIF or XMP            ")
        0xFFE2 -> print("\tICC                    ")
        0xFFE3 -> print("\tMETA                   ")
        0xFFE4 -> print("\tSCALADO                ")
        0xFFE5 -> print("\tSAMSUNG or RMETA       ")
        0xFFE6 -> print("\tGOPRO EPPIM            ")
        0xFFE7 -> print("\tPENTAX or QUALCOMM     ")
        0xFFE8 -> print("\tSPIFF                  ")
        0xFFE9 -> print("\tMEDIA JUKEBOX          ")
        0xFFEA -> print("\tPHOTOSHOP COMMENT      ")
        0xFFEB -> print("\tJPEG-HDR               ")
        0xFFEC -> print("\tPICTURE INFO or Ducky  ")
        0xFFED -> print("\tPHOTOSHOP IRB          ")
        0xFFEE -> print("\tADOBE                  ")
        0xFFEF -> print("\tGRAPHCONV              ")
    }
    print(stream.getPosition().toString(16))
    val length = stream.readUint(16)
    print("\t\tLength: ${length.toString(16)}\n")
    stream.seekTo(stream.getPosition() + length - 2)
}

fun printAndSkipMaths(current: Int, stream: Stream) {
    print(current.toString(16))
    when(current) {
        0xFFC0 -> {
            print("\tStart of Frame         ")
            print(stream.getPosition().toString(16))
            // Header len
            stream.readUint(16)
            // Sample precision
            stream.readUint(8)
            print("\t\tWidth: ${stream.readUint(16)}, Height: ${stream.readUint(16)}.")
        }
        0xFFC1 -> print("\tExt Sequential Huffman ")
        0xFFC2 -> print("\tProgressive Huffman    ")
        0xFFC3 -> print("\tLossless Huffman       ")
        0xFFC5 -> print("\tDiff Seq Huffman       ")
        0xFFC6 -> print("\tDiff Prog Huffman      ")
        0xFFC7 -> print("\tDiff Lossless Huffman  ")
        0xFFC9 -> print("\tExt. Seq Arithmetic    ")
        0xFFCA -> print("\tProgressive Arithmetic ")
        0xFFCB -> print("\tLossless Arithmetic    ")
        0xFFCD -> print("\tDiff Seq Arithmetic    ")
        0xFFCE -> print("\tDiff Prog Arithmetic   ")
        0xFFCF -> print("\tDiff Lossless Arith    ")
    }
    if(current != 0xFFC0) print(stream.getPosition().toString(16))
    print("\n")
    // Skip to next marker
    var marker: Boolean = false
    while (!marker) {
        marker = stream.readUint(8) == 0xFF
    }
    stream.seekTo(stream.getPosition() - 1)
}

fun printAndSkipDefines(current: Int, stream: Stream) {
    print(current.toString(16))
    when(current) {
        0xFFC4 -> print("\tDefine Huffman Table   ")
        0xFFC8 -> print("\tReserved JPEG Ext.     ")
        0xFFCC -> print("\tDefine Arith Code Conds")
        0xFFDB -> print("\tDefine Quant Table     ")
        0xFFDC -> print("\tDefine Number of Lines ")
        0xFFDD -> print("\tDefine Restart Interval")
        0xFFDE -> print("\tDefine Hierarch Prog   ")
    }
    print(stream.getPosition().toString(16))
    print("\n")
    // Skip to next marker
    var marker: Boolean = false
    while (!marker) {
        marker = stream.readUint(8) == 0xFF
    }
    stream.seekTo(stream.getPosition() - 1)
}
