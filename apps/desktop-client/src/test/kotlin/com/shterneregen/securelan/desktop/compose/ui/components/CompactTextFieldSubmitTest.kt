package com.shterneregen.securelan.desktop.compose.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.shterneregen.securelan.desktop.compose.SecureLanTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class CompactTextFieldSubmitTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun enterSubmitsTheCurrentTextOnce() {
        val submissions = mutableListOf<String>()
        val focusRequester = FocusRequester()
        compose.setContent {
            SecureLanTheme {
                var text by remember { mutableStateOf("") }
                Row {
                    CompactTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = "",
                        modifier = Modifier.weight(1f),
                        inputModifier = Modifier.focusRequester(focusRequester),
                        onSubmit = { submissions += text },
                    )
                }
            }
        }
        compose.runOnIdle { focusRequester.requestFocus() }
        compose.onNode(hasSetTextAction()).assertIsFocused()
        compose.onNode(hasSetTextAction()).performTextInput("hello")
        compose.onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(listOf("hello"), submissions) }
    }

    @Test
    fun numpadEnterSubmitsTheCurrentTextOnce() {
        val submissions = mutableListOf<String>()
        compose.setContent {
            SecureLanTheme {
                var text by remember { mutableStateOf("") }
                CompactTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "",
                    onSubmit = { submissions += text },
                )
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("hello")
        compose.onNode(hasSetTextAction()).performKeyInput { pressKey(Key.NumPadEnter) }
        compose.runOnIdle { assertEquals(listOf("hello"), submissions) }
    }

    @Test
    fun imeSendSubmitsTheCurrentTextOnce() {
        val submissions = mutableListOf<String>()
        compose.setContent {
            SecureLanTheme {
                var text by remember { mutableStateOf("") }
                CompactTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "",
                    onSubmit = { submissions += text },
                )
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("hello")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.runOnIdle { assertEquals(listOf("hello"), submissions) }
    }
}
