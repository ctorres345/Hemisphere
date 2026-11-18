package io.github.ctorres345.hemisphere.feature.browser

import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.Listing
import io.github.ctorres345.hemisphere.core.model.VolumeId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneResult
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneState
import org.junit.Assert.assertEquals
import org.junit.Test

/** The screen reducer only routes a pane's result to that pane; the pane rules are in `PaneReducerTest`. */
class BrowserReducerTest {
    private val home = FsPath.root(VolumeId("v"))
    private val music = home.child("Music")
    private val both = BrowserUiState(panes = PaneId.entries.associateWith { PaneState.on(home) })

    @Test
    fun `a pane result changes that pane only`() {
        val next = reduce(both, BrowserResult.Pane(PaneId.Left, PaneResult.Opened(music, leaving = null)))

        assertEquals(
            music,
            next.panes
                .getValue(PaneId.Left)
                .tab.folder,
        )
        assertEquals(
            home,
            next.panes
                .getValue(PaneId.Right)
                .tab.folder,
        )
    }

    @Test
    fun `a result for a pane that is not there changes nothing`() {
        val leftOnly = BrowserUiState(panes = mapOf(PaneId.Left to PaneState.on(home)))

        val next = reduce(leftOnly, BrowserResult.Pane(PaneId.Right, PaneResult.Opened(music, leaving = null)))

        assertEquals(leftOnly, next)
    }

    @Test
    fun `a partial listing shows its rows on the active pane`() {
        val rows = listOf(Entry("a.txt", Entry.Kind.File))

        val next =
            reduce(
                both,
                BrowserResult.Pane(PaneId.Left, PaneResult.ListingChanged(home, Listing(rows, complete = false))),
            )

        assertEquals(
            rows,
            next.activePane
                ?.tab
                ?.listing
                ?.entries,
        )
        assertEquals(
            false,
            next.activePane
                ?.tab
                ?.listing
                ?.complete,
        )
    }
}
