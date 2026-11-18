package io.github.ctorres345.hemisphere.core.input

import android.view.InputDevice
import android.view.KeyEvent
import io.github.ctorres345.hemisphere.core.designsystem.InputMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun anAlphabeticKeyboardsNavigationKeysAreKeyboard() {
        val sources = InputDevice.SOURCE_KEYBOARD or InputDevice.SOURCE_DPAD
        listOf(
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_TAB,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_ESCAPE,
        ).forEach { key ->
            assertEquals(InputMode.Keyboard, keyInputMode(sources, key, sources, InputDevice.KEYBOARD_TYPE_ALPHABETIC))
        }
    }

    @Test
    fun lettersAndOtherKeysFromAKeyboardChangeNothing() {
        val sources = InputDevice.SOURCE_KEYBOARD or InputDevice.SOURCE_DPAD
        val alphabetic = InputDevice.KEYBOARD_TYPE_ALPHABETIC
        assertNull(keyInputMode(sources, KeyEvent.KEYCODE_A, sources, alphabetic))
        assertNull(keyInputMode(sources, KeyEvent.KEYCODE_SPACE, sources, alphabetic))
        assertNull(
            keyInputMode(
                InputDevice.SOURCE_KEYBOARD,
                KeyEvent.KEYCODE_VOLUME_UP,
                InputDevice.SOURCE_KEYBOARD,
                InputDevice.KEYBOARD_TYPE_NON_ALPHABETIC,
            ),
        )
    }

    @Test
    fun theSameArrowFromAGamepadOrADpadDeviceIsGamepad() {
        val gamepad = InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_KEYBOARD
        val dpad = InputDevice.SOURCE_DPAD or InputDevice.SOURCE_KEYBOARD
        val down = KeyEvent.KEYCODE_DPAD_DOWN
        val none = InputDevice.KEYBOARD_TYPE_NONE
        val alphabetic = InputDevice.KEYBOARD_TYPE_ALPHABETIC
        assertEquals(InputMode.Gamepad, keyInputMode(gamepad, down, gamepad, none))
        assertEquals(InputMode.Gamepad, keyInputMode(dpad, down, dpad, InputDevice.KEYBOARD_TYPE_NON_ALPHABETIC))
        // A controller that also reports an alphabetic layout is still a gamepad.
        assertEquals(InputMode.Gamepad, keyInputMode(gamepad, down, gamepad, alphabetic))
        val joystick = InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_DPAD
        assertEquals(InputMode.Gamepad, keyInputMode(InputDevice.SOURCE_DPAD, down, joystick, alphabetic))
        assertEquals(InputMode.Gamepad, keyInputMode(gamepad, KeyEvent.KEYCODE_BUTTON_A, gamepad, none))
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

    @Test
    fun aPadThatReportsAsAKeyboardKeepsGamepadForItsButtons() {
        val pad = InputDevice.SOURCE_KEYBOARD or InputDevice.SOURCE_DPAD
        val alphabetic = InputDevice.KEYBOARD_TYPE_ALPHABETIC
        assertEquals(InputMode.Gamepad, keyInputMode(pad, KeyEvent.KEYCODE_BUTTON_A, pad, alphabetic))
        assertEquals(InputMode.Gamepad, keyInputMode(pad, KeyEvent.KEYCODE_BUTTON_1, pad, alphabetic))
    }
}
