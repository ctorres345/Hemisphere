package io.github.ctorres345.hemisphere.feature.browser

import io.github.ctorres345.hemisphere.core.model.Entry
import io.github.ctorres345.hemisphere.core.model.FsError
import io.github.ctorres345.hemisphere.core.model.FsPath
import io.github.ctorres345.hemisphere.core.model.Listing
import io.github.ctorres345.hemisphere.core.model.VolumeId
import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserReducerTest {
    private val home = FsPath.root(VolumeId("v"))
    private val music = Entry("Music", Entry.Kind.Folder)

    @Test
    fun `opening a folder starts loading and clears the error`() {
        val state = BrowserUiState(loading = false, error = FsError.NotFound)

        val next = reduce(state, BrowserResult.FolderOpened(home))

        assertEquals(BrowserUiState(folder = home, loading = true, error = null), next)
    }

    @Test
    fun `retrying the same folder keeps its rows`() {
        val state = BrowserUiState(folder = home, entries = listOf(music), loading = false, error = FsError.Io(null))

        val next = reduce(state, BrowserResult.FolderOpened(home))

        assertEquals(listOf(music), next.entries)
        assertEquals(true, next.loading)
    }

    @Test
    fun `opening another folder drops the old rows`() {
        val state = BrowserUiState(folder = home, entries = listOf(music), loading = false)

        val next = reduce(state, BrowserResult.FolderOpened(home.child("Music")))

        assertEquals(emptyList<Entry>(), next.entries)
    }

    @Test
    fun `a partial listing shows rows and keeps loading`() {
        val next =
            reduce(
                BrowserUiState(folder = home),
                BrowserResult.ListingChanged(Listing(listOf(music), complete = false)),
            )

        assertEquals(listOf(music), next.entries)
        assertEquals(true, next.loading)
    }

    @Test
    fun `a complete listing stops loading and carries its error`() {
        val next =
            reduce(
                BrowserUiState(folder = home),
                BrowserResult.ListingChanged(Listing(emptyList(), true, FsError.AccessDenied)),
            )

        assertEquals(BrowserUiState(folder = home, loading = false, error = FsError.AccessDenied), next)
    }
}
