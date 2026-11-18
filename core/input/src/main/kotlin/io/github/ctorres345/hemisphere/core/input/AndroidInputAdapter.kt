package io.github.ctorres345.hemisphere.core.input

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import io.github.ctorres345.hemisphere.core.designsystem.InputMode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.max

/** The gamepad view of this key event, for [GamepadMapper.onKey]. */
fun KeyEvent.toGamepadKeyEvent(): GamepadKeyEvent =
    GamepadKeyEvent(keyCode, action == KeyEvent.ACTION_DOWN, eventTime, repeatCount, deviceId)

/** The gamepad view of this motion event, for [GamepadMapper.onMotion]. */
fun MotionEvent.toGamepadMotionEvent(): GamepadMotionEvent =
    GamepadMotionEvent(
        timeMs = eventTime,
        deviceId = deviceId,
        hatX = getAxisValue(MotionEvent.AXIS_HAT_X),
        hatY = getAxisValue(MotionEvent.AXIS_HAT_Y),
        leftX = getAxisValue(MotionEvent.AXIS_X),
        leftY = getAxisValue(MotionEvent.AXIS_Y),
        rightY = getAxisValue(MotionEvent.AXIS_RZ),
        leftTrigger = max(getAxisValue(MotionEvent.AXIS_LTRIGGER), getAxisValue(MotionEvent.AXIS_BRAKE)),
        rightTrigger = max(getAxisValue(MotionEvent.AXIS_RTRIGGER), getAxisValue(MotionEvent.AXIS_GAS)),
    )

/**
 * True when [source] (an `InputDevice` source bit set) is a gamepad, D-pad or joystick. Each source is
 * matched in full: they share class bits with the keyboard, so a single-bit test would count volume
 * keys and hardware keyboards as gamepads.
 */
fun isGamepadSource(source: Int): Boolean =
    source.has(InputDevice.SOURCE_GAMEPAD) || source.has(InputDevice.SOURCE_DPAD) ||
        source.has(InputDevice.SOURCE_JOYSTICK)

/** True when [source] is a touch screen, mouse or stylus: anything that points. */
fun isPointerSource(source: Int): Boolean = source and InputDevice.SOURCE_CLASS_POINTER != 0

private fun Int.has(source: Int): Boolean = this and source == source

/**
 * The [InputMode] this key event signals, or null (ignore). See [keyInputMode]; the device's own sources
 * and keyboard type tell a hardware keyboard from a gamepad. A soft keyboard's key events signal nothing.
 */
fun KeyEvent.inputMode(): InputMode? {
    // Most keys (letters typed in a text field) signal nothing: skip the device lookup for them. A soft
    // keyboard's keys (Gboard's cursor arrows, Enter) come from a touch user, so they signal nothing too.
    val soft = flags and KeyEvent.FLAG_SOFT_KEYBOARD != 0
    val buttonOrNavigation = keyCode in NAVIGATION_KEYS || keyCode in BUTTON_KEYS || keyCode in NUMBERED_BUTTON_KEYS
    if (soft || (!buttonOrNavigation && !isGamepadKey(source, keyCode))) return null
    val device = device
    return keyInputMode(
        source = source,
        keyCode = keyCode,
        deviceSources = device?.sources ?: source,
        keyboardType = device?.keyboardType ?: InputDevice.KEYBOARD_TYPE_NONE,
    )
}

/**
 * The [InputMode] a key signals, or null (ignore). A device with an alphabetic [keyboardType] and no
 * gamepad or joystick in its [deviceSources] is a hardware keyboard: its navigation keys (arrows, Tab,
 * Enter, Escape) signal Keyboard, and its other keys, such as letters typed in a text field, nothing.
 * Every other device signals Gamepad for a gamepad key ([isGamepadKey]), so a D-pad stays a gamepad.
 */
fun keyInputMode(
    source: Int,
    keyCode: Int,
    deviceSources: Int,
    keyboardType: Int,
): InputMode? {
    // Not isGamepadSource: keyboards report SOURCE_DPAD too, for their arrow keys.
    val keyboard =
        keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC &&
            !deviceSources.has(InputDevice.SOURCE_GAMEPAD) &&
            !deviceSources.has(InputDevice.SOURCE_JOYSTICK)
    return when {
        // A real keyboard never sends gamepad buttons: a pad that reports as a keyboard keeps Gamepad.
        keyboard && (keyCode in BUTTON_KEYS || keyCode in NUMBERED_BUTTON_KEYS) -> InputMode.Gamepad

        keyboard -> if (keyCode in NAVIGATION_KEYS) InputMode.Keyboard else null

        isGamepadKey(source, keyCode) -> InputMode.Gamepad

        else -> null
    }
}

/** A hardware keyboard's keys that move or act on focus; its arrows arrive as D-pad key codes. */
private val NAVIGATION_KEYS =
    setOf(
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_TAB,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_ESCAPE,
    )

/**
 * True when a key event with this [source] and [keyCode] came from a gamepad. Android also tags hardware
 * keyboards that have arrow keys as D-pads, so a D-pad-only source counts just for D-pad and gamepad keys.
 */
fun isGamepadKey(
    source: Int,
    keyCode: Int,
): Boolean =
    source.has(InputDevice.SOURCE_GAMEPAD) ||
        source.has(InputDevice.SOURCE_JOYSTICK) ||
        (
            source.has(InputDevice.SOURCE_DPAD) &&
                (keyCode in DPAD_KEYS || keyCode in BUTTON_KEYS || keyCode in NUMBERED_BUTTON_KEYS)
        )

private val DPAD_KEYS = KeyEvent.KEYCODE_DPAD_UP..KeyEvent.KEYCODE_DPAD_CENTER
private val BUTTON_KEYS = KeyEvent.KEYCODE_BUTTON_A..KeyEvent.KEYCODE_BUTTON_MODE
private val NUMBERED_BUTTON_KEYS = KeyEvent.KEYCODE_BUTTON_1..KeyEvent.KEYCODE_BUTTON_16

/**
 * The [InputMode] this motion event signals: Touch from anything that points, Gamepad from a joystick
 * only when an axis is past [deadzone], so stick drift never brings the focus rings back.
 */
fun MotionEvent.inputMode(deadzone: Float = GamepadConfig().deadzone): InputMode? =
    when {
        isPointerSource(source) -> InputMode.Touch
        isGamepadSource(source) && toGamepadMotionEvent().isActive(deadzone) -> InputMode.Gamepad
        else -> null
    }

/**
 * The vendor id of the first connected gamepad, or null when none is connected. Emits again as devices
 * come and go. Feeds [FaceButtonLayoutResolver].
 */
fun connectedGamepadVendorId(context: Context): Flow<Int?> =
    callbackFlow {
        val manager = context.getSystemService(Context.INPUT_SERVICE) as InputManager

        fun current(): Int? =
            InputDevice
                .getDeviceIds()
                .asList()
                .mapNotNull { InputDevice.getDevice(it) }
                .firstOrNull {
                    !it.isVirtual &&
                        (it.sources.has(InputDevice.SOURCE_GAMEPAD) || it.sources.has(InputDevice.SOURCE_JOYSTICK))
                }?.vendorId

        val listener =
            object : InputManager.InputDeviceListener {
                override fun onInputDeviceAdded(deviceId: Int) {
                    trySend(current())
                }

                override fun onInputDeviceRemoved(deviceId: Int) {
                    trySend(current())
                }

                override fun onInputDeviceChanged(deviceId: Int) {
                    trySend(current())
                }
            }
        manager.registerInputDeviceListener(listener, Handler(Looper.getMainLooper()))
        trySend(current())
        awaitClose { manager.unregisterInputDeviceListener(listener) }
    }.distinctUntilChanged()
