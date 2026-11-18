package io.github.ctorres345.hemisphere.feature.browser.selection

import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.Listing
import io.github.ctorres345.hemisphere.core.model.VolumeId
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneIntent
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneResult
import io.github.ctorres345.hemisphere.feature.browser.pane.PaneState
import io.github.ctorres345.hemisphere.feature.browser.pane.TabState
import io.github.ctorres345.hemisphere.feature.browser.pane.reducePane
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionReducerTest {
    private val home = FsPath.root(VolumeId("v"))
    private val music = home.child("Music")
    private val albums = Entry("Albums", Entry.Kind.Folder)
    private val a = Entry("a.flac", Entry.Kind.File, size = 1_000)
    private val b = Entry("b.flac", Entry.Kind.File, size = 2_000)
    private val c = Entry("c.flac", Entry.Kind.File, size = 4_000)
    private val d = Entry("d.flac", Entry.Kind.File, size = 8_000)
    private val rows = listOf(albums, a, b, c, d)

    private fun tab(entries: List<Entry> = rows) =
        TabState(backStack = listOf(home, music), listing = Listing(entries, complete = true))

    private fun TabState.key(entry: Entry) = keyOf(entry)

    private fun TabState.keys(vararg entries: Entry) = entries.map { keyOf(it) }.toSet()

    private fun TabState.longPress(entry: Entry) = reduceSelection(this, SelectionResult.LongPressed(key(entry)))

    private fun TabState.toggle(entry: Entry) = reduceSelection(this, SelectionResult.Toggled(key(entry)))

    @Test
    fun `a long-press starts the selection with its row`() {
        val next = tab().longPress(b)

        assertEquals(next.keys(b), next.selection)
        assertTrue(next.selecting)
    }

    @Test
    fun `taps toggle rows in and out`() {
        val next = tab().longPress(b).toggle(d).toggle(b)

        assertEquals(next.keys(d), next.selection)
    }

    @Test
    fun `a long-press on a second row selects the range between, either way`() {
        val down = tab().longPress(a).longPress(c)
        val up = tab().longPress(d).longPress(b)

        assertEquals(down.keys(a, b, c), down.selection)
        assertEquals(up.keys(b, c, d), up.selection)
    }

    @Test
    fun `a range adds to rows already toggled and starts the next range`() {
        val next =
            tab()
                .longPress(albums)
                .toggle(d)
                .longPress(b)
                .longPress(c)

        // d was the anchor: d..b, then b..c.
        assertEquals(next.keys(albums, b, c, d), next.selection)
        assertEquals(next.key(c), next.selectionAnchor)
    }

    @Test
    fun `untoggling the anchor drops it, so the next long-press adds one row`() {
        val next =
            tab()
                .longPress(a)
                .toggle(c)
                .toggle(c)
                .toggle(a)
                .longPress(d)

        assertEquals(next.keys(d), next.selection)
    }

    @Test
    fun `select all takes every listed row and clear empties it`() {
        val all = reduceSelection(tab().longPress(a), SelectionResult.SelectedAll)
        val cleared = reduceSelection(all, SelectionResult.Cleared)

        assertEquals(all.keys(albums, a, b, c, d), all.selection)
        assertFalse(cleared.selecting)
        assertNull(cleared.selectionAnchor)
    }

    @Test
    fun `a key the folder does not list is ignored`() {
        val stranger = home.child("elsewhere").key

        assertEquals(tab(), reduceSelection(tab(), SelectionResult.LongPressed(stranger)))
        assertEquals(tab(), reduceSelection(tab(), SelectionResult.Toggled(stranger)))
    }

    @Test
    fun `the header counts the selection and sums file sizes`() {
        val summary = tab().longPress(a).longPress(c).selectionSummary()

        assertEquals(SelectionSummary(count = 3, fileBytes = 7_000), summary)
    }

    @Test
    fun `the header has no size once a folder is selected`() {
        val summary = tab().longPress(albums).toggle(a).selectionSummary()

        assertEquals(2, summary.count)
        assertNull(summary.fileBytes)
    }

    @Test
    fun `the header has no size when a file's size is unknown`() {
        val unknown = Entry("stream.bin", Entry.Kind.File)
        val summary = tab(rows + unknown).longPress(a).toggle(unknown).selectionSummary()

        assertNull(summary.fileBytes)
    }

    @Test
    fun `rename is offered for one item only`() {
        val one = tab().longPress(a).selectionSummary()
        val two = tab().longPress(a).toggle(b).selectionSummary()

        assertTrue(one.allows(SelectionAction.Rename))
        assertFalse(two.allows(SelectionAction.Rename))
        assertTrue(two.allows(SelectionAction.Delete))
        assertFalse(tab().selectionSummary().allows(SelectionAction.Copy))
    }

    @Test
    fun `a listing change drops selected rows that are gone`() {
        val selected = tab().longPress(a).longPress(c)
        val pane = PaneState(listOf(selected))

        val next =
            reducePane(
                pane,
                PaneResult.ListingChanged(music, Listing(listOf(albums, a, c, d), complete = true)),
            ).tab

        assertEquals(next.keys(a, c), next.selection)
        assertEquals(next.key(c), next.selectionAnchor)
    }

    @Test
    fun `a gone anchor is dropped with its row`() {
        val selected = tab().longPress(a).longPress(c)
        val pane = PaneState(listOf(selected))

        val next = reducePane(pane, PaneResult.ListingChanged(music, Listing(listOf(a, b), complete = true))).tab

        assertEquals(next.keys(a, b), next.selection)
        assertNull(next.selectionAnchor)
    }

    @Test
    fun `a partial or failed listing keeps rows not read yet`() {
        val selected = tab().longPress(a).longPress(d)
        val pane = PaneState(listOf(selected))
        val partial = Listing(listOf(albums, a), complete = false)
        val failed = Listing(listOf(albums, a), complete = true, error = FsError.Timeout)

        assertEquals(selected.selection, reducePane(pane, PaneResult.ListingChanged(music, partial)).tab.selection)
        assertEquals(selected.selection, reducePane(pane, PaneResult.ListingChanged(music, failed)).tab.selection)
    }

    @Test
    fun `opening a folder or going back starts with nothing selected`() {
        val pane = PaneState(listOf(tab().longPress(a)))

        val opened = reducePane(pane, PaneResult.Opened(music.child("Albums"), leaving = null)).tab
        val back = reducePane(pane, PaneResult.WentBack).tab

        assertFalse(opened.selecting)
        assertFalse(back.selecting)
        assertNull(back.selectionAnchor)
    }

    @Test
    fun `back clears a selection first, and is not a selection change without one`() {
        val selecting = tab().longPress(a)

        assertEquals(SelectionResult.Cleared, selecting.selectionResultFor(PaneIntent.Back))
        assertFalse(reduceSelection(selecting, SelectionResult.Cleared).selecting)
        assertNull(tab().selectionResultFor(PaneIntent.Back))
    }

    @Test
    fun `while selecting a tap toggles the row instead of opening it`() {
        val selecting = tab().longPress(a)

        assertEquals(
            SelectionResult.Toggled(selecting.key(albums)),
            selecting.selectionResultFor(PaneIntent.OpenEntry(albums, from = music, leaving = null)),
        )
        assertNull(tab().selectionResultFor(PaneIntent.OpenEntry(albums, from = music, leaving = null)))
    }

    @Test
    fun `the header has no size while selected rows are not read yet`() {
        val selected = tab().longPress(a).longPress(d)
        val refreshing = selected.copy(listing = Listing(listOf(albums, a), complete = false))

        assertEquals(4, refreshing.selectionSummary().count)
        assertNull(refreshing.selectionSummary().fileBytes)
    }
}
