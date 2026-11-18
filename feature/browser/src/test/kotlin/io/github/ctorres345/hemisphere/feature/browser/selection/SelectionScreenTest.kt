package io.github.ctorres345.hemisphere.feature.browser.selection

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.Listing
import io.github.ctorres345.hemisphere.core.model.VolumeId
import io.github.ctorres345.hemisphere.feature.browser.BrowserIntent
import io.github.ctorres345.hemisphere.feature.browser.BrowserScreen
import io.github.ctorres345.hemisphere.feature.browser.BrowserUiState
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneIntent
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneState
import io.github.ctorres345.hemisphere.feature.browser.pane.TabState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The screen side of the selection: system Back while selecting at a root, and the header's text. */
@RunWith(RobolectricTestRunner::class)
// Robolectric needs Java 21 for API 35 and up; the build runs on JDK 17 (ADR 0003).
@Config(sdk = [34], qualifiers = "w411dp-h914dp-port-hdpi")
class SelectionScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val root = FsPath.root(VolumeId("v"))
    private val folder = Entry("Albums", Entry.Kind.Folder)
    private val song = Entry("a.flac", Entry.Kind.File, size = 2_000_000)
    private val atRoot = TabState(backStack = listOf(root), listing = Listing(listOf(folder, song), complete = true))

    private fun show(
        tab: TabState,
        onIntent: (BrowserIntent) -> Unit = {},
    ) {
        val state = BrowserUiState(panes = mapOf(PaneId.Left to PaneState(listOf(tab))))
        compose.setContent { HemisphereTheme { BrowserScreen(state = state, onIntent = onIntent) } }
    }

    private fun TabState.selected(vararg entries: Entry) = copy(selection = entries.map(::keyOf).toSet())

    @Test
    fun `system back at a root with a selection stays in the app and clears it`() {
        val intents = mutableListOf<BrowserIntent>()
        show(atRoot.selected(song)) { intents += it }
        val dispatcher = compose.activity.onBackPressedDispatcher

        assertTrue(dispatcher.hasEnabledCallbacks())
        compose.runOnUiThread { dispatcher.onBackPressed() }

        assertEquals(
            listOf(BrowserIntent.Pane(PaneId.Left, PaneIntent.Back)),
            intents.filterIsInstance<BrowserIntent.Pane>(),
        )
    }

    @Test
    fun `system back at a root with nothing selected is left to the system`() {
        show(atRoot)

        assertFalse(compose.activity.onBackPressedDispatcher.hasEnabledCallbacks())
    }

    @Test
    fun `the header shows the count, and the size only for files`() {
        show(atRoot.selected(song))
        compose.onNodeWithText("1 selected · ", substring = true).assertIsDisplayed()
    }

    @Test
    fun `the header has no size once a folder is selected`() {
        show(atRoot.selected(folder, song))
        compose.onNodeWithText("2 selected").assertIsDisplayed()
    }

    @Test
    fun `rename is offered in the action bar for one item`() {
        show(atRoot.selected(song))
        compose.onNodeWithText("Rename").assertIsEnabled()
    }

    @Test
    fun `rename is not offered for two items`() {
        show(atRoot.selected(folder, song))
        compose.onNodeWithText("Rename").assertIsNotEnabled()
    }
}
