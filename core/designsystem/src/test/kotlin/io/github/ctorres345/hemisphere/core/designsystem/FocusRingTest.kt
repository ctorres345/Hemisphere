package io.github.ctorres345.hemisphere.core.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The ring's behaviour without goldens: it draws `accent.focus` only on a focused element in Gamepad mode. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class FocusRingTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun ringPixels(
        mode: InputMode,
        focused: Boolean,
        size: Dp = HemisphereSizes.hintFooter,
    ): Int {
        var focusColor = Color.Unspecified
        compose.setContent {
            HemisphereTheme {
                focusColor = HemisphereTheme.colors.accent.focus
                CompositionLocalProvider(LocalInputMode provides mode) {
                    val focus = remember { FocusRequester() }
                    Box(
                        Modifier
                            .testTag("box")
                            .size(size)
                            .background(HemisphereTheme.colors.surfaceCanvas)
                            .focusRequester(focus)
                            .focusRing()
                            .focusable(),
                    )
                    if (focused) LaunchedEffect(Unit) { focus.requestFocus() }
                }
            }
        }
        if (focused) compose.onNodeWithTag("box").assertIsFocused()
        compose.waitForIdle()
        // Draw the window into a bitmap (captureToImage times out under Robolectric) and count ring pixels.
        val root = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val ring = focusColor.toArgb()
        return pixels.count { it == ring }
    }

    @Test
    fun `gamepad mode draws the ring on the focused element`() {
        assertTrue(ringPixels(InputMode.Gamepad, focused = true) > 0)
    }

    @Test
    fun `an element thinner than the stroke draws nothing and does not crash`() {
        assertEquals(0, ringPixels(InputMode.Gamepad, focused = true, size = HemisphereSizes.border))
    }

    @Test
    fun `touch mode draws no ring`() {
        assertEquals(0, ringPixels(InputMode.Touch, focused = true))
    }

    @Test
    fun `an unfocused element has no ring`() {
        assertEquals(0, ringPixels(InputMode.Gamepad, focused = false))
    }
}
