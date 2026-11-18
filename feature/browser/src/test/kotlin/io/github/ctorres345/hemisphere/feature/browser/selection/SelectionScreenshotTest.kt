package io.github.ctorres345.hemisphere.feature.browser.selection

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
import io.github.ctorres345.hemisphere.feature.browser.layout.PaneLayout
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
 * Goldens of the selection (prototype artboard "Selection + clipboard"): one file on a phone (Rename
 * offered), three files on a phone (header with their size), and a range that takes in a folder on a
 * handheld in Dual (actions in the active pane's header, no size), also on a small handheld (icons
 * only). Linux renders only, recorded by CI; see `BrowserScreenshotTest` for how to re-record.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Robolectric needs Java 21 for API 35 and up; the build runs on JDK 17 (ADR 0003).
@Config(sdk = [34])
class SelectionScreenshotTest(
    private val style: ThemeStyle,
) {
    @Test
    fun one() = capture("one_phone", PHONE, PaneLayout.Single, selected = listOf(ZIP))

    @Test
    fun many() = capture("many_phone", PHONE, PaneLayout.Single, selected = listOf(ZIP, GBA, ISO))

    @Test
    fun range() = capture("range_handheld", HANDHELD, PaneLayout.Dual, selected = listOf(PS1, ZIP, GBA))

    @Test
    fun rangeSmall() =
        capture("range_small_handheld", SMALL_HANDHELD, PaneLayout.Dual, selected = listOf(PS1, ZIP, GBA))

    private fun capture(
        name: String,
        qualifiers: String,
        layout: PaneLayout,
        selected: List<Entry>,
    ) {
        val roms = tab(ROMS, ROM_ENTRIES)
        val selecting =
            roms.copy(
                selection = selected.map(roms::keyOf).toSet(),
                selectionAnchor = roms.keyOf(selected.last()),
            )
        val uiState =
            BrowserUiState(
                shell = ShellState(activePane = PaneId.Left, layout = layout),
                panes =
                    mapOf(
                        PaneId.Left to PaneState(listOf(selecting)),
                        PaneId.Right to PaneState(listOf(tab(MUSIC, MUSIC_ENTRIES))),
                    ),
            )
        RuntimeEnvironment.setQualifiers(qualifiers)
        captureRoboImage(
            filePath = "src/test/screenshots/Selection_${name}_${style.name.lowercase()}.png",
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

    private fun tab(
        folder: FsPath,
        entries: List<Entry>,
    ) = TabState(backStack = listOf(ROOT, folder), listing = Listing(entries, complete = true))

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun parameters(): List<Array<Any>> = ThemeStyle.entries.map { arrayOf<Any>(it) }

        private const val PHONE = "w411dp-h914dp-port-hdpi"
        private const val HANDHELD = "w960dp-h540dp-land-hdpi"
        private const val SMALL_HANDHELD = "w640dp-h360dp-land-xxhdpi"

        private val ROOT = FsPath.root(VolumeId("v"))
        private val ROMS = ROOT.child("ROMs")
        private val MUSIC = ROOT.child("Music")

        private val GBA_FOLDER = Entry("GBA", Entry.Kind.Folder)
        private val PS1 = Entry("PS1", Entry.Kind.Folder)
        private val ZIP = Entry("Legend of the Twin Hemispheres (USA).zip", Entry.Kind.File, size = 734_003_200)
        private val GBA = Entry("Pocket Racer Turbo (USA).gba", Entry.Kind.File, size = 16_777_216)
        private val ISO = Entry("Moonlit Harbor (Japan) (En).iso", Entry.Kind.File, size = 524_288_000)

        private val ROM_ENTRIES = listOf(GBA_FOLDER, PS1, ZIP, GBA, ISO)

        private val MUSIC_ENTRIES =
            listOf(
                Entry("Albums", Entry.Kind.Folder),
                Entry("intro.flac", Entry.Kind.File, size = 31_457_280),
            )
    }
}
