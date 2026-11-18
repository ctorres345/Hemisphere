package io.github.ctorres345.hemisphere.core.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import io.github.ctorres345.hemisphere.core.designsystem.InputMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The tracker fed real platform events, as MainActivity feeds it (HEM-74). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InputModeEventsTest {
    private fun key(
        code: Int,
        source: Int,
        flags: Int = 0,
    ) = KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, code, 0, 0, NO_DEVICE, 0, flags, source)

    private fun touch() =
        MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }

    @Test
    fun aGamepadButtonThenATouchSwitchInTurn() {
        val tracker = InputModeTracker()
        tracker.onKey(key(KeyEvent.KEYCODE_BUTTON_A, InputDevice.SOURCE_GAMEPAD))
        assertEquals(InputMode.Gamepad, tracker.mode.value)
        tracker.onMotion(touch())
        assertEquals(InputMode.Touch, tracker.mode.value)
    }

    @Test
    fun aSoftKeyboardArrowChangesNothing() {
        val tracker = InputModeTracker()
        val soft = KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
        tracker.onKey(key(KeyEvent.KEYCODE_DPAD_DOWN, InputDevice.SOURCE_KEYBOARD or InputDevice.SOURCE_DPAD, soft))
        assertEquals(InputMode.Touch, tracker.mode.value)
    }

    @Test
    fun aLetterChangesNothing() {
        val tracker = InputModeTracker()
        tracker.onKey(key(KeyEvent.KEYCODE_A, InputDevice.SOURCE_KEYBOARD))
        assertEquals(InputMode.Touch, tracker.mode.value)
    }

    /** Robolectric has no input devices: an arrow with no device falls back to today's D-pad rule. */
    @Test
    fun anArrowWithNoDeviceIsClassifiedAsBefore() {
        val tracker = InputModeTracker()
        tracker.onKey(key(KeyEvent.KEYCODE_DPAD_DOWN, InputDevice.SOURCE_KEYBOARD or InputDevice.SOURCE_DPAD))
        assertEquals(InputMode.Gamepad, tracker.mode.value)
    }

    private companion object {
        /** A device id Robolectric does not know, so `KeyEvent.device` is null. */
        const val NO_DEVICE = 99
    }
}
