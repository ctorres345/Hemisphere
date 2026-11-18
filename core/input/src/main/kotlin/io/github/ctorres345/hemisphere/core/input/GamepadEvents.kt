package io.github.ctorres345.hemisphere.core.input

/**
 * A key press or release from a gamepad, stripped of `android.view.KeyEvent` so the mapping runs on the
 * JVM. [timeMs] is the event time on any monotonic millisecond clock (Android's `eventTime`).
 * [deviceId] tells two connected pads apart.
 */
data class GamepadKeyEvent(
    val keyCode: Int,
    val isDown: Boolean,
    val timeMs: Long,
    val repeatCount: Int = 0,
    val deviceId: Int = 0,
)

/**
 * The axes of one `MotionEvent` that the app reads. Stick Y values follow Android: negative is up.
 * Triggers run from 0 (released) to 1 (fully pressed); the adapter merges `AXIS_LTRIGGER`/`AXIS_BRAKE`.
 */
data class GamepadMotionEvent(
    val timeMs: Long,
    val deviceId: Int = 0,
    val hatX: Float = 0f,
    val hatY: Float = 0f,
    val leftX: Float = 0f,
    val leftY: Float = 0f,
    val rightY: Float = 0f,
    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f,
)

/**
 * The numbers the mapper uses. Times are in milliseconds, axis values in the range 0 to 1.
 *
 * @property deadzone A stick or trigger below this does nothing.
 * @property repeatThreshold A stick at or past this repeats while held; between the deadzone and this
 *   it steps once.
 * @property repeatDelayMs How long a held direction waits before it starts repeating.
 * @property repeatIntervalMs The steady gap between repeats.
 * @property holdMs How long Ⓨ is held before it counts as a range mark instead of a tap.
 * @property dedupeWindowMs Equal actions closer than this are one press (a pad that sends both a D-pad
 *   key and a hat axis for one press).
 */
data class GamepadConfig(
    val deadzone: Float = 0.25f,
    val repeatThreshold: Float = 0.6f,
    val repeatDelayMs: Long = 400,
    val repeatIntervalMs: Long = 100,
    val holdMs: Long = 400,
    val dedupeWindowMs: Long = 16,
)

/** True when any axis is past [deadzone]: the user moved something, not just stick drift. */
fun GamepadMotionEvent.isActive(deadzone: Float): Boolean =
    listOf(hatX, hatY, leftX, leftY, rightY, leftTrigger, rightTrigger).any { kotlin.math.abs(it) >= deadzone }
