package io.github.ctorres345.hemisphere.core.input

import android.view.KeyEvent
import io.github.ctorres345.hemisphere.core.designsystem.FaceButtonLayout
import kotlin.math.abs
import kotlin.math.max

/**
 * Turns raw gamepad key and motion events into [GamepadAction]s (docs/design-system.md §3).
 *
 * Pure logic over small value types: time comes from the events and from [tick], so tests drive it with
 * virtual time. Call [tick] regularly (every frame or every ~50 ms) while a gamepad is in use; it
 * produces stick repeats and the range-mark hold. Not thread-safe: use it from the main thread.
 *
 * | Xbox | Action | Nintendo |
 * |---|---|---|
 * | A | Confirm | B |
 * | B | Back | A |
 * | X | Menu | Y |
 * | Y | Mark (tap) / MarkRange (hold) | X |
 *
 * L1/R1 switch pane, L2/R2 switch tab, Start and Select map to themselves, the D-pad and left stick
 * navigate, the right stick pages.
 */
class GamepadMapper(
    var layout: FaceButtonLayout = FaceButtonLayout.Xbox,
    private val config: GamepadConfig = GamepadConfig(),
) {
    private class HeldAxis(
        var nextRepeatAt: Long,
        var repeating: Boolean,
    )

    private val lastEmitted = mutableMapOf<GamepadAction, Long>()
    private val heldKeys = mutableSetOf<GamepadAction>()
    private val heldAxes = mutableMapOf<GamepadAction, HeldAxis>()
    private var markDownAt: Long? = null
    private var markRangeSent = false
    private var leftTriggerDown = false
    private var rightTriggerDown = false
    private val heldTriggerKeys = mutableSetOf<GamepadAction>()

    /** Pads that report analog trigger axes: their L2/R2 keys are ignored, the axis counts. */
    private val padsWithTriggerAxes = mutableSetOf<Int>()
    private var leftStickVertical: Boolean? = null

    /** True while the Mark button is down; a screen can read it to treat D-pad moves as range extension. */
    val isMarkHeld: Boolean get() = markDownAt != null

    /** Maps one key event. Returns the actions it produces, usually none or one. */
    fun onKey(event: GamepadKeyEvent): List<GamepadAction> {
        val action = actionForKey(event.keyCode) ?: return emptyList()
        val out = mutableListOf<GamepadAction>()
        when {
            action == GamepadAction.Mark -> {
                onMarkKey(event, out)
            }

            action in TRIGGER_KEY_ACTIONS && event.keyCode in TRIGGER_KEYS -> {
                // Pads that also report trigger axes send both for one pull: the axis wins (design-system §3).
                // Only a key that fired holds the axis back, so a pad with axes is never left with neither.
                if (!event.isDown) {
                    heldTriggerKeys -= action
                } else if (event.repeatCount == 0 && event.deviceId !in padsWithTriggerAxes) {
                    heldTriggerKeys += action
                    emit(action, event.timeMs, out)
                }
            }

            action in DIRECTIONS -> {
                if (event.isDown) heldKeys += action else heldKeys -= action
                if (event.isDown) emit(action, event.timeMs, out)
            }

            event.isDown && event.repeatCount == 0 -> {
                emit(action, event.timeMs, out)
            }
        }
        return out
    }

    /** Maps one motion event (hat, sticks, triggers). Returns the actions it produces. */
    fun onMotion(event: GamepadMotionEvent): List<GamepadAction> {
        val out = mutableListOf<GamepadAction>()
        val magnitudes = magnitudes(event)
        for (action in AXIS_ACTIONS) {
            val value = magnitudes.getValue(action)
            val held = heldAxes[action]
            when {
                value < config.deadzone -> {
                    heldAxes.remove(action)
                }

                held == null -> {
                    heldAxes[action] = HeldAxis(event.timeMs + config.repeatDelayMs, value >= config.repeatThreshold)
                    if (action !in heldKeys) emit(action, event.timeMs, out)
                }

                else -> {
                    val repeating = value >= config.repeatThreshold
                    // Crossing into the repeat band starts the delay afresh, so a slow push never bursts.
                    if (repeating && !held.repeating) held.nextRepeatAt = event.timeMs + config.repeatDelayMs
                    held.repeating = repeating
                }
            }
        }
        if (event.leftTrigger > 0f || event.rightTrigger > 0f) padsWithTriggerAxes += event.deviceId
        leftTriggerDown = edge(event.leftTrigger, leftTriggerDown, GamepadAction.PrevTab, event.timeMs, out)
        rightTriggerDown = edge(event.rightTrigger, rightTriggerDown, GamepadAction.NextTab, event.timeMs, out)
        return out
    }

    /** Advances time: emits due stick repeats and the range mark when Ⓨ has been held long enough. */
    fun tick(nowMs: Long): List<GamepadAction> {
        val out = mutableListOf<GamepadAction>()
        for ((action, held) in heldAxes) {
            if (held.repeating && held.nextRepeatAt <= nowMs) {
                // One step per tick: a late tick never fires a burst of missed repeats.
                if (action !in heldKeys) emit(action, nowMs, out)
                held.nextRepeatAt = max(held.nextRepeatAt, nowMs) + config.repeatIntervalMs
            }
        }
        val down = markDownAt
        if (down != null && !markRangeSent && nowMs - down >= config.holdMs) {
            markRangeSent = true
            emit(GamepadAction.MarkRange, nowMs, out)
        }
        return out
    }

    /** Forgets everything held, for when the pad disconnects or the app loses focus. */
    fun reset() {
        heldKeys.clear()
        heldAxes.clear()
        markDownAt = null
        markRangeSent = false
        leftTriggerDown = false
        rightTriggerDown = false
        heldTriggerKeys.clear()
        padsWithTriggerAxes.clear()
        leftStickVertical = null
    }

    private fun onMarkKey(
        event: GamepadKeyEvent,
        out: MutableList<GamepadAction>,
    ) {
        if (event.isDown) {
            if (event.repeatCount == 0 && markDownAt == null) {
                markDownAt = event.timeMs
                markRangeSent = false
            }
            return
        }
        val down = markDownAt ?: return
        markDownAt = null
        if (!markRangeSent) {
            val held = event.timeMs - down >= config.holdMs
            emit(if (held) GamepadAction.MarkRange else GamepadAction.Mark, event.timeMs, out)
        }
    }

    private fun edge(
        value: Float,
        wasDown: Boolean,
        action: GamepadAction,
        timeMs: Long,
        out: MutableList<GamepadAction>,
    ): Boolean {
        // Hysteresis: a slow pull that jitters around the press point fires once.
        val down = if (wasDown) value >= TRIGGER_RELEASE else value >= TRIGGER_PRESS
        if (down && !wasDown && action !in heldTriggerKeys) emit(action, timeMs, out)
        return down
    }

    /** Adds [action] unless the same action fired within the dedupe window (one press seen twice). */
    private fun emit(
        action: GamepadAction,
        timeMs: Long,
        out: MutableList<GamepadAction>,
    ) {
        val last = lastEmitted[action]
        if (last != null && timeMs - last in 0 until config.dedupeWindowMs) return
        lastEmitted[action] = timeMs
        out += action
    }

    private fun magnitudes(event: GamepadMotionEvent): Map<GamepadAction, Float> {
        val vertical = leftStickAxis(abs(event.leftX), abs(event.leftY))
        val leftX = if (vertical) 0f else event.leftX
        val leftY = if (vertical) event.leftY else 0f
        return mapOf(
            GamepadAction.Up to max(-event.hatY, -leftY),
            GamepadAction.Down to max(event.hatY, leftY),
            GamepadAction.Left to max(-event.hatX, -leftX),
            GamepadAction.Right to max(event.hatX, leftX),
            GamepadAction.PageUp to -event.rightY,
            GamepadAction.PageDown to event.rightY,
        ).mapValues { max(0f, it.value) }
    }

    /**
     * The left stick moves along its dominant axis only, so a slightly diagonal push is one direction.
     * The held axis keeps going until the other beats it by [AXIS_SWITCH_RATIO], so a stick near 45°
     * does not flip between two directions.
     */
    private fun leftStickAxis(
        x: Float,
        y: Float,
    ): Boolean {
        val current = leftStickVertical
        val vertical =
            when {
                x < config.deadzone && y < config.deadzone -> null
                current == true -> x <= y * AXIS_SWITCH_RATIO
                current == false -> y > x * AXIS_SWITCH_RATIO
                else -> y >= x
            }
        leftStickVertical = vertical
        return vertical ?: (y >= x)
    }

    private fun actionForKey(keyCode: Int): GamepadAction? =
        if (layout == FaceButtonLayout.Nintendo) {
            NINTENDO_FACE_KEYS[keyCode] ?: COMMON_KEYS[keyCode]
        } else {
            XBOX_FACE_KEYS[keyCode] ?: COMMON_KEYS[keyCode]
        }

    private companion object {
        const val TRIGGER_PRESS = 0.5f
        const val TRIGGER_RELEASE = 0.3f
        const val AXIS_SWITCH_RATIO = 1.2f
        val TRIGGER_KEYS = setOf(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2)
        val TRIGGER_KEY_ACTIONS = setOf(GamepadAction.PrevTab, GamepadAction.NextTab)

        val XBOX_FACE_KEYS =
            mapOf(
                KeyEvent.KEYCODE_BUTTON_A to GamepadAction.Confirm,
                KeyEvent.KEYCODE_BUTTON_B to GamepadAction.Back,
                KeyEvent.KEYCODE_BUTTON_X to GamepadAction.Menu,
                KeyEvent.KEYCODE_BUTTON_Y to GamepadAction.Mark,
            )

        /** Nintendo swaps A with B and X with Y (design-system §3). */
        val NINTENDO_FACE_KEYS =
            mapOf(
                KeyEvent.KEYCODE_BUTTON_A to GamepadAction.Back,
                KeyEvent.KEYCODE_BUTTON_B to GamepadAction.Confirm,
                KeyEvent.KEYCODE_BUTTON_X to GamepadAction.Mark,
                KeyEvent.KEYCODE_BUTTON_Y to GamepadAction.Menu,
            )

        val COMMON_KEYS =
            mapOf(
                KeyEvent.KEYCODE_DPAD_CENTER to GamepadAction.Confirm,
                KeyEvent.KEYCODE_BUTTON_L1 to GamepadAction.PrevPane,
                KeyEvent.KEYCODE_BUTTON_R1 to GamepadAction.NextPane,
                KeyEvent.KEYCODE_BUTTON_L2 to GamepadAction.PrevTab,
                KeyEvent.KEYCODE_BUTTON_R2 to GamepadAction.NextTab,
                KeyEvent.KEYCODE_BUTTON_START to GamepadAction.Start,
                KeyEvent.KEYCODE_BUTTON_SELECT to GamepadAction.Select,
                KeyEvent.KEYCODE_DPAD_UP to GamepadAction.Up,
                KeyEvent.KEYCODE_DPAD_DOWN to GamepadAction.Down,
                KeyEvent.KEYCODE_DPAD_LEFT to GamepadAction.Left,
                KeyEvent.KEYCODE_DPAD_RIGHT to GamepadAction.Right,
            )

        val DIRECTIONS = setOf(GamepadAction.Up, GamepadAction.Down, GamepadAction.Left, GamepadAction.Right)
        val AXIS_ACTIONS = DIRECTIONS + setOf(GamepadAction.PageUp, GamepadAction.PageDown)
    }
}
