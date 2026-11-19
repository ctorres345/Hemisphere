package io.github.ctorres345.hemisphere.feature.settings

import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.ThemeStyle
import io.github.ctorres345.hemisphere.core.model.Accent
import io.github.ctorres345.hemisphere.core.model.LayoutPin
import io.github.ctorres345.hemisphere.core.model.Settings
import io.github.ctorres345.hemisphere.core.model.ThemeSetting
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Golden images of the Settings screen in each theme, on a phone (`Settings_<theme>`) and a handheld
 * (`Settings_handheld_<theme>`). The goldens are Linux renders recorded by CI, as for the Browser goldens
 * (see `BrowserScreenshotTest`): never commit images recorded on a Mac.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Robolectric needs Java 21 for API 35 and up; the build runs on JDK 17 (ADR 0003).
@Config(sdk = [34])
class SettingsScreenshotTest(
    private val device: Device,
    private val style: ThemeStyle,
) {
    /** The Browser goldens' screen sizes. */
    enum class Device(
        val qualifiers: String,
        val prefix: String,
    ) {
        Phone("w411dp-h914dp-port-hdpi", "Settings"),
        Handheld("w960dp-h540dp-land-hdpi", "Settings_handheld"),
    }

    @Test
    fun settings() {
        RuntimeEnvironment.setQualifiers(device.qualifiers)
        captureRoboImage(
            filePath = "src/test/screenshots/${device.prefix}_${style.name.lowercase()}.png",
            // Exact match, as the Browser goldens.
            roborazziOptions =
                RoborazziOptions(
                    compareOptions =
                        RoborazziOptions.CompareOptions(
                            imageComparator = SimpleImageComparator(maxDistance = 0f),
                            changeThreshold = 0f,
                        ),
                ),
        ) {
            // The app theme follows the saved accent, so the screen shows in it.
            HemisphereTheme(style = style, accent = SAMPLE.accent.token()) {
                SettingsScreen(
                    state = SettingsUiState(settings = SAMPLE.copy(theme = style.setting())),
                    device = SettingsDevice(version = "0.1.0"),
                    onIntent = {},
                    onBack = {},
                )
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun parameters(): List<Array<Any>> =
            Device.entries.flatMap { device -> ThemeStyle.entries.map { style -> arrayOf<Any>(device, style) } }

        /** The saved theme each golden is drawn in, so its chip shows selected. */
        private fun ThemeStyle.setting(): ThemeSetting =
            when (this) {
                ThemeStyle.Light -> ThemeSetting.Light
                ThemeStyle.Dark -> ThemeSetting.Dark
                ThemeStyle.Amoled -> ThemeSetting.AMOLED
            }

        /** Not the defaults, so each selected chip and the ring show a choice the user made. */
        private val SAMPLE =
            Settings(
                theme = ThemeSetting.AMOLED,
                accent = Accent.Teal,
                layoutPinLandscape = LayoutPin.Dual,
            )
    }
}
