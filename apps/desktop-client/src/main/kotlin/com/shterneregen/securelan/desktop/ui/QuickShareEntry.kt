package com.shterneregen.securelan.desktop.ui

import com.shterneregen.securelan.filetransfer.quickshare.QuickShareSnapshot
import java.util.Objects

class QuickShareEntry(snapshot: QuickShareSnapshot) {
    private val snapshot: QuickShareSnapshot = Objects.requireNonNull(snapshot, "snapshot must not be null")

    fun snapshot(): QuickShareSnapshot = snapshot

    fun id(): String = snapshot.id()
    fun url(): String = snapshot.primaryUrl()
    fun active(): Boolean = snapshot.active()
}
