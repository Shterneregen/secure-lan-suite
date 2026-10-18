package com.shterneregen.securelan.androidclient.network.discovery

import android.os.Build
import android.os.ext.SdkExtensions

/** Extension checks live here; platform calls also use lint-recognized baseline guards. */
data class NsdApiCapabilities(val networkScoped: Boolean, val serviceUpdates: Boolean) {
    companion object {
        fun current(): NsdApiCapabilities {
            val extension = if (Build.VERSION.SDK_INT >= 33) SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) else 0
            return NsdApiCapabilities(Build.VERSION.SDK_INT >= 33 || extension >= 3, Build.VERSION.SDK_INT >= 34 || extension >= 7)
        }
    }
}
