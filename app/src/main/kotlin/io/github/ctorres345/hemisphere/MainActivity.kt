package io.github.ctorres345.hemisphere

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.InputMode
import io.github.ctorres345.hemisphere.core.designsystem.InputReporter
import io.github.ctorres345.hemisphere.core.designsystem.LocalInputMode
import io.github.ctorres345.hemisphere.core.designsystem.LocalInputReporter
import io.github.ctorres345.hemisphere.core.input.InputModeTracker
import io.github.ctorres345.hemisphere.core.input.inputMode

/**
 * The only Activity. Hosts the app back stack ([HemisphereNavHost]) and tracks the last input used
 * (touch, gamepad or keyboard), which it provides to the UI as [LocalInputMode].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var inputModes: InputModeTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Attaching a keyboard recreates the Activity: keep the mode, so the ring does not blink off.
        val saved = savedInstanceState?.getString(INPUT_MODE_KEY)
        inputModes = InputModeTracker(InputMode.entries.firstOrNull { it.name == saved } ?: InputMode.Touch)
        setContent {
            val mode by inputModes.mode.collectAsStateWithLifecycle()
            HemisphereTheme {
                CompositionLocalProvider(LocalInputMode provides mode, LocalInputReporter provides popupInput) {
                    HemisphereNavHost()
                }
            }
        }
    }

    /** Input from popup windows (menus), which skips this Activity's dispatch methods. */
    private val popupInput =
        object : InputReporter {
            override fun onKey(event: KeyEvent) = track(event.inputMode())

            override fun onPointer() = track(InputMode.Touch)
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(INPUT_MODE_KEY, inputModes.mode.value.name)
    }

    // Activity.dispatchKeyEvent is public platform API; androidx.core's ComponentActivity only marks its
    // own override restricted. The event goes on to super unchanged.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        track(event.inputMode())
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        track(event.inputMode())
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        track(event.inputMode())
        return super.dispatchGenericMotionEvent(event)
    }

    /** Every event reaches the UI unchanged; a mode-bearing one also updates the tracked mode. */
    private fun track(mode: InputMode?) {
        mode?.let(inputModes::onInput)
    }

    private companion object {
        const val INPUT_MODE_KEY = "input_mode"
    }
}
