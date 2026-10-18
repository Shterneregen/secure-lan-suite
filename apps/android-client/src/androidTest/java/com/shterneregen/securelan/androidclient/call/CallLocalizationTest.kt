package com.shterneregen.securelan.androidclient.call

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.ui.LocalAppResources
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** Call menus must use the selected app language even when framework resources use another locale. */
@RunWith(AndroidJUnit4::class)
class CallLocalizationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun englishCallMenuIgnoresRussianFrameworkLocale() = verifyMenu(Locale.ENGLISH, Locale.forLanguageTag("ru"))

    @Test fun russianCallMenuIgnoresEnglishFrameworkLocale() = verifyMenu(Locale.forLanguageTag("ru"), Locale.ENGLISH)

    private fun verifyMenu(appLocale: Locale, frameworkLocale: Locale) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fun localized(locale: Locale) = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(locale))
        })
        val appResources = localized(appLocale).resources
        val frameworkContext = localized(frameworkLocale)
        val calls = mutableListOf<Pair<String, Boolean>>()
        compose.setContent {
            CompositionLocalProvider(
                LocalAppResources provides appResources,
                LocalContext provides frameworkContext,
                LocalConfiguration provides frameworkContext.resources.configuration,
            ) {
                MaterialTheme { CallButtons(mapOf("Atlas" to true), true) { peer, video -> calls += peer to video } }
            }
        }
        val start = appResources.getString(R.string.call_start)
        val audio = appResources.getString(R.string.call_audio_peer, "Atlas")
        val video = appResources.getString(R.string.call_video_peer, "Atlas")
        compose.onNodeWithText(appResources.getString(R.string.call_title)).assertIsDisplayed()
        compose.onNodeWithText(start).performClick()
        compose.onNodeWithText(audio).assertIsDisplayed()
        compose.onNodeWithText(video).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(listOf("Atlas" to true), calls) }
        compose.onNodeWithText(start).performClick()
        compose.onNodeWithText(audio).performClick()
        compose.runOnIdle { assertEquals(listOf("Atlas" to true, "Atlas" to false), calls) }
    }
}
