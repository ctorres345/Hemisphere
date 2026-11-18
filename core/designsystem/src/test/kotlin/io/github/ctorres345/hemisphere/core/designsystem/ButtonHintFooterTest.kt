package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// Robolectric needs Java 21 for API 35 and up; the build runs on JDK 17 (ADR 0003).
@Config(sdk = [34])
class ButtonHintFooterTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the footer is absent in touch mode`() {
        show(InputMode.Touch, FaceButtonLayout.Xbox)
        compose.onNodeWithText("Open").assertDoesNotExist()
        compose.onNodeWithText("Pane").assertDoesNotExist()
    }

    @Test
    fun `the footer shows every hint in gamepad mode`() {
        show(InputMode.Gamepad, FaceButtonLayout.Xbox)
        compose.onNodeWithText("Open").assert(hasText("A"))
        compose.onNodeWithText("Back").assert(hasText("B"))
        compose.onNodeWithText("Menu").assert(hasText("X"))
        compose.onNodeWithText("Mark").assert(hasText("Y"))
        compose.onNodeWithText("Pane").assert(hasText("L1")).assert(hasText("R1"))
    }

    @Test
    fun `the Nintendo layout swaps A with B and X with Y`() {
        show(InputMode.Gamepad, FaceButtonLayout.Nintendo)
        compose.onNodeWithText("Open").assert(hasText("B"))
        compose.onNodeWithText("Back").assert(hasText("A"))
        compose.onNodeWithText("Menu").assert(hasText("Y"))
        compose.onNodeWithText("Mark").assert(hasText("X"))
    }

    @Test
    fun `each action keeps its glyph pair across layouts`() {
        val xbox = faceHints(FaceButtonLayout.Xbox).associate { (glyph, label) -> label to glyph }
        val nintendo = faceHints(FaceButtonLayout.Nintendo).associate { (glyph, label) -> label to glyph }
        val swap = mapOf("A" to "B", "B" to "A", "X" to "Y", "Y" to "X")
        xbox.forEach { (label, glyph) -> assertEquals(swap.getValue(glyph), nintendo.getValue(label)) }
    }

    @Test
    fun `touch mode uses default rows and gamepad mode compact rows`() {
        assertEquals(HemisphereRowHeight.default, InputMode.Touch.rowHeight)
        assertEquals(HemisphereRowHeight.compact, InputMode.Gamepad.rowHeight)
    }

    private fun show(
        mode: InputMode,
        layout: FaceButtonLayout,
    ) {
        compose.setContent {
            HemisphereTheme(style = ThemeStyle.Light) {
                CompositionLocalProvider(LocalInputMode provides mode) { ButtonHintFooter(layout = layout) }
            }
        }
    }
}
