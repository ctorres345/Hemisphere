package io.github.ctorres345.hemisphere.core.designsystem

import androidx.compose.runtime.CompositionLocalProvider
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The button-hint footer in gamepad mode, for both face-button layouts, on a phone and a handheld, in each theme. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Robolectric needs Java 21 for API 35 and up; the build runs on JDK 17 (ADR 0003).
@Config(sdk = [34])
internal class ButtonHintFooterScreenshotTest(
    private val device: ScreenshotDevice,
    private val style: ThemeStyle,
) {
    @Test
    fun xbox() = capture(FaceButtonLayout.Xbox)

    @Test
    fun nintendo() = capture(FaceButtonLayout.Nintendo)

    private fun capture(layout: FaceButtonLayout) {
        RuntimeEnvironment.setQualifiers(device.qualifiers)
        captureRoboImage(
            filePath =
                "src/test/screenshots/ButtonHintFooter_${layout.name.lowercase()}_" +
                    "${device.name.lowercase()}_${style.name.lowercase()}.png",
            roborazziOptions = exactMatch(),
        ) {
            HemisphereTheme(style = style) {
                CompositionLocalProvider(LocalInputMode provides InputMode.Gamepad) {
                    ButtonHintFooter(layout = layout)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun parameters(): List<Array<Any>> =
            ScreenshotDevice.entries.flatMap { device ->
                ThemeStyle.entries.map { style -> arrayOf<Any>(device, style) }
            }
    }
}
