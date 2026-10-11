package com.shterneregen.securelan.androidclient.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalAppResources = staticCompositionLocalOf<Resources> {
    error("Localized application resources are not available")
}

/** Read resources from the application's selected language, including inside dialogs. */
@Composable
internal fun appStringResource(id: Int, vararg formatArgs: Any): String =
    LocalAppResources.current.getString(id, *formatArgs)
