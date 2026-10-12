package com.shterneregen.securelan.androidclient.transfer

import java.io.InputStream
import java.io.OutputStream

/** Shared streaming checks: exact sender length; legacy receiver zero means unknown. */
internal fun validateTransferLength(expected: Long, actual: Long, complete: Boolean, legacyUnknown: Boolean = false) {
    require(expected >= 0 && actual >= 0) { "Invalid file length" }
    if (legacyUnknown && expected == 0L) return
    require(actual <= expected && (!complete || actual == expected)) { "File length changed" }
}

/** Copies with bounded memory and cancellation checks, returning the exact length without closing streams. */
internal fun stageStream(input: InputStream, output: OutputStream, checkActive: () -> Unit): Long {
    val buffer = ByteArray(16 * 1024)
    var length = 0L
    while (true) {
        checkActive()
        val count = input.read(buffer)
        if (count == -1) break
        checkActive()
        check(length <= Long.MAX_VALUE - count)
        output.write(buffer, 0, count)
        length += count
    }
    return length
}

/** Appends a duplicate suffix before the extension; index zero preserves the original name. */
internal fun numberedTransferFileName(fileName: String, index: Int): String {
    require(index >= 0) { "Invalid duplicate index" }
    if (index == 0) return fileName
    val dot = fileName.lastIndexOf('.')
    val base = if (dot > 0) fileName.substring(0, dot) else fileName
    val extension = if (dot > 0) fileName.substring(dot) else ""
    return "$base-$index$extension"
}
