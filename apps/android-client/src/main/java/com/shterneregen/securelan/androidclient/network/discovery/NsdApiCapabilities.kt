package com.shterneregen.securelan.androidclient.network.discovery

import android.os.Build

/** API-level checks live here; platform calls also use lint-recognized baseline guards. */
data class NsdApiCapabilities(val networkScoped: Boolean, val serviceUpdates: Boolean) {
    companion object {
        fun current(): NsdApiCapabilities {
            return NsdApiCapabilities(Build.VERSION.SDK_INT >= 33, Build.VERSION.SDK_INT >= 34)
        }
    }
}
