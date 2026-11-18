package io.github.ctorres345.hemisphere.core.input

import android.view.InputDevice
import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidInputAdapterTest {
    @Test
    fun gamepadDpadAndJoystickSourcesAreGamepads() {
        assertTrue(isGamepadSource(InputDevice.SOURCE_GAMEPAD))
        assertTrue(isGamepadSource(InputDevice.SOURCE_DPAD))
        assertTrue(isGamepadSource(InputDevice.SOURCE_JOYSTICK))
        assertTrue(isGamepadSource(InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_KEYBOARD))
    }

    @Test
    fun keyboardsAndScreensAreNotGamepads() {
        assertFalse(isGamepadSource(InputDevice.SOURCE_KEYBOARD))
        assertFalse(isGamepadSource(InputDevice.SOURCE_TOUCHSCREEN))
        assertFalse(isGamepadSource(InputDevice.SOURCE_MOUSE))
    }

    @Test
    fun aKeyboardWithArrowKeysIsAGamepadOnlyForArrows() {
        val keyboard = InputDevice.SOURCE_KEYBOARD or InputDevice.SOURCE_DPAD
        assertFalse(isGamepadKey(keyboard, KeyEvent.KEYCODE_A))
        assertFalse(isGamepadKey(InputDevice.SOURCE_KEYBOARD, KeyEvent.KEYCODE_VOLUME_UP))
        assertTrue(isGamepadKey(keyboard, KeyEvent.KEYCODE_DPAD_DOWN))
        assertTrue(isGamepadKey(InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_KEYBOARD, KeyEvent.KEYCODE_BUTTON_A))
    }

    @Test
    fun everythingThatPointsCountsAsTouch() {
        assertTrue(isPointerSource(InputDevice.SOURCE_TOUCHSCREEN))
        assertTrue(isPointerSource(InputDevice.SOURCE_MOUSE))
        assertTrue(isPointerSource(InputDevice.SOURCE_STYLUS))
        assertFalse(isPointerSource(InputDevice.SOURCE_JOYSTICK))
        assertFalse(isPointerSource(InputDevice.SOURCE_KEYBOARD))
    }

    @Test
    fun stickDriftIsNotActivity() {
        assertFalse(GamepadMotionEvent(0, leftX = 0.1f, rightY = -0.05f).isActive(0.25f))
        assertTrue(GamepadMotionEvent(0, leftX = 0.6f).isActive(0.25f))
    }
}
