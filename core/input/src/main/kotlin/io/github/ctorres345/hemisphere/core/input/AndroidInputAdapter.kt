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

/** The [InputMode] this key event signals: Gamepad from a gamepad key, else null (ignore). */
fun KeyEvent.inputMode(): InputMode? = if (isGamepadKey(source, keyCode)) InputMode.Gamepad else null

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
