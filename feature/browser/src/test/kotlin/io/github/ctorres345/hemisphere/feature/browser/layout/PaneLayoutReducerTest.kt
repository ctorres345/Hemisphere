package io.github.ctorres345.hemisphere.feature.browser.layout

import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.VolumeId
import io.github.ctorres345.hemisphere.feature.browser.BrowserResult
import io.github.ctorres345.hemisphere.feature.browser.BrowserUiState
import io.github.ctorres345.hemisphere.feature.browser.ShellState
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneResult
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneState
import io.github.ctorres345.hemisphere.feature.browser.pane.reducePane
import io.github.ctorres345.hemisphere.feature.browser.reduce
import org.junit.Assert.assertEquals
import org.junit.Test

/** Switching layout or the active pane changes the shell only; both panes keep their folders. */
class PaneLayoutReducerTest {
    private val home = FsPath.root(VolumeId("v"))
    private val music = home.child("Music")
    private val photos = home.child("Photos")
    private val panes =
        mapOf(
            PaneId.Left to reducePaneOpened(PaneState.on(home), music),
            PaneId.Right to reducePaneOpened(PaneState.on(home), photos),
        )
    private val single = BrowserUiState(panes = panes)

    private fun reducePaneOpened(
        pane: PaneState,
        to: FsPath,
    ): PaneState = reducePane(pane, PaneResult.Opened(to, leaving = null))

    @Test
    fun `switching to Dual and back keeps both panes' folders`() {
        val dual = reduce(single, BrowserResult.LayoutChanged(PaneLayout.Dual))
        val back = reduce(dual, BrowserResult.LayoutChanged(PaneLayout.Single))

        assertEquals(PaneLayout.Dual, dual.shell.layout)
        assertEquals(panes, dual.panes)
        assertEquals(single, back)
    }

    @Test
    fun `Dual shows both panes left to right, Single only the active one`() {
        val dual = reduce(single, BrowserResult.LayoutChanged(PaneLayout.Dual))
        val rightActive = reduce(dual, BrowserResult.Activated(PaneId.Right))

        assertEquals(listOf(PaneId.Left, PaneId.Right), rightActive.shownPanes)
        assertEquals(
            listOf(PaneId.Right),
            reduce(rightActive, BrowserResult.LayoutChanged(PaneLayout.Single)).shownPanes,
        )
        assertEquals(listOf(PaneId.Left), single.shownPanes)
    }

    @Test
    fun `activating a pane changes the active pane only`() {
        val dual = reduce(single, BrowserResult.LayoutChanged(PaneLayout.Dual))

        val next = reduce(dual, BrowserResult.Activated(PaneId.Right))

        assertEquals(ShellState(activePane = PaneId.Right, layout = PaneLayout.Dual), next.shell)
        assertEquals(photos, next.activePane!!.tab.folder)
        assertEquals(panes, next.panes)
    }

    @Test
    fun `in Single, activating the other pane changes nothing`() {
        assertEquals(single, reduce(single, BrowserResult.Activated(PaneId.Right)))
    }
}
