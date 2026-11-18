package io.github.ctorres345.hemisphere.feature.browser.layout

import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.designsystem.ThemeStyle
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.Listing
import io.github.ctorres345.hemisphere.core.model.VolumeId
import io.github.ctorres345.hemisphere.feature.browser.BrowserScreen
import io.github.ctorres345.hemisphere.feature.browser.BrowserUiState
import io.github.ctorres345.hemisphere.feature.browser.ShellState
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneState
import io.github.ctorres345.hemisphere.feature.browser.pane.TabState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Goldens of the two layouts in each theme (prototype artboards "Phone · Single" and "Handheld · Dual"):
 * Single on a phone, and Dual on a handheld with the left pane active and the right one stepped down.
 * Linux renders only, recorded by CI; see `BrowserScreenshotTest` for how to re-record.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Robolectric needs Java 21 for API 35 and up; the build runs on JDK 17 (ADR 0003).
@Config(sdk = [34])
class PaneLayoutScreenshotTest(
    private val style: ThemeStyle,
) {
    @Test
    fun singlePhone() = capture("single_phone", "w411dp-h914dp-port-hdpi", PaneLayout.Single)

    @Test
    fun dualHandheld() = capture("dual_handheld", "w960dp-h540dp-land-hdpi", PaneLayout.Dual)

    private fun capture(
        name: String,
        qualifiers: String,
        layout: PaneLayout,
    ) {
        val uiState =
            BrowserUiState(
                shell = ShellState(activePane = PaneId.Left, layout = layout),
                panes = mapOf(PaneId.Left to pane(MUSIC, MUSIC_ENTRIES), PaneId.Right to pane(GAMES, GAME_ENTRIES)),
            )
        RuntimeEnvironment.setQualifiers(qualifiers)
        captureRoboImage(
            filePath = "src/test/screenshots/PaneLayout_${name}_${style.name.lowercase()}.png",
            // Exact match, as BrowserScreenshotTest. Built here because RoborazziOptions needs the sandbox.
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

    private fun pane(
        folder: FsPath,
        entries: List<Entry>,
    ) = PaneState(listOf(TabState(backStack = listOf(ROOT, folder), listing = Listing(entries, complete = true))))

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun parameters(): List<Array<Any>> = ThemeStyle.entries.map { arrayOf<Any>(it) }

        private val ROOT = FsPath.root(VolumeId("v"))
        private val MUSIC = ROOT.child("Music")
        private val GAMES = ROOT.child("ROMs")

        private val MUSIC_ENTRIES =
            listOf(
                Entry("Albums", Entry.Kind.Folder),
                Entry("Playlists", Entry.Kind.Folder),
                Entry("intro.flac", Entry.Kind.File, size = 31_457_280),
            )

        private val GAME_ENTRIES =
            listOf(
                Entry("GBA", Entry.Kind.Folder),
                Entry("PS1", Entry.Kind.Folder),
                Entry("Legend of the Twin Hemispheres (USA).zip", Entry.Kind.File, size = 2_048_000),
            )
    }
}
