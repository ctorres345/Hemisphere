package io.github.ctorres345.hemisphere.feature.browser

import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.ThemeStyle
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Golden images of every Browser state, in each theme, on a phone and a handheld.
 *
 * The goldens in `src/test/screenshots/` are Linux renders. macOS and Windows draw text a few pixels
 * differently, so never commit images recorded there. CI's `gradle-check` (`./gradlew check` on Linux)
 * compares every image exactly and fails on any difference; on failure it uploads the expected, actual and
 * diff images as the `roborazzi-diffs` artifact.
 *
 * After an intended UI change, re-record on Linux: run the `android` workflow by hand on the branch with
 * `record-screenshots` ticked, download its `screenshot-goldens` artifact, and commit its images over
 * `src/test/screenshots/`. On a Linux machine, `./gradlew recordRoborazziDebug` does the same.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Robolectric needs Java 21 for API 35 and up; the build runs on JDK 17 (ADR 0003).
@Config(sdk = [34])
class BrowserScreenshotTest(
    private val device: Device,
    private val style: ThemeStyle,
) {
    /** Screen sizes from Cesar's target devices (docs: hemisphere target devices). */
    enum class Device(
        val qualifiers: String,
    ) {
        /** A common 6.4" 20:9 phone in portrait. */
        Phone("w411dp-h914dp-port-hdpi"),

        /** A 7" 1080p handheld (AYN Odin 2 Portal class) in landscape. */
        Handheld("w960dp-h540dp-land-hdpi"),
    }

    @Test
    fun list() = capture("list", BrowserUiState(entries = SAMPLE_ENTRIES, loading = false))

    @Test
    fun empty() = capture("empty", BrowserUiState(loading = false))

    @Test
    fun error() = capture("error", BrowserUiState(loading = false, error = FsError.AccessDenied))

    /** A listing that failed part-way: the error row sits above the entries that did load. */
    @Test
    fun partialError() =
        capture("partial-error", BrowserUiState(entries = SAMPLE_ENTRIES, loading = false, error = FsError.Timeout))

    private fun capture(
        state: String,
        uiState: BrowserUiState,
    ) {
        RuntimeEnvironment.setQualifiers(device.qualifiers)
        captureRoboImage(
            filePath = "src/test/screenshots/browser-$state-${device.name.lowercase()}-${style.name.lowercase()}.png",
            // Exact match: Roborazzi's default tolerance lets a one-step color change through.
            // Built here, not in the companion, because RoborazziOptions needs the Robolectric sandbox.
            roborazziOptions =
                RoborazziOptions(
                    compareOptions =
                        RoborazziOptions.CompareOptions(
                            imageComparator = SimpleImageComparator(maxDistance = 0f),
                            changeThreshold = 0f,
                        ),
                ),
        ) {
            HemisphereTheme(style = style) { BrowserScreen(state = uiState, onIntent = {}) }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun parameters(): List<Array<Any>> =
            Device.entries.flatMap { device -> ThemeStyle.entries.map { style -> arrayOf<Any>(device, style) } }

        private val SAMPLE_ENTRIES =
            listOf(
                Entry("Music", Entry.Kind.Folder),
                Entry(
                    "Legend of the Twin Hemispheres - Director's Cut (USA, Europe) (Rev 2).zip",
                    Entry.Kind.File,
                    size = 2_048_000,
                ),
                Entry("Readme.txt", Entry.Kind.File, size = 2048),
            )
    }
}
