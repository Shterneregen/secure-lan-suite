package com.shterneregen.securelan.androidclient.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object AndroidClipboard {
    fun copyMessage(context: Context, message: String) {
        copyText(context, "SecureLan message", message)
    }

    fun copyLogs(context: Context, logs: String) {
        copyText(context, "SecureLan logs", logs)
    }

    private fun copyText(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
