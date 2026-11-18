package io.github.ctorres345.hemisphere.feature.browser

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.fs.testing.file
import io.github.ctorres345.hemisphere.core.fs.testing.folder
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneIntent
import io.github.ctorres345.hemisphere.feature.browser.pane.ScrollAnchor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Back after open lands on the row the user left: in the state, and on screen. */
@RunWith(RobolectricTestRunner::class)
// Robolectric needs Java 21 for API 35 and up; the build runs on JDK 17 (ADR 0003).
@Config(sdk = [34])
class BrowserBackAnchorTest : BrowserViewModelFixture() {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `back on screen scrolls the list to the row the user opened`() {
        val names = (1..60).map { "Folder %02d".format(it) }
        provider.addFolder(home, *names.map { folder(it) }.toTypedArray())
        provider.addFolder(home.child("Folder 45"), file("save.dat"))
        val viewModel = viewModel()
        compose.setContent {
            HemisphereTheme {
                val state by viewModel.state.collectAsState()
                BrowserScreen(state = state, onIntent = viewModel::onIntent)
            }
        }

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Folder 45"))
        compose.onNodeWithText("Folder 45").performClick()
        compose.onNodeWithText("save.dat").assertIsDisplayed()
        compose.runOnIdle { viewModel.send(PaneIntent.Back) }

        compose.onNodeWithText("Folder 45").assertIsDisplayed()
        compose.onNodeWithText("Folder 01").assertDoesNotExist()
    }

    @Test
    fun `opening a folder lists it, and back lists the parent again where the user left it`() =
        runTest(main.dispatcher) {
            provider.addFolder(home, folder("Music"), file("notes.md"))
            provider.addFolder(music, file("song.mp3"))
            val viewModel = viewModel()
            val anchor = ScrollAnchor(focusedRow = "Music", firstVisibleRow = "Music")

            viewModel.send(
                PaneIntent.OpenEntry(
                    viewModel.tab.listing.entries
                        .first(),
                    leaving = anchor,
                ),
            )
            assertEquals(music, viewModel.tab.folder)
            assertEquals(
                listOf("song.mp3"),
                viewModel.tab.listing.entries
                    .map { it.name },
            )

            viewModel.send(PaneIntent.Back)

            assertEquals(home, viewModel.tab.folder)
            assertEquals(anchor, viewModel.tab.anchor)
            assertEquals(
                listOf("Music", "notes.md"),
                viewModel.tab.listing.entries
                    .map { it.name },
            )
        }
}
