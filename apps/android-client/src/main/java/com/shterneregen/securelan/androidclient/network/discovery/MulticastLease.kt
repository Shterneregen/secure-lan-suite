package com.shterneregen.securelan.androidclient.network.discovery

import android.content.Context
import android.net.wifi.WifiManager

/** Each browser/advertiser owns its own legacy Wi-Fi lease; modern foreground NSD needs none. */
interface MulticastLeaseOwner : AutoCloseable { fun acquire() }
class MulticastLease(context: Context) : MulticastLeaseOwner {
    private val manager = context.applicationContext.getSystemService(WifiManager::class.java)
    private var lock: WifiManager.MulticastLock? = null
    override fun acquire() {
        if (NsdApiCapabilities.current().serviceUpdates || lock != null) return
        lock = manager.createMulticastLock("securelan-nsd").apply { setReferenceCounted(false); acquire() }
    }
    override fun close() { lock?.let { if (it.isHeld) it.release() }; lock = null }
}
