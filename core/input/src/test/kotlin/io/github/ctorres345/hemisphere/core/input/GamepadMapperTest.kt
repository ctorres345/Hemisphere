package io.github.ctorres345.hemisphere.core.input

import android.view.KeyEvent
import io.github.ctorres345.hemisphere.core.designsystem.FaceButtonLayout
import io.github.ctorres345.hemisphere.core.input.GamepadAction.Back
import io.github.ctorres345.hemisphere.core.input.GamepadAction.Confirm
import io.github.ctorres345.hemisphere.core.input.GamepadAction.Down
import io.github.ctorres345.hemisphere.core.input.GamepadAction.Left
import io.github.ctorres345.hemisphere.core.input.GamepadAction.Mark
import io.github.ctorres345.hemisphere.core.input.GamepadAction.MarkRange
import io.github.ctorres345.hemisphere.core.input.GamepadAction.Menu
import io.github.ctorres345.hemisphere.core.input.GamepadAction.NextPane
import io.github.ctorres345.hemisphere.core.input.GamepadAction.NextTab
import io.github.ctorres345.hemisphere.core.input.GamepadAction.PageDown
import io.github.ctorres345.hemisphere.core.input.GamepadAction.PageUp
import io.github.ctorres345.hemisphere.core.input.GamepadAction.PrevPane
import io.github.ctorres345.hemisphere.core.input.GamepadAction.PrevTab
import io.github.ctorres345.hemisphere.core.input.GamepadAction.Right
import io.github.ctorres345.hemisphere.core.input.GamepadAction.Select
import io.github.ctorres345.hemisphere.core.input.GamepadAction.Start
import io.github.ctorres345.hemisphere.core.input.GamepadAction.Up
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamepadMapperTest {
    private val xbox = GamepadMapper(FaceButtonLayout.Xbox)

    private fun GamepadMapper.press(
        keyCode: Int,
        at: Long = 0,
    ) = onKey(GamepadKeyEvent(keyCode, isDown = true, timeMs = at))

    private fun GamepadMapper.release(
        keyCode: Int,
        at: Long,
    ) = onKey(GamepadKeyEvent(keyCode, isDown = false, timeMs = at))

    @Test
    fun xboxKeysMapToActions() {
        val expected =
            mapOf(
                KeyEvent.KEYCODE_BUTTON_A to Confirm,
                KeyEvent.KEYCODE_BUTTON_B to Back,
                KeyEvent.KEYCODE_BUTTON_X to Menu,
                KeyEvent.KEYCODE_DPAD_CENTER to Confirm,
                KeyEvent.KEYCODE_BUTTON_L1 to PrevPane,
                KeyEvent.KEYCODE_BUTTON_R1 to NextPane,
                KeyEvent.KEYCODE_BUTTON_L2 to PrevTab,
                KeyEvent.KEYCODE_BUTTON_R2 to NextTab,
                KeyEvent.KEYCODE_BUTTON_START to Start,
                KeyEvent.KEYCODE_BUTTON_SELECT to Select,
                KeyEvent.KEYCODE_DPAD_UP to Up,
                KeyEvent.KEYCODE_DPAD_DOWN to Down,
                KeyEvent.KEYCODE_DPAD_LEFT to Left,
                KeyEvent.KEYCODE_DPAD_RIGHT to Right,
            )
        var t = 0L
        for ((key, action) in expected) {
            t += 1000
            assertEquals("key $key", listOf(action), xbox.press(key, t))
            xbox.release(key, t + 10)
        }
    }

    @Test
    fun nintendoSwapsConfirmBackAndMenuMark() {
        val pad = GamepadMapper(FaceButtonLayout.Nintendo)
        assertEquals(listOf(Back), pad.press(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(listOf(Confirm), pad.press(KeyEvent.KEYCODE_BUTTON_B))
        assertEquals(listOf(Mark), tap(pad, KeyEvent.KEYCODE_BUTTON_X))
        assertEquals(listOf(Menu), pad.press(KeyEvent.KEYCODE_BUTTON_Y))
    }

    private fun tap(
        pad: GamepadMapper,
        keyCode: Int,
    ) = pad.press(keyCode, 5000) + pad.release(keyCode, 5010)

    @Test
    fun unknownKeysAndKeyRepeatsOfButtonsDoNothing() {
        assertTrue(xbox.press(KeyEvent.KEYCODE_BUTTON_MODE).isEmpty())
        val repeat = GamepadKeyEvent(KeyEvent.KEYCODE_BUTTON_A, isDown = true, timeMs = 500, repeatCount = 2)
        assertTrue(xbox.onKey(repeat).isEmpty())
    }

    @Test
    fun tapOnMarkEmitsMarkOnRelease() {
        assertTrue(xbox.press(KeyEvent.KEYCODE_BUTTON_Y, 0).isEmpty())
        assertTrue(xbox.isMarkHeld)
        assertEquals(listOf(Mark), xbox.release(KeyEvent.KEYCODE_BUTTON_Y, 100))
        assertFalse(xbox.isMarkHeld)
    }

    @Test
    fun holdOnMarkEmitsMarkRangeOnceAndNoMarkOnRelease() {
        xbox.press(KeyEvent.KEYCODE_BUTTON_Y, 0)
        assertTrue(xbox.tick(399).isEmpty())
        assertEquals(listOf(MarkRange), xbox.tick(400))
        assertTrue(xbox.tick(900).isEmpty())
        assertTrue(xbox.release(KeyEvent.KEYCODE_BUTTON_Y, 1000).isEmpty())
    }

    @Test
    fun holdReleasedWithoutATickStillCountsAsRange() {
        xbox.press(KeyEvent.KEYCODE_BUTTON_Y, 0)
        assertEquals(listOf(MarkRange), xbox.release(KeyEvent.KEYCODE_BUTTON_Y, 450))
    }

    @Test
    fun hatMapsToDirections() {
        assertEquals(listOf(Up), xbox.onMotion(GamepadMotionEvent(0, hatY = -1f)))
        assertEquals(listOf(Left), xbox.onMotion(GamepadMotionEvent(1000, hatX = -1f)))
    }

    @Test
    fun rightStickPages() {
        assertEquals(listOf(PageUp), xbox.onMotion(GamepadMotionEvent(0, rightY = -0.9f)))
        xbox.onMotion(GamepadMotionEvent(10))
        assertEquals(listOf(PageDown), xbox.onMotion(GamepadMotionEvent(2000, rightY = 0.9f)))
    }

    @Test
    fun triggersEmitOncePerPress() {
        assertEquals(listOf(PrevTab), xbox.onMotion(GamepadMotionEvent(0, leftTrigger = 0.8f)))
        assertTrue(xbox.onMotion(GamepadMotionEvent(50, leftTrigger = 1f)).isEmpty())
        xbox.onMotion(GamepadMotionEvent(100))
        assertEquals(listOf(PrevTab), xbox.onMotion(GamepadMotionEvent(2000, leftTrigger = 0.8f)))
    }

    @Test
    fun stickInsideTheDeadzoneDoesNothing() {
        assertTrue(xbox.onMotion(GamepadMotionEvent(0, leftX = 0.2f, leftY = -0.2f)).isEmpty())
        assertTrue(xbox.tick(10_000).isEmpty())
    }

    @Test
    fun stickPastTheThresholdRepeatsAtASteadyRate() {
        assertEquals(listOf(Right), xbox.onMotion(GamepadMotionEvent(0, leftX = 0.9f)))
        assertTrue(xbox.tick(399).isEmpty())
        assertEquals(listOf(Right), xbox.tick(400))
        assertTrue(xbox.tick(499).isEmpty())
        assertEquals(listOf(Right), xbox.tick(500))
        assertEquals(listOf(Right), xbox.tick(600))
        // A late tick steps once, never a burst of missed repeats.
        assertEquals(listOf(Right), xbox.tick(900))
        assertTrue(xbox.tick(916).isEmpty())
        assertTrue(xbox.tick(999).isEmpty())
        assertEquals(listOf(Right), xbox.tick(1000))
    }

    @Test
    fun pushingPastTheThresholdLaterStartsTheDelayAfresh() {
        assertEquals(listOf(Right), xbox.onMotion(GamepadMotionEvent(0, leftX = 0.4f)))
        assertTrue(xbox.onMotion(GamepadMotionEvent(2000, leftX = 0.9f)).isEmpty())
        assertTrue(xbox.tick(2000).isEmpty())
        assertTrue(xbox.tick(2399).isEmpty())
        assertEquals(listOf(Right), xbox.tick(2400))
    }

    @Test
    fun aSlightlyDiagonalStickMovesOneWay() {
        assertEquals(listOf(Down), xbox.onMotion(GamepadMotionEvent(0, leftX = 0.3f, leftY = 0.9f)))
        assertEquals(listOf(Down), xbox.tick(400))
    }

    @Test
    fun oneTriggerPullSendingKeyAndAxisMovesOneTab() {
        assertEquals(listOf(PrevTab), xbox.press(KeyEvent.KEYCODE_BUTTON_L2, 0))
        assertTrue(xbox.onMotion(GamepadMotionEvent(80, leftTrigger = 0.9f)).isEmpty())
        xbox.release(KeyEvent.KEYCODE_BUTTON_L2, 200)
        xbox.onMotion(GamepadMotionEvent(200))
        // The pad has trigger axes: from now on the axis alone counts.
        assertEquals(listOf(PrevTab), xbox.onMotion(GamepadMotionEvent(1000, leftTrigger = 0.9f)))
        assertTrue(xbox.press(KeyEvent.KEYCODE_BUTTON_L2, 1080).isEmpty())
    }

    @Test
    fun everyPullCountsWhenTheKeyComesBeforeTheAxis() {
        for (pull in 0..2) {
            val t = pull * 1000L
            assertEquals(
                listOf(PrevTab),
                xbox.press(KeyEvent.KEYCODE_BUTTON_L2, t) +
                    xbox.onMotion(GamepadMotionEvent(t + 80, leftTrigger = 0.9f)),
            )
            xbox.release(KeyEvent.KEYCODE_BUTTON_L2, t + 200)
            xbox.onMotion(GamepadMotionEvent(t + 200))
        }
    }

    @Test
    fun aSecondPadWithDigitalTriggersStillSwitchesTabs() {
        xbox.onMotion(GamepadMotionEvent(0, deviceId = 1, leftTrigger = 0.9f))
        xbox.onMotion(GamepadMotionEvent(100, deviceId = 1))
        assertEquals(
            listOf(PrevTab),
            xbox.onKey(GamepadKeyEvent(KeyEvent.KEYCODE_BUTTON_L2, isDown = true, timeMs = 1000, deviceId = 2)),
        )
        assertTrue(
            xbox
                .onKey(
                    GamepadKeyEvent(KeyEvent.KEYCODE_BUTTON_L2, isDown = true, timeMs = 2000, deviceId = 1),
                ).isEmpty(),
        )
    }

    @Test
    fun digitalTriggerKeysWorkAgainAfterAReset() {
        xbox.onMotion(GamepadMotionEvent(0, leftTrigger = 0.9f))
        xbox.onMotion(GamepadMotionEvent(100))
        xbox.reset()
        assertEquals(listOf(PrevTab), xbox.press(KeyEvent.KEYCODE_BUTTON_L2, 1000))
    }

    @Test
    fun aStickNear45DegreesKeepsOneDirection() {
        assertEquals(listOf(Down), xbox.onMotion(GamepadMotionEvent(0, leftX = 0.70f, leftY = 0.72f)))
        assertTrue(xbox.onMotion(GamepadMotionEvent(10, leftX = 0.72f, leftY = 0.70f)).isEmpty())
        assertTrue(xbox.onMotion(GamepadMotionEvent(20, leftX = 0.70f, leftY = 0.72f)).isEmpty())
        assertEquals(listOf(Right), xbox.onMotion(GamepadMotionEvent(30, leftX = 0.95f, leftY = 0.2f)))
    }

    @Test
    fun aTriggerJitteringAroundThePressPointFiresOnce() {
        assertEquals(listOf(NextTab), xbox.onMotion(GamepadMotionEvent(0, rightTrigger = 0.52f)))
        assertTrue(xbox.onMotion(GamepadMotionEvent(40, rightTrigger = 0.48f)).isEmpty())
        assertTrue(xbox.onMotion(GamepadMotionEvent(80, rightTrigger = 0.51f)).isEmpty())
    }

    @Test
    fun stickBetweenDeadzoneAndThresholdStepsOnce() {
        assertEquals(listOf(Down), xbox.onMotion(GamepadMotionEvent(0, leftY = 0.4f)))
        assertTrue(xbox.tick(5_000).isEmpty())
    }

    @Test
    fun repeatStopsWhenTheStickReturnsToCenter() {
        xbox.onMotion(GamepadMotionEvent(0, leftX = 0.9f))
        xbox.onMotion(GamepadMotionEvent(200, leftX = 0f))
        assertTrue(xbox.tick(5_000).isEmpty())
    }

    @Test
    fun dpadKeyAndHatForOnePressAreMergedWithinOneFrame() {
        val keyThenHat = GamepadMapper()
        assertEquals(listOf(Down), keyThenHat.press(KeyEvent.KEYCODE_DPAD_DOWN, 1000))
        assertTrue(keyThenHat.onMotion(GamepadMotionEvent(1008, hatY = 1f)).isEmpty())

        val hatThenKey = GamepadMapper()
        assertEquals(listOf(Down), hatThenKey.onMotion(GamepadMotionEvent(1000, hatY = 1f)))
        assertTrue(hatThenKey.press(KeyEvent.KEYCODE_DPAD_DOWN, 1015).isEmpty())
    }

    @Test
    fun sameDirectionPressedAgainAfterTheWindowCountsAgain() {
        assertEquals(listOf(Down), xbox.press(KeyEvent.KEYCODE_DPAD_DOWN, 1000))
        xbox.release(KeyEvent.KEYCODE_DPAD_DOWN, 1050)
        assertEquals(listOf(Down), xbox.press(KeyEvent.KEYCODE_DPAD_DOWN, 1100))
    }

    @Test
    fun heldHatDoesNotDoubleRepeatWhileTheDpadKeyIsHeld() {
        xbox.press(KeyEvent.KEYCODE_DPAD_DOWN, 0)
        xbox.onMotion(GamepadMotionEvent(5, hatY = 1f))
        assertTrue(xbox.tick(1_000).isEmpty())
    }
}
