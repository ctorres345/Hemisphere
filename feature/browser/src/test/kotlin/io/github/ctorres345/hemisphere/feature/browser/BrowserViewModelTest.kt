package io.github.ctorres345.hemisphere.feature.browser

import androidx.compose.ui.unit.Dp
import io.github.ctorres345.hemisphere.core.fs.testing.FsCall
import io.github.ctorres345.hemisphere.core.fs.testing.file
import io.github.ctorres345.hemisphere.core.fs.testing.folder
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.LayoutPin
import io.github.ctorres345.hemisphere.core.model.Orientation
import io.github.ctorres345.hemisphere.feature.browser.layout.BrowserWindow
import io.github.ctorres345.hemisphere.feature.browser.layout.PaneLayout
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneIntent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Listing, errors and Retry, and which panes list in each layout. */
class BrowserViewModelTest : BrowserViewModelFixture() {
    @Test
    fun `opens on the home folder, sorted`() =
        runTest(main.dispatcher) {
            provider.addFolder(home, file("notes.md"), folder("Music"))

            val tab = viewModel().tab

            assertEquals(home, tab.folder)
            assertEquals(listOf("Music", "notes.md"), tab.listing.entries.map { it.name })
            assertTrue(tab.listing.complete)
            assertNull(tab.listing.error)
        }

    @Test
    fun `an error shows, and Retry lists again`() =
        runTest(main.dispatcher) {
            provider.addFolder(home, folder("Music"))
            provider.failNext(home, FsError.AccessDenied)
            val viewModel = viewModel()
            assertEquals(FsError.AccessDenied, viewModel.tab.listing.error)

            viewModel.send(PaneIntent.Retry)

            assertEquals(2, provider.listCalls)
            assertNull(viewModel.tab.listing.error)
            assertEquals(
                listOf("Music"),
                viewModel.tab.listing.entries
                    .map { it.name },
            )
        }

    @Test
    fun `opening a file does nothing yet, and back at the root does nothing`() =
        runTest(main.dispatcher) {
            provider.addFolder(home, file("notes.md"))
            val viewModel = viewModel()
            val before = viewModel.state.value

            viewModel.open("notes.md")
            viewModel.send(PaneIntent.Back)

            assertEquals(before, viewModel.state.value)
            assertEquals(1, provider.listCalls)
        }

    @Test
    fun `a second tap on the row just opened does nothing`() =
        runTest(main.dispatcher) {
            provider.addFolder(home, folder("Music"))
            provider.addFolder(music, folder("Rock"))
            val viewModel = viewModel()
            val row =
                viewModel.tab.listing.entries
                    .first()

            viewModel.send(PaneIntent.OpenEntry(row, from = home, leaving = null))
            viewModel.send(PaneIntent.OpenEntry(row, from = home, leaving = null))

            assertEquals(listOf(home, music), viewModel.tab.backStack)
        }

    @Test
    fun `a double tap on a folder whose child has the same name opens the folder only`() =
        runTest(main.dispatcher) {
            val a = home.child("a")
            provider.addFolder(home, folder("a"))
            provider.addFolder(a, folder("a"))
            provider.addFolder(a.child("a"))
            val viewModel = viewModel()
            val row =
                viewModel.tab.listing.entries
                    .first()

            viewModel.send(PaneIntent.OpenEntry(row, from = home, leaving = null))
            viewModel.send(PaneIntent.OpenEntry(row, from = home, leaving = null))

            assertEquals(listOf(home, a), viewModel.tab.backStack)
        }

    @Test
    fun `a folder inside a same-named folder opens from its own listing`() =
        runTest(main.dispatcher) {
            val a = home.child("a")
            provider.addFolder(home, folder("a"))
            provider.addFolder(a, folder("a"))
            provider.addFolder(a.child("a"))
            val viewModel = viewModel()

            viewModel.open("a")
            viewModel.open("a")

            assertEquals(listOf(home, a, a.child("a")), viewModel.tab.backStack)
        }

    @Test
    fun `a stale tap from the folder just left is dropped`() =
        runTest(main.dispatcher) {
            val a = home.child("a")
            provider.addFolder(home, folder("a"))
            provider.addFolder(a, folder("a"))
            provider.addFolder(a.child("a"))
            val viewModel = viewModel()
            val row =
                viewModel.tab.listing.entries
                    .first()
            viewModel.open("a")

            viewModel.send(PaneIntent.OpenEntry(row, from = home, leaving = null))

            assertEquals(listOf(home, a), viewModel.tab.backStack)
        }

    @Test
    fun `the right pane lists when Dual shows`() =
        runTest(main.dispatcher) {
            provider.addFolder(home, folder("Music"))
            val viewModel = viewModel()
            assertEquals(1, provider.listCalls)

            viewModel.onIntent(BrowserIntent.WindowChanged(handheld))

            val right =
                viewModel.state.value.panes
                    .getValue(PaneId.Right)
                    .tab
            assertEquals(PaneLayout.Dual, viewModel.state.value.shell.layout)
            assertEquals(2, provider.listCalls)
            assertEquals(listOf("Music"), right.listing.entries.map { it.name })
        }

    @Test
    fun `the right pane stops listing when Single hides it, and keeps its folder`() =
        runTest(main.dispatcher) {
            provider.addFolder(home, folder("Music"))
            val viewModel = viewModel()
            val stall = provider.stallNext(FsCall.List)
            viewModel.onIntent(BrowserIntent.WindowChanged(handheld))
            stall.awaitReached()

            viewModel.onIntent(BrowserIntent.WindowChanged(handheld.copy(width = Dp(599f))))
            stall.release()

            val right =
                viewModel.state.value.panes
                    .getValue(PaneId.Right)
                    .tab
            assertEquals(PaneLayout.Single, viewModel.state.value.shell.layout)
            assertEquals(home, right.folder)
            assertTrue(right.listing.entries.isEmpty())
            assertFalse(right.listing.complete)
        }

    @Test
    fun `tapping a pane activates it in Dual only`() =
        runTest(main.dispatcher) {
            val viewModel = viewModel()

            viewModel.onIntent(BrowserIntent.Activate(PaneId.Right))
            assertEquals(PaneId.Left, viewModel.state.value.shell.activePane)

            viewModel.onIntent(BrowserIntent.WindowChanged(handheld))
            viewModel.onIntent(BrowserIntent.Activate(PaneId.Right))
            assertEquals(PaneId.Right, viewModel.state.value.shell.activePane)
        }

    @Test
    fun `the toggle pins the other layout for this orientation, and switches to it`() =
        runTest(main.dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(BrowserIntent.WindowChanged(handheld))

            viewModel.onIntent(BrowserIntent.ToggleLayout)

            assertEquals(PaneLayout.Single, viewModel.state.value.shell.layout)
            val saved = settings.settings.first()
            assertEquals(LayoutPin.Single, saved.layoutPinLandscape)
            assertEquals(LayoutPin.Auto, saved.layoutPinPortrait)
        }

    @Test
    fun `switching to Single keeps the active pane's folder on screen`() =
        runTest(main.dispatcher) {
            provider.addFolder(home, folder("Music"))
            val viewModel = viewModel()
            viewModel.onIntent(BrowserIntent.WindowChanged(handheld))
            viewModel.onIntent(BrowserIntent.Activate(PaneId.Right))
            viewModel.onIntent(
                BrowserIntent.Pane(PaneId.Right, PaneIntent.OpenEntry(folder("Music"), from = home, leaving = null)),
            )

            viewModel.onIntent(BrowserIntent.WindowChanged(handheld.copy(width = Dp(599f))))

            assertEquals(listOf(PaneId.Right), viewModel.state.value.shownPanes)
            assertEquals(music, viewModel.tab.folder)
            assertEquals(
                home,
                viewModel.state.value.panes
                    .getValue(PaneId.Left)
                    .tab.folder,
            )
        }
}
