package io.github.ctorres345.hemisphere.core.designsystem

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.ui.input.key.Key as ComposeKey

/** A popup's content reports its keys and taps, which the Activity's tracker never sees. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReportsInputTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val keys = mutableListOf<Int>()
    private var pointers = 0

    private val reporter =
        object : InputReporter {
            override fun onKey(event: KeyEvent) {
                keys += event.keyCode
            }

            override fun onPointer() {
                pointers++
            }
        }

    private fun show() {
        compose.setContent {
            CompositionLocalProvider(LocalInputReporter provides reporter) {
                val focus = remember { FocusRequester() }
                Box(
                    Modifier
                        .reportsInput()
                        .testTag("content")
                        .size(HemisphereSizes.hintFooter)
                        .focusRequester(focus)
                        .focusable(),
                )
                LaunchedEffect(Unit) { focus.requestFocus() }
            }
        }
    }

    @Test
    fun aTapIsReported() {
        show()
        compose.onNodeWithTag("content").performClick()
        assertEquals(1, pointers)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aKeyOnTheFocusedContentIsReported() {
        show()
        compose.onNodeWithTag("content").performKeyInput { pressKey(ComposeKey.DirectionDown) }
        assertEquals(KeyEvent.KEYCODE_DPAD_DOWN, keys.first())
    }
}
