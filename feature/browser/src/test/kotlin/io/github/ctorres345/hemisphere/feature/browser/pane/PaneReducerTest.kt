package io.github.ctorres345.hemisphere.feature.browser.pane

import io.github.ctorres345.hemisphere.core.domain.files.FolderLock
import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.Listing
import io.github.ctorres345.hemisphere.core.model.VolumeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaneReducerTest {
    private val home = FsPath.root(VolumeId("v"))
    private val music = home.child("Music")
    private val rock = music.child("Rock")
    private val anchor = ScrollAnchor(focusedRow = "music", firstVisibleRow = "docs", firstVisibleOffset = 12)
    private val row = Entry("song.mp3", Entry.Kind.File)

    @Test
    fun `opening a folder pushes it and remembers where the user was`() {
        val next = reducePane(PaneState.on(home), PaneResult.Opened(music, leaving = anchor)).tab

        assertEquals(listOf(home, music), next.backStack)
        assertEquals(anchor, next.anchors[home.key])
        assertEquals(TabState.LOADING, next.listing)
    }

    @Test
    fun `going back pops and brings the anchor of the folder shown again`() {
        val opened = reducePane(PaneState.on(home), PaneResult.Opened(music, leaving = anchor))

        val back = reducePane(opened, PaneResult.WentBack).tab

        assertEquals(listOf(home), back.backStack)
        assertEquals(anchor, back.anchor)
    }

    @Test
    fun `going back at the root does nothing`() {
        val pane = PaneState.on(home)

        assertEquals(pane, reducePane(pane, PaneResult.WentBack))
    }

    @Test
    fun `anchors of folders popped off the stack are dropped`() {
        var pane = reducePane(PaneState.on(home), PaneResult.Opened(music, leaving = anchor))
        pane = reducePane(pane, PaneResult.Opened(rock, leaving = anchor))
        pane = reducePane(pane, PaneResult.WentBack)
        pane = reducePane(pane, PaneResult.WentBack)

        assertEquals(setOf(home.key), pane.tab.anchors.keys)
    }

    @Test
    fun `the stack is capped and the oldest folders drop off`() {
        var pane = PaneState.on(home)
        repeat(BACK_STACK_LIMIT + 5) { i ->
            pane =
                reducePane(pane, PaneResult.Opened(home.child("d$i"), leaving = anchor))
        }

        assertEquals(BACK_STACK_LIMIT, pane.tab.backStack.size)
        assertFalse(home in pane.tab.backStack)
        assertTrue(pane.tab.anchors.size < BACK_STACK_LIMIT)
    }

    @Test
    fun `a listing for a folder already left is ignored`() {
        val pane = reducePane(PaneState.on(home), PaneResult.Opened(music, leaving = null))

        val next = reducePane(pane, PaneResult.ListingChanged(home, Listing(listOf(row), complete = true)))

        assertEquals(pane, next)
    }

    @Test
    fun `a locked folder lists as done and empty`() {
        val lock = FolderLock(locked = true)

        val next = reducePane(PaneState.on(music), PaneResult.ListingStarted(music, lock)).tab

        assertEquals(lock, next.lock)
        assertEquals(Listing(emptyList(), complete = true), next.listing)
    }

    @Test
    fun `a retry keeps the rows shown and clears the error`() {
        val shown = Listing(listOf(row), complete = true, error = FsError.Io(null))
        val pane = PaneState(tabs = listOf(TabState(backStack = listOf(home), listing = shown)))

        val next = reducePane(pane, PaneResult.ListingStarted(home, FolderLock.NONE)).tab

        assertEquals(listOf(row), next.listing.entries)
        assertFalse(next.listing.complete)
        assertNull(next.listing.error)
    }
}
