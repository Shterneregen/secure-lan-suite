package com.shterneregen.securelan.desktop.compose

import java.io.InputStream

object ComposeDesktopResources {
    const val APP_ICON_PNG: String = "icons/app-icon.png"
    const val APP_ICON_ICO: String = "icons/app-icon.ico"
    const val JAVA_FX_APP_ICON_RESOURCE: String = "/$APP_ICON_PNG"

    @JvmStatic
    fun openAppIconStream(): InputStream? = javaClass.getResourceAsStream(JAVA_FX_APP_ICON_RESOURCE)
}
