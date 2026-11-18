package io.github.ctorres345.hemisphere.core.designsystem

import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions

/**
 * Exact match: Roborazzi's default tolerance lets a one-step color change through. A function, not a
 * constant, because RoborazziOptions needs the Robolectric sandbox.
 *
 * The goldens in `src/test/screenshots/` are Linux renders recorded by CI (the `android` workflow with
 * `record-screenshots`); never commit images recorded on macOS or Windows. See BrowserScreenshotTest.
 */
internal fun exactMatch() =
    RoborazziOptions(
        compareOptions =
            RoborazziOptions.CompareOptions(
                imageComparator = SimpleImageComparator(maxDistance = 0f),
                changeThreshold = 0f,
            ),
    )

/** Screen widths from Cesar's target devices, as in BrowserScreenshotTest. */
internal enum class ScreenshotDevice(
    val qualifiers: String,
) {
    /** A common 6.4" 20:9 phone in portrait. */
    Phone("w411dp-h914dp-port-hdpi"),

    /** A 7" 1080p handheld (AYN Odin 2 Portal class) in landscape. */
    Handheld("w960dp-h540dp-land-hdpi"),
}
