package io.github.ctorres345.hemisphere.core.input

import io.github.ctorres345.hemisphere.core.designsystem.InputMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InputModeTrackerTest {
    @Test
    fun startsInTouch() {
        assertEquals(InputMode.Touch, InputModeTracker().mode.value)
    }

    @Test
    fun gamepadEventFlipsToGamepadAndTouchFlipsBack() {
        val tracker = InputModeTracker()
        tracker.onGamepad()
        assertEquals(InputMode.Gamepad, tracker.mode.value)
        tracker.onTouch()
        assertEquals(InputMode.Touch, tracker.mode.value)
    }

    @Test
    fun keyboardThenTouchThenGamepadSwitchInTurn() {
        val tracker = InputModeTracker()
        tracker.onInput(InputMode.Keyboard)
        assertEquals(InputMode.Keyboard, tracker.mode.value)
        tracker.onTouch()
        assertEquals(InputMode.Touch, tracker.mode.value)
        tracker.onGamepad()
        assertEquals(InputMode.Gamepad, tracker.mode.value)
        tracker.onInput(InputMode.Keyboard)
        assertEquals(InputMode.Keyboard, tracker.mode.value)
    }

    @Test
    fun emitsOnlyOnTheFirstEventOfTheOtherKind() =
        runTest(UnconfinedTestDispatcher()) {
            val tracker = InputModeTracker()
            val seen = mutableListOf<InputMode>()
            val job = launch { tracker.mode.collect { seen += it } }
            repeat(3) { tracker.onTouch() }
            repeat(3) { tracker.onGamepad() }
            repeat(2) { tracker.onTouch() }
            job.cancel()
            assertEquals(listOf(InputMode.Touch, InputMode.Gamepad, InputMode.Touch), seen)
        }
}
