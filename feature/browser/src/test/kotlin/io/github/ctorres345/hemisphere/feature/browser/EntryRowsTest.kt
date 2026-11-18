package io.github.ctorres345.hemisphere.feature.browser

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.ctorres345.hemisphere.core.designsystem.HemisphereTheme
import io.github.ctorres345.hemisphere.core.fs.testing.file
import io.github.ctorres345.hemisphere.core.fs.testing.folder
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.Listing
import io.github.ctorres345.hemisphere.core.model.VolumeId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneIntent
import io.github.ctorres345.hemisphere.feature.browser.pane.TabState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The list's rows: the key of every row it adds, and opening a row on screen. */
@RunWith(RobolectricTestRunner::class)
// Robolectric needs Java 21 for API 35 and up; the build runs on JDK 17 (ADR 0003).
@Config(sdk = [34])
class EntryRowsTest : BrowserViewModelFixture() {
    @get:Rule
    val compose = createComposeRule()

    private class RecordingScope : LazyListScope {
        val keys = mutableListOf<Any?>()

        override fun item(
            key: Any?,
            contentType: Any?,
            content: @Composable LazyItemScope.() -> Unit,
        ) {
            keys += key
        }

        override fun items(
            count: Int,
            key: ((index: Int) -> Any)?,
            contentType: (index: Int) -> Any?,
            itemContent: @Composable LazyItemScope.(index: Int) -> Unit,
        ) {
            repeat(count) { keys += key?.invoke(it) }
        }

        @Deprecated("Not used by the browser list", level = DeprecationLevel.HIDDEN)
        override fun stickyHeader(
            key: Any?,
            contentType: Any?,
            content: @Composable LazyItemScope.() -> Unit,
        ): Unit = error("not used")
    }

    private fun keysFor(
        entries: List<Entry>,
        error: FsError? = null,
    ): List<Any?> {
        val tab =
            TabState(backStack = listOf(FsPath.root(VolumeId("v"))), listing = Listing(entries, complete = true, error))
        return RecordingScope().apply { entryRows(tab) }.keys
    }

    @Test
    fun `a listing with two equal names gets a row for each with its own key`() {
        val keys = keysFor(listOf(file("a.txt"), file("a.txt"), folder("a.txt")))

        assertEquals(3, keys.size)
        assertEquals(3, keys.filterNotNull().toSet().size)
    }

    @Test
    fun `the error row never shares a key with an entry`() {
        val keys = keysFor(listOf(file("error-row"), file("error-row")), FsError.Io(null))

        assertEquals(3, keys.size)
        assertEquals(3, keys.filterNotNull().toSet().size)
    }

    @Test
    fun `tapping a folder and then its same-named child on screen opens the child`() {
        val a = home.child("a")
        provider.addFolder(home, folder("a"))
        provider.addFolder(a, folder("a"))
        provider.addFolder(a.child("a"), file("deep.txt"))
        val viewModel = viewModel()
        compose.setContent {
            HemisphereTheme {
                val state by viewModel.state.collectAsState()
                BrowserScreen(state = state, onIntent = viewModel::onIntent)
            }
        }

        // The pane header shows the folder name too; the row is the clickable node.
        compose.onNode(hasText("a") and hasClickAction()).performClick()
        compose.runOnIdle { assertEquals(a, viewModel.tab.folder) }
        compose.onNode(hasText("a") and hasClickAction()).performClick()

        compose.onNodeWithText("deep.txt").assertIsDisplayed()
        assertEquals(listOf(home, a, a.child("a")), viewModel.tab.backStack)
    }

    @Test
    fun `a row tapped after the list moves to an equal listing sends the new folder`() {
        val a = home.child("a")
        val listing = Listing(listOf(folder("a")), complete = true)
        var tab by mutableStateOf(TabState(backStack = listOf(home), listing = listing))
        val sent = mutableListOf<PaneIntent>()
        compose.setContent {
            HemisphereTheme { EntryList(tab, onIntent = { sent += it }) }
        }

        compose.runOnIdle { tab = TabState(backStack = listOf(home, a), listing = listing) }
        compose.onNode(hasText("a") and hasClickAction()).performClick()

        assertEquals(a, (sent.single() as PaneIntent.OpenEntry).from)
    }
}
